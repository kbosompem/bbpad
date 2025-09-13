#!/bin/bash
set -e

echo "🔐 BBPad Code Signing and Notarization Helper"
echo "============================================="

# Configuration
APP_NAME="BBPad"
BUILD_DIR="build"
APP_BUNDLE="$BUILD_DIR/$APP_NAME.app"
DMG_NAME="$APP_NAME-0.1.0-macOS.dmg"
SIGNED_DMG_NAME="$APP_NAME-0.1.0-macOS-Signed.dmg"

# Check for required environment variables
if [ -z "$DEVELOPER_IDENTITY" ]; then
    echo "❌ Error: DEVELOPER_IDENTITY environment variable not set"
    echo "   Set it to your 'Developer ID Application' certificate name:"
    echo "   export DEVELOPER_IDENTITY='Developer ID Application: Your Name (TEAMID)'"
    echo ""
    echo "   To find your identity, run:"
    echo "   security find-identity -p codesigning -v"
    exit 1
fi

if [ -z "$NOTARYTOOL_PROFILE" ]; then
    echo "❌ Error: NOTARYTOOL_PROFILE environment variable not set"
    echo "   Set it to your notarytool keychain profile:"
    echo "   export NOTARYTOOL_PROFILE='your-profile-name'"
    echo ""
    echo "   To create a profile, run:"
    echo "   xcrun notarytool store-credentials 'your-profile-name' --apple-id 'your-email' --team-id 'TEAMID' --password 'app-specific-password'"
    exit 1
fi

# Verify app bundle exists
if [ ! -d "$APP_BUNDLE" ]; then
    echo "❌ Error: App bundle not found at $APP_BUNDLE"
    echo "   Run ./build_installer.sh first to create the app bundle"
    exit 1
fi

echo "🔍 Signing Configuration:"
echo "   Developer Identity: $DEVELOPER_IDENTITY"
echo "   Notarization Profile: $NOTARYTOOL_PROFILE"
echo "   App Bundle: $APP_BUNDLE"
echo ""

# Step 1: Sign the app bundle
echo "📝 Step 1: Signing app bundle..."

# Sign the bundled Babashka binary first
if [ -f "$APP_BUNDLE/Contents/Resources/bin/bb" ]; then
    echo "   Signing Babashka binary..."
    codesign --force --options runtime --deep --sign "$DEVELOPER_IDENTITY" "$APP_BUNDLE/Contents/Resources/bin/bb"
fi

# Sign any other binaries or libraries
find "$APP_BUNDLE" -type f \( -name "*.dylib" -o -name "*.so" \) -exec codesign --force --options runtime --sign "$DEVELOPER_IDENTITY" {} \;

# Sign the main app bundle
echo "   Signing main app bundle..."
codesign --force --options runtime --deep --sign "$DEVELOPER_IDENTITY" "$APP_BUNDLE"

# Verify the signature
echo "   Verifying signature..."
codesign --verify --verbose=2 "$APP_BUNDLE"
if [ $? -eq 0 ]; then
    echo "   ✅ App bundle signed successfully"
else
    echo "   ❌ App bundle signature verification failed"
    exit 1
fi

# Step 2: Create signed DMG
echo ""
echo "📦 Step 2: Creating signed DMG..."

# Remove existing signed DMG
rm -f "$BUILD_DIR/$SIGNED_DMG_NAME"

# Create temp folder for signed DMG contents
DMG_TEMP="$BUILD_DIR/signed_dmg_temp"
rm -rf "$DMG_TEMP"
mkdir -p "$DMG_TEMP"

# Copy signed app bundle
cp -R "$APP_BUNDLE" "$DMG_TEMP/"

# Create Applications symlink
ln -s /Applications "$DMG_TEMP/Applications"

# Copy installation instructions
if [ -f "$BUILD_DIR/dmg_temp/Installation Instructions.txt" ]; then
    cp "$BUILD_DIR/dmg_temp/Installation Instructions.txt" "$DMG_TEMP/"
else
    cat > "$DMG_TEMP/Installation Instructions.txt" << 'README'
BBPad Installation Instructions

1. Drag BBPad.app to the Applications folder
2. Open BBPad from Applications folder or Launchpad
3. BBPad will start automatically and open in your default browser

This version is code-signed and notarized for security.

Requirements:
- macOS 10.15 (Catalina) or later
- No additional dependencies required (Babashka is bundled)

For support, visit: https://github.com/kbosompem/bbpad
README
fi

# Create signed DMG
hdiutil create -srcfolder "$DMG_TEMP" -volname "$APP_NAME Installer (Signed)" -fs HFS+ -fsargs "-c d=64" -format UDZO -imagekey zlib-level=9 -o "$BUILD_DIR/$SIGNED_DMG_NAME"

# Clean up
rm -rf "$DMG_TEMP"

# Sign the DMG
echo "   Signing DMG..."
codesign --force --sign "$DEVELOPER_IDENTITY" "$BUILD_DIR/$SIGNED_DMG_NAME"

echo "   ✅ Signed DMG created: $BUILD_DIR/$SIGNED_DMG_NAME"

# Step 3: Submit for notarization
echo ""
echo "📮 Step 3: Submitting for notarization..."

SUBMIT_LOG=$(mktemp)
if xcrun notarytool submit "$BUILD_DIR/$SIGNED_DMG_NAME" --keychain-profile "$NOTARYTOOL_PROFILE" --wait > "$SUBMIT_LOG" 2>&1; then
    echo "   ✅ Notarization successful!"

    # Extract submission ID for reference
    SUBMISSION_ID=$(grep "id:" "$SUBMIT_LOG" | head -1 | awk '{print $2}')
    if [ -n "$SUBMISSION_ID" ]; then
        echo "   Submission ID: $SUBMISSION_ID"
    fi

    # Staple the notarization ticket
    echo "   Stapling notarization ticket..."
    xcrun stapler staple "$BUILD_DIR/$SIGNED_DMG_NAME"

    if [ $? -eq 0 ]; then
        echo "   ✅ Notarization ticket stapled successfully"
    else
        echo "   ⚠️  Warning: Failed to staple notarization ticket"
        echo "   The DMG is notarized but the ticket is not stapled"
    fi
else
    echo "   ❌ Notarization failed!"
    echo "   Error details:"
    cat "$SUBMIT_LOG"
    echo ""
    echo "   Common issues:"
    echo "   - Invalid Apple ID credentials"
    echo "   - Missing app-specific password"
    echo "   - Code signing issues"
    echo "   - Hardened runtime violations"
    rm -f "$SUBMIT_LOG"
    exit 1
fi

rm -f "$SUBMIT_LOG"

# Step 4: Verify notarization
echo ""
echo "🔍 Step 4: Verifying notarization..."

if xcrun stapler validate "$BUILD_DIR/$SIGNED_DMG_NAME"; then
    echo "   ✅ Notarization verification successful"
else
    echo "   ❌ Notarization verification failed"
    exit 1
fi

# Final summary
echo ""
echo "🎉 Code Signing and Notarization Complete!"
echo "=========================================="
echo ""
echo "📦 Distribution Files:"
echo "   Original DMG: $BUILD_DIR/$DMG_NAME"
echo "   Signed & Notarized DMG: $BUILD_DIR/$SIGNED_DMG_NAME"
echo ""
echo "📊 File Sizes:"
if [ -f "$BUILD_DIR/$DMG_NAME" ]; then
    echo "   Original: $(du -sh "$BUILD_DIR/$DMG_NAME" | cut -f1)"
fi
echo "   Signed & Notarized: $(du -sh "$BUILD_DIR/$SIGNED_DMG_NAME" | cut -f1)"
echo ""
echo "🚀 Ready for Distribution:"
echo "   The signed and notarized DMG can be safely distributed to end users."
echo "   Users will not see Gatekeeper warnings when installing BBPad."
echo ""
echo "📋 Distribution Checklist:"
echo "   ✅ App bundle created and signed"
echo "   ✅ DMG created and signed"
echo "   ✅ Notarization submitted and approved"
echo "   ✅ Notarization ticket stapled"
echo "   ✅ Ready for distribution"

ls -la "$BUILD_DIR/$SIGNED_DMG_NAME"
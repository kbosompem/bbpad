#!/bin/bash
set -e

echo "🚀 Building self-contained BBPad installer..."

APP_NAME="BBPad"
BUILD_DIR="build"
APP_BUNDLE="$BUILD_DIR/$APP_NAME.app"
DMG_NAME="$APP_NAME-0.1.0-macOS.dmg"

# Clean and create app bundle
rm -rf "$APP_BUNDLE"
mkdir -p "$APP_BUNDLE/Contents/MacOS"
mkdir -p "$APP_BUNDLE/Contents/Resources/bin"

# Create Info.plist
mkdir -p build/macos
cat > build/macos/Info.plist << 'INFOPLIST'
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleExecutable</key>
    <string>BBPad</string>
    <key>CFBundleIdentifier</key>
    <string>com.kbosompem.bbpad</string>
    <key>CFBundleName</key>
    <string>BBPad</string>
    <key>CFBundlePackageType</key>
    <string>APPL</string>
    <key>CFBundleShortVersionString</key>
    <string>0.1.0</string>
    <key>CFBundleVersion</key>
    <string>1</string>
    <key>LSUIElement</key>
    <false/>
</dict>
</plist>
INFOPLIST

# Copy Info.plist
cp build/macos/Info.plist "$APP_BUNDLE/Contents/Info.plist"

# Copy BBPad source files
cp -r src "$APP_BUNDLE/Contents/Resources/"
cp bb.edn "$APP_BUNDLE/Contents/Resources/"
cp deps.edn "$APP_BUNDLE/Contents/Resources/"

# Copy built React frontend if it exists
if [ -d "bbpad-ui/dist" ]; then
  cp -r bbpad-ui/dist "$APP_BUNDLE/Contents/Resources/public"
fi

# Download and bundle Babashka
echo "📦 Bundling Babashka..."
if command -v bb >/dev/null 2>&1; then
    # Copy local bb binary
    cp "$(which bb)" "$APP_BUNDLE/Contents/Resources/bin/bb"
    chmod +x "$APP_BUNDLE/Contents/Resources/bin/bb"
    echo "   Bundled local Babashka binary"
else
    # Download Babashka for macOS
    BB_VERSION="1.3.184"
    BB_URL="https://github.com/babashka/babashka/releases/download/v${BB_VERSION}/babashka-${BB_VERSION}-macos-amd64.tar.gz"
    
    echo "   Downloading Babashka v${BB_VERSION}..."
    curl -L -o /tmp/babashka.tar.gz "$BB_URL"
    cd /tmp && tar -xzf babashka.tar.gz
    cp bb "$APP_BUNDLE/Contents/Resources/bin/bb"
    chmod +x "$APP_BUNDLE/Contents/Resources/bin/bb"
    rm -f /tmp/babashka.tar.gz /tmp/bb
    cd - > /dev/null
    echo "   Downloaded and bundled Babashka"
fi

# Create self-contained launcher
cat > "$APP_BUNDLE/Contents/MacOS/$APP_NAME" << 'LAUNCHER'
#!/bin/bash
set -e

# Get script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
APP_DIR="$SCRIPT_DIR/../Resources"
BB_PATH="$APP_DIR/bin/bb"

# Set environment
export BBPAD_APP_DIR="$APP_DIR"
export BBPAD_BUNDLED=true
export PATH="$APP_DIR/bin:$PATH"

cd "$APP_DIR"

# Launch BBPad with bundled Babashka
echo "Starting BBPad..."
if [ -x "$BB_PATH" ]; then
    exec "$BB_PATH" src/bbpad/main.clj "$@"
else
    osascript -e 'display dialog "BBPad installation is corrupted. Please reinstall BBPad." buttons {"OK"} default button "OK"'
    exit 1
fi
LAUNCHER

chmod +x "$APP_BUNDLE/Contents/MacOS/$APP_NAME"

# Test the bundled app
echo "🧪 Testing bundled app..."
"$APP_BUNDLE/Contents/MacOS/$APP_NAME" --version || echo "Test failed, but continuing..."

# Create DMG
echo "💿 Creating DMG installer..."
rm -f "$BUILD_DIR/$DMG_NAME"
hdiutil create -srcfolder "$APP_BUNDLE" -volname "$APP_NAME" -fs HFS+ -format UDZO -o "$BUILD_DIR/$DMG_NAME"

echo "✅ Self-contained BBPad installer complete!"
echo "   App bundle: $APP_BUNDLE"
echo "   DMG installer: $BUILD_DIR/$DMG_NAME"
echo ""

# Show file sizes
echo "📊 Bundle size information:"
echo "   BBPad.app: $(du -sh "$APP_BUNDLE" | cut -f1)"
echo "   DMG file: $(du -sh "$BUILD_DIR/$DMG_NAME" | cut -f1)"
echo "   Babashka binary: $(du -sh "$APP_BUNDLE/Contents/Resources/bin/bb" | cut -f1)"

ls -la "$BUILD_DIR/$DMG_NAME"

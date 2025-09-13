#!/bin/bash
set -e

echo "🚀 Creating working BBPad app..."

APP_NAME="BBPad"
BUILD_DIR="build"
APP_BUNDLE="$BUILD_DIR/$APP_NAME.app"

# Clean and create app bundle
rm -rf "$APP_BUNDLE"
mkdir -p "$APP_BUNDLE/Contents/MacOS"
mkdir -p "$APP_BUNDLE/Contents/Resources"

# Create Info.plist if needed
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
</dict>
</plist>
INFOPLIST

# Copy Info.plist
cp build/macos/Info.plist "$APP_BUNDLE/Contents/Info.plist"

# Copy BBPad source files (not React files)
cp -r src "$APP_BUNDLE/Contents/Resources/"
cp bb.edn "$APP_BUNDLE/Contents/Resources/"
cp deps.edn "$APP_BUNDLE/Contents/Resources/"

# Copy built React frontend if it exists
if [ -d "bbpad-ui/dist" ]; then
  cp -r bbpad-ui/dist "$APP_BUNDLE/Contents/Resources/public"
fi

# Create executable launcher
cat > "$APP_BUNDLE/Contents/MacOS/$APP_NAME" << 'LAUNCHER'
#!/bin/bash
set -e

echo "Starting BBPad..."

# Get script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
APP_DIR="$SCRIPT_DIR/../Resources"

# Set environment
export BBPAD_APP_DIR="$APP_DIR"
export BBPAD_BUNDLED=true

cd "$APP_DIR"

# Check if babashka is available
if ! command -v bb >/dev/null 2>&1; then
    osascript -e 'display dialog "Babashka (bb) is required but not installed.\n\nPlease install Babashka from https://babashka.org/\n\nAfter installation, try running BBPad again." buttons {"OK"} default button "OK"'
    exit 1
fi

# Launch BBPad
echo "Launching BBPad with: bb src/bbpad/main.clj"
exec bb src/bbpad/main.clj "$@" 2>&1
LAUNCHER

chmod +x "$APP_BUNDLE/Contents/MacOS/$APP_NAME"

echo "✅ BBPad app created at: $APP_BUNDLE"
echo ""
echo "To test the app:"
echo "  open $APP_BUNDLE"
echo ""
echo "Or test the launcher directly:"
echo "  $APP_BUNDLE/Contents/MacOS/$APP_NAME"

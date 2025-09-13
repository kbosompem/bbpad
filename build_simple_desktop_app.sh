#!/bin/bash
set -e

echo "🚀 Building BBPad Simple Desktop App..."

APP_NAME="BBPad"
BUILD_DIR="build"
APP_BUNDLE="$BUILD_DIR/$APP_NAME.app"
DMG_NAME="$APP_NAME-0.1.0-macOS.dmg"

# Build React frontend first
echo "🔨 Building React frontend..."
if [ -d "bbpad-ui" ]; then
    cd bbpad-ui && npm run build && cd ..
    echo "   React frontend built successfully"
else
    echo "   Error: bbpad-ui directory not found"
    exit 1
fi

# Verify frontend build
if [ ! -d "bbpad-ui/dist" ]; then
    echo "   Error: bbpad-ui/dist not found - React frontend build failed"
    exit 1
fi

# Clean and create app bundle structure
echo "📦 Creating simple desktop app bundle..."
rm -rf "$APP_BUNDLE"
mkdir -p "$APP_BUNDLE/Contents/MacOS"
mkdir -p "$APP_BUNDLE/Contents/Resources/bin"

# Create enhanced Info.plist
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
    <key>CFBundleDisplayName</key>
    <string>BBPad</string>
    <key>CFBundlePackageType</key>
    <string>APPL</string>
    <key>CFBundleShortVersionString</key>
    <string>0.1.0</string>
    <key>CFBundleVersion</key>
    <string>1</string>
    <key>CFBundleInfoDictionaryVersion</key>
    <string>6.0</string>
    <key>LSMinimumSystemVersion</key>
    <string>10.15.0</string>
    <key>LSUIElement</key>
    <false/>
    <key>NSHighResolutionCapable</key>
    <true/>
    <key>NSRequiresAquaSystemAppearance</key>
    <false/>
    <key>LSApplicationCategoryType</key>
    <string>public.app-category.developer-tools</string>
    <key>CFBundleDocumentTypes</key>
    <array>
        <dict>
            <key>CFBundleTypeExtensions</key>
            <array>
                <string>clj</string>
                <string>cljs</string>
                <string>edn</string>
            </array>
            <key>CFBundleTypeName</key>
            <string>Clojure Files</string>
            <key>CFBundleTypeRole</key>
            <string>Editor</string>
            <key>LSHandlerRank</key>
            <string>Alternate</string>
        </dict>
    </array>
    <key>NSAppTransportSecurity</key>
    <dict>
        <key>NSExceptionDomains</key>
        <dict>
            <key>localhost</key>
            <dict>
                <key>NSTemporaryExceptionAllowsInsecureHTTPLoads</key>
                <true/>
            </dict>
        </dict>
    </dict>
</dict>
</plist>
INFOPLIST

# Copy Info.plist
cp build/macos/Info.plist "$APP_BUNDLE/Contents/Info.plist"

# Copy BBPad source files and resources
echo "📁 Copying application resources..."
cp -r src "$APP_BUNDLE/Contents/Resources/"
cp bb.edn "$APP_BUNDLE/Contents/Resources/"
cp deps.edn "$APP_BUNDLE/Contents/Resources/"

# Copy built React frontend
cp -r bbpad-ui/dist "$APP_BUNDLE/Contents/Resources/public"
echo "   React frontend bundled successfully"

# Download and bundle Babashka
echo "📦 Bundling Babashka..."
ARCH=$(uname -m)
case $ARCH in
    x86_64)
        BB_ARCH="macos-amd64"
        ;;
    arm64)
        BB_ARCH="macos-aarch64"
        ;;
    *)
        echo "   Warning: Unsupported architecture $ARCH, defaulting to amd64"
        BB_ARCH="macos-amd64"
        ;;
esac

if command -v bb >/dev/null 2>&1; then
    cp "$(which bb)" "$APP_BUNDLE/Contents/Resources/bin/bb"
    chmod +x "$APP_BUNDLE/Contents/Resources/bin/bb"
    echo "   Bundled local Babashka binary ($(bb --version))"
else
    BB_VERSION="1.3.194"
    BB_URL="https://github.com/babashka/babashka/releases/download/v${BB_VERSION}/babashka-${BB_VERSION}-${BB_ARCH}.tar.gz"

    echo "   Downloading Babashka v${BB_VERSION} for $BB_ARCH..."
    curl -L -o /tmp/babashka.tar.gz "$BB_URL" || {
        echo "   Error: Failed to download Babashka"
        exit 1
    }

    cd /tmp && tar -xzf babashka.tar.gz
    cp bb "$APP_BUNDLE/Contents/Resources/bin/bb"
    chmod +x "$APP_BUNDLE/Contents/Resources/bin/bb"
    rm -f /tmp/babashka.tar.gz /tmp/bb
    cd - > /dev/null
    echo "   Downloaded and bundled Babashka v${BB_VERSION}"
fi

# Create shell script launcher (simpler approach)
cat > "$APP_BUNDLE/Contents/MacOS/$APP_NAME" << 'LAUNCHER'
#!/bin/bash
set -e

echo "Starting BBPad..."

# Get script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
APP_DIR="$SCRIPT_DIR/../Resources"

# Create log directory for debugging
LOG_DIR="$HOME/Library/Logs/BBPad"
mkdir -p "$LOG_DIR"
LOG_FILE="$LOG_DIR/bbpad-$(date +%Y%m%d).log"

# Function for logging
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

log "Starting BBPad..."

# Set environment variables
export BBPAD_APP_DIR="$APP_DIR"
export BBPAD_BUNDLED=true
export BBPAD_DESKTOP=true

# Ensure app data directory exists
APP_DATA_DIR="$HOME/Library/Application Support/BBPad"
mkdir -p "$APP_DATA_DIR"

# Set working directory
cd "$APP_DIR" || {
    log "Error: Could not access BBPad application directory."
    osascript -e 'display dialog "Error: Could not access BBPad application directory." buttons {"OK"} default button "OK"'
    exit 1
}

# Verify bundled Babashka exists
BB_PATH="$APP_DIR/bin/bb"
if [ ! -x "$BB_PATH" ]; then
    log "Error: Bundled Babashka not found or not executable at $BB_PATH"
    osascript -e 'display dialog "BBPad installation is corrupted. The Babashka runtime is missing.\n\nPlease reinstall BBPad." buttons {"OK"} default button "OK"'
    exit 1
fi

# Verify main script exists
MAIN_SCRIPT="$APP_DIR/src/bbpad/main.clj"
if [ ! -f "$MAIN_SCRIPT" ]; then
    log "Error: Main script not found at $MAIN_SCRIPT"
    osascript -e 'display dialog "BBPad installation is corrupted. Core application files are missing.\n\nPlease reinstall BBPad." buttons {"OK"} default button "OK"'
    exit 1
fi

# Check for required React frontend
if [ ! -d "$APP_DIR/public" ]; then
    log "Warning: React frontend not found - BBPad may not function properly"
fi

# Find available port
SERVER_PORT=8080
for port in {8080..8100}; do
    if ! lsof -i :$port > /dev/null 2>&1; then
        SERVER_PORT=$port
        break
    fi
done

log "Using port $SERVER_PORT"

# Handle command line arguments
if [ "$1" = "--version" ]; then
    "$BB_PATH" -e "(println \"BBPad 0.1.0\")"
    exit 0
elif [ "$1" = "--help" ]; then
    echo "BBPad - A LINQPad-inspired tool for Babashka scripts"
    echo "Usage: $0 [options]"
    echo "Options:"
    echo "  --version    Show version"
    echo "  --help       Show this help"
    exit 0
fi

# Launch BBPad server in background
log "Launching BBPad with bundled Babashka on port $SERVER_PORT..."
"$BB_PATH" "$MAIN_SCRIPT" --port "$SERVER_PORT" > "$LOG_FILE" 2>&1 &
BBPAD_PID=$!
log "BBPad started with PID $BBPAD_PID"

# Give the server time to start
sleep 3

# Check if process is still running
if kill -0 "$BBPAD_PID" 2>/dev/null; then
    log "BBPad server started successfully"

    # Open in default browser
    log "Opening BBPad in default browser..."
    open "http://localhost:$SERVER_PORT" 2>/dev/null || {
        log "Warning: Could not open browser automatically"
        osascript -e "display dialog \"BBPad is running on http://localhost:$SERVER_PORT\n\nOpen this URL in your web browser to use BBPad.\" buttons {\"OK\"} default button \"OK\""
    }
else
    log "Error: BBPad failed to start"
    osascript -e 'display dialog "BBPad failed to start. Check Console logs for details.\n\nLog file: '"$LOG_FILE"'" buttons {"OK"} default button "OK"'
    exit 1
fi

log "BBPad is now running. You can close this terminal."
LAUNCHER

chmod +x "$APP_BUNDLE/Contents/MacOS/$APP_NAME"

# Test the app
echo "🧪 Testing simple desktop app..."
if "$APP_BUNDLE/Contents/MacOS/$APP_NAME" --version; then
    echo "   Desktop app test passed"
else
    echo "   Warning: Desktop app test failed"
fi

# Create DMG
echo "💿 Creating DMG installer..."
rm -f "$BUILD_DIR/$DMG_NAME"

DMG_TEMP="$BUILD_DIR/dmg_temp"
rm -rf "$DMG_TEMP"
mkdir -p "$DMG_TEMP"

cp -R "$APP_BUNDLE" "$DMG_TEMP/"
ln -s /Applications "$DMG_TEMP/Applications"

cat > "$DMG_TEMP/Installation Instructions.txt" << 'README'
BBPad Desktop App Installation Instructions

1. Drag BBPad.app to the Applications folder
2. Open BBPad from Applications folder or Launchpad
3. BBPad will start and open in your default browser
4. If you see a security warning, go to:
   System Preferences > Security & Privacy > General
   and click "Open Anyway"

Features:
- Self-contained with bundled Babashka runtime
- Automatic server management
- Opens in your default web browser
- Professional desktop app experience

Requirements:
- macOS 10.15 (Catalina) or later
- No additional dependencies required

For support, visit: https://github.com/kbosompem/bbpad
README

hdiutil create -srcfolder "$DMG_TEMP" -volname "BBPad Desktop" -fs HFS+ -format UDZO -o "$BUILD_DIR/$DMG_NAME"
rm -rf "$DMG_TEMP"
chmod 644 "$BUILD_DIR/$DMG_NAME"

echo "✅ BBPad Simple Desktop App complete!"
echo ""
echo "📦 Installation Package:"
echo "   App bundle: $APP_BUNDLE"
echo "   DMG installer: $BUILD_DIR/$DMG_NAME"
echo ""
echo "📊 Bundle size information:"
echo "   BBPad.app: $(du -sh "$APP_BUNDLE" | cut -f1)"
echo "   DMG file: $(du -sh "$BUILD_DIR/$DMG_NAME" | cut -f1)"

echo ""
echo "🎯 This version opens in your default browser but:"
echo "   ✅ Is a proper macOS app (not just a browser shortcut)"
echo "   ✅ Manages the Babashka server automatically"
echo "   ✅ Self-contained with no external dependencies"
echo "   ✅ Provides proper desktop app experience"

ls -la "$BUILD_DIR/$DMG_NAME"
# BBPad macOS Installer

This document explains the enhanced macOS installer system for BBPad, a LINQPad-inspired tool for Babashka scripts.

## Overview

The macOS installer creates a self-contained application bundle that includes:
- Bundled Babashka runtime (architecture-specific)
- Built React frontend
- All source code and dependencies
- Professional DMG installer with drag-to-install interface

## Quick Start

### Building the Installer

1. **Build React frontend:**
   ```bash
   cd bbpad-ui && npm run build
   ```

2. **Create installer:**
   ```bash
   ./build_installer.sh
   ```

3. **For distribution (optional):**
   ```bash
   # Set your credentials first
   export DEVELOPER_IDENTITY="Developer ID Application: Your Name (TEAMID)"
   export NOTARYTOOL_PROFILE="your-profile-name"

   # Sign and notarize
   ./sign_and_notarize.sh
   ```

## Architecture Support

The installer automatically detects and bundles the correct Babashka binary:
- **Apple Silicon (M1/M2):** `macos-aarch64`
- **Intel:** `macos-amd64`
- **Fallback:** Uses system `bb` if available

## File Structure

```
build/BBPad.app/
├── Contents/
│   ├── Info.plist              # App metadata
│   ├── MacOS/
│   │   └── BBPad              # Launcher script
│   └── Resources/
│       ├── bin/
│       │   └── bb             # Bundled Babashka
│       ├── src/               # BBPad source code
│       ├── public/            # Built React frontend
│       ├── app-data/          # User data directory
│       ├── bb.edn             # Build config
│       └── deps.edn           # Dependencies
```

## Features

### Enhanced Info.plist
- High resolution display support
- Dark mode compatibility
- File type associations (.clj, .cljs, .edn)
- Minimum macOS version (10.15+)
- Developer tools category

### Smart Launcher
- Automatic Babashka version detection
- Comprehensive error handling with user-friendly dialogs
- Logging to `~/Library/Logs/BBPad/`
- Background process management
- Auto-opens browser when server starts

### Professional DMG
- Drag-to-Applications interface
- Installation instructions included
- Proper volume naming and icons
- Optimized compression

### Development Features
- Architecture detection and appropriate binary selection
- Frontend build verification
- Comprehensive testing and validation
- Detailed size reporting

## Scripts

### build_installer.sh
Main installer creation script with features:
- Automatic React frontend building
- Multi-architecture Babashka bundling
- Enhanced Info.plist generation
- Professional DMG creation
- Comprehensive validation and testing

### sign_and_notarize.sh
Code signing and notarization script:
- Deep signing of all binaries
- DMG signing
- Apple notarization submission
- Ticket stapling
- Verification and validation

### create_working_app.sh
Simple development app bundle (requires system Babashka)

## Requirements

### For Building
- macOS 10.15+
- Node.js and npm (for React frontend)
- Babashka (will be bundled automatically)

### For Code Signing
- Apple Developer account
- Developer ID Application certificate
- App-specific password
- Xcode command line tools

## Code Signing Setup

1. **Get your Developer ID:**
   ```bash
   security find-identity -p codesigning -v
   ```

2. **Set environment variables:**
   ```bash
   export DEVELOPER_IDENTITY="Developer ID Application: Your Name (TEAMID)"
   ```

3. **Setup notarization profile:**
   ```bash
   xcrun notarytool store-credentials "bbpad-notary" \
     --apple-id "your-email@example.com" \
     --team-id "TEAMID" \
     --password "app-specific-password"

   export NOTARYTOOL_PROFILE="bbpad-notary"
   ```

## Distribution

### For Testing (Unsigned)
```bash
./build_installer.sh
# Distribute: build/BBPad-0.1.0-macOS.dmg
```

### For Production (Signed & Notarized)
```bash
./build_installer.sh
./sign_and_notarize.sh
# Distribute: build/BBPad-0.1.0-macOS-Signed.dmg
```

## User Experience

1. **Download DMG** - User downloads the installer
2. **Mount DMG** - Double-click to mount the disk image
3. **Drag to Install** - Drag BBPad.app to Applications folder
4. **Launch** - Open BBPad from Applications or Launchpad
5. **Auto-Start** - BBPad starts server and opens browser automatically

## Troubleshooting

### Build Issues
- **Frontend build fails:** Check `bbpad-ui/` exists and `npm run build` works
- **Babashka download fails:** Check internet connection and GitHub access
- **Permission errors:** Ensure write access to `build/` directory

### Runtime Issues
- **Gatekeeper warnings:** Use signed version or System Preferences override
- **Server won't start:** Check logs in `~/Library/Logs/BBPad/`
- **Browser doesn't open:** Manually navigate to `http://localhost:8080`

### Code Signing Issues
- **Certificate not found:** Verify Developer ID certificate installed
- **Notarization fails:** Check Apple ID credentials and app-specific password
- **Stapling fails:** Usually not critical, DMG is still notarized

## Development Notes

- The installer handles both development and production scenarios
- Comprehensive error handling provides clear feedback
- Logging system helps debug deployment issues
- Architecture detection ensures optimal performance
- Frontend build verification prevents broken installations

## Future Enhancements

- WebView integration (replacing browser dependency)
- Custom DMG background and styling
- Automatic update mechanism
- Multiple architecture universal binary
- Installer analytics and telemetry
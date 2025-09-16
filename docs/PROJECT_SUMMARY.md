# BBPad - Project Summary

## Vision
BBPad is a LINQPad-inspired desktop application for Babashka scripts, designed to make script sharing accessible to non-technical users. The goal is to create a simple, powerful tool for running and sharing Babashka scripts locally.

## Target Use Cases
- **Script Sharing**: Share bb scripts via URLs with non-technical friends
- **Data Processing**: Convert Excel to Datalevin DB, API calls to files, DB queries to HTML
- **Simple UI**: Web-based parameter forms, immediate execution, local file operations
- **Desktop Integration**: First-class desktop citizen like LINQPad

## Architecture Decision: Option 1A - WebView + Babashka

### Final Architecture
```
┌─────────────────────────────────────────┐
│    Single bb Executable + WebView       │
├─────────────────────────────────────────┤
│ • Embedded HTTP server (Ring/Jetty)     │
│ • Native WebView window (no browser)    │
│ • ClojureScript frontend (UIx/Helix)    │
│ • Looks like native desktop app         │
│ • Custom window controls, menus         │
└─────────────────────────────────────────┘
```

### Technology Stack
- **Backend**: Babashka + Ring + Jetty server
- **Frontend**: ClojureScript + UIx/Helix + Reagent
- **UI**: CodeMirror for syntax highlighting
- **Window**: Native WebView (WKWebView/WebView2/WebKitGTK)
- **Packaging**: Single executable across platforms
- **Storage**: Local file system (no database backend needed)

### Key Benefits
- **Pure Clojure/Babashka**: Full stack consistency
- **Single Executable**: Easy distribution and installation
- **Native Feel**: Dedicated app window, not browser tab
- **Cross-Platform**: Windows, macOS, Linux support
- **URL-based Script Sharing**: `bbpad://load?script=https://gist.github.com/...`

## User Experience Flow
1. User downloads `bbpad.exe` (single file)
2. Double-click launches dedicated app window
3. Paste script URL or write script directly
4. Auto-generated parameter forms
5. Click "Run" → immediate local execution
6. Rich result display with syntax highlighting
7. Save scripts locally for reuse

## Development Approach
- **Iterative Development**: Start with MVP, add features incrementally
- **Feature Tracking**: GitHub Issues with acceptance criteria
- **Testing**: Local validation before commits
- **Packaging**: Cross-platform builds via CI/CD

## Rejected Options
- **Electron**: Too heavy, not pure Clojure
- **Browser Tab**: Wanted dedicated app window
- **Datalevin Backend**: Unnecessary complexity for use case
- **Complex Database**: Local file storage sufficient

## Next Steps
1. Initialize repository and GitHub setup
2. Create Product Requirements Document
3. Define user stories as GitHub issues
4. Implement core WebView + Ring integration
5. Build basic script editor and execution engine
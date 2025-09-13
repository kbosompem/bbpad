# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

BBPad is a LINQPad-inspired desktop application for Babashka scripts that combines a Babashka backend with a React TypeScript frontend. The goal is to make script sharing accessible to non-technical users through a desktop application.

## Architecture

### Hybrid Architecture: Babashka Backend + React Frontend
- **Backend**: Pure Babashka with HTTP-Kit server (port 8082)
- **Frontend**: React + TypeScript + Vite development server (port 5173) 
- **UI Framework**: shadcn/ui components with Tailwind CSS
- **Database**: SQLite via Babashka pods (org.babashka/go-sqlite3)
- **Development**: Two separate dev servers communicating via HTTP APIs

### Key Design Decisions
- **Replaced ClojureScript**: Originally planned with ClojureScript/UIx, now uses React/TypeScript for faster development
- **Dual Development Mode**: Backend runs on 8082, frontend dev server on 5173 with API proxy
- **Pod-Based Database**: Uses Babashka pods instead of next.jdbc for better compatibility
- **Single Executable Target**: Eventually will embed React build into Babashka executable

## Development Commands

### Start Development Environment
```bash
# Start backend server (runs on port 8082)
bb dev

# Start frontend development server (runs on port 5173)
cd bbpad-ui && npm run dev
# OR from root:
npm run dev
```

### Build Commands
```bash
# Build React frontend for production
cd bbpad-ui && npm run build
# OR from root:
npm run build

# Full build including executable creation
bb build

# Clean build artifacts
bb clean
```

### Testing
```bash
# Frontend linting
cd bbpad-ui && npm run lint

# Backend tests
bb test

# E2E tests
npm run test:e2e
```

## Core Architecture Components

### Backend Structure (src/bbpad/)
- **main.clj**: Entry point, CLI parsing, server/WebView lifecycle
- **server/babashka_http.clj**: HTTP-Kit server with Ruuter routing, all API endpoints
- **server/handlers.clj**: Ring request handlers for all API endpoints
- **db/app_storage.clj**: SQLite operations via Babashka pods (scripts, connections, tab sessions)
- **core/script_engine.clj**: Babashka script execution engine
- **core/config.clj**: Configuration management and app directories

### Frontend Structure (bbpad-ui/src/)
- **App.tsx**: Main application component with script execution logic
- **components/ScriptTabs.tsx**: Tab management with persistence
- **components/CodeEditor.tsx**: Monaco Editor for Clojure code
- **components/SaveScriptDialog.tsx**: Script saving modal
- **components/OpenScriptDialog.tsx**: Script loading modal
- **components/ui/**: shadcn/ui component library

## Critical API Endpoints

### Script Operations
- `POST /api/execute` - Execute Babashka script code
- `POST /api/scripts/save` - Save script to database
- `GET /api/scripts/get/:id` - Load script by ID
- `GET /api/scripts/list` - List all saved scripts
- `POST /api/scripts/delete` - Delete script

### Session Management
- `POST /api/tabs/save` - Save current tab session
- `GET /api/tabs/load` - Load saved tab session

### Database Operations
- `GET /api/connections` - List database connections
- `POST /api/connections` - Create database connection
- `POST /api/query` - Execute SQL query

## Key Technical Patterns

### Frontend-Backend Communication
- Frontend makes HTTP requests to `http://localhost:8082/api/*`
- All request/response bodies use JSON
- Error handling via HTTP status codes and error objects

### Tab Persistence
- Tabs automatically save to database with 1-second debouncing
- Session restoration on app startup
- Includes tab content, modified state, and active tab

### Database Integration
- Uses org.babashka/go-sqlite3 pod (NOT next.jdbc)
- Database file: `~/Library/Application Support/BBPad/bbpad.db`
- Schema includes: scripts, script_results, connections, tab_sessions

### Script Execution
- Scripts execute in Babashka subprocess with timeout
- Results formatted as JSON with success/error status
- Support for parameters and execution context

## Development Patterns

### Adding New API Endpoints
1. Add route definition in `server/babashka_http.clj`
2. Implement handler function in `server/handlers.clj`
3. Add database operations in `db/app_storage.clj` if needed
4. Update frontend API calls in React components

### Database Schema Changes
- Modify `init-db!` function in `db/app_storage.clj`
- Schema changes are applied automatically on startup
- Use SQLite-compatible syntax (Babashka pod limitations)

### Frontend Component Development
- Use shadcn/ui components for consistency
- Follow existing patterns in ScriptTabs and dialogs
- Implement proper TypeScript interfaces
- Use React hooks for state management

## Babashka-Specific Considerations

### Pod Usage
- Database operations via `org.babashka/go-sqlite3` pod
- Pod loading handled in `core/pod_manager.clj`
- Use vector syntax for parameterized queries: `[sql param1 param2]`

### JDBC Limitations
- Avoid next.jdbc - use pods for database operations
- Some Java classes not available in Babashka runtime
- Test database operations thoroughly in Babashka environment

### Script Execution Safety
- Scripts run in isolated Babashka process
- No access to main application state
- Timeout protection for long-running scripts

## Project Scope (GitHub Issues)

### Current Implementation Phase
- ✅ Basic script execution engine
- ✅ React frontend with script editor
- ✅ Database integration with script storage
- ✅ Tab session persistence
- ✅ Save/Open script workflows

### Planned Features (GitHub Issues)
- **#13**: Single executable with WebView integration (replace dev server setup)
- **#12**: Enhanced database browser with schema visualization
- **#15**: Data visualization with charts and graphs
- **#14**: Advanced script library and sharing features
- **#24**: Datalevin integration for Datalog queries

### Future Enhancements
- Plugin system (#19)
- Performance monitoring (#20)
- Collaboration features (#21)
- Testing tools (#22)
- Documentation system (#23)

## Common Debugging

### Backend Issues
- Check Babashka server logs for pod loading errors
- Verify database file permissions in app config directory
- Test API endpoints with curl for request/response debugging

### Frontend Issues
- Check browser network tab for API call failures
- Verify CORS settings if requests blocked
- Check React dev tools for component state issues

### Database Issues
- Ensure org.babashka/go-sqlite3 pod is properly loaded
- Check SQLite file existence and permissions
- Use vector format for parameterized queries, not separate args

## Build and Deployment

The project targets single executable distribution but currently runs in development mode with separate frontend/backend servers. The final build process will:

1. Build React app to static files
2. Embed static files in Babashka executable
3. Serve React app from embedded HTTP server
4. Launch platform-specific WebView pointing to localhost

Current development maintains this dual-server setup for hot reload and debugging capabilities.
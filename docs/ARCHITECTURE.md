# BBPad Architecture

This document provides a comprehensive overview of BBPad's technical architecture, design decisions, and implementation details.

## 🎯 Design Goals

- **Single Executable**: No installation or dependencies required
- **Native Performance**: Fast startup and script execution
- **Cross-Platform**: Identical experience on Windows, macOS, and Linux
- **Developer Friendly**: Pure Clojure/ClojureScript stack
- **User Friendly**: Web-based UI with native desktop integration

## 🏗️ High-Level Architecture

BBPad combines several technologies to create a unique desktop application experience:

```mermaid
graph TB
    subgraph "BBPad Executable"
        subgraph "Babashka Runtime"
            A[Main Process] --> B[Ring Server]
            A --> C[Script Engine]
            A --> D[WebView Launcher]
            C --> E[JDBC Drivers]
            C --> F[File System Access]
        end
        
        subgraph "Embedded Assets"
            G[ClojureScript Bundle]
            H[HTML Templates]
            I[CSS Styles]
            J[Static Resources]
        end
        
        B --> G
        B --> H
        B --> I
        B --> J
    end
    
    subgraph "Native WebView"
        K[Platform WebView] --> L[Rendered UI]
        L --> M[Event Handlers]
        M --> N[API Calls]
    end
    
    subgraph "External Services"
        O[GitHub/Gists]
        P[Databases]
        Q[File System]
        R[HTTP APIs]
    end
    
    N --> B
    C --> P
    C --> Q
    C --> R
    B --> O
    
    classDef babashka fill:#4CAF50,stroke:#333,stroke-width:2px,color:#fff
    classDef assets fill:#2196F3,stroke:#333,stroke-width:2px,color:#fff
    classDef webview fill:#FF9800,stroke:#333,stroke-width:2px,color:#fff
    classDef external fill:#9C27B0,stroke:#333,stroke-width:2px,color:#fff
    
    class A,B,C,D,E,F babashka
    class G,H,I,J assets
    class K,L,M,N webview
    class O,P,Q,R external
```

## 🔧 Component Details

### 1. Babashka Runtime Layer

The core of BBPad runs entirely in Babashka, providing:

```mermaid
graph LR
    subgraph "Babashka Process"
        A[Main Entry Point] --> B[Server Lifecycle]
        B --> C[Port Management]
        B --> D[Asset Loading]
        B --> E[WebView Launch]
        
        F[Script Engine] --> G[Code Parsing]
        F --> H[Execution Context]
        F --> I[Result Formatting]
        
        J[API Handlers] --> K[Script Execution]
        J --> L[File Operations]
        J --> M[Database Access]
    end
    
    classDef core fill:#4CAF50,stroke:#333,stroke-width:2px
    class A,B,C,D,E,F,G,H,I,J,K,L,M core
```

**Key Responsibilities:**
- HTTP server management (Ring + Jetty)
- Script execution and sandboxing
- WebView process management
- Asset serving and caching
- Database connection pooling

### 2. Web Server (Ring + Jetty)

The embedded web server provides the API and UI serving:

```mermaid
sequenceDiagram
    participant WV as WebView
    participant R as Ring Handler
    participant SE as Script Engine
    participant DB as Database
    
    WV->>R: POST /api/execute
    R->>R: Parse request
    R->>SE: Execute script
    SE->>DB: Query data (if needed)
    DB-->>SE: Return results
    SE-->>R: Formatted results
    R->>R: Stream response
    R-->>WV: Script output (SSE)
```

**API Endpoints:**
- `GET /` - Serve main UI
- `POST /api/execute` - Execute Babashka script
- `GET /api/script/load` - Load script from URL
- `POST /api/script/save` - Save script locally
- `GET /api/connections` - Database connections
- `GET /events` - Server-Sent Events for real-time updates

### 3. Frontend Architecture (ClojureScript + UIx)

The UI is built with modern ClojureScript tooling:

```mermaid
graph TB
    subgraph "ClojureScript Application"
        A[Main App Component] --> B[Editor Panel]
        A --> C[Results Panel]
        A --> D[Toolbar]
        A --> E[Status Bar]
        
        B --> F[CodeMirror Integration]
        B --> G[Syntax Highlighting]
        
        C --> H[Result Renderer]
        C --> I[Table Display]
        C --> J[Chart Components]
        
        K[State Management] --> L[Script State]
        K --> M[UI State]
        K --> N[Connection State]
        
        O[API Client] --> P[HTTP Requests]
        O --> Q[WebSocket/SSE]
    end
    
    subgraph "UI Libraries"
        R[UIx/Helix]
        S[Reagent]
        T[Re-frame]
        U[CodeMirror]
    end
    
    A --> R
    K --> T
    F --> U
    
    classDef ui fill:#2196F3,stroke:#333,stroke-width:2px,color:#fff
    classDef lib fill:#FF9800,stroke:#333,stroke-width:2px
    
    class A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q ui
    class R,S,T,U lib
```

### 4. WebView Integration

Platform-specific WebView handling:

```mermaid
flowchart TD
    A[BBPad Startup] --> B{Detect Platform}
    
    B -->|Windows| C[WebView2]
    B -->|macOS| D[WKWebView]  
    B -->|Linux| E[WebKitGTK]
    
    C --> F[Create Window]
    D --> F
    E --> F
    
    F --> G[Configure WebView]
    G --> H[Load localhost URL]
    H --> I[Enable DevTools if Debug]
    I --> J[Setup Message Handlers]
    J --> K[Window Ready]
    
    classDef platform fill:#FF5722,stroke:#333,stroke-width:2px,color:#fff
    classDef process fill:#4CAF50,stroke:#333,stroke-width:2px,color:#fff
    
    class C,D,E platform
    class A,F,G,H,I,J,K process
```

**WebView Requirements:**
- **Windows**: WebView2 runtime (bundled or system-installed)
- **macOS**: WKWebView (built into macOS)
- **Linux**: WebKitGTK (commonly available)

## 🚀 Startup Sequence

The application follows a carefully orchestrated startup process:

```mermaid
sequenceDiagram
    participant U as User
    participant BBP as BBPad Process
    participant S as Ring Server
    participant WV as WebView
    participant UI as ClojureScript App
    
    U->>BBP: Launch bbpad.exe
    BBP->>BBP: Initialize Babashka runtime
    BBP->>S: Start Ring server on random port
    S-->>BBP: Server ready on port N
    BBP->>WV: Launch WebView → localhost:N
    WV->>S: Request main page
    S->>S: Serve embedded HTML + assets
    S-->>WV: HTML page with ClojureScript
    WV->>UI: Initialize React app
    UI->>S: WebSocket connection for live updates
    UI-->>U: Application ready for use
    
    Note over U,UI: Total startup time: < 3 seconds
```

## 🔒 Security Model

BBPad implements multiple layers of security:

```mermaid
graph TB
    subgraph "Security Layers"
        A[Script Sandboxing] --> B[Limited File Access]
        A --> C[Network Restrictions]
        A --> D[Process Isolation]
        
        E[Input Validation] --> F[Script Content Filtering]
        E --> G[Parameter Sanitization]
        E --> H[URL Validation]
        
        I[WebView Security] --> J[localhost-only Access]
        I --> K[CSP Headers]
        I --> L[CORS Protection]
    end
    
    classDef security fill:#F44336,stroke:#333,stroke-width:2px,color:#fff
    class A,B,C,D,E,F,G,H,I,J,K,L security
```

**Security Principles:**
- Scripts run in Babashka's natural sandbox
- File system access limited to user-designated directories  
- Network access explicit and controllable
- WebView confined to localhost communication
- Input validation at all API boundaries

## 📊 Data Flow

Understanding how data flows through the system:

```mermaid
flowchart TD
    subgraph "User Actions"
        A[Write Script] --> B[Click Run]
        C[Load from URL] --> D[Fill Parameters]
        E[Save Script] --> F[Share URL]
    end
    
    subgraph "Processing Pipeline"
        B --> G[Parse Script]
        G --> H[Validate Syntax]
        H --> I[Execute in Babashka]
        I --> J[Format Results]
        J --> K[Stream to UI]
        
        D --> L[Inject Parameters]
        L --> G
        
        C --> M[Fetch Remote Script]
        M --> N[Parse Metadata]
        N --> O[Generate Parameter Form]
    end
    
    subgraph "Result Display"
        K --> P{Result Type?}
        P -->|Text| Q[Text Display]
        P -->|Data| R[Table Renderer]
        P -->|Chart| S[Visualization]
        P -->|Error| T[Error Display]
    end
    
    classDef action fill:#4CAF50,stroke:#333,stroke-width:2px,color:#fff
    classDef process fill:#2196F3,stroke:#333,stroke-width:2px,color:#fff
    classDef display fill:#FF9800,stroke:#333,stroke-width:2px,color:#fff
    
    class A,B,C,D,E,F action
    class G,H,I,J,K,L,M,N,O process  
    class P,Q,R,S,T display
```

## 🔧 Build and Packaging

The build process creates a self-contained executable:

```mermaid
flowchart TD
    subgraph "Source Files"
        A[ClojureScript Sources] --> B[shadow-cljs Build]
        C[Babashka Scripts] --> D[Script Validation]
        E[Static Assets] --> F[Asset Processing]
        G[WebView Binaries] --> H[Platform Detection]
    end
    
    subgraph "Build Pipeline"
        B --> I[Compile to JS]
        I --> J[Optimize Bundle]
        J --> K[Generate Assets Map]
        
        D --> L[Create Main Script]
        L --> M[Embed Asset Data]
        
        F --> N[Compress Assets]
        N --> M
        
        H --> O[Bundle WebView Tools]
        O --> M
    end
    
    subgraph "Output"
        M --> P[Single Executable]
        P --> Q[Windows .exe]
        P --> R[macOS Binary]
        P --> S[Linux Binary]
    end
    
    classDef source fill:#E1F5FE,stroke:#333,stroke-width:2px
    classDef build fill:#FFF3E0,stroke:#333,stroke-width:2px
    classDef output fill:#E8F5E8,stroke:#333,stroke-width:2px
    
    class A,C,E,G source
    class B,D,F,H,I,J,K,L,M,N,O build
    class P,Q,R,S output
```

## 🧪 Testing Strategy

Comprehensive testing across all layers:

```mermaid
graph TB
    subgraph "Test Pyramid"
        A[Unit Tests] --> B[Component Tests]
        B --> C[Integration Tests]
        C --> D[E2E Tests]
        
        E[Babashka Script Tests] --> A
        F[ClojureScript Tests] --> A
        G[API Tests] --> C
        H[WebView Tests] --> D
        I[Cross-Platform Tests] --> D
    end
    
    subgraph "Test Tools"
        J[clojure.test] --> E
        K[shadow-cljs test-runner] --> F
        L[Ring Mock] --> G
        M[Playwright] --> H
        N[GitHub Actions Matrix] --> I
    end
    
    classDef test fill:#4CAF50,stroke:#333,stroke-width:2px,color:#fff
    classDef tool fill:#2196F3,stroke:#333,stroke-width:2px,color:#fff
    
    class A,B,C,D,E,F,G,H,I test
    class J,K,L,M,N tool
```

## ⚠️ Babashka Limitations & Solutions

### Library Constraints

Babashka has specific limitations that affect BBPad's architecture:

```mermaid
graph TB
    subgraph "Babashka Constraints"
        A[Pre-selected Java Classes Only] --> B[No Runtime Class Addition]
        C[Interpretation Overhead] --> D[Slower Loops vs JVM]
        E[No deftype/definterface] --> F[Limited Protocol Support]
        G[~75MB Binary Limit] --> H[Selective Library Inclusion]
    end
    
    subgraph "BBPad Solutions"
        I[Custom Babashka Build] --> J[Feature Flags for JDBC]
        K[Babashka Pods] --> L[External Functionality]
        M[Built-in Libraries] --> N[Ring, Jetty, Cheshire]
        O[Source Loading] --> P[Compatible Clojure Libraries]
    end
    
    A --> I
    C --> K
    E --> M
    G --> O
    
    classDef constraint fill:#FFE5E5,stroke:#FF6B6B,stroke-width:2px
    classDef solution fill:#E5F5E5,stroke:#4CAF50,stroke-width:2px
    
    class A,B,C,D,E,F,G,H constraint
    class I,J,K,L,M,N,O,P solution
```

### Critical Libraries for BBPad

**Built-in Support Required:**
- ✅ Ring/Jetty (web server)
- ✅ Cheshire (JSON)  
- ✅ next.jdbc (database - via feature flag)
- ✅ Hiccup (HTML generation)
- ⚠️ ClojureScript compiler (may need pod)

**Pod-based Extensions:**
- Database drivers (PostgreSQL, MySQL via pods)
- Advanced visualization libraries
- File format processors (Excel, PDF)
- OAuth/authentication providers

### Custom Babashka Build Strategy

```mermaid
flowchart TD
    A[Standard Babashka] --> B{Sufficient for BBPad?}
    B -->|No| C[Custom Build Required]
    B -->|Yes| D[Use Standard Build]
    
    C --> E[Enable Feature Flags]
    E --> F[BABASHKA_FEATURE_JDBC=true]
    E --> G[BABASHKA_FEATURE_POSTGRESQL=true]
    E --> H[BABASHKA_FEATURE_HTTPKIT_SERVER=true]
    
    F --> I[Build with GraalVM]
    G --> I
    H --> I
    
    I --> J{Build Success?}
    J -->|No| K[Fallback to Pods]
    J -->|Yes| L[Custom BBPad Binary]
    
    K --> M[Standard BB + Pods]
    L --> N[Single Executable]
    M --> N
    
    classDef build fill:#FFE5B4,stroke:#FF8C00,stroke-width:2px
    classDef success fill:#E5F5E5,stroke:#4CAF50,stroke-width:2px
    classDef fallback fill:#E5E5FF,stroke:#4169E1,stroke-width:2px
    
    class A,C,E,F,G,H,I build
    class D,L,N success
    class K,M fallback
```

### Pod Integration Architecture

```mermaid
sequenceDiagram
    participant BBPad as BBPad Main
    participant PodReg as Pod Registry
    participant LocalPod as Local Pod
    participant Script as User Script
    
    BBPad->>PodReg: Check required pods in bb.edn
    PodReg->>BBPad: Download missing pods
    BBPad->>LocalPod: Load pod via babashka.pods
    LocalPod->>BBPad: Register namespaces & functions
    Script->>BBPad: Execute script requiring pod functions
    BBPad->>LocalPod: Call pod functions
    LocalPod->>BBPad: Return results
    BBPad->>Script: Display formatted results
```

### Updated Build Configuration

**bb.edn with Pod Support:**
```clojure
{:paths ["src"]
 :pods {org.babashka/postgresql {:version "0.1.0"}
        org.babashka/hsqldb {:version "0.1.0"}
        epiccastle/bbssh {:version "0.5.0"}}
 :deps {; Standard deps remain the same}}
```

**Environment Variables for Custom Build:**
```bash
export BABASHKA_FEATURE_JDBC=true
export BABASHKA_FEATURE_POSTGRESQL=true  
export BABASHKA_FEATURE_HTTPKIT_SERVER=true
export GRAALVM_HOME="/path/to/graalvm-ce-21.0.1"
export NATIVE_IMAGE_DEPRECATED_BUILDER_SANITATION=true
```

## 🚀 Deployment and Distribution

Multi-platform release pipeline with Babashka considerations:

```mermaid
flowchart TD
    A[Git Tag] --> B[GitHub Actions Trigger]
    
    subgraph "Build Matrix"
        B --> C[Windows Build]
        B --> D[macOS Build]  
        B --> E[Linux Build]
    end
    
    subgraph "Build Steps"
        C --> F[Install Dependencies]
        F --> G[Run Tests]
        G --> H[Build Executable]
        H --> I[Sign Binary]
        I --> J[Create Installer]
    end
    
    subgraph "Distribution"
        J --> K[GitHub Release]
        J --> L[Homebrew Formula]
        J --> M[Scoop Manifest]
        J --> N[AUR Package]
    end
    
    D --> F
    E --> F
    
    classDef build fill:#FF9800,stroke:#333,stroke-width:2px,color:#fff
    classDef dist fill:#4CAF50,stroke:#333,stroke-width:2px,color:#fff
    
    class A,B,C,D,E,F,G,H,I,J build
    class K,L,M,N dist
```

## 🔮 Future Architecture Considerations

Planned architectural evolution:

- **Plugin System**: Dynamic loading of Babashka libraries
- **Collaborative Features**: Real-time script sharing and editing
- **Cloud Integration**: Optional cloud storage and sync
- **Performance Monitoring**: Built-in script performance profiling
- **Advanced Security**: Script signing and verification system

---

This architecture provides a solid foundation for BBPad's current needs while maintaining flexibility for future enhancements.
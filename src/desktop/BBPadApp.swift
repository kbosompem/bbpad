import Cocoa
import WebKit
import Foundation
import Darwin

class BBPadApp: NSObject, NSApplicationDelegate {
    var window: NSWindow!
    var webView: WKWebView!
    var serverTask: Process?
    var serverPort: Int = 8080
    var isServerReady = false

    func applicationDidFinishLaunching(_ aNotification: Notification) {
        setupApplication()
        startBabashkaServer()
    }

    func applicationWillTerminate(_ aNotification: Notification) {
        terminateServer()
    }

    func applicationShouldTerminateAfterLastWindowClosed(_ sender: NSApplication) -> Bool {
        return true
    }

    private func setupApplication() {
        // Configure the main window
        let windowRect = NSRect(x: 0, y: 0, width: 1400, height: 900)
        window = NSWindow(
            contentRect: windowRect,
            styleMask: [.titled, .closable, .miniaturizable, .resizable],
            backing: .buffered,
            defer: false
        )

        window.title = "BBPad"
        window.center()
        window.minSize = NSSize(width: 800, height: 600)

        // Configure WebView
        let webConfiguration = WKWebViewConfiguration()
        webConfiguration.preferences.setValue(true, forKey: "developerExtrasEnabled")

        webView = WKWebView(frame: windowRect, configuration: webConfiguration)
        webView.navigationDelegate = self

        // Allow localhost connections
        if #available(macOS 10.15, *) {
            webView.configuration.preferences.setValue(true, forKey: "allowUniversalAccessFromFileURLs")
        }

        window.contentView = webView
        window.makeKeyAndOrderFront(nil)

        // Show loading page initially
        showLoadingPage()
    }

    private func showLoadingPage() {
        let loadingHTML = """
        <!DOCTYPE html>
        <html>
        <head>
            <title>BBPad - Starting...</title>
            <style>
                body {
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
                    display: flex;
                    justify-content: center;
                    align-items: center;
                    height: 100vh;
                    margin: 0;
                    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                    color: white;
                }
                .container {
                    text-align: center;
                    padding: 40px;
                    background: rgba(255,255,255,0.1);
                    border-radius: 20px;
                    backdrop-filter: blur(10px);
                }
                .logo {
                    font-size: 48px;
                    font-weight: bold;
                    margin-bottom: 20px;
                }
                .spinner {
                    width: 40px;
                    height: 40px;
                    border: 4px solid rgba(255,255,255,0.3);
                    border-top: 4px solid white;
                    border-radius: 50%;
                    animation: spin 1s linear infinite;
                    margin: 20px auto;
                }
                @keyframes spin {
                    0% { transform: rotate(0deg); }
                    100% { transform: rotate(360deg); }
                }
                .status {
                    margin-top: 20px;
                    font-size: 16px;
                    opacity: 0.9;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="logo">BBPad</div>
                <div class="spinner"></div>
                <div class="status">Starting Babashka server...</div>
            </div>
        </body>
        </html>
        """

        webView.loadHTMLString(loadingHTML, baseURL: nil)
    }

    private func startBabashkaServer() {
        guard let resourcePath = Bundle.main.resourcePath else {
            showErrorDialog("Failed to find application resources")
            return
        }

        let bbPath = "\(resourcePath)/bin/bb"
        let mainScript = "\(resourcePath)/src/bbpad/main.clj"

        // Check if files exist
        guard FileManager.default.fileExists(atPath: bbPath) else {
            showErrorDialog("Babashka binary not found at \(bbPath)")
            return
        }

        guard FileManager.default.fileExists(atPath: mainScript) else {
            showErrorDialog("Main script not found at \(mainScript)")
            return
        }

        // Find available port
        serverPort = findAvailablePort(starting: 8080)

        serverTask = Process()
        serverTask?.launchPath = bbPath
        serverTask?.arguments = [mainScript, "--port", "\(serverPort)"]
        serverTask?.currentDirectoryPath = resourcePath

        // Set environment variables
        var environment = ProcessInfo.processInfo.environment
        environment["BBPAD_APP_DIR"] = resourcePath
        environment["BBPAD_BUNDLED"] = "true"
        environment["BBPAD_DESKTOP"] = "true"
        serverTask?.environment = environment

        // Capture output
        let pipe = Pipe()
        serverTask?.standardOutput = pipe
        serverTask?.standardError = pipe

        // Monitor server output
        pipe.fileHandleForReading.readabilityHandler = { handle in
            let data = handle.availableData
            if !data.isEmpty {
                let output = String(data: data, encoding: .utf8) ?? ""
                DispatchQueue.main.async {
                    self.processServerOutput(output)
                }
            }
        }

        do {
            try serverTask?.run()

            // Give server time to start, then check if it's ready
            DispatchQueue.main.asyncAfter(deadline: .now() + 3.0) {
                self.checkServerStatus()
            }
        } catch {
            showErrorDialog("Failed to start Babashka server: \(error.localizedDescription)")
        }
    }

    private func processServerOutput(_ output: String) {
        print("Server output: \(output)")

        // Look for server ready indicators
        if output.contains("HTTP-Kit server running") || output.contains("Server started") {
            if !isServerReady {
                isServerReady = true
                loadApplication()
            }
        }
    }

    private func checkServerStatus() {
        let url = URL(string: "http://localhost:\(serverPort)/api/health")!

        URLSession.shared.dataTask(with: url) { data, response, error in
            DispatchQueue.main.async {
                if let httpResponse = response as? HTTPURLResponse,
                   httpResponse.statusCode == 200 {
                    if !self.isServerReady {
                        self.isServerReady = true
                        self.loadApplication()
                    }
                } else {
                    // Retry after delay
                    DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
                        self.checkServerStatus()
                    }
                }
            }
        }.resume()
    }

    private func loadApplication() {
        let appURL = URL(string: "http://localhost:\(serverPort)")!
        let request = URLRequest(url: appURL)
        webView.load(request)
    }

    private func terminateServer() {
        serverTask?.terminate()
        serverTask?.waitUntilExit()
        serverTask = nil
    }

    private func findAvailablePort(starting: Int) -> Int {
        for port in starting...(starting + 100) {
            if isPortAvailable(port: port) {
                return port
            }
        }
        return starting // fallback
    }

    private func isPortAvailable(port: Int) -> Bool {
        let socket = socket(AF_INET, SOCK_STREAM, 0)
        defer { close(socket) }

        var addr = sockaddr_in()
        addr.sin_family = sa_family_t(AF_INET)
        addr.sin_port = in_port_t(port).bigEndian
        addr.sin_addr.s_addr = inet_addr("127.0.0.1")

        let result = withUnsafePointer(to: &addr) {
            $0.withMemoryRebound(to: sockaddr.self, capacity: 1) {
                Darwin.bind(socket, $0, socklen_t(MemoryLayout<sockaddr_in>.size))
            }
        }

        return result == 0
    }

    private func showErrorDialog(_ message: String) {
        let alert = NSAlert()
        alert.messageText = "BBPad Error"
        alert.informativeText = message
        alert.alertStyle = .critical
        alert.addButton(withTitle: "OK")
        alert.runModal()
        NSApp.terminate(nil)
    }
}

extension BBPadApp: WKNavigationDelegate {
    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        // Update window title when page loads
        webView.evaluateJavaScript("document.title") { result, error in
            if let title = result as? String, !title.isEmpty {
                DispatchQueue.main.async {
                    self.window.title = "BBPad - \(title)"
                }
            }
        }
    }

    func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
        if !isServerReady {
            // Server might still be starting, retry
            DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
                self.checkServerStatus()
            }
        }
    }

    func webView(_ webView: WKWebView, decidePolicyFor navigationAction: WKNavigationAction, decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
        // Allow localhost navigation
        if let url = navigationAction.request.url {
            if url.host == "localhost" || url.host == "127.0.0.1" {
                decisionHandler(.allow)
                return
            }

            // Open external URLs in default browser
            if url.scheme == "http" || url.scheme == "https" {
                NSWorkspace.shared.open(url)
                decisionHandler(.cancel)
                return
            }
        }

        decisionHandler(.allow)
    }
}

// Main entry point
let app = NSApplication.shared
let delegate = BBPadApp()
app.delegate = delegate
app.run()
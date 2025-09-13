#!/usr/bin/swift

import WebKit
import Cocoa

class WebViewController: NSViewController, WKNavigationDelegate {
    var webView: WKWebView!

    override func loadView() {
        webView = WKWebView()
        webView.navigationDelegate = self
        view = webView
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        let urlString = CommandLine.arguments.count > 1 ? CommandLine.arguments[1] : "http://localhost:8080"
        if let url = URL(string: urlString) {
            let request = URLRequest(url: url)
            webView.load(request)
        }
    }
}

class AppDelegate: NSObject, NSApplicationDelegate {
    var window: NSWindow!

    func applicationDidFinishLaunching(_ aNotification: Notification) {
        // Create window
        let width = CommandLine.arguments.count > 2 ? Int(CommandLine.arguments[2]) ?? 1200 : 1200
        let height = CommandLine.arguments.count > 3 ? Int(CommandLine.arguments[3]) ?? 800 : 800

        window = NSWindow(
            contentRect: NSRect(x: 100, y: 100, width: width, height: height),
            styleMask: [.titled, .closable, .miniaturizable, .resizable],
            backing: .buffered,
            defer: false
        )

        window.title = "BBPad"
        window.center()

        // Create WebView
        let viewController = WebViewController()
        window.contentViewController = viewController
        window.makeKeyAndOrderFront(nil)

        // Bring app to front
        NSApp.activate(ignoringOtherApps: true)
    }

    func applicationShouldTerminateAfterLastWindowClosed(_ sender: NSApplication) -> Bool {
        return true
    }
}

// Create and run the app
let app = NSApplication.shared
let delegate = AppDelegate()
app.delegate = delegate
app.run()
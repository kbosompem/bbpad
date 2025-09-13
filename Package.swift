// swift-tools-version:5.7
import PackageDescription

let package = Package(
    name: "BBPad",
    platforms: [
        .macOS(.v10_15)
    ],
    products: [
        .executable(name: "BBPadApp", targets: ["BBPadApp"])
    ],
    targets: [
        .executableTarget(
            name: "BBPadApp",
            path: "src/desktop"
        )
    ]
)

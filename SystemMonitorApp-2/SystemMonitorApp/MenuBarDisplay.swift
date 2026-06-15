import SwiftUI
import AppKit

enum MenuBarDisplayMode: String, CaseIterable {
    case iconOnly = "Icon Only"
    case cpu = "CPU Only"
    case cpuPercentage = "CPU %"
    case cpuDetailed = "CPU Detailed"
    case network = "Network Only"
    case networkSpeed = "Network Speed"
    case networkPercentage = "Network %"
    case networkDetailed = "Network Detailed"
    case cpuNetwork = "CPU + Network"
    case cpuNetworkPercentage = "CPU + Network %"
    case cpuNetworkSpeed = "CPU + Network Speed"
    case cpuNetworkAll = "CPU + Network All"
    case cpuNetworkDetailed = "CPU + Network Detailed"
    case temperature = "Temperature"
    case battery = "Battery"
    case batteryDetailed = "Battery Detailed"
    case memory = "Memory"
    case disk = "Disk"
    case rotation = "Rotation Mode"
    case smart = "Smart Mode"
}

enum NetworkUnit: String, CaseIterable {
    case auto = "Auto"
    case kbps = "KB/s"
    case mbps = "MB/s"
    case mbits = "Mb/s"
    case gbps = "GB/s"
    case gbits = "Gb/s"
}

class MenuBarDisplay: ObservableObject {
    @Published var displayMode: MenuBarDisplayMode = .smart
    @Published var networkUnit: NetworkUnit = .auto
    @Published var rotationInterval: Double = 3.0
    @Published var showIcon: Bool = true
    @Published var showPercentage: Bool = true
    @Published var showNetworkPercentage: Bool = true
    @Published var enableSpeedTest: Bool = false
    @Published var showDownloadUpload: Bool = true
    @Published var showDetailed: Bool = false
    @Published var compactMode: Bool = false
    @Published var animationSpeed: Double = 1.0
    
    private var rotationTimer: Timer?
    private var currentIndex = 0
    private let rotationModes: [MenuBarDisplayMode] = [.cpu, .network, .temperature, .battery, .memory]
    
    init() {
        loadSettings()
        startRotationIfNeeded()
    }
    
    deinit {
        stopRotation()
    }
    
    func loadSettings() {
        if let savedMode = UserDefaults.standard.string(forKey: "menuBarDisplayMode"),
           let mode = MenuBarDisplayMode(rawValue: savedMode) {
            displayMode = mode
        }
        
        if let savedUnit = UserDefaults.standard.string(forKey: "networkUnit"),
           let unit = NetworkUnit(rawValue: savedUnit) {
            networkUnit = unit
        }
        
        rotationInterval = UserDefaults.standard.double(forKey: "rotationInterval")
        if rotationInterval == 0 { rotationInterval = 3.0 }
        
        showIcon = UserDefaults.standard.bool(forKey: "showIcon")
        showPercentage = UserDefaults.standard.bool(forKey: "showPercentage")
        showNetworkPercentage = UserDefaults.standard.bool(forKey: "showNetworkPercentage")
        enableSpeedTest = UserDefaults.standard.bool(forKey: "enableSpeedTest")
        showDownloadUpload = UserDefaults.standard.bool(forKey: "showDownloadUpload")
        showDetailed = UserDefaults.standard.bool(forKey: "showDetailed")
        compactMode = UserDefaults.standard.bool(forKey: "compactMode")
        animationSpeed = UserDefaults.standard.double(forKey: "animationSpeed")
        if animationSpeed == 0 { animationSpeed = 1.0 }
    }
    
    func saveSettings() {
        UserDefaults.standard.set(displayMode.rawValue, forKey: "menuBarDisplayMode")
        UserDefaults.standard.set(networkUnit.rawValue, forKey: "networkUnit")
        UserDefaults.standard.set(rotationInterval, forKey: "rotationInterval")
        UserDefaults.standard.set(showIcon, forKey: "showIcon")
        UserDefaults.standard.set(showPercentage, forKey: "showPercentage")
        UserDefaults.standard.set(showNetworkPercentage, forKey: "showNetworkPercentage")
        UserDefaults.standard.set(enableSpeedTest, forKey: "enableSpeedTest")
        UserDefaults.standard.set(showDownloadUpload, forKey: "showDownloadUpload")
        UserDefaults.standard.set(showDetailed, forKey: "showDetailed")
        UserDefaults.standard.set(compactMode, forKey: "compactMode")
        UserDefaults.standard.set(animationSpeed, forKey: "animationSpeed")
    }
    
    func startRotationIfNeeded() {
        stopRotation()
        
        if displayMode == .rotation {
            rotationTimer = Timer.scheduledTimer(withTimeInterval: rotationInterval, repeats: true) { [weak self] _ in
                self?.rotateDisplay()
            }
        }
    }
    
    func stopRotation() {
        rotationTimer?.invalidate()
        rotationTimer = nil
    }
    
    private func rotateDisplay() {
        currentIndex = (currentIndex + 1) % rotationModes.count
    }
    
    func getCurrentRotationMode() -> MenuBarDisplayMode {
        return rotationModes[currentIndex]
    }
    
    func getDisplayText(cpu: Double, networkDown: Double, networkUp: Double, networkUsage: Double, temp: Double, battery: Double, memory: Double, disk: Double) -> String {
        switch displayMode {
        case .iconOnly:
            return ""
        case .cpu:
            return showPercentage ? "\(Int(cpu))%" : ""
        case .cpuPercentage:
            return "\(Int(cpu))%"
        case .cpuDetailed:
            if showDetailed {
                return "CPU: \(Int(cpu))%"
            } else {
                return "\(Int(cpu))%"
            }
        case .network:
            let total = networkDown + networkUp
            return formatSpeed(total)
        case .networkSpeed:
            let total = networkDown + networkUp
            return formatSpeed(total)
        case .networkPercentage:
            return showNetworkPercentage ? "\(Int(networkUsage))%" : ""
        case .networkDetailed:
            if showDetailed {
                return "NET: \(Int(networkUsage))%"
            } else {
                return "\(Int(networkUsage))%"
            }
        case .cpuNetwork:
            return "\(Int(cpu))% • \(formatSpeed(networkDown + networkUp))"
        case .cpuNetworkPercentage:
            return "\(Int(cpu))% • \(Int(networkUsage))%"
        case .cpuNetworkSpeed:
            return "\(Int(cpu))% • \(formatSpeed(networkDown + networkUp))"
        case .cpuNetworkAll:
            if showDownloadUpload {
                return "\(Int(cpu))% • \(Int(networkUsage))% • \(formatSpeed(networkDown + networkUp))"
            } else {
                return "\(Int(cpu))% • \(Int(networkUsage))%"
            }
        case .cpuNetworkDetailed:
            if showDetailed {
                return "CPU: \(Int(cpu))% • NET: \(Int(networkUsage))%"
            } else {
                return "\(Int(cpu))% • \(Int(networkUsage))%"
            }
        case .temperature:
            return "\(Int(temp))°C"
        case .battery:
            return "\(Int(battery))%"
        case .batteryDetailed:
            if showDetailed {
                return "BAT: \(Int(battery))%"
            } else {
                return "\(Int(battery))%"
            }
        case .memory:
            return "\(Int(memory))%"
        case .disk:
            return "\(Int(disk))%"
        case .rotation:
            let mode = getCurrentRotationMode()
            switch mode {
            case .cpu:
                return showPercentage ? "\(Int(cpu))%" : ""
            case .network:
                let total = networkDown + networkUp
                return formatSpeed(total)
            case .temperature:
                return "\(Int(temp))°C"
            case .battery:
                return "\(Int(battery))%"
            case .memory:
                return "\(Int(memory))%"
            default:
                return ""
            }
        case .smart:
            // Smart mode: shows most relevant info based on system state
            if cpu > 80 {
                return "\(Int(cpu))% CPU"
            } else if networkUsage > 50 {
                return "\(Int(networkUsage))% NET"
            } else if battery < 20 {
                return "\(Int(battery))% BAT"
            } else if temp > 70 {
                return "\(Int(temp))°C"
            } else {
                return "\(Int(cpu))% • \(Int(networkUsage))%"
            }
        }
    }
    
    private func formatSpeed(_ mbps: Double) -> String {
        let bytesPerSecond = mbps * 1_000_000 / 8
        
        switch networkUnit {
        case .kbps:
            let kbps = bytesPerSecond / 1024
            return String(format: "%.0f KB/s", kbps)
        case .mbps:
            let mbps = bytesPerSecond / 1_048_576
            return String(format: "%.1f MB/s", mbps)
        case .mbits:
            let mbits = mbps
            return String(format: "%.1f Mb/s", mbits)
        case .gbps:
            let gbps = bytesPerSecond / 1_073_741_824
            return String(format: "%.2f GB/s", gbps)
        case .gbits:
            let gbits = mbps / 1000
            return String(format: "%.2f Gb/s", gbits)
        case .auto:
            if mbps >= 1000 {
                let gbits = mbps / 1000
                return String(format: "%.1f Gb/s", gbits)
            } else if mbps >= 1 {
                let mbits = mbps
                return String(format: "%.1f Mb/s", mbits)
            } else {
                let kbps = mbps * 1000
                return String(format: "%.0f Kb/s", kbps)
            }
        }
    }
}

extension MenuBarDisplayMode {
    var icon: String {
        switch self {
        case .iconOnly: return "circle.fill"
        case .cpu, .cpuPercentage, .cpuDetailed: return "cpu.fill"
        case .network, .networkSpeed, .networkPercentage, .networkDetailed: return "network"
        case .cpuNetwork, .cpuNetworkPercentage, .cpuNetworkSpeed, .cpuNetworkAll, .cpuNetworkDetailed: return "cpu.fill"
        case .temperature: return "thermometer"
        case .battery, .batteryDetailed: return "battery.100"
        case .memory: return "memorychip.fill"
        case .disk: return "internaldrive.fill"
        case .rotation: return "arrow.triangle.2.circlepath"
        case .smart: return "sparkles"
        }
    }
}

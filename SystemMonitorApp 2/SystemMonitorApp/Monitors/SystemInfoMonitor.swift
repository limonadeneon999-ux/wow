import Foundation
import IOKit
import AppKit

class SystemInfoMonitor: ObservableObject {
    @Published var systemName: String = ""
    @Published var systemVersion: String = ""
    @Published var kernelVersion: String = ""
    @Published var hostname: String = ""
    @Published var uptime: String = ""
    @Published var model: String = ""
    @Published var processor: String = ""
    @Published var ram: String = ""
    @Published var serialNumber: String = ""
    @Published var macAddress: String = ""
    
    private var updateTimer: Timer?
    
    init() {
        loadSystemInfo()
        startMonitoring()
    }
    
    deinit {
        stopMonitoring()
    }
    
    private func startMonitoring() {
        updateTimer = Timer.scheduledTimer(withTimeInterval: 5.0, repeats: true) { [weak self] _ in
            self?.updateUptime()
        }
    }
    
    private func stopMonitoring() {
        updateTimer?.invalidate()
        updateTimer = nil
    }
    
    private func loadSystemInfo() {
        let processInfo = ProcessInfo.processInfo
        
        systemName = "macOS"
        systemVersion = processInfo.operatingSystemVersionString
        kernelVersion = getKernelVersion()
        hostname = processInfo.hostName
        model = getModelIdentifier()
        processor = getProcessorInfo()
        ram = getRAMInfo()
        serialNumber = getSerialNumber()
        macAddress = getMACAddress()
        updateUptime()
    }
    
    private func updateUptime() {
        let uptime = ProcessInfo.processInfo.systemUptime
        let days = Int(uptime) / 86400
        let hours = (Int(uptime) % 86400) / 3600
        let minutes = (Int(uptime) % 3600) / 60
        
        if days > 0 {
            self.uptime = "\(days)d \(hours)h \(minutes)m"
        } else if hours > 0 {
            self.uptime = "\(hours)h \(minutes)m"
        } else {
            self.uptime = "\(minutes)m"
        }
    }
    
    private func getKernelVersion() -> String {
        var mib: [Int32] = [CTL_KERN, KERN_VERSION]
        var size = 0
        sysctl(&mib, 2, nil, &size, nil, 0)
        
        var kernel = [CChar](repeating: 0, count: size)
        sysctl(&mib, 2, &kernel, &size, nil, 0)
        
        return String(cString: kernel)
    }
    
    private func getModelIdentifier() -> String {
        var size: Int = 0
        sysctlbyname("hw.model", nil, &size, nil, 0)
        
        var model = [CChar](repeating: 0, count: size)
        sysctlbyname("hw.model", &model, &size, nil, 0)
        
        return String(cString: model)
    }
    
    private func getProcessorInfo() -> String {
        var size: Int = 0
        sysctlbyname("machdep.cpu.brand_string", nil, &size, nil, 0)
        
        var cpu = [CChar](repeating: 0, count: size)
        sysctlbyname("machdep.cpu.brand_string", &cpu, &size, nil, 0)
        
        return String(cString: cpu)
    }
    
    private func getRAMInfo() -> String {
        let totalMemory = ProcessInfo.processInfo.physicalMemory
        let gb = totalMemory / 1_073_741_824
        return "\(gb) GB"
    }
    
    private func getSerialNumber() -> String {
        var size: Int = 0
        sysctlbyname("hw.serialno", nil, &size, nil, 0)
        
        var serial = [CChar](repeating: 0, count: size)
        sysctlbyname("hw.serialno", &serial, &size, nil, 0)
        
        return String(cString: serial)
    }
    
    private func getMACAddress() -> String {
        var ifaddr: UnsafeMutablePointer<ifaddrs>?
        guard getifaddrs(&ifaddr) == 0 else { return "Unknown" }
        
        var ptr = ifaddr
        while ptr != nil {
            let interface = String(cString: ptr!.pointee.ifa_name)
            
            if interface.hasPrefix("en") {
                let addr = ptr!.pointee.ifa_addr.pointee
                if addr.sa_family == UInt8(AF_LINK) {
                    let data = Data(bytes: ptr!.pointee.ifa_addr, count: Int(ptr!.pointee.ifa_addr.pointee.sa_len))
                    let macAddress = data.suffix(from: 6).map { String(format: "%02x", $0) }.joined(separator: ":")
                    freeifaddrs(ifaddr)
                    return macAddress.uppercased()
                }
            }
            
            ptr = ptr!.pointee.ifa_next
        }
        
        freeifaddrs(ifaddr)
        return "Unknown"
    }
}

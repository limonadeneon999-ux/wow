import Foundation
import Network

class SpeedTestMonitor: ObservableObject {
    @Published var downloadSpeed: Double = 0
    @Published var uploadSpeed: Double = 0
    @Published var isRunning: Bool = false
    @Published var testDuration: Int = 0
    @Published var latency: Double = 0
    @Published var jitter: Double = 0
    
    private var speedTestTimer: Timer?
    private var updateTimer: Timer?
    private var previousDownload: UInt64 = 0
    private var previousUpload: UInt64 = 0
    private var startTime: Date?
    private var latencyMeasurements: [Double] = []
    
    init() {
        startMonitoring()
    }
    
    deinit {
        stopMonitoring()
    }
    
    func startSpeedTest() {
        guard !isRunning else { return }
        
        startTime = Date()
        previousDownload = 0
        previousUpload = 0
        latencyMeasurements = []
        
        // Start lightweight monitoring
        updateTimer = Timer.scheduledTimer(withTimeInterval: 0.5, repeats: true) { [weak self] _ in
            self?.updateSpeedTest()
        }
        
        DispatchQueue.main.async {
            self.isRunning = true
        }
    }
    
    func stopSpeedTest() {
        updateTimer?.invalidate()
        updateTimer = nil
        startTime = nil
        
        DispatchQueue.main.async {
            self.isRunning = false
            self.downloadSpeed = 0
            self.uploadSpeed = 0
            self.testDuration = 0
            self.latency = 0
            self.jitter = 0
        }
    }
    
    private func startMonitoring() {
        // Lightweight continuous monitoring
        updateTimer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { [weak self] _ in
            self?.updateSpeedTest()
        }
    }
    
    private func stopMonitoring() {
        updateTimer?.invalidate()
        updateTimer = nil
    }
    
    private func updateSpeedTest() {
        // Measure latency
        let latencyStart = Date()
        if let stats = getNetworkStats() {
            let latency = Date().timeIntervalSince(latencyStart) * 1000
            latencyMeasurements.append(latency)
            if latencyMeasurements.count > 10 {
                latencyMeasurements.removeFirst()
            }
            
            // Calculate jitter
            if latencyMeasurements.count > 1 {
                let avgLatency = latencyMeasurements.reduce(0, +) / Double(latencyMeasurements.count)
                let variance = latencyMeasurements.map { pow($0 - avgLatency, 2) }.reduce(0, +) / Double(latencyMeasurements.count)
                DispatchQueue.main.async {
                    self.latency = avgLatency
                    self.jitter = sqrt(variance)
                }
            }
            
            let currentDownload = stats.download
            let currentUpload = stats.upload
            
            if previousDownload > 0 {
                let downloadDiff = currentDownload - previousDownload
                let downloadBytesPerSecond = Double(downloadDiff)
                let downloadMbps = downloadBytesPerSecond * 8 / 1_000_000
                
                DispatchQueue.main.async {
                    self.downloadSpeed = downloadMbps
                }
            }
            
            if previousUpload > 0 {
                let uploadDiff = currentUpload - previousUpload
                let uploadBytesPerSecond = Double(uploadDiff)
                let uploadMbps = uploadBytesPerSecond * 8 / 1_000_000
                
                DispatchQueue.main.async {
                    self.uploadSpeed = uploadMbps
                }
            }
            
            previousDownload = currentDownload
            previousUpload = currentUpload
            
            if let start = startTime {
                let elapsed = Int(Date().timeIntervalSince(start))
                DispatchQueue.main.async {
                    self.testDuration = elapsed
                }
            }
        }
    }
    
    private func getNetworkStats() -> (download: UInt64, upload: UInt64)? {
        var totalDownload: UInt64 = 0
        var totalUpload: UInt64 = 0
        
        let interfaces = getNetworkInterfaces()
        
        for interface in interfaces {
            if let stats = getInterfaceStats(interface: interface) {
                totalDownload += stats.download
                totalUpload += stats.upload
            }
        }
        
        return (download: totalDownload, upload: totalUpload)
    }
    
    private func getNetworkInterfaces() -> [String] {
        var interfaces: [String] = []
        
        var ifaddr: UnsafeMutablePointer<ifaddrs>?
        guard getifaddrs(&ifaddr) == 0 else { return interfaces }
        
        var ptr = ifaddr
        while ptr != nil {
            let interface = String(cString: ptr!.pointee.ifa_name)
            
            if interface != "lo0" && (interface.hasPrefix("en") || interface.hasPrefix("awdl")) {
                interfaces.append(interface)
            }
            
            ptr = ptr!.pointee.ifa_next
        }
        
        freeifaddrs(ifaddr)
        return interfaces
    }
    
    private func getInterfaceStats(interface: String) -> (download: UInt64, upload: UInt64)? {
        var mib: [Int32] = [
            CTL_NET,
            PF_ROUTE,
            0,
            AF_LINK,
            NET_RT_IFLIST,
            if_nametoindex(interface)
        ]
        
        var len = 0
        sysctl(&mib, u_int(mib.count), nil, &len, nil, 0)
        
        var buf = [UInt8](repeating: 0, count: len)
        sysctl(&mib, u_int(mib.count), &buf, &len, nil, 0)
        
        var ifmsghdr = buf.withUnsafeMemoryBinding(to: if_msghdr.self) { $0 }
        
        let ptr = UnsafeRawPointer(ifmsghdr)
        let if_data = ptr.load(as: if_data.self)
        
        return (
            download: UInt64(if_data.ifi_ibytes),
            upload: UInt64(if_data.ifi_obytes)
        )
    }
}

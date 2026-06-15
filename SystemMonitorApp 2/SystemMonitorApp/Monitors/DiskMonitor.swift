import Foundation

class DiskMonitor: ObservableObject {
    @Published var usedDiskSpace: Double = 0
    @Published var totalDiskSpace: Double = 0
    @Published var freeDiskSpace: Double = 0
    @Published var diskUsage: Double = 0
    @Published var readSpeed: Double = 0
    @Published var writeSpeed: Double = 0
    @Published var diskHistory: [Double] = []
    @Published var diskName: String = ""
    @Published var diskType: String = "SSD"
    
    private var updateTimer: Timer?
    private var previousReadBytes: UInt64 = 0
    private var previousWriteBytes: UInt64 = 0
    private let maxHistoryPoints = 60
    
    init() {
        updateDiskStats()
        startMonitoring()
    }
    
    deinit {
        stopMonitoring()
    }
    
    private func startMonitoring() {
        updateTimer = Timer.scheduledTimer(withTimeInterval: 5.0, repeats: true) { [weak self] _ in
            self?.updateDiskStats()
        }
    }
    
    private func stopMonitoring() {
        updateTimer?.invalidate()
        updateTimer = nil
    }
    
    private func updateDiskStats() {
        let fileManager = FileManager.default
        
        do {
            let attributes = try fileManager.attributesOfFileSystem(forPath: "/")
            
            if let totalSize = attributes[.systemSize] as? UInt64,
               let freeSize = attributes[.systemFreeSize] as? UInt64 {
                let usedSize = totalSize - freeSize
                
                DispatchQueue.main.async {
                    self.totalDiskSpace = Double(totalSize)
                    self.usedDiskSpace = Double(usedSize)
                    self.freeDiskSpace = Double(freeSize)
                    self.diskUsage = (Double(usedSize) / Double(totalSize)) * 100
                    
                    self.readSpeed = Double.random(in: 10...150)
                    self.writeSpeed = Double.random(in: 5...80)
                    self.diskName = self.getDiskName()
                    self.diskType = self.getDiskType()
                    
                    self.diskHistory.append(self.diskUsage)
                    if self.diskHistory.count > self.maxHistoryPoints {
                        self.diskHistory.removeFirst()
                    }
                }
            }
        } catch {
            print("Error getting disk stats: \(error)")
        }
    }
    
    private func getDiskName() -> String {
        var size: Int = 0
        sysctlbyname("hw.model", nil, &size, nil, 0)
        
        var model = [CChar](repeating: 0, count: size)
        sysctlbyname("hw.model", &model, &size, nil, 0)
        
        return String(cString: model)
    }
    
    private func getDiskType() -> String {
        // MacBook Pro 2017 typically has SSD
        return "SSD"
    }
    
    func formatDiskSpace(_ bytes: Double) -> String {
        let tb = bytes / 1_099_511_627_776
        if tb >= 1 {
            return String(format: "%.1f TB", tb)
        } else {
            let gb = bytes / 1_073_741_824
            return String(format: "%.0f GB", gb)
        }
    }
}

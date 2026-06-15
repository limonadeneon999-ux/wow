import Foundation

class MemoryMonitor: ObservableObject {
    @Published var usedMemory: Double = 0
    @Published var totalMemory: Double = 0
    @Published var memoryUsage: Double = 0
    @Published var freeMemory: Double = 0
    @Published var cacheMemory: Double = 0
    @Published var compressedMemory: Double = 0
    @Published var swapUsed: Double = 0
    @Published var swapTotal: Double = 0
    @Published var memoryPressure: String = "Normal"
    @Published var memoryHistory: [Double] = []
    
    private var updateTimer: Timer?
    private let maxHistoryPoints = 60
    
    init() {
        updateMemoryStats()
        startMonitoring()
    }
    
    deinit {
        stopMonitoring()
    }
    
    private func startMonitoring() {
        updateTimer = Timer.scheduledTimer(withTimeInterval: 2.0, repeats: true) { [weak self] _ in
            self?.updateMemoryStats()
        }
    }
    
    private func stopMonitoring() {
        updateTimer?.invalidate()
        updateTimer = nil
    }
    
    private func updateMemoryStats() {
        var stats = vm_statistics64()
        var count = mach_msg_type_number_t(MemoryLayout<vm_statistics64>.size / MemoryLayout<integer_t>.size)
        
        let result = withUnsafeMutablePointer(to: &stats) {
            $0.withMemoryRebound(to: integer_t.self, capacity: count) {
                host_statistics64(mach_host_self(), HOST_VM_INFO64, $0, &count)
            }
        }
        
        if result == KERN_SUCCESS {
            let pageSize = vm_kernel_page_size
            let totalMemory = ProcessInfo.processInfo.physicalMemory
            
            let free = UInt64(stats.free_count) * UInt64(pageSize)
            let active = UInt64(stats.active_count) * UInt64(pageSize)
            let inactive = UInt64(stats.inactive_count) * UInt64(pageSize)
            let wired = UInt64(stats.wire_count) * UInt64(pageSize)
            let compressed = UInt64(stats.compressor_page_count) * UInt64(pageSize)
            
            let used = active + inactive + wired + compressed
            
            var xswUsage = xsw_usage()
            var swapCount = mach_msg_type_number_t(MemoryLayout<xsw_usage>.size / MemoryLayout<integer_t>.size)
            host_swap_info64(mach_host_self_, &xswUsage, &swapCount)
            
            DispatchQueue.main.async {
                self.totalMemory = Double(totalMemory)
                self.usedMemory = Double(used)
                self.freeMemory = Double(free)
                self.cacheMemory = Double(inactive)
                self.compressedMemory = Double(compressed)
                self.swapUsed = Double(xswUsage.xsu_used)
                self.swapTotal = Double(xswUsage.xsu_total)
                self.memoryUsage = (Double(used) / Double(totalMemory)) * 100
                self.memoryPressure = self.getMemoryPressure(self.memoryUsage)
                
                self.memoryHistory.append(self.memoryUsage)
                if self.memoryHistory.count > self.maxHistoryPoints {
                    self.memoryHistory.removeFirst()
                }
            }
        }
    }
    
    private func getMemoryPressure(_ usage: Double) -> String {
        if usage > 90 {
            return "Critical"
        } else if usage > 75 {
            return "High"
        } else if usage > 50 {
            return "Moderate"
        } else {
            return "Normal"
        }
    }
    
    func formatMemory(_ bytes: Double) -> String {
        let gb = bytes / 1_073_741_824
        if gb >= 1 {
            return String(format: "%.1f GB", gb)
        } else {
            let mb = bytes / 1_048_576
            return String(format: "%.0f MB", mb)
        }
    }
}

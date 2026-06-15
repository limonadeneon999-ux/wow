import Foundation
import IOKit.ps

class CPUMonitor: ObservableObject {
    @Published var cpuUsage: Double = 0
    @Published var cpuCores: Int = 0
    @Published var cpuThreads: Int = 0
    @Published var historicalData: [Double] = []
    @Published var perCoreUsage: [Double] = []
    @Published var cpuFrequency: Double = 0
    @Published var thermalState: String = "Normal"
    
    private var previousCPUInfo: processor_info_array_t?
    private var previousNumCPUInfo: mach_msg_type_number_t = 0
    private var previousNumCPUs: natural_t = 0
    private var updateTimer: Timer?
    
    private let maxHistoryPoints = 60
    
    init() {
        getCPUInfo()
        startMonitoring()
    }
    
    deinit {
        stopMonitoring()
    }
    
    private func getCPUInfo() {
        var numCPUs: natural_t = 0
        var numCPUInfo: mach_msg_type_number_t = 0
        var cpuInfo: processor_info_array_t?
        
        let result = host_processor_info(mach_host_self(), PROCESSOR_CPU_LOAD_INFO, &numCPUs, &cpuInfo, &numCPUInfo)
        
        if result == KERN_SUCCESS {
            cpuCores = Int(numCPUs)
            cpuThreads = Int(ProcessInfo.processInfo.processorCount)
            cpuFrequency = getCPUFrequency()
            
            previousCPUInfo = cpuInfo
            previousNumCPUInfo = numCPUInfo
            previousNumCPUs = numCPUs
            
            perCoreUsage = Array(repeating: 0.0, count: cpuCores)
        }
    }
    
    private func startMonitoring() {
        updateTimer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { [weak self] _ in
            self?.updateCPUUsage()
        }
    }
    
    private func stopMonitoring() {
        updateTimer?.invalidate()
        updateTimer = nil
    }
    
    private func updateCPUUsage() {
        var numCPUs: natural_t = 0
        var numCPUInfo: mach_msg_type_number_t = 0
        var cpuInfo: processor_info_array_t?
        
        let result = host_processor_info(mach_host_self(), PROCESSOR_CPU_LOAD_INFO, &numCPUs, &cpuInfo, &numCPUInfo)
        
        if result == KERN_SUCCESS, let previousInfo = previousCPUInfo {
            var totalUser: UInt32 = 0
            var totalSystem: UInt32 = 0
            var totalIdle: UInt32 = 0
            var totalNice: UInt32 = 0
            
            var previousUser: UInt32 = 0
            var previousSystem: UInt32 = 0
            var previousIdle: UInt32 = 0
            var previousNice: UInt32 = 0
            
            var coreUsages: [Double] = []
            
            for i in 0..<Int(numCPUs) {
                let base = i * CPU_STATE_MAX
                
                let userDiff = cpuInfo![base + CPU_STATE_USER] - previousInfo[base + CPU_STATE_USER]
                let systemDiff = cpuInfo![base + CPU_STATE_SYSTEM] - previousInfo[base + CPU_STATE_SYSTEM]
                let idleDiff = cpuInfo![base + CPU_STATE_IDLE] - previousInfo[base + CPU_STATE_IDLE]
                let niceDiff = cpuInfo![base + CPU_STATE_NICE] - previousInfo[base + CPU_STATE_NICE]
                
                let coreTotal = userDiff + systemDiff + idleDiff + niceDiff
                if coreTotal > 0 {
                    let coreUsage = Double(userDiff + systemDiff + niceDiff) / Double(coreTotal) * 100
                    coreUsages.append(coreUsage)
                }
                
                totalUser += cpuInfo![base + CPU_STATE_USER]
                totalSystem += cpuInfo![base + CPU_STATE_SYSTEM]
                totalIdle += cpuInfo![base + CPU_STATE_IDLE]
                totalNice += cpuInfo![base + CPU_STATE_NICE]
                
                previousUser += previousInfo[base + CPU_STATE_USER]
                previousSystem += previousInfo[base + CPU_STATE_SYSTEM]
                previousIdle += previousInfo[base + CPU_STATE_IDLE]
                previousNice += previousInfo[base + CPU_STATE_NICE]
            }
            
            let userDiff = totalUser - previousUser
            let systemDiff = totalSystem - previousSystem
            let idleDiff = totalIdle - previousIdle
            let niceDiff = totalNice - previousNice
            
            let totalDiff = userDiff + systemDiff + idleDiff + niceDiff
            
            if totalDiff > 0 {
                let usage = Double(userDiff + systemDiff + niceDiff) / Double(totalDiff) * 100
                DispatchQueue.main.async {
                    self.cpuUsage = usage
                    self.perCoreUsage = coreUsages
                    self.thermalState = self.getThermalState(usage)
                    
                    self.historicalData.append(usage)
                    if self.historicalData.count > self.maxHistoryPoints {
                        self.historicalData.removeFirst()
                    }
                    
                    NotificationCenter.default.post(name: NSNotification.Name("CPUUpdate"), object: nil)
                }
            }
            
            vm_deallocate(mach_task_self_, vm_address_t(bitPattern: previousInfo), vm_size_t(previousNumCPUInfo * MemoryLayout<integer_t>.size))
            
            previousCPUInfo = cpuInfo
            previousNumCPUInfo = numCPUInfo
            previousNumCPUs = numCPUs
        }
    }
    
    private func getCPUFrequency() -> Double {
        var size: Int = 0
        sysctlbyname("hw.cpufrequency", nil, &size, nil, 0)
        
        var frequency: UInt64 = 0
        sysctlbyname("hw.cpufrequency", &frequency, &size, nil, 0)
        
        return Double(frequency) / 1_000_000_000 // Convert to GHz
    }
    
    private func getThermalState(_ usage: Double) -> String {
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
}

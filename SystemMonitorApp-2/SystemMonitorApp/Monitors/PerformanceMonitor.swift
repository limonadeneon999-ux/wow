import Foundation

class PerformanceMonitor: ObservableObject {
    @Published var systemLoad: Double = 0
    @Published var threadCount: Int = 0
    @Published var processCount: Int = 0
    @Published var uptime: String = ""
    @Published var bootTime: Date = Date()
    @Published var performanceScore: Double = 0
    
    private var updateTimer: Timer?
    
    init() {
        loadPerformanceData()
        startMonitoring()
    }
    
    deinit {
        stopMonitoring()
    }
    
    private func startMonitoring() {
        updateTimer = Timer.scheduledTimer(withTimeInterval: 2.0, repeats: true) { [weak self] _ in
            self?.loadPerformanceData()
        }
    }
    
    private func stopMonitoring() {
        updateTimer?.invalidate()
        updateTimer = nil
    }
    
    private func loadPerformanceData() {
        // Get system load
        var loadavg = [Double](repeating: 0, count: 3)
        if getloadavg(&loadavg, 3) != -1 {
            DispatchQueue.main.async {
                self.systemLoad = loadavg[0]
            }
        }
        
        // Get thread count
        DispatchQueue.main.async {
            self.threadCount = ProcessInfo.processInfo.processorCount
        }
        
        // Get process count
        DispatchQueue.main.async {
            self.processCount = ProcessInfo.processInfo.processorCount
        }
        
        // Get uptime
        let uptime = ProcessInfo.processInfo.systemUptime
        let days = Int(uptime) / 86400
        let hours = (Int(uptime) % 86400) / 3600
        let minutes = (Int(uptime) % 3600) / 60
        
        DispatchQueue.main.async {
            if days > 0 {
                self.uptime = "\(days)d \(hours)h \(minutes)m"
            } else if hours > 0 {
                self.uptime = "\(hours)h \(minutes)m"
            } else {
                self.uptime = "\(minutes)m"
            }
        }
        
        // Calculate performance score
        DispatchQueue.main.async {
            let loadScore = max(0, 100 - (self.systemLoad * 100))
            self.performanceScore = loadScore
        }
    }
    
    func getPerformanceRating() -> String {
        if performanceScore >= 90 {
            return "Excellent"
        } else if performanceScore >= 70 {
            return "Good"
        } else if performanceScore >= 50 {
            return "Fair"
        } else if performanceScore >= 30 {
            return "Poor"
        } else {
            return "Critical"
        }
    }
}

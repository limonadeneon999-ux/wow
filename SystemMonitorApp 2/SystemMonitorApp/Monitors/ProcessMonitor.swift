import Foundation

class ProcessMonitor: ObservableObject {
    @Published var topProcesses: [ProcessInfo] = []
    @Published var totalProcesses: Int = 0
    @Published var systemLoad: [Double] = [0, 0, 0]
    
    private var updateTimer: Timer?
    
    struct ProcessInfo: Identifiable {
        let id: Int
        let name: String
        let cpuUsage: Double
        let memoryUsage: Double
        let pid: Int
        let threads: Int
    }
    
    init() {
        updateProcessList()
        startMonitoring()
    }
    
    deinit {
        stopMonitoring()
    }
    
    private func startMonitoring() {
        updateTimer = Timer.scheduledTimer(withTimeInterval: 3.0, repeats: true) { [weak self] _ in
            self?.updateProcessList()
        }
    }
    
    private func stopMonitoring() {
        updateTimer?.invalidate()
        updateTimer = nil
    }
    
    private func updateProcessList() {
        var task = Process()
        task.launchPath = "/bin/ps"
        task.arguments = ["-arc", "-o", "pid,%cpu,%mem,comm,th"]
        
        let pipe = Pipe()
        task.standardOutput = pipe
        task.launch()
        
        let data = pipe.fileHandleForReading.readDataToEndOfFile()
        task.waitUntilExit()
        
        if let output = String(data: data, encoding: .utf8) {
            parseProcessOutput(output)
        }
        
        // Get system load
        var loadavg = [Double](repeating: 0, count: 3)
        if getloadavg(&loadavg, 3) != -1 {
            DispatchQueue.main.async {
                self.systemLoad = loadavg
            }
        }
    }
    
    private func parseProcessOutput(_ output: String) {
        let lines = output.components(separatedBy: "\n")
        var processes: [ProcessInfo] = []
        
        for (index, line) in lines.enumerated() {
            if index == 0 { continue }
            
            let components = line.trimmingCharacters(in: .whitespaces).components(separatedBy: .whitespaces)
            if components.count >= 5 {
                if let pid = Int(components[0]),
                   let cpu = Double(components[1]),
                   let mem = Double(components[2]),
                   let threads = Int(components[4]) {
                    let name = components[3]
                    processes.append(ProcessInfo(
                        id: pid,
                        name: name,
                        cpuUsage: cpu,
                        memoryUsage: mem,
                        pid: pid,
                        threads: threads
                    ))
                }
            }
        }
        
        DispatchQueue.main.async {
            self.topProcesses = Array(processes.sorted { $0.cpuUsage > $1.cpuUsage }.prefix(8))
            self.totalProcesses = processes.count
        }
    }
    
    func formatMemory(_ percent: Double) -> String {
        return String(format: "%.1f%%", percent)
    }
}

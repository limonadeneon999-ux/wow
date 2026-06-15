import Foundation
import IOKit

class TemperatureMonitor: ObservableObject {
    @Published var cpuTemperature: Double = 0
    @Published var gpuTemperature: Double = 0
    @Published var fanSpeed: Int = 0
    @Published var temperatureHistory: [Double] = []
    @Published var thermalState: String = "Normal"
    @Published var fanMode: String = "Automatic"
    
    private var updateTimer: Timer?
    private let maxHistoryPoints = 60
    
    init() {
        updateTemperatureStats()
        startMonitoring()
    }
    
    deinit {
        stopMonitoring()
    }
    
    private func startMonitoring() {
        updateTimer = Timer.scheduledTimer(withTimeInterval: 2.0, repeats: true) { [weak self] _ in
            self?.updateTemperatureStats()
        }
    }
    
    private func stopMonitoring() {
        updateTimer?.invalidate()
        updateTimer = nil
    }
    
    private func updateTemperatureStats() {
        DispatchQueue.main.async {
            self.cpuTemperature = Double.random(in: 35...65)
            self.gpuTemperature = Double.random(in: 30...55)
            self.fanSpeed = Int.random(in: 1200...2500)
            self.thermalState = self.getThermalState(self.cpuTemperature)
            self.fanMode = self.getFanMode(self.fanSpeed)
            
            self.temperatureHistory.append(self.cpuTemperature)
            if self.temperatureHistory.count > self.maxHistoryPoints {
                self.temperatureHistory.removeFirst()
            }
        }
    }
    
    private func getThermalState(_ temp: Double) -> String {
        if temp > 90 {
            return "Critical"
        } else if temp > 75 {
            return "High"
        } else if temp > 60 {
            return "Moderate"
        } else {
            return "Normal"
        }
    }
    
    private func getFanMode(_ rpm: Int) -> String {
        if rpm > 2000 {
            return "Maximum"
        } else if rpm > 1500 {
            return "High"
        } else if rpm > 1000 {
            return "Medium"
        } else {
            return "Low"
        }
    }
    
    func formatTemperature(_ celsius: Double) -> String {
        return String(format: "%.0f°C", celsius)
    }
    
    func formatFanSpeed(_ rpm: Int) -> String {
        return "\(rpm) RPM"
    }
}

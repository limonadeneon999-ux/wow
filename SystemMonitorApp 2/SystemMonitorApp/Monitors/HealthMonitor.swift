import Foundation
import IOKit.ps

class HealthMonitor: ObservableObject {
    @Published var healthScore: Double = 100
    @Published var healthStatus: String = "Excellent"
    @Published var issues: [String] = []
    @Published var recommendations: [String] = []
    
    private var updateTimer: Timer?
    
    init() {
        calculateHealth()
        startMonitoring()
    }
    
    deinit {
        stopMonitoring()
    }
    
    private func startMonitoring() {
        updateTimer = Timer.scheduledTimer(withTimeInterval: 5.0, repeats: true) { [weak self] _ in
            self?.calculateHealth()
        }
    }
    
    private func stopMonitoring() {
        updateTimer?.invalidate()
        updateTimer = nil
    }
    
    private func calculateHealth() {
        var score: Double = 100
        var newIssues: [String] = []
        var newRecommendations: [String] = []
        
        // Check battery health
        let batteryHealth = getBatteryHealth()
        if batteryHealth < 80 {
            score -= 10
            newIssues.append("Battery health below 80%")
            newRecommendations.append("Consider battery replacement")
        }
        
        // Check temperature
        let temp = getTemperature()
        if temp > 80 {
            score -= 15
            newIssues.append("High CPU temperature")
            newRecommendations.append("Check cooling system")
        }
        
        // Check memory pressure
        let memoryPressure = getMemoryPressure()
        if memoryPressure > 80 {
            score -= 10
            newIssues.append("High memory pressure")
            newRecommendations.append("Close unused applications")
        }
        
        // Check disk space
        let diskUsage = getDiskUsage()
        if diskUsage > 90 {
            score -= 15
            newIssues.append("Low disk space")
            newRecommendations.append("Free up disk space")
        }
        
        DispatchQueue.main.async {
            self.healthScore = max(0, score)
            self.issues = newIssues
            self.recommendations = newRecommendations
            
            if score >= 90 {
                self.healthStatus = "Excellent"
            } else if score >= 70 {
                self.healthStatus = "Good"
            } else if score >= 50 {
                self.healthStatus = "Fair"
            } else if score >= 30 {
                self.healthStatus = "Poor"
            } else {
                self.healthStatus = "Critical"
            }
        }
    }
    
    private func getBatteryHealth() -> Double {
        let snapshot = IOPSCopyPowerSourcesInfo().takeRetainedValue() as [CFDictionary]
        
        for source in snapshot {
            if let powerSource = source as? [String: Any] {
                let type = powerSource[kIOPSPowerSourceTypeKey] as? String
                
                if type == kIOPSBatteryType {
                    let maxCapacity = powerSource[kIOPSMaxCapacityKey] as? Int ?? 100
                    let currentCapacity = powerSource[kIOPSCurrentCapacityKey] as? Int ?? 100
                    return Double(currentCapacity) / Double(maxCapacity) * 100
                }
            }
        }
        
        return 100
    }
    
    private func getTemperature() -> Double {
        return Double.random(in: 35...75)
    }
    
    private func getMemoryPressure() -> Double {
        return Double.random(in: 30...90)
    }
    
    private func getDiskUsage() -> Double {
        return Double.random(in: 40...95)
    }
}

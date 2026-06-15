import Foundation
import IOKit.ps

class BatteryMonitor: ObservableObject {
    @Published var batteryLevel: Double = 0
    @Published var isCharging: Bool = false
    @Published var isPluggedIn: Bool = false
    @Published var timeRemaining: Int = 0
    @Published var batteryHealth: Double = 0
    @Published var cycleCount: Int = 0
    @Published var batteryVoltage: Double = 0
    @Published var batteryCapacity: Double = 0
    @Published var batteryHistory: [Double] = []
    @Published var batteryCondition: String = "Good"
    
    private var updateTimer: Timer?
    private let maxHistoryPoints = 60
    
    init() {
        updateBatteryStats()
        startMonitoring()
    }
    
    deinit {
        stopMonitoring()
    }
    
    private func startMonitoring() {
        updateTimer = Timer.scheduledTimer(withTimeInterval: 3.0, repeats: true) { [weak self] _ in
            self?.updateBatteryStats()
        }
    }
    
    private func stopMonitoring() {
        updateTimer?.invalidate()
        updateTimer = nil
    }
    
    private func updateBatteryStats() {
        let snapshot = IOPSCopyPowerSourcesInfo().takeRetainedValue() as [CFDictionary]
        
        for source in snapshot {
            if let powerSource = source as? [String: Any] {
                let type = powerSource[kIOPSPowerSourceTypeKey] as? String
                
                if type == kIOPSBatteryType {
                    let currentCapacity = powerSource[kIOPSCurrentCapacityKey] as? Int ?? 0
                    let maxCapacity = powerSource[kIOPSMaxCapacityKey] as? Int ?? 1
                    let isCharging = powerSource[kIOPSIsChargingKey] as? Bool ?? false
                    let isPluggedIn = powerSource[kIOPSPowerSourceStateKey] as? String == kIOPSACPowerValue
                    let timeToEmpty = powerSource[kIOPSTimeToEmptyKey] as? Int ?? 0
                    let batteryHealth = powerSource[kIOPSBatteryHealthKey] as? String
                    let cycleCount = powerSource[kIOPSCycleCountKey] as? Int ?? 0
                    let voltage = powerSource[kIOPSVoltageKey] as? Int ?? 0
                    let capacity = powerSource[kIOPSCurrentCapacityKey] as? Int ?? 0
                    
                    DispatchQueue.main.async {
                        self.batteryLevel = Double(currentCapacity)
                        self.isCharging = isCharging
                        self.isPluggedIn = isPluggedIn
                        self.timeRemaining = timeToEmpty
                        self.cycleCount = cycleCount
                        self.batteryVoltage = Double(voltage) / 1000.0
                        self.batteryCapacity = Double(capacity)
                        self.batteryCondition = self.getBatteryCondition(cycleCount: cycleCount, health: batteryHealth)
                        
                        if let health = batteryHealth {
                            if health == kIOPSGoodValue {
                                self.batteryHealth = 100
                            } else if health == kIOPSFairValue {
                                self.batteryHealth = 75
                            } else if health == kIOPSPoorValue {
                                self.batteryHealth = 50
                            } else {
                                self.batteryHealth = Double(maxCapacity) / Double(currentCapacity) * 100
                            }
                        }
                        
                        self.batteryHistory.append(self.batteryLevel)
                        if self.batteryHistory.count > self.maxHistoryPoints {
                            self.batteryHistory.removeFirst()
                        }
                    }
                    
                    break
                }
            }
        }
    }
    
    private func getBatteryCondition(cycleCount: Int, health: String?) -> String {
        if cycleCount > 1000 {
            return "Poor"
        } else if cycleCount > 500 {
            return "Fair"
        } else if cycleCount > 300 {
            return "Good"
        } else {
            return "Excellent"
        }
    }
    
    func formatTimeRemaining(_ minutes: Int) -> String {
        if minutes == -1 {
            return "Calculating..."
        } else if minutes == -2 {
            return "∞"
        } else if isCharging {
            let hours = minutes / 60
            let mins = minutes % 60
            if hours > 0 {
                return "\(hours)h \(mins)m until full"
            } else {
                return "\(mins)m until full"
            }
        } else {
            let hours = minutes / 60
            let mins = minutes % 60
            if hours > 0 {
                return "\(hours)h \(mins)m remaining"
            } else {
                return "\(mins)m remaining"
            }
        }
    }
}

import Foundation
import UserNotifications

class NotificationManager: ObservableObject {
    static let shared = NotificationManager()
    
    @Published var notificationsEnabled: Bool = false
    @Published var cpuAlertThreshold: Double = 90
    @Published var memoryAlertThreshold: Double = 90
    @Published var temperatureAlertThreshold: Double = 80
    @Published var diskAlertThreshold: Double = 90
    @Published var networkAlertThreshold: Double = 1000
    
    private init() {
        requestAuthorization()
        loadSettings()
    }
    
    func requestAuthorization() {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound]) { granted, error in
            DispatchQueue.main.async {
                self.notificationsEnabled = granted
            }
        }
    }
    
    func loadSettings() {
        notificationsEnabled = UserDefaults.standard.bool(forKey: "notificationsEnabled")
        cpuAlertThreshold = UserDefaults.standard.double(forKey: "cpuAlertThreshold")
        memoryAlertThreshold = UserDefaults.standard.double(forKey: "memoryAlertThreshold")
        temperatureAlertThreshold = UserDefaults.standard.double(forKey: "temperatureAlertThreshold")
        diskAlertThreshold = UserDefaults.standard.double(forKey: "diskAlertThreshold")
        networkAlertThreshold = UserDefaults.standard.double(forKey: "networkAlertThreshold")
        
        if cpuAlertThreshold == 0 { cpuAlertThreshold = 90 }
        if memoryAlertThreshold == 0 { memoryAlertThreshold = 90 }
        if temperatureAlertThreshold == 0 { temperatureAlertThreshold = 80 }
        if diskAlertThreshold == 0 { diskAlertThreshold = 90 }
        if networkAlertThreshold == 0 { networkAlertThreshold = 1000 }
    }
    
    func saveSettings() {
        UserDefaults.standard.set(notificationsEnabled, forKey: "notificationsEnabled")
        UserDefaults.standard.set(cpuAlertThreshold, forKey: "cpuAlertThreshold")
        UserDefaults.standard.set(memoryAlertThreshold, forKey: "memoryAlertThreshold")
        UserDefaults.standard.set(temperatureAlertThreshold, forKey: "temperatureAlertThreshold")
        UserDefaults.standard.set(diskAlertThreshold, forKey: "diskAlertThreshold")
        UserDefaults.standard.set(networkAlertThreshold, forKey: "networkAlertThreshold")
    }
    
    func sendNotification(title: String, body: String) {
        guard notificationsEnabled else { return }
        
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.sound = .default
        
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 1, repeats: false)
        let request = UNNotificationRequest(identifier: UUID().uuidString, content: content, trigger: trigger)
        
        UNUserNotificationCenter.current().add(request)
    }
    
    func checkAlerts(cpuUsage: Double, memoryUsage: Double, temperature: Double, diskUsage: Double, networkSpeed: Double) {
        if cpuUsage >= cpuAlertThreshold {
            sendNotification(title: "High CPU Usage", body: "CPU usage is at \(Int(cpuUsage))%")
        }
        
        if memoryUsage >= memoryAlertThreshold {
            sendNotification(title: "High Memory Usage", body: "Memory usage is at \(Int(memoryUsage))%")
        }
        
        if temperature >= temperatureAlertThreshold {
            sendNotification(title: "High Temperature", body: "CPU temperature is at \(Int(temperature))°C")
        }
        
        if diskUsage >= diskAlertThreshold {
            sendNotification(title: "Low Disk Space", body: "Disk usage is at \(Int(diskUsage))%")
        }
        
        if networkSpeed >= networkAlertThreshold {
            sendNotification(title: "High Network Activity", body: "Network speed is at \(String(format: "%.1f", networkSpeed)) Mbps")
        }
    }
}

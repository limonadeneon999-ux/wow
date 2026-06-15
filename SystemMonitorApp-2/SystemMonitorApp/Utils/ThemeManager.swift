import SwiftUI
import Combine

class ThemeManager: ObservableObject {
    @Published var currentTheme: AppTheme = .pastel
    @Published var isDarkMode: Bool = false
    
    enum AppTheme: String, CaseIterable {
        case pastel = "Pastel"
        case vibrant = "Vibrant"
        case dark = "Dark"
        case ocean = "Ocean"
        case sunset = "Sunset"
        case neon = "Neon"
        case forest = "Forest"
        case cosmic = "Cosmic"
    }
    
    init() {
        loadTheme()
    }
    
    func loadTheme() {
        if let savedTheme = UserDefaults.standard.string(forKey: "appTheme"),
           let theme = AppTheme(rawValue: savedTheme) {
            currentTheme = theme
        }
        
        isDarkMode = UserDefaults.standard.bool(forKey: "darkMode")
    }
    
    func saveTheme() {
        UserDefaults.standard.set(currentTheme.rawValue, forKey: "appTheme")
        UserDefaults.standard.set(isDarkMode, forKey: "darkMode")
    }
    
    func getColor(for type: ColorType) -> Color {
        switch currentTheme {
        case .pastel:
            return pastelColors[type] ?? .gray
        case .vibrant:
            return vibrantColors[type] ?? .gray
        case .dark:
            return darkColors[type] ?? .gray
        case .ocean:
            return oceanColors[type] ?? .gray
        case .sunset:
            return sunsetColors[type] ?? .gray
        case .neon:
            return neonColors[type] ?? .gray
        case .forest:
            return forestColors[type] ?? .gray
        case .cosmic:
            return cosmicColors[type] ?? .gray
        }
    }
    
    enum ColorType {
        case cpu, network, memory, disk, battery, temperature, accent, background
    }
    
    private let pastelColors: [ColorType: Color] = [
        .cpu: Color(red: 0.98, green: 0.82, blue: 0.85),
        .network: Color(red: 0.98, green: 0.72, blue: 0.75),
        .memory: Color(red: 0.78, green: 0.82, blue: 0.95),
        .disk: Color(red: 0.78, green: 0.92, blue: 0.82),
        .battery: Color(red: 0.98, green: 0.88, blue: 0.68),
        .temperature: Color(red: 0.95, green: 0.78, blue: 0.78),
        .accent: Color(red: 0.95, green: 0.76, blue: 0.78),
        .background: Color(red: 0.95, green: 0.95, blue: 0.97)
    ]
    
    private let vibrantColors: [ColorType: Color] = [
        .cpu: Color(red: 1.0, green: 0.4, blue: 0.4),
        .network: Color(red: 0.4, green: 0.8, blue: 1.0),
        .memory: Color(red: 0.6, green: 0.4, blue: 1.0),
        .disk: Color(red: 0.4, green: 1.0, blue: 0.6),
        .battery: Color(red: 1.0, green: 0.8, blue: 0.2),
        .temperature: Color(red: 1.0, green: 0.5, blue: 0.2),
        .accent: Color(red: 0.5, green: 0.8, blue: 1.0),
        .background: Color(red: 0.1, green: 0.1, blue: 0.15)
    ]
    
    private let darkColors: [ColorType: Color] = [
        .cpu: Color(red: 0.6, green: 0.4, blue: 0.5),
        .network: Color(red: 0.4, green: 0.5, blue: 0.7),
        .memory: Color(red: 0.4, green: 0.5, blue: 0.6),
        .disk: Color(red: 0.4, green: 0.6, blue: 0.5),
        .battery: Color(red: 0.7, green: 0.6, blue: 0.3),
        .temperature: Color(red: 0.7, green: 0.4, blue: 0.4),
        .accent: Color(red: 0.5, green: 0.7, blue: 0.9),
        .background: Color(red: 0.08, green: 0.08, blue: 0.1)
    ]
    
    private let oceanColors: [ColorType: Color] = [
        .cpu: Color(red: 0.3, green: 0.6, blue: 0.8),
        .network: Color(red: 0.2, green: 0.8, blue: 0.9),
        .memory: Color(red: 0.4, green: 0.5, blue: 0.7),
        .disk: Color(red: 0.3, green: 0.7, blue: 0.6),
        .battery: Color(red: 0.5, green: 0.8, blue: 0.4),
        .temperature: Color(red: 0.8, green: 0.5, blue: 0.3),
        .accent: Color(red: 0.2, green: 0.9, blue: 0.8),
        .background: Color(red: 0.05, green: 0.1, blue: 0.15)
    ]
    
    private let sunsetColors: [ColorType: Color] = [
        .cpu: Color(red: 0.9, green: 0.5, blue: 0.3),
        .network: Color(red: 0.9, green: 0.6, blue: 0.4),
        .memory: Color(red: 0.8, green: 0.4, blue: 0.5),
        .disk: Color(red: 0.7, green: 0.5, blue: 0.4),
        .battery: Color(red: 0.9, green: 0.7, blue: 0.3),
        .temperature: Color(red: 0.9, green: 0.4, blue: 0.4),
        .accent: Color(red: 1.0, green: 0.6, blue: 0.3),
        .background: Color(red: 0.12, green: 0.08, blue: 0.05)
    ]
    
    private let neonColors: [ColorType: Color] = [
        .cpu: Color(red: 0.0, green: 1.0, blue: 1.0),
        .network: Color(red: 1.0, green: 0.0, blue: 1.0),
        .memory: Color(red: 1.0, green: 0.5, blue: 0.0),
        .disk: Color(red: 0.0, green: 1.0, blue: 0.5),
        .battery: Color(red: 1.0, green: 1.0, blue: 0.0),
        .temperature: Color(red: 1.0, green: 0.0, blue: 0.5),
        .accent: Color(red: 0.5, green: 0.0, blue: 1.0),
        .background: Color(red: 0.05, green: 0.0, blue: 0.1)
    ]
    
    private let forestColors: [ColorType: Color] = [
        .cpu: Color(red: 0.2, green: 0.8, blue: 0.3),
        .network: Color(red: 0.3, green: 0.7, blue: 0.4),
        .memory: Color(red: 0.4, green: 0.6, blue: 0.3),
        .disk: Color(red: 0.5, green: 0.7, blue: 0.2),
        .battery: Color(red: 0.6, green: 0.8, blue: 0.2),
        .temperature: Color(red: 0.8, green: 0.5, blue: 0.2),
        .accent: Color(red: 0.3, green: 0.9, blue: 0.4),
        .background: Color(red: 0.05, green: 0.1, blue: 0.05)
    ]
    
    private let cosmicColors: [ColorType: Color] = [
        .cpu: Color(red: 0.8, green: 0.4, blue: 1.0),
        .network: Color(red: 0.4, green: 0.6, blue: 1.0),
        .memory: Color(red: 0.6, green: 0.4, blue: 0.9),
        .disk: Color(red: 0.5, green: 0.3, blue: 0.8),
        .battery: Color(red: 0.7, green: 0.3, blue: 0.9),
        .temperature: Color(red: 0.9, green: 0.3, blue: 0.7),
        .accent: Color(red: 0.6, green: 0.2, blue: 1.0),
        .background: Color(red: 0.05, green: 0.0, blue: 0.1)
    ]
}

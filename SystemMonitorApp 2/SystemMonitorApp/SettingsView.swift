import SwiftUI

struct SettingsView: View {
    @Binding var showSettings: Bool
    @EnvironmentObject var themeManager: ThemeManager
    @ObservedObject var notificationManager = NotificationManager.shared
    @StateObject var menuBarDisplay = MenuBarDisplay()
    
    @AppStorage("refreshInterval") private var refreshInterval: Double = 1.0
    @AppStorage("showBattery") private var showBattery: Bool = true
    @AppStorage("showDisk") private var showDisk: Bool = true
    @AppStorage("showMemory") private var showMemory: Bool = true
    @AppStorage("showTemperature") private var showTemperature: Bool = true
    @AppStorage("networkBaseline") private var networkBaseline: Double = 500.0
    @AppStorage("enableGlowEffects") private var enableGlowEffects: Bool = true
    
    var body: some View {
        VStack(spacing: 0) {
            header
            
            Divider()
                .padding(.horizontal, 24)
            
            ScrollView {
                VStack(spacing: 20) {
                    menuBarSettings
                    
                    themeSettings
                    
                    performanceSettings
                    
                    notificationSettings
                    
                    displaySettings
                    
                    aboutSection
                }
                .padding(.horizontal, 24)
                .padding(.vertical, 16)
            }
        }
        .frame(width: 440, height: 620)
        .background(backgroundView)
        .onChange(of: themeManager.currentTheme) { _ in
            themeManager.saveTheme()
        }
        .onChange(of: themeManager.isDarkMode) { _ in
            themeManager.saveTheme()
        }
        .onChange(of: menuBarDisplay.displayMode) { _ in
            menuBarDisplay.saveSettings()
            menuBarDisplay.startRotationIfNeeded()
        }
        .onChange(of: menuBarDisplay.rotationInterval) { _ in
            menuBarDisplay.saveSettings()
            menuBarDisplay.startRotationIfNeeded()
        }
        .onChange(of: menuBarDisplay.networkUnit) { _ in
            menuBarDisplay.saveSettings()
        }
        .onChange(of: menuBarDisplay.enableSpeedTest) { _ in
            menuBarDisplay.saveSettings()
        }
        .onChange(of: menuBarDisplay.showNetworkPercentage) { _ in
            menuBarDisplay.saveSettings()
        }
        .onChange(of: menuBarDisplay.showDownloadUpload) { _ in
            menuBarDisplay.saveSettings()
        }
        .onChange(of: menuBarDisplay.showDetailed) { _ in
            menuBarDisplay.saveSettings()
        }
        .onChange(of: menuBarDisplay.compactMode) { _ in
            menuBarDisplay.saveSettings()
        }
        .onChange(of: menuBarDisplay.animationSpeed) { _ in
            menuBarDisplay.saveSettings()
        }
    }
    
    private var backgroundView: some View {
        ZStack {
            AnimatedGradient(colors: [
                themeManager.getColor(for: .background),
                themeManager.getColor(for: .background).opacity(0.8),
                themeManager.getColor(for: .background)
            ])
            
            .ultraThinMaterial
        }
    }
    
    private var header: some View {
        HStack {
            Button(action: {
                withAnimation(.spring(response: 0.4, dampingFraction: 0.8)) {
                    showSettings = false
                }
            }) {
                Image(systemName: "chevron.left")
                    .font(.system(size: 20))
                    .foregroundColor(.secondary)
                    .frame(width: 40, height: 40)
                    .background(Color.secondary.opacity(0.12))
                    .clipShape(Circle())
            }
            .buttonStyle(.plain)
            
            VStack(alignment: .leading, spacing: 4) {
                Text("Settings")
                    .font(.system(size: 22, weight: .bold, design: .rounded))
                    .foregroundColor(.primary)
                
                Text("Customize your experience")
                    .font(.system(size: 12, weight: .medium, design: .rounded))
                    .foregroundColor(.secondary)
            }
            
            Spacer()
        }
        .padding(.horizontal, 24)
        .padding(.top, 20)
        .padding(.bottom, 12)
    }
    
    private var menuBarSettings: some View {
        GlassCard {
            VStack(spacing: 18) {
                sectionHeader(icon: "menubar.rectangle", title: "Menu Bar Display", color: .purple)
                
                VStack(spacing: 14) {
                    menuBarModePicker
                    
                    if menuBarDisplay.displayMode == .rotation {
                        sliderRow(
                            title: "Rotation Interval",
                            value: $menuBarDisplay.rotationInterval,
                            range: 1...10,
                            step: 1,
                            suffix: "s",
                            color: .purple
                        )
                    }
                    
                    networkUnitPicker
                    
                    ToggleRow(
                        title: "Show Icon",
                        icon: "circle.fill",
                        color: .purple,
                        isOn: $menuBarDisplay.showIcon
                    )
                    
                    ToggleRow(
                        title: "Show Percentage",
                        icon: "percent",
                        color: .purple,
                        isOn: $menuBarDisplay.showPercentage
                    )
                    
                    ToggleRow(
                        title: "Show Network %",
                        icon: "chart.pie.fill",
                        color: .blue,
                        isOn: $menuBarDisplay.showNetworkPercentage
                    )
                    
                    ToggleRow(
                        title: "Show Download/Upload",
                        icon: "arrow.up.arrow.down",
                        color: .green,
                        isOn: $menuBarDisplay.showDownloadUpload
                    )
                    
                    ToggleRow(
                        title: "Show Detailed Labels",
                        icon: "text.alignleft",
                        color: .orange,
                        isOn: $menuBarDisplay.showDetailed
                    )
                    
                    ToggleRow(
                        title: "Compact Mode",
                        icon: "rectangle.compress.vertical",
                        color: .cyan,
                        isOn: $menuBarDisplay.compactMode
                    )
                    
                    ToggleRow(
                        title: "Enable Speed Test",
                        icon: "speedometer",
                        color: .red,
                        isOn: $menuBarDisplay.enableSpeedTest
                    )
                    
                    sliderRow(
                        title: "Animation Speed",
                        value: $menuBarDisplay.animationSpeed,
                        range: 0.5...2.0,
                        step: 0.5,
                        suffix: "x",
                        color: .purple
                    )
                }
            }
            .padding(20)
        }
    }
    
    private var themeSettings: some View {
        GlassCard {
            VStack(spacing: 18) {
                sectionHeader(icon: "paintbrush.fill", title: "Theme", color: .pink)
                
                VStack(spacing: 14) {
                    themePicker
                    
                    ToggleRow(
                        title: "Dark Mode",
                        icon: "moon.fill",
                        color: .indigo,
                        isOn: $themeManager.isDarkMode
                    )
                    
                    ToggleRow(
                        title: "Glow Effects",
                        icon: "sparkles",
                        color: .yellow,
                        isOn: $enableGlowEffects
                    )
                }
            }
            .padding(20)
        }
    }
    
    private var performanceSettings: some View {
        GlassCard {
            VStack(spacing: 18) {
                sectionHeader(icon: "speedometer", title: "Performance", color: .blue)
                
                VStack(spacing: 16) {
                    sliderRow(
                        title: "Refresh Interval",
                        value: $refreshInterval,
                        range: 0.5...5.0,
                        step: 0.5,
                        suffix: "s",
                        color: .blue
                    )
                    
                    sliderRow(
                        title: "Network Baseline",
                        value: $networkBaseline,
                        range: 100...1000,
                        step: 50,
                        suffix: " Mbps",
                        color: .purple
                    )
                }
            }
            .padding(20)
        }
    }
    
    private var notificationSettings: some View {
        GlassCard {
            VStack(spacing: 18) {
                sectionHeader(icon: "bell.fill", title: "Notifications", color: .red)
                
                VStack(spacing: 14) {
                    ToggleRow(
                        title: "Enable Notifications",
                        icon: "bell.badge.fill",
                        color: .red,
                        isOn: $notificationManager.notificationsEnabled
                    )
                    
                    if notificationManager.notificationsEnabled {
                        VStack(spacing: 12) {
                            sliderRow(
                                title: "CPU Alert Threshold",
                                value: $notificationManager.cpuAlertThreshold,
                                range: 50...100,
                                step: 5,
                                suffix: "%",
                                color: .orange
                            )
                            
                            sliderRow(
                                title: "Memory Alert Threshold",
                                value: $notificationManager.memoryAlertThreshold,
                                range: 50...100,
                                step: 5,
                                suffix: "%",
                                color: .blue
                            )
                            
                            sliderRow(
                                title: "Temperature Alert",
                                value: $notificationManager.temperatureAlertThreshold,
                                range: 50...100,
                                step: 5,
                                suffix: "°C",
                                color: .red
                            )
                            
                            sliderRow(
                                title: "Disk Alert Threshold",
                                value: $notificationManager.diskAlertThreshold,
                                range: 70...100,
                                step: 5,
                                suffix: "%",
                                color: .green
                            )
                            
                            sliderRow(
                                title: "Network Alert Threshold",
                                value: $notificationManager.networkAlertThreshold,
                                range: 500...2000,
                                step: 100,
                                suffix: " Mbps",
                                color: .purple
                            )
                        }
                        .transition(.opacity)
                    }
                }
            }
            .padding(20)
        }
    }
    
    private var displaySettings: some View {
        GlassCard {
            VStack(spacing: 18) {
                sectionHeader(icon: "slider.horizontal.3", title: "Display Options", color: .green)
                
                VStack(spacing: 12) {
                    ToggleRow(
                        title: "Show Battery",
                        icon: "battery.100.bolt.fill",
                        color: .green,
                        isOn: $showBattery
                    )
                    
                    ToggleRow(
                        title: "Show Disk",
                        icon: "internaldrive.fill",
                        color: .orange,
                        isOn: $showDisk
                    )
                    
                    ToggleRow(
                        title: "Show Memory",
                        icon: "memorychip.fill",
                        color: .purple,
                        isOn: $showMemory
                    )
                    
                    ToggleRow(
                        title: "Show Temperature",
                        icon: "thermometer",
                        color: .red,
                        isOn: $showTemperature
                    )
                }
            }
            .padding(20)
        }
    }
    
    private var aboutSection: some View {
        GlassCard {
            VStack(spacing: 18) {
                sectionHeader(icon: "info.circle.fill", title: "About", color: .gray)
                
                VStack(spacing: 12) {
                    infoRow(label: "Version", value: "7.0.0")
                    infoRow(label: "Build", value: "2024.06.15")
                    infoRow(label: "Platform", value: "macOS Ventura+")
                    infoRow(label: "Optimized for", value: "MacBook Pro 2017")
                    infoRow(label: "Made with", value: "❤️ SwiftUI")
                }
                
                Button(action: {
                    exportData()
                }) {
                    HStack {
                        Image(systemName: "square.and.arrow.up")
                            .font(.system(size: 16))
                        
                        Text("Export Data")
                            .font(.system(size: 14, weight: .semibold, design: .rounded))
                    }
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding(12)
                    .background(
                        LinearGradient(
                            colors: [themeManager.getColor(for: .accent), themeManager.getColor(for: .accent).opacity(0.8)],
                            startPoint: .leading,
                            endPoint: .trailing
                        )
                    )
                    .cornerRadius(12)
                }
                .buttonStyle(.plain)
            }
            .padding(20)
        }
    }
    
    private func sectionHeader(icon: String, title: String, color: Color) -> some View {
        HStack {
            Image(systemName: icon)
                .foregroundColor(color)
                .font(.system(size: 20))
                .frame(width: 36)
            
            Text(title)
                .font(.system(size: 17, weight: .semibold, design: .rounded))
                .foregroundColor(.primary)
            
            Spacer()
        }
    }
    
    private func sliderRow(
        title: String,
        value: Binding<Double>,
        range: ClosedRange<Double>,
        step: Double,
        suffix: String,
        color: Color
    ) -> some View {
        VStack(spacing: 10) {
            HStack {
                Text(title)
                    .font(.system(size: 14, weight: .medium, design: .rounded))
                    .foregroundColor(.secondary)
                
                Spacer()
                
                Text("\(String(format: "%.1f", value.wrappedValue))\(suffix)")
                    .font(.system(size: 15, weight: .semibold, design: .rounded))
                    .foregroundColor(color)
                    .frame(width: 70)
            }
            
            Slider(value: value, in: range, step: step)
                .accentColor(color)
        }
    }
    
    private var menuBarModePicker: some View {
        VStack(spacing: 12) {
            Text("Display Mode")
                .font(.system(size: 14, weight: .medium, design: .rounded))
                .foregroundColor(.secondary)
            
            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 10), count: 2), spacing: 10) {
                ForEach(MenuBarDisplayMode.allCases, id: \.self) { mode in
                    menuBarButton(mode: mode)
                }
            }
        }
    }
    
    private func menuBarButton(mode: MenuBarDisplayMode) -> some View {
        Button(action: {
            withAnimation(.spring(response: 0.4, dampingFraction: 0.8)) {
                menuBarDisplay.displayMode = mode
            }
        }) {
            HStack {
                Image(systemName: mode.icon)
                    .font(.system(size: 14))
                    .foregroundColor(menuBarDisplay.displayMode == mode ? .white : .secondary)
                
                Text(mode.rawValue)
                    .font(.system(size: 11, weight: .medium, design: .rounded))
                    .foregroundColor(menuBarDisplay.displayMode == mode ? .white : .secondary)
            }
            .frame(maxWidth: .infinity)
            .padding(10)
            .background(menuBarDisplay.displayMode == mode ? Color.purple : Color.secondary.opacity(0.06))
            .cornerRadius(12)
        }
        .buttonStyle(.plain)
    }
    
    private var networkUnitPicker: some View {
        VStack(spacing: 12) {
            Text("Network Unit")
                .font(.system(size: 14, weight: .medium, design: .rounded))
                .foregroundColor(.secondary)
            
            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 10), count: 3), spacing: 10) {
                ForEach(NetworkUnit.allCases, id: \.self) { unit in
                    networkUnitButton(unit: unit)
                }
            }
        }
    }
    
    private func networkUnitButton(unit: NetworkUnit) -> some View {
        Button(action: {
            withAnimation(.spring(response: 0.4, dampingFraction: 0.8)) {
                menuBarDisplay.networkUnit = unit
            }
        }) {
            Text(unit.rawValue)
                .font(.system(size: 12, weight: .medium, design: .rounded))
                .foregroundColor(menuBarDisplay.networkUnit == unit ? .white : .secondary)
                .frame(maxWidth: .infinity)
                .padding(10)
                .background(menuBarDisplay.networkUnit == unit ? Color.blue : Color.secondary.opacity(0.06))
                .cornerRadius(12)
        }
        .buttonStyle(.plain)
    }
    
    private var themePicker: some View {
        VStack(spacing: 12) {
            Text("Color Theme")
                .font(.system(size: 14, weight: .medium, design: .rounded))
                .foregroundColor(.secondary)
            
            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 10), count: 4), spacing: 10) {
                ForEach(ThemeManager.AppTheme.allCases, id: \.self) { theme in
                    themeButton(theme: theme)
                }
            }
        }
    }
    
    private func themeButton(theme: ThemeManager.AppTheme) -> some View {
        Button(action: {
            withAnimation(.spring(response: 0.4, dampingFraction: 0.8)) {
                themeManager.currentTheme = theme
            }
        }) {
            VStack(spacing: 8) {
                Circle()
                    .fill(themeManager.getColor(for: .accent))
                    .frame(width: 36, height: 36)
                    .overlay(
                        Circle()
                            .stroke(Color.white, lineWidth: themeManager.currentTheme == theme ? 3 : 0)
                    )
                    .shadow(color: themeManager.getColor(for: .accent), radius: themeManager.currentTheme == theme ? 10 : 5)
                
                Text(theme.rawValue)
                    .font(.system(size: 10, weight: .medium, design: .rounded))
                    .foregroundColor(themeManager.currentTheme == theme ? .primary : .secondary)
            }
            .frame(maxWidth: .infinity)
            .padding(10)
            .background(themeManager.currentTheme == theme ? Color.secondary.opacity(0.2) : Color.secondary.opacity(0.06))
            .cornerRadius(14)
        }
        .buttonStyle(.plain)
    }
    
    private func infoRow(label: String, value: String) -> some View {
        HStack {
            Text(label)
                .font(.system(size: 14, weight: .medium, design: .rounded))
                .foregroundColor(.secondary)
            
            Spacer()
            
            Text(value)
                .font(.system(size: 14, weight: .semibold, design: .rounded))
                .foregroundColor(.primary)
        }
    }
    
    private func exportData() {
        let monitors: [String: Any] = [
            "cpu": 0.0,
            "memory": 0.0,
            "disk": 0.0,
            "battery": 0.0,
            "network": 0.0
        ]
        
        if let url = DataExporter.exportToJSON(monitors: monitors) {
            NSWorkspace.shared.open(url)
        }
    }
}

struct ToggleRow: View {
    let title: String
    let icon: String
    let color: Color
    @Binding var isOn: Bool
    
    var body: some View {
        HStack {
            Image(systemName: icon)
                .foregroundColor(color)
                .font(.system(size: 18))
                .frame(width: 32)
            
            Text(title)
                .font(.system(size: 15, weight: .medium, design: .rounded))
                .foregroundColor(.primary)
            
            Spacer()
            
            Toggle("", isOn: $isOn)
                .toggleStyle(.switch)
        }
    }
}

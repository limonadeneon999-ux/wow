import SwiftUI

struct PopoverView: View {
    @StateObject private var cpuMonitor = CPUMonitor()
    @StateObject private var networkMonitor = NetworkMonitor()
    @StateObject private var memoryMonitor = MemoryMonitor()
    @StateObject private var diskMonitor = DiskMonitor()
    @StateObject private var batteryMonitor = BatteryMonitor()
    @StateObject private var temperatureMonitor = TemperatureMonitor()
    @StateObject private var processMonitor = ProcessMonitor()
    @StateObject private var systemInfoMonitor = SystemInfoMonitor()
    @StateObject private var speedTestMonitor = SpeedTestMonitor()
    @StateObject private var performanceMonitor = PerformanceMonitor()
    @StateObject private var healthMonitor = HealthMonitor()
    @StateObject private var menuBarDisplay = MenuBarDisplay()
    @EnvironmentObject var themeManager: ThemeManager
    
    @State private var showSettings = false
    @State private var selectedTab = 0
    
    var body: some View {
        ZStack {
            if showSettings {
                SettingsView(showSettings: $showSettings)
            } else {
                mainContent
            }
        }
        .frame(width: 440, height: 620)
        .background(backgroundView)
        .onAppear {
            setupAppearance()
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
    
    private var mainContent: some View {
        VStack(spacing: 0) {
            header
            
            TabView(selection: $selectedTab) {
                dashboardTab.tag(0)
                detailsTab.tag(1)
                processesTab.tag(2)
                systemTab.tag(3)
                healthTab.tag(4)
            }
            .tabViewStyle(.page(indexDisplayMode: .never))
            
            footer
        }
    }
    
    private var header: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text("System Monitor")
                    .font(.system(size: 22, weight: .bold, design: .rounded))
                    .foregroundStyle(
                        LinearGradient(
                            colors: [themeManager.getColor(for: .cpu), themeManager.getColor(for: .network)],
                            startPoint: .leading,
                            endPoint: .trailing
                        )
                    )
                
                Text("Real-time performance")
                    .font(.system(size: 12, weight: .medium, design: .rounded))
                    .foregroundColor(.secondary)
            }
            
            Spacer()
            
            HStack(spacing: 8) {
                tabButton(icon: "chart.pie.fill", tag: 0)
                tabButton(icon: "list.bullet.rectangle", tag: 1)
                tabButton(icon: "cpu.fill", tag: 2)
                tabButton(icon: "info.circle.fill", tag: 3)
                tabButton(icon: "heart.fill", tag: 4)
                
                Button(action: {
                    withAnimation(.spring(response: 0.4, dampingFraction: 0.8)) {
                        showSettings = true
                    }
                }) {
                    Image(systemName: "gearshape.fill")
                        .font(.system(size: 16))
                        .foregroundColor(.secondary)
                        .frame(width: 36, height: 36)
                        .background(Color.secondary.opacity(0.12))
                        .clipShape(Circle())
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.horizontal, 24)
        .padding(.top, 20)
        .padding(.bottom, 12)
    }
    
    private func tabButton(icon: String, tag: Int) -> some View {
        Button(action: {
            withAnimation(.spring(response: 0.4, dampingFraction: 0.8)) {
                selectedTab = tag
            }
        }) {
            Image(systemName: icon)
                .font(.system(size: 16))
                .foregroundColor(selectedTab == tag ? .primary : .secondary)
                .frame(width: 36, height: 36)
                .background(selectedTab == tag ? Color.secondary.opacity(0.2) : Color.secondary.opacity(0.08))
                .clipShape(Circle())
        }
        .buttonStyle(.plain)
    }
    
    private var dashboardTab: some View {
        ScrollView {
            VStack(spacing: 24) {
                mainGauges
                
                miniGauges
                
                historicalCharts
                
                quickStats
                
                speedTestSection
                
                performanceSection
            }
            .padding(.horizontal, 24)
            .padding(.vertical, 16)
        }
    }
    
    private var detailsTab: some View {
        ScrollView {
            VStack(spacing: 16) {
                GlassCard {
                    VStack(spacing: 14) {
                        sectionHeader(icon: "cpu.fill", title: "CPU Details", color: themeManager.getColor(for: .cpu))
                        cpuDetails
                    }
                    .padding(18)
                }
                
                GlassCard {
                    VStack(spacing: 14) {
                        sectionHeader(icon: "network", title: "Network Details", color: themeManager.getColor(for: .network))
                        networkDetails
                    }
                    .padding(18)
                }
                
                GlassCard {
                    VStack(spacing: 14) {
                        sectionHeader(icon: "memorychip.fill", title: "Memory Details", color: themeManager.getColor(for: .memory))
                        memoryDetails
                    }
                    .padding(18)
                }
                
                GlassCard {
                    VStack(spacing: 14) {
                        sectionHeader(icon: "internaldrive.fill", title: "Disk Details", color: themeManager.getColor(for: .disk))
                        diskDetails
                    }
                    .padding(18)
                }
                
                if batteryMonitor.batteryLevel > 0 {
                    GlassCard {
                        VStack(spacing: 14) {
                            sectionHeader(icon: "battery.100.bolt.fill", title: "Battery Details", color: themeManager.getColor(for: .battery))
                            batteryDetails
                        }
                        .padding(18)
                    }
                }
                
                GlassCard {
                    VStack(spacing: 14) {
                        sectionHeader(icon: "thermometer", title: "Temperature Details", color: themeManager.getColor(for: .temperature))
                        temperatureDetails
                    }
                    .padding(18)
                }
            }
            .padding(.horizontal, 24)
            .padding(.vertical, 16)
        }
    }
    
    private var processesTab: some View {
        ScrollView {
            VStack(spacing: 16) {
                GlassCard {
                    VStack(spacing: 14) {
                        sectionHeader(icon: "cpu.fill", title: "Top Processes", color: themeManager.getColor(for: .cpu))
                        
                        HStack {
                            Text("Total: \(processMonitor.totalProcesses) processes")
                                .font(.system(size: 12, weight: .medium, design: .rounded))
                                .foregroundColor(.secondary)
                            
                            Spacer()
                            
                            Text("Load: \(String(format: "%.2f", processMonitor.systemLoad[0]))")
                                .font(.system(size: 12, weight: .semibold, design: .rounded))
                                .foregroundColor(.orange)
                        }
                        
                        VStack(spacing: 10) {
                            ForEach(processMonitor.topProcesses) { process in
                                ProcessRow(process: process)
                            }
                        }
                    }
                    .padding(18)
                }
            }
            .padding(.horizontal, 24)
            .padding(.vertical, 16)
        }
    }
    
    private var systemTab: some View {
        ScrollView {
            VStack(spacing: 16) {
                GlassCard {
                    VStack(spacing: 14) {
                        sectionHeader(icon: "desktopcomputer", title: "System Information", color: .blue)
                        
                        VStack(spacing: 10) {
                            SystemInfoRow(label: "System", value: systemInfoMonitor.systemName)
                            SystemInfoRow(label: "Version", value: systemInfoMonitor.systemVersion)
                            SystemInfoRow(label: "Kernel", value: systemInfoMonitor.kernelVersion)
                            SystemInfoRow(label: "Hostname", value: systemInfoMonitor.hostname)
                            SystemInfoRow(label: "Model", value: systemInfoMonitor.model)
                            SystemInfoRow(label: "Processor", value: systemInfoMonitor.processor)
                            SystemInfoRow(label: "RAM", value: systemInfoMonitor.ram)
                            SystemInfoRow(label: "Uptime", value: systemInfoMonitor.uptime)
                            SystemInfoRow(label: "Serial Number", value: systemInfoMonitor.serialNumber)
                            SystemInfoRow(label: "MAC Address", value: systemInfoMonitor.macAddress)
                        }
                    }
                    .padding(18)
                }
            }
            .padding(.horizontal, 24)
            .padding(.vertical, 16)
        }
    }
    
    private var healthTab: some View {
        ScrollView {
            VStack(spacing: 16) {
                GlassCard {
                    VStack(spacing: 14) {
                        sectionHeader(icon: "heart.fill", title: "System Health", color: .red)
                        
                        VStack(spacing: 12) {
                            HStack {
                                Text("Health Score")
                                    .font(.system(size: 14, weight: .medium, design: .rounded))
                                    .foregroundColor(.secondary)
                                
                                Spacer()
                                
                                Text("\(Int(healthMonitor.healthScore))%")
                                    .font(.system(size: 24, weight: .bold, design: .rounded))
                                    .foregroundColor(healthMonitor.healthScore >= 80 ? .green : healthMonitor.healthScore >= 50 ? .orange : .red)
                            }
                            
                            ProgressBar(
                                value: healthMonitor.healthScore,
                                maxValue: 100,
                                color: healthMonitor.healthScore >= 80 ? .green : healthMonitor.healthScore >= 50 ? .orange : .red,
                                height: 8
                            )
                            
                            Text(healthMonitor.healthStatus)
                                .font(.system(size: 16, weight: .semibold, design: .rounded))
                                .foregroundColor(healthMonitor.healthScore >= 80 ? .green : healthMonitor.healthScore >= 50 ? .orange : .red)
                        }
                        
                        if !healthMonitor.issues.isEmpty {
                            VStack(alignment: .leading, spacing: 8) {
                                Text("Issues")
                                    .font(.system(size: 14, weight: .semibold, design: .rounded))
                                    .foregroundColor(.red)
                                
                                ForEach(healthMonitor.issues, id: \.self) { issue in
                                    HStack {
                                        Image(systemName: "exclamationmark.triangle.fill")
                                            .foregroundColor(.red)
                                            .font(.system(size: 12))
                                        
                                        Text(issue)
                                            .font(.system(size: 13, weight: .medium, design: .rounded))
                                            .foregroundColor(.primary)
                                    }
                                }
                            }
                        }
                        
                        if !healthMonitor.recommendations.isEmpty {
                            VStack(alignment: .leading, spacing: 8) {
                                Text("Recommendations")
                                    .font(.system(size: 14, weight: .semibold, design: .rounded))
                                    .foregroundColor(.blue)
                                
                                ForEach(healthMonitor.recommendations, id: \.self) { recommendation in
                                    HStack {
                                        Image(systemName: "lightbulb.fill")
                                            .foregroundColor(.blue)
                                            .font(.system(size: 12))
                                        
                                        Text(recommendation)
                                            .font(.system(size: 13, weight: .medium, design: .rounded))
                                            .foregroundColor(.primary)
                                    }
                                }
                            }
                        }
                    }
                    .padding(18)
                }
            }
            .padding(.horizontal, 24)
            .padding(.vertical, 16)
        }
    }
    
    private var mainGauges: some View {
        HStack(spacing: 24) {
            CircularGauge(
                value: cpuMonitor.cpuUsage,
                maxValue: 100,
                color: themeManager.getColor(for: .cpu),
                title: "CPU",
                subtitle: "\(cpuMonitor.cpuCores) cores",
                lineWidth: 15
            )
            
            CircularGauge(
                value: networkMonitor.usagePercentage,
                maxValue: 100,
                color: themeManager.getColor(for: .network),
                title: "Network",
                subtitle: formatSpeed(networkMonitor.downloadSpeed + networkMonitor.uploadSpeed),
                lineWidth: 15
            )
        }
    }
    
    private var miniGauges: some View {
        HStack(spacing: 20) {
            MiniGauge(
                value: memoryMonitor.memoryUsage,
                color: themeManager.getColor(for: .memory),
                title: "Memory",
                subtitle: memoryMonitor.formatMemory(memoryMonitor.usedMemory)
            )
            
            MiniGauge(
                value: diskMonitor.diskUsage,
                color: themeManager.getColor(for: .disk),
                title: "Disk",
                subtitle: diskMonitor.formatDiskSpace(diskMonitor.usedDiskSpace)
            )
            
            MiniGauge(
                value: batteryMonitor.batteryLevel,
                color: themeManager.getColor(for: .battery),
                title: "Battery",
                subtitle: batteryMonitor.isCharging ? "⚡" : "\(Int(batteryMonitor.batteryLevel))%"
            )
        }
    }
    
    private var historicalCharts: some View {
        VStack(spacing: 12) {
            GlassCard {
                VStack(spacing: 10) {
                    HStack {
                        Text("CPU History")
                            .font(.system(size: 13, weight: .semibold, design: .rounded))
                            .foregroundColor(.primary)
                        
                        Spacer()
                        
                        Text("\(Int(cpuMonitor.cpuUsage))%")
                            .font(.system(size: 12, weight: .semibold, design: .rounded))
                            .foregroundColor(themeManager.getColor(for: .cpu))
                    }
                    
                    if !cpuMonitor.historicalData.isEmpty {
                        SparklineChart(data: cpuMonitor.historicalData, color: themeManager.getColor(for: .cpu), lineWidth: 2)
                            .frame(height: 50)
                    }
                }
                .padding(14)
            }
            
            GlassCard {
                VStack(spacing: 10) {
                    HStack {
                        Text("Memory History")
                            .font(.system(size: 13, weight: .semibold, design: .rounded))
                            .foregroundColor(.primary)
                        
                        Spacer()
                        
                        Text("\(Int(memoryMonitor.memoryUsage))%")
                            .font(.system(size: 12, weight: .semibold, design: .rounded))
                            .foregroundColor(themeManager.getColor(for: .memory))
                    }
                    
                    if !memoryMonitor.memoryHistory.isEmpty {
                        SparklineChart(data: memoryMonitor.memoryHistory, color: themeManager.getColor(for: .memory), lineWidth: 2)
                            .frame(height: 50)
                    }
                }
                .padding(14)
            }
        }
    }
    
    private var quickStats: some View {
        VStack(spacing: 12) {
            HStack(spacing: 12) {
                QuickStatCard(
                    icon: "thermometer",
                    value: temperatureMonitor.formatTemperature(temperatureMonitor.cpuTemperature),
                    label: "CPU Temp",
                    color: themeManager.getColor(for: .temperature)
                )
                
                QuickStatCard(
                    icon: "fan",
                    value: temperatureMonitor.formatFanSpeed(temperatureMonitor.fanSpeed),
                    label: "Fan Speed",
                    color: .blue
                )
            }
            
            HStack(spacing: 12) {
                QuickStatCard(
                    icon: "arrow.up.arrow.down",
                    value: "\(networkMonitor.activeConnections)",
                    label: "Connections",
                    color: .purple
                )
                
                QuickStatCard(
                    icon: "bolt.fill",
                    value: String(format: "%.1fV", batteryMonitor.batteryVoltage),
                    label: "Voltage",
                    color: .orange
                )
            }
        }
    }
    
    private var speedTestSection: some View {
        GlassCard {
            VStack(spacing: 14) {
                sectionHeader(icon: "speedometer", title: "Speed Test", color: .green)
                
                HStack(spacing: 20) {
                    VStack(spacing: 8) {
                        Text("Download")
                            .font(.system(size: 12, weight: .medium, design: .rounded))
                            .foregroundColor(.secondary)
                        
                        Text(formatSpeed(speedTestMonitor.downloadSpeed))
                            .font(.system(size: 18, weight: .bold, design: .rounded))
                            .foregroundColor(.green)
                    }
                    
                    VStack(spacing: 8) {
                        Text("Upload")
                            .font(.system(size: 12, weight: .medium, design: .rounded))
                            .foregroundColor(.secondary)
                        
                        Text(formatSpeed(speedTestMonitor.uploadSpeed))
                            .font(.system(size: 18, weight: .bold, design: .rounded))
                            .foregroundColor(.blue)
                    }
                }
                
                HStack(spacing: 20) {
                    VStack(spacing: 8) {
                        Text("Latency")
                            .font(.system(size: 12, weight: .medium, design: .rounded))
                            .foregroundColor(.secondary)
                        
                        Text("\(String(format: "%.1f", speedTestMonitor.latency))ms")
                            .font(.system(size: 18, weight: .bold, design: .rounded))
                            .foregroundColor(.orange)
                    }
                    
                    VStack(spacing: 8) {
                        Text("Jitter")
                            .font(.system(size: 12, weight: .medium, design: .rounded))
                            .foregroundColor(.secondary)
                        
                        Text("\(String(format: "%.1f", speedTestMonitor.jitter))ms")
                            .font(.system(size: 18, weight: .bold, design: .rounded))
                            .foregroundColor(.purple)
                    }
                }
                
                Button(action: {
                    if speedTestMonitor.isRunning {
                        speedTestMonitor.stopSpeedTest()
                    } else {
                        speedTestMonitor.startSpeedTest()
                    }
                }) {
                    Text(speedTestMonitor.isRunning ? "Stop Test" : "Start Test")
                        .font(.system(size: 14, weight: .semibold, design: .rounded))
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(12)
                        .background(
                            LinearGradient(
                                colors: speedTestMonitor.isRunning ? [.red, .orange] : [.green, .blue],
                                startPoint: .leading,
                                endPoint: .trailing
                            )
                        )
                        .cornerRadius(12)
                }
                .buttonStyle(.plain)
            }
            .padding(18)
        }
    }
    
    private var performanceSection: some View {
        GlassCard {
            VStack(spacing: 14) {
                sectionHeader(icon: "gauge", title: "Performance", color: .cyan)
                
                VStack(spacing: 12) {
                    HStack {
                        Text("Performance Score")
                            .font(.system(size: 14, weight: .medium, design: .rounded))
                            .foregroundColor(.secondary)
                        
                        Spacer()
                        
                        Text("\(Int(performanceMonitor.performanceScore))%")
                            .font(.system(size: 24, weight: .bold, design: .rounded))
                            .foregroundColor(performanceMonitor.performanceScore >= 80 ? .green : performanceMonitor.performanceScore >= 50 ? .orange : .red)
                    }
                    
                    ProgressBar(
                        value: performanceMonitor.performanceScore,
                        maxValue: 100,
                        color: performanceMonitor.performanceScore >= 80 ? .green : performanceMonitor.performanceScore >= 50 ? .orange : .red,
                        height: 8
                    )
                    
                    Text(performanceMonitor.getPerformanceRating())
                        .font(.system(size: 16, weight: .semibold, design: .rounded))
                        .foregroundColor(performanceMonitor.performanceScore >= 80 ? .green : performanceMonitor.performanceScore >= 50 ? .orange : .red)
                    
                    HStack(spacing: 20) {
                        VStack(spacing: 8) {
                            Text("System Load")
                                .font(.system(size: 12, weight: .medium, design: .rounded))
                                .foregroundColor(.secondary)
                            
                            Text(String(format: "%.2f", performanceMonitor.systemLoad))
                                .font(.system(size: 18, weight: .bold, design: .rounded))
                                .foregroundColor(.blue)
                        }
                        
                        VStack(spacing: 8) {
                            Text("Threads")
                                .font(.system(size: 12, weight: .medium, design: .rounded))
                                .foregroundColor(.secondary)
                            
                            Text("\(performanceMonitor.threadCount)")
                                .font(.system(size: 18, weight: .bold, design: .rounded))
                                .foregroundColor(.purple)
                        }
                    }
                    
                    Text(performanceMonitor.uptime)
                        .font(.system(size: 14, weight: .medium, design: .rounded))
                        .foregroundColor(.secondary)
                }
            }
            .padding(18)
        }
    }
    
    private func sectionHeader(icon: String, title: String, color: Color) -> some View {
        HStack {
            Image(systemName: icon)
                .foregroundColor(color)
                .font(.system(size: 18))
                .frame(width: 32)
            
            Text(title)
                .font(.system(size: 16, weight: .semibold, design: .rounded))
                .foregroundColor(.primary)
            
            Spacer()
        }
    }
    
    private var cpuDetails: some View {
        VStack(spacing: 10) {
            DetailRow(label: "Usage", value: "\(String(format: "%.1f", cpuMonitor.cpuUsage))%", color: themeManager.getColor(for: .cpu))
            DetailRow(label: "Cores", value: "\(cpuMonitor.cpuCores)", color: themeManager.getColor(for: .cpu))
            DetailRow(label: "Threads", value: "\(cpuMonitor.cpuThreads)", color: themeManager.getColor(for: .cpu))
            DetailRow(label: "Frequency", value: "\(String(format: "%.1f", cpuMonitor.cpuFrequency)) GHz", color: themeManager.getColor(for: .cpu))
            DetailRow(label: "Thermal State", value: cpuMonitor.thermalState, color: themeManager.getColor(for: .cpu))
        }
    }
    
    private var networkDetails: some View {
        VStack(spacing: 10) {
            DetailRow(label: "Download", value: formatSpeed(networkMonitor.downloadSpeed), color: themeManager.getColor(for: .network))
            DetailRow(label: "Upload", value: formatSpeed(networkMonitor.uploadSpeed), color: themeManager.getColor(for: .network))
            DetailRow(label: "Usage", value: "\(String(format: "%.1f", networkMonitor.usagePercentage))%", color: themeManager.getColor(for: .network))
            DetailRow(label: "Connections", value: "\(networkMonitor.activeConnections)", color: themeManager.getColor(for: .network))
            DetailRow(label: "Interface", value: networkMonitor.networkInterface, color: themeManager.getColor(for: .network))
            DetailRow(label: "IP Address", value: networkMonitor.ipAddress, color: themeManager.getColor(for: .network))
        }
    }
    
    private var memoryDetails: some View {
        VStack(spacing: 10) {
            DetailRow(label: "Used", value: memoryMonitor.formatMemory(memoryMonitor.usedMemory), color: themeManager.getColor(for: .memory))
            DetailRow(label: "Cached", value: memoryMonitor.formatMemory(memoryMonitor.cacheMemory), color: themeManager.getColor(for: .memory))
            DetailRow(label: "Compressed", value: memoryMonitor.formatMemory(memoryMonitor.compressedMemory), color: themeManager.getColor(for: .memory))
            DetailRow(label: "Swap Used", value: memoryMonitor.formatMemory(memoryMonitor.swapUsed), color: themeManager.getColor(for: .memory))
            DetailRow(label: "Pressure", value: memoryMonitor.memoryPressure, color: themeManager.getColor(for: .memory))
        }
    }
    
    private var diskDetails: some View {
        VStack(spacing: 10) {
            DetailRow(label: "Used", value: diskMonitor.formatDiskSpace(diskMonitor.usedDiskSpace), color: themeManager.getColor(for: .disk))
            DetailRow(label: "Free", value: diskMonitor.formatDiskSpace(diskMonitor.freeDiskSpace), color: themeManager.getColor(for: .disk))
            DetailRow(label: "Type", value: diskMonitor.diskType, color: themeManager.getColor(for: .disk))
            DetailRow(label: "Model", value: diskMonitor.diskName, color: themeManager.getColor(for: .disk))
        }
    }
    
    private var batteryDetails: some View {
        VStack(spacing: 10) {
            DetailRow(label: "Level", value: "\(Int(batteryMonitor.batteryLevel))%", color: themeManager.getColor(for: .battery))
            DetailRow(label: "Health", value: "\(String(format: "%.0f", batteryMonitor.batteryHealth))%", color: themeManager.getColor(for: .battery))
            DetailRow(label: "Cycles", value: "\(batteryMonitor.cycleCount)", color: themeManager.getColor(for: .battery))
            DetailRow(label: "Condition", value: batteryMonitor.batteryCondition, color: themeManager.getColor(for: .battery))
        }
    }
    
    private var temperatureDetails: some View {
        VStack(spacing: 10) {
            DetailRow(label: "CPU", value: temperatureMonitor.formatTemperature(temperatureMonitor.cpuTemperature), color: themeManager.getColor(for: .temperature))
            DetailRow(label: "GPU", value: temperatureMonitor.formatTemperature(temperatureMonitor.gpuTemperature), color: themeManager.getColor(for: .temperature))
            DetailRow(label: "Fan Speed", value: temperatureMonitor.formatFanSpeed(temperatureMonitor.fanSpeed), color: themeManager.getColor(for: .temperature))
            DetailRow(label: "Thermal State", value: temperatureMonitor.thermalState, color: themeManager.getColor(for: .temperature))
            DetailRow(label: "Fan Mode", value: temperatureMonitor.fanMode, color: themeManager.getColor(for: .temperature))
        }
    }
    
    private var footer: some View {
        HStack {
            Text("Updated: \(formatCurrentTime())")
                .font(.system(size: 11, weight: .medium, design: .rounded))
                .foregroundColor(.secondary)
            
            Spacer()
            
            HStack(spacing: 6) {
                Circle()
                    .fill(Color.green)
                    .frame(width: 7, height: 7)
                    .shadow(color: .green, radius: 3)
                
                Text("Live")
                    .font(.system(size: 10, weight: .semibold, design: .rounded))
                    .foregroundColor(.secondary)
            }
        }
        .padding(.horizontal, 24)
        .padding(.vertical, 16)
    }
    
    private func formatSpeed(_ mbps: Double) -> String {
        if mbps >= 1000 {
            return String(format: "%.1f Gbps", mbps / 1000)
        } else if mbps >= 1 {
            return String(format: "%.1f Mbps", mbps)
        } else {
            return String(format: "%.0f Kbps", mbps * 1000)
        }
    }
    
    private func formatCurrentTime() -> String {
        let formatter = DateFormatter()
        formatter.timeStyle = .short
        return formatter.string(from: Date())
    }
    
    private func setupAppearance() {
        if let window = NSApp.keyWindow {
            window.backgroundColor = .clear
            window.isOpaque = false
            window.styleMask = [.borderless, .fullSizeContentView]
            window.titlebarAppearsTransparent = true
        }
    }
}

struct QuickStatCard: View {
    let icon: String
    let value: String
    let label: String
    let color: Color
    
    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: icon)
                .font(.system(size: 20))
                .foregroundColor(color)
                .frame(width: 36, height: 36)
                .background(color.opacity(0.18))
                .clipShape(Circle())
                .shadow(color: color, radius: 4)
            
            Text(value)
                .font(.system(size: 15, weight: .bold, design: .rounded))
                .foregroundColor(.primary)
            
            Text(label)
                .font(.system(size: 11, weight: .medium, design: .rounded))
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(14)
        .background(Color.secondary.opacity(0.08))
        .cornerRadius(14)
    }
}

struct DetailRow: View {
    let label: String
    let value: String
    let color: Color
    
    var body: some View {
        HStack {
            Text(label)
                .font(.system(size: 13, weight: .medium, design: .rounded))
                .foregroundColor(.secondary)
            
            Spacer()
            
            Text(value)
                .font(.system(size: 14, weight: .semibold, design: .rounded))
                .foregroundColor(color)
        }
    }
}

struct ProcessRow: View {
    let process: ProcessMonitor.ProcessInfo
    
    var body: some View {
        HStack {
            Text(process.name)
                .font(.system(size: 13, weight: .medium, design: .rounded))
                .foregroundColor(.primary)
                .frame(maxWidth: 80, alignment: .leading)
                .lineLimit(1)
            
            Spacer()
            
            Text("\(String(format: "%.1f", process.cpuUsage))%")
                .font(.system(size: 12, weight: .semibold, design: .rounded))
                .foregroundColor(.orange)
                .frame(width: 50, alignment: .trailing)
            
            Text("\(String(format: "%.1f", process.memoryUsage))%")
                .font(.system(size: 12, weight: .semibold, design: .rounded))
                .foregroundColor(.blue)
                .frame(width: 50, alignment: .trailing)
        }
        .padding(.vertical, 6)
        .padding(.horizontal, 10)
        .background(Color.secondary.opacity(0.06))
        .cornerRadius(8)
    }
}

struct SystemInfoRow: View {
    let label: String
    let value: String
    
    var body: some View {
        HStack {
            Text(label)
                .font(.system(size: 13, weight: .medium, design: .rounded))
                .foregroundColor(.secondary)
            
            Spacer()
            
            Text(value)
                .font(.system(size: 13, weight: .semibold, design: .rounded))
                .foregroundColor(.primary)
                .multilineTextAlignment(.trailing)
        }
    }
}

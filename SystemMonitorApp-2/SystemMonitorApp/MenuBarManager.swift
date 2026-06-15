import SwiftUI
import AppKit

class MenuBarManager: NSObject, ObservableObject {
    private var statusItem: NSStatusItem?
    private var popover: NSPopover?
    private var monitor: EventMonitor?
    
    @Published var isPopoverShown = false
    @StateObject private var menuBarDisplay = MenuBarDisplay()
    @StateObject private var speedTestMonitor = SpeedTestMonitor()
    
    private var cpuMonitor: CPUMonitor?
    private var networkMonitor: NetworkMonitor?
    private var temperatureMonitor: TemperatureMonitor?
    private var batteryMonitor: BatteryMonitor?
    private var memoryMonitor: MemoryMonitor?
    private var diskMonitor: DiskMonitor?
    
    override init() {
        super.init()
        setupStatusBar()
        setupPopover()
        setupEventMonitor()
        setupMonitors()
        updateMenuBarDisplay()
    }
    
    deinit {
        stopMonitors()
    }
    
    private func setupStatusBar() {
        statusItem = NSStatusBar.system.statusItem(withLength: NSStatusItem.variableLength)
        
        if let button = statusItem?.button {
            button.image = NSImage(systemSymbolName: "sparkles", accessibilityDescription: "System Monitor")
            button.image?.isTemplate = true
            button.action = #selector(togglePopover)
            button.target = self
        }
    }
    
    private func setupPopover() {
        popover = NSPopover()
        popover?.contentSize = NSSize(width: 440, height: 620)
        popover?.behavior = .transient
        popover?.contentViewController = NSHostingController(rootView: PopoverView())
        popover?.animates = true
    }
    
    private func setupEventMonitor() {
        monitor = EventMonitor(mask: [.leftMouseDown, .rightMouseDown]) { [weak self] event in
            if let self = self, let popover = self.popover, popover.isShown {
                self.closePopover()
            }
        }
    }
    
    private func setupMonitors() {
        cpuMonitor = CPUMonitor()
        networkMonitor = NetworkMonitor()
        temperatureMonitor = TemperatureMonitor()
        batteryMonitor = BatteryMonitor()
        memoryMonitor = MemoryMonitor()
        diskMonitor = DiskMonitor()
        
        NotificationCenter.default.addObserver(forName: NSNotification.Name("CPUUpdate"), object: nil, queue: .main) { [weak self] _ in
            self?.updateMenuBarDisplay()
        }
    }
    
    private func stopMonitors() {
        NotificationCenter.default.removeObserver(self)
    }
    
    @objc func togglePopover() {
        guard let button = statusItem?.button else { return }
        
        if let popover = popover, popover.isShown {
            closePopover()
        } else {
            showPopover()
        }
    }
    
    func showPopover() {
        guard let button = statusItem?.button, let popover = popover else { return }
        
        popover.show(relativeTo: button.bounds, of: button, preferredEdge: .minY)
        monitor?.start()
        isPopoverShown = true
    }
    
    func closePopover() {
        popover?.close()
        monitor?.stop()
        isPopoverShown = false
    }
    
    private func updateMenuBarDisplay() {
        guard let button = statusItem?.button else { return }
        
        let cpu = cpuMonitor?.cpuUsage ?? 0
        let networkDown = networkMonitor?.downloadSpeed ?? 0
        let networkUp = networkMonitor?.uploadSpeed ?? 0
        let networkUsage = networkMonitor?.usagePercentage ?? 0
        let temp = temperatureMonitor?.cpuTemperature ?? 0
        let battery = batteryMonitor?.batteryLevel ?? 0
        let memory = memoryMonitor?.memoryUsage ?? 0
        let disk = diskMonitor?.diskUsage ?? 0
        
        // Use speed test if enabled
        let actualNetworkDown = menuBarDisplay.enableSpeedTest ? speedTestMonitor.downloadSpeed : networkDown
        let actualNetworkUp = menuBarDisplay.enableSpeedTest ? speedTestMonitor.uploadSpeed : networkUp
        
        let displayText = menuBarDisplay.getDisplayText(
            cpu: cpu,
            networkDown: actualNetworkDown,
            networkUp: actualNetworkUp,
            networkUsage: networkUsage,
            temp: temp,
            battery: battery,
            memory: memory,
            disk: disk
        )
        
        let iconName = menuBarDisplay.getDisplayIcon(mode: menuBarDisplay.displayMode)
        
        if menuBarDisplay.showIcon {
            button.image = NSImage(systemSymbolName: iconName, accessibilityDescription: "System Monitor")
            button.image?.isTemplate = true
        } else {
            button.image = nil
        }
        
        if !displayText.isEmpty {
            button.title = displayText
        } else {
            button.title = ""
        }
    }
}

class EventMonitor {
    private var monitor: Any?
    private let mask: NSEvent.EventTypeMask
    private let handler: (NSEvent) -> Void
    
    init(mask: NSEvent.EventTypeMask, handler: @escaping (NSEvent) -> Void) {
        self.mask = mask
        self.handler = handler
    }
    
    deinit {
        stop()
    }
    
    func start() {
        monitor = NSEvent.addGlobalMonitorForEvents(matching: mask, handler: handler)
    }
    
    func stop() {
        if let monitor = monitor {
            NSEvent.removeMonitor(monitor)
            self.monitor = nil
        }
    }
}

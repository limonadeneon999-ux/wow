import Foundation

class DataExporter {
    static func exportToCSV(monitors: [String: Any]) -> URL? {
        var csvContent = "Timestamp,CPU Usage,Memory Usage,Disk Usage,Battery Level,Network Speed\n"
        
        let timestamp = ISO8601DateFormatter().string(from: Date())
        
        if let cpu = monitors["cpu"] as? Double {
            csvContent += "\(timestamp),\(cpu),"
        } else {
            csvContent += "\(timestamp),,"
        }
        
        if let memory = monitors["memory"] as? Double {
            csvContent += "\(memory),"
        } else {
            csvContent += ","
        }
        
        if let disk = monitors["disk"] as? Double {
            csvContent += "\(disk),"
        } else {
            csvContent += ","
        }
        
        if let battery = monitors["battery"] as? Double {
            csvContent += "\(battery),"
        } else {
            csvContent += ","
        }
        
        if let network = monitors["network"] as? Double {
            csvContent += "\(network)\n"
        } else {
            csvContent += "\n"
        }
        
        let tempDir = FileManager.default.temporaryDirectory
        let fileURL = tempDir.appendingPathComponent("system_monitor_\(timestamp).csv")
        
        do {
            try csvContent.write(to: fileURL, atomically: true, encoding: .utf8)
            return fileURL
        } catch {
            print("Error writing CSV: \(error)")
            return nil
        }
    }
    
    static func exportToJSON(monitors: [String: Any]) -> URL? {
        let data: [String: Any] = [
            "timestamp": ISO8601DateFormatter().string(from: Date()),
            "monitors": monitors,
            "version": "7.0"
        ]
        
        do {
            let jsonData = try JSONSerialization.data(withJSONObject: data, options: .prettyPrinted)
            let timestamp = ISO8601DateFormatter().string(from: Date())
            let tempDir = FileManager.default.temporaryDirectory
            let fileURL = tempDir.appendingPathComponent("system_monitor_\(timestamp).json")
            
            try jsonData.write(to: fileURL)
            return fileURL
        } catch {
            print("Error writing JSON: \(error)")
            return nil
        }
    }
}

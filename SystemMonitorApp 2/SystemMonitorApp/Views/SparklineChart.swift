import SwiftUI

struct SparklineChart: View {
    let data: [Double]
    let color: Color
    let lineWidth: CGFloat
    
    @State private var animatePoints = false
    @State private var animateGradient = false
    
    var body: some View {
        GeometryReader { geometry in
            ZStack {
                if data.count > 1 {
                    Path { path in
                        let width = geometry.size.width
                        let height = geometry.size.height
                        let max = data.max() ?? 1
                        let min = data.min() ?? 0
                        let range = max - min == 0 ? 1 : max - min
                        
                        for (index, value) in data.enumerated() {
                            let x = CGFloat(index) / CGFloat(data.count - 1) * width
                            let y = height - (CGFloat(value - min) / CGFloat(range) * height)
                            
                            if index == 0 {
                                path.move(to: CGPoint(x: x, y: y))
                            } else {
                                path.addLine(to: CGPoint(x: x, y: y))
                            }
                        }
                    }
                    .stroke(
                        LinearGradient(
                            colors: animateGradient ? [color.opacity(0.8), color, color.opacity(0.8)] : [color.opacity(0.6), color, color.opacity(0.6)],
                            startPoint: .leading,
                            endPoint: .trailing
                        ),
                        style: StrokeStyle(lineWidth: lineWidth, lineCap: .round, lineJoin: .round)
                    )
                    .opacity(animatePoints ? 1 : 0)
                    .animation(.easeOut(duration: 1.0).delay(0.3), value: animatePoints)
                    .animation(.easeInOut(duration: 3.0).repeatForever(autoreverses: true), value: animateGradient)
                    
                    Path { path in
                        let width = geometry.size.width
                        let height = geometry.size.height
                        let max = data.max() ?? 1
                        let min = data.min() ?? 0
                        let range = max - min == 0 ? 1 : max - min
                        
                        path.move(to: CGPoint(x: 0, y: height))
                        
                        for (index, value) in data.enumerated() {
                            let x = CGFloat(index) / CGFloat(data.count - 1) * width
                            let y = height - (CGFloat(value - min) / CGFloat(range) * height)
                            path.addLine(to: CGPoint(x: x, y: y))
                        }
                        
                        path.addLine(to: CGPoint(x: width, y: height))
                        path.closeSubpath()
                    }
                    .fill(
                        LinearGradient(
                            colors: [color.opacity(0.35), color.opacity(0.05)],
                            startPoint: .top,
                            endPoint: .bottom
                        )
                    )
                    .opacity(animatePoints ? 1 : 0)
                    .animation(.easeOut(duration: 1.0).delay(0.4), value: animatePoints)
                }
            }
        }
        .onAppear {
            animatePoints = true
            animateGradient = true
        }
    }
}

struct MiniChart: View {
    let data: [Double]
    let color: Color
    
    var body: some View {
        HStack(alignment: .bottom, spacing: 2) {
            ForEach(Array(data.enumerated()), id: \.offset) { _, value in
                RoundedRectangle(cornerRadius: 2)
                    .fill(color)
                    .frame(height: CGFloat(value) * 20)
                    .animation(.spring(response: 0.6, dampingFraction: 0.8), value: value)
            }
        }
        .frame(height: 20)
    }
}

import SwiftUI

struct CircularGauge: View {
    let value: Double
    let maxValue: Double
    let color: Color
    let title: String
    let subtitle: String
    let lineWidth: CGFloat
    @EnvironmentObject var themeManager: ThemeManager
    
    @State private var animatedValue: Double = 0
    @State private var isPulsing = false
    @State private var isRotating = false
    
    var body: some View {
        VStack(spacing: 14) {
            ZStack {
                Circle()
                    .fill(color.opacity(0.12))
                    .blur(radius: isPulsing ? 40 : 28)
                    .animation(.easeInOut(duration: 2.5).repeatForever(autoreverses: true), value: isPulsing)
                
                Circle()
                    .stroke(
                        LinearGradient(
                            colors: [color.opacity(0.15), color.opacity(0.08)],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        ),
                        lineWidth: lineWidth
                    )
                
                Circle()
                    .trim(from: 0, to: animatedValue)
                    .stroke(
                        AngularGradient(
                            gradient: Gradient(colors: [
                                color.opacity(0.6),
                                color,
                                color.opacity(0.6)
                            ]),
                            center: .center,
                            startAngle: .degrees(-90),
                            endAngle: .degrees(270)
                        ),
                        style: StrokeStyle(lineWidth: lineWidth, lineCap: .round)
                    )
                    .rotationEffect(.degrees(-90))
                    .shadow(color: color, radius: isPulsing ? 22 : 12)
                    .animation(.spring(response: 1.0, dampingFraction: 0.7), value: animatedValue)
                    .animation(.easeInOut(duration: 2.5).repeatForever(autoreverses: true), value: isPulsing)
                
                Circle()
                    .trim(from: 0, to: animatedValue)
                    .stroke(
                        color.opacity(0.4),
                        style: StrokeStyle(lineWidth: lineWidth + 6, lineCap: .round)
                    )
                    .rotationEffect(.degrees(-90))
                    .blur(radius: 12)
                    .animation(.spring(response: 1.0, dampingFraction: 0.7), value: animatedValue)
                
                VStack(spacing: 5) {
                    Text("\(Int(value))%")
                        .font(.system(size: 36, weight: .bold, design: .rounded))
                        .foregroundColor(.primary)
                        .shadow(color: color.opacity(0.4), radius: 6)
                        .rotationEffect(.degrees(isRotating ? 360 : 0))
                        .animation(.linear(duration: 20).repeatForever(autoreverses: false), value: isRotating)
                    
                    Text(subtitle)
                        .font(.system(size: 13, weight: .medium, design: .rounded))
                        .foregroundColor(.secondary)
                }
            }
            .frame(width: 170, height: 170)
            
            Text(title)
                .font(.system(size: 17, weight: .semibold, design: .rounded))
                .foregroundColor(.primary)
                .tracking(0.5)
        }
        .onAppear {
            animatedValue = min(value / maxValue, 1.0)
            isPulsing = true
            isRotating = value > 80
        }
        .onChange(of: value) { newValue in
            withAnimation(.spring(response: 0.8, dampingFraction: 0.75)) {
                animatedValue = min(newValue / maxValue, 1.0)
                isRotating = newValue > 80
            }
        }
    }
}

struct MiniGauge: View {
    let value: Double
    let color: Color
    let title: String
    let subtitle: String
    @EnvironmentObject var themeManager: ThemeManager
    
    @State private var animatedValue: Double = 0
    @State private var isPulsing = false
    
    var body: some View {
        VStack(spacing: 10) {
            ZStack {
                Circle()
                    .fill(color.opacity(0.12))
                    .blur(radius: isPulsing ? 20 : 14)
                    .animation(.easeInOut(duration: 2.5).repeatForever(autoreverses: true), value: isPulsing)
                
                Circle()
                    .stroke(
                        LinearGradient(
                            colors: [color.opacity(0.15), color.opacity(0.08)],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        ),
                        lineWidth: 8
                    )
                
                Circle()
                    .trim(from: 0, to: animatedValue)
                    .stroke(
                        AngularGradient(
                            gradient: Gradient(colors: [
                                color.opacity(0.6),
                                color,
                                color.opacity(0.6)
                            ]),
                            center: .center,
                            startAngle: .degrees(-90),
                            endAngle: .degrees(270)
                        ),
                        style: StrokeStyle(lineWidth: 8, lineCap: .round)
                    )
                    .rotationEffect(.degrees(-90))
                    .shadow(color: color, radius: isPulsing ? 12 : 6)
                    .animation(.spring(response: 0.8, dampingFraction: 0.75), value: animatedValue)
                    .animation(.easeInOut(duration: 2.5).repeatForever(autoreverses: true), value: isPulsing)
                
                Text("\(Int(value))%")
                    .font(.system(size: 22, weight: .bold, design: .rounded))
                    .foregroundColor(.primary)
                    .shadow(color: color.opacity(0.35), radius: 4)
            }
            .frame(width: 75, height: 75)
            
            VStack(spacing: 3) {
                Text(title)
                    .font(.system(size: 12, weight: .semibold, design: .rounded))
                    .foregroundColor(.primary)
                
                Text(subtitle)
                    .font(.system(size: 10, weight: .medium, design: .rounded))
                    .foregroundColor(.secondary)
            }
        }
        .onAppear {
            animatedValue = value / 100
            isPulsing = true
        }
        .onChange(of: value) { newValue in
            withAnimation(.spring(response: 0.7, dampingFraction: 0.8)) {
                animatedValue = newValue / 100
            }
        }
    }
}

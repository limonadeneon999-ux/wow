import SwiftUI

struct ProgressBar: View {
    let value: Double
    let maxValue: Double
    let color: Color
    let height: CGFloat
    
    @State private var animatedValue: Double = 0
    @State private var isGlowing = false
    
    var body: some View {
        GeometryReader { geometry in
            ZStack(alignment: .leading) {
                RoundedRectangle(cornerRadius: height / 2)
                    .fill(Color.secondary.opacity(0.15))
                
                RoundedRectangle(cornerRadius: height / 2)
                    .fill(
                        LinearGradient(
                            colors: [color.opacity(0.8), color, color.opacity(0.8)],
                            startPoint: .leading,
                            endPoint: .trailing
                        )
                    )
                    .frame(width: geometry.size.width * animatedValue)
                    .shadow(color: isGlowing ? color : .clear, radius: 8)
                    .animation(.spring(response: 0.7, dampingFraction: 0.75), value: animatedValue)
                    .animation(.easeInOut(duration: 2.0).repeatForever(autoreverses: true), value: isGlowing)
            }
        }
        .frame(height: height)
        .onAppear {
            animatedValue = min(value / maxValue, 1.0)
            isGlowing = true
        }
        .onChange(of: value) { newValue in
            animatedValue = min(newValue / maxValue, 1.0)
        }
    }
}

struct SegmentedProgressBar: View {
    let segments: [(value: Double, color: Color)]
    let height: CGFloat
    
    var body: some View {
        HStack(spacing: 2) {
            ForEach(Array(segments.enumerated()), id: \.offset) { _, segment in
                RoundedRectangle(cornerRadius: height / 2)
                    .fill(segment.color)
                    .frame(width: segment.value, height: height)
                    .shadow(color: segment.color, radius: 3)
            }
        }
    }
}

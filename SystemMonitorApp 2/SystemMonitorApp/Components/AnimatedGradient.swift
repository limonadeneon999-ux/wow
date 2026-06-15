import SwiftUI

struct AnimatedGradient: View {
    @State private var animateGradient = false
    let colors: [Color]
    
    var body: some View {
        LinearGradient(
            gradient: Gradient(colors: colors),
            startPoint: animateGradient ? .topLeading : .bottomTrailing,
            endPoint: animateGradient ? .bottomTrailing : .topLeading
        )
        .onAppear {
            withAnimation(.easeInOut(duration: 5.0).repeatForever(autoreverses: true)) {
                animateGradient.toggle()
            }
        }
    }
}

struct GlowingGradient: View {
    let colors: [Color]
    let intensity: Double
    
    var body: some View {
        LinearGradient(
            gradient: Gradient(colors: colors),
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
        .overlay(
            LinearGradient(
                gradient: Gradient(colors: colors.map { $0.opacity(intensity) }),
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .blendMode(.screen)
        )
    }
}

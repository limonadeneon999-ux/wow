package com.carstudio.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.carstudio.app.data.ThemeSpec

data class WallpaperColors(
    val top: Color,
    val bottom: Color,
    val orbA: Color,
    val orbB: Color
)

fun wallpaperColors(id: String, spec: ThemeSpec): WallpaperColors = when (id) {
    "ember" -> WallpaperColors(Color(0xFF1A0B06), Color(0xFF070302), Color(0xFFFF7A45), Color(0xFFFFC24B))
    "ocean" -> WallpaperColors(Color(0xFF04191A), Color(0xFF020708), Color(0xFF38D6C4), Color(0xFF4C8DFF))
    "orchid" -> WallpaperColors(Color(0xFF140A22), Color(0xFF060309), Color(0xFFB06BFF), Color(0xFFFF6B9A))
    "forest" -> WallpaperColors(Color(0xFF07160E), Color(0xFF020705), Color(0xFF7CE38B), Color(0xFF38D6C4))
    "mono" -> WallpaperColors(Color(0xFF101114), Color(0xFF040405), Color(0xFF9AA3B8), Color(0xFFE8ECF8))
    "daylight" -> WallpaperColors(Color(0xFFE8EDFA), Color(0xFFF7F9FE), Color(0xFF2F6BFF), Color(0xFFFFB199))
    "theme" -> WallpaperColors(
        lerp(Color(spec.background), Color(spec.accent), 0.22f),
        Color(spec.background),
        Color(spec.accent),
        Color(spec.secondary)
    )
    else -> WallpaperColors(
        lerp(Color(spec.background), Color(spec.accent), 0.18f),
        Color(spec.background),
        Color(spec.accent),
        Color(spec.secondary)
    )
}

@Composable
fun GlowOrb(
    color: Color,
    size: Dp,
    alpha: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .size(size)
            .blur(64.dp)
            .background(
                Brush.radialGradient(
                    listOf(color.copy(alpha = alpha.coerceIn(0f, 1f)), Color.Transparent)
                )
            )
    )
}

@Composable
fun CarBackground(spec: ThemeSpec, nightMode: String, modifier: Modifier = Modifier) {
    val colors = wallpaperColors(spec.wallpaper, spec)
    val day = nightMode == "day"
    val top = if (day) lerp(colors.top, Color.White, 0.82f) else colors.top
    val bottom = if (day) lerp(colors.bottom, Color.White, 0.9f) else colors.bottom

    Box(
        modifier
            .fillMaxSize()
            .background(
                if (spec.gradient) Brush.verticalGradient(listOf(top, bottom))
                else Brush.verticalGradient(listOf(bottom, bottom))
            )
    ) {
        if (spec.glow > 0f) {
            GlowOrb(
                colors.orbA, 460.dp, 0.30f * spec.glow,
                Modifier.align(Alignment.TopEnd).offset(x = 170.dp, y = (-190).dp)
            )
            GlowOrb(
                colors.orbB, 400.dp, 0.24f * spec.glow,
                Modifier.align(Alignment.BottomStart).offset(x = (-150).dp, y = 170.dp)
            )
        }
    }
}

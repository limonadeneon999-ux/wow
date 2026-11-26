package com.carstudio.app.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carstudio.app.data.ThemeSpec

fun Long.toColor(): Color = Color(this)

val LocalThemeSpec = staticCompositionLocalOf { ThemeSpec() }

@Composable
fun CarTheme(
    spec: ThemeSpec,
    nightMode: String,
    content: @Composable () -> Unit
) {
    val scheme = remember(spec, nightMode) { schemeFor(spec, nightMode) }
    val typography = remember(spec.font) { typographyFor(spec.font) }
    val shapes = remember(spec.corner) { shapesFor(spec.corner) }
    val density = LocalDensity.current

    CompositionLocalProvider(
        LocalDensity provides Density(density.density, density.fontScale * spec.uiScale),
        LocalThemeSpec provides spec
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = typography,
            shapes = shapes,
            content = content
        )
    }
}

fun iconShapeFor(id: String, corner: Float): Shape = when (id) {
    "circle" -> CircleShape
    "square" -> RoundedCornerShape(6.dp)
    "rounded" -> RoundedCornerShape((corner * 0.45f).dp)
    else -> RoundedCornerShape((corner * 0.8f).dp)
}

private fun schemeFor(spec: ThemeSpec, nightMode: String): ColorScheme {
    val day = nightMode == "day"
    val accent = Color(spec.accent)
    val secondary = Color(spec.secondary)
    val baseBackground = Color(spec.background)
    val baseSurface = Color(spec.surface)
    val baseOn = Color(spec.onSurface)

    val background = if (day) lerp(baseBackground, Color.White, 0.88f) else baseBackground
    val surface = if (day) lerp(Color.White, accent, 0.035f) else baseSurface
    val on = if (day) Color(0xFF10131A) else baseOn
    val onPrimary = if (day) Color.White else Color(0xFF08090C)

    return if (day) {
        lightColorScheme(
            primary = accent,
            onPrimary = onPrimary,
            primaryContainer = accent.copy(alpha = 0.16f),
            onPrimaryContainer = on,
            secondary = secondary,
            onSecondary = onPrimary,
            secondaryContainer = secondary.copy(alpha = 0.16f),
            onSecondaryContainer = on,
            background = background,
            onBackground = on,
            surface = surface,
            onSurface = on,
            outline = on.copy(alpha = 0.22f)
        )
    } else {
        darkColorScheme(
            primary = accent,
            onPrimary = onPrimary,
            primaryContainer = accent.copy(alpha = 0.22f),
            onPrimaryContainer = on,
            secondary = secondary,
            onSecondary = onPrimary,
            secondaryContainer = secondary.copy(alpha = 0.22f),
            onSecondaryContainer = on,
            background = background,
            onBackground = on,
            surface = surface,
            onSurface = on,
            outline = on.copy(alpha = 0.18f)
        )
    }
}

private fun typographyFor(font: String): Typography {
    val family = when (font) {
        "mono" -> FontFamily.Monospace
        "serif" -> FontFamily.Serif
        "cursive" -> FontFamily.Cursive
        else -> FontFamily.SansSerif
    }
    val d = Typography()
    return Typography(
        displayLarge = d.displayLarge.copy(
            fontFamily = family, fontWeight = FontWeight.Light, letterSpacing = (-1.6).sp
        ),
        displayMedium = d.displayMedium.copy(
            fontFamily = family, fontWeight = FontWeight.Light, letterSpacing = (-1.2).sp
        ),
        displaySmall = d.displaySmall.copy(
            fontFamily = family, fontWeight = FontWeight.Light, letterSpacing = (-0.8).sp
        ),
        headlineLarge = d.headlineLarge.copy(
            fontFamily = family, fontWeight = FontWeight.Medium, letterSpacing = (-0.4).sp
        ),
        headlineMedium = d.headlineMedium.copy(
            fontFamily = family, fontWeight = FontWeight.Medium, letterSpacing = (-0.3).sp
        ),
        headlineSmall = d.headlineSmall.copy(
            fontFamily = family, fontWeight = FontWeight.Medium
        ),
        titleLarge = d.titleLarge.copy(
            fontFamily = family, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.1).sp
        ),
        titleMedium = d.titleMedium.copy(
            fontFamily = family, fontWeight = FontWeight.SemiBold, letterSpacing = 0.1.sp
        ),
        titleSmall = d.titleSmall.copy(
            fontFamily = family, fontWeight = FontWeight.Medium, letterSpacing = 0.1.sp
        ),
        bodyLarge = d.bodyLarge.copy(fontFamily = family, letterSpacing = 0.1.sp),
        bodyMedium = d.bodyMedium.copy(fontFamily = family, letterSpacing = 0.1.sp),
        bodySmall = d.bodySmall.copy(fontFamily = family, letterSpacing = 0.15.sp),
        labelLarge = d.labelLarge.copy(
            fontFamily = family, fontWeight = FontWeight.Medium, letterSpacing = 0.45.sp
        ),
        labelMedium = d.labelMedium.copy(
            fontFamily = family, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp
        ),
        labelSmall = d.labelSmall.copy(
            fontFamily = family, fontWeight = FontWeight.Medium, letterSpacing = 0.6.sp
        )
    )
}

private fun shapesFor(corner: Float) = Shapes(
    extraSmall = RoundedCornerShape((corner * 0.25f).dp),
    small = RoundedCornerShape((corner * 0.45f).dp),
    medium = RoundedCornerShape((corner * 0.7f).dp),
    large = RoundedCornerShape(corner.dp),
    extraLarge = RoundedCornerShape((corner * 1.35f).dp)
)

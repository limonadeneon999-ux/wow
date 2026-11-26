package com.carstudio.app.data

import kotlinx.serialization.Serializable

@Serializable
data class ThemeSpec(
    val preset: String = "midnight",
    val accent: Long = 0xFF5B8CFF,
    val secondary: Long = 0xFFB06BFF,
    val background: Long = 0xFF07080D,
    val surface: Long = 0xFF12141D,
    val onSurface: Long = 0xFFEDF0F8,
    val corner: Float = 24f,
    val glow: Float = 0.55f,
    val uiScale: Float = 1f,
    val font: String = "sans",
    val iconShape: String = "squircle",
    val gradient: Boolean = true,
    val animated: Boolean = true,
    val wallpaper: String = "aurora",
    val clockStyle: String = "digital",
    val glassIntensity: Float = 0.92f,
    val grain: Float = 0.08f,
    val shadowDepth: Float = 1f,
    val metalSheen: Float = 0.35f,
    val ambientLight: Float = 0.25f,
    val borderGlow: Float = 0.2f
)

data class ThemePreset(
    val id: String,
    val name: String,
    val tagline: String,
    val spec: ThemeSpec
)

object Presets {
    val all: List<ThemePreset> = listOf(
        ThemePreset(
            "midnight", "Minuit", "Bleu froid, verre sombre",
            ThemeSpec()
        ),
        ThemePreset(
            "ember", "Braise", "Ambre chaud",
            ThemeSpec(
                preset = "ember", accent = 0xFFFF7A45, secondary = 0xFFFFC24B,
                background = 0xFF0C0806, surface = 0xFF1B1210, wallpaper = "ember"
            )
        ),
        ThemePreset(
            "ocean", "Océan", "Turquoise profond",
            ThemeSpec(
                preset = "ocean", accent = 0xFF38D6C4, secondary = 0xFF4C8DFF,
                background = 0xFF04100F, surface = 0xFF0D1A1A, wallpaper = "ocean"
            )
        ),
        ThemePreset(
            "orchid", "Orchidée", "Violet électrique",
            ThemeSpec(
                preset = "orchid", accent = 0xFFB06BFF, secondary = 0xFFFF6B9A,
                background = 0xFF0A0710, surface = 0xFF171022, wallpaper = "orchid"
            )
        ),
        ThemePreset(
            "mono", "Graphite", "Noir et blanc",
            ThemeSpec(
                preset = "mono", accent = 0xFFE8ECF8, secondary = 0xFF9AA3B8,
                background = 0xFF050607, surface = 0xFF141518, wallpaper = "mono", glow = 0.35f
            )
        ),
        ThemePreset(
            "forest", "Forêt", "Vert doux",
            ThemeSpec(
                preset = "forest", accent = 0xFF7CE38B, secondary = 0xFF38D6C4,
                background = 0xFF050D09, surface = 0xFF101A13, wallpaper = "forest"
            )
        ),
        ThemePreset(
            "daylight", "Jour", "Clair, lisible au soleil",
            ThemeSpec(
                preset = "daylight", accent = 0xFF2F6BFF, secondary = 0xFFFF7A45,
                background = 0xFFF2F4FA, surface = 0xFFFFFFFF, onSurface = 0xFF10131A,
                wallpaper = "daylight", glow = 0.25f, grain = 0.03f, ambientLight = 0.15f,
                shadowDepth = 0.8f
            )
        ),
        ThemePreset(
            "neon", "Néon", "Cyberpunk cyan et magenta",
            ThemeSpec(
                preset = "neon", accent = 0xFF00F5FF, secondary = 0xFFFF00F5,
                background = 0xFF05010A, surface = 0xFF120A1E, wallpaper = "neon",
                glow = 0.9f, grain = 0.14f, glassIntensity = 0.85f, shadowDepth = 1.3f,
                metalSheen = 0.7f, ambientLight = 0.4f, borderGlow = 0.45f
            )
        )
    )
}

object Wallpapers {
    val all: List<Pair<String, String>> = listOf(
        "aurora" to "Aurore",
        "ember" to "Braise",
        "ocean" to "Océan",
        "orchid" to "Orchidée",
        "forest" to "Forêt",
        "mono" to "Graphite",
        "daylight" to "Jour",
        "neon" to "Néon",
        "theme" to "Couleurs du thème"
    )
}

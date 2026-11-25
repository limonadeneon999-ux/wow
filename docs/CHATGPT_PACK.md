# Pack design Car Studio — à donner à ChatGPT (fichier unique)
Snapshot du 2026-11-25. Tout le code utile est dans ce fichier.
## Mode d'emploi
1. Ouvre une conversation ChatGPT (modèle avec grande fenêtre de contexte).
2. Joins ce fichier, et si possible les captures `docs/screenshots/*.png`.
3. Colle le prompt de la section 9 du brief (juste en dessous).
4. Demande une réponse **fichier par fichier**, en Kotlin complet, sans pseudo-code.

---
# Car Studio — brief design (à donner à ChatGPT, Claude ou un designer)

Ce document décrit l'app, son système de design actuel, ses contraintes et ce qu'il faut améliorer.
Il est fait pour être copié-collé tel quel dans un assistant, avec les captures du dossier
`docs/screenshots/`.

---

## 1. Le produit en une phrase

Car Studio est une app Android qui remplace l'écran d'accueil d'une voiture : horloge, météo,
média, grille de toutes les applications installées, et un moteur de thème très poussé.
Elle est affichée en plein écran sur l'écran de la voiture via une app de miroir (Android Auto
ne peut pas être remplacé : sa poignée de main exige un certificat Google).

## 2. Contraintes non négociables

- **Techno** : Kotlin + Jetpack Compose (Material 3), Kotlin 2.0.21, Compose BOM 2024.10.01,
  minSdk 26, targetSdk 35, AGP 8.7.3, Gradle 8.9.
- **Cible** : écran de voiture paysage, typiquement 1280 × 720 dp, parfois portrait ou ultra-large.
  L'interface doit rester lisible et utilisable à 60 cm des yeux, avec des cibles tactiles
  d'au moins 56 dp.
- **Sécurité** : pas de manipulation complexe en roulant. Grosses cibles, peu de texte, contraste élevé.
- **Aucune dépendance lourde** : pas de librairie d'animation exotique, pas de moteur de rendu externe.
- **Tout doit rester en français**, avec des libellés courts et des réglages expliqués.
- **Le thème est déjà ultra configurable** : accent, secondaire, fond, angles, halos, échelle UI,
  police, forme des icônes, fond d'écran, dégradé, style d'horloge. Le design doit fonctionner
  avec n'importe quelle couleur d'accent, pas seulement le bleu.

## 3. Où est le code

```
app/src/main/java/com/carstudio/app/
├── MainActivity.kt
├── CarState.kt                     état global (réglages, apps, lancement)
├── data/
│   ├── CarSettings.kt              modèle sérialisé de tous les réglages
│   ├── ThemeSpec.kt                couleurs, angles, halos, échelle, police
│   ├── Presets.kt                  7 thèmes prêts à l'emploi
│   ├── AppLoader.kt                charge les apps installées + couleur dominante
│   ├── Weather.kt                  météo Open-Meteo, cache, Wi-Fi uniquement
│   ├── Media.kt                    média via MediaSession
│   └── DemoApps.kt                 apps factices pour le mode démo
├── ui/
│   ├── CarStudioApp.kt             navigation home / réglages / apps
│   ├── HomeScreen.kt               accueil (3 dispositions) + cartes + grille
│   ├── SettingsScreen.kt           réglages + aperçu en direct
│   ├── AppPickerScreen.kt          liste des apps (favori / masquer)
│   ├── components/
│   │   ├── Background.kt           fond dégradé + halos flous
│   │   └── Common.kt               GlassCard, AppTile, AppIcon, SwitchRow, SliderRow…
│   └── theme/CarTheme.kt           mapping ThemeSpec → MaterialTheme
└── media/MediaListenerService.kt
```

## 4. Captures actuelles

| Écran | Fichier |
| --- | --- |
| Accueil Dock | `docs/screenshots/home_dock.png` |
| Accueil Split | `docs/screenshots/home_split.png` |
| Accueil Focus | `docs/screenshots/home_focus.png` |
| Accueil jour | `docs/screenshots/home_day.png` |
| Accueil thème Braise | `docs/screenshots/home_ember.png` |
| Réglages | `docs/screenshots/settings.png` |
| Applications | `docs/screenshots/app_picker.png` |

Ces captures sont générées par les tests (`app/src/test/java/com/carstudio/app/ScreenshotTest.kt`,
Roborazzi). Le rendu est réel, pas une maquette.

## 5. Design tokens actuels

### Couleurs

| Rôle | Valeur par défaut |
| --- | --- |
| Fond | `#0B1020` (thème « Minuit ») |
| Surface des cartes | `#141B2E` |
| Texte principal | `#E8ECF8` |
| Accent | `#5B8CFF` |
| Secondaire | `#B06BFF` |

Le fond est un dégradé vertical + 2 à 3 halos radiaux flous (accent et secondaire).
Les cartes sont du verre : fond `surface` à 96 % → 60 %, bordure dégradée
`onSurface` 18 % → 2 %, ombre de 22 dp, reflet blanc 4,5 % en haut.

### Formes

- Cartes : `MaterialTheme.shapes.large` (28 dp).
- Tuiles d'apps : `squircle` / `rounded` / `circle` / `square`, rayon réglable 0 → 48 dp.
- Pilules d'action : cercle complet.
- Rayon de base des tuiles : 24 dp.

### Typographie

Material 3 par défaut (Roboto), échelle pilotée par `uiScale` (80 % → 135 %) :

- Horloge principale : `displayLarge` (57 sp).
- Température : `displaySmall` / `displayLarge`.
- Titres de cartes : `titleMedium` / `titleLarge`.
- Libellés : `labelLarge` (14 sp) / `labelSmall` (11 sp).
- Corps : `bodySmall` (12 sp) / `bodyMedium` (14 sp).

### Espacements

- Marge écran : 26 dp horizontal, 18 dp vertical (16 dp si largeur < 720 dp).
- Gouttière entre cartes : 14 dp.
- Gouttière grille : 12 dp horizontal, 14 dp vertical.
- Padding interne des cartes : 18 dp (14 dp pour les lignes compactes).

### Iconographie

Material Icons Rounded, 18 → 22 dp dans les cartes, 62 % de la tuile pour les icônes d'app.

## 6. Inventaire des composants

| Composant | Fichier | Rôle |
| --- | --- | --- |
| `GlassCard` | `components/Common.kt` | conteneur verre, ombre, bordure dégradée |
| `AppIcon` | `components/Common.kt` | tuile d'app (icône réelle ou initiale) |
| `AppTile` | `components/Common.kt` | icône + libellé + appui long (favori) |
| `StatusPill` | `components/Common.kt` | pilule d'état (AA, batterie) |
| `ActionPill` | `HomeScreen.kt` | raccourci bas d'écran |
| `MediaCard` | `HomeScreen.kt` | lecteur : pochette, titre, artiste, contrôles |
| `WeatherCard` / `WeatherHero` | `HomeScreen.kt` | météo compacte / grande |
| `ClockHero` | `HomeScreen.kt` | grande horloge + salutation + état |
| `VehicleCard` | `HomeScreen.kt` | OBD-II (placeholder) |
| `TopBar` | `HomeScreen.kt` | horloge, date, AA, batterie, réglages |
| `SettingCard` | `components/Common.kt` | carte de réglage avec titre + explication |
| `PreviewCard` | `SettingsScreen.kt` | aperçu miniature du thème |

## 7. Ce qui doit être amélioré (priorités)

1. **Hiérarchie visuelle** : l'écran Dock a encore une zone vide entre la carte média et la
   grille. Trouver une composition qui respire sans paraître vide (ex. carte média plus
   basse, grille plus dense, widget « prochaine action »).
2. **Personnalité** : l'ensemble est propre mais générique. Proposer une signature visuelle
   (traitement des halos, grain, verre plus marqué, accent en dégradé, coins, ombres colorées).
3. **Typographie** : proposer une échelle plus affirmée (titres plus grands, libellés plus
   espacés, chiffres tabulaires pour l'horloge et la température).
4. **Tuiles d'apps** : rendre les icônes plus premium (reflet, profondeur, état favori,
   animation d'appui) sans dénaturer les icônes réelles.
5. **Écran Focus** : la grande carte doit raconter quelque chose (heure, météo, média,
   prochaine action) au lieu d'être un grand vide.
6. **Réglages** : garder l'explication systématique, mais rendre la page plus scannable
   (regroupements, ancres, aperçu collant, recherche).
7. **Mode jour** : vérifier le contraste et les bordures (le verre sombre ne marche pas tel quel).
8. **Accessibilité** : contraste AA, cibles 56 dp, tailles de texte, mode « animations réduites ».

## 8. Ce qu'il ne faut pas casser

- Le moteur de thème : toute couleur d'accent doit fonctionner.
- Les 3 dispositions (Dock, Split, Focus) et leurs réglages.
- Le mode démo (apps factices) et le sélecteur d'apps (favori / masquer).
- La météo légère (cache, Wi-Fi uniquement) et le média via MediaSession.
- La lisibilité en voiture : pas de texte fin, pas de contraste faible, pas de micro-boutons.
- Le français, les libellés courts, les explications dans les réglages.

## 9. Prompt prêt à copier-coller

> Tu es designer d'interface et développeur Android senior. Voici le code d'une app Compose
> appelée Car Studio, qui s'affiche sur un écran de voiture 1280 × 720. Je veux que tu
> améliores son design sans changer ses fonctionnalités ni ses contraintes.
>
> Lis d'abord `docs/DESIGN_BRIEF.md`, puis les fichiers `ui/HomeScreen.kt`,
> `ui/components/Common.kt`, `ui/components/Background.kt`, `ui/SettingsScreen.kt`,
> `ui/theme/CarTheme.kt` et `data/ThemeSpec.kt`. Regarde les captures dans
> `docs/screenshots/`.
>
> Objectif : un rendu premium, cohérent, lisible à 60 cm, avec une vraie signature visuelle,
> qui fonctionne avec n'importe quelle couleur d'accent et en mode jour comme en mode nuit.
>
> Livre :
> 1. une critique courte (5 points max) de ce qui ne va pas ;
> 2. le code Kotlin/Compose complet des fichiers modifiés, prêt à remplacer l'existant ;
> 3. les nouveaux tokens (couleurs, rayons, espacements, typographie) ;
> 4. les risques de régression et comment les vérifier.
>
> Contraintes : Compose Material 3, Kotlin 2.0.21, minSdk 26, targetSdk 35, aucune nouvelle
> dépendance lourde, tout en français, cibles tactiles ≥ 56 dp, contraste AA.

## 10. Critères d'acceptation

- L'app compile (`./gradlew assembleDebug`).
- Les 6 captures se régénèrent sans erreur (`./gradlew recordRoborazziDebug`).
- Aucune régression fonctionnelle : dispositions, réglages, apps, météo, média, mode démo.
- Le rendu est meilleur sur les 4 critères : hiérarchie, personnalité, lisibilité, cohérence.

---

## Code source complet (snapshot)
Ces fichiers sont la vérité à modifier. Garde les noms de composables et les signatures publiques.

### `app/src/main/java/com/carstudio/app/data/ThemeSpec.kt`

```kotlin
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
    val clockStyle: String = "digital"
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
                wallpaper = "daylight", glow = 0.25f
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
        "theme" to "Couleurs du thème"
    )
}

```

### `app/src/main/java/com/carstudio/app/data/CarSettings.kt`

```kotlin
package com.carstudio.app.data

import kotlinx.serialization.Serializable

@Serializable
data class CarSettings(
    val theme: ThemeSpec = ThemeSpec(),
    val layout: String = "dock",
    val appsPerRow: Int = 4,
    val maxApps: Int = 12,
    val showClock: Boolean = true,
    val showMedia: Boolean = true,
    val showWeather: Boolean = true,
    val showActions: Boolean = true,
    val showVehicle: Boolean = false,
    val favorites: List<String> = emptyList(),
    val drivingLock: Boolean = true,
    val bigTouch: Boolean = true,
    val reduceMotion: Boolean = false,
    val leftHanded: Boolean = false,
    val nightMode: String = "auto",
    val ambient: Boolean = true,
    val weatherCity: String = "Paris",
    val weatherRefreshMinutes: Int = 60,
    val weatherWifiOnly: Boolean = true,
    val mediaEnabled: Boolean = true,
    val demoMode: Boolean = false,
    val hiddenApps: List<String> = emptyList()
)

```

### `app/src/main/java/com/carstudio/app/ui/theme/CarTheme.kt`

```kotlin
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.carstudio.app.data.ThemeSpec

fun Long.toColor(): Color = Color(this)

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
        LocalDensity provides Density(density.density, density.fontScale * spec.uiScale)
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
        displayLarge = d.displayLarge.copy(fontFamily = family),
        displayMedium = d.displayMedium.copy(fontFamily = family),
        displaySmall = d.displaySmall.copy(fontFamily = family),
        headlineLarge = d.headlineLarge.copy(fontFamily = family),
        headlineMedium = d.headlineMedium.copy(fontFamily = family),
        headlineSmall = d.headlineSmall.copy(fontFamily = family),
        titleLarge = d.titleLarge.copy(fontFamily = family),
        titleMedium = d.titleMedium.copy(fontFamily = family),
        titleSmall = d.titleSmall.copy(fontFamily = family),
        bodyLarge = d.bodyLarge.copy(fontFamily = family),
        bodyMedium = d.bodyMedium.copy(fontFamily = family),
        bodySmall = d.bodySmall.copy(fontFamily = family),
        labelLarge = d.labelLarge.copy(fontFamily = family),
        labelMedium = d.labelMedium.copy(fontFamily = family),
        labelSmall = d.labelSmall.copy(fontFamily = family)
    )
}

private fun shapesFor(corner: Float) = Shapes(
    extraSmall = RoundedCornerShape((corner * 0.25f).dp),
    small = RoundedCornerShape((corner * 0.45f).dp),
    medium = RoundedCornerShape((corner * 0.7f).dp),
    large = RoundedCornerShape(corner.dp),
    extraLarge = RoundedCornerShape((corner * 1.35f).dp)
)

```

### `app/src/main/java/com/carstudio/app/ui/components/Background.kt`

```kotlin
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
                colors.orbA, 620.dp, 0.42f * spec.glow,
                Modifier.align(Alignment.TopEnd).offset(x = 220.dp, y = (-250).dp)
            )
            GlowOrb(
                colors.orbB, 520.dp, 0.34f * spec.glow,
                Modifier.align(Alignment.BottomStart).offset(x = (-180).dp, y = 210.dp)
            )
            GlowOrb(
                colors.orbA, 360.dp, 0.16f * spec.glow,
                Modifier.align(Alignment.CenterStart).offset(x = (-170).dp, y = (-40).dp)
            )
        }
        if (!day) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.22f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.30f)
                            )
                        )
                    )
            )
        }
    }
}

```

### `app/src/main/java/com/carstudio/app/ui/components/Common.kt`

```kotlin
package com.carstudio.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carstudio.app.data.AppEntry

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    tint: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = MaterialTheme.shapes.large
    val ink = MaterialTheme.colorScheme.onSurface
    CompositionLocalProvider(LocalContentColor provides ink) {
        Box(
            modifier
                .shadow(22.dp, shape, clip = false)
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            lerp(tint, Color.White, 0.04f),
                            lerp(tint, Color.Black, 0.03f)
                        )
                    )
                )
                .border(
                    1.dp,
                    Brush.linearGradient(
                        listOf(ink.copy(alpha = 0.18f), ink.copy(alpha = 0.02f))
                    ),
                    shape
                )
        ) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(ink.copy(alpha = 0.045f), Color.Transparent)
                        )
                    )
            )
            Column(Modifier.padding(contentPadding), content = content)
        }
    }
}

@Composable
fun StatusPill(icon: ImageVector, label: String = "") {
    Row(
        Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon, null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            modifier = Modifier.size(16.dp)
        )
        if (label.isNotEmpty()) {
            Spacer(Modifier.width(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )
        }
    }
}

@Composable
fun SectionTitle(text: String, hint: String? = null) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp)) {
        Text(
            text.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.6.sp
        )
        if (hint != null) {
            Text(
                hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
        }
    }
}

@Composable
fun AppIcon(
    app: AppEntry,
    size: Dp,
    shape: Shape,
    modifier: Modifier = Modifier,
    favorite: Boolean = false
) {
    val tint = Color(app.color)
    Box(
        modifier
            .size(size)
            .shadow(
                elevation = 14.dp,
                shape = shape,
                ambientColor = tint.copy(alpha = 0.45f),
                spotColor = tint.copy(alpha = 0.45f)
            )
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        lerp(MaterialTheme.colorScheme.surface, tint, 0.55f),
                        lerp(MaterialTheme.colorScheme.surface, tint, 0.20f)
                    )
                )
            )
            .border(
                1.dp,
                Color.White.copy(alpha = if (favorite) 0.6f else 0.18f),
                shape
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.20f), Color.Transparent)
                    )
                )
        )
        val icon = app.icon
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = app.label,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(size * 0.62f)
            )
        } else {
            Text(
                app.label.take(1).uppercase(),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppTile(
    app: AppEntry,
    iconSize: Dp,
    shape: Shape,
    favorite: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        label = "appTileScale"
    )

    Column(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(MaterialTheme.shapes.medium)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppIcon(app, iconSize, shape, favorite = favorite)
        Spacer(Modifier.height(9.dp))
        Text(
            app.label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SettingCard(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    GlassCard(modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(3.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            if (trailing != null) {
                Spacer(Modifier.width(12.dp))
                trailing()
            }
        }
        if (content != null) {
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun SwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    SettingCard(title, description, trailing = {
        Switch(checked = checked, onCheckedChange = onChange)
    })
}

@Composable
fun SliderRow(
    title: String,
    description: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    onChange: (Float) -> Unit
) {
    SettingCard(title, description) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Slider(
                value = value,
                onValueChange = onChange,
                valueRange = range,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(14.dp))
            Text(
                valueLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun ChoiceRow(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (id, label) ->
            val isSelected = id == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
                    )
                    .clickable { onSelect(id) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun ColorSwatches(
    colors: List<Long>,
    selected: Long,
    onSelect: (Long) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        colors.forEach { value ->
            val isSelected = value == selected
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(value))
                    .border(
                        if (isSelected) 3.dp else 1.dp,
                        if (isSelected) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f),
                        CircleShape
                    )
                    .clickable { onSelect(value) }
            )
        }
    }
}

```

### `app/src/main/java/com/carstudio/app/ui/HomeScreen.kt`

```kotlin
package com.carstudio.app.ui

import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.carstudio.app.CarState
import com.carstudio.app.data.AppEntry
import com.carstudio.app.data.CarSettings
import com.carstudio.app.data.DemoApps
import com.carstudio.app.data.MediaRepository
import com.carstudio.app.data.MediaSnapshot
import com.carstudio.app.data.WeatherCodes
import com.carstudio.app.data.WeatherRepository
import com.carstudio.app.data.WeatherSnapshot
import com.carstudio.app.ui.components.AppTile
import com.carstudio.app.ui.components.GlassCard
import com.carstudio.app.ui.components.StatusPill
import com.carstudio.app.ui.theme.iconShapeFor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun HomeScreen(state: CarState, onOpenSettings: () -> Unit, onOpenApps: () -> Unit = {}) {
    val settings = state.settings
    val apps = if (settings.demoMode) DemoApps.list else state.apps
    val favorites = remember(settings.favorites, apps, settings.maxApps) {
        if (settings.favorites.isEmpty()) {
            apps.take(settings.maxApps)
        } else {
            apps.filter { it.pkg in settings.favorites }
                .take(settings.maxApps)
                .ifEmpty { apps.take(settings.maxApps) }
        }
    }
    val clock = rememberClock()

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxWidth < 720.dp
        val horizontal = if (compact) 16.dp else 26.dp
        Column(Modifier.fillMaxSize().padding(horizontal = horizontal, vertical = 18.dp)) {
            TopBar(clock, settings, onOpenSettings)
            Spacer(Modifier.height(14.dp))
            when (settings.layout) {
                "split" -> SplitLayout(state, favorites, clock, onOpenApps, Modifier.weight(1f))
                "focus" -> FocusLayout(state, favorites, clock, Modifier.weight(1f))
                else -> DockLayout(state, favorites, clock, onOpenApps, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun rememberClock(): Calendar {
    var now by remember { mutableStateOf(Calendar.getInstance()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Calendar.getInstance()
            delay(1000L)
        }
    }
    return now
}

@Composable
private fun rememberBattery(): Int {
    val context = LocalContext.current
    var level by remember { mutableStateOf(-1) }
    LaunchedEffect(Unit) {
        while (true) {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val raw = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            level = if (raw >= 0 && scale > 0) raw * 100 / scale else -1
            delay(60_000L)
        }
    }
    return level
}

@Composable
private fun rememberMedia(): MediaSnapshot? {
    val context = LocalContext.current
    var snapshot by remember { mutableStateOf<MediaSnapshot?>(null) }
    LaunchedEffect(Unit) {
        while (true) {
            snapshot = withContext(Dispatchers.IO) { MediaRepository.snapshot(context) }
            delay(2000L)
        }
    }
    return snapshot
}

@Composable
private fun rememberWeather(state: CarState): WeatherSnapshot? {
    val context = LocalContext.current
    val repository = remember { WeatherRepository(context) }
    val settings = state.settings
    var snapshot by remember { mutableStateOf(repository.cached()) }

    LaunchedEffect(settings.weatherCity, settings.weatherRefreshMinutes, settings.weatherWifiOnly) {
        while (true) {
            snapshot = repository.cached()
            val allowed = !settings.weatherWifiOnly || WeatherRepository.isWifi(context)
            if (allowed && repository.isStale(snapshot, settings.weatherRefreshMinutes)) {
                repository.refresh(settings.weatherCity)?.let { snapshot = it }
            }
            delay(5 * 60_000L)
        }
    }
    return snapshot
}

private fun formatTime(clock: Calendar, style: String): String {
    val pattern = if (style == "minimal") "H:mm" else "HH:mm"
    return SimpleDateFormat(pattern, Locale.FRANCE).format(clock.time)
}

private fun formatDate(clock: Calendar): String =
    SimpleDateFormat("EEEE d MMMM", Locale.FRANCE)
        .format(clock.time)
        .replaceFirstChar { it.uppercase() }

private fun greeting(clock: Calendar): String = when (clock.get(Calendar.HOUR_OF_DAY)) {
    in 5..11 -> "Bonjour"
    in 12..17 -> "Bon après-midi"
    in 18..22 -> "Bonsoir"
    else -> "Bonne nuit"
}

@Composable
private fun TopBar(clock: Calendar, settings: CarSettings, onOpenSettings: () -> Unit) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val battery = rememberBattery()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (settings.showClock) {
            Column {
                Text(
                    formatTime(clock, settings.theme.clockStyle),
                    style = MaterialTheme.typography.displaySmall,
                    color = onSurface
                )
                Text(
                    formatDate(clock),
                    style = MaterialTheme.typography.labelLarge,
                    color = onSurface.copy(alpha = 0.55f)
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusPill(Icons.Rounded.Wifi, "AA")
            StatusPill(Icons.Rounded.BatteryFull, if (battery >= 0) "$battery %" else "—")
            Box(
                Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(onSurface.copy(alpha = 0.06f))
                    .border(1.dp, onSurface.copy(alpha = 0.10f), CircleShape)
                    .clickable(onClick = onOpenSettings),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Tune, contentDescription = "Réglages")
            }
        }
    }
}

@Composable
private fun DockLayout(
    state: CarState,
    apps: List<AppEntry>,
    clock: Calendar,
    onOpenApps: () -> Unit,
    modifier: Modifier
) {
    val settings = state.settings
    Column(modifier) {
        if (settings.showMedia || settings.showWeather) {
            Row(
                Modifier.fillMaxWidth().height(200.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when {
                    settings.showMedia && settings.showWeather -> {
                        MediaCard(state, Modifier.weight(1.6f).fillMaxHeight())
                        WeatherCard(state, Modifier.weight(0.75f).fillMaxHeight())
                    }
                    settings.showMedia -> MediaCard(state, Modifier.fillMaxWidth().fillMaxHeight())
                    else -> WeatherCard(state, Modifier.fillMaxWidth().fillMaxHeight())
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        AppGrid(apps, state, Modifier.weight(1f))
        if (settings.showActions) {
            Spacer(Modifier.height(12.dp))
            QuickActions(state, onOpenApps)
        }
    }
}

@Composable
private fun SplitLayout(
    state: CarState,
    apps: List<AppEntry>,
    clock: Calendar,
    onOpenApps: () -> Unit,
    modifier: Modifier
) {
    val settings = state.settings
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        Column(Modifier.weight(0.40f).fillMaxHeight()) {
            ClockCard(clock, settings)
            if (settings.showWeather) {
                Spacer(Modifier.height(14.dp))
                WeatherCard(state, Modifier.fillMaxWidth().height(150.dp))
            }
            if (settings.showMedia) {
                Spacer(Modifier.height(14.dp))
                MediaCard(state, Modifier.fillMaxWidth().weight(1f))
            }
            if (settings.showVehicle) {
                Spacer(Modifier.height(14.dp))
                VehicleCard(Modifier.fillMaxWidth().height(130.dp))
            }
        }
        Column(Modifier.weight(0.60f).fillMaxHeight()) {
            AppGrid(apps, state, Modifier.weight(1f))
            if (settings.showActions) {
                Spacer(Modifier.height(12.dp))
                QuickActions(state, onOpenApps)
            }
        }
    }
}

@Composable
private fun FocusLayout(
    state: CarState,
    apps: List<AppEntry>,
    clock: Calendar,
    modifier: Modifier
) {
    val settings = state.settings
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().height(250.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ClockHero(clock, settings, Modifier.weight(1f).fillMaxHeight())
            if (settings.showWeather) {
                WeatherHero(state, Modifier.width(320.dp).fillMaxHeight())
            }
        }
        if (settings.showMedia || settings.showVehicle) {
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.fillMaxWidth().height(150.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (settings.showMedia) {
                    MediaCard(state, Modifier.weight(1f).fillMaxHeight())
                }
                if (settings.showVehicle) {
                    VehicleCard(Modifier.width(280.dp).fillMaxHeight())
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            apps.forEach { app ->
                AppTile(
                    app = app,
                    iconSize = 72.dp,
                    shape = iconShapeFor(settings.theme.iconShape, settings.theme.corner),
                    favorite = app.pkg in settings.favorites,
                    onClick = { state.launch(app.pkg) },
                    onLongClick = { state.toggleFavorite(app.pkg) },
                    modifier = Modifier.width(104.dp)
                )
            }
        }
    }
}

@Composable
private fun ClockHero(clock: Calendar, settings: CarSettings, modifier: Modifier = Modifier) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    GlassCard(modifier) {
        Text(
            greeting(clock),
            style = MaterialTheme.typography.labelLarge,
            color = onSurface.copy(alpha = 0.55f)
        )
        Spacer(Modifier.weight(1f))
        if (settings.theme.clockStyle == "analog") {
            AnalogClock(Modifier.size(170.dp))
        } else {
            Text(
                formatTime(clock, settings.theme.clockStyle),
                style = MaterialTheme.typography.displayLarge,
                color = onSurface
            )
            Text(
                formatDate(clock),
                style = MaterialTheme.typography.titleMedium,
                color = onSurface.copy(alpha = 0.55f)
            )
        }
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusPill(Icons.Rounded.Wifi, "Android Auto")
            StatusPill(Icons.Rounded.BatteryFull, "En ligne")
        }
    }
}

@Composable
private fun WeatherHero(state: CarState, modifier: Modifier = Modifier) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val weather = rememberWeather(state)
    GlassCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.Cloud, null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Météo",
                style = MaterialTheme.typography.labelLarge,
                color = onSurface.copy(alpha = 0.6f)
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            if (weather != null) "${weather.temperature.roundToInt()}°" else "--°",
            style = MaterialTheme.typography.displayLarge,
            color = if (weather != null) onSurface else onSurface.copy(alpha = 0.45f)
        )
        Text(
            if (weather != null) WeatherCodes.label(weather.code) else "À connecter",
            style = MaterialTheme.typography.titleMedium,
            color = onSurface.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            if (weather != null) "${weather.city} · ressenti ${weather.apparent.roundToInt()}°"
            else "Réglages → Météo",
            style = MaterialTheme.typography.bodySmall,
            color = onSurface.copy(alpha = 0.45f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.weight(1f))
        if (weather != null) {
            Text(
                "Mis à jour à " + SimpleDateFormat("HH:mm", Locale.FRANCE).format(Date(weather.updatedAt)),
                style = MaterialTheme.typography.labelSmall,
                color = onSurface.copy(alpha = 0.35f)
            )
        }
    }
}

@Composable
private fun ClockCard(clock: Calendar, settings: CarSettings) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    GlassCard {
        if (settings.theme.clockStyle == "analog") {
            AnalogClock(Modifier.size(110.dp))
        } else {
            Text(
                formatTime(clock, settings.theme.clockStyle),
                style = MaterialTheme.typography.displayMedium,
                color = onSurface
            )
            Text(
                formatDate(clock),
                style = MaterialTheme.typography.labelLarge,
                color = onSurface.copy(alpha = 0.55f)
            )
        }
    }
}

@Composable
private fun AnalogClock(modifier: Modifier = Modifier) {
    val clock = rememberClock()
    val color = MaterialTheme.colorScheme.onSurface
    val primary = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        val radius = size.minDimension / 2f
        val middle = center
        drawCircle(color.copy(alpha = 0.10f), radius = radius, center = middle)
        drawCircle(
            color.copy(alpha = 0.25f),
            radius = radius,
            center = middle,
            style = Stroke(width = 2f)
        )

        fun hand(angleDeg: Float, length: Float, width: Float, tint: Color) {
            val rad = Math.toRadians((angleDeg - 90f).toDouble())
            drawLine(
                tint,
                middle,
                Offset(
                    middle.x + (length * cos(rad)).toFloat(),
                    middle.y + (length * sin(rad)).toFloat()
                ),
                strokeWidth = width,
                cap = StrokeCap.Round
            )
        }

        val hour = clock.get(Calendar.HOUR).toFloat() + clock.get(Calendar.MINUTE) / 60f
        val minute = clock.get(Calendar.MINUTE).toFloat()
        val second = clock.get(Calendar.SECOND).toFloat()

        hand(hour * 30f, radius * 0.50f, 7f, color)
        hand(minute * 6f, radius * 0.72f, 4f, color)
        hand(second * 6f, radius * 0.82f, 2f, primary)
        drawCircle(primary, radius = 5f, center = middle)
    }
}

@Composable
private fun MediaCard(state: CarState, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val media = rememberMedia()
    val hasAccess = remember { MediaRepository.hasAccess(context) }
    val onSurface = MaterialTheme.colorScheme.onSurface

    BoxWithConstraints(modifier) {
        val tall = maxHeight > 260.dp
        GlassCard(Modifier.fillMaxSize()) {
            if (tall) {
                TallMedia(media, hasAccess, context, onSurface)
            } else {
                WideMedia(media, hasAccess, context, onSurface)
            }
        }
    }
}

@Composable
private fun ColumnScope.WideMedia(
    media: MediaSnapshot?,
    hasAccess: Boolean,
    context: android.content.Context,
    onSurface: Color
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        MediaArt(media, 62.dp)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                media?.title ?: "Aucun média",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                media?.artist?.ifBlank { "Lecture en cours" }
                    ?: if (hasAccess) "Lance une app de musique sur le téléphone"
                    else "Autorise l'accès aux notifications dans les Réglages",
                style = MaterialTheme.typography.bodySmall,
                color = onSurface.copy(alpha = 0.55f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
        Icon(
            Icons.Rounded.MusicNote, null,
            modifier = Modifier.size(96.dp),
            tint = onSurface.copy(alpha = 0.05f)
        )
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { MediaRepository.previous(context) }) {
            Icon(Icons.Rounded.SkipPrevious, null)
        }
        Spacer(Modifier.width(6.dp))
        PlayButton(media?.playing == true) { MediaRepository.playPause(context) }
        Spacer(Modifier.width(6.dp))
        IconButton(onClick = { MediaRepository.next(context) }) {
            Icon(Icons.Rounded.SkipNext, null)
        }
    }
}

@Composable
private fun ColumnScope.TallMedia(
    media: MediaSnapshot?,
    hasAccess: Boolean,
    context: android.content.Context,
    onSurface: Color
) {
    Spacer(Modifier.weight(1f))
    MediaArt(media, 132.dp, Modifier.align(Alignment.CenterHorizontally))
    Spacer(Modifier.height(16.dp))
    Text(
        media?.title ?: "Aucun média",
        style = MaterialTheme.typography.titleLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.align(Alignment.CenterHorizontally)
    )
    Text(
        media?.artist?.ifBlank { "Lecture en cours" }
            ?: if (hasAccess) "Lance une app de musique" else "Autorise l'accès aux notifications",
        style = MaterialTheme.typography.bodySmall,
        color = onSurface.copy(alpha = 0.55f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.align(Alignment.CenterHorizontally)
    )
    Spacer(Modifier.weight(1f))
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { MediaRepository.previous(context) }) {
            Icon(Icons.Rounded.SkipPrevious, null)
        }
        Spacer(Modifier.width(6.dp))
        PlayButton(media?.playing == true) { MediaRepository.playPause(context) }
        Spacer(Modifier.width(6.dp))
        IconButton(onClick = { MediaRepository.next(context) }) {
            Icon(Icons.Rounded.SkipNext, null)
        }
    }
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun MediaArt(media: MediaSnapshot?, size: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    val art = media?.art
    Box(
        modifier
            .size(size)
            .clip(MaterialTheme.shapes.large)
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (art != null) {
            Image(
                bitmap = art,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(Icons.Rounded.MusicNote, null, tint = Color.White)
        }
    }
}

@Composable
private fun PlayButton(playing: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            null,
            tint = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
private fun WeatherCard(state: CarState, modifier: Modifier = Modifier) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val weather = rememberWeather(state)
    GlassCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.Cloud, null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Météo",
                style = MaterialTheme.typography.labelLarge,
                color = onSurface.copy(alpha = 0.6f)
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            if (weather != null) "${weather.temperature.roundToInt()}°" else "--°",
            style = MaterialTheme.typography.displaySmall,
            color = if (weather != null) onSurface else onSurface.copy(alpha = 0.45f)
        )
        Text(
            if (weather != null) WeatherCodes.label(weather.code) else "À connecter",
            style = MaterialTheme.typography.bodyMedium,
            color = onSurface.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            if (weather != null) "${weather.city} · ressenti ${weather.apparent.roundToInt()}°"
            else "Réglages → Météo",
            style = MaterialTheme.typography.labelSmall,
            color = onSurface.copy(alpha = 0.42f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun VehicleCard(modifier: Modifier = Modifier) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    GlassCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.Speed, null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Véhicule",
                style = MaterialTheme.typography.labelLarge,
                color = onSurface.copy(alpha = 0.6f)
            )
        }
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Metric("Vitesse", "--", "km/h")
            Metric("Régime", "--", "tr/min")
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "OBD-II à connecter",
            style = MaterialTheme.typography.bodySmall,
            color = onSurface.copy(alpha = 0.45f)
        )
    }
}

@Composable
private fun Metric(label: String, value: String, unit: String) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.width(3.dp))
            Text(
                unit,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun AppGrid(apps: List<AppEntry>, state: CarState, modifier: Modifier = Modifier) {
    val settings = state.settings
    if (apps.isEmpty()) {
        Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                if (state.loading) "Chargement des apps…" else "Aucune app trouvée",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
        return
    }

    val iconSize = when {
        settings.appsPerRow >= 6 -> 50.dp
        settings.appsPerRow == 5 -> 60.dp
        settings.appsPerRow == 4 -> 80.dp
        else -> 92.dp
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(settings.appsPerRow),
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically)
    ) {
        items(apps, key = { it.pkg }) { app ->
            AppTile(
                app = app,
                iconSize = iconSize,
                shape = iconShapeFor(settings.theme.iconShape, settings.theme.corner),
                favorite = app.pkg in settings.favorites,
                onClick = { state.launch(app.pkg) },
                onLongClick = { state.toggleFavorite(app.pkg) }
            )
        }
    }
}

@Composable
private fun QuickActions(
    state: CarState,
    onOpenApps: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ActionPill(Icons.Rounded.Navigation, "Navigation", Modifier.weight(1f)) {}
        ActionPill(Icons.Rounded.MusicNote, "Musique", Modifier.weight(1f)) {}
        ActionPill(Icons.Rounded.Phone, "Téléphone", Modifier.weight(1f)) {}
        ActionPill(Icons.Rounded.Apps, "Toutes les apps", Modifier.weight(1f), onOpenApps)
    }
}

@Composable
private fun ActionPill(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    Row(
        modifier
            .clip(CircleShape)
            .background(primary.copy(alpha = 0.10f))
            .border(1.dp, primary.copy(alpha = 0.20f), CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = onSurface.copy(alpha = 0.9f),
            maxLines = 1
        )
    }
}

```

### `app/src/main/java/com/carstudio/app/ui/AppPickerScreen.kt`

```kotlin
package com.carstudio.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.carstudio.app.CarState
import com.carstudio.app.data.AppEntry
import com.carstudio.app.data.DemoApps
import com.carstudio.app.ui.components.AppIcon
import com.carstudio.app.ui.components.GlassCard
import com.carstudio.app.ui.theme.iconShapeFor

@Composable
fun AppPickerScreen(state: CarState, onClose: () -> Unit) {
    val settings = state.settings
    var query by remember { mutableStateOf("") }
    val source = if (settings.demoMode && state.allApps.isEmpty()) DemoApps.list else state.allApps
    val apps = remember(source, query) {
        if (query.isBlank()) source
        else source.filter {
            it.label.contains(query, ignoreCase = true) ||
                it.pkg.contains(query, ignoreCase = true)
        }
    }
    val shape = iconShapeFor(settings.theme.iconShape, settings.theme.corner)

    Column(Modifier.fillMaxSize().padding(horizontal = 26.dp, vertical = 18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Applications", style = MaterialTheme.typography.displaySmall)
                Text(
                    "Étoile = favori · œil = afficher ou masquer",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, contentDescription = "Fermer")
            }
        }
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = CircleShape,
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            placeholder = { Text("Rechercher une app…") }
        )
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${apps.size} app" + if (apps.size > 1) "s" else "",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Spacer(Modifier.weight(1f))
            Text(
                "${settings.favorites.size} favori" + if (settings.favorites.size > 1) "s" else "",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            items(apps, key = { it.pkg }) { app ->
                AppPickerRow(
                    app = app,
                    shape = shape,
                    favorite = app.pkg in settings.favorites,
                    hidden = app.pkg in settings.hiddenApps,
                    onLaunch = { state.launch(app.pkg) },
                    onFavorite = { state.toggleFavorite(app.pkg) },
                    onHidden = { state.toggleHidden(app.pkg) }
                )
            }
        }
    }
}

@Composable
private fun AppPickerRow(
    app: AppEntry,
    shape: androidx.compose.ui.graphics.Shape,
    favorite: Boolean,
    hidden: Boolean,
    onLaunch: () -> Unit,
    onFavorite: () -> Unit,
    onHidden: () -> Unit
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val primary = MaterialTheme.colorScheme.primary
    GlassCard(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(app, 44.dp, shape, favorite = favorite)
            Spacer(Modifier.width(12.dp))
            Column(
                Modifier
                    .weight(1f)
                    .clickable(onClick = onLaunch)
            ) {
                Text(
                    app.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = onSurface.copy(alpha = if (hidden) 0.45f else 1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    app.pkg,
                    style = MaterialTheme.typography.labelSmall,
                    color = onSurface.copy(alpha = 0.35f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onFavorite) {
                Icon(
                    if (favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    contentDescription = if (favorite) "Retirer des favoris" else "Ajouter aux favoris",
                    modifier = Modifier.size(20.dp),
                    tint = if (favorite) primary else onSurface.copy(alpha = 0.4f)
                )
            }
            IconButton(onClick = onHidden) {
                Icon(
                    if (hidden) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = if (hidden) "Afficher" else "Masquer",
                    modifier = Modifier.size(20.dp),
                    tint = if (hidden) primary else onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }
}

```

### `app/src/main/java/com/carstudio/app/ui/SettingsScreen.kt`

```kotlin
package com.carstudio.app.ui

import androidx.compose.ui.platform.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.carstudio.app.CarState
import com.carstudio.app.data.CarSettings
import com.carstudio.app.data.MediaRepository
import com.carstudio.app.data.Presets
import com.carstudio.app.data.ThemePreset
import com.carstudio.app.data.Wallpapers
import com.carstudio.app.ui.components.*
import com.carstudio.app.ui.theme.iconShapeFor

private data class SettingItem(
    val title: String,
    val description: String,
    val keywords: String = "",
    val body: @Composable () -> Unit
)

private data class SettingSection(
    val title: String,
    val hint: String?,
    val items: List<SettingItem>
)

@Composable
fun SettingsScreen(
    state: CarState,
    onClose: () -> Unit,
    onOpenAppPicker: () -> Unit = {}
) {
    val settings = state.settings
    var query by remember { mutableStateOf("") }
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    val sections = settingsSections(state, settings, clipboard, context, onOpenAppPicker)
    val visible = sections.mapNotNull { section ->
        val matches = section.items.filter { item ->
            query.isBlank() || (item.title + " " + item.description + " " + item.keywords)
                .contains(query, ignoreCase = true)
        }
        if (matches.isEmpty()) null else section.copy(items = matches)
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 22.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Réglages", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Chaque option est expliquée. Touche un titre pour la relire.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, contentDescription = "Fermer")
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = CircleShape,
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            placeholder = { Text("Rechercher un réglage…") }
        )

        Spacer(Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 28.dp)
        ) {
            if (query.isBlank()) {
                item { PreviewCard(settings) }
            }
            visible.forEach { section ->
                item { SectionTitle(section.title, section.hint) }
                items(section.items) { item -> item.body() }
            }
            if (visible.isEmpty()) {
                item {
                    Text(
                        "Aucun réglage ne correspond à « $query ».",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        modifier = Modifier.padding(vertical = 20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun settingsSections(
    state: CarState,
    s: CarSettings,
    clipboard: ClipboardManager,
    context: Context,
    onOpenAppPicker: () -> Unit
): List<SettingSection> {
    fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

    val appearance = SettingSection(
        "Apparence",
        "Les couleurs, les formes et la typographie de toute l'interface.",
        listOf(
            SettingItem(
                "Style de départ",
                "Un thème complet en un geste. Tu peux ensuite modifier chaque couleur.",
                "preset thème palette"
            ) {
                SettingCard("Style de départ", "Choisis une base, puis affine plus bas.") {
                    PresetChips(s.theme.preset) { preset ->
                        state.update { it.copy(theme = preset.spec) }
                    }
                }
            },
            SettingItem(
                "Couleur d'accent",
                "La teinte des boutons, des halos et des éléments actifs. C'est la couleur qui donne le caractère de l'écran.",
                "couleur accent primaire"
            ) {
                SettingCard("Couleur d'accent", "La teinte principale de l'interface.") {
                    ColorSwatches(ACCENTS, s.theme.accent) { color ->
                        state.updateTheme { it.copy(accent = color) }
                    }
                }
            },
            SettingItem(
                "Couleur secondaire",
                "Utilisée dans les dégradés et les reflets, en complément de l'accent.",
                "couleur secondaire dégradé"
            ) {
                SettingCard("Couleur secondaire", "Pour les dégradés et les reflets.") {
                    ColorSwatches(ACCENTS, s.theme.secondary) { color ->
                        state.updateTheme { it.copy(secondary = color) }
                    }
                }
            },
            SettingItem(
                "Fond",
                "La couleur de base derrière les cartes. Un fond très sombre fait ressortir les halos.",
                "fond background noir"
            ) {
                SettingCard("Fond", "La base derrière les cartes.") {
                    ColorSwatches(BACKGROUNDS, s.theme.background) { color ->
                        state.updateTheme { it.copy(background = color) }
                    }
                }
            },
            SettingItem(
                "Angles",
                "Arrondi des cartes et des icônes. 0 = angles droits, 48 = très arrondi.",
                "angle arrondi rayon corner"
            ) {
                SliderRow(
                    "Angles",
                    "Arrondi des cartes et des icônes.",
                    s.theme.corner,
                    0f..48f,
                    "${s.theme.corner.toInt()} dp"
                ) { value -> state.updateTheme { it.copy(corner = value) } }
            },
            SettingItem(
                "Intensité des halos",
                "Ces lueurs colorées dans les coins. Plus c'est haut, plus l'écran paraît vivant ; 0 les éteint.",
                "halo lueur glow lumière"
            ) {
                SliderRow(
                    "Intensité des halos",
                    "Les lueurs colorées dans les coins.",
                    s.theme.glow,
                    0f..1f,
                    "${(s.theme.glow * 100).toInt()} %"
                ) { value -> state.updateTheme { it.copy(glow = value) } }
            },
            SettingItem(
                "Taille de l'interface",
                "Agrandit ou réduit tout le texte et les espacements. Utile si l'écran de la voiture est loin.",
                "taille échelle zoom police texte accessibilité"
            ) {
                SliderRow(
                    "Taille de l'interface",
                    "Agrandit tout le texte et les espacements.",
                    s.theme.uiScale,
                    0.8f..1.35f,
                    "${(s.theme.uiScale * 100).toInt()} %"
                ) { value -> state.updateTheme { it.copy(uiScale = value) } }
            },
            SettingItem(
                "Police",
                "Change la famille de caractères. La police système reste la plus lisible en voiture.",
                "police typographie font"
            ) {
                SettingCard("Police", "La famille de caractères de l'interface.") {
                    ChoiceRow(
                        listOf(
                            "sans" to "Sans",
                            "mono" to "Mono",
                            "serif" to "Serif",
                            "cursive" to "Manuscrite"
                        ),
                        s.theme.font
                    ) { value -> state.updateTheme { it.copy(font = value) } }
                }
            },
            SettingItem(
                "Forme des icônes",
                "Le contour des tuiles d'applications.",
                "icône forme carré cercle arrondi"
            ) {
                SettingCard("Forme des icônes", "Le contour des tuiles d'applications.") {
                    ChoiceRow(
                        listOf(
                            "squircle" to "Squircle",
                            "rounded" to "Arrondi",
                            "circle" to "Cercle",
                            "square" to "Carré"
                        ),
                        s.theme.iconShape
                    ) { value -> state.updateTheme { it.copy(iconShape = value) } }
                }
            },
            SettingItem(
                "Fond d'écran",
                "L'ambiance derrière les cartes. « Couleurs du thème » suit automatiquement ton accent.",
                "fond wallpaper décor"
            ) {
                SettingCard("Fond d'écran", "L'ambiance derrière les cartes.") {
                    Chips(Wallpapers.all, s.theme.wallpaper) { value ->
                        state.updateTheme { it.copy(wallpaper = value) }
                    }
                }
            },
            SettingItem(
                "Dégradé",
                "Fond dégradé (activé) ou uni (désactivé).",
                "dégradé gradient fond"
            ) {
                SwitchRow(
                    "Dégradé",
                    "Fond dégradé plutôt qu'uni.",
                    s.theme.gradient
                ) { value -> state.updateTheme { it.copy(gradient = value) } }
            },
            SettingItem(
                "Horloge",
                "Digitale (00:00), minimale (0:00) ou analogique (aiguilles).",
                "horloge montre clock heure"
            ) {
                SettingCard("Horloge", "Le style d'affichage de l'heure.") {
                    ChoiceRow(
                        listOf(
                            "digital" to "Digitale",
                            "minimal" to "Minimale",
                            "analog" to "Analogique"
                        ),
                        s.theme.clockStyle
                    ) { value -> state.updateTheme { it.copy(clockStyle = value) } }
                }
            }
        )
    )

    val home = SettingSection(
        "Accueil",
        "La disposition de l'écran et les widgets visibles.",
        listOf(
            SettingItem(
                "Disposition",
                "Dock : une grande carte média puis la grille. Split : colonne d'infos à gauche, apps à droite. Focus : une seule grande carte.",
                "disposition layout accueil grille"
            ) {
                SettingCard("Disposition", "Comment l'écran est organisé.") {
                    ChoiceRow(
                        listOf("dock" to "Dock", "split" to "Split", "focus" to "Focus"),
                        s.layout
                    ) { value -> state.update { it.copy(layout = value) } }
                }
            },
            SettingItem(
                "Apps par ligne",
                "Nombre de colonnes dans la grille. Moins de colonnes = icônes plus grosses, plus faciles à viser.",
                "colonnes grille apps ligne"
            ) {
                SliderRow(
                    "Apps par ligne",
                    "Moins de colonnes = icônes plus grosses.",
                    s.appsPerRow.toFloat(),
                    3f..6f,
                    s.appsPerRow.toString()
                ) { value -> state.update { it.copy(appsPerRow = value.toInt()) } }
            },
            SettingItem(
                "Nombre d'apps affichées",
                "Combien de tuiles au maximum sur l'accueil. Les apps favorites passent en premier.",
                "nombre maximum apps tuiles"
            ) {
                SliderRow(
                    "Nombre d'apps affichées",
                    "Le maximum de tuiles sur l'accueil.",
                    s.maxApps.toFloat(),
                    4f..20f,
                    s.maxApps.toString()
                ) { value -> state.update { it.copy(maxApps = value.toInt()) } }
            },
            SettingItem(
                "Afficher l'horloge",
                "L'heure et la date en haut de l'écran.",
                "horloge date"
            ) {
                SwitchRow("Afficher l'horloge", "L'heure et la date en haut.", s.showClock) { value ->
                    state.update { it.copy(showClock = value) }
                }
            },
            SettingItem(
                "Afficher le média",
                "La carte du lecteur de musique. Elle est vide tant qu'aucun lecteur n'est connecté.",
                "média musique lecteur"
            ) {
                SwitchRow("Afficher le média", "La carte du lecteur de musique.", s.showMedia) { value ->
                    state.update { it.copy(showMedia = value) }
                }
            },
            SettingItem(
                "Afficher la météo",
                "La carte météo. Les données arrivent dans la prochaine version.",
                "météo temps"
            ) {
                SwitchRow("Afficher la météo", "La carte météo.", s.showWeather) { value ->
                    state.update { it.copy(showWeather = value) }
                }
            },
            SettingItem(
                "Afficher le véhicule",
                "Vitesse, régime et températures via un adaptateur OBD-II (à venir).",
                "véhicule obd vitesse"
            ) {
                SwitchRow("Afficher le véhicule", "Les données OBD-II (à venir).", s.showVehicle) { value ->
                    state.update { it.copy(showVehicle = value) }
                }
            },
            SettingItem(
                "Afficher les actions rapides",
                "La rangée de raccourcis en bas : navigation, musique, téléphone, apps.",
                "actions rapides raccourcis barre"
            ) {
                SwitchRow(
                    "Afficher les actions rapides",
                    "La rangée de raccourcis en bas.",
                    s.showActions
                ) { value -> state.update { it.copy(showActions = value) } }
            },
            SettingItem(
                "Mode démo",
                "Affiche des apps factices pour juger le rendu sans voiture. À désactiver pour retrouver tes vraies apps.",
                "démo test aperçu"
            ) {
                SwitchRow(
                    "Mode démo",
                    "Des apps factices pour tester le rendu.",
                    s.demoMode
                ) { value -> state.update { it.copy(demoMode = value) } }
            }
        )
    )

    val applications = SettingSection(
        "Applications",
        "Choisis ce qui apparaît sur l'accueil et ce qui reste caché.",
        listOf(
            SettingItem(
                "Choisir les apps",
                "Ouvre la liste complète : une étoile pour les favoris, un œil pour afficher ou masquer une app.",
                "apps applications favoris masquer liste"
            ) {
                SettingCard(
                    "Choisir les apps",
                    "${state.allApps.size} apps détectées · ${s.favorites.size} favoris · ${s.hiddenApps.size} masquées"
                ) {
                    Button(onClick = onOpenAppPicker) {
                        Text("Ouvrir la liste")
                    }
                }
            }
        )
    )

    val weather = SettingSection(
        "Météo",
        "Des données réelles, mais légères : quelques kilo-octets par mise à jour.",
        listOf(
            SettingItem(
                "Ville",
                "La ville utilisée pour la météo. La position est mémorisée après la première recherche, donc une seule requête de géocodage.",
                "ville météo localisation"
            ) {
                var city by remember(s.weatherCity) { mutableStateOf(s.weatherCity) }
                SettingCard("Ville", "La position est mémorisée après la première recherche.") {
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        singleLine = true,
                        label = { Text("Ville") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = {
                        state.update { it.copy(weatherCity = city.trim().ifBlank { "Paris" }) }
                        toast("Ville enregistrée")
                    }) {
                        Text("Enregistrer")
                    }
                }
            },
            SettingItem(
                "Fréquence de mise à jour",
                "Plus c'est espacé, moins ça consomme. 60 minutes suffisent largement pour la météo.",
                "fréquence rafraîchir intervalle données"
            ) {
                SliderRow(
                    "Fréquence de mise à jour",
                    "Un appel réseau toutes les X minutes.",
                    s.weatherRefreshMinutes.toFloat(),
                    15f..180f,
                    "${s.weatherRefreshMinutes} min"
                ) { value -> state.update { it.copy(weatherRefreshMinutes = value.toInt()) } }
            },
            SettingItem(
                "Wi-Fi uniquement",
                "Activé, la météo ne se télécharge jamais en données mobiles : elle reste sur le dernier relevé hors Wi-Fi.",
                "wifi données mobiles économie"
            ) {
                SwitchRow(
                    "Wi-Fi uniquement",
                    "Aucune mise à jour en données mobiles.",
                    s.weatherWifiOnly
                ) { value -> state.update { it.copy(weatherWifiOnly = value) } }
            }
        )
    )

    val media = SettingSection(
        "Média",
        "Le lecteur en cours : YouTube Music, Spotify, VLC…",
        listOf(
            SettingItem(
                "Accès aux médias",
                "Car Studio lit uniquement le titre, l'artiste et la pochette du lecteur actif. Aucune notification n'est lue, stockée ou envoyée.",
                "média notification autorisation lecteur"
            ) {
                val granted = MediaRepository.hasAccess(context)
                SettingCard(
                    "Accès aux médias",
                    if (granted) "Accès accordé : le lecteur peut s'afficher."
                    else "Accès non accordé : la carte média restera vide."
                ) {
                    Text(
                        if (granted) "Tout est prêt." else "Ouvre les réglages, puis autorise Car Studio dans « Accès aux notifications ».",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }) {
                        Text(if (granted) "Vérifier l'autorisation" else "Ouvrir les réglages")
                    }
                }
            }
        )
    )

    val driving = SettingSection(
        "Conduite",
        "Sécurité et confort pendant que tu roules.",
        listOf(
            SettingItem(
                "Mode conduite",
                "Verrouille les interactions quand la voiture roule. Recommandé : la loi interdit de manipuler l'écran en mouvement.",
                "conduite sécurité verrou roulage"
            ) {
                SwitchRow(
                    "Mode conduite",
                    "Verrouille les interactions en roulant.",
                    s.drivingLock
                ) { value -> state.update { it.copy(drivingLock = value) } }
            },
            SettingItem(
                "Grandes cibles tactiles",
                "Agrandit les zones cliquables pour viser sans regarder.",
                "tactile gros doigt cible accessibilité"
            ) {
                SwitchRow(
                    "Grandes cibles tactiles",
                    "Agrandit les zones cliquables.",
                    s.bigTouch
                ) { value -> state.update { it.copy(bigTouch = value) } }
            },
            SettingItem(
                "Réduire les animations",
                "Supprime les transitions. Utile si l'écran saccade ou si tu préfères un rendu net.",
                "animation mouvement transition"
            ) {
                SwitchRow(
                    "Réduire les animations",
                    "Supprime les transitions.",
                    s.reduceMotion
                ) { value -> state.update { it.copy(reduceMotion = value) } }
            },
            SettingItem(
                "Gaucher",
                "Place les commandes principales à gauche pour une voiture à conduite à droite.",
                "gaucher gauche droite volant"
            ) {
                SwitchRow("Gaucher", "Commandes principales à gauche.", s.leftHanded) { value ->
                    state.update { it.copy(leftHanded = value) }
                }
            },
            SettingItem(
                "Jour / nuit",
                "Auto suit l'heure et la luminosité. Jour force le thème clair, nuit force le sombre.",
                "nuit jour sombre clair thème"
            ) {
                SettingCard("Jour / nuit", "Auto suit l'heure et la luminosité.") {
                    ChoiceRow(
                        listOf("auto" to "Auto", "day" to "Jour", "night" to "Nuit"),
                        s.nightMode
                    ) { value -> state.update { it.copy(nightMode = value) } }
                }
            }
        )
    )

    val data = SettingSection(
        "Données",
        "Sauvegarde ton thème ou remets tout à zéro.",
        listOf(
            SettingItem(
                "Exporter le thème",
                "Copie toute la configuration (thème + disposition) dans le presse-papiers, en JSON. Colle-la où tu veux.",
                "export sauvegarde copie json"
            ) {
                SettingCard("Exporter le thème", "Copie la configuration en JSON.") {
                    Button(onClick = {
                        clipboard.setText(AnnotatedString(state.exportJson()))
                        toast("Configuration copiée")
                    }) {
                        Icon(Icons.Rounded.ContentCopy, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Copier le JSON")
                    }
                }
            },
            SettingItem(
                "Importer le thème",
                "Colle un JSON exporté depuis Car Studio pour retrouver un thème exact.",
                "import restauration colle json"
            ) {
                SettingCard("Importer le thème", "Colle un JSON exporté depuis Car Studio.") {
                    Button(onClick = {
                        val text = clipboard.getText()?.text
                        if (text.isNullOrBlank()) {
                            toast("Le presse-papiers est vide")
                        } else if (state.importJson(text)) {
                            toast("Thème importé")
                        } else {
                            toast("JSON invalide")
                        }
                    }) {
                        Icon(Icons.Rounded.ContentPaste, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Coller et appliquer")
                    }
                }
            },
            SettingItem(
                "Réinitialiser",
                "Efface tous les réglages et revient au thème Minuit d'origine. Action immédiate, sans confirmation.",
                "réinitialiser reset défaut"
            ) {
                SettingCard("Réinitialiser", "Revient au thème d'origine.") {
                    OutlinedButton(onClick = {
                        state.reset()
                        toast("Réglages réinitialisés")
                    }) {
                        Icon(Icons.Rounded.RestartAlt, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Tout réinitialiser")
                    }
                }
            }
        )
    )

    val setup = SettingSection(
        "Mise en route",
        "Comment voir Car Studio sur l'écran de la voiture, quel que soit le modèle.",
        listOf(
            SettingItem(
                "Le principe",
                "Car Studio est une app normale. Android Auto, lui, n'accepte que des composants signés par Google : impossible de le remplacer sur un autoradio d'origine.",
                "android auto signature miroir principe"
            ) {
                SettingCard("Le principe", "Deux minutes pour comprendre.") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Android Auto dessine son interface sur l'autoradio et vérifie un certificat Google. On ne peut donc pas installer une autre app « à la place » d'Android Auto sur un autoradio de série.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            "La solution qui marche partout : afficher Car Studio en plein écran par-dessus Android Auto, avec une app de miroir. Android Auto gère alors le câble ou le sans-fil, et l'écran affiche Car Studio.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            },
            SettingItem(
                "Afficher sur l'écran",
                "La marche à suivre pour un Pixel rooté sous Magisk, avec n'importe quel autoradio filaire ou sans fil.",
                "miroir screenonauto km auto magisk root installer"
            ) {
                SettingCard("Afficher sur l'écran", "La marche à suivre, étape par étape.") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Step(1, "Garde Android Auto installé. Ne le désinstalle pas : c'est lui qui parle à la voiture.")
                        Step(2, "Dans Android Auto : à propos, tape 10 fois sur « Version » pour activer le mode développeur, puis coche « Sources inconnues ».")
                        Step(3, "Installe une app de miroir compatible avec Android 17 : ScreenOnAuto (sideload) ou KM Auto (KoalaMirror, root).")
                        Step(4, "Branche en USB ou connecte en sans-fil, lance le miroir, puis choisis Car Studio.")
                        Spacer(Modifier.height(4.dp))
                        Button(onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.parse("package:com.google.android.projection.gearhead")
                                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }.onFailure { toast("Android Auto n'est pas installé") }
                        }) {
                            Text("Ouvrir la fiche Android Auto")
                        }
                    }
                }
            },
            SettingItem(
                "Compatibilité",
                "Filaire, sans fil, tactile, tous les formats d'écran : c'est Android Auto qui gère le transport, Car Studio s'adapte à la taille.",
                "filaire sans fil tactile format écran compatibilité"
            ) {
                SettingCard("Compatibilité", "Ce qui est déjà géré.") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("• Filaire et sans fil : gérés par Android Auto.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        Text("• Tactile et molette : la grille et les cartes restent utilisables.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        Text("• Tous formats : l'accueil s'adapte en largeur (paysage large, portrait, split).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                }
            }
        )
    )

    val about = SettingSection(
        "À propos",
        null,
        listOf(
            SettingItem(
                "Comment ça marche",
                "Car Studio affiche ton interface à la place de l'écran Android Auto, pas à la place de l'app Google.",
                "à propos android auto google signature"
            ) {
                SettingCard("Comment ça marche", "L'essentiel, sans jargon.") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Android Auto dessine lui-même son interface sur l'autoradio et n'accepte que des composants signés par Google. Impossible donc de la remplacer sur un autoradio d'origine.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            "Car Studio contourne le problème : c'est une app normale, affichée en plein écran sur l'écran de la voiture via une app de miroir (Screen2Auto, ScreenOnAuto, KM Auto…) sur téléphone rooté. Elle marche aussi nativement sur un autoradio Android ou Android Automotive.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Info, null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Ne manipule pas l'écran en roulant.",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        )
    )

    return listOf(appearance, home, applications, weather, media, driving, data, setup, about)
}

@Composable
private fun Step(number: Int, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                number.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PreviewCard(s: CarSettings) {
    val shape = MaterialTheme.shapes.large
    val colors = wallpaperColors(s.theme.wallpaper, s.theme)
    val ink = if (s.theme.wallpaper == "daylight") Color(0xFF10131A) else Color.White

    Box(
        Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(shape)
            .background(Brush.linearGradient(listOf(colors.top, colors.bottom)))
            .border(1.dp, Color.White.copy(alpha = 0.1f), shape)
            .padding(16.dp)
    ) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text("21:42", style = MaterialTheme.typography.headlineMedium, color = ink)
                Text(
                    "Aperçu en direct",
                    style = MaterialTheme.typography.labelSmall,
                    color = ink.copy(alpha = 0.6f)
                )
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(4) {
                        Box(
                            Modifier
                                .size(32.dp)
                                .clip(iconShapeFor(s.theme.iconShape, s.theme.corner))
                                .background(Color(s.theme.accent).copy(alpha = 0.35f))
                                .border(
                                    1.dp,
                                    Color(s.theme.accent).copy(alpha = 0.55f),
                                    iconShapeFor(s.theme.iconShape, s.theme.corner)
                                )
                        )
                    }
                }
            }
            Column(
                Modifier.width(150.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(ink.copy(alpha = 0.10f))
                        .border(1.dp, ink.copy(alpha = 0.08f), MaterialTheme.shapes.medium)
                        .padding(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(32.dp)
                                .clip(iconShapeFor(s.theme.iconShape, s.theme.corner))
                                .background(Color(s.theme.accent).copy(alpha = 0.55f))
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Box(
                                Modifier
                                    .height(5.dp)
                                    .fillMaxWidth(0.85f)
                                    .clip(CircleShape)
                                    .background(ink.copy(alpha = 0.55f))
                            )
                            Spacer(Modifier.height(5.dp))
                            Box(
                                Modifier
                                    .height(4.dp)
                                    .fillMaxWidth(0.55f)
                                    .clip(CircleShape)
                                    .background(ink.copy(alpha = 0.3f))
                            )
                        }
                    }
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(MaterialTheme.shapes.medium)
                        .background(ink.copy(alpha = 0.06f))
                        .padding(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        repeat(3) {
                            Box(
                                Modifier
                                    .size(24.dp)
                                    .clip(iconShapeFor(s.theme.iconShape, s.theme.corner))
                                    .background(Color(s.theme.accent).copy(alpha = 0.35f))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetChips(selected: String, onSelect: (ThemePreset) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Presets.all.forEach { preset ->
            val isSelected = preset.id == selected
            Column(
                Modifier
                    .width(132.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                    )
                    .border(
                        1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        MaterialTheme.shapes.medium
                    )
                    .clickable { onSelect(preset) }
                    .padding(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        Modifier.size(14.dp).clip(CircleShape)
                            .background(Color(preset.spec.accent))
                    )
                    Box(
                        Modifier.size(14.dp).clip(CircleShape)
                            .background(Color(preset.spec.secondary))
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(preset.name, style = MaterialTheme.typography.labelLarge)
                Text(
                    preset.tagline,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun Chips(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (id, label) ->
            val isSelected = id == selected
            Box(
                Modifier
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
                    )
                    .clickable { onSelect(id) }
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }
    }
}

private val ACCENTS = listOf(
    0xFF5B8CFF, 0xFF38D6C4, 0xFFB06BFF, 0xFFFF6B9A,
    0xFFFF8A3D, 0xFF7CE38B, 0xFFFFD166, 0xFFE8ECF8
)

private val BACKGROUNDS = listOf(
    0xFF07080D, 0xFF0B0A12, 0xFF0D0F14, 0xFF120D0A,
    0xFF0A1210, 0xFF111318, 0xFF050607, 0xFFF2F4FA
)

```

---

## Rappel final pour l'assistant
Réécris les fichiers un par un, en entier, avec le nouveau design. Ne change pas la logique (météo, média, apps, réglages). Livre aussi la liste des tokens modifiés et les risques de régression.

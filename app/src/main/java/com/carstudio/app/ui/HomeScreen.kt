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

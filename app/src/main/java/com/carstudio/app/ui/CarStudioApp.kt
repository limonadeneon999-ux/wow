package com.carstudio.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.carstudio.app.CarState
import com.carstudio.app.ui.components.CarBackground
import com.carstudio.app.ui.theme.CarTheme
import kotlinx.coroutines.delay
import java.util.Calendar

@Composable
fun CarStudioApp(state: CarState) {
    val settings = state.settings
    var showSettings by remember { mutableStateOf(false) }
    var hour by remember { mutableStateOf(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) }

    LaunchedEffect(Unit) {
        state.loadApps()
    }

    LaunchedEffect(Unit) {
        while (true) {
            hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            delay(60_000L)
        }
    }

    val nightMode = when (settings.nightMode) {
        "day" -> "day"
        "night" -> "night"
        else -> if (hour in 7..19) "day" else "night"
    }

    BackHandler(enabled = showSettings) { showSettings = false }

    CarTheme(settings.theme, nightMode) {
        Box(Modifier.fillMaxSize()) {
            CarBackground(settings.theme, nightMode)
            if (showSettings) {
                SettingsScreen(state, onClose = { showSettings = false })
            } else {
                HomeScreen(state, onOpenSettings = { showSettings = true })
            }
        }
    }
}

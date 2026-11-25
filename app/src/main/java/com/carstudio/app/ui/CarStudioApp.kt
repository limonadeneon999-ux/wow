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
    var screen by remember { mutableStateOf("home") }
    var pickerFrom by remember { mutableStateOf("home") }
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

    BackHandler(enabled = screen != "home") {
        screen = if (screen == "apps") pickerFrom else "home"
    }

    CarTheme(settings.theme, nightMode) {
        Box(Modifier.fillMaxSize()) {
            CarBackground(settings.theme, nightMode)
            when (screen) {
                "settings" -> SettingsScreen(
                    state,
                    onClose = { screen = "home" },
                    onOpenAppPicker = {
                        pickerFrom = "settings"
                        screen = "apps"
                    }
                )
                "apps" -> AppPickerScreen(state, onClose = { screen = pickerFrom })
                else -> HomeScreen(
                    state,
                    onOpenSettings = { screen = "settings" },
                    onOpenApps = {
                        pickerFrom = "home"
                        screen = "apps"
                    }
                )
            }
        }
    }
}

package com.carstudio.app

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.carstudio.app.ui.AppPickerScreen
import com.carstudio.app.ui.HomeScreen
import com.carstudio.app.ui.SettingsScreen
import com.carstudio.app.ui.components.CarBackground
import com.carstudio.app.ui.theme.CarTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Rend les vrais composables avec Robolectric et écrit des PNG.
 * Aucun émulateur, aucune signature Google.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w1280dp-h720dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun render(name: String, content: @Composable () -> Unit) {
        compose.setContent { content() }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/screenshots/$name.png")
    }

    private fun newState(): CarState {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.getSharedPreferences("car_studio_weather", android.content.Context.MODE_PRIVATE)
            .edit()
            .putString(
                "cache",
                """{"city":"Paris","temperature":12.4,"apparent":10.1,"code":3,"updatedAt":${System.currentTimeMillis()}}"""
            )
            .apply()
        val state = CarState(context)
        state.update { it.copy(demoMode = true) }
        return state
    }

    private fun screenshot(name: String, layout: String, nightMode: String = "night") {
        val state = newState()
        state.update { it.copy(layout = layout) }
        render(name) {
            CarTheme(state.settings.theme, nightMode) {
                Box(Modifier.fillMaxSize()) {
                    CarBackground(state.settings.theme, nightMode)
                    HomeScreen(state, onOpenSettings = {})
                }
            }
        }
    }

    @Test
    fun homeDock() = screenshot("home_dock", "dock")

    @Test
    fun homeSplit() = screenshot("home_split", "split")

    @Test
    fun homeFocus() = screenshot("home_focus", "focus")

    @Test
    fun homeDay() = screenshot("home_day", "dock", nightMode = "day")

    @Test
    fun appPicker() {
        val state = newState()
        render("app_picker") {
            CarTheme(state.settings.theme, "night") {
                Box(Modifier.fillMaxSize()) {
                    CarBackground(state.settings.theme, "night")
                    AppPickerScreen(state, onClose = {})
                }
            }
        }
    }

    @Test
    fun settings() {
        val state = newState()
        render("settings") {
            CarTheme(state.settings.theme, "night") {
                Box(Modifier.fillMaxSize()) {
                    CarBackground(state.settings.theme, "night")
                    SettingsScreen(state, onClose = {})
                }
            }
        }
    }
}

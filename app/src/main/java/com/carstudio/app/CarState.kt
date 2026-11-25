package com.carstudio.app

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.carstudio.app.data.AppEntry
import com.carstudio.app.data.AppLoader
import com.carstudio.app.data.CarSettings
import com.carstudio.app.data.SettingsRepository
import com.carstudio.app.data.ThemeSpec

class CarState(private val context: Context) {

    private val repo = SettingsRepository(context)

    var settings by mutableStateOf(repo.load())
        private set

    var apps by mutableStateOf<List<AppEntry>>(emptyList())
        private set

    var loading by mutableStateOf(true)
        private set

    fun update(block: (CarSettings) -> CarSettings) {
        settings = block(settings)
        repo.save(settings)
    }

    fun updateTheme(block: (ThemeSpec) -> ThemeSpec) =
        update { it.copy(theme = block(it.theme)) }

    suspend fun loadApps() {
        apps = AppLoader.load(context)
        loading = false
    }

    fun launch(pkg: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(pkg)
        if (intent == null) {
            Toast.makeText(context, "App introuvable", Toast.LENGTH_SHORT).show()
            return
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
            .onFailure {
                Toast.makeText(context, "Impossible de lancer l'app", Toast.LENGTH_SHORT).show()
            }
    }

    fun toggleFavorite(pkg: String) = update { s ->
        val favorites = if (pkg in s.favorites) s.favorites - pkg else s.favorites + pkg
        s.copy(favorites = favorites)
    }

    fun exportJson(): String = repo.encode(settings)

    fun importJson(text: String): Boolean {
        val parsed = repo.decode(text) ?: return false
        settings = parsed
        repo.save(parsed)
        return true
    }

    fun reset() {
        settings = CarSettings()
        repo.save(settings)
    }
}

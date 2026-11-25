package com.carstudio.app.data

import android.content.Context
import kotlinx.serialization.json.Json

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("car_studio", Context.MODE_PRIVATE)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun load(): CarSettings =
        prefs.getString(KEY, null)
            ?.let { runCatching { json.decodeFromString(CarSettings.serializer(), it) }.getOrNull() }
            ?: CarSettings()

    fun save(settings: CarSettings) {
        prefs.edit().putString(KEY, encode(settings)).apply()
    }

    fun encode(settings: CarSettings): String =
        json.encodeToString(CarSettings.serializer(), settings)

    fun decode(text: String): CarSettings? =
        runCatching { json.decodeFromString(CarSettings.serializer(), text) }.getOrNull()

    private companion object {
        const val KEY = "settings_v1"
    }
}

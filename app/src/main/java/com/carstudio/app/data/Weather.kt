package com.carstudio.app.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

@Serializable
data class WeatherSnapshot(
    val city: String,
    val temperature: Float,
    val apparent: Float,
    val code: Int,
    val updatedAt: Long
)

object WeatherCodes {
    fun label(code: Int): String = when (code) {
        0 -> "Ciel dégagé"
        1 -> "Plutôt dégagé"
        2 -> "Peu nuageux"
        3 -> "Couvert"
        45, 48 -> "Brouillard"
        51, 53, 55, 56, 57 -> "Bruine"
        61, 63, 65, 66, 67 -> "Pluie"
        71, 73, 75, 77 -> "Neige"
        80, 81, 82 -> "Averses"
        85, 86 -> "Averses de neige"
        95 -> "Orage"
        96, 99 -> "Orage de grêle"
        else -> "Météo"
    }

    fun glyph(code: Int): String = when (code) {
        0 -> "☀"
        1, 2 -> "⛅"
        3 -> "☁"
        45, 48 -> "≋"
        51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> "☂"
        71, 73, 75, 77, 85, 86 -> "❄"
        95, 96, 99 -> "⚡"
        else -> "◌"
    }
}

class WeatherRepository(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("car_studio_weather", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun cached(): WeatherSnapshot? =
        prefs.getString(KEY_CACHE, null)?.let {
            runCatching { json.decodeFromString(WeatherSnapshot.serializer(), it) }.getOrNull()
        }

    fun isStale(snapshot: WeatherSnapshot?, minutes: Int): Boolean =
        snapshot == null ||
            System.currentTimeMillis() - snapshot.updatedAt > TimeUnit.MINUTES.toMillis(minutes.toLong())

    fun lastError(): String? = prefs.getString(KEY_ERROR, null)

    suspend fun refresh(city: String): WeatherSnapshot? = withContext(Dispatchers.IO) {
        runCatching {
            val coords = geocode(city) ?: return@runCatching null
            val url = "https://api.open-meteo.com/v1/forecast" +
                "?latitude=${coords.first}&longitude=${coords.second}" +
                "&current=temperature_2m,apparent_temperature,weather_code&timezone=auto"
            val body = get(url) ?: return@runCatching null
            val current = JSONObject(body).getJSONObject("current")
            val snapshot = WeatherSnapshot(
                city = city,
                temperature = current.getDouble("temperature_2m").toFloat(),
                apparent = current.getDouble("apparent_temperature").toFloat(),
                code = current.getInt("weather_code"),
                updatedAt = System.currentTimeMillis()
            )
            prefs.edit()
                .putString(KEY_CACHE, json.encodeToString(WeatherSnapshot.serializer(), snapshot))
                .remove(KEY_ERROR)
                .apply()
            snapshot
        }.onFailure { error ->
            prefs.edit().putString(KEY_ERROR, error.message ?: "erreur réseau").apply()
        }.getOrNull()
    }

    private fun geocode(city: String): Pair<Double, Double>? {
        val key = "geo_" + city.lowercase()
        prefs.getString(key, null)?.let { cached ->
            val parts = cached.split(",")
            if (parts.size == 2) {
                val lat = parts[0].toDoubleOrNull()
                val lon = parts[1].toDoubleOrNull()
                if (lat != null && lon != null) return lat to lon
            }
        }
        val url = "https://geocoding-api.open-meteo.com/v1/search" +
            "?name=${java.net.URLEncoder.encode(city, "UTF-8")}&count=1&language=fr&format=json"
        val body = get(url) ?: return null
        val results = JSONObject(body).optJSONArray("results") ?: return null
        if (results.length() == 0) return null
        val first = results.getJSONObject(0)
        val lat = first.getDouble("latitude")
        val lon = first.getDouble("longitude")
        prefs.edit().putString(key, "$lat,$lon").apply()
        return lat to lon
    }

    private fun get(url: String): String? {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 8000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
        }
        return try {
            if (connection.responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private const val KEY_CACHE = "cache"
        private const val KEY_ERROR = "error"

        fun isWifi(context: Context): Boolean {
            val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
            return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        }
    }
}

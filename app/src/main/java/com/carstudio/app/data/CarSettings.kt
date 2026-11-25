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
    val mediaEnabled: Boolean = true
)

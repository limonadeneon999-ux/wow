package com.carstudio.app

import com.carstudio.app.data.CarSettings
import com.carstudio.app.data.ThemeSpec
import com.carstudio.app.data.WeatherCodes
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LogicTest {

    @Test
    fun weatherLabelsAreMapped() {
        assertEquals("Ciel dégagé", WeatherCodes.label(0))
        assertEquals("Orage", WeatherCodes.label(95))
        assertEquals("Neige", WeatherCodes.label(71))
        assertNotEquals("", WeatherCodes.glyph(61))
    }

    @Test
    fun settingsSurviveJsonRoundTrip() {
        val json = Json { encodeDefaults = true }
        val settings = CarSettings(
            weatherCity = "Lyon",
            layout = "focus",
            demoMode = true,
            theme = ThemeSpec(accent = 0xFF112233, corner = 40f)
        )

        val encoded = json.encodeToString(CarSettings.serializer(), settings)
        val decoded = json.decodeFromString(CarSettings.serializer(), encoded)

        assertEquals("Lyon", decoded.weatherCity)
        assertEquals("focus", decoded.layout)
        assertTrue(decoded.demoMode)
        assertEquals(0xFF112233, decoded.theme.accent)
        assertEquals(40f, decoded.theme.corner, 0.001f)
    }

    @Test
    fun unknownKeysAreIgnored() {
        val json = Json { ignoreUnknownKeys = true }
        val decoded = json.decodeFromString(
            CarSettings.serializer(),
            """{"layout":"split","futureOption":42}"""
        )
        assertEquals("split", decoded.layout)
        assertEquals("Paris", decoded.weatherCity)
    }
}

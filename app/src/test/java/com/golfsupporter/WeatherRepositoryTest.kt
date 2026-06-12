package com.golfsupporter

import com.golfsupporter.data.location.LatLng
import com.golfsupporter.data.weather.Weather
import com.golfsupporter.data.weather.WeatherRepository
import com.golfsupporter.data.weather.remote.MainInfo
import com.golfsupporter.data.weather.remote.WeatherApi
import com.golfsupporter.data.weather.remote.WeatherCondition
import com.golfsupporter.data.weather.remote.WeatherResponse
import com.golfsupporter.data.weather.remote.WindInfo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Verifies the OpenWeatherMap response → [Weather] mapping and staleness logic
 * using a fake [WeatherApi] (no live network, so independent of key activation).
 */
class WeatherRepositoryTest {

    /** Canned OpenWeatherMap response: 17.6°C, light rain (code 500), wind 3.2 m/s from 45° (NE). */
    private class FakeWeatherApi : WeatherApi {
        override suspend fun current(
            lat: Double, lon: Double, apiKey: String, units: String, lang: String,
        ): WeatherResponse = WeatherResponse(
            weather = listOf(WeatherCondition(id = 500, description = "약한 비")),
            main = MainInfo(temp = 17.6),
            wind = WindInfo(speed = 3.2, deg = 45.0),
        )
    }

    @Test
    fun fetch_maps_response_fields() = runBlocking {
        val repo = WeatherRepository(FakeWeatherApi())
        // fetch() is gated by a configured key; skip cleanly if the build has none.
        assumeTrue("OPENWEATHER_API_KEY not set in this build", repo.isEnabled)

        val w = repo.fetch(LatLng(37.5665, 126.9780))
        requireNotNull(w)
        assertEquals(18, w.temperatureC)            // 17.6 rounds to 18
        assertEquals("약한 비", w.description)
        assertEquals("🌧️", w.iconEmoji)             // code 500 → rain
        assertEquals("NE", w.windDirection)          // 45° → NE
        assertEquals(3.2, w.windSpeedMs, 0.0001)
        assertTrue(w.fetchedAt > 0)
    }

    @Test
    fun isStale_true_for_null_or_old_weather() {
        val repo = WeatherRepository(FakeWeatherApi())
        assertTrue(repo.isStale(null))

        val old = Weather(
            temperatureC = 20, description = "", iconEmoji = "",
            windSpeedMs = 0.0, windDirection = "N",
            fetchedAt = System.currentTimeMillis() - Weather.REFRESH_INTERVAL_MS - 1,
        )
        assertTrue(repo.isStale(old))

        val fresh = old.copy(fetchedAt = System.currentTimeMillis())
        assertFalse(repo.isStale(fresh))
    }
}

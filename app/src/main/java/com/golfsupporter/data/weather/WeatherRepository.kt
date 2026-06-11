package com.golfsupporter.data.weather

import com.golfsupporter.BuildConfig
import com.golfsupporter.data.location.LatLng
import com.golfsupporter.data.weather.remote.WeatherApi
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Fetches current weather for a course location. Degrades gracefully when no API
 * key is configured or the network is unavailable (PRD G-009): every failure
 * resolves to null rather than throwing.
 */
@Singleton
class WeatherRepository @Inject constructor(
    private val api: WeatherApi,
) {
    private val apiKey: String = BuildConfig.OPENWEATHER_API_KEY

    /** Whether weather is available at all (an API key has been configured). */
    val isEnabled: Boolean get() = apiKey.isNotBlank()

    suspend fun fetch(coords: LatLng): Weather? {
        if (!isEnabled) return null
        return try {
            val res = api.current(coords.latitude, coords.longitude, apiKey)
            val condition = res.weather.firstOrNull()
            Weather(
                temperatureC = (res.main?.temp ?: 0.0).roundToInt(),
                description = condition?.description.orEmpty(),
                iconEmoji = Weather.emojiForCode(condition?.id ?: 0),
                windSpeedMs = res.wind?.speed ?: 0.0,
                windDirection = Weather.compass(res.wind?.deg ?: 0.0),
                fetchedAt = System.currentTimeMillis(),
            )
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            null // offline / API error → no weather shown
        }
    }

    fun isStale(weather: Weather?): Boolean =
        weather == null ||
            System.currentTimeMillis() - weather.fetchedAt >= Weather.REFRESH_INTERVAL_MS
}

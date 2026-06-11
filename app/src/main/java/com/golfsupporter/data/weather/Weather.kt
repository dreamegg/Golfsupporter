package com.golfsupporter.data.weather

/** Current weather for the course location (PRD G-006 / G-007). */
data class Weather(
    val temperatureC: Int,
    val description: String,
    val iconEmoji: String,
    val windSpeedMs: Double,
    val windDirection: String,   // compass abbreviation, e.g. "NE"
    val fetchedAt: Long,
) {
    companion object {
        /** Weather is considered stale after 30 minutes (PRD G-008). */
        const val REFRESH_INTERVAL_MS = 30 * 60 * 1000L

        /** Maps an OpenWeatherMap condition code to a representative emoji. */
        fun emojiForCode(code: Int): String = when (code) {
            in 200..232 -> "⛈️"   // thunderstorm
            in 300..321 -> "🌦️"   // drizzle
            in 500..531 -> "🌧️"   // rain
            in 600..622 -> "🌨️"   // snow
            in 701..781 -> "🌫️"   // atmosphere (mist/fog…)
            800 -> "☀️"            // clear
            801, 802 -> "🌤️"      // few/scattered clouds
            803, 804 -> "☁️"      // broken/overcast
            else -> "🌡️"
        }

        /** Converts a wind bearing in degrees to an 8-point compass abbreviation. */
        fun compass(degrees: Double): String {
            val dirs = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
            val idx = (((degrees % 360) + 360) % 360 / 45.0).toInt() % 8
            return dirs[idx]
        }
    }
}

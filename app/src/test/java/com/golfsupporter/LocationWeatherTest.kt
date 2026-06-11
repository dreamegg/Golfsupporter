package com.golfsupporter

import com.golfsupporter.data.location.LatLng
import com.golfsupporter.data.weather.Weather
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationWeatherTest {

    @Test
    fun distance_is_zero_for_same_point() {
        val p = LatLng(37.5, 127.0)
        assertEquals(0.0, LatLng.distanceKm(p, p), 0.0001)
    }

    @Test
    fun distance_seoul_to_busan_is_roughly_325km() {
        val seoul = LatLng(37.5665, 126.9780)
        val busan = LatLng(35.1796, 129.0756)
        val km = LatLng.distanceKm(seoul, busan)
        assertTrue("expected ~325km but was $km", km in 300.0..360.0)
    }

    @Test
    fun compass_maps_degrees_to_eight_points() {
        assertEquals("N", Weather.compass(0.0))
        assertEquals("NE", Weather.compass(45.0))
        assertEquals("E", Weather.compass(90.0))
        assertEquals("S", Weather.compass(180.0))
        assertEquals("W", Weather.compass(270.0))
        assertEquals("N", Weather.compass(360.0))
    }

    @Test
    fun weather_emoji_for_known_codes() {
        assertEquals("☀️", Weather.emojiForCode(800))
        assertEquals("🌧️", Weather.emojiForCode(500))
        assertEquals("⛈️", Weather.emojiForCode(210))
    }
}

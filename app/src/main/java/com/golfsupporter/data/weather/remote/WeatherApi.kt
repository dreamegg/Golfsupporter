package com.golfsupporter.data.weather.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

/** OpenWeatherMap "current weather" endpoint. Base URL: https://api.openweathermap.org/ */
interface WeatherApi {
    @GET("data/2.5/weather")
    suspend fun current(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "kr",
    ): WeatherResponse
}

data class WeatherResponse(
    @SerializedName("weather") val weather: List<WeatherCondition> = emptyList(),
    @SerializedName("main") val main: MainInfo? = null,
    @SerializedName("wind") val wind: WindInfo? = null,
)

data class WeatherCondition(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("description") val description: String = "",
)

data class MainInfo(
    @SerializedName("temp") val temp: Double = 0.0,
)

data class WindInfo(
    @SerializedName("speed") val speed: Double = 0.0,
    @SerializedName("deg") val deg: Double = 0.0,
)

package com.golfsupporter.di

import com.golfsupporter.data.course.remote.GolfCourseApi
import com.golfsupporter.data.course.remote.StubGolfCourseApi
import com.golfsupporter.data.location.FusedLocationProvider
import com.golfsupporter.data.location.LocationProvider
import com.golfsupporter.data.weather.remote.WeatherApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Network + location/course bindings for the v2.0 location features (PRD §10).
 * The remote golf-course source is bound to the no-network stub by default;
 * swap [provideGolfCourseApi] for a real implementation when available.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val WEATHER_BASE_URL = "https://api.openweathermap.org/"

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(WEATHER_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideWeatherApi(retrofit: Retrofit): WeatherApi = retrofit.create(WeatherApi::class.java)

    @Provides
    @Singleton
    fun provideLocationProvider(impl: FusedLocationProvider): LocationProvider = impl

    @Provides
    @Singleton
    fun provideGolfCourseApi(impl: StubGolfCourseApi): GolfCourseApi = impl
}

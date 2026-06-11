package com.golfsupporter.data.course.remote

import com.golfsupporter.data.course.GolfCourse
import com.golfsupporter.data.location.LatLng
import javax.inject.Inject

/**
 * Remote source for golf-course data (e.g. GolfAPI.io / TheGolfAPI per PRD §10.3).
 * Kept as an interface so a real provider can be plugged in without touching the
 * repository, and so the app works offline against the local cache.
 */
interface GolfCourseApi {
    suspend fun nearby(coords: LatLng, radiusKm: Double): List<GolfCourse>
    suspend fun search(query: String): List<GolfCourse>
}

/**
 * Default no-network implementation. Returns nothing, so the app relies on the
 * local cache + manual entry (PRD G-009). Replace the Hilt binding in
 * [com.golfsupporter.di.LocationModule] with a Retrofit-backed implementation
 * once an API key/provider is available.
 */
class StubGolfCourseApi @Inject constructor() : GolfCourseApi {
    override suspend fun nearby(coords: LatLng, radiusKm: Double): List<GolfCourse> = emptyList()
    override suspend fun search(query: String): List<GolfCourse> = emptyList()
}

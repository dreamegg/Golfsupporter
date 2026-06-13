package com.golfsupporter.data.course

import com.golfsupporter.data.course.local.CachedCourseEntity
import com.golfsupporter.data.course.local.CourseDao
import com.golfsupporter.data.course.remote.GolfCourseApi
import com.golfsupporter.data.location.LatLng
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves golf courses from the remote API when available, always backed by a
 * local cache so detection/search keep working offline (PRD G-002/G-003/G-010).
 */
@Singleton
class CourseRepository @Inject constructor(
    private val courseDao: CourseDao,
    private val api: GolfCourseApi,
) {

    /**
     * Seeds the bundled sample courses. Upserts unconditionally (keyed by id) so
     * courses added in an app update appear on already-seeded devices without
     * disturbing remote-cached entries.
     */
    suspend fun ensureSeeded() {
        cache(SampleCourses.list)
    }

    /** Nearby courses within [radiusKm], sorted by distance (PRD G-002). */
    suspend fun nearby(coords: LatLng, radiusKm: Double = 5.0): List<NearbyCourse> {
        // Try remote first; persist whatever we get for offline reuse.
        val remote = safeRemote { api.nearby(coords, radiusKm) }
        if (remote.isNotEmpty()) cache(remote)

        val pool = (remote + cachedCourses()).distinctBy { it.id }
        return pool
            .map { NearbyCourse(it, LatLng.distanceKm(coords, it.location)) }
            .filter { it.distanceKm <= radiusKm }
            .sortedBy { it.distanceKm }
    }

    /** Course search by name/region (PRD G-003). */
    suspend fun search(query: String): List<GolfCourse> {
        if (query.isBlank()) return emptyList()
        val remote = safeRemote { api.search(query) }
        if (remote.isNotEmpty()) cache(remote)
        if (remote.isNotEmpty()) return remote
        return courseDao.search(query.trim()).map { it.toDomain() }
    }

    private suspend fun cachedCourses(): List<GolfCourse> =
        courseDao.getAll().map { it.toDomain() }

    private suspend fun cache(courses: List<GolfCourse>) {
        val now = System.currentTimeMillis()
        courseDao.upsertAll(courses.map { it.toEntity(now) })
    }

    /** Runs a remote call, returning an empty list on any failure (offline-safe). */
    private suspend inline fun safeRemote(block: () -> List<GolfCourse>): List<GolfCourse> =
        try {
            block()
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            emptyList()
        }
}

private fun CachedCourseEntity.toDomain() = GolfCourse(
    id, name, courseName, latitude, longitude, totalYardage, courseRating, holePars
)

private fun GolfCourse.toEntity(cachedAt: Long) = CachedCourseEntity(
    id, name, courseName, latitude, longitude, totalYardage, courseRating, holePars, cachedAt
)

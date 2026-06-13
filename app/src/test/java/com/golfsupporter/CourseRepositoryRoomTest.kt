package com.golfsupporter

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.golfsupporter.data.course.CourseRepository
import com.golfsupporter.data.course.SampleCourses
import com.golfsupporter.data.course.remote.StubGolfCourseApi
import com.golfsupporter.data.local.GolfDatabase
import com.golfsupporter.data.location.LatLng
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Exercises the offline-first course flow against a real Room database running
 * on the JVM via Robolectric (no emulator). Covers seeding, GPS nearby
 * detection/sorting, offline search, and the Map<Int,Int> par TypeConverter
 * round-trip (PRD G-002 / G-003 / G-009 / G-010).
 */
@RunWith(RobolectricTestRunner::class)
class CourseRepositoryRoomTest {

    private lateinit var db: GolfDatabase
    private lateinit var repo: CourseRepository

    // Namseoul CC coordinates (a seeded sample course).
    private val namseoul = LatLng(37.3624, 127.0986)

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, GolfDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        // StubGolfCourseApi returns nothing → repository must work purely offline.
        repo = CourseRepository(db.courseDao(), StubGolfCourseApi())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun ensureSeeded_populates_sample_courses() = runBlocking {
        assertEquals(0, db.courseDao().count())
        repo.ensureSeeded()
        assertEquals(SampleCourses.list.size, db.courseDao().count())
        // Idempotent: a second call must not duplicate.
        repo.ensureSeeded()
        assertEquals(SampleCourses.list.size, db.courseDao().count())
    }

    @Test
    fun nearby_filters_by_radius_and_excludes_far_courses() = runBlocking {
        repo.ensureSeeded()
        val within5 = repo.nearby(namseoul, radiusKm = 5.0)
        // Only Namseoul itself sits within 5 km; Lakeside (~9 km) and Jeju (~450 km) are out.
        assertEquals(1, within5.size)
        assertEquals("sample-namseoul", within5.first().course.id)
        assertEquals(0.0, within5.first().distanceKm, 0.5)
        // Jeju must never appear at this radius.
        assertFalse(within5.any { it.course.id == "sample-jeju" })
    }

    @Test
    fun nearby_sorts_by_ascending_distance() = runBlocking {
        repo.ensureSeeded()
        val within15 = repo.nearby(namseoul, radiusKm = 15.0)
        assertEquals(2, within15.size)
        assertEquals("sample-namseoul", within15[0].course.id)
        assertEquals("sample-leadersmenu", within15[1].course.id)
        assertTrue(within15[0].distanceKm <= within15[1].distanceKm)
    }

    @Test
    fun search_falls_back_to_offline_cache() = runBlocking {
        repo.ensureSeeded()
        val results = repo.search("제주")
        assertEquals(1, results.size)
        assertEquals("sample-jeju", results.first().id)
    }

    @Test
    fun par_map_round_trips_through_room() = runBlocking {
        repo.ensureSeeded()
        val namseoulCourse = repo.nearby(namseoul, radiusKm = 5.0).first().course
        // The 18-hole par map must survive the String TypeConverter intact.
        val expected = SampleCourses.list.first { it.id == "sample-namseoul" }.holePars
        assertEquals(18, namseoulCourse.holePars.size)
        assertEquals(expected, namseoulCourse.holePars)
        assertEquals(4, namseoulCourse.holePars[1])
        assertEquals(5, namseoulCourse.holePars[18])
    }
}

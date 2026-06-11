package com.golfsupporter.data.course.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface CourseDao {

    @Upsert
    suspend fun upsertAll(courses: List<CachedCourseEntity>)

    @Query("SELECT * FROM cached_courses")
    suspend fun getAll(): List<CachedCourseEntity>

    @Query("SELECT * FROM cached_courses WHERE name LIKE '%' || :query || '%' ORDER BY name LIMIT 30")
    suspend fun search(query: String): List<CachedCourseEntity>

    @Query("SELECT COUNT(*) FROM cached_courses")
    suspend fun count(): Int
}

package com.golfsupporter.data.course.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Locally cached golf course for offline reuse (PRD G-010). */
@Entity(tableName = "cached_courses")
data class CachedCourseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val courseName: String?,
    val latitude: Double,
    val longitude: Double,
    val totalYardage: Int?,
    val courseRating: Double?,
    val holePars: Map<Int, Int>,
    val cachedAt: Long,
)

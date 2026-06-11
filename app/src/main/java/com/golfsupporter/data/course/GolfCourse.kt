package com.golfsupporter.data.course

import com.golfsupporter.data.location.LatLng

/** A golf course with its hole pars and location (PRD G-002 / G-004 / G-005). */
data class GolfCourse(
    val id: String,
    val name: String,
    val courseName: String?,       // sub-course name, e.g. "East Course"
    val latitude: Double,
    val longitude: Double,
    val totalYardage: Int?,
    val courseRating: Double?,
    val holePars: Map<Int, Int>,   // holeNumber (1..18) -> par
) {
    val location: LatLng get() = LatLng(latitude, longitude)
}

/** A nearby course paired with its distance from the user (PRD G-002). */
data class NearbyCourse(
    val course: GolfCourse,
    val distanceKm: Double,
)

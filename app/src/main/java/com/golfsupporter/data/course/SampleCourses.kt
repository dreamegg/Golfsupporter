package com.golfsupporter.data.course

/**
 * Bundled sample courses seeded into the cache on first launch so that nearby
 * detection and par auto-load work offline without an external golf API
 * (PRD G-009 / G-010). These are demo fixtures — replace or augment with a real
 * [com.golfsupporter.data.course.remote.GolfCourseApi] implementation.
 */
object SampleCourses {

    private fun standardPars(layout: List<Int>): Map<Int, Int> =
        layout.mapIndexed { index, par -> (index + 1) to par }.toMap()

    val list: List<GolfCourse> = listOf(
        GolfCourse(
            id = "sample-namseoul",
            name = "남서울 컨트리클럽",
            courseName = null,
            latitude = 37.3624,
            longitude = 127.0986,
            totalYardage = 6800,
            courseRating = 72.0,
            holePars = standardPars(listOf(4, 4, 3, 5, 4, 4, 3, 5, 4, 4, 4, 3, 5, 4, 4, 3, 4, 5)),
        ),
        GolfCourse(
            id = "sample-leadersmenu",
            name = "레이크사이드 CC",
            courseName = "South",
            latitude = 37.2911,
            longitude = 127.1450,
            totalYardage = 7100,
            courseRating = 73.5,
            holePars = standardPars(listOf(4, 5, 4, 3, 4, 4, 5, 3, 4, 4, 3, 4, 5, 4, 4, 5, 3, 4)),
        ),
        GolfCourse(
            id = "sample-jeju",
            name = "제주 핀크스 GC",
            courseName = null,
            latitude = 33.3070,
            longitude = 126.3700,
            totalYardage = 7000,
            courseRating = 73.0,
            holePars = standardPars(listOf(4, 4, 5, 3, 4, 4, 4, 3, 5, 5, 4, 3, 4, 4, 4, 3, 5, 4)),
        ),
    )
}

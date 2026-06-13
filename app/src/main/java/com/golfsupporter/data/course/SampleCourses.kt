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

    val list: List<GolfCourse> get() = listOf(
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
    ) + vietnamNorth

    /**
     * Northern-Vietnam courses sourced from OpenStreetMap (leisure=golf_course).
     * Names + GPS only — OSM has no reliable per-hole par for Vietnam, so pars
     * are left empty and entered manually in setup (par auto-load is a no-op).
     */
    private val vietnamNorth: List<GolfCourse> = listOf(
        GolfCourse(id = "vn-node-2612992387", name = "Do Son 18 holes golf course", courseName = null, latitude = 20.7413269, longitude = 106.7730785, totalYardage = null, courseRating = null, holePars = emptyMap()),
        GolfCourse(id = "vn-node-13607166216", name = "Dragon Golf Links", courseName = null, latitude = 20.6847447, longitude = 106.7683031, totalYardage = null, courseRating = null, holePars = emptyMap()),
        GolfCourse(id = "vn-relation-9390659", name = "FLC Ha Long Bay Golf Club & Luxury Resort", courseName = null, latitude = 20.9556108, longitude = 107.1138588, totalYardage = null, courseRating = null, holePars = emptyMap()),
        GolfCourse(id = "vn-node-880591543", name = "Golf CL", courseName = null, latitude = 21.1022379, longitude = 106.398795, totalYardage = null, courseRating = null, holePars = emptyMap()),
        GolfCourse(id = "vn-way-1023111557", name = "Heron Lake Golf Course", courseName = null, latitude = 21.2955375, longitude = 105.6092554, totalYardage = null, courseRating = null, holePars = emptyMap()),
        GolfCourse(id = "vn-way-1466977031", name = "King's Island Golf", courseName = null, latitude = 21.0609473, longitude = 105.4727191, totalYardage = null, courseRating = null, holePars = emptyMap()),
        GolfCourse(id = "vn-way-149353388", name = "Mong Cai International Golf Club", courseName = null, latitude = 21.4875419, longitude = 108.0523255, totalYardage = null, courseRating = null, holePars = emptyMap()),
        GolfCourse(id = "vn-way-1190015423", name = "Mountain View - BRG Kings Island", courseName = null, latitude = 21.0530338, longitude = 105.4675037, totalYardage = null, courseRating = null, holePars = emptyMap()),
        GolfCourse(id = "vn-way-997324912", name = "Sky Lake Resort Golf Club", courseName = null, latitude = 20.8391948, longitude = 105.619337, totalYardage = null, courseRating = null, holePars = emptyMap()),
        GolfCourse(id = "vn-node-13629839867", name = "Sân Golf BRG Ruby Tree Hải Phòng", courseName = null, latitude = 20.7301152, longitude = 106.7776745, totalYardage = null, courseRating = null, holePars = emptyMap()),
        GolfCourse(id = "vn-way-452788687", name = "Sân Golf Long Biên", courseName = null, latitude = 21.0384099, longitude = 105.8915159, totalYardage = null, courseRating = null, holePars = emptyMap()),
        GolfCourse(id = "vn-way-746217590", name = "Sân Golf Sông Giá", courseName = null, latitude = 20.9611935, longitude = 106.6781201, totalYardage = null, courseRating = null, holePars = emptyMap()),
        GolfCourse(id = "vn-way-1500801247", name = "Sân Golf Đại Lải", courseName = null, latitude = 21.335568, longitude = 105.7281914, totalYardage = null, courseRating = null, holePars = emptyMap()),
        GolfCourse(id = "vn-node-4900827761", name = "Sân gôn Phượng Hoàng", courseName = null, latitude = 20.8982651, longitude = 105.4913818, totalYardage = null, courseRating = null, holePars = emptyMap()),
        GolfCourse(id = "vn-way-1117837123", name = "Vinpearl Golf Haiphong", courseName = null, latitude = 20.870562, longitude = 106.7304439, totalYardage = null, courseRating = null, holePars = emptyMap()),
    )
}

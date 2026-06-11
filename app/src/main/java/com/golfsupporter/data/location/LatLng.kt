package com.golfsupporter.data.location

/** A simple geographic coordinate. */
data class LatLng(
    val latitude: Double,
    val longitude: Double,
) {
    companion object {
        private const val EARTH_RADIUS_KM = 6371.0

        /** Great-circle distance in kilometres between two coordinates (Haversine). */
        fun distanceKm(a: LatLng, b: LatLng): Double {
            val dLat = Math.toRadians(b.latitude - a.latitude)
            val dLon = Math.toRadians(b.longitude - a.longitude)
            val lat1 = Math.toRadians(a.latitude)
            val lat2 = Math.toRadians(b.latitude)
            val h = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2)
            return 2 * EARTH_RADIUS_KM * Math.asin(Math.min(1.0, Math.sqrt(h)))
        }
    }
}

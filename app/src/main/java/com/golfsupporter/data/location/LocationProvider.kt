package com.golfsupporter.data.location

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/** Abstraction over device location so it can be faked in tests. */
interface LocationProvider {
    /** Returns the current coordinate, or null if unavailable / permission denied. */
    suspend fun currentLocation(): LatLng?
}

/**
 * Backed by Play Services [com.google.android.gms.location.FusedLocationProviderClient].
 * Caller is responsible for holding the location permission; a [SecurityException]
 * (missing permission) resolves to null rather than crashing (PRD G-001 fallback).
 */
@Singleton
class FusedLocationProvider @Inject constructor(
    @ApplicationContext context: Context,
) : LocationProvider {

    private val client = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    override suspend fun currentLocation(): LatLng? = suspendCancellableCoroutine { cont ->
        try {
            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .addOnSuccessListener { location ->
                    cont.resume(location?.let { LatLng(it.latitude, it.longitude) })
                }
                .addOnFailureListener { cont.resume(null) }
        } catch (e: SecurityException) {
            cont.resume(null)
        }
    }
}

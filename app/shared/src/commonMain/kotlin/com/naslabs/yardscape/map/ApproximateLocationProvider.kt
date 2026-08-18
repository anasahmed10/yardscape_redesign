package com.naslabs.yardscape.map

import com.naslabs.yardscape.domain.NeighborhoodCenter

sealed interface ApproximateLocationResult {
    data class Available(
        val center: NeighborhoodCenter,
        val accuracyRadiusMeters: Int,
    ) : ApproximateLocationResult {
        init {
            require(accuracyRadiusMeters >= MINIMUM_PUBLIC_ACCURACY_METERS) {
                "Approximate device location must remain neighborhood-level."
            }
        }
    }

    data object PermissionDenied : ApproximateLocationResult
    data object Unavailable : ApproximateLocationResult

    companion object {
        const val MINIMUM_PUBLIC_ACCURACY_METERS: Int = 500
    }
}

interface ApproximateLocationProvider {
    /** Requests approximate location. Call only after an explicit user action. */
    suspend fun requestApproximateLocation(): ApproximateLocationResult
}

expect fun createApproximateLocationProvider(): ApproximateLocationProvider

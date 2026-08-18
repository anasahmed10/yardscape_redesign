package com.naslabs.yardscape.map

import androidx.compose.runtime.Composable
import com.naslabs.yardscape.domain.NeighborhoodCenter
import kotlin.math.ceil
import kotlin.math.round

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

@Composable
expect fun rememberApproximateLocationProvider(): ApproximateLocationProvider

internal fun coarsenApproximateLocation(
    latitude: Double,
    longitude: Double,
    reportedAccuracyMeters: Double,
): ApproximateLocationResult.Available {
    val radius = if (reportedAccuracyMeters.isFinite() && reportedAccuracyMeters > 0.0) {
        maxOf(ApproximateLocationResult.MINIMUM_PUBLIC_ACCURACY_METERS * 2, ceil(reportedAccuracyMeters).toInt())
    } else {
        ApproximateLocationResult.MINIMUM_PUBLIC_ACCURACY_METERS * 2
    }
    return ApproximateLocationResult.Available(
        center = NeighborhoodCenter(
            latitude = round(latitude * COARSE_COORDINATE_SCALE) / COARSE_COORDINATE_SCALE,
            longitude = round(longitude * COARSE_COORDINATE_SCALE) / COARSE_COORDINATE_SCALE,
        ),
        accuracyRadiusMeters = radius,
    )
}

private const val COARSE_COORDINATE_SCALE = 100.0

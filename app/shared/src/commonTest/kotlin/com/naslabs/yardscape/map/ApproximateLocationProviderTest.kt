package com.naslabs.yardscape.map

import kotlin.test.Test
import kotlin.test.assertEquals

class ApproximateLocationProviderTest {
    @Test
    fun availableLocationIsQuantizedToNeighborhoodScale() {
        val result = coarsenApproximateLocation(
            latitude = 40.712834,
            longitude = -74.006123,
            reportedAccuracyMeters = 15.0,
        )

        assertEquals(40.71, result.center.latitude)
        assertEquals(-74.01, result.center.longitude)
        assertEquals(1_000, result.accuracyRadiusMeters)
    }

    @Test
    fun reportedAccuracyLargerThanNeighborhoodFloorIsPreservedConservatively() {
        val result = coarsenApproximateLocation(
            latitude = 40.0,
            longitude = -74.0,
            reportedAccuracyMeters = 2_400.2,
        )

        assertEquals(2_401, result.accuracyRadiusMeters)
    }
}

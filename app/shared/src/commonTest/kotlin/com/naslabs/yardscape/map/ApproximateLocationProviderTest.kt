package com.naslabs.yardscape.map

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ApproximateLocationProviderTest {
    @Test
    fun uiFirstProviderDoesNotGrantLocationWithoutAPlatformPermissionFlow() = runTest {
        val result = createApproximateLocationProvider().requestApproximateLocation()

        assertEquals(ApproximateLocationResult.PermissionDenied, result)
    }
}

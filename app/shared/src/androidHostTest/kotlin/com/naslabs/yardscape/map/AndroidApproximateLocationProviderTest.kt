package com.naslabs.yardscape.map

import android.Manifest
import kotlin.test.Test
import kotlin.test.assertEquals

class AndroidApproximateLocationProviderTest {
    @Test
    fun permissionContractRequestsCoarseLocationOnly() {
        assertEquals(Manifest.permission.ACCESS_COARSE_LOCATION, APPROXIMATE_LOCATION_PERMISSION)
    }
}

package com.naslabs.yardscape.map

actual fun createApproximateLocationProvider(): ApproximateLocationProvider =
    object : ApproximateLocationProvider {
        override suspend fun requestApproximateLocation(): ApproximateLocationResult =
            ApproximateLocationResult.PermissionDenied
    }

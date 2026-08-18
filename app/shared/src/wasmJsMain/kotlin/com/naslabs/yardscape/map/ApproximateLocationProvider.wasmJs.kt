package com.naslabs.yardscape.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberApproximateLocationProvider(): ApproximateLocationProvider = remember {
    object : ApproximateLocationProvider {
        override suspend fun requestApproximateLocation(): ApproximateLocationResult =
            ApproximateLocationResult.Unavailable
    }
}

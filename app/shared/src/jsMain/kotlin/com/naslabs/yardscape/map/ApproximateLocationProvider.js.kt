package com.naslabs.yardscape.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import web.navigator.navigator

@Composable
actual fun rememberApproximateLocationProvider(): ApproximateLocationProvider = remember {
    JsApproximateLocationProvider()
}

private class JsApproximateLocationProvider : ApproximateLocationProvider {
    override suspend fun requestApproximateLocation(): ApproximateLocationResult =
        suspendCoroutine { continuation ->
            val geolocation = navigator.asDynamic().geolocation
            if (geolocation == null) {
                continuation.resume(ApproximateLocationResult.Unavailable)
                return@suspendCoroutine
            }
            geolocation.getCurrentPosition(
                { position: dynamic ->
                    continuation.resume(
                        coarsenApproximateLocation(
                            latitude = position.coords.latitude as Double,
                            longitude = position.coords.longitude as Double,
                            reportedAccuracyMeters = position.coords.accuracy as Double,
                        ),
                    )
                },
                { error: dynamic ->
                    val denied = (error.code as? Int) == 1
                    continuation.resume(
                        if (denied) ApproximateLocationResult.PermissionDenied
                        else ApproximateLocationResult.Unavailable,
                    )
                },
                js("({ enableHighAccuracy: false, timeout: 10000, maximumAge: 300000 })"),
            )
        }
}

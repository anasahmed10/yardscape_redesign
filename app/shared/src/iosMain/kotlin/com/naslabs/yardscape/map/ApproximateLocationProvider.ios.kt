@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.naslabs.yardscape.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.cinterop.useContents
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.CoreLocation.kCLLocationAccuracyKilometer
import platform.Foundation.NSError
import platform.darwin.NSObject
import kotlin.coroutines.resume

@Composable
actual fun rememberApproximateLocationProvider(): ApproximateLocationProvider = remember {
    IosApproximateLocationProvider()
}

private class IosApproximateLocationProvider :
    ApproximateLocationProvider {
    private val locationDelegate = IosLocationDelegate(
        onAuthorizationChanged = ::onAuthorizationChanged,
        onLocationsUpdated = ::onLocationsUpdated,
        onLocationFailed = ::onLocationFailed,
    )
    private val locationManager = CLLocationManager().apply {
        delegate = locationDelegate
        desiredAccuracy = kCLLocationAccuracyKilometer
    }
    private var permissionContinuation: CancellableContinuation<Boolean>? = null
    private var locationContinuation: CancellableContinuation<ApproximateLocationResult>? = null

    override suspend fun requestApproximateLocation(): ApproximateLocationResult {
        if (!requestWhenInUsePermission()) return ApproximateLocationResult.PermissionDenied
        return suspendCancellableCoroutine { continuation ->
            locationContinuation?.resume(ApproximateLocationResult.Unavailable)
            locationContinuation = continuation
            locationManager.requestLocation()
        }
    }

    private suspend fun requestWhenInUsePermission(): Boolean = when (CLLocationManager.authorizationStatus()) {
        kCLAuthorizationStatusAuthorizedAlways,
        kCLAuthorizationStatusAuthorizedWhenInUse -> true
        kCLAuthorizationStatusDenied,
        kCLAuthorizationStatusRestricted -> false
        kCLAuthorizationStatusNotDetermined -> suspendCancellableCoroutine { continuation ->
            permissionContinuation?.resume(false)
            permissionContinuation = continuation
            locationManager.requestWhenInUseAuthorization()
        }
        else -> false
    }

    private fun onAuthorizationChanged(status: CLAuthorizationStatus) {
        val granted = status == kCLAuthorizationStatusAuthorizedAlways ||
            status == kCLAuthorizationStatusAuthorizedWhenInUse
        val denied = status == kCLAuthorizationStatusDenied ||
            status == kCLAuthorizationStatusRestricted
        if (granted || denied) {
            permissionContinuation?.resume(granted)
            permissionContinuation = null
        }
    }

    private fun onLocationsUpdated(locations: List<*>) {
        val location = locations.lastOrNull() as? CLLocation
        val result = location?.let {
            val center = it.coordinate.useContents { latitude to longitude }
            coarsenApproximateLocation(
                latitude = center.first,
                longitude = center.second,
                reportedAccuracyMeters = it.horizontalAccuracy,
            )
        } ?: ApproximateLocationResult.Unavailable
        locationContinuation?.resume(result)
        locationContinuation = null
    }

    private fun onLocationFailed() {
        locationContinuation?.resume(ApproximateLocationResult.Unavailable)
        locationContinuation = null
    }
}

private class IosLocationDelegate(
    private val onAuthorizationChanged: (CLAuthorizationStatus) -> Unit,
    private val onLocationsUpdated: (List<*>) -> Unit,
    private val onLocationFailed: () -> Unit,
) : NSObject(), CLLocationManagerDelegateProtocol {
    override fun locationManager(
        manager: CLLocationManager,
        didChangeAuthorizationStatus: CLAuthorizationStatus,
    ) {
        onAuthorizationChanged(didChangeAuthorizationStatus)
    }

    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        onLocationsUpdated(didUpdateLocations)
    }

    override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
        onLocationFailed()
    }
}

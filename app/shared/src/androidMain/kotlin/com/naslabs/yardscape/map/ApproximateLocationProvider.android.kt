package com.naslabs.yardscape.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CompletableDeferred

internal const val APPROXIMATE_LOCATION_PERMISSION: String = Manifest.permission.ACCESS_COARSE_LOCATION

@Composable
actual fun rememberApproximateLocationProvider(): ApproximateLocationProvider {
    val context = LocalContext.current.applicationContext
    val provider = remember(context) { AndroidApproximateLocationProvider(context) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
        provider::onPermissionResult,
    )
    SideEffect {
        provider.launchPermissionRequest = {
            permissionLauncher.launch(APPROXIMATE_LOCATION_PERMISSION)
        }
    }
    return provider
}

private class AndroidApproximateLocationProvider(
    private val context: Context,
) : ApproximateLocationProvider {
    var launchPermissionRequest: () -> Unit = {}
    private var pendingPermission: CompletableDeferred<Boolean>? = null

    override suspend fun requestApproximateLocation(): ApproximateLocationResult {
        val granted = context.checkSelfPermission(APPROXIMATE_LOCATION_PERMISSION) ==
            PackageManager.PERMISSION_GRANTED || requestPermission()
        if (!granted) return ApproximateLocationResult.PermissionDenied
        return readCoarseLocation()
    }

    fun onPermissionResult(granted: Boolean) {
        pendingPermission?.complete(granted)
        pendingPermission = null
    }

    private suspend fun requestPermission(): Boolean {
        pendingPermission?.complete(false)
        val response = CompletableDeferred<Boolean>()
        pendingPermission = response
        launchPermissionRequest()
        return response.await()
    }

    @SuppressLint("MissingPermission")
    private fun readCoarseLocation(): ApproximateLocationResult {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return ApproximateLocationResult.Unavailable
        val location = manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            ?: manager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            ?: return ApproximateLocationResult.Unavailable
        return coarsenApproximateLocation(
            latitude = location.latitude,
            longitude = location.longitude,
            reportedAccuracyMeters = location.accuracy.toDouble(),
        )
    }
}

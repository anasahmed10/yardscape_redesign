package com.naslabs.yardscape.map

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.naslabs.yardscape.domain.MapViewport
import com.naslabs.yardscape.domain.ViewportCenter
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.map.GestureOptions
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position
import kotlin.math.roundToInt

actual fun platformMapCapability(): PlatformMapCapability = PlatformMapCapability.Interactive

actual fun platformMapSupportsComposeOverlay(): Boolean = true

@Composable
actual fun PlatformMapSurface(
    state: PlatformMapState,
    modifier: Modifier,
    style: PlatformMapStyle,
    onViewportChanged: (MapViewport) -> Unit,
    onMarkerSelected: (String) -> Unit,
    onClusterSelected: (String) -> Unit,
    onMapLoaded: () -> Unit,
    onMapLoadFailed: (String?) -> Unit,
) {
    val cameraState = rememberCameraState(firstPosition = state.viewport.toCameraPosition())

    LaunchedEffect(state.viewport) {
        val requestedPosition = state.viewport.toCameraPosition()
        if (cameraState.position != requestedPosition) cameraState.position = requestedPosition
    }
    LaunchedEffect(cameraState) {
        snapshotFlow { cameraState.isCameraMoving to cameraState.position }
            .distinctUntilChanged()
            .drop(1)
            .filter { (isMoving, _) -> !isMoving }
            .collect { (_, position) -> onViewportChanged(position.toMapViewport()) }
    }

    BoxWithConstraints(modifier) {
        MaplibreMap(
            modifier = Modifier.fillMaxSize(),
            baseStyle = BaseStyle.Uri(style.styleUri),
            cameraState = cameraState,
            pitchRange = 0f..0f,
            options = MapOptions(gestureOptions = GestureOptions.RotationLocked),
            onMapLoadFinished = onMapLoaded,
            onMapLoadFailed = onMapLoadFailed,
        )

        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx().toDouble() }
        val heightPx = with(density) { maxHeight.toPx().toDouble() }
        val pixelScale = density.density.toDouble()
        val visibleViewport = cameraState.position.toMapViewport()
        state.clusters.forEach { cluster ->
            MapOverlayMarker(
                offset = projectMapPoint(
                    cluster.area.center.latitude,
                    cluster.area.center.longitude,
                    visibleViewport,
                    widthPx,
                    heightPx,
                    pixelScale,
                ),
                color = Color(0xFF1F5B4A),
                description = "${cluster.eventCount} sales near ${cluster.area.displayLabel}",
                label = cluster.eventCount.toString(),
                onClick = { onClusterSelected(cluster.id) },
            )
        }
        state.markers.forEach { marker ->
            MapOverlayMarker(
                offset = projectMapPoint(
                    marker.area.center.latitude,
                    marker.area.center.longitude,
                    visibleViewport,
                    widthPx,
                    heightPx,
                    pixelScale,
                ),
                color = if (marker.eventId == state.selectedEventId) Color(0xFFD76845) else Color(0xFF2F6F4E),
                description = "Open ${marker.title} near ${marker.area.displayLabel}",
                onClick = { onMarkerSelected(marker.eventId) },
            )
        }
    }
}

@Composable
private fun MapOverlayMarker(
    offset: MapScreenOffset,
    color: Color,
    description: String,
    label: String? = null,
    onClick: () -> Unit,
) {
    val markerSize = 48.dp
    val density = LocalDensity.current
    val halfMarkerPx = with(density) { markerSize.toPx() / 2f }
    Surface(
        onClick = onClick,
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (offset.xPx - halfMarkerPx).roundToInt(),
                    y = (offset.yPx - halfMarkerPx).roundToInt(),
                )
            }
            .size(markerSize)
            .semantics { contentDescription = description },
        shape = CircleShape,
        color = color,
        contentColor = Color.White,
        border = BorderStroke(3.dp, Color.White),
        shadowElevation = 4.dp,
    ) {
        if (label != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text(label, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun MapViewport.toCameraPosition(): CameraPosition = CameraPosition(
    target = Position(longitude = center.longitude, latitude = center.latitude),
    zoom = zoomLevel,
)

private fun CameraPosition.toMapViewport(): MapViewport = MapViewport(
    center = ViewportCenter(latitude = target.latitude, longitude = target.longitude),
    zoomLevel = zoom,
)

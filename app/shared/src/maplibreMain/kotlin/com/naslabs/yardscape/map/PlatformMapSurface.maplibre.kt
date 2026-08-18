package com.naslabs.yardscape.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.naslabs.yardscape.domain.MapViewport
import com.naslabs.yardscape.domain.ViewportCenter
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position

actual fun platformMapCapability(): PlatformMapCapability = PlatformMapCapability.Interactive

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
    val cameraState = rememberCameraState(
        firstPosition = state.viewport.toCameraPosition(),
    )

    LaunchedEffect(state.viewport) {
        val requestedPosition = state.viewport.toCameraPosition()
        if (cameraState.position != requestedPosition) {
            cameraState.position = requestedPosition
        }
    }
    LaunchedEffect(cameraState) {
        snapshotFlow { cameraState.isCameraMoving to cameraState.position }
            .distinctUntilChanged()
            .drop(1)
            .filter { (isMoving, _) -> !isMoving }
            .collect { (_, position) -> onViewportChanged(position.toMapViewport()) }
    }

    MaplibreMap(
        modifier = modifier,
        baseStyle = BaseStyle.Uri(style.styleUri),
        cameraState = cameraState,
        onMapLoadFinished = onMapLoaded,
        onMapLoadFailed = onMapLoadFailed,
    ) {
        state.clusters.forEachIndexed { index, cluster ->
            key(cluster.id) {
                val source = rememberGeoJsonSource(
                    GeoJsonData.Features(
                        Point(
                            Position(
                                longitude = cluster.area.center.longitude,
                                latitude = cluster.area.center.latitude,
                            ),
                        ),
                    ),
                )
                CircleLayer(
                    id = "yardscape-cluster-$index",
                    source = source,
                    radius = const(18.dp),
                    color = const(Color(0xFF1F5B4A)),
                    strokeColor = const(Color.White),
                    strokeWidth = const(3.dp),
                    onClick = {
                        onClusterSelected(cluster.id)
                        ClickResult.Consume
                    },
                )
            }
        }

        state.markers.forEachIndexed { index, marker ->
            key(marker.eventId) {
                val selected = marker.eventId == state.selectedEventId
                val source = rememberGeoJsonSource(
                    GeoJsonData.Features(
                        Point(
                            Position(
                                longitude = marker.area.center.longitude,
                                latitude = marker.area.center.latitude,
                            ),
                        ),
                    ),
                )
                CircleLayer(
                    id = "yardscape-event-$index",
                    source = source,
                    radius = const(if (selected) 11.dp else 8.dp),
                    color = const(if (selected) Color(0xFFD76845) else Color(0xFF3478A0)),
                    strokeColor = const(Color.White),
                    strokeWidth = const(3.dp),
                    onClick = {
                        onMarkerSelected(marker.eventId)
                        ClickResult.Consume
                    },
                )
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

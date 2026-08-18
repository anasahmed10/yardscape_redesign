package com.naslabs.yardscape.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.viewinterop.WebElementView
import com.naslabs.yardscape.domain.MapViewport
import com.naslabs.yardscape.domain.ViewportCenter
import kotlinx.browser.document
import org.maplibre.kmp.js.controls.NavigationControl
import org.maplibre.kmp.js.geometry.LngLat
import org.maplibre.kmp.js.map.JumpToOptions
import org.maplibre.kmp.js.map.Map
import org.maplibre.kmp.js.map.MapOptions
import org.maplibre.kmp.js.util.getVersion
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLElement

actual fun platformMapCapability(): PlatformMapCapability = PlatformMapCapability.Interactive

actual fun platformMapSupportsComposeOverlay(): Boolean = false

@Composable
@OptIn(ExperimentalComposeUiApi::class)
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
    var map by remember { mutableStateOf<Map?>(null) }
    var pendingProgrammaticViewport by remember { mutableStateOf<MapViewport?>(state.viewport) }
    var lastObservedViewport by remember { mutableStateOf(state.viewport) }
    val mapMarkers = remember { mutableListOf<Marker>() }
    val latestViewportChanged by rememberUpdatedState(onViewportChanged)
    val latestMarkerSelected by rememberUpdatedState(onMarkerSelected)
    val latestClusterSelected by rememberUpdatedState(onClusterSelected)
    val latestMapLoaded by rememberUpdatedState(onMapLoaded)
    val latestMapLoadFailed by rememberUpdatedState(onMapLoadFailed)

    WebElementView(
        modifier = modifier.onGloballyPositioned { map?.resize() },
        factory = {
            ensureMapLibreStyleSheet()
            document.createElement("div").unsafeCast<HTMLElement>().apply {
                setAttribute("style", "width:100%;height:100%;")
            }
        },
        update = { element ->
            if (map == null) {
                map = Map(MapOptions(element)).apply {
                    setStyle(style.styleUri)
                    addControl(NavigationControl(), "top-right")
                    on("load") {
                        resize()
                        latestMapLoaded()
                    }
                    on("error") { event -> latestMapLoadFailed(event.toString()) }
                    val publishCamera = {
                        val currentViewport = MapViewport(
                            center = ViewportCenter(
                                latitude = getCenter().lat,
                                longitude = getCenter().lng,
                            ),
                            zoomLevel = getZoom(),
                        )
                        val requestedViewport = pendingProgrammaticViewport
                        pendingProgrammaticViewport = null
                        if (
                            (requestedViewport == null || !currentViewport.approximatelyEquals(requestedViewport)) &&
                            !currentViewport.approximatelyEquals(lastObservedViewport)
                        ) {
                            lastObservedViewport = currentViewport
                            latestViewportChanged(
                                currentViewport,
                            )
                        }
                    }
                    on("moveend") { publishCamera() }
                    on("zoomend") { publishCamera() }
                    jumpTo(state.viewport.toJumpOptions())
                }
            }
        },
    )

    LaunchedEffect(map, state.viewport) {
        val activeMap = map ?: return@LaunchedEffect
        val center = activeMap.getCenter()
        if (
            center.lat != state.viewport.center.latitude ||
            center.lng != state.viewport.center.longitude ||
            activeMap.getZoom() != state.viewport.zoomLevel
        ) {
            pendingProgrammaticViewport = state.viewport
            activeMap.jumpTo(state.viewport.toJumpOptions())
        }
    }

    LaunchedEffect(map, state.markers, state.clusters, state.selectedEventId) {
        val activeMap = map ?: return@LaunchedEffect
        mapMarkers.forEach(Marker::remove)
        mapMarkers.clear()

        state.clusters.forEach { cluster ->
            val element = mapMarkerElement(
                description = "${cluster.eventCount} sales near ${cluster.area.displayLabel}",
                color = "#1F5B4A",
                label = cluster.eventCount.toString(),
                onClick = { latestClusterSelected(cluster.id) },
            )
            mapMarkers += Marker(jsObject<MarkerOptions> { this.element = element })
                .setLngLat(arrayOf(cluster.area.center.longitude, cluster.area.center.latitude))
                .addTo(activeMap)
        }
        state.markers.forEach { marker ->
            val element = mapMarkerElement(
                description = "Open ${marker.title} near ${marker.area.displayLabel}",
                color = if (marker.eventId == state.selectedEventId) "#D76845" else "#3478A0",
                onClick = { latestMarkerSelected(marker.eventId) },
            )
            mapMarkers += Marker(jsObject<MarkerOptions> { this.element = element })
                .setLngLat(arrayOf(marker.area.center.longitude, marker.area.center.latitude))
                .addTo(activeMap)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mapMarkers.forEach(Marker::remove)
            mapMarkers.clear()
            map?.remove()
            map = null
        }
    }
}

private fun MapViewport.toJumpOptions(): JumpToOptions = JumpToOptions(
    center = LngLat(center.longitude, center.latitude),
    zoom = zoomLevel,
)

private fun MapViewport.approximatelyEquals(other: MapViewport): Boolean =
    kotlin.math.abs(center.latitude - other.center.latitude) < 0.000001 &&
        kotlin.math.abs(center.longitude - other.center.longitude) < 0.000001 &&
        kotlin.math.abs(zoomLevel - other.zoomLevel) < 0.000001

private fun ensureMapLibreStyleSheet() {
    if (document.getElementById(MAPLIBRE_STYLE_ID) != null) return
    document.createElement("link").apply {
        id = MAPLIBRE_STYLE_ID
        setAttribute("rel", "stylesheet")
        setAttribute("href", "https://unpkg.com/maplibre-gl@${getVersion()}/dist/maplibre-gl.css")
        document.head?.appendChild(this)
    }
}

private fun mapMarkerElement(
    description: String,
    color: String,
    label: String? = null,
    onClick: () -> Unit,
): HTMLElement = document.createElement("button").unsafeCast<HTMLButtonElement>().apply {
    type = "button"
    setAttribute("aria-label", description)
    textContent = label.orEmpty()
    style.width = "48px"
    style.height = "48px"
    style.border = "3px solid white"
    style.borderRadius = "50%"
    style.background = color
    style.color = "white"
    style.fontWeight = "700"
    style.boxShadow = "0 2px 7px rgba(26, 52, 43, 0.28)"
    style.cursor = "pointer"
    onclick = { event ->
        event.stopPropagation()
        onClick()
    }
}

private const val MAPLIBRE_STYLE_ID = "yardscape-maplibre-css"

private inline fun <T> jsObject(block: T.() -> Unit): T =
    js("({})").unsafeCast<T>().apply(block)

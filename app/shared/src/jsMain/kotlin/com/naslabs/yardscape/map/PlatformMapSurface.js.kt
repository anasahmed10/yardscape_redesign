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
    val renderedMarkers = remember { mutableMapOf<String, RenderedMapMarker>() }
    val latestViewportChanged = rememberUpdatedState(onViewportChanged)
    val latestMarkerSelected = rememberUpdatedState(onMarkerSelected)
    val latestClusterSelected = rememberUpdatedState(onClusterSelected)
    val latestMapLoaded = rememberUpdatedState(onMapLoaded)
    val latestMapLoadFailed = rememberUpdatedState(onMapLoadFailed)

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
                        latestMapLoaded.value()
                    }
                    on("error") { event -> latestMapLoadFailed.value(event.toString()) }
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
                            latestViewportChanged.value(
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
        val currentItems = mapRenderItemsFor(
            state = state,
            onMarkerSelected = { eventId -> latestMarkerSelected.value(eventId) },
            onClusterSelected = { clusterId -> latestClusterSelected.value(clusterId) },
        )
        val reconciliation = reconcile(
            previous = renderedMarkers.mapValues { it.value.item },
            current = currentItems,
            previousSelectedId = renderedMarkers.values.firstOrNull { it.item.selected }?.item?.id,
            selectedId = currentItems.values.firstOrNull { it.selected }?.id,
        )
        reconciliation.removedIds.forEach { id -> renderedMarkers.remove(id)?.marker?.remove() }
        reconciliation.added.forEach { item ->
            renderedMarkers[item.id] = createRenderedMapMarker(item, activeMap)
        }
        (reconciliation.retainedIds + reconciliation.selectionChangedIds).forEach { id ->
            currentItems[id]?.let { item -> renderedMarkers[id]?.update(item) }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            renderedMarkers.values.forEach { it.marker.remove() }
            renderedMarkers.clear()
            map?.remove()
            map = null
        }
    }
}

private data class MapRenderItem(
    val id: String,
    val longitude: Double,
    val latitude: Double,
    val description: String,
    val color: String,
    val label: String,
    val selected: Boolean = false,
    val onClick: () -> Unit,
)

private class RenderedMapMarker(
    val marker: Marker,
    private val element: HTMLElement,
    var item: MapRenderItem,
) {
    fun update(next: MapRenderItem) {
        if (item.longitude != next.longitude || item.latitude != next.latitude) {
            marker.setLngLat(arrayOf(next.longitude, next.latitude))
        }
        if (item.description != next.description) element.setAttribute("aria-label", next.description)
        if (item.color != next.color) element.style.background = next.color
        if (item.label != next.label) element.textContent = next.label
        item = next
    }
}

private fun mapRenderItemsFor(
    state: PlatformMapState,
    onMarkerSelected: (String) -> Unit,
    onClusterSelected: (String) -> Unit,
): kotlin.collections.Map<String, MapRenderItem> = buildList {
    state.clusters.forEach { cluster ->
        add(
            MapRenderItem(
                id = cluster.id,
                longitude = cluster.area.center.longitude,
                latitude = cluster.area.center.latitude,
                description = "${cluster.eventCount} sales near ${cluster.area.displayLabel}",
                color = "#1F5B4A",
                label = cluster.eventCount.toString(),
                onClick = { onClusterSelected(cluster.id) },
            ),
        )
    }
    state.markers.forEach { marker ->
        add(
            MapRenderItem(
                id = marker.eventId,
                longitude = marker.area.center.longitude,
                latitude = marker.area.center.latitude,
                description = "Open ${marker.title} near ${marker.area.displayLabel}",
                color = if (marker.eventId == state.selectedEventId) "#D76845" else "#2F6F4E",
                label = "",
                selected = marker.eventId == state.selectedEventId,
                onClick = { onMarkerSelected(marker.eventId) },
            ),
        )
    }
}.associateBy(MapRenderItem::id)

private fun createRenderedMapMarker(item: MapRenderItem, map: Map): RenderedMapMarker {
    lateinit var rendered: RenderedMapMarker
    val element = mapMarkerElement(
        description = item.description,
        color = item.color,
        label = item.label,
        onClick = { rendered.item.onClick() },
    )
    val marker = Marker(jsObject<MarkerOptions> { this.element = element })
        .setLngLat(arrayOf(item.longitude, item.latitude))
        .addTo(map)
    return RenderedMapMarker(marker, element, item).also { rendered = it }
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

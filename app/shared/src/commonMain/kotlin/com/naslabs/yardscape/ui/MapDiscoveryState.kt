package com.naslabs.yardscape.ui

import com.naslabs.yardscape.domain.MapViewport
import com.naslabs.yardscape.domain.NeighborhoodCenter
import com.naslabs.yardscape.domain.PublicEventMarker
import com.naslabs.yardscape.domain.PublicEventPreview
import com.naslabs.yardscape.domain.PublicMapArea

enum class ViewportSearchReadiness {
    Synchronized,
    WaitingForDebounce,
    Ready,
}

enum class MapResultsSheetPosition {
    Collapsed,
    HalfExpanded,
    Expanded,
}

sealed interface MapAvailability {
    data object Loading : MapAvailability
    data object Available : MapAvailability
    data object Unavailable : MapAvailability
    data object Offline : MapAvailability
    data class Failed(val message: String) : MapAvailability
}

fun usesMapFallback(
    capability: com.naslabs.yardscape.map.PlatformMapCapability,
    availability: MapAvailability,
): Boolean = capability == com.naslabs.yardscape.map.PlatformMapCapability.StaticFallback ||
    availability == MapAvailability.Offline ||
    availability == MapAvailability.Unavailable ||
    availability is MapAvailability.Failed

enum class ApproximateLocationPermission {
    NotRequested,
    Requesting,
    Granted,
    Denied,
    Unavailable,
}

fun interface PublicMapAreaSource {
    fun areaFor(event: PublicEventPreview): PublicMapArea?
}

object SeededPublicMapAreaSource : PublicMapAreaSource {
    override fun areaFor(event: PublicEventPreview): PublicMapArea? {
        val neighborhood = event.publicLocation.neighborhood.trim()
        val city = event.publicLocation.city.trim()
        val center = when (neighborhood.lowercase()) {
            "maple ridge" -> NeighborhoodCenter(47.615, -122.210)
            "old mill" -> NeighborhoodCenter(47.625, -122.220)
            else -> if (city.equals("Riverton", ignoreCase = true)) {
                NeighborhoodCenter(47.618, -122.215)
            } else {
                return null
            }
        }
        return PublicMapArea(
            center = center,
            approximationRadiusMeters = if (neighborhood.isBlank()) 2_500 else 900,
            displayLabel = listOf(neighborhood, city).filter(String::isNotBlank).joinToString(", "),
        )
    }
}

data class MapDiscoveryState(
    val cameraViewportDraft: MapViewport? = null,
    val searchedViewport: MapViewport? = null,
    val markers: List<PublicEventMarker> = emptyList(),
    val selectedEventId: String? = null,
    val mapAvailability: MapAvailability = MapAvailability.Loading,
    val locationPermission: ApproximateLocationPermission = ApproximateLocationPermission.NotRequested,
    val sheetPosition: MapResultsSheetPosition = MapResultsSheetPosition.Collapsed,
    val viewportSearchReadiness: ViewportSearchReadiness = ViewportSearchReadiness.Synchronized,
) {
    val canSearchThisArea: Boolean
        get() = viewportSearchReadiness == ViewportSearchReadiness.Ready

    val hasInteractiveMap: Boolean
        get() = mapAvailability == MapAvailability.Available

    val selectedMarker: PublicEventMarker?
        get() = markers.firstOrNull { it.eventId == selectedEventId }

    fun synchronizeMarkers(markers: List<PublicEventMarker>): MapDiscoveryState {
        val synchronizedMarkers = markers.distinctBy(PublicEventMarker::eventId)
        return copy(
            markers = synchronizedMarkers,
            selectedEventId = selectedEventId?.takeIf { selectedId ->
                synchronizedMarkers.any { it.eventId == selectedId }
            },
        )
    }

    fun selectEvent(eventId: String?): MapDiscoveryState =
        copy(
            selectedEventId = eventId?.takeIf { selectedId ->
                markers.any { it.eventId == selectedId }
            },
        )

    fun requestApproximateLocation(): MapDiscoveryState =
        when (locationPermission) {
            ApproximateLocationPermission.NotRequested,
            ApproximateLocationPermission.Denied,
            -> copy(locationPermission = ApproximateLocationPermission.Requesting)
            ApproximateLocationPermission.Requesting,
            ApproximateLocationPermission.Granted,
            ApproximateLocationPermission.Unavailable,
            -> this
        }

    fun updateLocationPermission(permission: ApproximateLocationPermission): MapDiscoveryState =
        copy(locationPermission = permission)

    fun updateMapAvailability(availability: MapAvailability): MapDiscoveryState =
        copy(mapAvailability = availability)

    fun updateSheetPosition(position: MapResultsSheetPosition): MapDiscoveryState =
        copy(sheetPosition = position)

    fun onCameraViewportChanged(viewport: MapViewport): MapDiscoveryState =
        copy(
            cameraViewportDraft = viewport,
            viewportSearchReadiness = if (viewport == searchedViewport) {
                ViewportSearchReadiness.Synchronized
            } else {
                ViewportSearchReadiness.WaitingForDebounce
            },
        )

    fun onCameraViewportSettled(): MapDiscoveryState =
        if (viewportSearchReadiness == ViewportSearchReadiness.WaitingForDebounce) {
            copy(viewportSearchReadiness = ViewportSearchReadiness.Ready)
        } else {
            this
        }

    fun searchThisArea(): MapDiscoveryState =
        if (canSearchThisArea && cameraViewportDraft != null) {
            copy(
                searchedViewport = cameraViewportDraft,
                viewportSearchReadiness = ViewportSearchReadiness.Synchronized,
            )
        } else {
            this
        }

    fun clearSearchedArea(): MapDiscoveryState = copy(
        cameraViewportDraft = null,
        searchedViewport = null,
        selectedEventId = null,
        viewportSearchReadiness = ViewportSearchReadiness.Synchronized,
    )
}

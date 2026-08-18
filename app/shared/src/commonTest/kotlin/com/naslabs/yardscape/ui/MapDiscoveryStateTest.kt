package com.naslabs.yardscape.ui

import com.naslabs.yardscape.domain.MapViewport
import com.naslabs.yardscape.domain.NeighborhoodCenter
import com.naslabs.yardscape.domain.PublicEventMarker
import com.naslabs.yardscape.domain.PublicMapArea
import com.naslabs.yardscape.domain.ViewportCenter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import com.naslabs.yardscape.map.PlatformMapCapability

class MapDiscoveryStateTest {
    @Test
    fun defaultsKeepListDiscoveryUsableBeforeMapOrPermissionIsAvailable() {
        val state = MapDiscoveryState()

        assertEquals(MapAvailability.Loading, state.mapAvailability)
        assertEquals(ApproximateLocationPermission.NotRequested, state.locationPermission)
        assertEquals(MapResultsSheetPosition.Collapsed, state.sheetPosition)
        assertEquals(ViewportSearchReadiness.Synchronized, state.viewportSearchReadiness)
        assertFalse(state.canSearchThisArea)
    }

    @Test
    fun cameraMovementMustSettleBeforeTheDraftViewportCanBeSearched() {
        val searchedViewport = viewport(latitude = 47.61, longitude = -122.20, zoom = 11.0)
        val cameraDraft = viewport(latitude = 47.63, longitude = -122.24, zoom = 12.0)
        val initial = MapDiscoveryState(
            cameraViewportDraft = searchedViewport,
            searchedViewport = searchedViewport,
        )

        val moving = initial.onCameraViewportChanged(cameraDraft)
        assertEquals(cameraDraft, moving.cameraViewportDraft)
        assertEquals(searchedViewport, moving.searchedViewport)
        assertEquals(ViewportSearchReadiness.WaitingForDebounce, moving.viewportSearchReadiness)
        assertFalse(moving.canSearchThisArea)

        val settled = moving.onCameraViewportSettled()
        assertEquals(ViewportSearchReadiness.Ready, settled.viewportSearchReadiness)
        assertTrue(settled.canSearchThisArea)

        val searched = settled.searchThisArea()
        assertEquals(cameraDraft, searched.searchedViewport)
        assertEquals(ViewportSearchReadiness.Synchronized, searched.viewportSearchReadiness)
        assertFalse(searched.canSearchThisArea)
    }

    @Test
    fun markerAndResultSelectionShareOneIdAndInvalidSelectionsAreCleared() {
        val maple = marker("maple", "Maple Ridge Sale", 47.615, -122.21)
        val oldMill = marker("old-mill", "Old Mill Sale", 47.625, -122.23)
        val state = MapDiscoveryState()
            .synchronizeMarkers(listOf(maple, oldMill))
            .selectEvent(oldMill.eventId)

        assertEquals(oldMill.eventId, state.selectedEventId)
        assertEquals(oldMill, state.selectedMarker)

        val filtered = state.synchronizeMarkers(listOf(maple))
        assertEquals(null, filtered.selectedEventId)
        assertEquals(null, filtered.selectedMarker)

        val unknownSelection = filtered.selectEvent("blocked-or-filtered")
        assertEquals(null, unknownSelection.selectedEventId)
    }

    @Test
    fun selectionAndSheetChangesKeepTheStableMarkerInputForMapPresentation() {
        val maple = marker("maple", "Maple Ridge Sale", 47.615, -122.21)
        val oldMill = marker("old-mill", "Old Mill Sale", 47.625, -122.23)
        val state = MapDiscoveryState().synchronizeMarkers(listOf(maple, oldMill))

        val selected = state.selectEvent(maple.eventId)
        val movedSheet = selected.updateSheetPosition(MapResultsSheetPosition.HalfExpanded)

        assertSame(state.markers, selected.markers)
        assertSame(state.markers, movedSheet.markers)
        assertEquals(maple.eventId, movedSheet.selectedEventId)
        assertEquals(MapResultsSheetPosition.HalfExpanded, movedSheet.sheetPosition)
    }

    @Test
    fun mapFailureAndPermissionDenialDoNotRemoveAccessibleDiscoveryResults() {
        val marker = marker("maple", "Maple Ridge Sale", 47.615, -122.21)
        val initial = MapDiscoveryState()
            .synchronizeMarkers(listOf(marker))
            .selectEvent(marker.eventId)

        val requesting = initial.requestApproximateLocation()
        assertEquals(ApproximateLocationPermission.Requesting, requesting.locationPermission)

        val degraded = requesting
            .updateLocationPermission(ApproximateLocationPermission.Denied)
            .updateMapAvailability(MapAvailability.Offline)

        assertFalse(degraded.hasInteractiveMap)
        assertEquals(listOf(marker), degraded.markers)
        assertEquals(marker.eventId, degraded.selectedEventId)
        assertEquals(ApproximateLocationPermission.Denied, degraded.locationPermission)
        assertEquals(MapAvailability.Offline, degraded.mapAvailability)
    }

    @Test
    fun failedAndOfflineInteractiveMapsUseTheFallbackUntilRetry() {
        assertTrue(usesMapFallback(PlatformMapCapability.Interactive, MapAvailability.Offline))
        assertTrue(usesMapFallback(PlatformMapCapability.Interactive, MapAvailability.Failed("tiles")))
        assertFalse(usesMapFallback(PlatformMapCapability.Interactive, MapAvailability.Loading))
        assertTrue(usesMapFallback(PlatformMapCapability.StaticFallback, MapAvailability.Available))
    }

    private fun viewport(latitude: Double, longitude: Double, zoom: Double): MapViewport =
        MapViewport(
            center = ViewportCenter(latitude, longitude),
            zoomLevel = zoom,
        )

    private fun marker(
        eventId: String,
        title: String,
        latitude: Double,
        longitude: Double,
    ): PublicEventMarker = PublicEventMarker(
        eventId = eventId,
        title = title,
        area = PublicMapArea(
            center = NeighborhoodCenter(latitude, longitude),
            approximationRadiusMeters = 800,
            displayLabel = title,
        ),
    )
}

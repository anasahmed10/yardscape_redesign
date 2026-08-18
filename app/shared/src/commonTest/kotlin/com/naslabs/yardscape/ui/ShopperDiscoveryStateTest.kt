package com.naslabs.yardscape.ui

import com.naslabs.yardscape.data.SeededYardSaleData
import com.naslabs.yardscape.data.SeededYardSaleEventRepository
import com.naslabs.yardscape.domain.MapViewport
import com.naslabs.yardscape.domain.ViewportCenter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ShopperDiscoveryStateTest {
    @Test
    fun browseStartsInMapModeWithACompleteListAlternative() {
        val state = YardScapeAppState()

        assertEquals(DiscoveryDisplayMode.Map, state.discoveryState().displayMode)
        state.updateDiscoveryDisplayMode(DiscoveryDisplayMode.List)
        assertEquals(2, state.discoveryState().items.size)
    }

    @Test
    fun seededEventsCanBeFoundThroughKeywordNeighborhoodAndCategory() {
        val state = YardScapeAppState()

        state.updateDiscoveryQuery("vinyl")
        assertEquals(listOf(SeededYardSaleData.ESTATE_TOOLS_EVENT_ID), state.discoveryState().items.map { it.id })

        state.updateDiscoveryQuery("Maple Ridge")
        assertEquals(listOf(SeededYardSaleData.FAMILY_GARAGE_EVENT_ID), state.discoveryState().items.map { it.id })

        state.updateDiscoveryQuery("")
        state.toggleDiscoveryCategory("tools")
        assertEquals(listOf(SeededYardSaleData.ESTATE_TOOLS_EVENT_ID), state.discoveryState().items.map { it.id })
    }

    @Test
    fun dateAndDistanceFiltersUsePublicMockMetadata() {
        val state = YardScapeAppState()

        state.updateDiscoveryDate(DiscoveryDateFilter.Today)
        assertEquals(listOf(SeededYardSaleData.FAMILY_GARAGE_EVENT_ID), state.discoveryState().items.map { it.id })

        state.updateDiscoveryDate(DiscoveryDateFilter.Tomorrow)
        assertEquals(listOf(SeededYardSaleData.ESTATE_TOOLS_EVENT_ID), state.discoveryState().items.map { it.id })

        state.updateDiscoveryDate(DiscoveryDateFilter.Any)
        state.updateDiscoveryDistance(DiscoveryDistanceFilter.WithinTwoMiles)
        assertEquals(listOf(SeededYardSaleData.FAMILY_GARAGE_EVENT_ID), state.discoveryState().items.map { it.id })

        state.updateDiscoveryDistance(DiscoveryDistanceFilter.Any)
        state.updateDiscoveryDate(DiscoveryDateFilter.Weekend)
        assertEquals(DiscoveryDateFilter.Weekend, state.discoveryState().filters.date)
        assertTrue(state.discoveryState().hasNoMatches)
    }

    @Test
    fun noMatchStateAndResetProvideDeterministicRecovery() {
        val state = YardScapeAppState()

        state.updateDiscoveryQuery("no such sale")
        assertTrue(state.discoveryState().hasNoMatches)
        assertTrue(state.discoveryState().filters.isActive)

        state.clearDiscoveryFilters()
        assertFalse(state.discoveryState().hasNoMatches)
        assertEquals(2, state.discoveryState().items.size)
    }

    @Test
    fun emptyRepositoryReportsNoNearbyEventsRatherThanNoMatch() {
        val state = YardScapeAppState(
            repository = SeededYardSaleEventRepository(events = emptyList(), rsvps = emptyList()),
        )

        state.updateDiscoveryQuery("anything")

        assertTrue(state.discoveryState().hasNoNearbyEvents)
        assertFalse(state.discoveryState().hasNoMatches)
    }

    @Test
    fun browseAvailabilitySeparatesLoadingEmptyFilteredOfflineAndRecoverableStates() {
        val populated = YardScapeAppState().discoveryState()
        val noNearby = YardScapeAppState(
            repository = SeededYardSaleEventRepository(events = emptyList(), rsvps = emptyList()),
        ).discoveryState()
        val filtered = YardScapeAppState().apply {
            updateDiscoveryQuery("no such sale")
        }.discoveryState()

        assertEquals(ShopperBrowseAvailability.Loading, populated.browsePresentationFor(AppDataAvailability.Loading).availability)
        assertEquals(ShopperBrowseAvailability.Results, populated.browsePresentationFor(AppDataAvailability.Available).availability)
        assertEquals(ShopperBrowseAvailability.EmptyNearby, noNearby.browsePresentationFor(AppDataAvailability.Available).availability)
        assertEquals(ShopperBrowseAvailability.FilteredEmpty, filtered.browsePresentationFor(AppDataAvailability.Available).availability)
        assertEquals(ShopperBrowseAvailability.OfflineCached, populated.browsePresentationFor(AppDataAvailability.Offline).availability)
        val recoverable = populated.browsePresentationFor(
            AppDataAvailability.RecoverableError("Try again in a moment."),
        )
        assertEquals(ShopperBrowseAvailability.RecoverableError, recoverable.availability)
        assertEquals("Retry", recoverable.actionLabel)
    }

    @Test
    fun savedEventsPersistAcrossFiltersModesAndRoutesForTheSession() {
        val state = YardScapeAppState()
        val eventId = SeededYardSaleData.FAMILY_GARAGE_EVENT_ID

        assertTrue(state.toggleSavedEvent(eventId))
        state.updateDiscoveryQuery("vinyl")
        state.updateDiscoveryDisplayMode(DiscoveryDisplayMode.Map)
        state.navigateTo(YardScapePrimaryDestination.MyFinds)

        assertEquals(YardScapeRoute.MyFinds(), state.route)
        assertEquals(DiscoveryDisplayMode.Map, state.discoveryState().displayMode)
        assertEquals(listOf(eventId), state.savedItems().map { it.id })

        assertFalse(state.toggleSavedEvent(eventId))
        assertTrue(state.savedItems().isEmpty())
    }

    @Test
    fun savedEventDetailAndRsvpReturnToSavedContext() {
        val state = YardScapeAppState()
        val eventId = SeededYardSaleData.FAMILY_GARAGE_EVENT_ID
        state.toggleSavedEvent(eventId)
        state.navigateTo(YardScapePrimaryDestination.MyFinds)

        state.openEvent(eventId)
        assertEquals(YardScapePrimaryDestination.MyFinds, state.activePrimaryDestination)
        state.openRsvp(eventId)
        assertEquals(YardScapePrimaryDestination.MyFinds, state.activePrimaryDestination)
        assertTrue(state.navigateBack())
        assertEquals(YardScapeRoute.EventDetail(eventId, YardScapePrimaryDestination.MyFinds), state.route)
        assertTrue(state.navigateBack())
        assertEquals(YardScapeRoute.MyFinds(), state.route)
    }

    @Test
    fun discoveryAndSavedStateContainNoProtectedLocationValues() {
        val state = YardScapeAppState()
        state.toggleSavedEvent(SeededYardSaleData.FAMILY_GARAGE_EVENT_ID)

        val publicStateText = state.discoveryState().toString() + state.savedItems().toString()

        assertFalse(publicStateText.contains("123 Cedar Street"))
        assertFalse(publicStateText.contains("418 Juniper Avenue"))
        assertFalse(publicStateText.contains("47.6101"))
        assertFalse(publicStateText.contains("-122.2142"))
        assertTrue(publicStateText.contains("Maple Ridge"))
        assertTrue(publicStateText.contains("2 mi"))
    }

    @Test
    fun unknownEventsCannotBeInjectedIntoSavedState() {
        val state = YardScapeAppState()

        assertFalse(state.toggleSavedEvent("private-or-unknown-event"))
        assertTrue(state.savedEventIds.isEmpty())
    }

    @Test
    fun listResultsAndPublicMapMarkersUseTheSameFiltersAndSelection() {
        val state = YardScapeAppState()
        val familyEventId = SeededYardSaleData.FAMILY_GARAGE_EVENT_ID

        assertEquals(
            state.discoveryState().items.map { it.id },
            state.mapDiscoveryState.markers.map { it.eventId },
        )
        assertTrue(state.selectDiscoveryEvent(familyEventId))
        assertEquals(familyEventId, state.mapDiscoveryState.selectedEventId)

        state.updateDiscoveryQuery("vinyl")

        assertEquals(
            state.discoveryState().items.map { it.id },
            state.mapDiscoveryState.markers.map { it.eventId },
        )
        assertEquals(null, state.mapDiscoveryState.selectedEventId)
    }

    @Test
    fun mapViewportSelectionAndSheetPositionSurviveDetailAndBackNavigation() {
        val state = YardScapeAppState()
        val eventId = SeededYardSaleData.FAMILY_GARAGE_EVENT_ID
        val viewport = MapViewport(
            center = ViewportCenter(47.62, -122.22),
            zoomLevel = 12.0,
        )
        state.updateMapCameraViewport(viewport)
        state.settleMapCameraViewport()
        state.searchMapCameraArea()
        state.updateMapResultsSheetPosition(MapResultsSheetPosition.Expanded)
        state.selectDiscoveryEvent(eventId)
        val beforeDetail = state.mapDiscoveryState

        state.openEvent(eventId)
        assertTrue(state.navigateBack())

        assertEquals(YardScapeRoute.Browse, state.route)
        assertEquals(beforeDetail, state.mapDiscoveryState)
    }

    @Test
    fun blockingASelectedEventRemovesItsMarkerAndClearsMapSelection() {
        val state = YardScapeAppState()
        val eventId = SeededYardSaleData.ESTATE_TOOLS_EVENT_ID
        assertTrue(state.selectDiscoveryEvent(eventId))

        assertTrue(state.blockHostForEvent(eventId))

        assertFalse(state.mapDiscoveryState.markers.any { it.eventId == eventId })
        assertEquals(null, state.mapDiscoveryState.selectedEventId)
        assertIs<LocationRevealState.Blocked>(state.detailStateFor(eventId)?.revealState)
    }

    @Test
    fun offlineAppStartsWithADegradedMapWhileKeepingListResults() {
        val state = YardScapeAppState(dataAvailability = AppDataAvailability.Offline)

        assertEquals(MapAvailability.Offline, state.mapDiscoveryState.mapAvailability)
        assertEquals(2, state.discoveryState().items.size)
        assertEquals(2, state.mapDiscoveryState.markers.size)
    }

    @Test
    fun retryingARecoverableBrowseErrorRestoresDataAndRestartsMapLoading() {
        val state = YardScapeAppState(
            dataAvailability = AppDataAvailability.RecoverableError("Try again in a moment."),
        )

        assertTrue(state.retryBrowseData())

        assertEquals(AppDataAvailability.Available, state.dataAvailability)
        assertEquals(MapAvailability.Loading, state.mapDiscoveryState.mapAvailability)
        assertEquals(2, state.discoveryState().items.size)
        assertFalse(state.retryBrowseData())
    }

    @Test
    fun repeatedLocationRequestsAreRejectedWhileOneIsPending() {
        val state = YardScapeAppState()

        assertTrue(state.requestApproximateLocation())
        assertFalse(state.requestApproximateLocation())
    }

    @Test
    fun searchThisAreaUpdatesBothMapMarkersAndListResults() {
        val state = YardScapeAppState()
        state.updateMapCameraViewport(
            MapViewport(
                center = ViewportCenter(48.8, -123.8),
                zoomLevel = 14.0,
            ),
        )
        state.settleMapCameraViewport()

        state.searchMapCameraArea()

        val discovery = state.discoveryState()
        val presentation = discovery.browsePresentationFor(AppDataAvailability.Available)
        assertTrue(discovery.items.isEmpty())
        assertEquals(2, discovery.totalEventCount)
        assertFalse(discovery.filters.isActive)
        assertTrue(state.mapDiscoveryState.markers.isEmpty())
        assertEquals("No sales in this map area", presentation.title)
        assertEquals("Show all nearby sales", presentation.actionLabel)
    }

    @Test
    fun showAllNearbySalesClearsACommittedEmptyMapArea() {
        val state = YardScapeAppState()
        state.updateMapCameraViewport(
            MapViewport(
                center = ViewportCenter(48.8, -123.8),
                zoomLevel = 14.0,
            ),
        )
        state.settleMapCameraViewport()
        state.searchMapCameraArea()
        assertTrue(state.discoveryState().items.isEmpty())

        state.showAllNearbySales()

        assertEquals(2, state.discoveryState().items.size)
        assertEquals(2, state.mapDiscoveryState.markers.size)
        assertEquals(null, state.mapDiscoveryState.searchedViewport)
    }
}

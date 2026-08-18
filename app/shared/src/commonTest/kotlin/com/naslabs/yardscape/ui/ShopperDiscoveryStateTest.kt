package com.naslabs.yardscape.ui

import com.naslabs.yardscape.data.SeededYardSaleData
import com.naslabs.yardscape.data.SeededYardSaleEventRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShopperDiscoveryStateTest {
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
}

package com.naslabs.yardscape.ui

import com.naslabs.yardscape.data.SeededYardSaleData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class YardScapeNavigationTest {
    @Test
    fun everyPrimaryDestinationIsReachableAndSelected() {
        val state = YardScapeAppState()

        YardScapePrimaryDestination.entries.forEach { destination ->
            state.navigateTo(destination)

            assertEquals(destination, state.activePrimaryDestination)
            assertEquals(destination, state.route.primaryDestination)
        }
    }

    @Test
    fun browseDetailRsvpAndBackPreserveEventContext() {
        val state = YardScapeAppState()
        val eventId = SeededYardSaleData.FAMILY_GARAGE_EVENT_ID

        state.openEvent(eventId)
        assertEquals(YardScapeRoute.EventDetail(eventId), state.route)
        assertEquals(YardScapePrimaryDestination.Browse, state.activePrimaryDestination)

        state.openRsvp(eventId)
        assertEquals(YardScapeRoute.Rsvp(eventId), state.route)
        assertTrue(state.navigateBack())
        assertEquals(YardScapeRoute.EventDetail(eventId), state.route)
        assertTrue(state.navigateBack())
        assertEquals(YardScapeRoute.Browse, state.route)
        assertFalse(state.navigateBack())
    }

    @Test
    fun hostEditorBackReturnsToSeparatedHostWorkspace() {
        val state = YardScapeAppState()

        state.navigateTo(YardScapePrimaryDestination.Host)
        state.openHostCreateEdit(SeededYardSaleData.DRAFT_EVENT_ID)

        assertEquals(YardScapePrimaryDestination.Host, state.activePrimaryDestination)
        assertTrue(state.navigateBack())
        assertEquals(YardScapeRoute.Host, state.route)
    }

    @Test
    fun routePathsRoundTripForDeepLinkShapedState() {
        val routes = listOf(
            YardScapeRoute.Browse,
            YardScapeRoute.Saved,
            YardScapeRoute.Host,
            YardScapeRoute.Account,
            YardScapeRoute.EventDetail("event-123"),
            YardScapeRoute.Rsvp("event-123"),
            YardScapeRoute.HostCreateEdit(),
            YardScapeRoute.HostCreateEdit("event-123"),
        )

        routes.forEach { route ->
            assertEquals(route, YardScapeRoute.fromPath(route.path))
        }
        assertEquals(YardScapeRoute.EventDetail("event-123"), YardScapeRoute.fromPath("/events/event-123?tab=overview"))
        assertNull(YardScapeRoute.fromPath("/private/location/123-cedar-street"))
    }

    @Test
    fun navigationLabelsNeverContainProtectedLocationData() {
        val routes = listOf(
            YardScapeRoute.Browse,
            YardScapeRoute.Saved,
            YardScapeRoute.Host,
            YardScapeRoute.Account,
            YardScapeRoute.EventDetail(SeededYardSaleData.FAMILY_GARAGE_EVENT_ID),
            YardScapeRoute.Rsvp(SeededYardSaleData.FAMILY_GARAGE_EVENT_ID),
            YardScapeRoute.HostCreateEdit(SeededYardSaleData.DRAFT_EVENT_ID),
        )
        val labels = buildList {
            addAll(routes.map { it.destinationLabel })
            addAll(YardScapePrimaryDestination.entries.map { it.label })
            addAll(YardScapePrimaryDestination.entries.map { it.contextLabel })
        }.joinToString()

        assertFalse(labels.contains("123 Cedar Street"))
        assertFalse(labels.contains("418 Juniper Avenue"))
        assertFalse(labels.contains("47.6101"))
    }

    @Test
    fun appStateCanAcceptAValidPathWithoutExternalDeepLinkHandling() {
        val state = YardScapeAppState()

        assertTrue(state.navigateToPath("/host/events/${SeededYardSaleData.DRAFT_EVENT_ID}/edit"))
        assertEquals(YardScapeRoute.HostCreateEdit(SeededYardSaleData.DRAFT_EVENT_ID), state.route)
        assertFalse(state.navigateToPath("/unknown"))
        assertEquals(YardScapeRoute.HostCreateEdit(SeededYardSaleData.DRAFT_EVENT_ID), state.route)
    }
}

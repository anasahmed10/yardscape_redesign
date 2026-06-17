package com.naslabs.yardscape.ui

import com.naslabs.yardscape.data.SeededYardSaleData
import com.naslabs.yardscape.data.SeededYardSaleEventRepository
import com.naslabs.yardscape.domain.LocationVisibility
import com.naslabs.yardscape.domain.Rsvp
import com.naslabs.yardscape.domain.RsvpStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class YardScapeAppStateTest {
    @Test
    fun browseIsInitialRouteAndShowsSeededEvents() {
        val state = YardScapeAppState()

        assertIs<YardScapeRoute.Browse>(state.route)
        assertEquals(
            listOf(
                SeededYardSaleData.FAMILY_GARAGE_EVENT_ID,
                SeededYardSaleData.ESTATE_TOOLS_EVENT_ID,
            ),
            state.browseItems().map { it.id },
        )
    }

    @Test
    fun selectingBrowseEventNavigatesToPublicDetail() {
        val state = YardScapeAppState()

        state.openEvent(SeededYardSaleData.FAMILY_GARAGE_EVENT_ID)

        assertEquals(
            YardScapeRoute.EventDetail(SeededYardSaleData.FAMILY_GARAGE_EVENT_ID),
            state.route,
        )
        assertNotNull(state.selectedEventDetailState())
    }

    @Test
    fun browseDisplayItemsDoNotContainExactAddresses() {
        val itemText = YardScapeAppState().browseItems().joinToString()

        assertFalse(itemText.contains("123 Cedar Street"))
        assertFalse(itemText.contains("418 Juniper Avenue"))
        assertFalse(itemText.contains("47.6101"))
        assertFalse(itemText.contains("-122.2142"))
    }

    @Test
    fun eventDetailBeforeRsvpDoesNotRevealExactAddress() {
        val state = YardScapeAppState()

        val detailState = state.detailStateFor(SeededYardSaleData.FAMILY_GARAGE_EVENT_ID)

        assertIs<LocationRevealState.Pending>(detailState?.revealState)
        assertFalse(detailState.toString().contains("123 Cedar Street"))
    }

    @Test
    fun confirmingRsvpRevealsExactAddressForPublishedEvent() {
        val state = YardScapeAppState()

        state.confirmRsvp(SeededYardSaleData.FAMILY_GARAGE_EVENT_ID)
        val detailState = state.detailStateFor(SeededYardSaleData.FAMILY_GARAGE_EVENT_ID)

        val revealed = assertIs<LocationRevealState.Revealed>(detailState?.revealState)
        assertEquals("123 Cedar Street", revealed.exactAddress.streetAddress)
    }

    @Test
    fun revokedAndExpiredAccessHideExactAddress() {
        val revokedState = YardScapeAppState(
            repository = SeededYardSaleEventRepository(
                rsvps = listOf(
                    Rsvp(
                        id = "revoked",
                        eventId = SeededYardSaleData.ESTATE_TOOLS_EVENT_ID,
                        shopperId = "shopper-revoked",
                        status = RsvpStatus.ACCEPTED,
                        locationVisibility = LocationVisibility.REVOKED,
                    ),
                ),
            ),
            shopperId = "shopper-revoked",
        ).detailStateFor(SeededYardSaleData.ESTATE_TOOLS_EVENT_ID)
        val expiredState = YardScapeAppState(
            repository = SeededYardSaleEventRepository(
                rsvps = listOf(
                    Rsvp(
                        id = "expired",
                        eventId = SeededYardSaleData.ESTATE_TOOLS_EVENT_ID,
                        shopperId = "shopper-expired",
                        status = RsvpStatus.ACCEPTED,
                        locationVisibility = LocationVisibility.EXPIRED,
                    ),
                ),
            ),
            shopperId = "shopper-expired",
        ).detailStateFor(SeededYardSaleData.ESTATE_TOOLS_EVENT_ID)

        assertIs<LocationRevealState.Revoked>(revokedState?.revealState)
        assertIs<LocationRevealState.Expired>(expiredState?.revealState)
        assertFalse(revokedState.toString().contains("418 Juniper Avenue"))
        assertFalse(expiredState.toString().contains("418 Juniper Avenue"))
    }

    @Test
    fun cancelledEventHidesExactAddressEvenWithAcceptedRsvp() {
        val cancelledState = YardScapeAppState(
            shopperId = SeededYardSaleData.SHOPPER_WITH_ACCEPTED_ACCESS_ID,
        ).detailStateFor(SeededYardSaleData.CANCELLED_EVENT_ID)

        assertIs<LocationRevealState.Cancelled>(cancelledState?.revealState)
        assertFalse(cancelledState.toString().contains("418 Juniper Avenue"))
    }
}

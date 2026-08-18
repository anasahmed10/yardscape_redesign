package com.naslabs.yardscape.ui

import com.naslabs.yardscape.data.SeededYardSaleData
import com.naslabs.yardscape.data.SeededYardSaleEventRepository
import com.naslabs.yardscape.domain.EventStatus
import com.naslabs.yardscape.domain.LocationVisibility
import com.naslabs.yardscape.domain.Rsvp
import com.naslabs.yardscape.domain.RsvpStatus
import com.naslabs.yardscape.domain.SaleWindow
import com.naslabs.yardscape.scenarios.MockScenarioCatalog
import com.naslabs.yardscape.scenarios.MockScenarioId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ShopperRsvpStateTest {
    @Test
    fun directRsvpRoutesCannotRestoreRevokedOrExpiredAccess() {
        listOf(LocationVisibility.REVOKED, LocationVisibility.EXPIRED).forEach { visibility ->
            val eventId = SeededYardSaleData.FAMILY_GARAGE_EVENT_ID
            val shopperId = "direct-${visibility.name.lowercase()}"
            val repository = SeededYardSaleEventRepository(
                rsvps = listOf(restrictedRsvp(eventId, shopperId, visibility)),
            )
            val expectedDetailRoute = YardScapeRoute.EventDetail(
                eventId = eventId,
                origin = YardScapePrimaryDestination.MyFinds,
                myFindsSection = MyFindsSection.Rsvps,
            )
            val state = YardScapeAppState(
                repository = repository,
                shopperId = shopperId,
                initialRoute = YardScapeRoute.Rsvp(
                    eventId = eventId,
                    origin = YardScapePrimaryDestination.MyFinds,
                    myFindsSection = MyFindsSection.Rsvps,
                ),
            )

            state.confirmRsvp(eventId)

            assertEquals(expectedDetailRoute, state.route)
            assertEquals(visibility, repository.rsvpFor(eventId, shopperId)?.locationVisibility)
            assertNull(repository.exactLocationFor(eventId, shopperId, SeededYardSaleData.BASE_NOW_EPOCH_MILLIS))

            state.openRsvp(eventId)

            assertEquals(expectedDetailRoute, state.route)
            assertEquals(visibility, repository.rsvpFor(eventId, shopperId)?.locationVisibility)
        }
    }

    @Test
    fun directRsvpCallsRejectUnavailableStatusEndedWindowAndCapacity() {
        val eventId = SeededYardSaleData.FAMILY_GARAGE_EVENT_ID
        val baseEvent = SeededYardSaleData.events.first { it.id == eventId }
        val unavailableEvents = listOf(
            baseEvent.copy(status = EventStatus.DRAFT),
            baseEvent.copy(status = EventStatus.CANCELLED),
            baseEvent.copy(status = EventStatus.COMPLETED),
            baseEvent.copy(status = EventStatus.HIDDEN),
            baseEvent.copy(
                saleWindow = SaleWindow(
                    startsAtEpochMillis = SeededYardSaleData.BASE_NOW_EPOCH_MILLIS - 2_000L,
                    endsAtEpochMillis = SeededYardSaleData.BASE_NOW_EPOCH_MILLIS - 1_000L,
                ),
            ),
        )

        unavailableEvents.forEach { event ->
            val repository = SeededYardSaleEventRepository(events = listOf(event), rsvps = emptyList())
            val expectedRoute = YardScapeRoute.EventDetail(
                eventId = eventId,
                origin = YardScapePrimaryDestination.MyFinds,
                myFindsSection = MyFindsSection.Rsvps,
            )
            val state = YardScapeAppState(
                repository = repository,
                shopperId = "unavailable-shopper",
                initialRoute = YardScapeRoute.Rsvp(
                    eventId = eventId,
                    origin = YardScapePrimaryDestination.MyFinds,
                    myFindsSection = MyFindsSection.Rsvps,
                ),
            )

            state.confirmRsvp(eventId)

            assertEquals(expectedRoute, state.route)
            assertNull(repository.rsvpFor(eventId, "unavailable-shopper"))

            state.openRsvp(eventId)

            assertEquals(expectedRoute, state.route)
            assertNull(repository.rsvpFor(eventId, "unavailable-shopper"))
        }

        val capacityRepository = SeededYardSaleEventRepository(events = listOf(baseEvent), rsvps = emptyList())
        val capacityState = YardScapeAppState(
            repository = capacityRepository,
            shopperId = "capacity-shopper",
            eventCapacitySource = EventCapacitySource { it == eventId },
            initialRoute = YardScapeRoute.Rsvp(
                eventId = eventId,
                origin = YardScapePrimaryDestination.MyFinds,
                myFindsSection = MyFindsSection.Rsvps,
            ),
        )

        capacityState.confirmRsvp(eventId)

        assertEquals(
            YardScapeRoute.EventDetail(
                eventId = eventId,
                origin = YardScapePrimaryDestination.MyFinds,
                myFindsSection = MyFindsSection.Rsvps,
            ),
            capacityState.route,
        )
        assertNull(capacityRepository.rsvpFor(eventId, "capacity-shopper"))
    }

    @Test
    fun blockedDirectRsvpCallCannotRestoreAccessAndKeepsMyFindsOrigin() {
        val eventId = SeededYardSaleData.ESTATE_TOOLS_EVENT_ID
        val shopperId = "blocked-direct-shopper"
        val repository = SeededYardSaleEventRepository(
            rsvps = listOf(
                restrictedRsvp(eventId, shopperId, LocationVisibility.RSVP_ACCEPTED),
            ),
        )
        val state = YardScapeAppState(
            repository = repository,
            shopperId = shopperId,
            initialRoute = YardScapeRoute.Rsvp(
                eventId = eventId,
                origin = YardScapePrimaryDestination.MyFinds,
                myFindsSection = MyFindsSection.Rsvps,
            ),
        )
        assertTrue(state.blockHostForEvent(eventId))

        state.confirmRsvp(eventId)

        assertEquals(
            YardScapeRoute.EventDetail(
                eventId = eventId,
                origin = YardScapePrimaryDestination.MyFinds,
                myFindsSection = MyFindsSection.Rsvps,
            ),
            state.route,
        )
        assertEquals(LocationVisibility.REVOKED, repository.rsvpFor(eventId, shopperId)?.locationVisibility)
        assertNull(repository.exactLocationFor(eventId, shopperId, SeededYardSaleData.BASE_NOW_EPOCH_MILLIS))
    }

    @Test
    fun deniedDirectRsvpFromMyFindsKeepsRsvpSection() {
        val eventId = SeededYardSaleData.CANCELLED_EVENT_ID
        val state = YardScapeAppState(
            initialRoute = YardScapeRoute.MyFinds(MyFindsSection.Rsvps),
        )

        state.openRsvp(eventId)

        assertEquals(
            YardScapeRoute.EventDetail(
                eventId = eventId,
                origin = YardScapePrimaryDestination.MyFinds,
                myFindsSection = MyFindsSection.Rsvps,
            ),
            state.route,
        )
    }

    @Test
    fun endedPublishedEventUsesExpiredRevealStateWithoutRsvpAction() {
        val eventId = SeededYardSaleData.FAMILY_GARAGE_EVENT_ID
        val endedEvent = SeededYardSaleData.events.first { it.id == eventId }.copy(
            saleWindow = SaleWindow(
                startsAtEpochMillis = SeededYardSaleData.BASE_NOW_EPOCH_MILLIS - 2_000L,
                endsAtEpochMillis = SeededYardSaleData.BASE_NOW_EPOCH_MILLIS - 1_000L,
            ),
        )
        val state = assertNotNull(YardScapeAppState(
            repository = SeededYardSaleEventRepository(events = listOf(endedEvent), rsvps = emptyList()),
            shopperId = "ended-shopper",
        ).detailStateFor(eventId))

        assertIs<LocationRevealState.Expired>(state.revealState)
        assertFalse(state.shouldShowRsvpAction)
    }

    @Test
    fun rsvpActionIsAvailableOnlyBeforeAcceptanceOrWhileRequestIsPending() {
        val detail = MockScenarioCatalog.createAppState(MockScenarioId.NewShopper)
            .detailStateFor(SeededYardSaleData.FAMILY_GARAGE_EVENT_ID)
            ?.detail
        assertNotNull(detail)
        val exactAddress = MockScenarioCatalog.createAppState(MockScenarioId.AcceptedAccess)
            .myRsvpItems()
            .first { it.state == ShopperRsvpUiState.Accepted }
            .exactAddress
        assertNotNull(exactAddress)

        val expectedVisibility = listOf(
            LocationRevealState.NotRequested to true,
            LocationRevealState.Pending to true,
            LocationRevealState.Revoked to false,
            LocationRevealState.Expired to false,
            LocationRevealState.Cancelled to false,
            LocationRevealState.Blocked to false,
            LocationRevealState.Revealed(exactAddress) to false,
        )

        expectedVisibility.forEach { (revealState, expected) ->
            val state = EventDetailState(detail = detail, revealState = revealState)
            assertEquals(expected, state.shouldShowRsvpAction, "Unexpected RSVP action for $revealState")
        }
    }

    @Test
    fun deterministicScenariosCoverEveryShopperRsvpState() {
        val expected = mapOf(
            MockScenarioId.PendingRsvp to ShopperRsvpUiState.Requested,
            MockScenarioId.AcceptedAccess to ShopperRsvpUiState.Accepted,
            MockScenarioId.EventAtCapacity to ShopperRsvpUiState.Full,
            MockScenarioId.WaitlistedRsvp to ShopperRsvpUiState.Waitlisted,
            MockScenarioId.DeclinedRsvp to ShopperRsvpUiState.Declined,
            MockScenarioId.CancelledRsvp to ShopperRsvpUiState.Cancelled,
            MockScenarioId.RevokedAccess to ShopperRsvpUiState.Revoked,
            MockScenarioId.ExpiredAccess to ShopperRsvpUiState.Expired,
        )

        expected.forEach { (scenarioId, expectedState) ->
            val states = MockScenarioCatalog.createAppState(scenarioId).myRsvpItems().map { it.state }
            assertTrue(expectedState in states, "$scenarioId should include $expectedState, but was $states")
        }
    }

    @Test
    fun exactAddressAndDirectionsExistOnlyForActiveAcceptedAccess() {
        MockScenarioId.entries.forEach { scenarioId ->
            val state = MockScenarioCatalog.createAppState(scenarioId)
            state.myRsvpItems().forEach { item ->
                if (item.exactAddress != null) {
                    assertEquals(ShopperRsvpUiState.Accepted, item.state)
                    assertTrue(item.canOpenDirections)
                } else {
                    assertFalse(item.canOpenDirections)
                    assertFalse(item.toString().contains("123 Cedar Street"))
                    assertFalse(item.toString().contains("418 Juniper Avenue"))
                }
            }
        }
    }

    @Test
    fun cancellationRequiresConfirmationAndImmediatelyClearsExactLocation() {
        val state = MockScenarioCatalog.createAppState(MockScenarioId.AcceptedAccess)
        val eventId = SeededYardSaleData.ESTATE_TOOLS_EVENT_ID
        assertNotNull(state.myRsvpItems().first { it.eventId == eventId }.exactAddress)

        assertTrue(state.requestRsvpCancellation(eventId))
        assertEquals(eventId, state.pendingRsvpCancellationEventId)
        assertNotNull(state.myRsvpItems().first { it.eventId == eventId }.exactAddress)

        assertTrue(state.confirmRsvpCancellation())
        val cancelled = state.myRsvpItems().first { it.eventId == eventId }
        assertEquals(ShopperRsvpUiState.Cancelled, cancelled.state)
        assertNull(cancelled.exactAddress)
        assertNull(state.directionsEventId)
    }

    @Test
    fun dismissingCancellationKeepsAcceptedAccess() {
        val state = MockScenarioCatalog.createAppState(MockScenarioId.AcceptedAccess)
        val eventId = SeededYardSaleData.ESTATE_TOOLS_EVENT_ID

        assertTrue(state.requestRsvpCancellation(eventId))
        state.dismissRsvpCancellation()

        assertNull(state.pendingRsvpCancellationEventId)
        assertNotNull(state.myRsvpItems().first { it.eventId == eventId }.exactAddress)
    }

    @Test
    fun revokeBlockAndExpiryClearStaleDirectionsAndAddress() {
        listOf<(YardScapeAppState, String) -> Boolean>(
            { state, eventId -> state.revokeRsvpAccess(eventId) },
            { state, eventId -> state.blockHostForEvent(eventId) },
            { state, eventId -> state.expireRsvpAccess(eventId) },
        ).forEach { transition ->
            val state = MockScenarioCatalog.createAppState(MockScenarioId.AcceptedAccess)
            val eventId = SeededYardSaleData.ESTATE_TOOLS_EVENT_ID
            assertNotNull(state.requestDirections(eventId))
            assertEquals(eventId, state.directionsEventId)

            assertTrue(transition(state, eventId))

            assertNull(state.myRsvpItems().first { it.eventId == eventId }.exactAddress)
            assertNull(state.requestDirections(eventId))
            assertNull(state.directionsEventId)
        }
    }

    @Test
    fun reminderAndCalendarEntriesAreLocalMockState() {
        val state = MockScenarioCatalog.createAppState(MockScenarioId.PendingRsvp)
        val eventId = SeededYardSaleData.FAMILY_GARAGE_EVENT_ID

        assertTrue(state.addMockReminder(eventId))
        assertTrue(state.prepareMockCalendarExport(eventId))

        val item = state.myRsvpItems().single()
        assertTrue(item.reminderAdded)
        assertTrue(item.calendarExportPrepared)
        assertNull(state.requestDirections(eventId))
    }

    @Test
    fun pendingFullRevokedAndCancelledStatesHaveClearNextActions() {
        val states = listOf(
            ShopperRsvpUiState.Requested,
            ShopperRsvpUiState.Full,
            ShopperRsvpUiState.Revoked,
            ShopperRsvpUiState.Cancelled,
        )

        states.forEach { assertTrue(it.nextAction.isNotBlank()) }
    }

    private fun restrictedRsvp(
        eventId: String,
        shopperId: String,
        visibility: LocationVisibility,
    ): Rsvp = Rsvp(
        id = "rsvp-$shopperId",
        eventId = eventId,
        shopperId = shopperId,
        status = RsvpStatus.ACCEPTED,
        locationVisibility = visibility,
    )
}

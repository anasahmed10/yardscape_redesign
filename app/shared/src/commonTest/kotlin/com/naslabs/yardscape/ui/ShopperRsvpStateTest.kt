package com.naslabs.yardscape.ui

import com.naslabs.yardscape.data.SeededYardSaleData
import com.naslabs.yardscape.scenarios.MockScenarioCatalog
import com.naslabs.yardscape.scenarios.MockScenarioId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ShopperRsvpStateTest {
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
}

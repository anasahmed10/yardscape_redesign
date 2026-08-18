package com.naslabs.yardscape.ui

import com.naslabs.yardscape.data.SeededYardSaleData
import com.naslabs.yardscape.domain.UserRole
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HostAttendanceStateTest {
    private val eventId = SeededYardSaleData.FAMILY_GARAGE_EVENT_ID

    @Test
    fun seededHostEventCoversEveryAttendeeLifecycleRow() {
        val state = assertNotNull(YardScapeAppState().hostAttendanceState(eventId))

        assertEquals(HostAttendeeUiState.entries.toSet(), state.attendees.map { it.state }.toSet())
        assertEquals(2, state.acceptedCount)
        assertEquals(1, state.requestedCount)
        assertEquals(1, state.activeLocationAccessCount)
        assertTrue(state.isAtCapacity)
    }

    @Test
    fun attendanceProjectionUsesFlatActionableRowsAndCompactSummaryMetrics() {
        val state = assertNotNull(YardScapeAppState().hostAttendanceState(eventId))

        assertEquals(
            listOf(
                HostAttendeeUiState.Requested,
                HostAttendeeUiState.Accepted,
                HostAttendeeUiState.Revoked,
                HostAttendeeUiState.Waitlisted,
                HostAttendeeUiState.Declined,
                HostAttendeeUiState.Cancelled,
                HostAttendeeUiState.Removed,
                HostAttendeeUiState.Expired,
            ),
            state.attendeeRows.map { it.state },
        )
        assertEquals("2 / 2", state.capacityMetric.value)
        assertEquals("1", state.exactAccessMetric.value)
        assertTrue(state.summaryMetrics.all { it.value.isNotBlank() })
    }

    @Test
    fun exactAccessLabelsAndMessageActionsFollowCurrentPolicy() {
        val state = assertNotNull(YardScapeAppState(activeUserRole = UserRole.HOST).hostAttendanceState(eventId))
        val accepted = state.attendeeRows.single { it.shopperId == SeededAttendeeIds.Accepted }
        val revoked = state.attendeeRows.single { it.shopperId == SeededAttendeeIds.Revoked }
        val requested = state.attendeeRows.single { it.state == HostAttendeeUiState.Requested }

        assertEquals("Exact location active", accepted.exactAccessLabel)
        assertTrue(accepted.canMessageAttendee)
        assertEquals("Exact location hidden", revoked.exactAccessLabel)
        assertFalse(revoked.canMessageAttendee)
        assertEquals("Exact location not granted", requested.exactAccessLabel)
        assertFalse(requested.canMessageAttendee)
    }

    @Test
    fun everyNonActiveAttendanceTransitionHidesExactAccessAndHostMessaging() {
        val rows = assertNotNull(
            YardScapeAppState(activeUserRole = UserRole.HOST).hostAttendanceState(eventId),
        ).attendeeRows

        rows.forEach { attendee ->
            if (attendee.state == HostAttendeeUiState.Accepted) {
                assertTrue(attendee.hasLocationAccess)
                assertTrue(attendee.canMessageAttendee)
            } else {
                assertFalse(attendee.hasLocationAccess)
                assertFalse(attendee.canMessageAttendee)
                assertFalse(attendee.exactAccessLabel == "Exact location active")
            }
        }
    }

    @Test
    fun emptyEventHasNoAttendeeRows() {
        val state = assertNotNull(
            YardScapeAppState().hostAttendanceState(SeededYardSaleData.DRAFT_EVENT_ID),
        )

        assertTrue(state.attendees.isEmpty())
        assertEquals(0, state.activeLocationAccessCount)
    }

    @Test
    fun capacityPreventsUnsafeAcceptanceUntilHostCreatesSpace() {
        val appState = YardScapeAppState()
        val shopperId = SeededYardSaleData.SHOPPER_WITHOUT_ACCESS_ID

        assertFalse(appState.requestHostAttendeeAction(eventId, shopperId, HostAttendeeAction.Accept))
        assertNull(appState.pendingHostAttendeeAction)
        assertTrue(
            appState.updateHostAttendancePolicy(
                eventId,
                HostAttendancePolicy(attendeeCap = 3, approvalMode = HostRsvpApprovalMode.ManualReview),
            ),
        )

        assertTrue(appState.requestHostAttendeeAction(eventId, shopperId, HostAttendeeAction.Accept))
        assertEquals(HostAttendeeUiState.Requested, appState.myRsvpItems().single { it.eventId == eventId }.state.toHostState())
        assertTrue(appState.confirmHostAttendeeAction())

        val accepted = appState.hostAttendanceState(eventId)?.attendees?.single { it.shopperId == shopperId }
        assertEquals(HostAttendeeUiState.Accepted, accepted?.state)
        assertTrue(accepted?.hasLocationAccess == true)
        assertNotNull(appState.myRsvpItems().single { it.eventId == eventId }.exactAddress)
    }

    @Test
    fun revealRevocationImmediatelyUpdatesHostAndShopperState() {
        val shopperId = SeededAttendeeIds.Accepted
        val appState = YardScapeAppState(shopperId = shopperId)
        assertNotNull(appState.myRsvpItems().single { it.eventId == eventId }.exactAddress)

        assertTrue(appState.requestHostAttendeeAction(eventId, shopperId, HostAttendeeAction.Revoke))
        assertNotNull(appState.myRsvpItems().single { it.eventId == eventId }.exactAddress)
        assertTrue(appState.confirmHostAttendeeAction())

        val hostItem = appState.hostAttendanceState(eventId)?.attendees?.single { it.shopperId == shopperId }
        val shopperItem = appState.myRsvpItems().single { it.eventId == eventId }
        assertEquals(HostAttendeeUiState.Revoked, hostItem?.state)
        assertFalse(hostItem?.hasLocationAccess == true)
        assertEquals(ShopperRsvpUiState.Revoked, shopperItem.state)
        assertNull(shopperItem.exactAddress)
    }

    @Test
    fun declineAndRemoveRequireConfirmationAndNeverGrantLocation() {
        val declineState = YardScapeAppState()
        val requestedId = SeededYardSaleData.SHOPPER_WITHOUT_ACCESS_ID
        assertTrue(declineState.requestHostAttendeeAction(eventId, requestedId, HostAttendeeAction.Decline))
        assertEquals(HostAttendeeUiState.Requested, declineState.hostAttendanceState(eventId)?.attendees?.single { it.shopperId == requestedId }?.state)
        assertTrue(declineState.confirmHostAttendeeAction())
        assertEquals(HostAttendeeUiState.Declined, declineState.hostAttendanceState(eventId)?.attendees?.single { it.shopperId == requestedId }?.state)

        val acceptedId = SeededAttendeeIds.Accepted
        val removeState = YardScapeAppState(shopperId = acceptedId)
        assertTrue(removeState.requestHostAttendeeAction(eventId, acceptedId, HostAttendeeAction.Remove))
        assertTrue(removeState.confirmHostAttendeeAction())
        assertEquals(HostAttendeeUiState.Removed, removeState.hostAttendanceState(eventId)?.attendees?.single { it.shopperId == acceptedId }?.state)
        assertNull(removeState.myRsvpItems().single { it.eventId == eventId }.exactAddress)
    }

    @Test
    fun confirmationsExplainLocationConsequences() {
        HostAttendeeAction.entries.forEach { action ->
            assertTrue(action.consequence.contains("location", ignoreCase = true))
            assertTrue(action.confirmationTitle.isNotBlank())
        }
    }

    @Test
    fun publicModelsNeverContainAttendeeIdentityOrCounts() {
        val appState = YardScapeAppState()
        val publicText = buildString {
            append(appState.browseItems())
            append(appState.detailStateFor(eventId))
        }

        listOf("Maya R.", "Theo K.", SeededAttendeeIds.Accepted, "awaiting review").forEach { privateValue ->
            assertFalse(publicText.contains(privateValue))
        }
    }

    @Test
    fun anotherHostsEventCannotBeManaged() {
        val appState = YardScapeAppState()

        assertNull(appState.hostAttendanceState(SeededYardSaleData.CANCELLED_EVENT_ID))
        assertFalse(appState.openHostAttendees(SeededYardSaleData.CANCELLED_EVENT_ID))
    }

    @Test
    fun hostMessageEntryResolvesOnlyAnAuthorizedOpaqueConversation() = runTest {
        val appState = YardScapeAppState(activeUserRole = UserRole.HOST)
        val shopperId = SeededAttendeeIds.Accepted

        assertTrue(appState.openHostAttendeeMessage(eventId, shopperId))

        val route = assertIs<YardScapeRoute.MessageThread>(appState.route)
        assertFalse(route.conversationId.value.contains(eventId))
        assertFalse(route.conversationId.value.contains(shopperId))

        assertTrue(appState.requestHostAttendeeAction(eventId, shopperId, HostAttendeeAction.Revoke))
        assertTrue(appState.confirmHostAttendeeAction())
        assertFalse(appState.openHostAttendeeMessage(eventId, shopperId))
    }

    @Test
    fun signedOutHostHasNoMessageActionOrProtectedEntry() = runTest {
        val appState = YardScapeAppState(activeUserRole = UserRole.HOST)
        appState.signOutMock()

        assertNull(appState.hostAttendanceState(eventId))
        assertFalse(appState.openHostAttendeeMessage(eventId, SeededAttendeeIds.Accepted))
    }
}

private fun ShopperRsvpUiState.toHostState(): HostAttendeeUiState = when (this) {
    ShopperRsvpUiState.Requested -> HostAttendeeUiState.Requested
    ShopperRsvpUiState.Accepted -> HostAttendeeUiState.Accepted
    ShopperRsvpUiState.Full,
    ShopperRsvpUiState.Waitlisted,
    -> HostAttendeeUiState.Waitlisted
    ShopperRsvpUiState.Declined -> HostAttendeeUiState.Declined
    ShopperRsvpUiState.Cancelled -> HostAttendeeUiState.Cancelled
    ShopperRsvpUiState.Revoked -> HostAttendeeUiState.Revoked
    ShopperRsvpUiState.Expired -> HostAttendeeUiState.Expired
}

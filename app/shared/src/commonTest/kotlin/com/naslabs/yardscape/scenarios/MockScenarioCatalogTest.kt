package com.naslabs.yardscape.scenarios

import com.naslabs.yardscape.data.SeededYardSaleData
import com.naslabs.yardscape.domain.UserRole
import com.naslabs.yardscape.ui.AppDataAvailability
import com.naslabs.yardscape.ui.EventAttendanceState
import com.naslabs.yardscape.ui.LocationRevealState
import com.naslabs.yardscape.ui.MockSessionStatus
import com.naslabs.yardscape.ui.BlockMutationState
import com.naslabs.yardscape.ui.ReportSubmissionState
import com.naslabs.yardscape.ui.SafetyFailureKind
import com.naslabs.yardscape.ui.YardScapeRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

class MockScenarioCatalogTest {
    @Test
    fun catalogContainsEveryRequiredScenarioWithDocumentedAssertions() {
        assertEquals(MockScenarioId.entries.toSet(), MockScenarioCatalog.scenarios.map { it.id }.toSet())
        assertTrue(MockScenarioCatalog.scenarios.all { it.name.isNotBlank() })
        assertTrue(MockScenarioCatalog.scenarios.all { it.intendedAssertions.isNotEmpty() })
    }

    @Test
    fun everyScenarioConstructsDeterministicallyWithFreshState() {
        MockScenarioId.entries.forEach { id ->
            val first = MockScenarioCatalog.createAppState(id)
            val second = MockScenarioCatalog.createAppState(id)

            assertNotSame(first, second)
            assertEquals(first.route, second.route)
            assertEquals(first.activeUserRole, second.activeUserRole)
            assertEquals(first.dataAvailability, second.dataAvailability)
            assertEquals(first.browseItems(), second.browseItems())
        }
    }

    @Test
    fun browseAndAvailabilityScenariosExposeTheirIntendedState() {
        assertEquals(
            2,
            MockScenarioCatalog.createAppState(MockScenarioId.PopulatedBrowse).browseItems().size,
        )
        assertTrue(MockScenarioCatalog.createAppState(MockScenarioId.NoNearbyEvents).browseItems().isEmpty())
        assertIs<AppDataAvailability.Offline>(
            MockScenarioCatalog.createAppState(MockScenarioId.Offline).dataAvailability,
        )
        assertIs<AppDataAvailability.RecoverableError>(
            MockScenarioCatalog.createAppState(MockScenarioId.RecoverableError).dataAvailability,
        )
    }

    @Test
    fun rsvpScenariosCoverRevealStatesWithoutUnauthorizedAddressOutput() {
        val pending = detailState(MockScenarioId.PendingRsvp)
        val accepted = detailState(MockScenarioId.AcceptedAccess)
        val revoked = detailState(MockScenarioId.RevokedAccess)
        val expired = detailState(MockScenarioId.ExpiredAccess)
        val cancelled = detailState(MockScenarioId.CancelledEvent)

        assertIs<LocationRevealState.Pending>(pending.revealState)
        assertIs<LocationRevealState.Revealed>(accepted.revealState)
        assertIs<LocationRevealState.Revoked>(revoked.revealState)
        assertIs<LocationRevealState.Expired>(expired.revealState)
        assertIs<LocationRevealState.Cancelled>(cancelled.revealState)

        val protectedAddress = "123 Cedar Street"
        assertFalse(pending.toString().contains(protectedAddress))
        assertFalse(revoked.toString().contains(protectedAddress))
        assertFalse(expired.toString().contains(protectedAddress))
        assertFalse(cancelled.toString().contains("418 Juniper Avenue"))
    }

    @Test
    fun capacityAndHostScenariosInjectPolicyAndRole() {
        val capacity = detailState(MockScenarioId.EventAtCapacity)
        val hostDrafts = MockScenarioCatalog.createAppState(MockScenarioId.HostWithDrafts)
        val hostAttendees = MockScenarioCatalog.createAppState(MockScenarioId.HostWithPendingAttendees)

        assertEquals(EventAttendanceState.AtCapacity, capacity.attendanceState)
        assertFalse(capacity.shouldShowRsvpAction)
        assertEquals(UserRole.HOST, hostDrafts.activeUserRole)
        assertEquals(YardScapeRoute.HostCreateEdit(SeededYardSaleData.DRAFT_EVENT_ID), hostDrafts.route)
        assertEquals(UserRole.HOST, hostAttendees.activeUserRole)
        assertEquals(1, hostAttendees.pendingAttendeeCount(SeededYardSaleData.FAMILY_GARAGE_EVENT_ID))
    }

    @Test
    fun shopperCannotReadHostPendingAttendeeCount() {
        val shopper = MockScenarioCatalog.createAppState(MockScenarioId.PendingRsvp)

        assertEquals(0, shopper.pendingAttendeeCount(SeededYardSaleData.FAMILY_GARAGE_EVENT_ID))
    }

    @Test
    fun accountScenariosCoverSignedOutExpiredAndRoleProfiles() {
        val signedOut = MockScenarioCatalog.createAppState(MockScenarioId.SignedOutAccount)
        val expired = MockScenarioCatalog.createAppState(MockScenarioId.SessionExpiredAccount)
        val shopper = MockScenarioCatalog.createAppState(MockScenarioId.ShopperProfile)
        val host = MockScenarioCatalog.createAppState(MockScenarioId.HostProfile)

        assertEquals(MockSessionStatus.SignedOut, signedOut.accountState.sessionStatus)
        assertTrue(signedOut.browseItems().isNotEmpty())
        assertEquals(MockSessionStatus.Expired, expired.accountState.sessionStatus)
        assertFalse(expired.accountState.isSignedIn)
        assertEquals(UserRole.SHOPPER, shopper.accountState.activeProfile?.role)
        assertEquals(UserRole.HOST, host.accountState.activeProfile?.role)
    }

    @Test
    fun safetyScenariosExposeDeterministicFailureAndMutationStates() {
        val validation = MockScenarioCatalog.createAppState(MockScenarioId.ReportValidation)
        validation.submitSafetyReport()
        assertEquals(
            SafetyFailureKind.Validation,
            assertIs<ReportSubmissionState.Failed>(validation.shopperSafetyState?.reportState).kind,
        )

        val offlineReport = MockScenarioCatalog.createAppState(MockScenarioId.ReportOffline)
        offlineReport.updateSafetyReportReason(com.naslabs.yardscape.domain.ReportReason.Other)
        offlineReport.submitSafetyReport()
        assertEquals(
            SafetyFailureKind.Offline,
            assertIs<ReportSubmissionState.Failed>(offlineReport.shopperSafetyState?.reportState).kind,
        )

        val blocked = MockScenarioCatalog.createAppState(MockScenarioId.BlockHost)
        blocked.requestBlockMutation()
        blocked.confirmBlockMutation()
        assertIs<BlockMutationState.Completed>(blocked.shopperSafetyState?.blockState)
        assertTrue(SeededYardSaleData.ESTATE_TOOLS_EVENT_ID in blocked.blockedEventIds)

        val failedBlock = MockScenarioCatalog.createAppState(MockScenarioId.BlockOffline)
        failedBlock.requestBlockMutation()
        failedBlock.confirmBlockMutation()
        assertEquals(
            SafetyFailureKind.Offline,
            assertIs<BlockMutationState.Failed>(failedBlock.shopperSafetyState?.blockState).kind,
        )
        assertTrue(failedBlock.blockedEventIds.isEmpty())
    }

    private fun detailState(id: MockScenarioId) =
        requireNotNull(MockScenarioCatalog.createAppState(id).selectedEventDetailState())
}

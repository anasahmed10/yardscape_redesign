package com.naslabs.yardscape.ui

import com.naslabs.yardscape.data.SeededSafetyBehavior
import com.naslabs.yardscape.data.SeededSafetyOutcome
import com.naslabs.yardscape.data.SeededShopperSafetyRepository
import com.naslabs.yardscape.data.SeededYardSaleData
import com.naslabs.yardscape.domain.ReportReason
import com.naslabs.yardscape.domain.UserRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ShopperSafetyFlowTest {
    private val familyEventId = SeededYardSaleData.FAMILY_GARAGE_EVENT_ID
    private val acceptedEventId = SeededYardSaleData.ESTATE_TOOLS_EVENT_ID

    @Test
    fun signedOutSafetyActionsResumeTheExactEventAndOriginAfterSignIn() {
        val reportState = YardScapeAppState(initialAccountStatus = MockSessionStatus.SignedOut)
        reportState.navigateTo(YardScapePrimaryDestination.MyFinds)
        reportState.openEvent(familyEventId)

        reportState.openReport(familyEventId)

        assertEquals(YardScapeRoute.Account, reportState.route)
        assertEquals(ProtectedAction.Report, reportState.pendingProtectedAction?.action)
        reportState.signInMock(UserRole.SHOPPER)
        assertEquals(
            YardScapeRoute.EventSafety(
                familyEventId,
                ShopperSafetyAction.Report,
                YardScapePrimaryDestination.MyFinds,
            ),
            reportState.route,
        )

        val blockState = YardScapeAppState(initialAccountStatus = MockSessionStatus.SignedOut)
        assertTrue(blockState.navigateToPath("/events/$familyEventId/safety/block"))
        assertEquals(YardScapeRoute.Account, blockState.route)
        assertEquals(ProtectedAction.Block, blockState.pendingProtectedAction?.action)
    }

    @Test
    fun reportsDistinguishValidationSuccessOfflineAndServerFailure() {
        val validation = YardScapeAppState()
        validation.openReport(familyEventId)
        validation.submitSafetyReport()
        assertEquals(
            SafetyFailureKind.Validation,
            assertIs<ReportSubmissionState.Failed>(validation.shopperSafetyState?.reportState).kind,
        )

        val success = YardScapeAppState()
        success.openReport(familyEventId)
        success.updateSafetyReportReason(ReportReason.MisleadingListing)
        success.updateSafetyReportDetails("Public description did not match the sale.")
        success.submitSafetyReport()
        assertIs<ReportSubmissionState.Submitted>(success.shopperSafetyState?.reportState)
        assertTrue(success.browseItems().any { it.id == familyEventId })

        val offline = safetyState(reportOutcome = SeededSafetyOutcome.Offline)
        offline.openReport(familyEventId)
        offline.updateSafetyReportReason(ReportReason.ScamOrPaymentConcern)
        offline.submitSafetyReport()
        assertEquals(
            SafetyFailureKind.Offline,
            assertIs<ReportSubmissionState.Failed>(offline.shopperSafetyState?.reportState).kind,
        )

        val server = safetyState(reportOutcome = SeededSafetyOutcome.ServerError)
        server.openReport(familyEventId)
        server.updateSafetyReportReason(ReportReason.Other)
        server.submitSafetyReport()
        assertEquals(
            SafetyFailureKind.Server,
            assertIs<ReportSubmissionState.Failed>(server.shopperSafetyState?.reportState).kind,
        )
    }

    @Test
    fun successfulBlockImmediatelyDeniesEveryAffectedLocationAndUnblockDoesNotRestoreAccess() {
        val state = acceptedAccessState()
        assertNotNull(state.requestDirections(acceptedEventId))

        state.openBlock(acceptedEventId)
        state.requestBlockMutation()
        state.confirmBlockMutation()

        assertTrue(state.shopperSafetyState?.isBlocked == true)
        assertNull(state.directionsEventId)
        assertTrue(acceptedEventId in state.blockedEventIds)
        assertTrue(SeededYardSaleData.CANCELLED_EVENT_ID in state.blockedEventIds)
        assertFalse(state.browseItems().any { it.id == acceptedEventId })
        assertIs<LocationRevealState.Blocked>(state.detailStateFor(acceptedEventId)?.revealState)
        assertNull(state.myRsvpItems().single { it.eventId == acceptedEventId }.exactAddress)
        assertFalse(state.detailStateFor(acceptedEventId).toString().contains("418 Juniper Avenue"))

        state.confirmRsvp(acceptedEventId)
        assertIs<LocationRevealState.Blocked>(state.detailStateFor(acceptedEventId)?.revealState)

        state.requestBlockMutation()
        state.confirmBlockMutation()

        assertFalse(state.shopperSafetyState?.isBlocked == true)
        assertTrue(state.browseItems().any { it.id == acceptedEventId })
        assertIs<LocationRevealState.Revoked>(state.detailStateFor(acceptedEventId)?.revealState)
        assertNull(state.myRsvpItems().single { it.eventId == acceptedEventId }.exactAddress)
    }

    @Test
    fun failedBlockDoesNotClearOrClaimSuccess() {
        val state = acceptedAccessState(blockOutcome = SeededSafetyOutcome.Offline)
        val address = assertNotNull(state.requestDirections(acceptedEventId))

        state.openBlock(acceptedEventId)
        state.requestBlockMutation()
        state.confirmBlockMutation()

        val failure = assertIs<BlockMutationState.Failed>(state.shopperSafetyState?.blockState)
        assertEquals(SafetyFailureKind.Offline, failure.kind)
        assertTrue(state.blockedEventIds.isEmpty())
        assertEquals(address, state.requestDirections(acceptedEventId))
        assertIs<LocationRevealState.Revealed>(state.detailStateFor(acceptedEventId)?.revealState)
    }

    @Test
    fun signOutClearsSafetyDraftAndConfirmation() {
        val state = YardScapeAppState()
        state.openReport(familyEventId)
        state.updateSafetyReportReason(ReportReason.HarassmentOrPressure)
        state.updateSafetyReportDetails("Draft with potentially sensitive user text")

        state.signOutMock()

        assertNull(state.shopperSafetyState)
        assertNull(state.pendingProtectedAction)
        assertEquals(YardScapeRoute.Account, state.route)
    }

    private fun safetyState(reportOutcome: SeededSafetyOutcome): YardScapeAppState = YardScapeAppState(
        shopperSafetyRepository = SeededShopperSafetyRepository(
            behavior = SeededSafetyBehavior(reportOutcome = reportOutcome),
        ),
    )

    private fun acceptedAccessState(
        blockOutcome: SeededSafetyOutcome = SeededSafetyOutcome.Success,
    ): YardScapeAppState = YardScapeAppState(
        shopperId = SeededYardSaleData.SHOPPER_WITH_ACCEPTED_ACCESS_ID,
        shopperSafetyRepository = SeededShopperSafetyRepository(
            behavior = SeededSafetyBehavior(blockOutcome = blockOutcome),
        ),
    )
}

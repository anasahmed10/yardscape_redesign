package com.naslabs.yardscape.data

import com.naslabs.yardscape.domain.REPORT_DETAILS_MAX_LENGTH
import com.naslabs.yardscape.domain.ReportReason
import com.naslabs.yardscape.domain.SafetyReportDraft
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SeededShopperSafetyRepositoryTest {
    @Test
    fun successfulReportIsNormalizedAndRecordedOnce() {
        val repository = SeededShopperSafetyRepository()

        val result = repository.submitReport(
            SafetyReportDraft(
                eventId = "  ${SeededYardSaleData.FAMILY_GARAGE_EVENT_ID}  ",
                reason = ReportReason.MisleadingListing,
                details = "  The advertised hours do not match.  ",
            ),
        )

        assertEquals(
            ReportReceipt("mock-report-1"),
            assertIs<SafetyRepositoryResult.Success<ReportReceipt>>(result).value,
        )
        assertEquals(1, repository.submittedReports.size)
        assertEquals(
            SeededYardSaleData.FAMILY_GARAGE_EVENT_ID,
            repository.submittedReports.single().draft.eventId,
        )
        assertEquals("The advertised hours do not match.", repository.submittedReports.single().draft.details)
    }

    @Test
    fun validationOfflineAndServerFailuresNeverRecordAReport() {
        val invalidRepository = SeededShopperSafetyRepository()
        val invalid = invalidRepository.submitReport(SafetyReportDraft(eventId = "", reason = null))
        assertIs<SafetyRepositoryResult.ValidationFailure>(invalid)
        assertIs<SafetyRepositoryResult.ValidationFailure>(
            invalidRepository.submitReport(
                validDraft().copy(details = " ".repeat(REPORT_DETAILS_MAX_LENGTH + 1)),
            ),
        )
        assertTrue(invalidRepository.submittedReports.isEmpty())

        listOf(SeededSafetyOutcome.Offline, SeededSafetyOutcome.ServerError).forEach { outcome ->
            val repository = SeededShopperSafetyRepository(
                behavior = SeededSafetyBehavior(reportOutcome = outcome),
            )
            val result = repository.submitReport(validDraft())

            when (outcome) {
                SeededSafetyOutcome.Offline -> assertIs<SafetyRepositoryResult.Offline>(result)
                SeededSafetyOutcome.ServerError -> assertIs<SafetyRepositoryResult.ServerError>(result)
                SeededSafetyOutcome.Success -> error("Success is not a failure scenario")
            }
            assertTrue(repository.submittedReports.isEmpty())
        }
    }

    @Test
    fun blockAndUnblockReturnEveryEventForTheTargetHost() {
        val repository = SeededShopperSafetyRepository()
        val targetEventId = SeededYardSaleData.FAMILY_GARAGE_EVENT_ID
        val expectedEventIds = SeededYardSaleData.events
            .filter { it.host.id == SeededYardSaleData.HOST_AVERY_ID }
            .mapTo(mutableSetOf()) { it.id }

        val blocked = assertIs<SafetyRepositoryResult.Success<BlockedHostUpdate>>(
            repository.blockHostForEvent(targetEventId),
        ).value

        assertEquals(SeededYardSaleData.HOST_AVERY_ID, blocked.hostId)
        assertEquals(expectedEventIds, blocked.affectedEventIds)
        assertTrue(blocked.isBlocked)
        assertEquals(setOf(SeededYardSaleData.HOST_AVERY_ID), repository.blockedHosts)

        val unblocked = assertIs<SafetyRepositoryResult.Success<BlockedHostUpdate>>(
            repository.unblockHostForEvent(targetEventId),
        ).value
        assertEquals(expectedEventIds, unblocked.affectedEventIds)
        assertTrue(!unblocked.isBlocked)
        assertTrue(repository.blockedHosts.isEmpty())
    }

    @Test
    fun blockFailuresAndUnknownEventsDoNotMutateBlockedState() {
        SeededSafetyOutcome.entries.filterNot { it == SeededSafetyOutcome.Success }.forEach { outcome ->
            val repository = SeededShopperSafetyRepository(
                behavior = SeededSafetyBehavior(blockOutcome = outcome),
            )

            val result = repository.blockHostForEvent(SeededYardSaleData.FAMILY_GARAGE_EVENT_ID)

            assertTrue(result !is SafetyRepositoryResult.Success)
            assertTrue(repository.blockedHosts.isEmpty())
        }

        val repository = SeededShopperSafetyRepository()
        assertIs<SafetyRepositoryResult.ValidationFailure>(repository.blockHostForEvent("missing-event"))
        assertTrue(repository.blockedHosts.isEmpty())
    }

    private fun validDraft(): SafetyReportDraft = SafetyReportDraft(
        eventId = SeededYardSaleData.FAMILY_GARAGE_EVENT_ID,
        reason = ReportReason.UnsafeOrProhibited,
        details = "Please review the public sale details.",
    )
}

package com.naslabs.yardscape.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShopperSafetyPolicyTest {
    @Test
    fun reportRequiresAnEventAndReason() {
        val issues = ShopperSafetyPolicy.validateReport(
            SafetyReportDraft(eventId = " ", reason = null),
        )

        assertEquals(
            listOf(
                SafetyReportValidationIssue.EventRequired,
                SafetyReportValidationIssue.ReasonRequired,
            ),
            issues,
        )
    }

    @Test
    fun optionalDetailsAcceptTheBoundaryButRejectLongerInput() {
        val atLimit = SafetyReportDraft(
            eventId = "event-1",
            reason = ReportReason.UnsafeOrProhibited,
            details = "a".repeat(REPORT_DETAILS_MAX_LENGTH),
        )
        val overLimit = atLimit.copy(details = "a".repeat(REPORT_DETAILS_MAX_LENGTH + 1))

        assertTrue(ShopperSafetyPolicy.validateReport(atLimit).isEmpty())
        assertEquals(
            listOf(SafetyReportValidationIssue.DetailsTooLong),
            ShopperSafetyPolicy.validateReport(overLimit),
        )
    }

    @Test
    fun reportNormalizationTrimsIdentifiersAndOptionalDetails() {
        val normalized = ShopperSafetyPolicy.normalizedReport(
            SafetyReportDraft(
                eventId = "  event-1  ",
                reason = ReportReason.Other,
                details = "  Please review this sale.  ",
            ),
        )

        assertEquals("event-1", normalized.eventId)
        assertEquals("Please review this sale.", normalized.details)
    }
}

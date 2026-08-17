package com.naslabs.yardscape.domain

const val REPORT_DETAILS_MAX_LENGTH: Int = 500

enum class ReportReason(val displayLabel: String) {
    UnsafeOrProhibited("Unsafe or prohibited activity"),
    MisleadingListing("Misleading sale information"),
    HarassmentOrPressure("Harassment or pressure"),
    ScamOrPaymentConcern("Scam or payment concern"),
    Other("Something else"),
}

data class SafetyReportDraft(
    val eventId: String,
    val reason: ReportReason?,
    val details: String = "",
)

enum class SafetyReportValidationIssue(val message: String) {
    EventRequired("A sale is required before a report can be submitted."),
    ReasonRequired("Choose a reason for the report."),
    DetailsTooLong("Report details must be $REPORT_DETAILS_MAX_LENGTH characters or fewer."),
}

object ShopperSafetyPolicy {
    fun validateReport(draft: SafetyReportDraft): List<SafetyReportValidationIssue> = buildList {
        if (draft.eventId.isBlank()) add(SafetyReportValidationIssue.EventRequired)
        if (draft.reason == null) add(SafetyReportValidationIssue.ReasonRequired)
        if (draft.details.length > REPORT_DETAILS_MAX_LENGTH) {
            add(SafetyReportValidationIssue.DetailsTooLong)
        }
    }

    fun normalizedReport(draft: SafetyReportDraft): SafetyReportDraft =
        draft.copy(
            eventId = draft.eventId.trim(),
            details = draft.details.trim(),
        )
}

package com.naslabs.yardscape.ui

import com.naslabs.yardscape.data.HostEventDraft
import com.naslabs.yardscape.data.validateFor
import com.naslabs.yardscape.domain.EventStatus

enum class HostEditorStep(val label: String) {
    Basics("Basics"),
    Schedule("Schedule"),
    Location("Location"),
    SaleDetails("Sale details"),
    Photos("Photos"),
    RsvpSettings("RSVP settings"),
    Preview("Preview"),
}

enum class HostRsvpApprovalMode(val label: String) {
    AutoAccept("Auto-accept RSVPs"),
    ManualReview("Review each RSVP"),
}

enum class HostConfirmationAction(val title: String, val message: String, val confirmLabel: String) {
    Publish(
        "Publish this sale?",
        "Review the public preview before making this event visible to shoppers.",
        "Publish sale",
    ),
    Hide(
        "Hide this sale?",
        "The event will leave public search and all protected location access will be revoked.",
        "Hide sale",
    ),
    Cancel(
        "Cancel this sale?",
        "Cancellation is visible to attendees and immediately revokes protected location access.",
        "Cancel sale",
    ),
}

data class HostPublicPreview(
    val title: String,
    val description: String,
    val scheduleLabel: String,
    val approximateLocationLabel: String,
    val categories: List<String>,
    val photoCaptions: List<String>,
    val photoReferences: List<String>,
    val acceptedPaymentTypes: List<String>,
    val accessibilityNotes: List<String>,
    val hostContext: String,
)

internal fun shopperDetailSections(
    scheduleLabel: String,
    approximateLocationLabel: String,
    categories: List<String>,
    acceptedPaymentTypes: List<String>,
    accessibilityNotes: List<String>,
    hostContext: String,
): List<Pair<String, String>> = listOf(
    "When" to scheduleLabel,
    "Area" to approximateLocationLabel,
    "Categories" to categories.joinToString(", "),
    "Payments" to acceptedPaymentTypes.joinToString(", "),
    "Accessibility" to accessibilityNotes.joinToString(", "),
    "Host" to hostContext,
)

internal fun HostPublicPreview.shopperDetailSections(): List<Pair<String, String>> =
    shopperDetailSections(
        scheduleLabel = scheduleLabel,
        approximateLocationLabel = approximateLocationLabel,
        categories = categories,
        acceptedPaymentTypes = acceptedPaymentTypes,
        accessibilityNotes = accessibilityNotes,
        hostContext = hostContext,
    )

data class HostEditorProgress(
    val activeStep: HostEditorStep,
    val currentStep: Int,
    val totalSteps: Int,
) {
    val previousStep: HostEditorStep?
        get() = HostEditorStep.entries.getOrNull(currentStep - 2)
}

data class HostEditorState(
    val draft: HostEventDraft,
    val validationErrors: List<String>,
    val savedEventId: String? = draft.id,
    val step: HostEditorStep = HostEditorStep.Basics,
    val attendeeCapInput: String = "",
    val approvalMode: HostRsvpApprovalMode = HostRsvpApprovalMode.AutoAccept,
    val hostDisplayName: String = "YardScape host",
    val hostTrustSignals: List<String> = emptyList(),
    val pendingConfirmation: HostConfirmationAction? = null,
) {
    val progress: HostEditorProgress
        get() = HostEditorProgress(
            activeStep = step,
            currentStep = step.ordinal + 1,
            totalSteps = HostEditorStep.entries.size,
        )

    val attendeeCap: Int?
        get() = attendeeCapInput.toIntOrNull()

    fun errorsFor(targetStep: HostEditorStep = step): List<String> = when (targetStep) {
        HostEditorStep.Basics -> buildList {
            if (draft.title.isBlank()) add("Add a title to continue.")
            if (draft.description.isBlank()) add("Add a description to continue.")
        }
        HostEditorStep.Schedule -> buildList {
            if (draft.startsAtEpochMillis == null) add("Choose a start time to continue.")
            if (draft.endsAtEpochMillis == null) add("Choose an end time to continue.")
            if (draft.startsAtEpochMillis != null && draft.endsAtEpochMillis != null &&
                draft.startsAtEpochMillis >= draft.endsAtEpochMillis
            ) add("End time must be after start time.")
        }
        HostEditorStep.Location -> buildList {
            if (draft.publicNeighborhood.isBlank()) add("Select a map location to continue.")
            if (draft.exactStreetAddress.isBlank()) add("A protected street address is required.")
        }
        HostEditorStep.SaleDetails -> buildList {
            if (draft.categories.isEmpty()) add("Add at least one category to continue.")
        }
        HostEditorStep.RsvpSettings -> buildList {
            if (attendeeCapInput.isNotBlank() && attendeeCap == null) add("Use a whole number for attendee cap.")
            attendeeCap?.let { if (it < 1) add("Attendee cap must be at least 1.") }
        }
        HostEditorStep.Photos,
        HostEditorStep.Preview,
        -> emptyList()
    }

    fun publicPreview(): HostPublicPreview = HostPublicPreview(
        title = draft.title.ifBlank { "Untitled yard sale" },
        description = draft.description,
        scheduleLabel = listOfNotNull(
            draft.startsAtEpochMillis?.toHostClockTimeLabel(),
            draft.endsAtEpochMillis?.toHostClockTimeLabel(),
        ).joinToString(" - "),
        approximateLocationLabel = listOf(
            draft.publicNeighborhood,
            draft.publicCity,
            draft.publicAreaDescription,
        ).filter { it.isNotBlank() }.joinToString(" - "),
        categories = draft.categories,
        photoCaptions = draft.photos.mapNotNull { it.description },
        photoReferences = draft.photos.map { it.url },
        acceptedPaymentTypes = draft.acceptedPaymentTypes,
        accessibilityNotes = draft.accessibilityNotes,
        hostContext = listOf(hostDisplayName, hostTrustSignals.firstOrNull()).filterNotNull().joinToString(" - "),
    )

    fun publishErrors(): List<String> =
        draft.validateFor(EventStatus.PUBLISHED) + errorsFor(HostEditorStep.RsvpSettings)

    fun requestConfirmation(action: HostConfirmationAction): HostEditorState =
        copy(pendingConfirmation = action)

    fun dismissConfirmation(): HostEditorState =
        copy(pendingConfirmation = null)
}

package com.naslabs.yardscape.ui

import com.naslabs.yardscape.domain.ExactAddress

enum class ShopperRsvpUiState(
    val label: String,
    val nextAction: String,
) {
    Requested("Requested", "Wait for host approval"),
    Accepted("Accepted", "View location and directions"),
    Full("Full", "Try another sale"),
    Waitlisted("Waitlisted", "Watch for an opening"),
    Declined("Declined", "Browse other sales"),
    Cancelled("Cancelled", "Browse other sales"),
    Revoked("Access revoked", "Choose another sale or contact the host"),
    Expired("Access expired", "Location is no longer available"),
}

enum class RsvpGroup(val label: String) {
    ActionNeeded("Needs attention"),
    Upcoming("Upcoming"),
    History("Past and closed"),
}

internal enum class ShopperRsvpAction(
    val label: String,
) {
    OpenEvent("Open event"),
    Directions("Directions"),
    AddReminder("Add reminder"),
    ExportCalendar("Export calendar"),
    CancelRsvp("Cancel RSVP"),
}

data class ShopperRsvpItem(
    val eventId: String,
    val title: String,
    val dateLabel: String,
    val approximateLocationLabel: String,
    val state: ShopperRsvpUiState,
    val group: RsvpGroup,
    val exactAddress: ExactAddress?,
    val reminderAdded: Boolean,
    val calendarExportPrepared: Boolean,
) {
    val canCancel: Boolean
        get() = state in setOf(
            ShopperRsvpUiState.Requested,
            ShopperRsvpUiState.Accepted,
            ShopperRsvpUiState.Waitlisted,
        )

    val canOpenDirections: Boolean
        get() = state == ShopperRsvpUiState.Accepted && exactAddress != null

    val canAddReminder: Boolean
        get() = state in setOf(
            ShopperRsvpUiState.Requested,
            ShopperRsvpUiState.Accepted,
            ShopperRsvpUiState.Waitlisted,
        )

    val canExportCalendar: Boolean
        get() = canAddReminder

    internal val visibleActions: List<ShopperRsvpAction>
        get() = buildList {
            add(ShopperRsvpAction.OpenEvent)
            if (canOpenDirections) add(ShopperRsvpAction.Directions)
            if (canAddReminder) add(ShopperRsvpAction.AddReminder)
            if (canExportCalendar) add(ShopperRsvpAction.ExportCalendar)
            if (canCancel) add(ShopperRsvpAction.CancelRsvp)
        }
}

package com.naslabs.yardscape.ui

import com.naslabs.yardscape.domain.EventStatus
import com.naslabs.yardscape.domain.LocationVisibility
import com.naslabs.yardscape.domain.Rsvp
import com.naslabs.yardscape.domain.RsvpStatus

enum class HostAttendeeUiState(val label: String, val guidance: String) {
    Requested("Requested", "Accept, decline, or leave pending"),
    Accepted("Accepted", "Has protected location access"),
    Waitlisted("Waitlisted", "Accept when space is available"),
    Declined("Declined", "Does not have location access"),
    Cancelled("Cancelled", "Shopper cancelled attendance"),
    Removed("Removed", "Removed by host; location access revoked"),
    Revoked("Access revoked", "Attendance remains, exact location is hidden"),
    Expired("Expired", "Event access has ended"),
}

enum class HostAttendeeAction(
    val label: String,
    val confirmationTitle: String,
    val consequence: String,
) {
    Accept("Accept", "Accept this RSVP?", "The shopper will immediately receive protected exact-location access."),
    Decline("Decline", "Decline this RSVP?", "The shopper will not receive exact-location access."),
    Remove("Remove", "Remove this attendee?", "Attendance and protected exact-location access will be removed immediately."),
    Revoke("Revoke location", "Revoke exact-location access?", "The attendee remains accepted, but the exact location disappears immediately."),
}

data class PendingHostAttendeeAction(
    val eventId: String,
    val shopperId: String,
    val attendeeName: String,
    val action: HostAttendeeAction,
)

data class HostAttendancePolicy(
    val attendeeCap: Int? = null,
    val approvalMode: HostRsvpApprovalMode = HostRsvpApprovalMode.AutoAccept,
)

data class HostAttendeeItem(
    val shopperId: String,
    val displayName: String,
    val state: HostAttendeeUiState,
    val hasLocationAccess: Boolean,
) {
    val availableActions: Set<HostAttendeeAction> = when (state) {
        HostAttendeeUiState.Requested,
        HostAttendeeUiState.Waitlisted,
        -> setOf(HostAttendeeAction.Accept, HostAttendeeAction.Decline)
        HostAttendeeUiState.Accepted -> setOf(HostAttendeeAction.Revoke, HostAttendeeAction.Remove)
        HostAttendeeUiState.Revoked -> setOf(HostAttendeeAction.Remove)
        else -> emptySet()
    }
}

data class HostAttendanceState(
    val eventId: String,
    val eventTitle: String,
    val policy: HostAttendancePolicy,
    val attendees: List<HostAttendeeItem>,
) {
    val acceptedCount: Int = attendees.count {
        it.state == HostAttendeeUiState.Accepted || it.state == HostAttendeeUiState.Revoked
    }
    val requestedCount: Int = attendees.count { it.state == HostAttendeeUiState.Requested }
    val activeLocationAccessCount: Int = attendees.count { it.hasLocationAccess }
    val isAtCapacity: Boolean = policy.attendeeCap?.let { acceptedCount >= it } ?: false
}

fun Rsvp.toHostAttendeeUiState(eventStatus: EventStatus, eventHasEnded: Boolean): HostAttendeeUiState = when {
    status == RsvpStatus.REMOVED -> HostAttendeeUiState.Removed
    status == RsvpStatus.CANCELLED -> HostAttendeeUiState.Cancelled
    eventStatus == EventStatus.CANCELLED -> HostAttendeeUiState.Cancelled
    locationVisibility == LocationVisibility.REVOKED -> HostAttendeeUiState.Revoked
    locationVisibility == LocationVisibility.EXPIRED || eventHasEnded || eventStatus == EventStatus.COMPLETED ->
        HostAttendeeUiState.Expired
    status == RsvpStatus.REQUESTED -> HostAttendeeUiState.Requested
    status == RsvpStatus.ACCEPTED && locationVisibility == LocationVisibility.RSVP_ACCEPTED ->
        HostAttendeeUiState.Accepted
    status == RsvpStatus.ACCEPTED -> HostAttendeeUiState.Revoked
    status == RsvpStatus.WAITLISTED || status == RsvpStatus.FULL -> HostAttendeeUiState.Waitlisted
    status == RsvpStatus.DECLINED -> HostAttendeeUiState.Declined
    else -> HostAttendeeUiState.Cancelled
}

fun String.toMockAttendeeDisplayName(): String = when (this) {
    SeededAttendeeIds.Accepted -> "Maya R."
    SeededAttendeeIds.Waitlisted -> "Theo K."
    SeededAttendeeIds.Declined -> "Priya S."
    SeededAttendeeIds.Cancelled -> "Jon B."
    SeededAttendeeIds.Removed -> "Nia L."
    SeededAttendeeIds.Revoked -> "Owen C."
    SeededAttendeeIds.Expired -> "Lena D."
    else -> "Shopper ${takeLast(4).uppercase()}"
}

object SeededAttendeeIds {
    const val Accepted = "shopper-host-accepted"
    const val Waitlisted = "shopper-host-waitlisted"
    const val Declined = "shopper-host-declined"
    const val Cancelled = "shopper-host-cancelled"
    const val Removed = "shopper-host-removed"
    const val Revoked = "shopper-host-revoked"
    const val Expired = "shopper-host-expired"
}

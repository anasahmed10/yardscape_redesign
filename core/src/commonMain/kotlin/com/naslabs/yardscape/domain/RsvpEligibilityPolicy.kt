package com.naslabs.yardscape.domain

enum class RsvpEligibilityStatus {
    ELIGIBLE,
    BLOCKED,
    ACCESS_REVOKED,
    ACCESS_EXPIRED,
    EVENT_CANCELLED,
    EVENT_COMPLETED,
    EVENT_UNAVAILABLE,
    EVENT_ENDED,
    AT_CAPACITY,
    WAITLISTED,
    DECLINED,
    ALREADY_ACCEPTED,
}

object RsvpEligibilityPolicy {
    fun statusFor(
        eventStatus: EventStatus,
        saleWindow: SaleWindow,
        currentRsvpStatus: RsvpStatus?,
        currentLocationVisibility: LocationVisibility?,
        nowEpochMillis: Long,
        isBlocked: Boolean,
        isAtCapacity: Boolean,
    ): RsvpEligibilityStatus = when {
        isBlocked -> RsvpEligibilityStatus.BLOCKED
        currentLocationVisibility == LocationVisibility.REVOKED -> RsvpEligibilityStatus.ACCESS_REVOKED
        currentLocationVisibility == LocationVisibility.EXPIRED -> RsvpEligibilityStatus.ACCESS_EXPIRED
        eventStatus == EventStatus.CANCELLED -> RsvpEligibilityStatus.EVENT_CANCELLED
        eventStatus == EventStatus.COMPLETED -> RsvpEligibilityStatus.EVENT_COMPLETED
        eventStatus != EventStatus.PUBLISHED -> RsvpEligibilityStatus.EVENT_UNAVAILABLE
        saleWindow.hasEnded(nowEpochMillis) -> RsvpEligibilityStatus.EVENT_ENDED
        currentRsvpStatus == RsvpStatus.ACCEPTED ||
            currentLocationVisibility == LocationVisibility.RSVP_ACCEPTED -> RsvpEligibilityStatus.ALREADY_ACCEPTED
        currentRsvpStatus == RsvpStatus.WAITLISTED -> RsvpEligibilityStatus.WAITLISTED
        currentRsvpStatus == RsvpStatus.DECLINED -> RsvpEligibilityStatus.DECLINED
        currentRsvpStatus == RsvpStatus.FULL -> RsvpEligibilityStatus.AT_CAPACITY
        currentRsvpStatus == RsvpStatus.REMOVED -> RsvpEligibilityStatus.ACCESS_REVOKED
        isAtCapacity -> RsvpEligibilityStatus.AT_CAPACITY
        else -> RsvpEligibilityStatus.ELIGIBLE
    }
}

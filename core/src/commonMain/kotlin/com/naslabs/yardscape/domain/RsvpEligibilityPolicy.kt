package com.naslabs.yardscape.domain

object RsvpEligibilityPolicy {
    fun canSubmit(
        eventStatus: EventStatus,
        saleWindow: SaleWindow,
        currentLocationVisibility: LocationVisibility?,
        nowEpochMillis: Long,
        isBlocked: Boolean,
        isAtCapacity: Boolean,
    ): Boolean =
        !isBlocked &&
            !isAtCapacity &&
            eventStatus == EventStatus.PUBLISHED &&
            !saleWindow.hasEnded(nowEpochMillis) &&
            currentLocationVisibility != LocationVisibility.REVOKED &&
            currentLocationVisibility != LocationVisibility.EXPIRED
}

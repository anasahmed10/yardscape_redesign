package com.naslabs.yardscape.domain

object LocationRevealPolicy {
    fun canRevealExactLocation(
        event: YardSaleEvent,
        rsvp: Rsvp?,
        nowEpochMillis: Long,
    ): Boolean =
        event.status == EventStatus.PUBLISHED &&
            !event.saleWindow.hasEnded(nowEpochMillis) &&
            rsvp != null &&
            rsvp.eventId == event.id &&
            rsvp.status == RsvpStatus.ACCEPTED &&
            rsvp.locationVisibility == LocationVisibility.RSVP_ACCEPTED

    fun exactLocationFor(
        event: YardSaleEvent,
        rsvp: Rsvp?,
        nowEpochMillis: Long,
    ): ExactAddress? =
        if (canRevealExactLocation(event, rsvp, nowEpochMillis)) {
            event.location.exactAddress
        } else {
            null
        }
}

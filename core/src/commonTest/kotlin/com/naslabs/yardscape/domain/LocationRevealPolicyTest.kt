package com.naslabs.yardscape.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocationRevealPolicyTest {
    @Test
    fun publicPreviewUsesApproximateLocationOnly() {
        val preview = event().toPublicPreview()

        assertEquals("Maple Ridge", preview.publicLocation.neighborhood)
        assertEquals("Riverton", preview.publicLocation.city)
        assertEquals("Near Maple Ridge Park", preview.publicLocation.areaDescription)
        assertEquals("2 mi", preview.publicLocation.distanceLabel)
        assertFalse(preview.toString().contains("123 Cedar Street"))
        assertFalse(preview.toString().contains("47.6101"))
        assertFalse(preview.toString().contains("-122.2015"))
    }

    @Test
    fun acceptedRsvpCanRevealExactLocationBeforeEventEnds() {
        val event = event()
        val rsvp = rsvp(
            status = RsvpStatus.ACCEPTED,
            locationVisibility = LocationVisibility.RSVP_ACCEPTED,
        )

        assertTrue(LocationRevealPolicy.canRevealExactLocation(event, rsvp, nowEpochMillis = NOW))
        assertEquals(
            "123 Cedar Street",
            LocationRevealPolicy.exactLocationFor(event, rsvp, nowEpochMillis = NOW)?.streetAddress,
        )
    }

    @Test
    fun requestedRsvpCannotRevealExactLocation() {
        val rsvp = rsvp(
            status = RsvpStatus.REQUESTED,
            locationVisibility = LocationVisibility.RSVP_REQUESTED,
        )

        assertFalse(LocationRevealPolicy.canRevealExactLocation(event(), rsvp, nowEpochMillis = NOW))
        assertNull(LocationRevealPolicy.exactLocationFor(event(), rsvp, nowEpochMillis = NOW))
    }

    @Test
    fun revokedAccessCannotRevealExactLocation() {
        val rsvp = rsvp(
            status = RsvpStatus.ACCEPTED,
            locationVisibility = LocationVisibility.REVOKED,
        )

        assertFalse(LocationRevealPolicy.canRevealExactLocation(event(), rsvp, nowEpochMillis = NOW))
    }

    @Test
    fun expiredAccessCannotRevealExactLocation() {
        val rsvp = rsvp(
            status = RsvpStatus.ACCEPTED,
            locationVisibility = LocationVisibility.EXPIRED,
        )

        assertFalse(LocationRevealPolicy.canRevealExactLocation(event(), rsvp, nowEpochMillis = NOW))
        assertFalse(LocationRevealPolicy.canRevealExactLocation(event(), acceptedRsvp(), nowEpochMillis = AFTER_END))
    }

    @Test
    fun cancelledOrCompletedEventsCannotRevealExactLocation() {
        assertFalse(
            LocationRevealPolicy.canRevealExactLocation(
                event(status = EventStatus.CANCELLED),
                acceptedRsvp(),
                nowEpochMillis = NOW,
            ),
        )
        assertFalse(
            LocationRevealPolicy.canRevealExactLocation(
                event(status = EventStatus.COMPLETED),
                acceptedRsvp(),
                nowEpochMillis = NOW,
            ),
        )
    }

    private fun acceptedRsvp(): Rsvp =
        rsvp(
            status = RsvpStatus.ACCEPTED,
            locationVisibility = LocationVisibility.RSVP_ACCEPTED,
        )

    private fun rsvp(
        status: RsvpStatus,
        locationVisibility: LocationVisibility,
        eventId: String = EVENT_ID,
    ): Rsvp =
        Rsvp(
            id = "rsvp-1",
            eventId = eventId,
            shopperId = "shopper-1",
            status = status,
            locationVisibility = locationVisibility,
        )

    private fun event(status: EventStatus = EventStatus.PUBLISHED): YardSaleEvent =
        YardSaleEvent(
            id = EVENT_ID,
            title = "Maple Ridge Yard Sale",
            description = "Housewares, kids bikes, and garden tools.",
            saleWindow = SaleWindow(
                startsAtEpochMillis = START,
                endsAtEpochMillis = END,
            ),
            categories = listOf("housewares", "kids", "garden"),
            photos = listOf(EventPhoto(url = "https://example.test/photo.jpg")),
            host = UserProfile(
                id = "host-1",
                displayName = "Avery",
                role = UserRole.HOST,
                verificationState = VerificationState.VERIFIED,
                trustSignals = listOf("Hosted 3 sales"),
            ),
            status = status,
            location = EventLocation(
                publicLocation = PublicLocation(
                    neighborhood = "Maple Ridge",
                    city = "Riverton",
                    areaDescription = "Near Maple Ridge Park",
                    distanceLabel = "2 mi",
                ),
                exactAddress = ExactAddress(
                    streetAddress = "123 Cedar Street",
                    unit = "Garage",
                    city = "Riverton",
                    region = "WA",
                    postalCode = "98000",
                    latitude = 47.6101,
                    longitude = -122.2015,
                    accessInstructions = "Use the side gate.",
                ),
            ),
        )

    private companion object {
        const val EVENT_ID = "event-1"
        const val START = 1_000L
        const val NOW = 2_000L
        const val END = 3_000L
        const val AFTER_END = END
    }
}

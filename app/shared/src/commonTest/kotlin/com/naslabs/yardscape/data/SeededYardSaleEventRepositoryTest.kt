package com.naslabs.yardscape.data

import com.naslabs.yardscape.domain.EventStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SeededYardSaleEventRepositoryTest {
    private val repository = SeededYardSaleEventRepository()
    private val now = SeededYardSaleData.BASE_NOW_EPOCH_MILLIS

    @Test
    fun publicPreviewsContainOnlyPublishedUpcomingEvents() {
        val previews = repository.publicPreviews(now)

        assertEquals(
            listOf(
                SeededYardSaleData.FAMILY_GARAGE_EVENT_ID,
                SeededYardSaleData.ESTATE_TOOLS_EVENT_ID,
            ),
            previews.map { it.id },
        )
        assertTrue(previews.all { it.status == EventStatus.PUBLISHED })
    }

    @Test
    fun publicPreviewsDoNotLeakExactLocationData() {
        val previewText = repository.publicPreviews(now).joinToString()

        assertFalse(previewText.contains("123 Cedar Street"))
        assertFalse(previewText.contains("418 Juniper Avenue"))
        assertFalse(previewText.contains("47.6101"))
        assertFalse(previewText.contains("-122.2142"))
    }

    @Test
    fun publicDetailIsPrivacySafeByDefault() {
        val detail = repository.publicEventDetail(SeededYardSaleData.FAMILY_GARAGE_EVENT_ID)

        assertNotNull(detail)
        assertEquals("Maple Ridge", detail.publicLocation.neighborhood)
        assertEquals(listOf("Cash", "Venmo"), detail.acceptedPaymentTypes)
        assertTrue(detail.accessibilityNotes.contains("Driveway sale"))
        assertTrue(detail.rsvpPrompt.contains("RSVP"))
        assertFalse(detail.toString().contains("123 Cedar Street"))
    }

    @Test
    fun requestedRsvpDoesNotRevealExactLocation() {
        val exactLocation = repository.exactLocationFor(
            eventId = SeededYardSaleData.FAMILY_GARAGE_EVENT_ID,
            shopperId = SeededYardSaleData.SHOPPER_WITHOUT_ACCESS_ID,
            nowEpochMillis = now,
        )

        assertNull(exactLocation)
    }

    @Test
    fun acceptedRsvpRevealsExactLocationForPublishedUpcomingEvent() {
        val exactLocation = repository.exactLocationFor(
            eventId = SeededYardSaleData.ESTATE_TOOLS_EVENT_ID,
            shopperId = SeededYardSaleData.SHOPPER_WITH_ACCEPTED_ACCESS_ID,
            nowEpochMillis = now,
        )

        assertEquals("418 Juniper Avenue", exactLocation?.streetAddress)
    }

    @Test
    fun cancelledSeedEventDoesNotRevealEvenWithRsvpRecord() {
        val exactLocation = repository.exactLocationFor(
            eventId = SeededYardSaleData.CANCELLED_EVENT_ID,
            shopperId = SeededYardSaleData.SHOPPER_WITH_ACCEPTED_ACCESS_ID,
            nowEpochMillis = now,
        )

        assertNull(exactLocation)
    }

    @Test
    fun submitRsvpAutoAcceptsForMvpRevealTesting() {
        val submitted = repository.submitRsvp(
            eventId = SeededYardSaleData.FAMILY_GARAGE_EVENT_ID,
            shopperId = "shopper-new",
        )
        val exactLocation = repository.exactLocationFor(
            eventId = SeededYardSaleData.FAMILY_GARAGE_EVENT_ID,
            shopperId = "shopper-new",
            nowEpochMillis = now,
        )

        assertNotNull(submitted)
        assertEquals("123 Cedar Street", exactLocation?.streetAddress)
    }

    @Test
    fun hostEventsIncludeDraftAndCancelledManagementStates() {
        val averyEvents = repository.hostEvents(SeededYardSaleData.HOST_AVERY_ID)
        val marinEvents = repository.hostEvents(SeededYardSaleData.HOST_MARIN_ID)

        assertTrue(averyEvents.any { it.id == SeededYardSaleData.DRAFT_EVENT_ID })
        assertTrue(marinEvents.any { it.id == SeededYardSaleData.CANCELLED_EVENT_ID })
    }

    @Test
    fun publishingRequiresTimeAndLocationFields() {
        val result = repository.saveHostEvent(
            draft = validHostDraft().copy(
                startsAtEpochMillis = null,
                publicNeighborhood = "",
                exactStreetAddress = "",
            ),
            status = EventStatus.PUBLISHED,
        )

        assertFalse(result.isSuccess)
        assertTrue(result.validationErrors.any { it.contains("Start time") })
        assertTrue(result.validationErrors.any { it.contains("Public neighborhood") })
        assertTrue(result.validationErrors.any { it.contains("Protected street address") })
    }

    @Test
    fun draftCanBeSavedWithoutAppearingInBrowse() {
        val result = repository.saveHostEvent(
            draft = validHostDraft().copy(id = "event-local-draft"),
            status = EventStatus.DRAFT,
        )

        assertTrue(result.isSuccess)
        assertTrue(repository.hostEvents(SeededYardSaleData.HOST_AVERY_ID).any { it.id == "event-local-draft" })
        assertFalse(repository.publicPreviews(now).any { it.id == "event-local-draft" })
    }

    @Test
    fun publishedHostEventAppearsInBrowseAndKeepsExactAddressProtected() {
        val result = repository.saveHostEvent(
            draft = validHostDraft().copy(id = "event-local-published"),
            status = EventStatus.PUBLISHED,
        )
        val preview = repository.publicPreviews(now).firstOrNull { it.id == "event-local-published" }

        assertTrue(result.isSuccess)
        assertNotNull(preview)
        assertFalse(preview.toString().contains("900 Hidden Lane"))
    }

    @Test
    fun hostCanEditProtectedAddressAndCancelRevokesReveal() {
        repository.saveHostEvent(
            draft = validHostDraft().copy(id = "event-local-edit"),
            status = EventStatus.PUBLISHED,
        )
        repository.saveHostEvent(
            draft = validHostDraft().copy(
                id = "event-local-edit",
                title = "Edited title",
                exactStreetAddress = "901 Hidden Lane",
            ),
            status = EventStatus.PUBLISHED,
        )
        repository.submitRsvp("event-local-edit", "shopper-edit")

        assertEquals("901 Hidden Lane", repository.exactLocationFor("event-local-edit", "shopper-edit", now)?.streetAddress)
        assertTrue(repository.cancelHostEvent("event-local-edit"))
        assertNull(repository.exactLocationFor("event-local-edit", "shopper-edit", now))
    }

    private fun validHostDraft(): HostEventDraft =
        HostEventDraft(
            hostId = SeededYardSaleData.HOST_AVERY_ID,
            title = "Saturday Shed Cleanout",
            description = "Garden tools, storage bins, and a folding table.",
            startsAtEpochMillis = now + 10_000L,
            endsAtEpochMillis = now + 20_000L,
            publicNeighborhood = "Maple Ridge",
            publicCity = "Riverton",
            publicAreaDescription = "Near the south park entrance",
            publicDistanceLabel = "3 mi",
            exactStreetAddress = "900 Hidden Lane",
            exactCity = "Riverton",
            exactRegion = "WA",
            exactPostalCode = "98002",
            exactLatitude = 47.611,
            exactLongitude = -122.202,
            accessInstructions = "Knock on the garage door.",
            categories = listOf("garden", "storage"),
            acceptedPaymentTypes = listOf("Cash"),
            accessibilityNotes = listOf("Flat driveway"),
        )
}

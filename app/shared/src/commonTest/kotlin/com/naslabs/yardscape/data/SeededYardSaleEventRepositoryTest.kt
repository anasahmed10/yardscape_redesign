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
    fun hostEventsIncludeDraftAndCancelledManagementStates() {
        val averyEvents = repository.hostEvents(SeededYardSaleData.HOST_AVERY_ID)
        val marinEvents = repository.hostEvents(SeededYardSaleData.HOST_MARIN_ID)

        assertTrue(averyEvents.any { it.id == SeededYardSaleData.DRAFT_EVENT_ID })
        assertTrue(marinEvents.any { it.id == SeededYardSaleData.CANCELLED_EVENT_ID })
    }
}

package com.naslabs.yardscape.data

import com.naslabs.yardscape.domain.ExactAddress
import com.naslabs.yardscape.domain.EventPhoto
import com.naslabs.yardscape.domain.EventStatus
import com.naslabs.yardscape.domain.PublicEventPreview
import com.naslabs.yardscape.domain.PublicLocation
import com.naslabs.yardscape.domain.Rsvp
import com.naslabs.yardscape.domain.SaleWindow
import com.naslabs.yardscape.domain.YardSaleEvent

interface YardSaleEventRepository {
    fun publicPreviews(nowEpochMillis: Long): List<PublicEventPreview>

    fun publicEventDetail(eventId: String): PublicEventDetail?

    fun rsvpFor(eventId: String, shopperId: String): Rsvp?

    fun exactLocationFor(
        eventId: String,
        shopperId: String,
        nowEpochMillis: Long,
    ): ExactAddress?

    fun hostEvents(hostId: String): List<YardSaleEvent>
}

data class PublicEventDetail(
    val id: String,
    val title: String,
    val description: String,
    val saleWindow: SaleWindow,
    val categories: List<String>,
    val photos: List<EventPhoto>,
    val hostDisplayName: String,
    val hostTrustSignals: List<String>,
    val publicLocation: PublicLocation,
    val status: EventStatus,
    val rsvpPrompt: String,
)

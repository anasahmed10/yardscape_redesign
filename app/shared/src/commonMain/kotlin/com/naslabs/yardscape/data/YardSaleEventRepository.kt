package com.naslabs.yardscape.data

import com.naslabs.yardscape.domain.ExactAddress
import com.naslabs.yardscape.domain.EventLocation
import com.naslabs.yardscape.domain.EventPhoto
import com.naslabs.yardscape.domain.EventStatus
import com.naslabs.yardscape.domain.PublicEventPreview
import com.naslabs.yardscape.domain.PublicLocation
import com.naslabs.yardscape.domain.Rsvp
import com.naslabs.yardscape.domain.SaleWindow
import com.naslabs.yardscape.domain.UserProfile
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

    fun submitRsvp(eventId: String, shopperId: String): Rsvp?

    fun hostEvents(hostId: String): List<YardSaleEvent>

    fun hostEvent(eventId: String): YardSaleEvent?

    fun saveHostEvent(draft: HostEventDraft, status: EventStatus): HostEventSaveResult

    fun cancelHostEvent(eventId: String): Boolean

    fun hideHostEvent(eventId: String): Boolean
}

data class PublicEventDetail(
    val id: String,
    val title: String,
    val description: String,
    val saleWindow: SaleWindow,
    val categories: List<String>,
    val photos: List<EventPhoto>,
    val acceptedPaymentTypes: List<String>,
    val accessibilityNotes: List<String>,
    val hostDisplayName: String,
    val hostTrustSignals: List<String>,
    val publicLocation: PublicLocation,
    val status: EventStatus,
    val rsvpPrompt: String,
)

data class HostEventDraft(
    val id: String? = null,
    val hostId: String,
    val title: String,
    val description: String,
    val startsAtEpochMillis: Long?,
    val endsAtEpochMillis: Long?,
    val publicNeighborhood: String,
    val publicCity: String,
    val publicAreaDescription: String,
    val publicDistanceLabel: String? = null,
    val exactStreetAddress: String,
    val exactUnit: String? = null,
    val exactCity: String,
    val exactRegion: String,
    val exactPostalCode: String,
    val exactLatitude: Double,
    val exactLongitude: Double,
    val accessInstructions: String? = null,
    val categories: List<String>,
    val acceptedPaymentTypes: List<String>,
    val accessibilityNotes: List<String>,
)

data class HostEventSaveResult(
    val event: YardSaleEvent?,
    val validationErrors: List<String>,
) {
    val isSuccess: Boolean = event != null && validationErrors.isEmpty()
}

fun HostEventDraft.validateFor(status: EventStatus): List<String> {
    val errors = mutableListOf<String>()
    if (title.isBlank()) errors += "Title is required."
    if (description.isBlank()) errors += "Description is required."
    if (categories.isEmpty()) errors += "At least one category is required."

    if (status == EventStatus.PUBLISHED) {
        if (startsAtEpochMillis == null) errors += "Start time is required to publish."
        if (endsAtEpochMillis == null) errors += "End time is required to publish."
        if (startsAtEpochMillis != null &&
            endsAtEpochMillis != null &&
            startsAtEpochMillis >= endsAtEpochMillis
        ) {
            errors += "End time must be after start time."
        }
        if (publicNeighborhood.isBlank()) errors += "Public neighborhood is required to publish."
        if (publicCity.isBlank()) errors += "Public city is required to publish."
        if (publicAreaDescription.isBlank()) errors += "Public area description is required to publish."
        if (exactStreetAddress.isBlank()) errors += "Protected street address is required to publish."
        if (exactCity.isBlank()) errors += "Protected city is required to publish."
        if (exactRegion.isBlank()) errors += "Protected region is required to publish."
        if (exactPostalCode.isBlank()) errors += "Protected postal code is required to publish."
    }
    return errors
}

fun HostEventDraft.toYardSaleEvent(
    host: UserProfile,
    status: EventStatus,
    fallbackNowEpochMillis: Long,
): YardSaleEvent {
    val start = startsAtEpochMillis ?: fallbackNowEpochMillis
    val end = endsAtEpochMillis ?: (start + DEFAULT_DRAFT_WINDOW_MILLIS)
    return YardSaleEvent(
        id = id ?: "event-host-${fallbackNowEpochMillis}",
        title = title.ifBlank { "Untitled yard sale" },
        description = description,
        saleWindow = SaleWindow(
            startsAtEpochMillis = start,
            endsAtEpochMillis = if (end > start) end else start + DEFAULT_DRAFT_WINDOW_MILLIS,
        ),
        categories = categories.ifEmpty { listOf("general") },
        photos = emptyList(),
        acceptedPaymentTypes = acceptedPaymentTypes,
        accessibilityNotes = accessibilityNotes,
        host = host,
        status = status,
        location = EventLocation(
            publicLocation = PublicLocation(
                neighborhood = publicNeighborhood,
                city = publicCity,
                areaDescription = publicAreaDescription,
                distanceLabel = publicDistanceLabel,
            ),
            exactAddress = ExactAddress(
                streetAddress = exactStreetAddress,
                unit = exactUnit,
                city = exactCity,
                region = exactRegion,
                postalCode = exactPostalCode,
                latitude = exactLatitude,
                longitude = exactLongitude,
                accessInstructions = accessInstructions,
            ),
        ),
    )
}

private const val DEFAULT_DRAFT_WINDOW_MILLIS = 4L * 60L * 60L * 1_000L

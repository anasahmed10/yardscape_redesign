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
    val selectedMapLocation: MapSelectedLocation? = null,
    val categories: List<String>,
    val acceptedPaymentTypes: List<String>,
    val accessibilityNotes: List<String>,
)

data class MapSelectedLocation(
    val providerPlaceId: String,
    val displayName: String,
    val formattedAddress: String,
    val streetAddress: String,
    val city: String,
    val region: String,
    val postalCode: String,
    val latitude: Double,
    val longitude: Double,
    val publicNeighborhood: String,
    val publicAreaDescription: String,
    val publicDistanceLabel: String? = null,
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
        if (resolvedPublicNeighborhood().isBlank()) errors += "Map location is required to publish."
        if (resolvedPublicCity().isBlank()) errors += "Map location city is required to publish."
        if (resolvedPublicAreaDescription().isBlank()) errors += "Map location public area is required to publish."
        if (resolvedExactStreetAddress().isBlank()) errors += "Protected map street address is required to publish."
        if (resolvedExactCity().isBlank()) errors += "Protected map city is required to publish."
        if (resolvedExactRegion().isBlank()) errors += "Protected map region is required to publish."
        if (resolvedExactPostalCode().isBlank()) errors += "Protected map postal code is required to publish."
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
                neighborhood = resolvedPublicNeighborhood(),
                city = resolvedPublicCity(),
                areaDescription = resolvedPublicAreaDescription(),
                distanceLabel = resolvedPublicDistanceLabel(),
            ),
            exactAddress = ExactAddress(
                streetAddress = resolvedExactStreetAddress(),
                unit = exactUnit,
                city = resolvedExactCity(),
                region = resolvedExactRegion(),
                postalCode = resolvedExactPostalCode(),
                latitude = resolvedExactLatitude(),
                longitude = resolvedExactLongitude(),
                accessInstructions = accessInstructions,
            ),
        ),
    )
}

fun HostEventDraft.withMapSelectedLocation(location: MapSelectedLocation): HostEventDraft =
    copy(
        selectedMapLocation = location,
        publicNeighborhood = location.publicNeighborhood,
        publicCity = location.city,
        publicAreaDescription = location.publicAreaDescription,
        publicDistanceLabel = location.publicDistanceLabel,
        exactStreetAddress = location.streetAddress,
        exactCity = location.city,
        exactRegion = location.region,
        exactPostalCode = location.postalCode,
        exactLatitude = location.latitude,
        exactLongitude = location.longitude,
    )

private fun HostEventDraft.resolvedPublicNeighborhood(): String =
    selectedMapLocation?.publicNeighborhood ?: publicNeighborhood

private fun HostEventDraft.resolvedPublicCity(): String =
    selectedMapLocation?.city ?: publicCity

private fun HostEventDraft.resolvedPublicAreaDescription(): String =
    selectedMapLocation?.publicAreaDescription ?: publicAreaDescription

private fun HostEventDraft.resolvedPublicDistanceLabel(): String? =
    selectedMapLocation?.publicDistanceLabel ?: publicDistanceLabel

private fun HostEventDraft.resolvedExactStreetAddress(): String =
    selectedMapLocation?.streetAddress ?: exactStreetAddress

private fun HostEventDraft.resolvedExactCity(): String =
    selectedMapLocation?.city ?: exactCity

private fun HostEventDraft.resolvedExactRegion(): String =
    selectedMapLocation?.region ?: exactRegion

private fun HostEventDraft.resolvedExactPostalCode(): String =
    selectedMapLocation?.postalCode ?: exactPostalCode

private fun HostEventDraft.resolvedExactLatitude(): Double =
    selectedMapLocation?.latitude ?: exactLatitude

private fun HostEventDraft.resolvedExactLongitude(): Double =
    selectedMapLocation?.longitude ?: exactLongitude

private const val DEFAULT_DRAFT_WINDOW_MILLIS = 4L * 60L * 60L * 1_000L

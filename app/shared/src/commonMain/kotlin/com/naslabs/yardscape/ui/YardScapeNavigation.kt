package com.naslabs.yardscape.ui

import com.naslabs.yardscape.data.PublicEventDetail
import com.naslabs.yardscape.data.SeededYardSaleData
import com.naslabs.yardscape.data.SeededYardSaleEventRepository
import com.naslabs.yardscape.data.YardSaleEventRepository
import com.naslabs.yardscape.data.HostEventDraft
import com.naslabs.yardscape.data.HostEventSaveResult
import com.naslabs.yardscape.domain.EventStatus
import com.naslabs.yardscape.domain.ExactAddress
import com.naslabs.yardscape.domain.LocationVisibility
import com.naslabs.yardscape.domain.PublicEventPreview
import com.naslabs.yardscape.domain.RsvpStatus
import com.naslabs.yardscape.domain.YardSaleEvent

sealed interface YardScapeRoute {
    data object Browse : YardScapeRoute
    data class EventDetail(val eventId: String) : YardScapeRoute
    data class Rsvp(val eventId: String) : YardScapeRoute
    data class HostCreateEdit(val eventId: String? = null) : YardScapeRoute
}

class YardScapeAppState(
    private val repository: YardSaleEventRepository = SeededYardSaleEventRepository(),
    private val nowEpochMillis: Long = SeededYardSaleData.BASE_NOW_EPOCH_MILLIS,
    private val shopperId: String = SeededYardSaleData.SHOPPER_WITHOUT_ACCESS_ID,
    private val hostId: String = SeededYardSaleData.HOST_AVERY_ID,
) {
    var route: YardScapeRoute = YardScapeRoute.Browse
        private set

    fun browseItems(): List<BrowseEventItem> =
        repository.publicPreviews(nowEpochMillis).map { it.toBrowseEventItem(nowEpochMillis) }

    fun selectedEventDetailState(): EventDetailState? {
        val detailRoute = route as? YardScapeRoute.EventDetail ?: return null
        return detailStateFor(detailRoute.eventId)
    }

    fun detailStateFor(eventId: String): EventDetailState? {
        val detail = repository.publicEventDetail(eventId) ?: return null
        val rsvp = repository.rsvpFor(eventId, shopperId)
        val exactAddress = repository.exactLocationFor(
            eventId = eventId,
            shopperId = shopperId,
            nowEpochMillis = nowEpochMillis,
        )
        return EventDetailState(
            detail = detail,
            revealState = detail.toLocationRevealState(
                rsvpStatus = rsvp?.status,
                locationVisibility = rsvp?.locationVisibility,
                exactAddress = exactAddress,
            ),
        )
    }

    fun openEvent(eventId: String) {
        route = YardScapeRoute.EventDetail(eventId)
    }

    fun openRsvp(eventId: String) {
        route = YardScapeRoute.Rsvp(eventId)
    }

    fun confirmRsvp(eventId: String) {
        repository.submitRsvp(eventId, shopperId)
        route = YardScapeRoute.EventDetail(eventId)
    }

    fun openHostCreateEdit(eventId: String? = null) {
        route = YardScapeRoute.HostCreateEdit(eventId)
    }

    fun returnToBrowse() {
        route = YardScapeRoute.Browse
    }

    fun hostEventItems(): List<HostEventItem> =
        repository.hostEvents(hostId).map { it.toHostEventItem(nowEpochMillis) }

    fun hostEditorState(eventId: String?): HostEditorState =
        HostEditorState(
            draft = repository.hostEvent(eventId.orEmpty())?.toHostEventDraft()
                ?: blankHostEventDraft(),
            validationErrors = emptyList(),
        )

    fun saveHostDraft(draft: HostEventDraft): HostEditorState =
        hostStateFrom(draft, repository.saveHostEvent(draft, EventStatus.DRAFT))

    fun publishHostEvent(draft: HostEventDraft): HostEditorState =
        hostStateFrom(draft, repository.saveHostEvent(draft, EventStatus.PUBLISHED))

    fun cancelHostEvent(eventId: String) {
        repository.cancelHostEvent(eventId)
        route = YardScapeRoute.HostCreateEdit(eventId)
    }

    fun hideHostEvent(eventId: String) {
        repository.hideHostEvent(eventId)
        route = YardScapeRoute.HostCreateEdit(eventId)
    }

    private fun blankHostEventDraft(): HostEventDraft =
        HostEventDraft(
            hostId = hostId,
            title = "",
            description = "",
            startsAtEpochMillis = nowEpochMillis + MILLIS_PER_DAY,
            endsAtEpochMillis = nowEpochMillis + MILLIS_PER_DAY + 5L * MILLIS_PER_HOUR,
            publicNeighborhood = "",
            publicCity = "",
            publicAreaDescription = "",
            exactStreetAddress = "",
            exactCity = "",
            exactRegion = "",
            exactPostalCode = "",
            exactLatitude = 0.0,
            exactLongitude = 0.0,
            categories = emptyList(),
            acceptedPaymentTypes = emptyList(),
            accessibilityNotes = emptyList(),
        )

    private fun hostStateFrom(originalDraft: HostEventDraft, result: HostEventSaveResult): HostEditorState {
        val draft = result.event?.toHostEventDraft() ?: originalDraft
        return HostEditorState(
            draft = draft,
            validationErrors = result.validationErrors,
            savedEventId = result.event?.id,
        )
    }
}

data class BrowseEventItem(
    val id: String,
    val title: String,
    val dateLabel: String,
    val locationLabel: String,
    val categoryLabels: List<String>,
    val statusLabel: String,
)

data class HostEventItem(
    val id: String,
    val title: String,
    val statusLabel: String,
    val dateLabel: String,
    val publicLocationLabel: String,
)

data class HostEditorState(
    val draft: HostEventDraft,
    val validationErrors: List<String>,
    val savedEventId: String? = draft.id,
)

data class EventDetailState(
    val detail: PublicEventDetail,
    val revealState: LocationRevealState,
) {
    val shouldShowRsvpAction: Boolean =
        detail.status == EventStatus.PUBLISHED &&
            revealState !is LocationRevealState.Revealed
}

sealed interface LocationRevealState {
    val title: String
    val message: String

    data object NotRequested : LocationRevealState {
        override val title: String = "Approximate area only"
        override val message: String =
            "Exact addresses stay private until your RSVP is accepted."
    }

    data object Pending : LocationRevealState {
        override val title: String = "RSVP pending"
        override val message: String =
            "The host has not granted exact-location access yet."
    }

    data object Revoked : LocationRevealState {
        override val title: String = "Access revoked"
        override val message: String =
            "The host removed exact-location access for this RSVP."
    }

    data object Expired : LocationRevealState {
        override val title: String = "Location access expired"
        override val message: String =
            "Exact-location access ends after the sale window closes."
    }

    data object Cancelled : LocationRevealState {
        override val title: String = "Sale cancelled"
        override val message: String =
            "This event is no longer active, so exact-location access is hidden."
    }

    data class Revealed(val exactAddress: ExactAddress) : LocationRevealState {
        override val title: String = "Exact location"
        override val message: String = exactAddress.displayLabel()
    }
}

fun PublicEventPreview.toBrowseEventItem(nowEpochMillis: Long): BrowseEventItem =
    BrowseEventItem(
        id = id,
        title = title,
        dateLabel = saleWindow.toBrowseDateLabel(nowEpochMillis),
        locationLabel = listOfNotNull(
            publicLocation.neighborhood,
            publicLocation.distanceLabel ?: publicLocation.areaDescription,
        ).joinToString(" - "),
        categoryLabels = categories,
        statusLabel = status.name.lowercase().replaceFirstChar { it.uppercase() },
    )

fun YardSaleEvent.toHostEventItem(nowEpochMillis: Long): HostEventItem =
    HostEventItem(
        id = id,
        title = title,
        statusLabel = status.name.lowercase().replaceFirstChar { it.uppercase() },
        dateLabel = saleWindow.toBrowseDateLabel(nowEpochMillis),
        publicLocationLabel = listOf(
            location.publicLocation.neighborhood,
            location.publicLocation.city,
        ).filter { it.isNotBlank() }.joinToString(" - "),
    )

fun YardSaleEvent.toHostEventDraft(): HostEventDraft =
    HostEventDraft(
        id = id,
        hostId = host.id,
        title = title,
        description = description,
        startsAtEpochMillis = saleWindow.startsAtEpochMillis,
        endsAtEpochMillis = saleWindow.endsAtEpochMillis,
        publicNeighborhood = location.publicLocation.neighborhood,
        publicCity = location.publicLocation.city,
        publicAreaDescription = location.publicLocation.areaDescription,
        publicDistanceLabel = location.publicLocation.distanceLabel,
        exactStreetAddress = location.exactAddress.streetAddress,
        exactUnit = location.exactAddress.unit,
        exactCity = location.exactAddress.city,
        exactRegion = location.exactAddress.region,
        exactPostalCode = location.exactAddress.postalCode,
        exactLatitude = location.exactAddress.latitude,
        exactLongitude = location.exactAddress.longitude,
        accessInstructions = location.exactAddress.accessInstructions,
        categories = categories,
        acceptedPaymentTypes = acceptedPaymentTypes,
        accessibilityNotes = accessibilityNotes,
    )

fun PublicEventDetail.toDetailSections(nowEpochMillis: Long): List<Pair<String, String>> =
    listOf(
        "When" to saleWindow.toBrowseDateLabel(nowEpochMillis),
        "Area" to listOfNotNull(
            publicLocation.neighborhood,
            publicLocation.city,
            publicLocation.distanceLabel ?: publicLocation.areaDescription,
        ).joinToString(" - "),
        "Categories" to categories.joinToString(", "),
        "Payments" to acceptedPaymentTypes.joinToString(", "),
        "Accessibility" to accessibilityNotes.joinToString(", "),
        "Host" to listOf(hostDisplayName, hostTrustSignals.firstOrNull()).joinToString(" - "),
    )

private fun PublicEventDetail.toLocationRevealState(
    rsvpStatus: RsvpStatus?,
    locationVisibility: LocationVisibility?,
    exactAddress: ExactAddress?,
): LocationRevealState {
    if (status == EventStatus.CANCELLED || status == EventStatus.COMPLETED) {
        return LocationRevealState.Cancelled
    }
    if (exactAddress != null) {
        return LocationRevealState.Revealed(exactAddress)
    }
    return when {
        locationVisibility == LocationVisibility.REVOKED -> LocationRevealState.Revoked
        locationVisibility == LocationVisibility.EXPIRED -> LocationRevealState.Expired
        rsvpStatus == RsvpStatus.REQUESTED ||
            locationVisibility == LocationVisibility.RSVP_REQUESTED -> LocationRevealState.Pending
        else -> LocationRevealState.NotRequested
    }
}

private fun ExactAddress.displayLabel(): String =
    listOfNotNull(
        streetAddress,
        unit,
        "$city, $region $postalCode",
        accessInstructions,
    ).joinToString("\n")

private fun com.naslabs.yardscape.domain.SaleWindow.toBrowseDateLabel(
    nowEpochMillis: Long,
): String {
    val dayOffset = (startsAtEpochMillis - nowEpochMillis).floorDiv(MILLIS_PER_DAY)
    val dayLabel = when (dayOffset) {
        0L -> "Today"
        1L -> "Tomorrow"
        else -> "In $dayOffset days"
    }
    return "$dayLabel, ${startsAtEpochMillis.toHourLabel()}-${endsAtEpochMillis.toHourLabel()}"
}

private fun Long.toHourLabel(): String {
    val hour24 = (floorMod(MILLIS_PER_DAY) / MILLIS_PER_HOUR).toInt()
    val hour12 = when (val normalized = hour24 % 12) {
        0 -> 12
        else -> normalized
    }
    val meridiem = if (hour24 < 12) "AM" else "PM"
    return "$hour12 $meridiem"
}

private fun Long.floorDiv(other: Long): Long {
    val quotient = this / other
    val remainder = this % other
    return if (remainder != 0L && (this xor other) < 0L) quotient - 1L else quotient
}

private fun Long.floorMod(other: Long): Long =
    this - floorDiv(other) * other

private const val MILLIS_PER_HOUR = 60L * 60L * 1_000L
private const val MILLIS_PER_DAY = 24L * MILLIS_PER_HOUR

package com.naslabs.yardscape.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.naslabs.yardscape.data.PublicEventDetail
import com.naslabs.yardscape.data.SeededYardSaleData
import com.naslabs.yardscape.data.SeededYardSaleEventRepository
import com.naslabs.yardscape.data.YardSaleEventRepository
import com.naslabs.yardscape.data.HostEventDraft
import com.naslabs.yardscape.data.HostEventSaveResult
import com.naslabs.yardscape.data.MapLocationSearchRepository
import com.naslabs.yardscape.data.MapSelectedLocation
import com.naslabs.yardscape.data.SeededMapLocationSearchRepository
import com.naslabs.yardscape.domain.EventStatus
import com.naslabs.yardscape.domain.ExactAddress
import com.naslabs.yardscape.domain.LocationVisibility
import com.naslabs.yardscape.domain.PublicEventPreview
import com.naslabs.yardscape.domain.RsvpStatus
import com.naslabs.yardscape.domain.UserRole
import com.naslabs.yardscape.domain.YardSaleEvent

enum class YardScapePrimaryDestination(
    val label: String,
    val contextLabel: String,
) {
    Browse(label = "Browse", contextLabel = "Shopper workspace"),
    Rsvps(label = "RSVPs", contextLabel = "Shopper workspace"),
    Host(label = "Host", contextLabel = "Host workspace"),
    Account(label = "Account", contextLabel = "Account workspace"),
}

sealed interface YardScapeRoute {
    val path: String
    val destinationLabel: String
    val primaryDestination: YardScapePrimaryDestination

    data object Browse : YardScapeRoute {
        override val path: String = "/browse"
        override val destinationLabel: String = "Browse sales"
        override val primaryDestination: YardScapePrimaryDestination = YardScapePrimaryDestination.Browse
    }

    data object Rsvps : YardScapeRoute {
        override val path: String = "/rsvps"
        override val destinationLabel: String = "My RSVPs"
        override val primaryDestination: YardScapePrimaryDestination = YardScapePrimaryDestination.Rsvps
    }

    data object Host : YardScapeRoute {
        override val path: String = "/host"
        override val destinationLabel: String = "Host tools"
        override val primaryDestination: YardScapePrimaryDestination = YardScapePrimaryDestination.Host
    }

    data object Account : YardScapeRoute {
        override val path: String = "/account"
        override val destinationLabel: String = "Account"
        override val primaryDestination: YardScapePrimaryDestination = YardScapePrimaryDestination.Account
    }

    data class EventDetail(val eventId: String) : YardScapeRoute {
        override val path: String = "/events/$eventId"
        override val destinationLabel: String = "Event details"
        override val primaryDestination: YardScapePrimaryDestination = YardScapePrimaryDestination.Browse
    }

    data class Rsvp(val eventId: String) : YardScapeRoute {
        override val path: String = "/events/$eventId/rsvp"
        override val destinationLabel: String = "RSVP"
        override val primaryDestination: YardScapePrimaryDestination = YardScapePrimaryDestination.Browse
    }

    data class HostCreateEdit(val eventId: String? = null) : YardScapeRoute {
        override val path: String = eventId?.let { "/host/events/$it/edit" } ?: "/host/events/new"
        override val destinationLabel: String = if (eventId == null) "Create sale" else "Edit sale"
        override val primaryDestination: YardScapePrimaryDestination = YardScapePrimaryDestination.Host
    }

    companion object {
        fun fromPath(path: String): YardScapeRoute? {
            val segments = path.substringBefore('?').substringBefore('#')
                .trim()
                .trim('/')
                .split('/')
                .filter { it.isNotBlank() }
            return when {
                segments == listOf("browse") || segments.isEmpty() -> Browse
                segments == listOf("rsvps") -> Rsvps
                segments == listOf("host") -> Host
                segments == listOf("account") -> Account
                segments.size == 2 && segments[0] == "events" -> EventDetail(segments[1])
                segments.size == 3 && segments[0] == "events" && segments[2] == "rsvp" -> Rsvp(segments[1])
                segments == listOf("host", "events", "new") -> HostCreateEdit()
                segments.size == 4 &&
                    segments[0] == "host" && segments[1] == "events" && segments[3] == "edit" ->
                    HostCreateEdit(segments[2])
                else -> null
            }
        }
    }
}

object YardScapeTestTags {
    const val BrowseScreen: String = "browse-screen"
    const val LocationAccessPanel: String = "event-detail-location-access"
    const val ExactLocationContent: String = "event-detail-exact-location"
    const val RsvpAction: String = "event-detail-rsvp-action"
    const val RsvpConfirmAction: String = "rsvp-confirm-action"
    const val AppShell: String = "app-shell"

    fun browseEventCard(eventId: String): String = "browse-event-card-$eventId"
    fun primaryDestination(destination: YardScapePrimaryDestination): String =
        "primary-destination-${destination.name.lowercase()}"
}

class YardScapeAppState(
    private val repository: YardSaleEventRepository = SeededYardSaleEventRepository(),
    private val mapLocationSearchRepository: MapLocationSearchRepository = SeededMapLocationSearchRepository(),
    private val nowEpochMillis: Long = SeededYardSaleData.BASE_NOW_EPOCH_MILLIS,
    private val shopperId: String = SeededYardSaleData.SHOPPER_WITHOUT_ACCESS_ID,
    private val hostId: String = SeededYardSaleData.HOST_AVERY_ID,
    val activeUserRole: UserRole = UserRole.SHOPPER,
    val dataAvailability: AppDataAvailability = AppDataAvailability.Available,
    private val eventCapacitySource: EventCapacitySource = EventCapacitySource.None,
    initialRoute: YardScapeRoute = YardScapeRoute.Browse,
) {
    var route: YardScapeRoute by mutableStateOf(initialRoute)
        private set

    val activePrimaryDestination: YardScapePrimaryDestination
        get() = route.primaryDestination

    fun navigateTo(destination: YardScapePrimaryDestination) {
        route = when (destination) {
            YardScapePrimaryDestination.Browse -> YardScapeRoute.Browse
            YardScapePrimaryDestination.Rsvps -> YardScapeRoute.Rsvps
            YardScapePrimaryDestination.Host -> YardScapeRoute.Host
            YardScapePrimaryDestination.Account -> YardScapeRoute.Account
        }
    }

    fun navigateToPath(path: String): Boolean {
        val target = YardScapeRoute.fromPath(path) ?: return false
        route = target
        return true
    }

    fun navigateBack(): Boolean {
        route = when (val current = route) {
            YardScapeRoute.Browse -> return false
            YardScapeRoute.Rsvps,
            YardScapeRoute.Host,
            YardScapeRoute.Account,
            -> YardScapeRoute.Browse
            is YardScapeRoute.EventDetail -> YardScapeRoute.Browse
            is YardScapeRoute.Rsvp -> YardScapeRoute.EventDetail(current.eventId)
            is YardScapeRoute.HostCreateEdit -> YardScapeRoute.Host
        }
        return true
    }

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
            attendanceState = if (eventCapacitySource.isAtCapacity(eventId)) {
                EventAttendanceState.AtCapacity
            } else {
                EventAttendanceState.Available
            },
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
        navigateTo(YardScapePrimaryDestination.Browse)
    }

    fun hostEventItems(): List<HostEventItem> =
        repository.hostEvents(hostId).map { it.toHostEventItem(nowEpochMillis) }

    fun pendingAttendeeCount(eventId: String): Int {
        val isOwningHost = activeUserRole == UserRole.HOST &&
            repository.hostEvent(eventId)?.host?.id == hostId
        if (!isOwningHost) return 0
        return repository.rsvpsForEvent(eventId).count { it.status == RsvpStatus.REQUESTED }
    }

    fun searchHostLocations(query: String): List<MapSelectedLocation> =
        mapLocationSearchRepository.searchHostLocations(query)

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
    val photoDescription: String?,
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
    val attendanceState: EventAttendanceState = EventAttendanceState.Available,
) {
    val shouldShowRsvpAction: Boolean =
        detail.status == EventStatus.PUBLISHED &&
            attendanceState == EventAttendanceState.Available &&
            revealState !is LocationRevealState.Revealed
}

sealed interface AppDataAvailability {
    data object Available : AppDataAvailability
    data object Offline : AppDataAvailability
    data class RecoverableError(val message: String) : AppDataAvailability
}

enum class EventAttendanceState {
    Available,
    AtCapacity,
}

fun interface EventCapacitySource {
    fun isAtCapacity(eventId: String): Boolean

    data object None : EventCapacitySource {
        override fun isAtCapacity(eventId: String): Boolean = false
    }
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
        photoDescription = photos.firstOrNull()?.description,
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
        selectedMapLocation = MapSelectedLocation(
            providerPlaceId = "saved:${id}",
            displayName = location.publicLocation.areaDescription,
            formattedAddress = location.exactAddress.streetAddress,
            streetAddress = location.exactAddress.streetAddress,
            city = location.exactAddress.city,
            region = location.exactAddress.region,
            postalCode = location.exactAddress.postalCode,
            latitude = location.exactAddress.latitude,
            longitude = location.exactAddress.longitude,
            publicNeighborhood = location.publicLocation.neighborhood,
            publicAreaDescription = location.publicLocation.areaDescription,
            publicDistanceLabel = location.publicLocation.distanceLabel,
        ),
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

fun Long.toHostClockTimeLabel(): String {
    val minutesSinceMidnight = (floorMod(MILLIS_PER_DAY) / MILLIS_PER_MINUTE).toInt()
    val hour24 = minutesSinceMidnight / 60
    val minute = minutesSinceMidnight % 60
    val hour12 = when (val normalized = hour24 % 12) {
        0 -> 12
        else -> normalized
    }
    val minuteLabel = minute.toString().padStart(2, '0')
    val meridiem = if (hour24 < 12) "AM" else "PM"
    return "$hour12:$minuteLabel $meridiem"
}

fun Long.withHostClockTime(input: String): Long? {
    val parsedMinutes = input.toClockMinutesSinceMidnight() ?: return null
    val dayStart = this - floorMod(MILLIS_PER_DAY)
    return dayStart + parsedMinutes * MILLIS_PER_MINUTE
}

fun String.toClockMinutesSinceMidnight(): Int? {
    val compactInput = trim().lowercase().replace(".", "")
    if (compactInput.isBlank()) return null

    val meridiem = when {
        compactInput.endsWith("am") -> "am"
        compactInput.endsWith("pm") -> "pm"
        else -> null
    }
    val timePart = when (meridiem) {
        null -> compactInput
        else -> compactInput.removeSuffix(meridiem).trim()
    }
    val parts = timePart.split(":")
    if (parts.size > 2 || parts.any { it.isBlank() }) return null

    val hourInput = parts[0].toIntOrNull() ?: return null
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
    if (minute !in 0..59) return null

    val hour24 = when (meridiem) {
        "am" -> when (hourInput) {
            in 1..11 -> hourInput
            12 -> 0
            else -> return null
        }
        "pm" -> when (hourInput) {
            in 1..11 -> hourInput + 12
            12 -> 12
            else -> return null
        }
        else -> when (hourInput) {
            in 0..23 -> hourInput
            else -> return null
        }
    }
    return hour24 * 60 + minute
}

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
private const val MILLIS_PER_MINUTE = 60L * 1_000L
private const val MILLIS_PER_DAY = 24L * MILLIS_PER_HOUR

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
import com.naslabs.yardscape.data.HostPhotoPicker
import com.naslabs.yardscape.data.MapLocationSearchRepository
import com.naslabs.yardscape.data.MapSelectedLocation
import com.naslabs.yardscape.data.SeededMapLocationSearchRepository
import com.naslabs.yardscape.data.SeededHostPhotoPicker
import com.naslabs.yardscape.domain.EventStatus
import com.naslabs.yardscape.domain.ExactAddress
import com.naslabs.yardscape.domain.LocationVisibility
import com.naslabs.yardscape.domain.PublicEventPreview
import com.naslabs.yardscape.domain.Rsvp
import com.naslabs.yardscape.domain.RsvpStatus
import com.naslabs.yardscape.domain.UserRole
import com.naslabs.yardscape.domain.YardSaleEvent

enum class YardScapePrimaryDestination(
    val label: String,
    val contextLabel: String,
) {
    Browse(label = "Browse", contextLabel = "Shopper workspace"),
    Saved(label = "Saved", contextLabel = "Shopper workspace"),
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

    data object Saved : YardScapeRoute {
        override val path: String = "/saved"
        override val destinationLabel: String = "Saved sales"
        override val primaryDestination: YardScapePrimaryDestination = YardScapePrimaryDestination.Saved
    }

    data object MyRsvps : YardScapeRoute {
        override val path: String = "/rsvps"
        override val destinationLabel: String = "My RSVPs"
        override val primaryDestination: YardScapePrimaryDestination = YardScapePrimaryDestination.Saved
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

    data class EventDetail(
        val eventId: String,
        val origin: YardScapePrimaryDestination = YardScapePrimaryDestination.Browse,
    ) : YardScapeRoute {
        init {
            require(origin == YardScapePrimaryDestination.Browse || origin == YardScapePrimaryDestination.Saved)
        }

        override val path: String = "/events/$eventId"
        override val destinationLabel: String = "Event details"
        override val primaryDestination: YardScapePrimaryDestination = origin
    }

    data class Rsvp(
        val eventId: String,
        val origin: YardScapePrimaryDestination = YardScapePrimaryDestination.Browse,
    ) : YardScapeRoute {
        init {
            require(origin == YardScapePrimaryDestination.Browse || origin == YardScapePrimaryDestination.Saved)
        }

        override val path: String = "/events/$eventId/rsvp"
        override val destinationLabel: String = "RSVP"
        override val primaryDestination: YardScapePrimaryDestination = origin
    }

    data class HostCreateEdit(val eventId: String? = null) : YardScapeRoute {
        override val path: String = eventId?.let { "/host/events/$it/edit" } ?: "/host/events/new"
        override val destinationLabel: String = if (eventId == null) "Create sale" else "Edit sale"
        override val primaryDestination: YardScapePrimaryDestination = YardScapePrimaryDestination.Host
    }

    data class HostAttendees(val eventId: String) : YardScapeRoute {
        override val path: String = "/host/events/$eventId/attendees"
        override val destinationLabel: String = "Manage attendees"
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
                segments == listOf("saved") -> Saved
                segments == listOf("rsvps") -> MyRsvps
                segments == listOf("host") -> Host
                segments == listOf("account") -> Account
                segments.size == 2 && segments[0] == "events" -> EventDetail(segments[1])
                segments.size == 3 && segments[0] == "events" && segments[2] == "rsvp" -> Rsvp(segments[1])
                segments == listOf("host", "events", "new") -> HostCreateEdit()
                segments.size == 4 &&
                    segments[0] == "host" && segments[1] == "events" && segments[3] == "edit" ->
                    HostCreateEdit(segments[2])
                segments.size == 4 &&
                    segments[0] == "host" && segments[1] == "events" && segments[3] == "attendees" ->
                    HostAttendees(segments[2])
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
    const val DiscoveryNoResults: String = "discovery-no-results"
    const val SavedScreen: String = "saved-screen"
    const val MyRsvpsScreen: String = "my-rsvps-screen"

    fun browseEventCard(eventId: String): String = "browse-event-card-$eventId"
    fun primaryDestination(destination: YardScapePrimaryDestination): String =
        "primary-destination-${destination.name.lowercase()}"
}

class YardScapeAppState(
    private val repository: YardSaleEventRepository = SeededYardSaleEventRepository(),
    private val mapLocationSearchRepository: MapLocationSearchRepository = SeededMapLocationSearchRepository(),
    private val hostPhotoPicker: HostPhotoPicker = SeededHostPhotoPicker(),
    private val nowEpochMillis: Long = SeededYardSaleData.BASE_NOW_EPOCH_MILLIS,
    private val shopperId: String = SeededYardSaleData.SHOPPER_WITHOUT_ACCESS_ID,
    private val hostId: String = SeededYardSaleData.HOST_AVERY_ID,
    val activeUserRole: UserRole = UserRole.SHOPPER,
    val dataAvailability: AppDataAvailability = AppDataAvailability.Available,
    private val eventCapacitySource: EventCapacitySource = EventCapacitySource.None,
    initialRoute: YardScapeRoute = YardScapeRoute.Browse,
) {
    private val hostEditorSessions = mutableMapOf<String, HostEditorState>()
    private val hostAttendancePolicies = mutableMapOf(
        SeededYardSaleData.FAMILY_GARAGE_EVENT_ID to HostAttendancePolicy(
            attendeeCap = 2,
            approvalMode = HostRsvpApprovalMode.ManualReview,
        ),
    )
    var route: YardScapeRoute by mutableStateOf(initialRoute)
        private set

    var discoveryFilters: DiscoveryFilters by mutableStateOf(DiscoveryFilters())
        private set

    var discoveryDisplayMode: DiscoveryDisplayMode by mutableStateOf(DiscoveryDisplayMode.List)
        private set

    var savedEventIds: Set<String> by mutableStateOf(emptySet())
        private set

    var pendingRsvpCancellationEventId: String? by mutableStateOf(null)
        private set

    var reminderEventIds: Set<String> by mutableStateOf(emptySet())
        private set

    var calendarExportEventIds: Set<String> by mutableStateOf(emptySet())
        private set

    var directionsEventId: String? by mutableStateOf(null)
        private set

    var pendingHostAttendeeAction: PendingHostAttendeeAction? by mutableStateOf(null)
        private set

    val activePrimaryDestination: YardScapePrimaryDestination
        get() = route.primaryDestination

    fun navigateTo(destination: YardScapePrimaryDestination) {
        route = when (destination) {
            YardScapePrimaryDestination.Browse -> YardScapeRoute.Browse
            YardScapePrimaryDestination.Saved -> YardScapeRoute.Saved
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
            YardScapeRoute.Saved,
            YardScapeRoute.MyRsvps,
            YardScapeRoute.Host,
            YardScapeRoute.Account,
            -> if (current == YardScapeRoute.MyRsvps) YardScapeRoute.Saved else YardScapeRoute.Browse
            is YardScapeRoute.EventDetail -> when (current.origin) {
                YardScapePrimaryDestination.Saved -> YardScapeRoute.Saved
                else -> YardScapeRoute.Browse
            }
            is YardScapeRoute.Rsvp -> YardScapeRoute.EventDetail(current.eventId, current.origin)
            is YardScapeRoute.HostCreateEdit -> YardScapeRoute.Host
            is YardScapeRoute.HostAttendees -> YardScapeRoute.Host
        }
        return true
    }

    fun browseItems(): List<BrowseEventItem> =
        repository.publicPreviews(nowEpochMillis).map { it.toBrowseEventItem(nowEpochMillis) }

    fun discoveryState(): ShopperDiscoveryState {
        val allItems = browseItems()
        return ShopperDiscoveryState(
            items = allItems.filter { it.matches(discoveryFilters, nowEpochMillis) },
            totalEventCount = allItems.size,
            availableCategories = allItems.flatMap { it.categoryLabels }.distinct().sorted(),
            filters = discoveryFilters,
            displayMode = discoveryDisplayMode,
            savedEventIds = savedEventIds,
        )
    }

    fun updateDiscoveryQuery(query: String) {
        discoveryFilters = discoveryFilters.copy(query = query)
    }

    fun updateDiscoveryDate(date: DiscoveryDateFilter) {
        discoveryFilters = discoveryFilters.copy(date = date)
    }

    fun updateDiscoveryDistance(distance: DiscoveryDistanceFilter) {
        discoveryFilters = discoveryFilters.copy(distance = distance)
    }

    fun toggleDiscoveryCategory(category: String) {
        val categories = discoveryFilters.categories.toMutableSet()
        if (!categories.add(category)) categories.remove(category)
        discoveryFilters = discoveryFilters.copy(categories = categories)
    }

    fun clearDiscoveryFilters() {
        discoveryFilters = DiscoveryFilters()
    }

    fun updateDiscoveryDisplayMode(mode: DiscoveryDisplayMode) {
        discoveryDisplayMode = mode
    }

    fun toggleSavedEvent(eventId: String): Boolean {
        if (browseItems().none { it.id == eventId }) return false
        savedEventIds = if (eventId in savedEventIds) savedEventIds - eventId else savedEventIds + eventId
        return eventId in savedEventIds
    }

    fun savedItems(): List<BrowseEventItem> =
        browseItems().filter { it.id in savedEventIds }

    fun openMyRsvps() {
        route = YardScapeRoute.MyRsvps
    }

    fun myRsvpItems(): List<ShopperRsvpItem> =
        repository.rsvpsForShopper(shopperId).mapNotNull { rsvp ->
            val detail = repository.publicEventDetail(rsvp.eventId) ?: return@mapNotNull null
            val exactAddress = repository.exactLocationFor(rsvp.eventId, shopperId, nowEpochMillis)
            val uiState = rsvp.toShopperUiState(detail.status, detail.saleWindow.hasEnded(nowEpochMillis))
            ShopperRsvpItem(
                eventId = detail.id,
                title = detail.title,
                dateLabel = detail.saleWindow.toBrowseDateLabel(nowEpochMillis),
                approximateLocationLabel = listOfNotNull(
                    detail.publicLocation.neighborhood,
                    detail.publicLocation.distanceLabel ?: detail.publicLocation.areaDescription,
                ).joinToString(" - "),
                state = uiState,
                group = uiState.toGroup(),
                exactAddress = exactAddress.takeIf { uiState == ShopperRsvpUiState.Accepted },
                reminderAdded = rsvp.eventId in reminderEventIds,
                calendarExportPrepared = rsvp.eventId in calendarExportEventIds,
            )
        }.sortedWith(compareBy({ it.group.ordinal }, { it.dateLabel }))

    fun requestRsvpCancellation(eventId: String): Boolean {
        val item = myRsvpItems().firstOrNull { it.eventId == eventId } ?: return false
        if (!item.canCancel) return false
        pendingRsvpCancellationEventId = eventId
        return true
    }

    fun dismissRsvpCancellation() {
        pendingRsvpCancellationEventId = null
    }

    fun confirmRsvpCancellation(): Boolean {
        val eventId = pendingRsvpCancellationEventId ?: return false
        pendingRsvpCancellationEventId = null
        val cancelled = repository.cancelRsvp(eventId, shopperId) ?: return false
        directionsEventId = null
        return cancelled.status == RsvpStatus.CANCELLED
    }

    fun addMockReminder(eventId: String): Boolean {
        val item = myRsvpItems().firstOrNull { it.eventId == eventId } ?: return false
        if (!item.canAddReminder) return false
        reminderEventIds = reminderEventIds + eventId
        return true
    }

    fun prepareMockCalendarExport(eventId: String): Boolean {
        val item = myRsvpItems().firstOrNull { it.eventId == eventId } ?: return false
        if (!item.canExportCalendar) return false
        calendarExportEventIds = calendarExportEventIds + eventId
        return true
    }

    fun requestDirections(eventId: String): ExactAddress? {
        val item = myRsvpItems().firstOrNull { it.eventId == eventId } ?: return null
        val exactAddress = item.exactAddress.takeIf { item.canOpenDirections } ?: return null
        directionsEventId = eventId
        return exactAddress
    }

    fun revokeRsvpAccess(eventId: String): Boolean {
        val updated = repository.revokeRsvpAccess(eventId, shopperId) ?: return false
        directionsEventId = null
        return updated.locationVisibility == LocationVisibility.REVOKED
    }

    fun blockHostForEvent(eventId: String): Boolean = revokeRsvpAccess(eventId)

    fun expireRsvpAccess(eventId: String): Boolean {
        val updated = repository.expireRsvpAccess(eventId, shopperId) ?: return false
        directionsEventId = null
        return updated.locationVisibility == LocationVisibility.EXPIRED
    }

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
        val origin = activePrimaryDestination.takeIf {
            it == YardScapePrimaryDestination.Browse || it == YardScapePrimaryDestination.Saved
        } ?: YardScapePrimaryDestination.Browse
        route = YardScapeRoute.EventDetail(eventId, origin)
    }

    fun openRsvp(eventId: String) {
        val origin = (route as? YardScapeRoute.EventDetail)
            ?.takeIf { it.eventId == eventId }
            ?.origin
            ?: YardScapePrimaryDestination.Browse
        route = YardScapeRoute.Rsvp(eventId, origin)
    }

    fun confirmRsvp(eventId: String) {
        repository.submitRsvp(eventId, shopperId)
        val origin = (route as? YardScapeRoute.Rsvp)
            ?.takeIf { it.eventId == eventId }
            ?.origin
            ?: YardScapePrimaryDestination.Browse
        route = YardScapeRoute.EventDetail(eventId, origin)
    }

    fun openHostCreateEdit(eventId: String? = null) {
        if (eventId == null) hostEditorSessions.remove(NEW_HOST_SESSION_KEY)
        route = YardScapeRoute.HostCreateEdit(eventId)
    }

    fun openHostAttendees(eventId: String): Boolean {
        val event = repository.hostEvent(eventId) ?: return false
        if (event.host.id != hostId) return false
        route = YardScapeRoute.HostAttendees(eventId)
        return true
    }

    fun updateHostAttendancePolicy(eventId: String, policy: HostAttendancePolicy): Boolean {
        val event = repository.hostEvent(eventId) ?: return false
        if (event.host.id != hostId) return false
        if (policy.attendeeCap != null && policy.attendeeCap < 1) return false
        hostAttendancePolicies[eventId] = policy
        return true
    }

    fun hostAttendanceState(eventId: String): HostAttendanceState? {
        val event = repository.hostEvent(eventId) ?: return null
        if (event.host.id != hostId) return null
        val attendees = repository.rsvpsForEvent(eventId).map { rsvp ->
            val uiState = rsvp.toHostAttendeeUiState(event.status, event.saleWindow.hasEnded(nowEpochMillis))
            HostAttendeeItem(
                shopperId = rsvp.shopperId,
                displayName = rsvp.shopperId.toMockAttendeeDisplayName(),
                state = uiState,
                hasLocationAccess = uiState == HostAttendeeUiState.Accepted &&
                    repository.exactLocationFor(eventId, rsvp.shopperId, nowEpochMillis) != null,
            )
        }.sortedWith(compareBy({ it.state.ordinal }, { it.displayName }))
        return HostAttendanceState(
            eventId = eventId,
            eventTitle = event.title,
            policy = hostAttendancePolicies[eventId] ?: HostAttendancePolicy(),
            attendees = attendees,
        )
    }

    fun requestHostAttendeeAction(
        eventId: String,
        shopperId: String,
        action: HostAttendeeAction,
    ): Boolean {
        val state = hostAttendanceState(eventId) ?: return false
        val attendee = state.attendees.firstOrNull { it.shopperId == shopperId } ?: return false
        if (action !in attendee.availableActions) return false
        if (action == HostAttendeeAction.Accept && state.isAtCapacity) return false
        pendingHostAttendeeAction = PendingHostAttendeeAction(eventId, shopperId, attendee.displayName, action)
        return true
    }

    fun dismissHostAttendeeAction() {
        pendingHostAttendeeAction = null
    }

    fun confirmHostAttendeeAction(): Boolean {
        val pending = pendingHostAttendeeAction ?: return false
        pendingHostAttendeeAction = null
        val updated = when (pending.action) {
            HostAttendeeAction.Accept -> {
                if (hostAttendanceState(pending.eventId)?.isAtCapacity == true) return false
                repository.acceptRsvp(pending.eventId, pending.shopperId)
            }
            HostAttendeeAction.Decline -> repository.declineRsvp(pending.eventId, pending.shopperId)
            HostAttendeeAction.Remove -> repository.removeRsvp(pending.eventId, pending.shopperId)
            HostAttendeeAction.Revoke -> repository.revokeRsvpAccess(pending.eventId, pending.shopperId)
        }
        directionsEventId = null
        return updated != null
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

    fun availableHostPhotos() = hostPhotoPicker.availablePhotos()

    fun hostEditorState(eventId: String?): HostEditorState {
        val sessionKey = eventId ?: NEW_HOST_SESSION_KEY
        return hostEditorSessions.getOrPut(sessionKey) {
            HostEditorState(
                draft = repository.hostEvent(eventId.orEmpty())?.toHostEventDraft()
                    ?: blankHostEventDraft(),
                validationErrors = emptyList(),
            )
        }
    }

    fun rememberHostEditorState(state: HostEditorState): HostEditorState {
        hostEditorSessions[state.savedEventId ?: state.draft.id ?: NEW_HOST_SESSION_KEY] = state
        return state
    }

    fun moveHostEditor(state: HostEditorState, target: HostEditorStep): HostEditorState {
        val movingForward = target.ordinal > state.step.ordinal
        val firstInvalidStep = if (movingForward) {
            HostEditorStep.entries
                .filter { it.ordinal >= state.step.ordinal && it.ordinal < target.ordinal }
                .firstOrNull { state.errorsFor(it).isNotEmpty() }
        } else {
            null
        }
        val errors = firstInvalidStep?.let(state::errorsFor).orEmpty()
        return rememberHostEditorState(
            state.copy(
                step = firstInvalidStep ?: target,
                validationErrors = errors,
                pendingConfirmation = null,
            ),
        )
    }

    fun saveHostDraft(state: HostEditorState): HostEditorState =
        rememberHostEditorState(hostStateFrom(state, repository.saveHostEvent(state.draft, EventStatus.DRAFT)))

    fun saveHostDraft(draft: HostEventDraft): HostEditorState =
        saveHostDraft(HostEditorState(draft = draft, validationErrors = emptyList()))

    fun publishHostEvent(state: HostEditorState): HostEditorState {
        val errors = state.publishErrors()
        if (errors.isNotEmpty()) return rememberHostEditorState(state.copy(validationErrors = errors, pendingConfirmation = null))
        val published = rememberHostEditorState(
            hostStateFrom(state, repository.saveHostEvent(state.draft, EventStatus.PUBLISHED))
                .copy(step = HostEditorStep.Preview, pendingConfirmation = null),
        )
        published.savedEventId?.let { eventId ->
            hostAttendancePolicies[eventId] = HostAttendancePolicy(state.attendeeCap, state.approvalMode)
        }
        return published
    }

    fun publishHostEvent(draft: HostEventDraft): HostEditorState =
        publishHostEvent(HostEditorState(draft = draft, validationErrors = emptyList()))

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

    private fun hostStateFrom(originalState: HostEditorState, result: HostEventSaveResult): HostEditorState {
        val draft = result.event?.toHostEventDraft() ?: originalState.draft
        return originalState.copy(
            draft = draft,
            validationErrors = result.validationErrors,
            savedEventId = result.event?.id,
        )
    }
}

data class BrowseEventItem(
    val id: String,
    val title: String,
    val description: String,
    val dateLabel: String,
    val locationLabel: String,
    val neighborhood: String,
    val city: String,
    val startsAtEpochMillis: Long,
    val distanceMiles: Int?,
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
        description = description,
        dateLabel = saleWindow.toBrowseDateLabel(nowEpochMillis),
        locationLabel = listOfNotNull(
            publicLocation.neighborhood,
            publicLocation.distanceLabel ?: publicLocation.areaDescription,
        ).joinToString(" - "),
        neighborhood = publicLocation.neighborhood,
        city = publicLocation.city,
        startsAtEpochMillis = saleWindow.startsAtEpochMillis,
        distanceMiles = publicLocation.distanceLabel?.substringBefore(' ')?.toIntOrNull(),
        categoryLabels = categories,
        statusLabel = status.name.lowercase().replaceFirstChar { it.uppercase() },
        photoDescription = photos.firstOrNull()?.description,
    )

private fun BrowseEventItem.matches(filters: DiscoveryFilters, nowEpochMillis: Long): Boolean {
    val normalizedQuery = filters.query.trim().lowercase()
    val searchableText = listOf(
        title,
        description,
        neighborhood,
        city,
        categoryLabels.joinToString(" "),
    ).joinToString(" ").lowercase()
    val dayOffset = (startsAtEpochMillis - nowEpochMillis).floorDiv(MILLIS_PER_DAY)
    val matchesDate = when (filters.date) {
        DiscoveryDateFilter.Any -> true
        DiscoveryDateFilter.Today -> dayOffset == 0L
        DiscoveryDateFilter.Tomorrow -> dayOffset == 1L
        DiscoveryDateFilter.Weekend -> startsAtEpochMillis.isWeekendDay()
    }
    val matchesDistance = filters.distance.maximumMiles?.let { maximum ->
        distanceMiles?.let { it <= maximum } ?: false
    } ?: true
    val matchesCategory = filters.categories.isEmpty() || categoryLabels.any { it in filters.categories }
    return (normalizedQuery.isBlank() || normalizedQuery in searchableText) &&
        matchesDate && matchesDistance && matchesCategory
}

private fun Long.isWeekendDay(): Boolean {
    val daysSinceEpoch = floorDiv(MILLIS_PER_DAY)
    val isoDayOfWeek = (daysSinceEpoch + 3L).floorMod(7L) + 1L
    return isoDayOfWeek == 6L || isoDayOfWeek == 7L
}

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
        photos = photos,
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

private fun Rsvp.toShopperUiState(
    eventStatus: EventStatus,
    eventHasEnded: Boolean,
): ShopperRsvpUiState = when {
    eventStatus == EventStatus.CANCELLED || eventStatus == EventStatus.COMPLETED ->
        ShopperRsvpUiState.Cancelled
    locationVisibility == LocationVisibility.REVOKED -> ShopperRsvpUiState.Revoked
    locationVisibility == LocationVisibility.EXPIRED || eventHasEnded -> ShopperRsvpUiState.Expired
    status == RsvpStatus.REQUESTED -> ShopperRsvpUiState.Requested
    status == RsvpStatus.ACCEPTED -> ShopperRsvpUiState.Accepted
    status == RsvpStatus.FULL -> ShopperRsvpUiState.Full
    status == RsvpStatus.WAITLISTED -> ShopperRsvpUiState.Waitlisted
    status == RsvpStatus.DECLINED -> ShopperRsvpUiState.Declined
    else -> ShopperRsvpUiState.Cancelled
}

private fun ShopperRsvpUiState.toGroup(): RsvpGroup = when (this) {
    ShopperRsvpUiState.Requested,
    ShopperRsvpUiState.Full,
    ShopperRsvpUiState.Waitlisted,
    -> RsvpGroup.ActionNeeded
    ShopperRsvpUiState.Accepted -> RsvpGroup.Upcoming
    ShopperRsvpUiState.Declined,
    ShopperRsvpUiState.Cancelled,
    ShopperRsvpUiState.Revoked,
    ShopperRsvpUiState.Expired,
    -> RsvpGroup.History
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
private const val NEW_HOST_SESSION_KEY = "new-host-event"

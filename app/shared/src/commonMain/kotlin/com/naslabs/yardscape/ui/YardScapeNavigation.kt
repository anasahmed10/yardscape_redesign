package com.naslabs.yardscape.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.naslabs.yardscape.data.PublicEventDetail
import com.naslabs.yardscape.data.BlockedHostUpdate
import com.naslabs.yardscape.data.SeededYardSaleData
import com.naslabs.yardscape.data.SeededYardSaleEventRepository
import com.naslabs.yardscape.data.YardSaleEventRepository
import com.naslabs.yardscape.data.HostEventDraft
import com.naslabs.yardscape.data.HostEventSaveResult
import com.naslabs.yardscape.data.HostPhotoPicker
import com.naslabs.yardscape.data.MapLocationSearchRepository
import com.naslabs.yardscape.data.MapSelectedLocation
import com.naslabs.yardscape.data.MarketplaceMessagingAccessSource
import com.naslabs.yardscape.data.MarketplaceMessagingRepository
import com.naslabs.yardscape.data.MessagingRepositoryResult
import com.naslabs.yardscape.data.SeededMapLocationSearchRepository
import com.naslabs.yardscape.data.SeededHostPhotoPicker
import com.naslabs.yardscape.data.SeededMarketplaceMessagingRepository
import com.naslabs.yardscape.data.SeededShopperSafetyRepository
import com.naslabs.yardscape.data.SafetyRepositoryResult
import com.naslabs.yardscape.data.ShopperSafetyRepository
import com.naslabs.yardscape.domain.EventStatus
import com.naslabs.yardscape.domain.ExactAddress
import com.naslabs.yardscape.domain.LocationVisibility
import com.naslabs.yardscape.domain.MapViewport
import com.naslabs.yardscape.domain.MarketplaceConversationKey
import com.naslabs.yardscape.domain.MarketplaceMessagingPolicy
import com.naslabs.yardscape.domain.MessagingAccessContext
import com.naslabs.yardscape.domain.MessagingActor
import com.naslabs.yardscape.domain.MessagingClosedReason
import com.naslabs.yardscape.domain.MessagingComposerAccess
import com.naslabs.yardscape.domain.PublicEventPreview
import com.naslabs.yardscape.domain.PublicEventMarker
import com.naslabs.yardscape.domain.Rsvp
import com.naslabs.yardscape.domain.RsvpEligibilityPolicy
import com.naslabs.yardscape.domain.RsvpEligibilityStatus
import com.naslabs.yardscape.domain.RsvpStatus
import com.naslabs.yardscape.domain.ReportReason
import com.naslabs.yardscape.domain.SafetyReportDraft
import com.naslabs.yardscape.domain.UserRole
import com.naslabs.yardscape.domain.YardSaleEvent
import com.naslabs.yardscape.domain.toPublicEventMarker

enum class YardScapePrimaryDestination(
    val label: String,
    val contextLabel: String,
) {
    Browse(label = "Browse", contextLabel = "Shopper workspace"),
    MyFinds(label = "My Finds", contextLabel = "Shopper workspace"),
    Host(label = "Host", contextLabel = "Host workspace"),
    Messages(label = "Messages", contextLabel = "Shopper workspace"),
    Account(label = "Account", contextLabel = "Account workspace"),
}

enum class ShopperSafetyAction(val pathSegment: String, val label: String) {
    Report(pathSegment = "report", label = "Report sale"),
    Block(pathSegment = "block", label = "Block host"),
}

class MarketplaceConversationId private constructor(val value: String) {
    override fun equals(other: Any?): Boolean = other is MarketplaceConversationId && value == other.value
    override fun hashCode(): Int = value.hashCode()
    override fun toString(): String = "MarketplaceConversationId(redacted)"

    companion object {
        private val canonicalPattern = Regex("conversation-[0-9a-f]{8,64}")

        fun parse(value: String): MarketplaceConversationId? =
            value.takeIf(canonicalPattern::matches)?.let(::MarketplaceConversationId)

        fun require(value: String): MarketplaceConversationId =
            requireNotNull(parse(value)) { "Conversation ID must be a canonical opaque identifier." }
    }
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

    data class MyFinds(val section: MyFindsSection = MyFindsSection.Saved) : YardScapeRoute {
        override val path: String = when (section) {
            MyFindsSection.Saved -> "/finds"
            MyFindsSection.Rsvps -> "/finds/rsvps"
        }
        override val destinationLabel: String = "My Finds"
        override val primaryDestination: YardScapePrimaryDestination = YardScapePrimaryDestination.MyFinds
    }

    data object Host : YardScapeRoute {
        override val path: String = "/host"
        override val destinationLabel: String = "Host tools"
        override val primaryDestination: YardScapePrimaryDestination = YardScapePrimaryDestination.Host
    }

    data object Messages : YardScapeRoute {
        override val path: String = "/messages"
        override val destinationLabel: String = "Messages"
        override val primaryDestination: YardScapePrimaryDestination = YardScapePrimaryDestination.Messages
    }

    data class MessageThread(val conversationId: MarketplaceConversationId) : YardScapeRoute {
        constructor(conversationId: String) : this(MarketplaceConversationId.require(conversationId))

        override val path: String = "/messages/${conversationId.value}"
        override val destinationLabel: String = "Message thread"
        override val primaryDestination: YardScapePrimaryDestination = YardScapePrimaryDestination.Messages
    }

    data object Account : YardScapeRoute {
        override val path: String = "/account"
        override val destinationLabel: String = "Account"
        override val primaryDestination: YardScapePrimaryDestination = YardScapePrimaryDestination.Account
    }

    data class EventDetail(
        val eventId: String,
        val origin: YardScapePrimaryDestination = YardScapePrimaryDestination.Browse,
        val myFindsSection: MyFindsSection = MyFindsSection.Saved,
    ) : YardScapeRoute {
        init {
            require(origin == YardScapePrimaryDestination.Browse || origin == YardScapePrimaryDestination.MyFinds)
        }

        override val path: String = "/events/$eventId"
        override val destinationLabel: String = "Event details"
        override val primaryDestination: YardScapePrimaryDestination = origin
    }

    data class Rsvp(
        val eventId: String,
        val origin: YardScapePrimaryDestination = YardScapePrimaryDestination.Browse,
        val myFindsSection: MyFindsSection = MyFindsSection.Saved,
    ) : YardScapeRoute {
        init {
            require(origin == YardScapePrimaryDestination.Browse || origin == YardScapePrimaryDestination.MyFinds)
        }

        override val path: String = "/events/$eventId/rsvp"
        override val destinationLabel: String = "RSVP"
        override val primaryDestination: YardScapePrimaryDestination = origin
    }

    data class EventSafety(
        val eventId: String,
        val action: ShopperSafetyAction,
        val origin: YardScapePrimaryDestination = YardScapePrimaryDestination.Browse,
        val myFindsSection: MyFindsSection = MyFindsSection.Saved,
        val messageThreadId: MarketplaceConversationId? = null,
    ) : YardScapeRoute {
        init {
            require(
                origin == YardScapePrimaryDestination.Browse ||
                    origin == YardScapePrimaryDestination.MyFinds ||
                    origin == YardScapePrimaryDestination.Messages,
            )
            require(origin != YardScapePrimaryDestination.Messages || messageThreadId != null)
        }

        override val path: String = "/events/$eventId/safety/${action.pathSegment}"
        override val destinationLabel: String = action.label
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
            if (path.trim().trimStart('/').startsWith("messages/") && ('?' in path || '#' in path)) return null
            val segments = path.substringBefore('?').substringBefore('#')
                .trim()
                .trim('/')
                .split('/')
                .filter { it.isNotBlank() }
            return when {
                segments == listOf("browse") || segments.isEmpty() -> Browse
                segments == listOf("finds") -> MyFinds()
                segments == listOf("finds", "rsvps") -> MyFinds(MyFindsSection.Rsvps)
                segments == listOf("saved") -> MyFinds(MyFindsSection.Saved)
                segments == listOf("rsvps") -> MyFinds(MyFindsSection.Rsvps)
                segments == listOf("host") -> Host
                segments == listOf("messages") -> Messages
                segments.size == 2 && segments[0] == "messages" ->
                    MarketplaceConversationId.parse(segments[1])?.let(::MessageThread)
                segments == listOf("account") -> Account
                segments.size == 2 && segments[0] == "events" -> EventDetail(segments[1])
                segments.size == 3 && segments[0] == "events" && segments[2] == "rsvp" -> Rsvp(segments[1])
                segments.size == 4 && segments[0] == "events" && segments[2] == "safety" ->
                    ShopperSafetyAction.entries.firstOrNull { it.pathSegment == segments[3] }
                        ?.let { EventSafety(segments[1], it) }
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
    const val EventDetailScreen: String = "event-detail-screen"
    const val RsvpScreen: String = "rsvp-screen"
    const val LocationAccessPanel: String = "event-detail-location-access"
    const val ExactLocationContent: String = "event-detail-exact-location"
    const val DirectionsAction: String = "event-detail-directions-action"
    const val RsvpAction: String = "event-detail-rsvp-action"
    const val RsvpConfirmAction: String = "rsvp-confirm-action"
    const val EventDetailHero: String = "event-detail-hero-artwork"
    const val EventDetailPublicStatus: String = "event-detail-public-status"
    const val ProtectedLocationCard: String = "protected-location-card"
    const val RsvpProtectedLocationCard: String = "rsvp-protected-location-card"
    const val ShopperSafetyScreen: String = "shopper-safety-screen"
    const val ShopperSafetyActions: String = "shopper-safety-actions"
    const val MyFindsEventCardPrefix: String = "my-finds-event-card-"
    const val AppShell: String = "app-shell"
    const val EditorialHeader: String = "editorial-header"
    const val EditorialHeaderTitle: String = "editorial-header-title"
    const val EditorialBackNavigation: String = "editorial-back-navigation"
    const val AccountIntro: String = "account-intro"
    const val MessagesIntro: String = "messages-intro"
    const val DiscoveryNoResults: String = "discovery-no-results"
    const val SavedScreen: String = "saved-screen"
    const val MyFindsScreen: String = "my-finds-screen"
    const val MessagesScreen: String = "messages-screen"
    const val AccountScreen: String = "account-screen"
    const val DiscoveryMap: String = "discovery-map"
    const val DiscoveryResultsSheet: String = "discovery-results-sheet"

    fun browseEventCard(eventId: String): String = "browse-event-card-$eventId"
    fun mapResult(eventId: String): String = "discovery-map-result-$eventId"
    fun myFindsEventCard(eventId: String): String = "$MyFindsEventCardPrefix$eventId"
    fun primaryDestination(destination: YardScapePrimaryDestination): String =
        "primary-destination-${destination.name.lowercase()}"

    fun editorialSegment(value: String): String = "editorial-segment-$value"
}

private data class RsvpRouteContext(
    val origin: YardScapePrimaryDestination,
    val myFindsSection: MyFindsSection,
) {
    fun detailRoute(eventId: String): YardScapeRoute.EventDetail =
        YardScapeRoute.EventDetail(eventId, origin, myFindsSection)

    fun rsvpRoute(eventId: String): YardScapeRoute.Rsvp =
        YardScapeRoute.Rsvp(eventId, origin, myFindsSection)
}

class YardScapeAppState(
    private val repository: YardSaleEventRepository = SeededYardSaleEventRepository(),
    private val mapLocationSearchRepository: MapLocationSearchRepository = SeededMapLocationSearchRepository(),
    private val hostPhotoPicker: HostPhotoPicker = SeededHostPhotoPicker(),
    private val accountSessionController: AccountSessionController = SeededAccountSessionController(),
    private val shopperSafetyRepository: ShopperSafetyRepository = SeededShopperSafetyRepository(),
    private val publicMapAreaSource: PublicMapAreaSource = SeededPublicMapAreaSource,
    messagingRepository: MarketplaceMessagingRepository? = null,
    messagingRepositoryFactory: ((MarketplaceMessagingAccessSource) -> MarketplaceMessagingRepository)? = null,
    initialAccountStatus: MockSessionStatus = MockSessionStatus.SignedIn,
    val nowEpochMillis: Long = SeededYardSaleData.BASE_NOW_EPOCH_MILLIS,
    private val shopperId: String = SeededYardSaleData.SHOPPER_WITHOUT_ACCESS_ID,
    private val hostId: String = SeededYardSaleData.HOST_AVERY_ID,
    activeUserRole: UserRole = UserRole.SHOPPER,
    dataAvailability: AppDataAvailability = AppDataAvailability.Available,
    private val eventCapacitySource: EventCapacitySource = EventCapacitySource.None,
    initialBlockedEventIds: Set<String> = emptySet(),
    initialRoute: YardScapeRoute = YardScapeRoute.Browse,
) {
    var activeUserRole: UserRole by mutableStateOf(activeUserRole)
        private set

    private val hostEditorSessions = mutableMapOf<String, HostEditorState>()
    var hostEditorSessionSignal: Long by mutableStateOf(0L)
        private set
    private val hostAttendancePolicies = mutableMapOf(
        SeededYardSaleData.FAMILY_GARAGE_EVENT_ID to HostAttendancePolicy(
            attendeeCap = 2,
            approvalMode = HostRsvpApprovalMode.ManualReview,
        ),
    )
    var route: YardScapeRoute by mutableStateOf(
        if (initialRoute is YardScapeRoute.MessageThread) YardScapeRoute.Messages else initialRoute,
    )
        private set

    var accountState: MockAccountState by mutableStateOf(
        accountSessionController.stateFor(initialAccountStatus, activeUserRole),
    )
        private set

    var pendingProtectedAction: PendingProtectedAction? by mutableStateOf(null)
        private set

    private var pendingMessageThreadAuthorization: MarketplaceConversationId? by mutableStateOf(
        (initialRoute as? YardScapeRoute.MessageThread)?.conversationId,
    )
    var pendingMessageThreadAuthorizationSignal: Long by mutableStateOf(
        if (initialRoute is YardScapeRoute.MessageThread) 1L else 0L,
    )
        private set
    val hasPendingMessageThreadAuthorization: Boolean
        get() = pendingMessageThreadAuthorization != null
    val currentMessagingActor: MessagingActor
        get() = messagingActor()
    private var messagingSessionVersion: Long = 0L
    private var messagingNavigationVersion: Long = 0L
    private var locationAccessRevision: Long by mutableStateOf(0L)

    var shopperSafetyState: ShopperSafetyUiState? by mutableStateOf(null)
        private set

    var blockedEventIds: Set<String> by mutableStateOf(initialBlockedEventIds)
        private set

    var discoveryFilters: DiscoveryFilters by mutableStateOf(DiscoveryFilters())
        private set

    var discoveryDisplayMode: DiscoveryDisplayMode by mutableStateOf(DiscoveryDisplayMode.Map)
        private set

    var dataAvailability: AppDataAvailability by mutableStateOf(dataAvailability)
        private set

    var mapDiscoveryState: MapDiscoveryState by mutableStateOf(
        MapDiscoveryState(
            mapAvailability = when (dataAvailability) {
                AppDataAvailability.Loading,
                AppDataAvailability.Available -> MapAvailability.Loading
                AppDataAvailability.Offline -> MapAvailability.Offline
                is AppDataAvailability.RecoverableError -> MapAvailability.Failed(dataAvailability.message)
            },
        ),
    )
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

    private val messagingAccessSource = object : MarketplaceMessagingAccessSource {
        override fun accessContextFor(key: MarketplaceConversationKey): MessagingAccessContext? =
            messagingAccessContextFor(key)
    }
    private val marketplaceMessagingRepository = messagingRepository
        ?: messagingRepositoryFactory?.invoke(messagingAccessSource)
        ?: SeededMarketplaceMessagingRepository(messagingAccessSource)
    private val marketplaceMessagingState = MarketplaceMessagingState(
        repository = marketplaceMessagingRepository,
        actorSource = ::messagingActor,
        composerAccessFor = ::currentMessagingComposerAccess,
        sessionVersionSource = { messagingSessionVersion },
    )

    val messagingInboxState: MessagingInboxUiState
        get() = marketplaceMessagingState.inboxState

    val messagingThreadState: MessagingThreadUiState
        get() = marketplaceMessagingState.threadState

    init {
        synchronizeDiscoveryMapMarkers()
        (initialRoute as? YardScapeRoute.EventSafety)?.let { safetyRoute ->
            openShopperSafety(
                safetyRoute.eventId,
                safetyRoute.action,
                safetyRoute.origin,
                safetyRoute.messageThreadId,
            )
        }
        (initialRoute as? YardScapeRoute.MessageThread)?.let(::openMessageThreadRoute)
        if (initialRoute == YardScapeRoute.Messages) openMessagesRoute()
    }

    val activePrimaryDestination: YardScapePrimaryDestination
        get() = route.primaryDestination

    fun navigateTo(destination: YardScapePrimaryDestination) {
        invalidateMessagingNavigation()
        if (destination != YardScapePrimaryDestination.Messages) pendingMessageThreadAuthorization = null
        route = when (destination) {
            YardScapePrimaryDestination.Browse -> YardScapeRoute.Browse
            YardScapePrimaryDestination.MyFinds -> YardScapeRoute.MyFinds(
                when (val currentRoute = route) {
                    is YardScapeRoute.MyFinds -> currentRoute.section
                    is YardScapeRoute.EventDetail -> currentRoute.myFindsSection
                    is YardScapeRoute.Rsvp -> currentRoute.myFindsSection
                    is YardScapeRoute.EventSafety -> currentRoute.myFindsSection
                    else -> MyFindsSection.Saved
                },
            )
            YardScapePrimaryDestination.Host -> YardScapeRoute.Host
            YardScapePrimaryDestination.Messages -> {
                openMessagesRoute()
                route
            }
            YardScapePrimaryDestination.Account -> YardScapeRoute.Account
        }
    }

    fun protectedActionDecision(action: ProtectedAction): ProtectedActionDecision =
        accountSessionController.gate(accountState, action)

    fun signInMock(role: UserRole) {
        val pending = pendingProtectedAction
        marketplaceMessagingState.resetForNewSession()
        messagingSessionVersion++
        activeUserRole = role
        accountState = accountSessionController.signIn(role, accountState.notificationPreferences)
        pendingProtectedAction = null
        val pendingRoute = pending?.resumeRoute
        if (pendingRoute is YardScapeRoute.MessageThread) {
            queuePendingMessageThreadAuthorization(pendingRoute.conversationId)
            route = YardScapeRoute.Messages
        } else {
            route = pendingRoute ?: YardScapeRoute.Account
        }
    }

    fun signOutMock() {
        messagingSessionVersion++
        accountState = accountSessionController.stateFor(MockSessionStatus.SignedOut, activeUserRole)
        clearProtectedSessionState()
        route = YardScapeRoute.Account
    }

    fun expireMockSession() {
        messagingSessionVersion++
        accountState = accountSessionController.stateFor(MockSessionStatus.Expired, activeUserRole)
        clearProtectedSessionState()
        route = YardScapeRoute.Account
    }

    fun viewMockProfile(role: UserRole) {
        accountState = accountState.copy(viewedProfile = accountSessionController.profileFor(role))
    }

    fun openAccountSettings(section: AccountSettingsSection) {
        accountState = accountState.copy(selectedSettingsSection = section)
    }

    fun updateNotificationPreferences(preferences: NotificationPreferences) {
        accountState = accountState.copy(notificationPreferences = preferences)
    }

    private fun clearProtectedSessionState() {
        directionsEventId = null
        pendingRsvpCancellationEventId = null
        pendingHostAttendeeAction = null
        pendingProtectedAction = null
        pendingMessageThreadAuthorization = null
        shopperSafetyState = null
        marketplaceMessagingState.closeAndClear(MessagingClosedReason.CONVERSATION_UNAVAILABLE)
    }

    fun navigateToPath(path: String): Boolean {
        val target = YardScapeRoute.fromPath(path) ?: return false
        invalidateMessagingNavigation()
        if (target !is YardScapeRoute.MessageThread) pendingMessageThreadAuthorization = null
        when (target) {
            is YardScapeRoute.MyFinds -> openMyFinds(target.section)
            is YardScapeRoute.Rsvp -> openRsvp(target.eventId)
            is YardScapeRoute.EventSafety -> openShopperSafety(target.eventId, target.action, target.origin)
            is YardScapeRoute.HostCreateEdit -> openHostCreateEdit(target.eventId)
            is YardScapeRoute.HostAttendees -> openHostAttendees(target.eventId)
            is YardScapeRoute.MessageThread -> openMessageThreadRoute(target)
            YardScapeRoute.Messages -> openMessagesRoute()
            else -> route = target
        }
        return true
    }

    fun navigateBack(): Boolean {
        invalidateMessagingNavigation()
        pendingMessageThreadAuthorization = null
        route = when (val current = route) {
            YardScapeRoute.Browse -> return false
            is YardScapeRoute.MyFinds,
            YardScapeRoute.Host,
            YardScapeRoute.Messages,
            YardScapeRoute.Account,
            -> YardScapeRoute.Browse
            is YardScapeRoute.MessageThread -> YardScapeRoute.Messages
            is YardScapeRoute.EventDetail -> when (current.origin) {
                YardScapePrimaryDestination.MyFinds -> YardScapeRoute.MyFinds(current.myFindsSection)
                else -> YardScapeRoute.Browse
            }
            is YardScapeRoute.Rsvp -> YardScapeRoute.EventDetail(
                current.eventId,
                current.origin,
                current.myFindsSection,
            )
            is YardScapeRoute.EventSafety -> when (current.origin) {
                YardScapePrimaryDestination.Messages ->
                    YardScapeRoute.MessageThread(requireNotNull(current.messageThreadId))
                else -> YardScapeRoute.EventDetail(
                    current.eventId,
                    current.origin,
                    current.myFindsSection,
                )
            }
            is YardScapeRoute.HostCreateEdit -> YardScapeRoute.Host
            is YardScapeRoute.HostAttendees -> YardScapeRoute.Host
        }
        return true
    }

    private fun openMessageThreadRoute(target: YardScapeRoute.MessageThread): Boolean {
        when (val gate = protectedActionDecision(ProtectedAction.Messaging)) {
            ProtectedActionDecision.Allowed -> {
                queuePendingMessageThreadAuthorization(target.conversationId)
                route = YardScapeRoute.Messages
                return false
            }
            is ProtectedActionDecision.SignInRequired -> {
                pendingProtectedAction = PendingProtectedAction(ProtectedAction.Messaging, target)
                accountState = accountState.copy(signInReason = gate.message)
                route = YardScapeRoute.Account
                return false
            }
        }
        return true
    }

    private fun queuePendingMessageThreadAuthorization(conversationId: MarketplaceConversationId) {
        pendingMessageThreadAuthorization = conversationId
        pendingMessageThreadAuthorizationSignal++
    }

    private fun openMessagesRoute(): Boolean {
        return when (val gate = protectedActionDecision(ProtectedAction.Messaging)) {
            ProtectedActionDecision.Allowed -> {
                route = YardScapeRoute.Messages
                true
            }
            is ProtectedActionDecision.SignInRequired -> {
                pendingProtectedAction = PendingProtectedAction(
                    ProtectedAction.Messaging,
                    YardScapeRoute.Messages,
                )
                accountState = accountState.copy(signInReason = gate.message)
                route = YardScapeRoute.Account
                false
            }
        }
    }

    suspend fun loadMessagingInbox(): Boolean {
        val target = YardScapeRoute.Messages
        return when (val gate = protectedActionDecision(ProtectedAction.Messaging)) {
            ProtectedActionDecision.Allowed -> marketplaceMessagingState.loadInbox()
            is ProtectedActionDecision.SignInRequired -> {
                pendingProtectedAction = PendingProtectedAction(ProtectedAction.Messaging, target)
                accountState = accountState.copy(signInReason = gate.message)
                route = YardScapeRoute.Account
                false
            }
        }
    }

    suspend fun openMessageThread(conversationId: String): Boolean {
        val opaqueId = MarketplaceConversationId.parse(conversationId) ?: return false
        val target = YardScapeRoute.MessageThread(opaqueId)
        when (val gate = protectedActionDecision(ProtectedAction.Messaging)) {
            is ProtectedActionDecision.SignInRequired -> {
                pendingProtectedAction = PendingProtectedAction(ProtectedAction.Messaging, target)
                accountState = accountState.copy(signInReason = gate.message)
                route = YardScapeRoute.Account
                return false
            }
            ProtectedActionDecision.Allowed -> Unit
        }
        val origin = route
        val navigationVersion = messagingNavigationVersion
        val opened = marketplaceMessagingState.openThread(opaqueId.value)
        if (route != origin || messagingNavigationVersion != navigationVersion) return false
        route = if (opened) target else YardScapeRoute.Messages
        return opened
    }

    suspend fun resumePendingMessageThread(): Boolean {
        val pending = pendingMessageThreadAuthorization ?: return false
        pendingMessageThreadAuthorization = null
        return openMessageThread(pending.value)
    }

    fun updateMessageDraft(draft: String) {
        marketplaceMessagingState.updateDraft(draft)
    }

    suspend fun markCurrentMessageThreadRead(): Boolean =
        marketplaceMessagingState.markCurrentThreadRead()

    suspend fun sendMessageDraft(sentAtEpochMillis: Long): Boolean =
        marketplaceMessagingState.sendDraft(sentAtEpochMillis)

    suspend fun sendMessageDraft(): Boolean = sendMessageDraft(nowEpochMillis)

    suspend fun retryMessage(messageId: String, attemptedAtEpochMillis: Long): Boolean =
        marketplaceMessagingState.retryMessage(messageId, attemptedAtEpochMillis)

    suspend fun retryMessage(messageId: String): Boolean = retryMessage(messageId, nowEpochMillis)

    fun browseItems(): List<BrowseEventItem> =
        repository.publicPreviews(nowEpochMillis)
            .filterNot { it.id in blockedEventIds }
            .map { it.toBrowseEventItem(nowEpochMillis) }

    fun discoveryState(): ShopperDiscoveryState {
        val allItems = browseItems()
        val viewportEventIds = mapDiscoveryState.searchedViewport?.let { viewport ->
            markersInViewport(
                repository.publicPreviews(nowEpochMillis)
                    .filterNot { it.id in blockedEventIds }
                    .mapNotNull { event ->
                        publicMapAreaSource.areaFor(event)?.let { area -> event.toPublicEventMarker(area) }
                    },
                viewport,
            ).map(PublicEventMarker::eventId).toSet()
        }
        return ShopperDiscoveryState(
            items = allItems.filter { item ->
                item.matches(discoveryFilters, nowEpochMillis) &&
                    (viewportEventIds == null || item.id in viewportEventIds)
            },
            totalEventCount = allItems.size,
            availableCategories = allItems.flatMap { it.categoryLabels }.distinct().sorted(),
            filters = discoveryFilters,
            displayMode = discoveryDisplayMode,
            savedEventIds = savedEventIds,
            hasCommittedMapAreaSearch = mapDiscoveryState.searchedViewport != null,
        )
    }

    private fun synchronizeDiscoveryMapMarkers() {
        val filteredMarkers = repository.publicPreviews(nowEpochMillis)
            .filterNot { it.id in blockedEventIds }
            .filter { it.toBrowseEventItem(nowEpochMillis).matches(discoveryFilters, nowEpochMillis) }
            .mapNotNull { event ->
                publicMapAreaSource.areaFor(event)?.let { area -> event.toPublicEventMarker(area) }
            }
        val markers = mapDiscoveryState.searchedViewport?.let { viewport ->
            markersInViewport(filteredMarkers, viewport)
        } ?: filteredMarkers
        mapDiscoveryState = mapDiscoveryState.synchronizeMarkers(markers)
    }

    fun updateDiscoveryQuery(query: String) {
        discoveryFilters = discoveryFilters.copy(query = query)
        synchronizeDiscoveryMapMarkers()
    }

    fun updateDiscoveryDate(date: DiscoveryDateFilter) {
        discoveryFilters = discoveryFilters.copy(date = date)
        synchronizeDiscoveryMapMarkers()
    }

    fun updateDiscoveryDistance(distance: DiscoveryDistanceFilter) {
        discoveryFilters = discoveryFilters.copy(distance = distance)
        synchronizeDiscoveryMapMarkers()
    }

    fun toggleDiscoveryCategory(category: String) {
        val categories = discoveryFilters.categories.toMutableSet()
        if (!categories.add(category)) categories.remove(category)
        discoveryFilters = discoveryFilters.copy(categories = categories)
        synchronizeDiscoveryMapMarkers()
    }

    fun clearDiscoveryFilters() {
        discoveryFilters = DiscoveryFilters()
        synchronizeDiscoveryMapMarkers()
    }

    fun updateDiscoveryDisplayMode(mode: DiscoveryDisplayMode) {
        discoveryDisplayMode = mode
    }

    fun updateMapCameraViewport(viewport: MapViewport) {
        mapDiscoveryState = mapDiscoveryState.onCameraViewportChanged(viewport)
    }

    fun settleMapCameraViewport() {
        mapDiscoveryState = mapDiscoveryState.onCameraViewportSettled()
    }

    fun searchMapCameraArea() {
        mapDiscoveryState = mapDiscoveryState.searchThisArea()
        synchronizeDiscoveryMapMarkers()
    }

    fun showAllNearbySales() {
        discoveryFilters = DiscoveryFilters()
        mapDiscoveryState = mapDiscoveryState.clearSearchedArea()
        synchronizeDiscoveryMapMarkers()
    }

    fun selectDiscoveryEvent(eventId: String?): Boolean {
        val updated = mapDiscoveryState.selectEvent(eventId)
        mapDiscoveryState = updated
        return eventId == null || updated.selectedEventId == eventId
    }

    fun updateMapResultsSheetPosition(position: MapResultsSheetPosition) {
        mapDiscoveryState = mapDiscoveryState.updateSheetPosition(position)
    }

    fun updateMapAvailability(availability: MapAvailability) {
        mapDiscoveryState = mapDiscoveryState.updateMapAvailability(availability)
    }

    fun retryBrowseData(): Boolean {
        if (dataAvailability !is AppDataAvailability.RecoverableError) return false
        dataAvailability = AppDataAvailability.Available
        mapDiscoveryState = mapDiscoveryState.updateMapAvailability(MapAvailability.Loading)
        synchronizeDiscoveryMapMarkers()
        return true
    }

    fun requestApproximateLocation(): Boolean {
        val previous = mapDiscoveryState
        mapDiscoveryState = mapDiscoveryState.requestApproximateLocation()
        return previous.locationPermission != mapDiscoveryState.locationPermission
    }

    fun updateApproximateLocationPermission(permission: ApproximateLocationPermission) {
        mapDiscoveryState = mapDiscoveryState.updateLocationPermission(permission)
    }

    fun toggleSavedEvent(eventId: String): Boolean {
        if (browseItems().none { it.id == eventId }) return false
        savedEventIds = if (eventId in savedEventIds) savedEventIds - eventId else savedEventIds + eventId
        return eventId in savedEventIds
    }

    fun savedItems(): List<BrowseEventItem> =
        browseItems().filter { it.id in savedEventIds }

    fun myFindsState(section: MyFindsSection): MyFindsState = MyFindsState(
        section = section,
        savedItems = savedItems(),
        rsvpItems = myRsvpItems(),
    )

    fun openMyFinds(section: MyFindsSection = MyFindsSection.Saved) {
        if (section == MyFindsSection.Rsvps) {
            val gate = protectedActionDecision(ProtectedAction.RevealLocation)
            if (gate is ProtectedActionDecision.SignInRequired) {
                pendingProtectedAction = PendingProtectedAction(
                    ProtectedAction.RevealLocation,
                    YardScapeRoute.MyFinds(section),
                )
                accountState = accountState.copy(signInReason = gate.message)
                route = YardScapeRoute.Account
                return
            }
        }
        route = YardScapeRoute.MyFinds(section)
    }

    fun myRsvpItems(): List<ShopperRsvpItem> =
        if (!accountState.isSignedIn) {
            emptyList()
        } else {
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
                    exactAddress = exactAddress.takeIf {
                        uiState == ShopperRsvpUiState.Accepted && rsvp.eventId !in blockedEventIds
                    },
                    reminderAdded = rsvp.eventId in reminderEventIds,
                    calendarExportPrepared = rsvp.eventId in calendarExportEventIds,
                    photoReference = detail.photos.firstOrNull()?.url,
                )
            }.sortedWith(compareBy({ it.group.ordinal }, { it.dateLabel }))
        }

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
        val cancellationCompleted = cancelled.status == RsvpStatus.CANCELLED
        if (cancellationCompleted) {
            exitMatchingRsvp(eventId)
            synchronizeMessagingComposer()
        }
        return cancellationCompleted
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
        val revoked = updated.locationVisibility == LocationVisibility.REVOKED
        if (revoked) {
            locationAccessRevision++
            exitMatchingRsvp(eventId)
            synchronizeMessagingComposer()
        }
        return revoked
    }

    fun blockHostForEvent(eventId: String): Boolean =
        when (val result = shopperSafetyRepository.blockHostForEvent(eventId)) {
            is SafetyRepositoryResult.Success -> {
                applyBlockedHostUpdate(result.value)
                true
            }
            else -> false
        }

    fun expireRsvpAccess(eventId: String): Boolean {
        val updated = repository.expireRsvpAccess(eventId, shopperId) ?: return false
        directionsEventId = null
        val expired = updated.locationVisibility == LocationVisibility.EXPIRED
        if (expired) {
            locationAccessRevision++
            exitMatchingRsvp(eventId)
            synchronizeMessagingComposer()
        }
        return expired
    }

    fun selectedEventDetailState(): EventDetailState? {
        val detailRoute = route as? YardScapeRoute.EventDetail ?: return null
        return detailStateFor(detailRoute.eventId)
    }

    fun detailStateFor(eventId: String): EventDetailState? {
        @Suppress("UNUSED_VARIABLE")
        val observedLocationAccessRevision = locationAccessRevision
        val detail = repository.publicEventDetail(eventId) ?: return null
        val rsvp = repository.rsvpFor(eventId, shopperId).takeIf { accountState.isSignedIn }
        val exactAddress = repository.exactLocationFor(
            eventId = eventId,
            shopperId = shopperId,
            nowEpochMillis = nowEpochMillis,
        ).takeIf { accountState.isSignedIn && eventId !in blockedEventIds }
        return EventDetailState(
            detail = detail,
            attendanceState = if (eventCapacitySource.isAtCapacity(eventId)) {
                EventAttendanceState.AtCapacity
            } else {
                EventAttendanceState.Available
            },
            revealState = if (eventId in blockedEventIds) {
                LocationRevealState.Blocked
            } else {
                detail.toLocationRevealState(
                    rsvpStatus = rsvp?.status,
                    locationVisibility = rsvp?.locationVisibility,
                    exactAddress = exactAddress,
                    eventHasEnded = detail.saleWindow.hasEnded(nowEpochMillis),
                )
            },
        )
    }

    fun openEvent(eventId: String) {
        invalidateMessagingNavigation()
        val origin = activePrimaryDestination.takeIf {
            it == YardScapePrimaryDestination.Browse || it == YardScapePrimaryDestination.MyFinds
        } ?: YardScapePrimaryDestination.Browse
        val myFindsSection = (route as? YardScapeRoute.MyFinds)?.section ?: MyFindsSection.Saved
        if (origin == YardScapePrimaryDestination.Browse) {
            selectDiscoveryEvent(eventId)
        }
        route = YardScapeRoute.EventDetail(eventId, origin, myFindsSection)
    }

    fun openRsvp(eventId: String) {
        val context = rsvpRouteContext(eventId)
        if (!canSubmitRsvp(eventId)) {
            route = context.detailRoute(eventId)
            return
        }
        val gate = protectedActionDecision(ProtectedAction.Rsvp)
        if (gate is ProtectedActionDecision.SignInRequired) {
            pendingProtectedAction = PendingProtectedAction(
                ProtectedAction.Rsvp,
                context.detailRoute(eventId),
            )
            accountState = accountState.copy(signInReason = gate.message)
            route = YardScapeRoute.Account
            return
        }
        route = context.rsvpRoute(eventId)
    }

    fun confirmRsvp(eventId: String) {
        val context = rsvpRouteContext(eventId)
        if (!accountState.isSignedIn) {
            openRsvp(eventId)
            return
        }
        if (!canSubmitRsvp(eventId)) {
            route = context.detailRoute(eventId)
            return
        }
        repository.submitRsvp(eventId, shopperId)
        route = context.detailRoute(eventId)
    }

    fun rsvpScreenStateFor(eventId: String): RsvpScreenState =
        rsvpEligibilityStatusFor(eventId).toRsvpScreenState()

    private fun canSubmitRsvp(eventId: String): Boolean {
        return rsvpEligibilityStatusFor(eventId) == RsvpEligibilityStatus.ELIGIBLE
    }

    private fun rsvpEligibilityStatusFor(eventId: String): RsvpEligibilityStatus {
        val detail = repository.publicEventDetail(eventId) ?: return RsvpEligibilityStatus.EVENT_UNAVAILABLE
        val currentRsvp = repository.rsvpFor(eventId, shopperId)
        return RsvpEligibilityPolicy.statusFor(
            eventStatus = detail.status,
            saleWindow = detail.saleWindow,
            currentRsvpStatus = currentRsvp?.status,
            currentLocationVisibility = currentRsvp?.locationVisibility,
            nowEpochMillis = nowEpochMillis,
            isBlocked = eventId in blockedEventIds,
            isAtCapacity = eventCapacitySource.isAtCapacity(eventId),
        )
    }

    private fun rsvpRouteContext(eventId: String): RsvpRouteContext {
        val childRoute = when (val currentRoute = route) {
            is YardScapeRoute.EventDetail -> currentRoute.takeIf { it.eventId == eventId }
            is YardScapeRoute.Rsvp -> currentRoute.takeIf { it.eventId == eventId }
            else -> null
        }
        val origin = childRoute?.primaryDestination ?: route.primaryDestination.takeIf {
            it == YardScapePrimaryDestination.Browse || it == YardScapePrimaryDestination.MyFinds
        } ?: YardScapePrimaryDestination.Browse
        val myFindsSection = when (childRoute) {
            is YardScapeRoute.EventDetail -> childRoute.myFindsSection
            is YardScapeRoute.Rsvp -> childRoute.myFindsSection
            else -> (route as? YardScapeRoute.MyFinds)?.section ?: MyFindsSection.Saved
        }
        return RsvpRouteContext(origin = origin, myFindsSection = myFindsSection)
    }

    private fun exitMatchingRsvp(eventId: String) {
        val currentRoute = route as? YardScapeRoute.Rsvp ?: return
        if (currentRoute.eventId != eventId) return
        route = YardScapeRoute.EventDetail(
            eventId = eventId,
            origin = currentRoute.origin,
            myFindsSection = currentRoute.myFindsSection,
        )
    }

    fun openReport(eventId: String) {
        openShopperSafety(eventId, ShopperSafetyAction.Report)
    }

    fun openBlock(eventId: String) {
        openShopperSafety(eventId, ShopperSafetyAction.Block)
    }

    fun openMessageThreadReport() {
        openMessageThreadSafety(ShopperSafetyAction.Report)
    }

    fun openMessageThreadBlock() {
        openMessageThreadSafety(ShopperSafetyAction.Block)
    }

    private fun openMessageThreadSafety(action: ShopperSafetyAction) {
        val messageRoute = route as? YardScapeRoute.MessageThread ?: return
        val loaded = messagingThreadState as? MessagingThreadUiState.Loaded ?: return
        if (loaded.presentation.conversationId != messageRoute.conversationId.value) return
        val actor = messagingActor()
        if (!accountState.isSignedIn || actor.role != UserRole.SHOPPER) return
        if (actor.userId != loaded.presentation.thread.conversationKey.shopperId) return
        invalidateMessagingNavigation()
        openShopperSafety(
            eventId = loaded.presentation.thread.conversationKey.eventId,
            action = action,
            requestedOrigin = YardScapePrimaryDestination.Messages,
            returnMessageThreadId = messageRoute.conversationId,
        )
    }

    private fun openShopperSafety(
        eventId: String,
        action: ShopperSafetyAction,
        requestedOrigin: YardScapePrimaryDestination? = null,
        returnMessageThreadId: MarketplaceConversationId? = null,
    ) {
        val detail = repository.publicEventDetail(eventId) ?: return
        val detailRoute = route as? YardScapeRoute.EventDetail
        val origin = requestedOrigin ?: route.primaryDestination.takeIf {
            it == YardScapePrimaryDestination.Browse ||
                it == YardScapePrimaryDestination.MyFinds ||
                (it == YardScapePrimaryDestination.Messages && returnMessageThreadId != null)
        } ?: YardScapePrimaryDestination.Browse
        val target = YardScapeRoute.EventSafety(
            eventId,
            action,
            origin,
            detailRoute?.myFindsSection ?: MyFindsSection.Saved,
            returnMessageThreadId,
        )
        shopperSafetyState = shopperSafetyState
            ?.takeIf { it.eventId == eventId && it.action == action }
            ?.copy(isBlocked = eventId in blockedEventIds)
            ?: ShopperSafetyUiState(
                eventId = eventId,
                eventTitle = detail.title,
                action = action,
                isBlocked = eventId in blockedEventIds,
            )
        val protectedAction = when (action) {
            ShopperSafetyAction.Report -> ProtectedAction.Report
            ShopperSafetyAction.Block -> ProtectedAction.Block
        }
        when (val gate = protectedActionDecision(protectedAction)) {
            ProtectedActionDecision.Allowed -> route = target
            is ProtectedActionDecision.SignInRequired -> {
                pendingProtectedAction = PendingProtectedAction(protectedAction, target)
                accountState = accountState.copy(signInReason = gate.message)
                route = YardScapeRoute.Account
            }
        }
    }

    fun updateSafetyReportReason(reason: ReportReason) {
        shopperSafetyState = shopperSafetyState?.copy(
            reason = reason,
            reportState = ReportSubmissionState.Idle,
        )
    }

    fun updateSafetyReportDetails(details: String) {
        shopperSafetyState = shopperSafetyState?.copy(
            details = details,
            reportState = ReportSubmissionState.Idle,
        )
    }

    fun submitSafetyReport() {
        val state = shopperSafetyState ?: return
        val result = shopperSafetyRepository.submitReport(
            SafetyReportDraft(eventId = state.eventId, reason = state.reason, details = state.details),
        )
        shopperSafetyState = state.copy(
            reportState = when (result) {
                is SafetyRepositoryResult.Success -> ReportSubmissionState.Submitted(result.value.id)
                is SafetyRepositoryResult.ValidationFailure -> ReportSubmissionState.Failed(
                    SafetyFailureKind.Validation,
                    result.messages.joinToString(" "),
                )
                is SafetyRepositoryResult.Offline -> ReportSubmissionState.Failed(
                    SafetyFailureKind.Offline,
                    result.message,
                )
                is SafetyRepositoryResult.ServerError -> ReportSubmissionState.Failed(
                    SafetyFailureKind.Server,
                    result.message,
                )
            },
        )
    }

    fun requestBlockMutation() {
        val state = shopperSafetyState ?: return
        shopperSafetyState = state.copy(
            pendingBlockMutation = if (state.isBlocked) BlockMutation.Unblock else BlockMutation.Block,
            blockState = BlockMutationState.Idle,
        )
    }

    fun dismissBlockMutation() {
        shopperSafetyState = shopperSafetyState?.copy(pendingBlockMutation = null)
    }

    fun confirmBlockMutation() {
        val state = shopperSafetyState ?: return
        val mutation = state.pendingBlockMutation ?: return
        val result = when (mutation) {
            BlockMutation.Block -> shopperSafetyRepository.blockHostForEvent(state.eventId)
            BlockMutation.Unblock -> shopperSafetyRepository.unblockHostForEvent(state.eventId)
        }
        shopperSafetyState = when (result) {
            is SafetyRepositoryResult.Success -> {
                applyBlockedHostUpdate(result.value)
                state.copy(
                    isBlocked = result.value.isBlocked,
                    pendingBlockMutation = null,
                    blockState = BlockMutationState.Completed(
                        isBlocked = result.value.isBlocked,
                        message = if (result.value.isBlocked) {
                            "Host blocked. Their sales and protected locations are no longer active."
                        } else {
                            "Host unblocked for discovery. Previous RSVP and location access remain revoked."
                        },
                    ),
                )
            }
            is SafetyRepositoryResult.ValidationFailure -> state.copy(
                pendingBlockMutation = null,
                blockState = BlockMutationState.Failed(
                    SafetyFailureKind.Validation,
                    result.messages.joinToString(" "),
                ),
            )
            is SafetyRepositoryResult.Offline -> state.copy(
                pendingBlockMutation = null,
                blockState = BlockMutationState.Failed(SafetyFailureKind.Offline, result.message),
            )
            is SafetyRepositoryResult.ServerError -> state.copy(
                pendingBlockMutation = null,
                blockState = BlockMutationState.Failed(SafetyFailureKind.Server, result.message),
            )
        }
    }

    private fun applyBlockedHostUpdate(update: BlockedHostUpdate) {
        if (update.isBlocked) {
            blockedEventIds = blockedEventIds + update.affectedEventIds
            update.affectedEventIds.forEach { eventId ->
                repository.revokeRsvpAccess(eventId, shopperId)
                exitMatchingRsvp(eventId)
            }
            directionsEventId = null
            if (pendingRsvpCancellationEventId in update.affectedEventIds) {
                pendingRsvpCancellationEventId = null
            }
            val safetyRoute = route as? YardScapeRoute.EventSafety
            if (safetyRoute?.origin == YardScapePrimaryDestination.Messages &&
                safetyRoute.eventId in update.affectedEventIds
            ) {
                route = YardScapeRoute.Messages
            }
        } else {
            blockedEventIds = blockedEventIds - update.affectedEventIds
        }
        synchronizeMessagingComposer()
        synchronizeDiscoveryMapMarkers()
    }

    fun openHostCreateEdit(eventId: String? = null) {
        val target = YardScapeRoute.HostCreateEdit(eventId)
        val gate = protectedActionDecision(ProtectedAction.HostManagement)
        if (gate is ProtectedActionDecision.SignInRequired) {
            pendingProtectedAction = PendingProtectedAction(ProtectedAction.HostManagement, target)
            accountState = accountState.copy(signInReason = gate.message)
            route = YardScapeRoute.Account
            return
        }
        if (eventId == null) {
            hostEditorSessions.remove(NEW_HOST_SESSION_KEY)
            hostEditorSessionSignal++
        }
        route = target
    }

    fun openHostAttendees(eventId: String): Boolean {
        if (!accountState.isSignedIn) {
            pendingProtectedAction = PendingProtectedAction(
                ProtectedAction.HostManagement,
                YardScapeRoute.HostAttendees(eventId),
            )
            val gate = protectedActionDecision(ProtectedAction.HostManagement)
            accountState = accountState.copy(signInReason = (gate as? ProtectedActionDecision.SignInRequired)?.message)
            route = YardScapeRoute.Account
            return false
        }
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
        if (!accountState.isSignedIn) return null
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
                canMessageAttendee = activeUserRole == UserRole.HOST &&
                    currentMessagingComposerAccess(
                        MarketplaceConversationKey(eventId, rsvp.shopperId),
                        MessagingActor(hostId, UserRole.HOST),
                    ) is MessagingComposerAccess.Open,
            )
        }.sortedWith(compareBy({ it.state.ordinal }, { it.displayName }))
        return HostAttendanceState(
            eventId = eventId,
            eventTitle = event.title,
            eventPhoto = event.photos.firstOrNull(),
            policy = hostAttendancePolicies[eventId] ?: HostAttendancePolicy(),
            attendees = attendees,
        )
    }

    /** Resolves host messaging through the repository before committing an opaque thread route. */
    suspend fun openHostAttendeeMessage(eventId: String, shopperId: String): Boolean {
        if (!accountState.isSignedIn || activeUserRole != UserRole.HOST) return false
        val event = repository.hostEvent(eventId) ?: return false
        if (event.host.id != hostId) return false
        val key = MarketplaceConversationKey(eventId, shopperId)
        val actor = MessagingActor(hostId, UserRole.HOST)
        if (currentMessagingComposerAccess(key, actor) !is MessagingComposerAccess.Open) return false
        val origin = route
        val navigationVersion = messagingNavigationVersion
        val sessionVersion = messagingSessionVersion

        val result = marketplaceMessagingRepository.threadFor(key, actor)
        if (route != origin ||
            messagingNavigationVersion != navigationVersion ||
            messagingActor() != actor ||
            messagingSessionVersion != sessionVersion ||
            !accountState.isSignedIn ||
            activeUserRole != UserRole.HOST
        ) return false
        val currentEvent = repository.hostEvent(eventId) ?: return false
        if (currentEvent.host.id != hostId) return false
        if (currentMessagingComposerAccess(key, actor) !is MessagingComposerAccess.Open) return false
        val thread = (result as? MessagingRepositoryResult.Success)?.value ?: return false
        if (thread.conversationKey != key || thread.composerAccess !is MessagingComposerAccess.Open) return false
        val opaqueId = MarketplaceConversationId.parse(thread.conversationId) ?: return false
        if (!marketplaceMessagingState.openAuthorizedThread(thread)) return false
        pendingMessageThreadAuthorization = null
        route = YardScapeRoute.MessageThread(opaqueId)
        return true
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
        if (updated != null) synchronizeMessagingComposer()
        return updated != null
    }

    fun returnToBrowse() {
        navigateTo(YardScapePrimaryDestination.Browse)
    }

    fun hostEventItems(): List<HostEventItem> =
        if (accountState.isSignedIn) {
            repository.hostEvents(hostId).map { event ->
                val attendance = hostAttendanceState(event.id)
                event.toHostEventItem(
                    nowEpochMillis = nowEpochMillis,
                    acceptedRsvpCount = attendance?.acceptedCount ?: 0,
                    pendingRsvpCount = attendance?.requestedCount ?: 0,
                    attendeeCap = attendance?.policy?.attendeeCap,
                )
            }
        } else {
            emptyList()
        }

    fun pendingAttendeeCount(eventId: String): Int {
        val isOwningHost = accountState.isSignedIn &&
            activeUserRole == UserRole.HOST &&
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
            val event = repository.hostEvent(eventId.orEmpty())
            val policy = eventId?.let { hostAttendancePolicies[it] } ?: HostAttendancePolicy()
            HostEditorState(
                draft = event?.toHostEventDraft()
                    ?: blankHostEventDraft(),
                validationErrors = emptyList(),
                attendeeCapInput = policy.attendeeCap?.toString().orEmpty(),
                approvalMode = policy.approvalMode,
                hostDisplayName = event?.host?.displayName ?: "YardScape host",
                hostTrustSignals = event?.host?.trustSignals.orEmpty(),
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
        synchronizeDiscoveryMapMarkers()
        return published
    }

    fun publishHostEvent(draft: HostEventDraft): HostEditorState =
        publishHostEvent(HostEditorState(draft = draft, validationErrors = emptyList()))

    fun cancelHostEvent(eventId: String) {
        if (repository.cancelHostEvent(eventId)) {
            if (directionsEventId == eventId) directionsEventId = null
            synchronizeMessagingComposer()
        }
        synchronizeDiscoveryMapMarkers()
        route = YardScapeRoute.HostCreateEdit(eventId)
    }

    fun hideHostEvent(eventId: String) {
        if (repository.hideHostEvent(eventId)) synchronizeMessagingComposer()
        synchronizeDiscoveryMapMarkers()
        route = YardScapeRoute.HostCreateEdit(eventId)
    }

    private fun messagingActor(): MessagingActor = MessagingActor(
        userId = when (activeUserRole) {
            UserRole.SHOPPER -> shopperId
            UserRole.HOST -> hostId
        },
        role = activeUserRole,
    )

    private fun messagingAccessContextFor(key: MarketplaceConversationKey): MessagingAccessContext? {
        val event = repository.hostEvent(key.eventId) ?: return null
        val rsvp = repository.rsvpFor(key.eventId, key.shopperId)
        return MessagingAccessContext(
            conversationKey = key,
            hostId = event.host.id,
            eventStatus = event.status,
            eventHasEnded = event.saleWindow.hasEnded(nowEpochMillis),
            rsvpStatus = rsvp?.status,
            locationVisibility = rsvp?.locationVisibility,
            isBlocked = key.eventId in blockedEventIds,
        )
    }

    private fun currentMessagingComposerAccess(
        key: MarketplaceConversationKey,
        actor: MessagingActor,
    ): MessagingComposerAccess {
        if (!accountState.isSignedIn) {
            return MessagingComposerAccess.Closed(MessagingClosedReason.CONVERSATION_UNAVAILABLE)
        }
        val context = messagingAccessContextFor(key)
            ?: return MessagingComposerAccess.Closed(MessagingClosedReason.CONVERSATION_UNAVAILABLE)
        return MarketplaceMessagingPolicy.composerAccess(context, actor)
    }

    private fun synchronizeMessagingComposer() {
        marketplaceMessagingState.synchronizeComposerAccess()
    }

    private fun invalidateMessagingNavigation() {
        messagingNavigationVersion++
        marketplaceMessagingState.invalidatePendingWork()
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
    val photoReference: String? = null,
)

data class HostEventItem(
    val id: String,
    val title: String,
    val statusLabel: String,
    val dateLabel: String,
    val publicLocationLabel: String,
    val photoReference: String? = null,
    val acceptedRsvpCount: Int = 0,
    val pendingRsvpCount: Int = 0,
    val attendeeCap: Int? = null,
)

data class EventDetailState(
    val detail: PublicEventDetail,
    val revealState: LocationRevealState,
    val attendanceState: EventAttendanceState = EventAttendanceState.Available,
) {
    val shouldShowRsvpAction: Boolean =
        detail.status == EventStatus.PUBLISHED &&
            attendanceState == EventAttendanceState.Available &&
            (revealState is LocationRevealState.NotRequested ||
                revealState is LocationRevealState.Pending)
}

sealed interface AppDataAvailability {
    data object Loading : AppDataAvailability
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

    data object Waitlisted : LocationRevealState {
        override val title: String = "RSVP waitlisted"
        override val message: String =
            "This RSVP is waiting for space. Exact-location access has not been granted."
    }

    data object Declined : LocationRevealState {
        override val title: String = "RSVP declined"
        override val message: String =
            "This RSVP was declined, so exact-location access remains private."
    }

    data object AtCapacity : LocationRevealState {
        override val title: String = "Sale at capacity"
        override val message: String =
            "This RSVP could not be accepted because the sale reached its attendee limit."
    }

    data object AcceptedWithoutAccess : LocationRevealState {
        override val title: String = "RSVP accepted"
        override val message: String =
            "Your RSVP is accepted, but exact-location access is not currently available."
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

    data object Blocked : LocationRevealState {
        override val title: String = "Protected location unavailable"
        override val message: String =
            "You blocked this host. Unblocking can restore discovery, but it will not restore this RSVP or exact-location access."
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
        photoReference = photos.firstOrNull()?.url,
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

fun YardSaleEvent.toHostEventItem(
    nowEpochMillis: Long,
    acceptedRsvpCount: Int = 0,
    pendingRsvpCount: Int = 0,
    attendeeCap: Int? = null,
): HostEventItem =
    HostEventItem(
        id = id,
        title = title,
        statusLabel = status.name.lowercase().replaceFirstChar { it.uppercase() },
        dateLabel = saleWindow.toBrowseDateLabel(nowEpochMillis),
        publicLocationLabel = listOf(
            location.publicLocation.neighborhood,
            location.publicLocation.city,
        ).filter { it.isNotBlank() }.joinToString(" - "),
        photoReference = photos.firstOrNull()?.url,
        acceptedRsvpCount = acceptedRsvpCount,
        pendingRsvpCount = pendingRsvpCount,
        attendeeCap = attendeeCap,
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
    shopperScheduleAndArea(
        saleWindow = saleWindow,
        publicNeighborhood = publicLocation.neighborhood,
        publicCity = publicLocation.city,
        publicDistanceLabel = publicLocation.distanceLabel,
        publicAreaDescription = publicLocation.areaDescription,
        nowEpochMillis = nowEpochMillis,
    ).run {
        shopperDetailSections(
            scheduleLabel = scheduleLabel,
            approximateLocationLabel = approximateLocationLabel,
            categories = categories,
            acceptedPaymentTypes = acceptedPaymentTypes,
            accessibilityNotes = accessibilityNotes,
            hostContext = listOf(hostDisplayName, hostTrustSignals.firstOrNull()).filterNotNull().joinToString(" - "),
        )
    }

private fun PublicEventDetail.toLocationRevealState(
    rsvpStatus: RsvpStatus?,
    locationVisibility: LocationVisibility?,
    exactAddress: ExactAddress?,
    eventHasEnded: Boolean,
): LocationRevealState {
    if (status == EventStatus.CANCELLED || status == EventStatus.COMPLETED) {
        return LocationRevealState.Cancelled
    }
    if (eventHasEnded) {
        return LocationRevealState.Expired
    }
    if (exactAddress != null) {
        return LocationRevealState.Revealed(exactAddress)
    }
    return when {
        locationVisibility == LocationVisibility.REVOKED -> LocationRevealState.Revoked
        locationVisibility == LocationVisibility.EXPIRED -> LocationRevealState.Expired
        rsvpStatus == RsvpStatus.WAITLISTED -> LocationRevealState.Waitlisted
        rsvpStatus == RsvpStatus.DECLINED -> LocationRevealState.Declined
        rsvpStatus == RsvpStatus.FULL -> LocationRevealState.AtCapacity
        rsvpStatus == RsvpStatus.ACCEPTED -> LocationRevealState.AcceptedWithoutAccess
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

internal fun com.naslabs.yardscape.domain.SaleWindow.toBrowseDateLabel(
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

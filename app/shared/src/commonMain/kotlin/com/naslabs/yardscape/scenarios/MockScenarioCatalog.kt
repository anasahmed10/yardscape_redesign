package com.naslabs.yardscape.scenarios

import com.naslabs.yardscape.data.SeededYardSaleData
import com.naslabs.yardscape.data.SeededYardSaleEventRepository
import com.naslabs.yardscape.data.SeededShopperSafetyRepository
import com.naslabs.yardscape.data.SeededSafetyBehavior
import com.naslabs.yardscape.data.SeededSafetyOutcome
import com.naslabs.yardscape.data.YardSaleEventRepository
import com.naslabs.yardscape.domain.LocationVisibility
import com.naslabs.yardscape.domain.Rsvp
import com.naslabs.yardscape.domain.RsvpStatus
import com.naslabs.yardscape.domain.SaleWindow
import com.naslabs.yardscape.domain.UserRole
import com.naslabs.yardscape.ui.AppDataAvailability
import com.naslabs.yardscape.ui.EventCapacitySource
import com.naslabs.yardscape.ui.MockSessionStatus
import com.naslabs.yardscape.ui.YardScapeAppState
import com.naslabs.yardscape.ui.MyFindsSection
import com.naslabs.yardscape.ui.YardScapeRoute
import com.naslabs.yardscape.ui.ShopperSafetyAction

enum class MockScenarioId {
    NewShopper,
    PopulatedBrowse,
    Loading,
    NoNearbyEvents,
    PendingRsvp,
    AcceptedAccess,
    WaitlistedRsvp,
    DeclinedRsvp,
    CancelledRsvp,
    RevokedAccess,
    ExpiredAccess,
    CancelledEvent,
    EventAtCapacity,
    HostWithDrafts,
    HostWithPendingAttendees,
    SignedOutAccount,
    SessionExpiredAccount,
    ShopperProfile,
    HostProfile,
    ReportValidation,
    ReportOffline,
    ReportServerError,
    BlockHost,
    BlockOffline,
    Offline,
    RecoverableError,
}

class MockScenario internal constructor(
    val id: MockScenarioId,
    val name: String,
    val intendedAssertions: List<String>,
    val activeUserRole: UserRole,
    private val shopperId: String = NEW_SHOPPER_ID,
    private val hostId: String = SeededYardSaleData.HOST_AVERY_ID,
    private val initialRoute: YardScapeRoute = YardScapeRoute.Browse,
    private val dataAvailability: AppDataAvailability = AppDataAvailability.Available,
    private val atCapacityEventIds: Set<String> = emptySet(),
    private val initialAccountStatus: MockSessionStatus = MockSessionStatus.SignedIn,
    private val safetyBehavior: SeededSafetyBehavior = SeededSafetyBehavior(),
    private val repositoryFactory: () -> YardSaleEventRepository,
) {
    fun createAppState(): YardScapeAppState = YardScapeAppState(
        repository = repositoryFactory(),
        nowEpochMillis = SeededYardSaleData.BASE_NOW_EPOCH_MILLIS,
        shopperId = shopperId,
        hostId = hostId,
        activeUserRole = activeUserRole,
        dataAvailability = dataAvailability,
        eventCapacitySource = EventCapacitySource { it in atCapacityEventIds },
        initialAccountStatus = initialAccountStatus,
        shopperSafetyRepository = SeededShopperSafetyRepository(behavior = safetyBehavior),
        initialRoute = initialRoute,
    )
}

object MockScenarioCatalog {
    val scenarios: List<MockScenario> = listOf(
        scenario(
            id = MockScenarioId.NewShopper,
            name = "New shopper",
            assertions = listOf("Starts on Browse", "No RSVP grants exact-location access"),
            repositoryFactory = { seeded(rsvps = emptyList()) },
        ),
        scenario(
            id = MockScenarioId.PopulatedBrowse,
            name = "Populated browse",
            assertions = listOf("Shows two upcoming public previews", "Previews contain approximate areas only"),
            repositoryFactory = { seeded(rsvps = emptyList()) },
        ),
        scenario(
            id = MockScenarioId.Loading,
            name = "Loading nearby sales",
            assertions = listOf("Browse reports loading", "Public previews remain privacy-safe"),
            availability = AppDataAvailability.Loading,
            repositoryFactory = { seeded(rsvps = emptyList()) },
        ),
        scenario(
            id = MockScenarioId.NoNearbyEvents,
            name = "No nearby events",
            assertions = listOf("Browse is empty", "Host controls remain available"),
            repositoryFactory = { seeded(events = emptyList(), rsvps = emptyList()) },
        ),
        scenario(
            id = MockScenarioId.PendingRsvp,
            name = "Pending RSVP",
            assertions = listOf("Detail reports RSVP pending", "Exact location remains hidden"),
            shopperId = SeededYardSaleData.SHOPPER_WITHOUT_ACCESS_ID,
            initialRoute = YardScapeRoute.EventDetail(SeededYardSaleData.FAMILY_GARAGE_EVENT_ID),
            repositoryFactory = { seeded() },
        ),
        scenario(
            id = MockScenarioId.AcceptedAccess,
            name = "Accepted location access",
            assertions = listOf("Accepted shopper sees the protected exact location"),
            shopperId = SeededYardSaleData.SHOPPER_WITH_ACCEPTED_ACCESS_ID,
            initialRoute = YardScapeRoute.EventDetail(SeededYardSaleData.ESTATE_TOOLS_EVENT_ID),
            repositoryFactory = { seeded() },
        ),
        scenario(
            id = MockScenarioId.WaitlistedRsvp,
            name = "Waitlisted RSVP",
            assertions = listOf("My RSVPs reports waitlisted", "Exact location remains hidden"),
            shopperId = WAITLISTED_SHOPPER_ID,
            initialRoute = YardScapeRoute.MyFinds(MyFindsSection.Rsvps),
            repositoryFactory = {
                seeded(rsvps = listOf(rsvp(WAITLISTED_SHOPPER_ID, RsvpStatus.WAITLISTED)))
            },
        ),
        scenario(
            id = MockScenarioId.DeclinedRsvp,
            name = "Declined RSVP",
            assertions = listOf("My RSVPs reports declined", "Exact location remains hidden"),
            shopperId = DECLINED_SHOPPER_ID,
            initialRoute = YardScapeRoute.MyFinds(MyFindsSection.Rsvps),
            repositoryFactory = {
                seeded(rsvps = listOf(rsvp(DECLINED_SHOPPER_ID, RsvpStatus.DECLINED)))
            },
        ),
        scenario(
            id = MockScenarioId.CancelledRsvp,
            name = "Cancelled RSVP",
            assertions = listOf("My RSVPs reports shopper cancellation", "Exact location is cleared"),
            shopperId = CANCELLED_SHOPPER_ID,
            initialRoute = YardScapeRoute.MyFinds(MyFindsSection.Rsvps),
            repositoryFactory = {
                seeded(rsvps = listOf(rsvp(CANCELLED_SHOPPER_ID, RsvpStatus.CANCELLED)))
            },
        ),
        scenario(
            id = MockScenarioId.RevokedAccess,
            name = "Revoked location access",
            assertions = listOf("Detail reports revoked access", "Exact location remains hidden"),
            shopperId = REVOKED_SHOPPER_ID,
            initialRoute = YardScapeRoute.EventDetail(SeededYardSaleData.FAMILY_GARAGE_EVENT_ID),
            repositoryFactory = {
                seeded(rsvps = listOf(rsvp(REVOKED_SHOPPER_ID, visibility = LocationVisibility.REVOKED)))
            },
        ),
        scenario(
            id = MockScenarioId.ExpiredAccess,
            name = "Expired location access",
            assertions = listOf("Detail reports expired access", "Exact location remains hidden after the sale"),
            shopperId = EXPIRED_SHOPPER_ID,
            initialRoute = YardScapeRoute.EventDetail(SeededYardSaleData.FAMILY_GARAGE_EVENT_ID),
            repositoryFactory = {
                val expiredEvent = SeededYardSaleData.events
                    .first { it.id == SeededYardSaleData.FAMILY_GARAGE_EVENT_ID }
                    .copy(
                        saleWindow = SaleWindow(
                            startsAtEpochMillis = SeededYardSaleData.BASE_NOW_EPOCH_MILLIS - 7_200_000L,
                            endsAtEpochMillis = SeededYardSaleData.BASE_NOW_EPOCH_MILLIS - 3_600_000L,
                        ),
                    )
                seeded(
                    events = listOf(expiredEvent),
                    rsvps = listOf(rsvp(EXPIRED_SHOPPER_ID, visibility = LocationVisibility.EXPIRED)),
                )
            },
        ),
        scenario(
            id = MockScenarioId.CancelledEvent,
            name = "Cancelled event",
            assertions = listOf("Detail reports cancellation", "Cancellation suppresses exact location"),
            shopperId = SeededYardSaleData.SHOPPER_WITH_ACCEPTED_ACCESS_ID,
            initialRoute = YardScapeRoute.EventDetail(SeededYardSaleData.CANCELLED_EVENT_ID),
            repositoryFactory = { seeded() },
        ),
        scenario(
            id = MockScenarioId.EventAtCapacity,
            name = "Event at capacity",
            assertions = listOf("Detail reports capacity", "New RSVP action is unavailable"),
            shopperId = FULL_SHOPPER_ID,
            initialRoute = YardScapeRoute.EventDetail(SeededYardSaleData.FAMILY_GARAGE_EVENT_ID),
            atCapacityEventIds = setOf(SeededYardSaleData.FAMILY_GARAGE_EVENT_ID),
            repositoryFactory = {
                seeded(rsvps = listOf(rsvp(FULL_SHOPPER_ID, RsvpStatus.FULL)))
            },
        ),
        scenario(
            id = MockScenarioId.HostWithDrafts,
            name = "Host with drafts",
            assertions = listOf("Starts in host editor", "Draft event is available for editing"),
            role = UserRole.HOST,
            initialRoute = YardScapeRoute.HostCreateEdit(SeededYardSaleData.DRAFT_EVENT_ID),
            repositoryFactory = { seeded() },
        ),
        scenario(
            id = MockScenarioId.HostWithPendingAttendees,
            name = "Host with pending attendees",
            assertions = listOf("Host event has one pending attendee", "Attendee has no reveal grant"),
            role = UserRole.HOST,
            initialRoute = YardScapeRoute.HostCreateEdit(SeededYardSaleData.FAMILY_GARAGE_EVENT_ID),
            repositoryFactory = { seeded() },
        ),
        scenario(
            id = MockScenarioId.SignedOutAccount,
            name = "Signed-out account",
            assertions = listOf("Public browsing remains available", "Protected actions request sign-in"),
            initialRoute = YardScapeRoute.Account,
            accountStatus = MockSessionStatus.SignedOut,
            repositoryFactory = { seeded() },
        ),
        scenario(
            id = MockScenarioId.SessionExpiredAccount,
            name = "Expired account session",
            assertions = listOf("Protected state is cleared", "Public browsing remains available"),
            initialRoute = YardScapeRoute.Account,
            accountStatus = MockSessionStatus.Expired,
            repositoryFactory = { seeded() },
        ),
        scenario(
            id = MockScenarioId.ShopperProfile,
            name = "Shopper profile",
            assertions = listOf("Confirmed facts are separate from community activity"),
            initialRoute = YardScapeRoute.Account,
            repositoryFactory = { seeded() },
        ),
        scenario(
            id = MockScenarioId.HostProfile,
            name = "Host profile",
            assertions = listOf("Host trust language avoids identity guarantees"),
            role = UserRole.HOST,
            initialRoute = YardScapeRoute.Account,
            repositoryFactory = { seeded() },
        ),
        scenario(
            id = MockScenarioId.ReportValidation,
            name = "Report validation",
            assertions = listOf("Reason is required", "No report success is claimed"),
            initialRoute = YardScapeRoute.EventSafety(
                SeededYardSaleData.FAMILY_GARAGE_EVENT_ID,
                ShopperSafetyAction.Report,
            ),
            repositoryFactory = { seeded() },
        ),
        scenario(
            id = MockScenarioId.ReportOffline,
            name = "Report offline",
            assertions = listOf("Offline submission stays failed", "Event remains discoverable"),
            initialRoute = YardScapeRoute.EventSafety(
                SeededYardSaleData.FAMILY_GARAGE_EVENT_ID,
                ShopperSafetyAction.Report,
            ),
            safetyBehavior = SeededSafetyBehavior(reportOutcome = SeededSafetyOutcome.Offline),
            repositoryFactory = { seeded() },
        ),
        scenario(
            id = MockScenarioId.ReportServerError,
            name = "Report server error",
            assertions = listOf("Server failure stays failed", "Retry remains available"),
            initialRoute = YardScapeRoute.EventSafety(
                SeededYardSaleData.FAMILY_GARAGE_EVENT_ID,
                ShopperSafetyAction.Report,
            ),
            safetyBehavior = SeededSafetyBehavior(reportOutcome = SeededSafetyOutcome.ServerError),
            repositoryFactory = { seeded() },
        ),
        scenario(
            id = MockScenarioId.BlockHost,
            name = "Block host",
            assertions = listOf("Block confirmation explains reveal loss", "Success clears exact location"),
            shopperId = SeededYardSaleData.SHOPPER_WITH_ACCEPTED_ACCESS_ID,
            initialRoute = YardScapeRoute.EventSafety(
                SeededYardSaleData.ESTATE_TOOLS_EVENT_ID,
                ShopperSafetyAction.Block,
            ),
            repositoryFactory = { seeded() },
        ),
        scenario(
            id = MockScenarioId.BlockOffline,
            name = "Block offline",
            assertions = listOf("Offline block does not mutate state", "Exact location is not falsely cleared"),
            shopperId = SeededYardSaleData.SHOPPER_WITH_ACCEPTED_ACCESS_ID,
            initialRoute = YardScapeRoute.EventSafety(
                SeededYardSaleData.ESTATE_TOOLS_EVENT_ID,
                ShopperSafetyAction.Block,
            ),
            safetyBehavior = SeededSafetyBehavior(blockOutcome = SeededSafetyOutcome.Offline),
            repositoryFactory = { seeded() },
        ),
        scenario(
            id = MockScenarioId.Offline,
            name = "Offline",
            assertions = listOf("Browse reports offline state", "Seeded previews remain privacy-safe"),
            availability = AppDataAvailability.Offline,
            repositoryFactory = { seeded(rsvps = emptyList()) },
        ),
        scenario(
            id = MockScenarioId.RecoverableError,
            name = "Recoverable refresh error",
            assertions = listOf("Browse reports a recoverable error", "Retry guidance is visible"),
            availability = AppDataAvailability.RecoverableError("Try again in a moment."),
            repositoryFactory = { seeded(rsvps = emptyList()) },
        ),
    )

    fun scenario(id: MockScenarioId): MockScenario =
        scenarios.first { it.id == id }

    fun createAppState(id: MockScenarioId): YardScapeAppState =
        scenario(id).createAppState()
}

private fun scenario(
    id: MockScenarioId,
    name: String,
    assertions: List<String>,
    role: UserRole = UserRole.SHOPPER,
    shopperId: String = NEW_SHOPPER_ID,
    initialRoute: YardScapeRoute = YardScapeRoute.Browse,
    availability: AppDataAvailability = AppDataAvailability.Available,
    atCapacityEventIds: Set<String> = emptySet(),
    accountStatus: MockSessionStatus = MockSessionStatus.SignedIn,
    safetyBehavior: SeededSafetyBehavior = SeededSafetyBehavior(),
    repositoryFactory: () -> YardSaleEventRepository,
): MockScenario = MockScenario(
    id = id,
    name = name,
    intendedAssertions = assertions,
    activeUserRole = role,
    shopperId = shopperId,
    initialRoute = initialRoute,
    dataAvailability = availability,
    atCapacityEventIds = atCapacityEventIds,
    initialAccountStatus = accountStatus,
    safetyBehavior = safetyBehavior,
    repositoryFactory = repositoryFactory,
)

private fun seeded(
    events: List<com.naslabs.yardscape.domain.YardSaleEvent> = SeededYardSaleData.events,
    rsvps: List<Rsvp> = SeededYardSaleData.rsvps,
): YardSaleEventRepository = SeededYardSaleEventRepository(events = events, rsvps = rsvps)

private fun rsvp(
    shopperId: String,
    status: RsvpStatus = RsvpStatus.ACCEPTED,
    visibility: LocationVisibility = if (status == RsvpStatus.ACCEPTED) {
        LocationVisibility.RSVP_ACCEPTED
    } else {
        LocationVisibility.PUBLIC_APPROXIMATION
    },
): Rsvp = Rsvp(
    id = "scenario-rsvp-$shopperId",
    eventId = SeededYardSaleData.FAMILY_GARAGE_EVENT_ID,
    shopperId = shopperId,
    status = status,
    locationVisibility = visibility,
)

private const val NEW_SHOPPER_ID = "scenario-new-shopper"
private const val REVOKED_SHOPPER_ID = "scenario-revoked-shopper"
private const val EXPIRED_SHOPPER_ID = "scenario-expired-shopper"
private const val WAITLISTED_SHOPPER_ID = "scenario-waitlisted-shopper"
private const val DECLINED_SHOPPER_ID = "scenario-declined-shopper"
private const val CANCELLED_SHOPPER_ID = "scenario-cancelled-shopper"
private const val FULL_SHOPPER_ID = "scenario-full-shopper"

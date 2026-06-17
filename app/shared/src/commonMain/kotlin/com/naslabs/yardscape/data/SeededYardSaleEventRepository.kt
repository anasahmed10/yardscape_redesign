package com.naslabs.yardscape.data

import com.naslabs.yardscape.domain.EventLocation
import com.naslabs.yardscape.domain.EventPhoto
import com.naslabs.yardscape.domain.EventStatus
import com.naslabs.yardscape.domain.ExactAddress
import com.naslabs.yardscape.domain.LocationRevealPolicy
import com.naslabs.yardscape.domain.LocationVisibility
import com.naslabs.yardscape.domain.PublicEventPreview
import com.naslabs.yardscape.domain.PublicLocation
import com.naslabs.yardscape.domain.Rsvp
import com.naslabs.yardscape.domain.RsvpStatus
import com.naslabs.yardscape.domain.SaleWindow
import com.naslabs.yardscape.domain.UserProfile
import com.naslabs.yardscape.domain.UserRole
import com.naslabs.yardscape.domain.VerificationState
import com.naslabs.yardscape.domain.YardSaleEvent
import com.naslabs.yardscape.domain.toPublicPreview

class SeededYardSaleEventRepository(
    private val events: List<YardSaleEvent> = SeededYardSaleData.events,
    private val rsvps: List<Rsvp> = SeededYardSaleData.rsvps,
) : YardSaleEventRepository {
    override fun publicPreviews(nowEpochMillis: Long): List<PublicEventPreview> =
        events
            .filter { event ->
                event.status == EventStatus.PUBLISHED &&
                    !event.saleWindow.hasEnded(nowEpochMillis)
            }
            .sortedBy { it.saleWindow.startsAtEpochMillis }
            .map { it.toPublicPreview() }

    override fun publicEventDetail(eventId: String): PublicEventDetail? =
        events
            .firstOrNull { it.id == eventId && it.status == EventStatus.PUBLISHED }
            ?.toPublicEventDetail()

    override fun rsvpFor(eventId: String, shopperId: String): Rsvp? =
        rsvps.firstOrNull { it.eventId == eventId && it.shopperId == shopperId }

    override fun exactLocationFor(
        eventId: String,
        shopperId: String,
        nowEpochMillis: Long,
    ): ExactAddress? {
        val event = events.firstOrNull { it.id == eventId } ?: return null
        return LocationRevealPolicy.exactLocationFor(
            event = event,
            rsvp = rsvpFor(eventId, shopperId),
            nowEpochMillis = nowEpochMillis,
        )
    }

    override fun hostEvents(hostId: String): List<YardSaleEvent> =
        events
            .filter { it.host.id == hostId }
            .sortedBy { it.saleWindow.startsAtEpochMillis }
}

private fun YardSaleEvent.toPublicEventDetail(): PublicEventDetail =
    PublicEventDetail(
        id = id,
        title = title,
        description = description,
        saleWindow = saleWindow,
        categories = categories,
        photos = photos,
        hostDisplayName = host.displayName,
        hostTrustSignals = host.trustSignals,
        publicLocation = location.publicLocation,
        status = status,
        rsvpPrompt = "RSVP to request the exact location. Accepted guests can view it until the sale ends.",
    )

object SeededYardSaleData {
    const val SHOPPER_WITHOUT_ACCESS_ID: String = "shopper-browse-only"
    const val SHOPPER_WITH_ACCEPTED_ACCESS_ID: String = "shopper-accepted"
    const val HOST_AVERY_ID: String = "host-avery"
    const val HOST_MARIN_ID: String = "host-marin"

    const val FAMILY_GARAGE_EVENT_ID: String = "event-family-garage"
    const val ESTATE_TOOLS_EVENT_ID: String = "event-estate-tools"
    const val DRAFT_EVENT_ID: String = "event-draft-host"
    const val CANCELLED_EVENT_ID: String = "event-cancelled-rain"

    const val BASE_NOW_EPOCH_MILLIS: Long = 1_750_300_000_000L

    private val avery = UserProfile(
        id = HOST_AVERY_ID,
        displayName = "Avery",
        role = UserRole.HOST,
        verificationState = VerificationState.VERIFIED,
        trustSignals = listOf("Hosted 3 sales", "Usually replies within an hour"),
    )

    private val marin = UserProfile(
        id = HOST_MARIN_ID,
        displayName = "Marin",
        role = UserRole.HOST,
        verificationState = VerificationState.UNVERIFIED,
        trustSignals = listOf("Neighborhood member since 2024"),
    )

    val events: List<YardSaleEvent> = listOf(
        yardSaleEvent(
            id = FAMILY_GARAGE_EVENT_ID,
            title = "Maple Ridge Family Garage Sale",
            description = "Kids bikes, puzzles, kitchen extras, and a few small patio pieces.",
            saleWindow = SaleWindow(
                startsAtEpochMillis = BASE_NOW_EPOCH_MILLIS + HOURS_2,
                endsAtEpochMillis = BASE_NOW_EPOCH_MILLIS + HOURS_7,
            ),
            categories = listOf("kids", "housewares", "furniture"),
            photos = listOf(
                EventPhoto(
                    url = "seed://maple-ridge-driveway",
                    description = "Driveway tables with toys and kitchen bins.",
                ),
            ),
            host = avery,
            publicLocation = PublicLocation(
                neighborhood = "Maple Ridge",
                city = "Riverton",
                areaDescription = "Near Maple Ridge Park",
                distanceLabel = "2 mi",
            ),
            exactAddress = ExactAddress(
                streetAddress = "123 Cedar Street",
                unit = "Garage",
                city = "Riverton",
                region = "WA",
                postalCode = "98000",
                latitude = 47.6101,
                longitude = -122.2015,
                accessInstructions = "Use the side gate by the blue planter.",
            ),
        ),
        yardSaleEvent(
            id = ESTATE_TOOLS_EVENT_ID,
            title = "Marin Estate Tools and Records",
            description = "Well-kept hand tools, vinyl records, lamps, and framed prints.",
            saleWindow = SaleWindow(
                startsAtEpochMillis = BASE_NOW_EPOCH_MILLIS + HOURS_26,
                endsAtEpochMillis = BASE_NOW_EPOCH_MILLIS + HOURS_32,
            ),
            categories = listOf("tools", "music", "decor"),
            photos = listOf(
                EventPhoto(
                    url = "seed://marin-tools-records",
                    description = "Workbench with hand tools and crates of records.",
                ),
            ),
            host = marin,
            publicLocation = PublicLocation(
                neighborhood = "Old Mill",
                city = "Riverton",
                areaDescription = "A few blocks west of Old Mill Library",
                distanceLabel = "4 mi",
            ),
            exactAddress = ExactAddress(
                streetAddress = "418 Juniper Avenue",
                city = "Riverton",
                region = "WA",
                postalCode = "98001",
                latitude = 47.6208,
                longitude = -122.2142,
                accessInstructions = "Sale entrance is through the driveway tent.",
            ),
        ),
        yardSaleEvent(
            id = DRAFT_EVENT_ID,
            title = "Avery Spring Closet Draft",
            description = "Draft sale for host create and edit flows.",
            saleWindow = SaleWindow(
                startsAtEpochMillis = BASE_NOW_EPOCH_MILLIS + HOURS_50,
                endsAtEpochMillis = BASE_NOW_EPOCH_MILLIS + HOURS_55,
            ),
            categories = listOf("clothing", "books"),
            photos = emptyList(),
            host = avery,
            status = EventStatus.DRAFT,
            publicLocation = PublicLocation(
                neighborhood = "Maple Ridge",
                city = "Riverton",
                areaDescription = "Approximate area not yet published",
            ),
            exactAddress = ExactAddress(
                streetAddress = "123 Cedar Street",
                city = "Riverton",
                region = "WA",
                postalCode = "98000",
                latitude = 47.6101,
                longitude = -122.2015,
            ),
        ),
        yardSaleEvent(
            id = CANCELLED_EVENT_ID,
            title = "Rain Check Porch Sale",
            description = "Cancelled seed event for host management and privacy tests.",
            saleWindow = SaleWindow(
                startsAtEpochMillis = BASE_NOW_EPOCH_MILLIS + HOURS_4,
                endsAtEpochMillis = BASE_NOW_EPOCH_MILLIS + HOURS_8,
            ),
            categories = listOf("garden", "decor"),
            photos = emptyList(),
            host = marin,
            status = EventStatus.CANCELLED,
            publicLocation = PublicLocation(
                neighborhood = "Old Mill",
                city = "Riverton",
                areaDescription = "Near Old Mill Library",
            ),
            exactAddress = ExactAddress(
                streetAddress = "418 Juniper Avenue",
                city = "Riverton",
                region = "WA",
                postalCode = "98001",
                latitude = 47.6208,
                longitude = -122.2142,
            ),
        ),
    )

    val rsvps: List<Rsvp> = listOf(
        Rsvp(
            id = "rsvp-requested-family",
            eventId = FAMILY_GARAGE_EVENT_ID,
            shopperId = SHOPPER_WITHOUT_ACCESS_ID,
            status = RsvpStatus.REQUESTED,
            locationVisibility = LocationVisibility.RSVP_REQUESTED,
        ),
        Rsvp(
            id = "rsvp-accepted-estate",
            eventId = ESTATE_TOOLS_EVENT_ID,
            shopperId = SHOPPER_WITH_ACCEPTED_ACCESS_ID,
            status = RsvpStatus.ACCEPTED,
            locationVisibility = LocationVisibility.RSVP_ACCEPTED,
        ),
        Rsvp(
            id = "rsvp-revoked-cancelled",
            eventId = CANCELLED_EVENT_ID,
            shopperId = SHOPPER_WITH_ACCEPTED_ACCESS_ID,
            status = RsvpStatus.ACCEPTED,
            locationVisibility = LocationVisibility.REVOKED,
        ),
    )

    private fun yardSaleEvent(
        id: String,
        title: String,
        description: String,
        saleWindow: SaleWindow,
        categories: List<String>,
        photos: List<EventPhoto>,
        host: UserProfile,
        publicLocation: PublicLocation,
        exactAddress: ExactAddress,
        status: EventStatus = EventStatus.PUBLISHED,
    ): YardSaleEvent =
        YardSaleEvent(
            id = id,
            title = title,
            description = description,
            saleWindow = saleWindow,
            categories = categories,
            photos = photos,
            host = host,
            status = status,
            location = EventLocation(
                publicLocation = publicLocation,
                exactAddress = exactAddress,
            ),
        )

    private const val HOURS_2 = 2L * 60L * 60L * 1_000L
    private const val HOURS_4 = 4L * 60L * 60L * 1_000L
    private const val HOURS_7 = 7L * 60L * 60L * 1_000L
    private const val HOURS_8 = 8L * 60L * 60L * 1_000L
    private const val HOURS_26 = 26L * 60L * 60L * 1_000L
    private const val HOURS_32 = 32L * 60L * 60L * 1_000L
    private const val HOURS_50 = 50L * 60L * 60L * 1_000L
    private const val HOURS_55 = 55L * 60L * 60L * 1_000L
}

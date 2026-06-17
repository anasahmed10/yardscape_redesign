package com.naslabs.yardscape.api

import com.naslabs.yardscape.domain.EventLocation
import com.naslabs.yardscape.domain.EventPhoto
import com.naslabs.yardscape.domain.EventStatus
import com.naslabs.yardscape.domain.ExactAddress
import com.naslabs.yardscape.domain.LocationRevealPolicy
import com.naslabs.yardscape.domain.LocationVisibility
import com.naslabs.yardscape.domain.PublicLocation
import com.naslabs.yardscape.domain.Rsvp
import com.naslabs.yardscape.domain.RsvpStatus
import com.naslabs.yardscape.domain.SaleWindow
import com.naslabs.yardscape.domain.UserProfile
import com.naslabs.yardscape.domain.UserRole
import com.naslabs.yardscape.domain.VerificationState
import com.naslabs.yardscape.domain.YardSaleEvent
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.eventApi(store: EventApiStore = EventApiStore.seeded()) {
    route("/events") {
        get {
            val now = call.nowEpochMillis()
            call.respondJson(store.publicPreviews(now).toJsonArray { it.toJson() })
        }

        get("/{id}") {
            val eventId = call.eventId() ?: return@get
            val detail = store.publicDetail(eventId)
            if (detail == null) {
                call.respondText("Not found", status = HttpStatusCode.NotFound)
            } else {
                call.respondJson(detail.toJson())
            }
        }

        post("/{id}/rsvps") {
            val eventId = call.eventId() ?: return@post
            val shopperId = call.shopperId()
            val rsvp = store.submitRsvp(eventId, shopperId)
            if (rsvp == null) {
                call.respondText("Not found", status = HttpStatusCode.NotFound)
            } else {
                call.respondJson(rsvp.toDto().toJson())
            }
        }

        get("/{id}/location") {
            val eventId = call.eventId() ?: return@get
            val exactLocation = store.exactLocationFor(
                eventId = eventId,
                shopperId = call.shopperId(),
                nowEpochMillis = call.nowEpochMillis(),
            )
            if (exactLocation == null) {
                call.respondText("Location unavailable", status = HttpStatusCode.Forbidden)
            } else {
                call.respondJson(exactLocation.toProtectedDto().toJson())
            }
        }

        post {
            val hostId = call.hostId()
            val event = store.createDraft(hostId = hostId, title = call.query("title") ?: "Untitled yard sale")
            call.respondJson(event.toPublicDetailDto().toJson(), status = HttpStatusCode.Created)
        }

        patch("/{id}") {
            val eventId = call.eventId() ?: return@patch
            val title = call.query("title")
            val description = call.query("description")
            val updated = store.updateEvent(eventId, title, description)
            if (updated == null) {
                call.respondText("Not found", status = HttpStatusCode.NotFound)
            } else {
                call.respondJson(updated.toPublicDetailDto().toJson())
            }
        }

        post("/{id}/cancel") {
            val eventId = call.eventId() ?: return@post
            if (store.cancelEvent(eventId)) {
                call.respondJson("""{"status":"cancelled"}""")
            } else {
                call.respondText("Not found", status = HttpStatusCode.NotFound)
            }
        }
    }
}

class EventApiStore(
    private val events: MutableList<YardSaleEvent>,
    private val rsvps: MutableList<Rsvp>,
) {
    fun publicPreviews(nowEpochMillis: Long): List<PublicEventPreviewDto> =
        events
            .filter { it.status == EventStatus.PUBLISHED && !it.saleWindow.hasEnded(nowEpochMillis) }
            .sortedBy { it.saleWindow.startsAtEpochMillis }
            .map { it.toPublicPreviewDto() }

    fun publicDetail(eventId: String): PublicEventDetailDto? =
        events
            .firstOrNull { it.id == eventId && it.status != EventStatus.DRAFT && it.status != EventStatus.HIDDEN }
            ?.toPublicDetailDto()

    fun submitRsvp(eventId: String, shopperId: String): Rsvp? {
        val event = events.firstOrNull { it.id == eventId } ?: return null
        if (event.status != EventStatus.PUBLISHED) return rsvpFor(eventId, shopperId)

        val accepted = Rsvp(
            id = "rsvp-$eventId-$shopperId",
            eventId = eventId,
            shopperId = shopperId,
            status = RsvpStatus.ACCEPTED,
            locationVisibility = LocationVisibility.RSVP_ACCEPTED,
        )
        rsvps.removeAll { it.eventId == eventId && it.shopperId == shopperId }
        rsvps += accepted
        return accepted
    }

    fun exactLocationFor(eventId: String, shopperId: String, nowEpochMillis: Long): ExactAddress? {
        val event = events.firstOrNull { it.id == eventId } ?: return null
        return LocationRevealPolicy.exactLocationFor(
            event = event,
            rsvp = rsvpFor(eventId, shopperId),
            nowEpochMillis = nowEpochMillis,
        )
    }

    fun createDraft(hostId: String, title: String): YardSaleEvent {
        val host = hosts.firstOrNull { it.id == hostId } ?: UserProfile(
            id = hostId,
            displayName = "Host",
            role = UserRole.HOST,
        )
        val event = YardSaleEvent(
            id = "event-server-${events.size + 1}",
            title = title,
            description = "Draft event created through the MVP API.",
            saleWindow = SaleWindow(BASE_NOW + DAY, BASE_NOW + DAY + HOURS_4),
            categories = listOf("general"),
            photos = emptyList(),
            acceptedPaymentTypes = listOf("Cash"),
            accessibilityNotes = emptyList(),
            host = host,
            status = EventStatus.DRAFT,
            location = EventLocation(
                publicLocation = PublicLocation(
                    neighborhood = "Draft area",
                    city = "Riverton",
                    areaDescription = "Approximate area pending publication",
                ),
                exactAddress = ExactAddress(
                    streetAddress = "Draft protected address",
                    city = "Riverton",
                    region = "WA",
                    postalCode = "98000",
                    latitude = 0.0,
                    longitude = 0.0,
                ),
            ),
        )
        events += event
        return event
    }

    fun updateEvent(eventId: String, title: String?, description: String?): YardSaleEvent? {
        val index = events.indexOfFirst { it.id == eventId }
        if (index == -1) return null
        events[index] = events[index].copy(
            title = title ?: events[index].title,
            description = description ?: events[index].description,
        )
        return events[index]
    }

    fun cancelEvent(eventId: String): Boolean {
        val index = events.indexOfFirst { it.id == eventId }
        if (index == -1) return false
        events[index] = events[index].copy(status = EventStatus.CANCELLED)
        rsvps.replaceAll { rsvp ->
            if (rsvp.eventId == eventId) {
                rsvp.copy(locationVisibility = LocationVisibility.REVOKED)
            } else {
                rsvp
            }
        }
        return true
    }

    private fun rsvpFor(eventId: String, shopperId: String): Rsvp? =
        rsvps.firstOrNull { it.eventId == eventId && it.shopperId == shopperId }

    companion object {
        fun seeded(): EventApiStore =
            EventApiStore(
                events = seededEvents().toMutableList(),
                rsvps = seededRsvps().toMutableList(),
            )
    }
}

data class PublicEventPreviewDto(
    val id: String,
    val title: String,
    val startsAtEpochMillis: Long,
    val endsAtEpochMillis: Long,
    val categories: List<String>,
    val neighborhood: String,
    val city: String,
    val areaDescription: String,
    val distanceLabel: String?,
    val status: String,
)

data class PublicEventDetailDto(
    val id: String,
    val title: String,
    val description: String,
    val startsAtEpochMillis: Long,
    val endsAtEpochMillis: Long,
    val categories: List<String>,
    val acceptedPaymentTypes: List<String>,
    val accessibilityNotes: List<String>,
    val neighborhood: String,
    val city: String,
    val areaDescription: String,
    val distanceLabel: String?,
    val status: String,
)

data class ProtectedLocationDto(
    val streetAddress: String,
    val unit: String?,
    val city: String,
    val region: String,
    val postalCode: String,
    val latitude: Double,
    val longitude: Double,
    val accessInstructions: String?,
)

data class RsvpDto(
    val eventId: String,
    val shopperId: String,
    val status: String,
    val locationVisibility: String,
)

private fun YardSaleEvent.toPublicPreviewDto(): PublicEventPreviewDto =
    PublicEventPreviewDto(
        id = id,
        title = title,
        startsAtEpochMillis = saleWindow.startsAtEpochMillis,
        endsAtEpochMillis = saleWindow.endsAtEpochMillis,
        categories = categories,
        neighborhood = location.publicLocation.neighborhood,
        city = location.publicLocation.city,
        areaDescription = location.publicLocation.areaDescription,
        distanceLabel = location.publicLocation.distanceLabel,
        status = status.name,
    )

private fun YardSaleEvent.toPublicDetailDto(): PublicEventDetailDto =
    PublicEventDetailDto(
        id = id,
        title = title,
        description = description,
        startsAtEpochMillis = saleWindow.startsAtEpochMillis,
        endsAtEpochMillis = saleWindow.endsAtEpochMillis,
        categories = categories,
        acceptedPaymentTypes = acceptedPaymentTypes,
        accessibilityNotes = accessibilityNotes,
        neighborhood = location.publicLocation.neighborhood,
        city = location.publicLocation.city,
        areaDescription = location.publicLocation.areaDescription,
        distanceLabel = location.publicLocation.distanceLabel,
        status = status.name,
    )

private fun ExactAddress.toProtectedDto(): ProtectedLocationDto =
    ProtectedLocationDto(
        streetAddress = streetAddress,
        unit = unit,
        city = city,
        region = region,
        postalCode = postalCode,
        latitude = latitude,
        longitude = longitude,
        accessInstructions = accessInstructions,
    )

private fun Rsvp.toDto(): RsvpDto =
    RsvpDto(
        eventId = eventId,
        shopperId = shopperId,
        status = status.name,
        locationVisibility = locationVisibility.name,
    )

private fun PublicEventPreviewDto.toJson(): String =
    jsonObject(
        "id" to id.json(),
        "title" to title.json(),
        "startsAtEpochMillis" to startsAtEpochMillis.toString(),
        "endsAtEpochMillis" to endsAtEpochMillis.toString(),
        "categories" to categories.toJsonArray { it.json() },
        "neighborhood" to neighborhood.json(),
        "city" to city.json(),
        "areaDescription" to areaDescription.json(),
        "distanceLabel" to distanceLabel.jsonOrNull(),
        "status" to status.json(),
    )

private fun PublicEventDetailDto.toJson(): String =
    jsonObject(
        "id" to id.json(),
        "title" to title.json(),
        "description" to description.json(),
        "startsAtEpochMillis" to startsAtEpochMillis.toString(),
        "endsAtEpochMillis" to endsAtEpochMillis.toString(),
        "categories" to categories.toJsonArray { it.json() },
        "acceptedPaymentTypes" to acceptedPaymentTypes.toJsonArray { it.json() },
        "accessibilityNotes" to accessibilityNotes.toJsonArray { it.json() },
        "neighborhood" to neighborhood.json(),
        "city" to city.json(),
        "areaDescription" to areaDescription.json(),
        "distanceLabel" to distanceLabel.jsonOrNull(),
        "status" to status.json(),
    )

private fun ProtectedLocationDto.toJson(): String =
    jsonObject(
        "streetAddress" to streetAddress.json(),
        "unit" to unit.jsonOrNull(),
        "city" to city.json(),
        "region" to region.json(),
        "postalCode" to postalCode.json(),
        "latitude" to latitude.toString(),
        "longitude" to longitude.toString(),
        "accessInstructions" to accessInstructions.jsonOrNull(),
    )

private fun RsvpDto.toJson(): String =
    jsonObject(
        "eventId" to eventId.json(),
        "shopperId" to shopperId.json(),
        "status" to status.json(),
        "locationVisibility" to locationVisibility.json(),
    )

private suspend fun ApplicationCall.respondJson(json: String, status: HttpStatusCode = HttpStatusCode.OK) {
    respondText(json, ContentType.Application.Json, status)
}

private fun ApplicationCall.eventId(): String? =
    parameters["id"]

private fun ApplicationCall.shopperId(): String =
    query("shopperId") ?: "shopper-browse-only"

private fun ApplicationCall.hostId(): String =
    query("hostId") ?: HOST_AVERY_ID

private fun ApplicationCall.nowEpochMillis(): Long =
    query("now")?.toLongOrNull() ?: BASE_NOW

private fun ApplicationCall.query(name: String): String? =
    request.queryParameters[name]

@Suppress("unused")
private suspend fun ApplicationCall.optionalBodyText(): String =
    receiveText()

private fun jsonObject(vararg fields: Pair<String, String>): String =
    fields.joinToString(prefix = "{", postfix = "}") { (key, value) -> "${key.json()}:$value" }

private fun String.json(): String =
    buildString {
        append('"')
        this@json.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(char)
            }
        }
        append('"')
    }

private fun String?.jsonOrNull(): String =
    this?.json() ?: "null"

private fun <T> Iterable<T>.toJsonArray(render: (T) -> String): String =
    joinToString(prefix = "[", postfix = "]") { render(it) }

private fun seededEvents(): List<YardSaleEvent> =
    listOf(
        yardSaleEvent(
            id = FAMILY_GARAGE_EVENT_ID,
            title = "Maple Ridge Family Garage Sale",
            description = "Kids bikes, puzzles, kitchen extras, and patio pieces.",
            categories = listOf("kids", "housewares", "furniture"),
            publicLocation = PublicLocation("Maple Ridge", "Riverton", "Near Maple Ridge Park", "2 mi"),
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
            host = avery,
        ),
        yardSaleEvent(
            id = CANCELLED_EVENT_ID,
            title = "Rain Check Porch Sale",
            description = "Cancelled seed event.",
            categories = listOf("garden", "decor"),
            publicLocation = PublicLocation("Old Mill", "Riverton", "Near Old Mill Library"),
            exactAddress = ExactAddress(
                streetAddress = "418 Juniper Avenue",
                city = "Riverton",
                region = "WA",
                postalCode = "98001",
                latitude = 47.6208,
                longitude = -122.2142,
            ),
            host = marin,
            status = EventStatus.CANCELLED,
        ),
    )

private fun seededRsvps(): List<Rsvp> =
    listOf(
        Rsvp(
            id = "rsvp-requested-family",
            eventId = FAMILY_GARAGE_EVENT_ID,
            shopperId = "shopper-browse-only",
            status = RsvpStatus.REQUESTED,
            locationVisibility = LocationVisibility.RSVP_REQUESTED,
        ),
        Rsvp(
            id = "rsvp-revoked-cancelled",
            eventId = CANCELLED_EVENT_ID,
            shopperId = "shopper-accepted",
            status = RsvpStatus.ACCEPTED,
            locationVisibility = LocationVisibility.REVOKED,
        ),
    )

private fun yardSaleEvent(
    id: String,
    title: String,
    description: String,
    categories: List<String>,
    publicLocation: PublicLocation,
    exactAddress: ExactAddress,
    host: UserProfile,
    status: EventStatus = EventStatus.PUBLISHED,
): YardSaleEvent =
    YardSaleEvent(
        id = id,
        title = title,
        description = description,
        saleWindow = SaleWindow(BASE_NOW + HOURS_2, BASE_NOW + HOURS_7),
        categories = categories,
        photos = listOf(EventPhoto(url = "seed://$id", description = title)),
        acceptedPaymentTypes = listOf("Cash", "Venmo"),
        accessibilityNotes = listOf("Driveway sale"),
        host = host,
        status = status,
        location = EventLocation(publicLocation, exactAddress),
    )

private const val FAMILY_GARAGE_EVENT_ID = "event-family-garage"
private const val CANCELLED_EVENT_ID = "event-cancelled-rain"
private const val HOST_AVERY_ID = "host-avery"
private const val BASE_NOW = 1_750_300_000_000L
private const val HOURS_2 = 2L * 60L * 60L * 1_000L
private const val HOURS_4 = 4L * 60L * 60L * 1_000L
private const val HOURS_7 = 7L * 60L * 60L * 1_000L
private const val DAY = 24L * 60L * 60L * 1_000L

private val avery = UserProfile(
    id = HOST_AVERY_ID,
    displayName = "Avery",
    role = UserRole.HOST,
    verificationState = VerificationState.VERIFIED,
)

private val marin = UserProfile(
    id = "host-marin",
    displayName = "Marin",
    role = UserRole.HOST,
)

private val hosts = listOf(avery, marin)

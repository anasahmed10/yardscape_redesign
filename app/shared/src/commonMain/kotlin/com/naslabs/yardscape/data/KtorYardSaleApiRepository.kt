package com.naslabs.yardscape.data

import com.naslabs.yardscape.domain.EventPhoto
import com.naslabs.yardscape.domain.EventStatus
import com.naslabs.yardscape.domain.ExactAddress
import com.naslabs.yardscape.domain.PublicEventPreview
import com.naslabs.yardscape.domain.PublicLocation
import com.naslabs.yardscape.domain.Rsvp
import com.naslabs.yardscape.domain.RsvpStatus
import com.naslabs.yardscape.domain.SaleWindow
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodeURLParameter

sealed interface NetworkResult<out T> {
    data class Success<T>(val value: T) : NetworkResult<T>
    data class Failure(val message: String, val statusCode: Int? = null) : NetworkResult<Nothing>
}

interface RemoteYardSaleEventRepository {
    suspend fun publicPreviews(): NetworkResult<List<PublicEventPreview>>

    suspend fun publicEventDetail(eventId: String): NetworkResult<PublicEventDetail>

    suspend fun submitRsvp(eventId: String, shopperId: String): NetworkResult<Rsvp>

    suspend fun exactLocationFor(eventId: String, shopperId: String): NetworkResult<ExactAddress>

    suspend fun createDraft(hostId: String, title: String): NetworkResult<PublicEventDetail>

    suspend fun updateEvent(eventId: String, title: String?, description: String?): NetworkResult<PublicEventDetail>

    suspend fun cancelEvent(eventId: String): NetworkResult<Unit>
}

class KtorYardSaleEventRepository(
    private val apiClient: KtorYardSaleApiClient,
) : RemoteYardSaleEventRepository {
    override suspend fun publicPreviews(): NetworkResult<List<PublicEventPreview>> =
        apiClient.getPublicPreviews().map { items -> items.map { it.toDomainPreview() } }

    override suspend fun publicEventDetail(eventId: String): NetworkResult<PublicEventDetail> =
        apiClient.getPublicDetail(eventId).map { it.toPublicEventDetail() }

    override suspend fun submitRsvp(eventId: String, shopperId: String): NetworkResult<Rsvp> =
        apiClient.submitRsvp(eventId, shopperId).map { it.toRsvp() }

    override suspend fun exactLocationFor(eventId: String, shopperId: String): NetworkResult<ExactAddress> =
        apiClient.getProtectedLocation(eventId, shopperId).map { it.toExactAddress() }

    override suspend fun createDraft(hostId: String, title: String): NetworkResult<PublicEventDetail> =
        apiClient.createDraft(hostId, title).map { it.toPublicEventDetail() }

    override suspend fun updateEvent(
        eventId: String,
        title: String?,
        description: String?,
    ): NetworkResult<PublicEventDetail> =
        apiClient.updateEvent(eventId, title, description).map { it.toPublicEventDetail() }

    override suspend fun cancelEvent(eventId: String): NetworkResult<Unit> =
        apiClient.cancelEvent(eventId)
}

class KtorYardSaleApiClient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) {
    suspend fun getPublicPreviews(): NetworkResult<List<ApiPublicEventPreview>> =
        request("/events") { json ->
            JsonArrayParser.objects(json).map { ApiPublicEventPreview.fromJson(it) }
        }

    suspend fun getPublicDetail(eventId: String): NetworkResult<ApiPublicEventDetail> =
        request("/events/$eventId") { ApiPublicEventDetail.fromJson(JsonObjectParser.parse(it)) }

    suspend fun submitRsvp(eventId: String, shopperId: String): NetworkResult<ApiRsvp> =
        request(
            path = "/events/$eventId/rsvps?shopperId=${shopperId.encodeURLParameter()}",
            method = Method.POST,
        ) { ApiRsvp.fromJson(JsonObjectParser.parse(it)) }

    suspend fun getProtectedLocation(eventId: String, shopperId: String): NetworkResult<ApiProtectedLocation> =
        request("/events/$eventId/location?shopperId=${shopperId.encodeURLParameter()}") {
            ApiProtectedLocation.fromJson(JsonObjectParser.parse(it))
        }

    suspend fun createDraft(hostId: String, title: String): NetworkResult<ApiPublicEventDetail> =
        request(
            path = "/events?hostId=${hostId.encodeURLParameter()}&title=${title.encodeURLParameter()}",
            method = Method.POST,
        ) { ApiPublicEventDetail.fromJson(JsonObjectParser.parse(it)) }

    suspend fun updateEvent(
        eventId: String,
        title: String?,
        description: String?,
    ): NetworkResult<ApiPublicEventDetail> {
        val query = listOfNotNull(
            title?.let { "title=${it.encodeURLParameter()}" },
            description?.let { "description=${it.encodeURLParameter()}" },
        ).joinToString("&")
        val suffix = if (query.isBlank()) "" else "?$query"
        return request(
            path = "/events/$eventId$suffix",
            method = Method.PATCH,
        ) { ApiPublicEventDetail.fromJson(JsonObjectParser.parse(it)) }
    }

    suspend fun cancelEvent(eventId: String): NetworkResult<Unit> =
        request(path = "/events/$eventId/cancel", method = Method.POST) { _ -> Unit }

    private suspend fun <T> request(
        path: String,
        method: Method = Method.GET,
        parse: (String) -> T,
    ): NetworkResult<T> =
        try {
            val response = when (method) {
                Method.GET -> httpClient.get(baseUrl + path)
                Method.POST -> httpClient.post(baseUrl + path)
                Method.PATCH -> httpClient.patch(baseUrl + path)
            }
            val body = response.body<String>()
            if (response.status.value in 200..299) {
                NetworkResult.Success(parse(body))
            } else {
                NetworkResult.Failure(body, response.status.value)
            }
        } catch (error: Throwable) {
            NetworkResult.Failure(error.message ?: "Network request failed")
        }

    private enum class Method {
        GET,
        POST,
        PATCH,
    }
}

data class ApiPublicEventPreview(
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
) {
    fun toDomainPreview(): PublicEventPreview =
        PublicEventPreview(
            id = id,
            title = title,
            description = "",
            saleWindow = SaleWindow(startsAtEpochMillis, endsAtEpochMillis),
            categories = categories,
            photos = emptyList(),
            acceptedPaymentTypes = emptyList(),
            accessibilityNotes = emptyList(),
            hostDisplayName = "Host",
            hostTrustSignals = emptyList(),
            publicLocation = PublicLocation(neighborhood, city, areaDescription, distanceLabel),
            status = status.toEventStatus(),
        )

    companion object {
        fun fromJson(json: Map<String, String>): ApiPublicEventPreview =
            ApiPublicEventPreview(
                id = json.requireString("id"),
                title = json.requireString("title"),
                startsAtEpochMillis = json.requireLong("startsAtEpochMillis"),
                endsAtEpochMillis = json.requireLong("endsAtEpochMillis"),
                categories = json.requireArray("categories"),
                neighborhood = json.requireString("neighborhood"),
                city = json.requireString("city"),
                areaDescription = json.requireString("areaDescription"),
                distanceLabel = json.optionalString("distanceLabel"),
                status = json.requireString("status"),
            )
    }
}

data class ApiPublicEventDetail(
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
) {
    fun toPublicEventDetail(): PublicEventDetail =
        PublicEventDetail(
            id = id,
            title = title,
            description = description,
            saleWindow = SaleWindow(startsAtEpochMillis, endsAtEpochMillis),
            categories = categories,
            photos = listOf(EventPhoto(url = "api://$id", description = title)),
            acceptedPaymentTypes = acceptedPaymentTypes,
            accessibilityNotes = accessibilityNotes,
            hostDisplayName = "Host",
            hostTrustSignals = emptyList(),
            publicLocation = PublicLocation(neighborhood, city, areaDescription, distanceLabel),
            status = status.toEventStatus(),
            rsvpPrompt = "RSVP to request the exact location. Accepted guests can view it until the sale ends.",
        )

    companion object {
        fun fromJson(json: Map<String, String>): ApiPublicEventDetail =
            ApiPublicEventDetail(
                id = json.requireString("id"),
                title = json.requireString("title"),
                description = json.requireString("description"),
                startsAtEpochMillis = json.requireLong("startsAtEpochMillis"),
                endsAtEpochMillis = json.requireLong("endsAtEpochMillis"),
                categories = json.requireArray("categories"),
                acceptedPaymentTypes = json.requireArray("acceptedPaymentTypes"),
                accessibilityNotes = json.requireArray("accessibilityNotes"),
                neighborhood = json.requireString("neighborhood"),
                city = json.requireString("city"),
                areaDescription = json.requireString("areaDescription"),
                distanceLabel = json.optionalString("distanceLabel"),
                status = json.requireString("status"),
            )
    }
}

data class ApiProtectedLocation(
    val streetAddress: String,
    val unit: String?,
    val city: String,
    val region: String,
    val postalCode: String,
    val latitude: Double,
    val longitude: Double,
    val accessInstructions: String?,
) {
    fun toExactAddress(): ExactAddress =
        ExactAddress(
            streetAddress = streetAddress,
            unit = unit,
            city = city,
            region = region,
            postalCode = postalCode,
            latitude = latitude,
            longitude = longitude,
            accessInstructions = accessInstructions,
        )

    companion object {
        fun fromJson(json: Map<String, String>): ApiProtectedLocation =
            ApiProtectedLocation(
                streetAddress = json.requireString("streetAddress"),
                unit = json.optionalString("unit"),
                city = json.requireString("city"),
                region = json.requireString("region"),
                postalCode = json.requireString("postalCode"),
                latitude = json.requireDouble("latitude"),
                longitude = json.requireDouble("longitude"),
                accessInstructions = json.optionalString("accessInstructions"),
            )
    }
}

data class ApiRsvp(
    val eventId: String,
    val shopperId: String,
    val status: String,
    val locationVisibility: String,
) {
    fun toRsvp(): Rsvp =
        Rsvp(
            id = "remote-rsvp-$eventId-$shopperId",
            eventId = eventId,
            shopperId = shopperId,
            status = RsvpStatus.valueOf(status),
            locationVisibility = com.naslabs.yardscape.domain.LocationVisibility.valueOf(locationVisibility),
        )

    companion object {
        fun fromJson(json: Map<String, String>): ApiRsvp =
            ApiRsvp(
                eventId = json.requireString("eventId"),
                shopperId = json.requireString("shopperId"),
                status = json.requireString("status"),
                locationVisibility = json.requireString("locationVisibility"),
            )
    }
}

private fun <T, R> NetworkResult<T>.map(transform: (T) -> R): NetworkResult<R> =
    when (this) {
        is NetworkResult.Success -> NetworkResult.Success(transform(value))
        is NetworkResult.Failure -> this
    }

private fun String.toEventStatus(): EventStatus =
    EventStatus.valueOf(this)

private fun Map<String, String>.requireString(key: String): String =
    optionalString(key) ?: error("Missing JSON string: $key")

private fun Map<String, String>.optionalString(key: String): String? =
    this[key]?.takeUnless { it == "null" }

private fun Map<String, String>.requireLong(key: String): Long =
    requireString(key).toLong()

private fun Map<String, String>.requireDouble(key: String): Double =
    requireString(key).toDouble()

private fun Map<String, String>.requireArray(key: String): List<String> =
    this[key]
        ?.removePrefix("[")
        ?.removeSuffix("]")
        ?.takeIf { it.isNotBlank() }
        ?.let { arrayBody ->
            arrayBody.splitTopLevel(',').map { it.trim().unquoteJson() }
        }
        ?: emptyList()

private object JsonArrayParser {
    fun objects(json: String): List<Map<String, String>> {
        val body = json.trim().removePrefix("[").removeSuffix("]")
        if (body.isBlank()) return emptyList()
        return body.splitTopLevel(',').map { JsonObjectParser.parse(it) }
    }
}

private object JsonObjectParser {
    fun parse(json: String): Map<String, String> {
        val body = json.trim().removePrefix("{").removeSuffix("}")
        if (body.isBlank()) return emptyMap()
        return body.splitTopLevel(',').associate { field ->
            val separatorIndex = field.indexOf(':')
            val key = field.substring(0, separatorIndex).trim().unquoteJson()
            val value = field.substring(separatorIndex + 1).trim()
            key to when {
                value.startsWith('"') -> value.unquoteJson()
                else -> value
            }
        }
    }
}

private fun String.splitTopLevel(separator: Char): List<String> {
    val parts = mutableListOf<String>()
    var depth = 0
    var inString = false
    var escaped = false
    var start = 0
    forEachIndexed { index, char ->
        when {
            escaped -> escaped = false
            char == '\\' && inString -> escaped = true
            char == '"' -> inString = !inString
            !inString && (char == '[' || char == '{') -> depth += 1
            !inString && (char == ']' || char == '}') -> depth -= 1
            !inString && depth == 0 && char == separator -> {
                parts += substring(start, index)
                start = index + 1
            }
        }
    }
    parts += substring(start)
    return parts.map { it.trim() }.filter { it.isNotEmpty() }
}

private fun String.unquoteJson(): String {
    val raw = trim().removePrefix("\"").removeSuffix("\"")
    return buildString {
        var index = 0
        while (index < raw.length) {
            val char = raw[index]
            if (char == '\\' && index + 1 < raw.length) {
                append(
                    when (raw[index + 1]) {
                        'n' -> '\n'
                        'r' -> '\r'
                        't' -> '\t'
                        else -> raw[index + 1]
                    },
                )
                index += 2
            } else {
                append(char)
                index += 1
            }
        }
    }
}

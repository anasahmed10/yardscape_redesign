package com.naslabs.yardscape.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class KtorYardSaleEventRepositoryTest {
    @Test
    fun publicPreviewMappingDoesNotContainExactLocationFields() = runTest {
        val repository = repositoryFor { path ->
            assertEquals("/events", path)
            respondJson(
                """
                [
                  {
                    "id":"event-family-garage",
                    "title":"Maple Ridge Family Garage Sale",
                    "startsAtEpochMillis":1750307200000,
                    "endsAtEpochMillis":1750325200000,
                    "categories":["kids","housewares"],
                    "neighborhood":"Maple Ridge",
                    "city":"Riverton",
                    "areaDescription":"Near Maple Ridge Park",
                    "distanceLabel":"2 mi",
                    "status":"PUBLISHED"
                  }
                ]
                """.trimIndent(),
            )
        }

        val result = assertIs<NetworkResult.Success<List<com.naslabs.yardscape.domain.PublicEventPreview>>>(
            repository.publicPreviews(),
        )

        assertEquals("Maple Ridge", result.value.single().publicLocation.neighborhood)
        assertFalse(result.value.toString().contains("streetAddress"))
        assertFalse(result.value.toString().contains("123 Cedar Street"))
        assertFalse(result.value.toString().contains("latitude"))
    }

    @Test
    fun rsvpAndProtectedLocationMapToDomainTypes() = runTest {
        val repository = repositoryFor { path ->
            when {
                path.startsWith("/events/event-family-garage/rsvps") -> respondJson(
                    """{"eventId":"event-family-garage","shopperId":"shopper-new","status":"ACCEPTED","locationVisibility":"RSVP_ACCEPTED"}""",
                )

                path.startsWith("/events/event-family-garage/location") -> respondJson(
                    """
                    {
                      "streetAddress":"123 Cedar Street",
                      "unit":"Garage",
                      "city":"Riverton",
                      "region":"WA",
                      "postalCode":"98000",
                      "latitude":47.6101,
                      "longitude":-122.2015,
                      "accessInstructions":"Use the side gate."
                    }
                    """.trimIndent(),
                )

                else -> error("Unexpected path: $path")
            }
        }

        val rsvp = assertIs<NetworkResult.Success<com.naslabs.yardscape.domain.Rsvp>>(
            repository.submitRsvp("event-family-garage", "shopper-new"),
        )
        val location = assertIs<NetworkResult.Success<com.naslabs.yardscape.domain.ExactAddress>>(
            repository.exactLocationFor("event-family-garage", "shopper-new"),
        )

        assertEquals("ACCEPTED", rsvp.value.status.name)
        assertEquals("123 Cedar Street", location.value.streetAddress)
    }

    @Test
    fun forbiddenLocationReturnsFailureState() = runTest {
        val repository = repositoryFor {
            respond(
                content = "Location unavailable",
                status = HttpStatusCode.Forbidden,
                headers = headersOf(HttpHeaders.ContentType, "text/plain"),
            )
        }

        val result = repository.exactLocationFor("event-family-garage", "shopper-pending")

        val failure = assertIs<NetworkResult.Failure>(result)
        assertEquals(HttpStatusCode.Forbidden.value, failure.statusCode)
        assertTrue(failure.message.contains("Location unavailable"))
    }

    private fun repositoryFor(handler: MockRequestHandleScope.(String) -> HttpResponseData): KtorYardSaleEventRepository {
        val client = HttpClient(
            MockEngine { request ->
                handler(request.url.encodedPath + request.url.encodedQuery.let { if (it.isBlank()) "" else "?$it" })
            },
        )
        return KtorYardSaleEventRepository(
            KtorYardSaleApiClient(
                httpClient = client,
                baseUrl = "https://yardscape.test",
            ),
        )
    }

    private fun MockRequestHandleScope.respondJson(content: String): HttpResponseData =
        respond(
            content = content,
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
        )
}

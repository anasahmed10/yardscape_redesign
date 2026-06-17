package com.naslabs.yardscape

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.*

class ApplicationTest {

    @Test
    fun testRoot() = testApplication {
        application {
            module()
        }
        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Hello, Ktor!", response.bodyAsText())
    }

    @Test
    fun publicEventListDoesNotExposeExactAddress() = testApplication {
        application {
            module()
        }

        val response = client.get("/events")
        val body = response.bodyAsText()

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(body.contains("Maple Ridge Family Garage Sale"))
        assertFalse(body.contains("123 Cedar Street"))
        assertFalse(body.contains("streetAddress"))
        assertFalse(body.contains("latitude"))
        assertFalse(body.contains("longitude"))
    }

    @Test
    fun publicEventDetailDoesNotExposeExactAddress() = testApplication {
        application {
            module()
        }

        val response = client.get("/events/event-family-garage")
        val body = response.bodyAsText()

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(body.contains("acceptedPaymentTypes"))
        assertTrue(body.contains("Maple Ridge"))
        assertFalse(body.contains("123 Cedar Street"))
        assertFalse(body.contains("accessInstructions"))
    }

    @Test
    fun exactLocationRequiresAcceptedRsvp() = testApplication {
        application {
            module()
        }

        val beforeRsvp = client.get("/events/event-family-garage/location?shopperId=shopper-new")
        assertEquals(HttpStatusCode.Forbidden, beforeRsvp.status)

        val rsvp = client.post("/events/event-family-garage/rsvps?shopperId=shopper-new")
        assertEquals(HttpStatusCode.OK, rsvp.status)
        assertTrue(rsvp.bodyAsText().contains("RSVP_ACCEPTED"))

        val afterRsvp = client.get("/events/event-family-garage/location?shopperId=shopper-new")
        val body = afterRsvp.bodyAsText()

        assertEquals(HttpStatusCode.OK, afterRsvp.status)
        assertTrue(body.contains("123 Cedar Street"))
        assertTrue(body.contains("accessInstructions"))
    }

    @Test
    fun cancelledEventDoesNotRevealLocation() = testApplication {
        application {
            module()
        }

        val response = client.get("/events/event-cancelled-rain/location?shopperId=shopper-accepted")

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertFalse(response.bodyAsText().contains("418 Juniper Avenue"))
    }

    @Test
    fun cancellingPublishedEventRevokesLocationAccess() = testApplication {
        application {
            module()
        }

        client.post("/events/event-family-garage/rsvps?shopperId=shopper-cancel")
        val beforeCancel = client.get("/events/event-family-garage/location?shopperId=shopper-cancel")
        assertEquals(HttpStatusCode.OK, beforeCancel.status)

        val cancel = client.post("/events/event-family-garage/cancel")
        assertEquals(HttpStatusCode.OK, cancel.status)

        val afterCancel = client.get("/events/event-family-garage/location?shopperId=shopper-cancel")
        assertEquals(HttpStatusCode.Forbidden, afterCancel.status)
        assertFalse(afterCancel.bodyAsText().contains("123 Cedar Street"))
    }
}

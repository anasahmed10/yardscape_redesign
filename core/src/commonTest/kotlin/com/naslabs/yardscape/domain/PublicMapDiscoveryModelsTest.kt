package com.naslabs.yardscape.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PublicMapDiscoveryModelsTest {
    @Test
    fun publicMapAreaRejectsRadiusSmallerThanNeighborhoodApproximationMinimum() {
        val error = assertFailsWith<IllegalArgumentException> {
            publicMapArea(
                approximationRadiusMeters = PublicMapArea.MINIMUM_APPROXIMATION_RADIUS_METERS - 1,
            )
        }

        assertTrue(error.message.orEmpty().contains("approximation radius", ignoreCase = true))
    }

    @Test
    fun publicMapAreaAcceptsMinimumNeighborhoodApproximationRadius() {
        val area = publicMapArea(
            approximationRadiusMeters = PublicMapArea.MINIMUM_APPROXIMATION_RADIUS_METERS,
        )

        assertEquals(PublicMapArea.MINIMUM_APPROXIMATION_RADIUS_METERS, area.approximationRadiusMeters)
        assertEquals("Maple Ridge, Riverton", area.displayLabel)
    }

    @Test
    fun publicCenterIsSeededIndependentlyFromProtectedExactCoordinates() {
        val exactAddress = exactAddress()
        val area = publicMapArea()

        assertNotEquals(exactAddress.latitude, area.center.latitude)
        assertNotEquals(exactAddress.longitude, area.center.longitude)
        assertNotEquals<Any>(exactAddress::class, area.center::class)
    }

    @Test
    fun publicEventMarkerContainsOnlyPreviewAndCoarseAreaData() {
        val event = event()
        val marker = event.toPublicPreview().toPublicEventMarker(publicMapArea())
        val renderedPublicModel = marker.toString()

        assertEquals(event.id, marker.eventId)
        assertEquals(event.title, marker.title)
        assertEquals("Maple Ridge, Riverton", marker.area.displayLabel)
        assertFalse(renderedPublicModel.contains("123 Cedar Street"))
        assertFalse(renderedPublicModel.contains("Garage"))
        assertFalse(renderedPublicModel.contains("Use the side gate"))
        assertFalse(renderedPublicModel.contains("47.610123"))
        assertFalse(renderedPublicModel.contains("-122.201567"))
    }

    @Test
    fun clusterRejectsMismatchedEventCount() {
        assertFailsWith<IllegalArgumentException> {
            PublicMapCluster(
                id = "cluster-maple-ridge",
                area = publicMapArea(),
                eventIds = listOf("event-1", "event-2"),
                eventCount = 3,
            )
        }
    }

    @Test
    fun viewportRejectsCoordinatesOutsideWorldBounds() {
        assertFailsWith<IllegalArgumentException> {
            MapViewport(
                center = ViewportCenter(latitude = 91.0, longitude = -122.2),
                zoomLevel = 11.0,
            )
        }
    }

    private fun publicMapArea(
        approximationRadiusMeters: Int = 800,
    ): PublicMapArea =
        PublicMapArea(
            center = NeighborhoodCenter(
                latitude = 47.615,
                longitude = -122.21,
            ),
            approximationRadiusMeters = approximationRadiusMeters,
            displayLabel = "Maple Ridge, Riverton",
        )

    private fun exactAddress(): ExactAddress =
        ExactAddress(
            streetAddress = "123 Cedar Street",
            unit = "Garage",
            city = "Riverton",
            region = "WA",
            postalCode = "98000",
            latitude = 47.610123,
            longitude = -122.201567,
            accessInstructions = "Use the side gate.",
        )

    private fun event(): YardSaleEvent =
        YardSaleEvent(
            id = "event-1",
            title = "Maple Ridge Yard Sale",
            description = "Housewares and garden tools.",
            saleWindow = SaleWindow(1_000L, 3_000L),
            categories = listOf("housewares", "garden"),
            photos = emptyList(),
            host = UserProfile(
                id = "host-1",
                displayName = "Avery",
                role = UserRole.HOST,
            ),
            status = EventStatus.PUBLISHED,
            location = EventLocation(
                publicLocation = PublicLocation(
                    neighborhood = "Maple Ridge",
                    city = "Riverton",
                    areaDescription = "Near Maple Ridge Park",
                ),
                exactAddress = exactAddress(),
            ),
        )
}

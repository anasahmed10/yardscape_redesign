package com.naslabs.yardscape.ui

import com.naslabs.yardscape.domain.NeighborhoodCenter
import com.naslabs.yardscape.domain.PublicEventMarker
import com.naslabs.yardscape.domain.PublicMapArea
import com.naslabs.yardscape.domain.MapViewport
import com.naslabs.yardscape.domain.ViewportCenter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MapDiscoveryPresentationTest {
    @Test
    fun nearbyMarkersClusterWhileSingleMarkersRemainIndividuallySelectable() {
        val mapleOne = marker("maple-1", 47.6150, -122.2100, "Maple Ridge")
        val mapleTwo = marker("maple-2", 47.6153, -122.2102, "Maple Ridge")
        val oldMill = marker("old-mill", 47.6250, -122.2200, "Old Mill")

        val presentation = mapPresentationFor(listOf(mapleOne, mapleTwo, oldMill))

        assertEquals(listOf("maple-1", "maple-2"), presentation.clusters.single().eventIds)
        assertEquals(listOf("old-mill"), presentation.unclusteredMarkers.map { it.eventId })
    }

    @Test
    fun defaultViewportUsesOnlyPublicMarkerCenters() {
        val presentation = mapPresentationFor(
            listOf(
                marker("maple", 47.61, -122.20, "Maple Ridge"),
                marker("old-mill", 47.63, -122.24, "Old Mill"),
            ),
        )

        assertEquals(47.62, presentation.defaultViewport.center.latitude, absoluteTolerance = 0.0001)
        assertEquals(-122.22, presentation.defaultViewport.center.longitude, absoluteTolerance = 0.0001)
        assertTrue(presentation.defaultViewport.zoomLevel in 10.0..13.0)
    }

    @Test
    fun clusteringUsesPublicDistanceInsteadOfNeighborhoodLabel() {
        val nearbyDifferentLabel = listOf(
            marker("near-1", 47.6150, -122.2100, "Maple Ridge"),
            marker("near-2", 47.6153, -122.2102, "South Park"),
        )
        val farSameLabel = listOf(
            marker("far-1", 47.6150, -122.2100, "Maple Ridge"),
            marker("far-2", 47.6350, -122.2400, "Maple Ridge"),
        )

        assertEquals(2, mapPresentationFor(nearbyDifferentLabel).clusters.single().eventCount)
        assertTrue(mapPresentationFor(farSameLabel).clusters.isEmpty())
    }

    @Test
    fun viewportSearchKeepsOnlyMarkersInsideTheSearchedPublicArea() {
        val markers = listOf(
            marker("center", 47.6150, -122.2100, "Maple Ridge"),
            marker("far", 48.6150, -123.2100, "Far Away"),
        )
        val viewport = MapViewport(
            center = ViewportCenter(47.6150, -122.2100),
            zoomLevel = 12.0,
        )

        assertEquals(listOf("center"), markersInViewport(markers, viewport).map { it.eventId })
    }

    private fun marker(eventId: String, latitude: Double, longitude: Double, label: String) =
        PublicEventMarker(
            eventId = eventId,
            title = "$label sale",
            area = PublicMapArea(
                center = NeighborhoodCenter(latitude, longitude),
                approximationRadiusMeters = 900,
                displayLabel = label,
            ),
        )
}

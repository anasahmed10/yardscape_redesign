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
    fun thousandPublicMarkersClusterDeterministicallyAndRepresentEveryEventOnce() {
        val markers = stressMarkers()

        val forward = mapPresentationFor(markers)
        val reversed = mapPresentationFor(markers.reversed())
        val forwardIds = forward.clusters.flatMap { it.eventIds } + forward.unclusteredMarkers.map { it.eventId }
        val reversedIds = reversed.clusters.flatMap { it.eventIds } + reversed.unclusteredMarkers.map { it.eventId }

        assertEquals(markers.map { it.eventId }.toSet(), forwardIds.toSet())
        assertEquals(markers.size, forwardIds.size)
        assertEquals(333, forward.clusters.size)
        assertEquals(334, forward.unclusteredMarkers.size)
        assertEquals(
            (0 until 333).map { group -> listOf("event-${group}-a", "event-${group}-b") }.toSet(),
            forward.clusters.map { it.eventIds }.toSet(),
        )
        assertEquals(forward.clusters, reversed.clusters)
        assertEquals(forward.unclusteredMarkers, reversed.unclusteredMarkers)
        assertEquals(markers.map { it.eventId }.toSet(), reversedIds.toSet())
        assertEquals(markers.size, reversedIds.size)
    }

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
    fun publicMarkersAcrossTheAntimeridianClusterWithAStableId() {
        val east = marker("date-1", 0.0, 179.999, "Dateline East")
        val west = marker("date-2", 0.0, -179.999, "Dateline West")

        val forward = mapPresentationFor(listOf(east, west))
        val reversed = mapPresentationFor(listOf(west, east))

        assertEquals(listOf("date-1", "date-2"), forward.clusters.single().eventIds)
        assertEquals("cluster-date-1-date-2", forward.clusters.single().id)
        assertEquals(forward.clusters, reversed.clusters)
        assertTrue(forward.unclusteredMarkers.isEmpty())
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

    private fun stressMarkers(): List<PublicEventMarker> = buildList {
        repeat(333) { group ->
            val row = group / 18
            val column = group % 18
            val latitude = 30.0 + row * 0.05
            val longitude = -120.0 + column * 0.05
            add(marker("event-${group}-a", latitude, longitude, "Public area $group"))
            add(marker("event-${group}-b", latitude, longitude + 0.01, "Public area $group"))
            add(marker("event-${group}-c", latitude, longitude + 0.02, "Public area $group"))
        }
        add(marker("event-extra", 55.0, -100.0, "Public area extra"))
    }
}

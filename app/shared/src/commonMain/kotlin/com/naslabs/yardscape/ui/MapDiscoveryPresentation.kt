package com.naslabs.yardscape.ui

import com.naslabs.yardscape.domain.MapViewport
import com.naslabs.yardscape.domain.PublicEventMarker
import com.naslabs.yardscape.domain.PublicMapCluster
import com.naslabs.yardscape.domain.ViewportCenter
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class MapDiscoveryPresentation(
    val clusters: List<PublicMapCluster>,
    val unclusteredMarkers: List<PublicEventMarker>,
    val defaultViewport: MapViewport,
)

fun mapPresentationFor(
    markers: List<PublicEventMarker>,
    zoomLevel: Double = 11.5,
): MapDiscoveryPresentation {
    val clusterDistanceMeters = clusterDistanceMeters(markers, zoomLevel)
    val unassigned = markers.toMutableList()
    val markerGroups = buildList {
        while (unassigned.isNotEmpty()) {
            val anchor = unassigned.removeAt(0)
            val nearby = unassigned.filter { marker ->
                publicDistanceMeters(anchor, marker) <= clusterDistanceMeters
            }
            unassigned.removeAll(nearby)
            add(listOf(anchor) + nearby)
        }
    }
    val clusteredGroups = markerGroups.filter { it.size > 1 }
    val clusters = clusteredGroups.map { group ->
        val eventIds = group.map(PublicEventMarker::eventId).sorted()
        PublicMapCluster(
            id = "cluster-${eventIds.joinToString("-")}",
            area = group.first().area,
            eventIds = eventIds,
        )
    }
    val clusteredIds = clusters.flatMap(PublicMapCluster::eventIds).toSet()
    val visibleMarkers = markers.filterNot { it.eventId in clusteredIds }
    val centers = markers.map { it.area.center }
    val center = if (centers.isEmpty()) {
        ViewportCenter(latitude = 47.618, longitude = -122.215)
    } else {
        ViewportCenter(
            latitude = centers.map { it.latitude }.average(),
            longitude = centers.map { it.longitude }.average(),
        )
    }
    return MapDiscoveryPresentation(
        clusters = clusters,
        unclusteredMarkers = visibleMarkers,
        defaultViewport = MapViewport(center = center, zoomLevel = 11.5),
    )
}

fun markersInViewport(
    markers: List<PublicEventMarker>,
    viewport: MapViewport,
): List<PublicEventMarker> {
    val searchRadiusMeters = max(1_000.0, 40_075_000.0 / 2.0.pow(viewport.zoomLevel) * 1.75)
    val centerMarker = PublicEventMarker(
        eventId = "viewport-center",
        title = "Viewport center",
        area = com.naslabs.yardscape.domain.PublicMapArea(
            center = com.naslabs.yardscape.domain.NeighborhoodCenter(
                latitude = viewport.center.latitude,
                longitude = viewport.center.longitude,
            ),
            approximationRadiusMeters = 500,
            displayLabel = "Searched map area",
        ),
    )
    return markers.filter { publicDistanceMeters(centerMarker, it) <= searchRadiusMeters }
}

private fun clusterDistanceMeters(markers: List<PublicEventMarker>, zoomLevel: Double): Double {
    val latitude = markers.firstOrNull()?.area?.center?.latitude ?: 0.0
    val metersPerPixel = 156_543.03392 * cos(latitude * PI / 180.0) / 2.0.pow(zoomLevel)
    return max(120.0, metersPerPixel * 24.0)
}

private fun publicDistanceMeters(first: PublicEventMarker, second: PublicEventMarker): Double {
    val lat1 = first.area.center.latitude * PI / 180.0
    val lat2 = second.area.center.latitude * PI / 180.0
    val deltaLat = lat2 - lat1
    val deltaLon = (second.area.center.longitude - first.area.center.longitude) * PI / 180.0
    val a = sin(deltaLat / 2.0).pow(2) + cos(lat1) * cos(lat2) * sin(deltaLon / 2.0).pow(2)
    return 2.0 * 6_371_000.0 * asin(sqrt(a))
}

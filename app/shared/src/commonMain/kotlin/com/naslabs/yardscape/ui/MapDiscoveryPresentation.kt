package com.naslabs.yardscape.ui

import com.naslabs.yardscape.domain.MapViewport
import com.naslabs.yardscape.domain.PublicEventMarker
import com.naslabs.yardscape.domain.PublicMapCluster
import com.naslabs.yardscape.domain.ViewportCenter

data class MapDiscoveryPresentation(
    val clusters: List<PublicMapCluster>,
    val unclusteredMarkers: List<PublicEventMarker>,
    val defaultViewport: MapViewport,
)

fun mapPresentationFor(markers: List<PublicEventMarker>): MapDiscoveryPresentation {
    val markerGroups = markers.groupBy { marker -> marker.area.displayLabel.trim().lowercase() }
    val clusteredGroups = markerGroups.values.filter { it.size > 1 }
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

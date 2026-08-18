package com.naslabs.yardscape.ui

import com.naslabs.yardscape.domain.MapViewport
import com.naslabs.yardscape.domain.PublicEventMarker
import com.naslabs.yardscape.domain.PublicMapCluster
import com.naslabs.yardscape.domain.ViewportCenter
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.floor
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
    val orderedMarkers = markers.sortedBy(PublicEventMarker::eventId)
    val clusterDistanceMeters = clusterDistanceMeters(orderedMarkers, zoomLevel)
    val markerGroups = spatialMarkerGroups(orderedMarkers, clusterDistanceMeters)
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
    val visibleMarkers = orderedMarkers.filterNot { it.eventId in clusteredIds }
    return MapDiscoveryPresentation(
        clusters = clusters,
        unclusteredMarkers = visibleMarkers,
        defaultViewport = defaultViewportFor(orderedMarkers),
    )
}

fun defaultViewportFor(markers: List<PublicEventMarker>): MapViewport {
    val centers = markers.map { it.area.center }
    val center = if (centers.isEmpty()) {
        ViewportCenter(latitude = 47.618, longitude = -122.215)
    } else {
        ViewportCenter(
            latitude = centers.map { it.latitude }.average(),
            longitude = centers.map { it.longitude }.average(),
        )
    }
    return MapViewport(center = center, zoomLevel = 11.5)
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

/**
 * Groups public map markers from a deterministic, coarse spatial grid. Buckets only limit distance
 * candidates; final membership continues to use public-area great-circle distance.
 */
private fun spatialMarkerGroups(
    orderedMarkers: List<PublicEventMarker>,
    clusterDistanceMeters: Double,
): List<List<PublicEventMarker>> {
    if (orderedMarkers.isEmpty()) return emptyList()

    val grid = publicMarkerGridFor(orderedMarkers, clusterDistanceMeters)
    val buckets = orderedMarkers.groupBy { marker -> grid.cellFor(marker) }
    val unassignedIds = orderedMarkers.map(PublicEventMarker::eventId).toMutableSet()

    return buildList {
        orderedMarkers.forEach { anchor ->
            if (anchor.eventId !in unassignedIds) return@forEach
            val group = buildList {
                add(anchor)
                grid.neighboringCells(anchor).forEach { cell ->
                    buckets[cell].orEmpty().forEach { candidate ->
                        if (
                            candidate.eventId != anchor.eventId &&
                            candidate.eventId in unassignedIds &&
                            publicDistanceMeters(anchor, candidate) <= clusterDistanceMeters
                        ) {
                            add(candidate)
                        }
                    }
                }
            }.sortedBy(PublicEventMarker::eventId)
            unassignedIds.removeAll(group.map(PublicEventMarker::eventId).toSet())
            add(group)
        }
    }
}

private data class PublicMarkerGrid(
    val latitudeCellDegrees: Double,
    val longitudeCellDegrees: Double,
) {
    fun cellFor(marker: PublicEventMarker): PublicMarkerGridCell = PublicMarkerGridCell(
        latitude = floor(marker.area.center.latitude / latitudeCellDegrees).toInt(),
        longitude = floor(marker.area.center.longitude / longitudeCellDegrees).toInt(),
    )

    fun neighboringCells(marker: PublicEventMarker): List<PublicMarkerGridCell> {
        val cell = cellFor(marker)
        return buildList {
            for (latitudeOffset in -1..1) {
                for (longitudeOffset in -1..1) {
                    add(PublicMarkerGridCell(cell.latitude + latitudeOffset, cell.longitude + longitudeOffset))
                }
            }
        }
    }
}

private data class PublicMarkerGridCell(
    val latitude: Int,
    val longitude: Int,
)

private fun publicMarkerGridFor(
    markers: List<PublicEventMarker>,
    clusterDistanceMeters: Double,
): PublicMarkerGrid {
    val smallestLongitudeScale = markers
        .minOf { marker -> abs(cos(marker.area.center.latitude * PI / 180.0)) }
    return PublicMarkerGrid(
        latitudeCellDegrees = clusterDistanceMeters / METERS_PER_LATITUDE_DEGREE,
        longitudeCellDegrees = if (smallestLongitudeScale < MINIMUM_LONGITUDE_SCALE) {
            360.0
        } else {
            clusterDistanceMeters / (METERS_PER_LATITUDE_DEGREE * smallestLongitudeScale)
        },
    )
}

private const val METERS_PER_LATITUDE_DEGREE = 111_320.0
private const val MINIMUM_LONGITUDE_SCALE = 0.000001

private fun publicDistanceMeters(first: PublicEventMarker, second: PublicEventMarker): Double {
    val lat1 = first.area.center.latitude * PI / 180.0
    val lat2 = second.area.center.latitude * PI / 180.0
    val deltaLat = lat2 - lat1
    val deltaLon = (second.area.center.longitude - first.area.center.longitude) * PI / 180.0
    val a = sin(deltaLat / 2.0).pow(2) + cos(lat1) * cos(lat2) * sin(deltaLon / 2.0).pow(2)
    return 2.0 * 6_371_000.0 * asin(sqrt(a))
}

package com.naslabs.yardscape.domain

/** A deliberately coarse center suitable for neighborhood-level public discovery. */
data class NeighborhoodCenter(
    val latitude: Double,
    val longitude: Double,
)

/** Public map placement that cannot carry an exact address or access details. */
data class PublicMapArea(
    val center: NeighborhoodCenter,
    val approximationRadiusMeters: Int,
    val displayLabel: String,
) {
    init {
        require(approximationRadiusMeters >= MINIMUM_APPROXIMATION_RADIUS_METERS) {
            "Public map approximation radius must be at least $MINIMUM_APPROXIMATION_RADIUS_METERS meters."
        }
    }

    companion object {
        const val MINIMUM_APPROXIMATION_RADIUS_METERS: Int = 500
    }
}

/** Provider-neutral center used to preserve map camera state. */
data class ViewportCenter(
    val latitude: Double,
    val longitude: Double,
) {
    init {
        require(latitude.isFinite() && latitude in -90.0..90.0) {
            "Viewport center latitude must be between -90 and 90."
        }
        require(longitude.isFinite() && longitude in -180.0..180.0) {
            "Viewport center longitude must be between -180 and 180."
        }
    }
}

/** Map camera state without any dependency on a specific map SDK. */
data class MapViewport(
    val center: ViewportCenter,
    val zoomLevel: Double,
)

/** A public event pin. Its position is necessarily a [PublicMapArea], never an exact address. */
data class PublicEventMarker(
    val eventId: String,
    val title: String,
    val area: PublicMapArea,
)

/** A provider-neutral grouping of public events at a coarse map area. */
data class PublicMapCluster(
    val id: String,
    val area: PublicMapArea,
    val eventIds: List<String>,
    val eventCount: Int = eventIds.size,
) {
    init {
        require(eventCount == eventIds.size) {
            "Public map cluster event count must match its event ids."
        }
    }
}

fun PublicEventPreview.toPublicEventMarker(area: PublicMapArea): PublicEventMarker =
    PublicEventMarker(
        eventId = id,
        title = title,
        area = area,
    )

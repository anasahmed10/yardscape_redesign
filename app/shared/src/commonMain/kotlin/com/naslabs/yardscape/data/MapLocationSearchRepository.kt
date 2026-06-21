package com.naslabs.yardscape.data

interface MapLocationSearchRepository {
    fun searchHostLocations(query: String): List<MapSelectedLocation>
}

class SeededMapLocationSearchRepository : MapLocationSearchRepository {
    override fun searchHostLocations(query: String): List<MapSelectedLocation> {
        val normalizedQuery = query.trim().lowercase()
        if (normalizedQuery.length < MIN_AUTOCOMPLETE_QUERY_LENGTH) return emptyList()

        return locations.filter { location ->
            listOf(
                location.displayName,
                location.formattedAddress,
                location.streetAddress,
                location.city,
                location.region,
                location.postalCode,
                location.publicNeighborhood,
            ).any { field -> field.lowercase().contains(normalizedQuery) }
        }
    }

    private val locations: List<MapSelectedLocation> =
        listOf(
            MapSelectedLocation(
                providerPlaceId = "maps-demo-maple-ridge",
                displayName = "Maple Ridge driveway",
                formattedAddress = "900 Hidden Lane, Riverton, WA 98002",
                streetAddress = "900 Hidden Lane",
                city = "Riverton",
                region = "WA",
                postalCode = "98002",
                latitude = 47.611,
                longitude = -122.202,
                publicNeighborhood = "Maple Ridge",
                publicAreaDescription = "Near the south park entrance",
                publicDistanceLabel = "3 mi",
            ),
            MapSelectedLocation(
                providerPlaceId = "maps-demo-old-mill",
                displayName = "Old Mill garage",
                formattedAddress = "901 Hidden Lane, Riverton, WA 98002",
                streetAddress = "901 Hidden Lane",
                city = "Riverton",
                region = "WA",
                postalCode = "98002",
                latitude = 47.612,
                longitude = -122.206,
                publicNeighborhood = "Old Mill",
                publicAreaDescription = "Near Old Mill Library",
                publicDistanceLabel = "4 mi",
            ),
        )
}

private const val MIN_AUTOCOMPLETE_QUERY_LENGTH = 3

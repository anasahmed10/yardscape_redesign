package com.naslabs.yardscape.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MapLocationSearchRepositoryTest {
    private val repository = SeededMapLocationSearchRepository()

    @Test
    fun addressAutocompleteRequiresSearchText() {
        assertTrue(repository.searchHostLocations("").isEmpty())
        assertTrue(repository.searchHostLocations("ma").isEmpty())
    }

    @Test
    fun addressAutocompleteMatchesAddressAndNeighborhoodText() {
        val addressResults = repository.searchHostLocations("900 Hidden")
        val neighborhoodResults = repository.searchHostLocations("old mill")

        assertEquals("900 Hidden Lane", addressResults.single().streetAddress)
        assertEquals("901 Hidden Lane", neighborhoodResults.single().streetAddress)
    }
}

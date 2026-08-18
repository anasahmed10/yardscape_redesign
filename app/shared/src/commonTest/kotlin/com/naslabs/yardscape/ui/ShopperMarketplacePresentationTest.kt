package com.naslabs.yardscape.ui

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ShopperMarketplacePresentationTest {
    @Test
    fun marketplacePresentationUsesCompactLayoutBelowTheMapSplit() {
        assertEquals(ShopperMarketplaceLayout.Compact, shopperMarketplaceLayoutFor(759.dp))
    }

    @Test
    fun marketplacePresentationUsesExpandedLayoutAtTheMapSplit() {
        assertEquals(ShopperMarketplaceLayout.Expanded, shopperMarketplaceLayoutFor(760.dp))
    }

    @Test
    fun seededPhotoReferencesResolveToTheirBundledArtworkAndTrustedAltText() {
        val mapleArtwork = shopperArtworkFor(
            eventId = "maple-ridge",
            photoReference = "seed://maple-ridge-driveway",
        )
        val marinArtwork = shopperArtworkFor(
            eventId = "marin-tools",
            photoReference = "seed://marin-tools-records",
        )

        assertEquals(ShopperArtworkResource.GarageSale, mapleArtwork.resource)
        assertEquals("Table filled with second-hand kitchenware and toys", mapleArtwork.contentDescription)
        assertEquals(ShopperArtworkResource.FleaMarket, marinArtwork.resource)
        assertEquals("Shoppers browse vintage furniture at an outdoor market", marinArtwork.contentDescription)
        assertTrue(mapleArtwork.contentDescription.isNotBlank())
    }

    @Test
    fun unknownPhotoReferencesUseAStableBundledFallbackInsteadOfRemoteMedia() {
        val first = shopperArtworkFor("event-77", "https://untrusted.example/photo.jpg")
        val second = shopperArtworkFor("event-77", "https://untrusted.example/photo.jpg")
        val anotherEvent = shopperArtworkFor("event-78", "https://untrusted.example/photo.jpg")

        assertEquals(first, second)
        assertNotEquals("https://untrusted.example/photo.jpg", first.contentDescription)
        assertTrue(anotherEvent.resource in ShopperArtworkResource.entries)
    }

    @Test
    fun bundledArtworkCatalogProvidesSixLocalResourcesWithAltText() {
        assertEquals(6, ShopperArtworkResource.entries.size)
        assertEquals(6, ShopperArtworkResource.entries.map { it.resourceName }.toSet().size)
        assertTrue(ShopperArtworkResource.entries.all { it.contentDescription.isNotBlank() })
    }
}

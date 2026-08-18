package com.naslabs.yardscape.ui

import androidx.compose.ui.unit.dp
import com.naslabs.yardscape.data.PublicEventDetail
import com.naslabs.yardscape.domain.EventPhoto
import com.naslabs.yardscape.domain.EventStatus
import com.naslabs.yardscape.domain.PublicEventPreview
import com.naslabs.yardscape.domain.PublicLocation
import com.naslabs.yardscape.domain.SaleWindow
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
    fun browseAndDetailArtworkPresentationsKeepTheirPublicSeedReferences() {
        val preview = publicPreview(photoReference = "seed://maple-ridge-driveway")
        val detail = PublicEventDetail(
            id = "marin-tools",
            title = "Marin Tools and Records",
            description = "Tools and records.",
            saleWindow = SaleWindow(1_000L, 2_000L),
            categories = listOf("tools"),
            photos = listOf(EventPhoto("seed://marin-tools-records")),
            acceptedPaymentTypes = emptyList(),
            accessibilityNotes = emptyList(),
            hostDisplayName = "Marin",
            hostTrustSignals = emptyList(),
            publicLocation = PublicLocation("Harbor", "Portland", "Near the park"),
            status = EventStatus.PUBLISHED,
            rsvpPrompt = "RSVP for location access.",
        )

        val browsePresentation = preview
            .toBrowseEventItem(nowEpochMillis = 0L)
            .toShopperEventArtworkPresentation()
        val detailPresentation = detail.toShopperEventArtworkPresentation()

        assertEquals("seed://maple-ridge-driveway", browsePresentation.photoReference)
        assertEquals(ShopperArtworkResource.GarageSale, browsePresentation.artwork.resource)
        assertEquals("seed://marin-tools-records", detailPresentation.photoReference)
        assertEquals(ShopperArtworkResource.FleaMarket, detailPresentation.artwork.resource)
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

    private fun publicPreview(photoReference: String): PublicEventPreview =
        PublicEventPreview(
            id = "maple-ridge",
            title = "Maple Ridge Yard Sale",
            description = "Toys and kitchenware.",
            saleWindow = SaleWindow(1_000L, 2_000L),
            categories = listOf("housewares"),
            photos = listOf(EventPhoto(photoReference)),
            acceptedPaymentTypes = emptyList(),
            accessibilityNotes = emptyList(),
            hostDisplayName = "Avery",
            hostTrustSignals = emptyList(),
            publicLocation = PublicLocation("Maple Ridge", "Portland", "Near the library"),
            status = EventStatus.PUBLISHED,
        )
}

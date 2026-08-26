package com.naslabs.yardscape.ui

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShopperBrowsePresentationTest {
    @Test
    fun approvedMarketplaceChromeUsesCompactAndExpandedLayoutsAtSharedBreakpoint() {
        assertEquals(BrowseMarketplaceLayout.Compact, browseMarketplaceLayoutFor(390.dp))
        assertEquals(BrowseMarketplaceLayout.Compact, browseMarketplaceLayoutFor(759.dp))
        assertEquals(BrowseMarketplaceLayout.Expanded, browseMarketplaceLayoutFor(760.dp))
        assertEquals(BrowseMarketplaceLayout.Expanded, browseMarketplaceLayoutFor(1440.dp))
    }

    @Test
    fun compactShellCallsMyFindsSavedWithoutChangingOtherDestinationLabels() {
        assertEquals(
            "Saved",
            marketplaceNavigationLabelFor(
                destination = YardScapePrimaryDestination.MyFinds,
                layout = BrowseMarketplaceLayout.Compact,
            ),
        )
        assertEquals(
            "My Finds",
            marketplaceNavigationLabelFor(
                destination = YardScapePrimaryDestination.MyFinds,
                layout = BrowseMarketplaceLayout.Expanded,
            ),
        )
        YardScapePrimaryDestination.entries
            .filterNot { it == YardScapePrimaryDestination.MyFinds }
            .forEach { destination ->
                assertEquals(
                    destination.label,
                    marketplaceNavigationLabelFor(destination, BrowseMarketplaceLayout.Compact),
                )
            }
    }

    @Test
    fun dateFiltersUseTheApprovedMarketplaceWording() {
        assertEquals("All dates", marketplaceDateLabelFor(DiscoveryDateFilter.Any))
        assertEquals("Today", marketplaceDateLabelFor(DiscoveryDateFilter.Today))
        assertEquals("Tomorrow", marketplaceDateLabelFor(DiscoveryDateFilter.Tomorrow))
        assertEquals("This weekend", marketplaceDateLabelFor(DiscoveryDateFilter.Weekend))
    }

    @Test
    fun marketplaceModeSwitcherLeadsWithTheMapOption() {
        assertEquals(
            listOf(DiscoveryDisplayMode.Map, DiscoveryDisplayMode.List),
            marketplaceDisplayModeOrder(),
        )
    }

    @Test
    fun browseListUsesCompactAndExpandedLayoutsAtTheSharedBreakpoint() {
        assertEquals(ShopperBrowseListLayout.Compact, shopperBrowseListLayoutFor(759.dp))
        assertEquals(ShopperBrowseListLayout.Expanded, shopperBrowseListLayoutFor(760.dp))
    }

    @Test
    fun browseActionsProvideExplicitVisibleOpenAndSaveLabels() {
        val unsaved = shopperBrowseEventActionsFor(isSaved = false)
        val saved = shopperBrowseEventActionsFor(isSaved = true)

        assertEquals("View sale", unsaved.openLabel)
        assertEquals("Save", unsaved.saveLabel)
        assertEquals("View sale", saved.openLabel)
        assertEquals("Remove saved", saved.saveLabel)
        assertFalse(unsaved.isSaved)
        assertTrue(saved.isSaved)
    }

    @Test
    fun compactMapResultUsesTheSameStatefulSaveActionAsListResults() {
        val saved = compactMapResultActionsFor(isSaved = true)

        assertEquals("Remove saved", saved.saveLabel)
        assertTrue(saved.isSaved)
    }

    @Test
    fun browsePresentationUsesOnlyPublicDiscoveryMetadata() {
        val presentation = YardScapeAppState().discoveryState()
            .browsePresentationFor(AppDataAvailability.Available)

        val publicPresentation = presentation.toString()
        assertFalse(publicPresentation.contains("123 Cedar Street"))
        assertFalse(publicPresentation.contains("418 Juniper Avenue"))
        assertFalse(publicPresentation.contains("47.6101"))
        assertFalse(publicPresentation.contains("-122.2142"))
    }
}

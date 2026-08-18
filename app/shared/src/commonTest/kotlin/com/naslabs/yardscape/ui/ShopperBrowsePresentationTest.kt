package com.naslabs.yardscape.ui

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShopperBrowsePresentationTest {
    @Test
    fun browseListUsesCompactAndExpandedLayoutsAtTheSharedBreakpoint() {
        assertEquals(ShopperBrowseListLayout.Compact, shopperBrowseListLayoutFor(759.dp))
        assertEquals(ShopperBrowseListLayout.Expanded, shopperBrowseListLayoutFor(760.dp))
    }

    @Test
    fun browseActionsKeepOpeningPrimaryAndSavingExplicit() {
        val unsaved = shopperBrowseEventActionsFor(isSaved = false)
        val saved = shopperBrowseEventActionsFor(isSaved = true)

        assertEquals("View sale", unsaved.openLabel)
        assertEquals("Save sale", unsaved.saveLabel)
        assertEquals("View sale", saved.openLabel)
        assertEquals("Remove from saved", saved.saveLabel)
        assertFalse(unsaved.isSaved)
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

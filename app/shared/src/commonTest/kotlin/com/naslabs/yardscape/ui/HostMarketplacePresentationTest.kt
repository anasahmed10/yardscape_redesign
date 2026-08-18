package com.naslabs.yardscape.ui

import androidx.compose.ui.unit.dp
import com.naslabs.yardscape.data.SeededYardSaleData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HostMarketplacePresentationTest {
    @Test
    fun dashboardItemsExposePublicArtworkAndHostOnlyRsvpProgressWithoutProtectedLocation() {
        val appState = YardScapeAppState()
        val item = appState.hostEventItems().first { it.id == SeededYardSaleData.FAMILY_GARAGE_EVENT_ID }

        assertEquals("seed://maple-ridge-driveway", item.photoReference)
        assertTrue(item.acceptedRsvpCount > 0)
        assertTrue(item.pendingRsvpCount > 0)
        assertFalse(item.toString().contains("123 Cedar Street"))
        assertFalse(item.toString().contains("47.6101"))
        assertFalse(item.toString().contains("side gate by the blue planter"))
    }

    @Test
    fun hostLayoutsKeepMobileSerialAndUseExpandedWorkspaceAtWideWidths() {
        assertEquals(HostMarketplaceLayout.Compact, hostMarketplaceLayoutFor(390.dp))
        assertEquals(HostMarketplaceLayout.Expanded, hostMarketplaceLayoutFor(1440.dp))
    }

    @Test
    fun editorProgressIsDurableForEveryOneOfTheSevenSteps() {
        val state = YardScapeAppState().hostEditorState(null).copy(step = HostEditorStep.RsvpSettings)

        assertEquals(6, state.progress.currentStep)
        assertEquals(7, state.progress.totalSteps)
        assertEquals(HostEditorStep.RsvpSettings, state.progress.activeStep)
    }

    @Test
    fun previewArtworkUsesTheFirstPubliclySelectedPhoto() {
        val preview = HostPublicPreview(
            title = "Sale",
            description = "Description",
            scheduleLabel = "Saturday",
            approximateLocationLabel = "Maple Ridge - Austin",
            categories = emptyList(),
            photoCaptions = emptyList(),
            photoReferences = listOf("mock://host-photo/furniture"),
            rsvpSummary = "Auto-accept RSVPs",
        )

        assertEquals(
            ShopperArtworkResource.FurnitureMarket,
            preview.toShopperEventArtworkPresentation("preview-event").artwork.resource,
        )
    }
}

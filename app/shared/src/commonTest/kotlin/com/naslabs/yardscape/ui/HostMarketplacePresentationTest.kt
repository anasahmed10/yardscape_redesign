package com.naslabs.yardscape.ui

import androidx.compose.ui.unit.dp
import com.naslabs.yardscape.data.SeededYardSaleData
import com.naslabs.yardscape.domain.EventPhoto
import com.naslabs.yardscape.domain.SaleWindow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HostMarketplacePresentationTest {
    @Test
    fun hostEditorialSurfacesKeepPhotoLedCompactCardsAndTwoColumnExpandedActions() {
        val compact = hostEditorialSurfacePresentationFor(
            HostEditorialSurface.Dashboard,
            HostMarketplaceLayout.Compact,
        )
        val expanded = hostEditorialSurfacePresentationFor(
            HostEditorialSurface.Dashboard,
            HostMarketplaceLayout.Expanded,
        )

        assertEquals(128.dp, compact.artworkSize)
        assertFalse(compact.actionsInline)
        assertTrue(expanded.actionsInline)
    }

    @Test
    fun dashboardItemsExposePublicArtworkAndHostOnlyRsvpProgressWithoutProtectedLocation() {
        val appState = YardScapeAppState()
        val item = appState.hostEventItems().first { it.id == SeededYardSaleData.FAMILY_GARAGE_EVENT_ID }

        assertEquals("seed://maple-ridge-driveway", item.photoReference)
        assertEquals(2, item.acceptedRsvpCount)
        assertEquals(1, item.pendingRsvpCount)
        assertEquals(2, item.attendeeCap)
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
        assertEquals(HostEditorStep.Photos, state.progress.previousStep)
    }

    @Test
    fun previewArtworkUsesTheFirstPubliclySelectedPhoto() {
        val photo = EventPhoto("mock://host-photo/furniture", "Furniture")
        val pickerArtwork = hostArtworkPresentationFor(draftId = null, photoReference = photo.url)
        val selectedArtwork = hostArtworkPresentationFor(draftId = null, photoReference = photo.url)
        val reorderedArtwork = hostArtworkPresentationFor(draftId = null, photoReference = photo.url)

        assertEquals(ShopperArtworkResource.FurnitureMarket, pickerArtwork.artwork.resource)
        assertEquals(pickerArtwork.artwork.resource, selectedArtwork.artwork.resource)
        assertEquals(pickerArtwork.artwork.resource, reorderedArtwork.artwork.resource)
    }

    @Test
    fun previewUsesShopperVisibleMetadataAndArtworkFallbackWithoutHostRsvpPolicy() {
        val appState = YardScapeAppState()
        val seeded = appState.hostEditorState(SeededYardSaleData.FAMILY_GARAGE_EVENT_ID)
        val preview = seeded.publicPreview(SeededYardSaleData.BASE_NOW_EPOCH_MILLIS)
        val noPhotoPreview = seeded.copy(draft = seeded.draft.copy(photos = emptyList()))
            .publicPreview(SeededYardSaleData.BASE_NOW_EPOCH_MILLIS)
        appState.openEvent(SeededYardSaleData.FAMILY_GARAGE_EVENT_ID)
        val shopperSections = appState.selectedEventDetailState()!!.detail
            .toDetailSections(SeededYardSaleData.BASE_NOW_EPOCH_MILLIS)

        assertEquals(listOf("Cash", "Venmo"), preview.acceptedPaymentTypes)
        assertEquals(listOf("Driveway sale", "One small curb step"), preview.accessibilityNotes)
        assertTrue(preview.hostContext.contains("Avery"))
        assertEquals(
            shopperArtworkFor(SeededYardSaleData.FAMILY_GARAGE_EVENT_ID, null).resource,
            noPhotoPreview.toShopperEventArtworkPresentation(SeededYardSaleData.FAMILY_GARAGE_EVENT_ID).artwork.resource,
        )
        assertFalse(preview.toString().contains("Auto-accept RSVPs"))
        assertFalse(preview.toString().contains("123 Cedar Street"))
        assertFalse(preview.toString().contains("side gate by the blue planter"))
        assertTrue(preview.shopperDetailSections().any { it.first == "Payments" && it.second == "Cash, Venmo" })
        assertTrue(preview.shopperDetailSections().any { it.first == "Accessibility" })
        assertTrue(preview.shopperDetailSections().any { it.first == "Host" })
        assertEquals(
            shopperSections.filter { it.first in setOf("When", "Area", "Payments", "Accessibility", "Host") },
            preview.shopperDetailSections().filter { it.first in setOf("When", "Area", "Payments", "Accessibility", "Host") },
        )
    }

    @Test
    fun draftPreviewUsesTheSamePublicSectionsAndFallsBackToAreaDescriptionWithoutDistance() {
        val appState = YardScapeAppState()
        val editor = appState.hostEditorState(SeededYardSaleData.DRAFT_EVENT_ID)
        val draft = editor.draft.copy(
            publicDistanceLabel = null,
            publicAreaDescription = "Community garden entrance",
        )
        val preview = editor.copy(draft = draft).publicPreview(SeededYardSaleData.BASE_NOW_EPOCH_MILLIS)
        val shopperSections = appState.detailStateFor(SeededYardSaleData.FAMILY_GARAGE_EVENT_ID)!!.detail.copy(
            saleWindow = SaleWindow(draft.startsAtEpochMillis!!, draft.endsAtEpochMillis!!),
            categories = draft.categories,
            acceptedPaymentTypes = draft.acceptedPaymentTypes,
            accessibilityNotes = draft.accessibilityNotes,
            publicLocation = appState.detailStateFor(SeededYardSaleData.FAMILY_GARAGE_EVENT_ID)!!.detail.publicLocation.copy(
                neighborhood = draft.publicNeighborhood,
                city = draft.publicCity,
                distanceLabel = null,
                areaDescription = draft.publicAreaDescription,
            ),
        ).toDetailSections(SeededYardSaleData.BASE_NOW_EPOCH_MILLIS)

        assertEquals(
            shopperSections.filter { it.first in setOf("When", "Area", "Payments", "Accessibility", "Host") },
            preview.shopperDetailSections().filter { it.first in setOf("When", "Area", "Payments", "Accessibility", "Host") },
        )
        assertTrue(preview.approximateLocationLabel.endsWith("Community garden entrance"))
        assertFalse(preview.toString().contains(draft.exactStreetAddress))
        draft.accessInstructions?.let { assertFalse(preview.toString().contains(it)) }
    }

    @Test
    fun newDraftUsesNeutralShopperFacingHostContext() {
        assertEquals(
            "YardScape host",
            YardScapeAppState().hostEditorState(null).publicPreview(SeededYardSaleData.BASE_NOW_EPOCH_MILLIS).hostContext,
        )
    }
}

package com.naslabs.yardscape.ui

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class YardScapeAccessibilityTest {
    @Test
    fun minimumInteractiveTargetMatchesTheSharedAccessibilityContract() {
        assertEquals(48.dp, YardScapeMinimumInteractiveTarget)
    }

    @Test
    fun collapsedMapSheetOffersOnlyTheLegalExpandAction() {
        assertEquals(
            listOf("Expand nearby sales"),
            mapSheetAccessibilityFor(MapResultsSheetPosition.Collapsed).actionLabels,
        )
    }

    @Test
    fun expandedMapSheetAnnouncesItsStateAndOffersOnlyCollapse() {
        val accessibility = mapSheetAccessibilityFor(MapResultsSheetPosition.Expanded)

        assertEquals("Expanded", accessibility.stateDescription)
        assertEquals(listOf("Collapse nearby sales"), accessibility.actionLabels)
    }

    @Test
    fun halfExpandedMapSheetOffersBothLegalResizeActions() {
        val accessibility = mapSheetAccessibilityFor(MapResultsSheetPosition.HalfExpanded)

        assertEquals("Partially expanded", accessibility.stateDescription)
        assertEquals(
            listOf("Expand nearby sales", "Collapse nearby sales"),
            accessibility.actionLabels,
        )
    }

    @Test
    fun mapSheetLayoutReservesItsFullHeightAboveOverlayedMapAttribution() {
        val collapsed = mapSheetLayoutFor(MapResultsSheetPosition.Collapsed)
        val halfExpanded = mapSheetLayoutFor(MapResultsSheetPosition.HalfExpanded)
        val expanded = mapSheetLayoutFor(MapResultsSheetPosition.Expanded)

        assertEquals(190.dp, collapsed.height)
        assertEquals(collapsed.height, collapsed.mapBottomClearance)
        assertEquals(300.dp, halfExpanded.height)
        assertEquals(halfExpanded.height, halfExpanded.mapBottomClearance)
        assertEquals(470.dp, expanded.height)
        assertEquals(expanded.height, expanded.mapBottomClearance)
    }

    @Test
    fun asynchronousStatusPresentationUsesPoliteLiveRegions() {
        assertEquals(
            LiveRegionMode.Polite,
            statusAnnouncementPresentationFor(YardScapeStatusMessageKind.Offline).liveRegion,
        )
    }
}

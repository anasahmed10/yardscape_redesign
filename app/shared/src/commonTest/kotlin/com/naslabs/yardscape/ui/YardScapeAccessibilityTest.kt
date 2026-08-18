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
    fun asynchronousStatusPresentationUsesPoliteLiveRegions() {
        assertEquals(
            LiveRegionMode.Polite,
            statusAnnouncementPresentationFor(YardScapeStatusMessageKind.Offline).liveRegion,
        )
    }
}

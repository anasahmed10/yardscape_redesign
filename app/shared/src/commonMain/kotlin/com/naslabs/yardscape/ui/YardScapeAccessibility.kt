package com.naslabs.yardscape.ui

import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal val YardScapeMinimumInteractiveTarget = 48.dp

internal fun Modifier.yardScapeInteractiveTarget(): Modifier =
    heightIn(min = YardScapeMinimumInteractiveTarget)

internal data class MapSheetAccessibilityState(
    val label: String,
    val stateDescription: String,
    val actionLabels: List<String>,
)

internal fun mapSheetAccessibilityFor(position: MapResultsSheetPosition): MapSheetAccessibilityState =
    when (position) {
        MapResultsSheetPosition.Collapsed -> MapSheetAccessibilityState(
            label = "Nearby sales",
            stateDescription = "Collapsed",
            actionLabels = listOf("Expand nearby sales"),
        )
        MapResultsSheetPosition.HalfExpanded -> MapSheetAccessibilityState(
            label = "Nearby sales",
            stateDescription = "Partially expanded",
            actionLabels = listOf("Expand nearby sales", "Collapse nearby sales"),
        )
        MapResultsSheetPosition.Expanded -> MapSheetAccessibilityState(
            label = "Nearby sales",
            stateDescription = "Expanded",
            actionLabels = listOf("Collapse nearby sales"),
        )
    }

internal data class MapSheetLayout(
    val height: Dp,
    val mapBottomClearance: Dp,
)

internal fun mapSheetLayoutFor(position: MapResultsSheetPosition): MapSheetLayout {
    val height = when (position) {
        MapResultsSheetPosition.Collapsed -> 190.dp
        MapResultsSheetPosition.HalfExpanded -> 300.dp
        MapResultsSheetPosition.Expanded -> 470.dp
    }
    return MapSheetLayout(
        height = height,
        mapBottomClearance = height,
    )
}

internal enum class YardScapeStatusMessageKind {
    Success,
    Failure,
    Offline,
    ClosedAccess,
}

internal data class YardScapeStatusAnnouncementPresentation(
    val liveRegion: LiveRegionMode,
)

internal fun statusAnnouncementPresentationFor(
    kind: YardScapeStatusMessageKind,
): YardScapeStatusAnnouncementPresentation = when (kind) {
    YardScapeStatusMessageKind.Success,
    YardScapeStatusMessageKind.Failure,
    YardScapeStatusMessageKind.Offline,
    YardScapeStatusMessageKind.ClosedAccess,
    -> YardScapeStatusAnnouncementPresentation(liveRegion = LiveRegionMode.Polite)
}

internal fun Modifier.yardScapeStatusAnnouncement(
    kind: YardScapeStatusMessageKind,
): Modifier = semantics {
    liveRegion = statusAnnouncementPresentationFor(kind).liveRegion
}

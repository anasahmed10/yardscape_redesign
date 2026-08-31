package com.naslabs.yardscape.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Presentation-only decisions shared by the non-Browse marketplace destinations. */
internal enum class MarketplaceEditorialLayout {
    Compact,
    Expanded,
}

internal val MarketplaceEditorialContentMaxWidth: Dp = 960.dp

internal fun marketplaceEditorialLayoutFor(width: Dp): MarketplaceEditorialLayout =
    if (width >= 760.dp) MarketplaceEditorialLayout.Expanded else MarketplaceEditorialLayout.Compact

internal fun marketplaceEditorialContentWidthFor(availableWidth: Dp): Dp =
    availableWidth.coerceAtMost(MarketplaceEditorialContentMaxWidth)

internal data class MarketplaceEditorialHeaderPresentation(
    val title: String,
    val showsBackNavigation: Boolean,
)

internal fun marketplaceEditorialHeaderFor(route: YardScapeRoute): MarketplaceEditorialHeaderPresentation =
    MarketplaceEditorialHeaderPresentation(
        title = route.destinationLabel,
        showsBackNavigation = when (route) {
            YardScapeRoute.Browse,
            is YardScapeRoute.MyFinds,
            YardScapeRoute.Host,
            YardScapeRoute.Messages,
            YardScapeRoute.Account,
            -> false

            else -> true
        },
    )

internal data class MarketplaceSegmentOption<T>(
    val value: T,
    val label: String,
    val isSelected: Boolean,
)

internal data class MarketplaceSegmentPresentation(
    val isSelected: Boolean,
    val minimumHeight: Dp = YardScapeMinimumInteractiveTarget,
)

internal fun marketplaceSegmentPresentationFor(selected: Boolean): MarketplaceSegmentPresentation =
    MarketplaceSegmentPresentation(isSelected = selected)

package com.naslabs.yardscape.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal enum class BrowseMarketplaceLayout {
    Compact,
    Expanded,
}

internal fun browseMarketplaceLayoutFor(width: Dp): BrowseMarketplaceLayout =
    if (width >= 760.dp) BrowseMarketplaceLayout.Expanded else BrowseMarketplaceLayout.Compact

internal fun marketplaceNavigationLabelFor(
    destination: YardScapePrimaryDestination,
    layout: BrowseMarketplaceLayout,
): String = when {
    destination == YardScapePrimaryDestination.MyFinds && layout == BrowseMarketplaceLayout.Compact -> "Saved"
    else -> destination.label
}

internal fun marketplaceDateLabelFor(filter: DiscoveryDateFilter): String = when (filter) {
    DiscoveryDateFilter.Any -> "All dates"
    DiscoveryDateFilter.Today -> "Today"
    DiscoveryDateFilter.Tomorrow -> "Tomorrow"
    DiscoveryDateFilter.Weekend -> "This weekend"
}

internal fun marketplaceDisplayModeOrder(): List<DiscoveryDisplayMode> =
    listOf(DiscoveryDisplayMode.Map, DiscoveryDisplayMode.List)

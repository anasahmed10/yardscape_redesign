package com.naslabs.yardscape.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class DiscoveryDateFilter(val label: String) {
    Any("Any date"),
    Today("Today"),
    Tomorrow("Tomorrow"),
    Weekend("Weekend"),
}

enum class DiscoveryDistanceFilter(val label: String, val maximumMiles: Int?) {
    Any("Any distance", null),
    WithinTwoMiles("Within 2 mi", 2),
    WithinFiveMiles("Within 5 mi", 5),
}

enum class DiscoveryDisplayMode {
    List,
    Map,
}

data class DiscoveryFilters(
    val query: String = "",
    val date: DiscoveryDateFilter = DiscoveryDateFilter.Any,
    val distance: DiscoveryDistanceFilter = DiscoveryDistanceFilter.Any,
    val categories: Set<String> = emptySet(),
) {
    val isActive: Boolean
        get() = query.isNotBlank() ||
            date != DiscoveryDateFilter.Any ||
            distance != DiscoveryDistanceFilter.Any ||
            categories.isNotEmpty()
}

data class ShopperDiscoveryState(
    val items: List<BrowseEventItem>,
    val totalEventCount: Int,
    val availableCategories: List<String>,
    val filters: DiscoveryFilters,
    val displayMode: DiscoveryDisplayMode,
    val savedEventIds: Set<String>,
    val hasCommittedMapAreaSearch: Boolean = false,
) {
    val hasNoMatches: Boolean
        get() = totalEventCount > 0 && items.isEmpty() && filters.isActive

    val hasNoNearbyEvents: Boolean
        get() = totalEventCount == 0

    val hasNoEventsInMapArea: Boolean
        get() = totalEventCount > 0 && items.isEmpty() && hasCommittedMapAreaSearch
}

internal enum class ShopperBrowseAvailability {
    Loading,
    Results,
    EmptyNearby,
    EmptySearchArea,
    FilteredEmpty,
    OfflineCached,
    RecoverableError,
}

internal data class ShopperBrowsePresentation(
    val availability: ShopperBrowseAvailability,
    val title: String,
    val message: String,
    val actionLabel: String? = null,
)

internal fun ShopperDiscoveryState.browsePresentationFor(
    dataAvailability: AppDataAvailability,
): ShopperBrowsePresentation = when (dataAvailability) {
    AppDataAvailability.Loading -> ShopperBrowsePresentation(
        availability = ShopperBrowseAvailability.Loading,
        title = "Loading nearby sales",
        message = "Finding public previews in your area.",
    )
    AppDataAvailability.Offline -> ShopperBrowsePresentation(
        availability = ShopperBrowseAvailability.OfflineCached,
        title = "You're offline",
        message = "Reconnect to refresh nearby sales. Previously loaded public previews stay available.",
    )
    is AppDataAvailability.RecoverableError -> ShopperBrowsePresentation(
        availability = ShopperBrowseAvailability.RecoverableError,
        title = "Couldn't refresh sales",
        message = dataAvailability.message,
        actionLabel = "Retry",
    )
    AppDataAvailability.Available -> when {
        hasNoNearbyEvents -> ShopperBrowsePresentation(
            availability = ShopperBrowseAvailability.EmptyNearby,
            title = "No nearby sales yet",
            message = "Check again soon or use Host to add the first sale in this area.",
        )
        hasNoEventsInMapArea -> ShopperBrowsePresentation(
            availability = ShopperBrowseAvailability.EmptySearchArea,
            title = "No sales in this map area",
            message = "Show all nearby public previews, then move the map to search a different area.",
            actionLabel = "Show all nearby sales",
        )
        hasNoMatches -> ShopperBrowsePresentation(
            availability = ShopperBrowseAvailability.FilteredEmpty,
            title = "No sales match those filters",
            message = "Clear the filters to see every nearby public preview.",
            actionLabel = "Show all sales",
        )
        else -> ShopperBrowsePresentation(
            availability = ShopperBrowseAvailability.Results,
            title = "Nearby sales",
            message = "$totalEventCount public previews",
        )
    }
}

internal enum class ShopperBrowseListLayout {
    Compact,
    Expanded,
}

internal fun shopperBrowseListLayoutFor(width: Dp): ShopperBrowseListLayout =
    if (width >= 760.dp) ShopperBrowseListLayout.Expanded else ShopperBrowseListLayout.Compact

internal data class ShopperBrowseEventActions(
    val openLabel: String = "View sale",
    val saveLabel: String,
    val isSaved: Boolean,
)

internal fun shopperBrowseEventActionsFor(isSaved: Boolean): ShopperBrowseEventActions =
    ShopperBrowseEventActions(
        saveLabel = if (isSaved) "Remove saved" else "Save",
        isSaved = isSaved,
    )

internal fun compactMapResultActionsFor(isSaved: Boolean): ShopperBrowseEventActions =
    shopperBrowseEventActionsFor(isSaved)

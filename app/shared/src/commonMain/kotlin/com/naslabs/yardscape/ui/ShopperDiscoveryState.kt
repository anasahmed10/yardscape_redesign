package com.naslabs.yardscape.ui

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
) {
    val hasNoMatches: Boolean
        get() = totalEventCount > 0 && items.isEmpty() && filters.isActive

    val hasNoNearbyEvents: Boolean
        get() = totalEventCount == 0
}

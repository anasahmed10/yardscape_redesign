package com.naslabs.yardscape.ui

enum class MyFindsSection {
    Saved,
    Rsvps,
}

data class MyFindsState(
    val section: MyFindsSection,
    val savedItems: List<BrowseEventItem>,
    val rsvpItems: List<ShopperRsvpItem>,
) {
    val isEmpty: Boolean
        get() = when (section) {
            MyFindsSection.Saved -> savedItems.isEmpty()
            MyFindsSection.Rsvps -> rsvpItems.isEmpty()
        }
}

package com.naslabs.yardscape.ui

enum class MyFindsSection {
    Saved,
    Rsvps,
}

internal enum class MyFindsWorkspaceLayout {
    Compact,
    Expanded,
}

internal fun myFindsWorkspaceLayoutFor(widthDp: Int): MyFindsWorkspaceLayout =
    if (widthDp >= 760) MyFindsWorkspaceLayout.Expanded else MyFindsWorkspaceLayout.Compact

internal data class MyFindsSegmentPresentation(
    val section: MyFindsSection,
    val label: String,
    val isSelected: Boolean,
)

internal data class MyFindsEmptyPresentation(
    val title: String,
    val message: String,
    val actionLabel: String,
)

internal data class MyFindsRsvpGroupPresentation(
    val group: RsvpGroup,
    val items: List<ShopperRsvpItem>,
)

internal data class MyFindsWorkspacePresentation(
    val selectedSection: MyFindsSection,
    val segments: List<MyFindsSegmentPresentation>,
    val emptyState: MyFindsEmptyPresentation?,
    val rsvpGroups: List<MyFindsRsvpGroupPresentation>,
)

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

    internal fun workspacePresentation(): MyFindsWorkspacePresentation = MyFindsWorkspacePresentation(
        selectedSection = section,
        segments = MyFindsSection.entries.map { candidate ->
            MyFindsSegmentPresentation(
                section = candidate,
                label = candidate.label,
                isSelected = candidate == section,
            )
        },
        emptyState = when (section) {
            MyFindsSection.Saved -> savedItems.takeIf { it.isEmpty() }?.let {
                MyFindsEmptyPresentation(
                    title = "Nothing saved yet",
                    message = "Browse nearby public previews and save the sales you want to revisit.",
                    actionLabel = "Browse sales",
                )
            }
            MyFindsSection.Rsvps -> rsvpItems.takeIf { it.isEmpty() }?.let {
                MyFindsEmptyPresentation(
                    title = "No RSVPs yet",
                    message = "Browse a public sale to request attendance and follow its RSVP status here.",
                    actionLabel = "Browse sales",
                )
            }
        },
        rsvpGroups = if (section == MyFindsSection.Rsvps) {
            RsvpGroup.entries.mapNotNull { group ->
                rsvpItems.filter { it.group == group }.takeIf { it.isNotEmpty() }?.let {
                    MyFindsRsvpGroupPresentation(group = group, items = it)
                }
            }
        } else {
            emptyList()
        },
    )
}

internal val MyFindsSection.label: String
    get() = when (this) {
        MyFindsSection.Saved -> "Saved"
        MyFindsSection.Rsvps -> "RSVPs"
    }

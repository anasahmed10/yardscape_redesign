package com.naslabs.yardscape.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.naslabs.yardscape.domain.ExactAddress

@Composable
fun MyFindsScreen(
    state: MyFindsState,
    pendingCancellationEventId: String?,
    onSectionSelected: (MyFindsSection) -> Unit,
    onEventSelected: (String) -> Unit,
    onUnsave: (String) -> Unit,
    onBrowse: () -> Unit,
    onRequestCancellation: (String) -> Boolean,
    onDismissCancellation: () -> Unit,
    onConfirmCancellation: () -> Boolean,
    onAddReminder: (String) -> Boolean,
    onExportCalendar: (String) -> Boolean,
    onDirections: (String) -> ExactAddress?,
) {
    val spacing = YardScapeDesign.spacing
    val presentation = state.workspacePresentation()
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val layout = myFindsWorkspaceLayoutFor(maxWidth.value.toInt())
        LazyColumn(
            modifier = Modifier
                .width(myFindsWorkspaceContentWidthFor(maxWidth))
                .fillMaxHeight()
                .align(Alignment.TopCenter)
                .testTag(YardScapeTestTags.MyFindsScreen)
                .padding(horizontal = spacing.large),
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
            item {
                Column(
                    modifier = Modifier.padding(top = spacing.large),
                    verticalArrangement = Arrangement.spacedBy(spacing.small),
                ) {
                    Text(
                        "Save public previews, track RSVP status, and use protected directions only when access is active.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    MarketplaceSegmentedControl(
                        options = presentation.segments.map { segment ->
                            MarketplaceSegmentOption(
                                value = segment.section,
                                label = segment.label,
                                isSelected = segment.isSelected,
                            )
                        },
                        onSelected = onSectionSelected,
                        testTagFor = { section -> YardScapeTestTags.editorialSegment(section.name.lowercase()) },
                    )
                }
            }
            when (presentation.selectedSection) {
                MyFindsSection.Saved -> savedContent(
                    events = state.savedItems,
                    emptyState = presentation.emptyState,
                    layout = layout,
                    onEventSelected = onEventSelected,
                    onUnsave = onUnsave,
                    onBrowse = onBrowse,
                )
                MyFindsSection.Rsvps -> rsvpContent(
                    groups = presentation.rsvpGroups,
                    emptyState = presentation.emptyState,
                    layout = layout,
                    onEventSelected = onEventSelected,
                    onRequestCancellation = onRequestCancellation,
                    onAddReminder = onAddReminder,
                    onExportCalendar = onExportCalendar,
                    onDirections = onDirections,
                    onBrowse = onBrowse,
                )
            }
        }
    }

    if (pendingCancellationEventId != null) {
        AlertDialog(
            onDismissRequest = onDismissCancellation,
            title = { Text("Cancel this RSVP?") },
            text = { Text("Protected exact-location access and directions will be removed immediately.") },
            confirmButton = {
                Button(
                    modifier = Modifier.heightIn(min = 48.dp),
                    onClick = { onConfirmCancellation() },
                ) { Text("Cancel RSVP") }
            },
            dismissButton = {
                TextButton(
                    modifier = Modifier.heightIn(min = 48.dp),
                    onClick = onDismissCancellation,
                ) { Text("Keep RSVP") }
            },
        )
    }
}

private fun LazyListScope.savedContent(
    events: List<BrowseEventItem>,
    emptyState: MyFindsEmptyPresentation?,
    layout: MyFindsWorkspaceLayout,
    onEventSelected: (String) -> Unit,
    onUnsave: (String) -> Unit,
    onBrowse: () -> Unit,
) {
    item { ShopperSectionHeader("Saved sales", "Public previews you can revisit anytime.") }
    emptyState?.let { state ->
        item { ShopperStatePanel(state.title, state.message, state.actionLabel, onBrowse) }
    }
    items(events, key = { it.id }) { event ->
        MyFindsSavedEventRow(event, layout, { onEventSelected(event.id) }, { onUnsave(event.id) })
    }
}

private fun LazyListScope.rsvpContent(
    groups: List<MyFindsRsvpGroupPresentation>,
    emptyState: MyFindsEmptyPresentation?,
    layout: MyFindsWorkspaceLayout,
    onEventSelected: (String) -> Unit,
    onRequestCancellation: (String) -> Boolean,
    onAddReminder: (String) -> Boolean,
    onExportCalendar: (String) -> Boolean,
    onDirections: (String) -> ExactAddress?,
    onBrowse: () -> Unit,
) {
    item {
        ShopperSectionHeader(
            "RSVP plans",
            "Your exact location remains protected until an RSVP is actively accepted.",
        )
    }
    emptyState?.let { state ->
        item { ShopperStatePanel(state.title, state.message, state.actionLabel, onBrowse) }
    }
    groups.forEach { group ->
        item { Text(group.group.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
        items(group.items, key = { it.eventId }) { item ->
            MyFindsRsvpRow(
                item = item,
                layout = layout,
                onEventSelected = { onEventSelected(item.eventId) },
                onRequestCancellation = { onRequestCancellation(item.eventId) },
                onAddReminder = { onAddReminder(item.eventId) },
                onExportCalendar = { onExportCalendar(item.eventId) },
                onDirections = { onDirections(item.eventId) },
            )
        }
    }
}

@Composable
private fun MyFindsSavedEventRow(
    event: BrowseEventItem,
    layout: MyFindsWorkspaceLayout,
    onEventSelected: () -> Unit,
    onUnsave: () -> Unit,
) = MyFindsEventRow(
    eventId = event.id,
    title = event.title,
    dateLabel = event.dateLabel,
    locationLabel = event.locationLabel,
    statusLabel = event.statusLabel,
    description = event.description,
    photoReference = event.photoReference,
    layout = layout,
) {
    MyFindsActionButton(ShopperRsvpAction.OpenEvent, primary = true, onClick = onEventSelected)
    MyFindsActionButton(ShopperRsvpAction.CancelRsvp, label = "Remove saved", onClick = onUnsave)
}

@Composable
private fun MyFindsRsvpRow(
    item: ShopperRsvpItem,
    layout: MyFindsWorkspaceLayout,
    onEventSelected: () -> Unit,
    onRequestCancellation: () -> Unit,
    onAddReminder: () -> Unit,
    onExportCalendar: () -> Unit,
    onDirections: () -> Unit,
) = MyFindsEventRow(
    eventId = item.eventId,
    title = item.title,
    dateLabel = item.dateLabel,
    locationLabel = item.approximateLocationLabel,
    statusLabel = item.state.label,
    description = item.supportingCopy,
    photoReference = item.photoReference,
    layout = layout,
) {
    if (ShopperRsvpAction.Directions in item.visibleActions) {
        PrivacyNote("Protected location is available for this active accepted RSVP.")
    }
    item.visibleActions.forEach { action ->
        when (action) {
            ShopperRsvpAction.OpenEvent -> MyFindsActionButton(action, primary = true, onClick = onEventSelected)
            ShopperRsvpAction.Directions -> MyFindsActionButton(action, primary = true, onClick = onDirections)
            ShopperRsvpAction.AddReminder -> MyFindsActionButton(
                action,
                label = if (item.reminderAdded) "Reminder added" else action.label,
                onClick = onAddReminder,
            )
            ShopperRsvpAction.ExportCalendar -> MyFindsActionButton(
                action,
                label = if (item.calendarExportPrepared) "Calendar ready" else action.label,
                onClick = onExportCalendar,
            )
            ShopperRsvpAction.CancelRsvp -> MyFindsActionButton(action, onClick = onRequestCancellation)
        }
    }
}

@Composable
private fun MyFindsEventRow(
    eventId: String,
    title: String,
    dateLabel: String,
    locationLabel: String,
    statusLabel: String,
    description: String,
    photoReference: String?,
    layout: MyFindsWorkspaceLayout,
    actions: @Composable () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
        if (layout == MyFindsWorkspaceLayout.Expanded) {
            Row(
                modifier = Modifier.padding(vertical = YardScapeDesign.spacing.medium),
                horizontalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.large),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ShopperEventArtwork(
                    ShopperEventArtworkPresentation(eventId, photoReference),
                    modifier = Modifier.weight(0.8f),
                    height = 196.dp,
                )
                MyFindsEventDetails(title, dateLabel, locationLabel, statusLabel, description, actions, Modifier.weight(1.2f))
            }
        } else {
            Column(
                modifier = Modifier.padding(vertical = YardScapeDesign.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.medium),
            ) {
                ShopperEventArtwork(ShopperEventArtworkPresentation(eventId, photoReference), height = 184.dp)
                MyFindsEventDetails(title, dateLabel, locationLabel, statusLabel, description, actions)
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun MyFindsEventDetails(
    title: String,
    dateLabel: String,
    locationLabel: String,
    statusLabel: String,
    description: String,
    actions: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.small)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.small),
            verticalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.small),
        ) {
            StatusLabel(statusLabel)
            InfoChip(dateLabel)
        }
        Text(locationLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.small),
            verticalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.small),
        ) { actions() }
    }
}

@Composable
private fun MyFindsActionButton(
    action: ShopperRsvpAction,
    label: String = action.label,
    primary: Boolean = false,
    onClick: () -> Unit,
) {
    val modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = label }
    if (primary) {
        Button(modifier = modifier, onClick = onClick) { Text(label) }
    } else {
        OutlinedButton(modifier = modifier, onClick = onClick) { Text(label) }
    }
}

package com.naslabs.yardscape.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag(YardScapeTestTags.MyFindsScreen)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(
                modifier = Modifier.padding(top = 18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("My Finds", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Keep saved public previews and your RSVP plans together.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MyFindsSection.entries.forEach { section ->
                        val selected = state.section == section
                        if (selected) {
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = { onSectionSelected(section) },
                            ) { Text(section.label) }
                        } else {
                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                onClick = { onSectionSelected(section) },
                            ) { Text(section.label) }
                        }
                    }
                }
            }
        }

        when (state.section) {
            MyFindsSection.Saved -> savedItems(state.savedItems, onEventSelected, onUnsave, onBrowse)
            MyFindsSection.Rsvps -> rsvpItems(
                items = state.rsvpItems,
                onEventSelected = onEventSelected,
                onRequestCancellation = onRequestCancellation,
                onAddReminder = onAddReminder,
                onExportCalendar = onExportCalendar,
                onDirections = onDirections,
            )
        }
    }

    if (pendingCancellationEventId != null) {
        AlertDialog(
            onDismissRequest = onDismissCancellation,
            title = { Text("Cancel this RSVP?") },
            text = { Text("Your protected exact-location access will be removed immediately.") },
            confirmButton = {
                Button(onClick = { onConfirmCancellation() }) { Text("Cancel RSVP") }
            },
            dismissButton = {
                TextButton(onClick = onDismissCancellation) { Text("Keep RSVP") }
            },
        )
    }
}

private val MyFindsSection.label: String
    get() = when (this) {
        MyFindsSection.Saved -> "Saved"
        MyFindsSection.Rsvps -> "RSVPs"
    }

private fun androidx.compose.foundation.lazy.LazyListScope.savedItems(
    events: List<BrowseEventItem>,
    onEventSelected: (String) -> Unit,
    onUnsave: (String) -> Unit,
    onBrowse: () -> Unit,
) {
    item { Text("Saved sales", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
    if (events.isEmpty()) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("Nothing saved yet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Browse nearby sales and save the ones you want to revisit.")
                    Button(onClick = onBrowse) { Text("Browse sales") }
                }
            }
        }
    } else {
        items(events, key = { it.id }) { event ->
            EventPreviewCard(
                event = event,
                isSaved = true,
                onClick = { onEventSelected(event.id) },
                onSavedToggle = { onUnsave(event.id) },
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.rsvpItems(
    items: List<ShopperRsvpItem>,
    onEventSelected: (String) -> Unit,
    onRequestCancellation: (String) -> Boolean,
    onAddReminder: (String) -> Boolean,
    onExportCalendar: (String) -> Boolean,
    onDirections: (String) -> ExactAddress?,
) {
    item { Text("RSVPs", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
    if (items.isEmpty()) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text("No RSVPs yet. Open a sale to request attendance.", modifier = Modifier.padding(18.dp))
            }
        }
    } else {
        RsvpGroup.entries.forEach { group ->
            val groupItems = items.filter { it.group == group }
            if (groupItems.isNotEmpty()) {
                item { Text(group.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                items(groupItems, key = { it.eventId }) { item ->
                    MyFindsRsvpCard(
                        item = item,
                        onEventSelected = onEventSelected,
                        onRequestCancellation = onRequestCancellation,
                        onAddReminder = onAddReminder,
                        onExportCalendar = onExportCalendar,
                        onDirections = onDirections,
                    )
                }
            }
        }
    }
}

@Composable
private fun MyFindsRsvpCard(
    item: ShopperRsvpItem,
    onEventSelected: (String) -> Unit,
    onRequestCancellation: (String) -> Boolean,
    onAddReminder: (String) -> Boolean,
    onExportCalendar: (String) -> Boolean,
    onDirections: (String) -> ExactAddress?,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(item.dateLabel)
            Text(item.approximateLocationLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(item.state.label, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            Text(item.state.nextAction)
            if (item.canOpenDirections) {
                PrivacyNote("Protected location is available for this accepted RSVP.")
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onEventSelected(item.eventId) }) { Text("Open event") }
                if (item.canOpenDirections) {
                    Button(onClick = { onDirections(item.eventId) }) { Text("Directions") }
                }
                if (item.canAddReminder) {
                    OutlinedButton(onClick = { onAddReminder(item.eventId) }) {
                        Text(if (item.reminderAdded) "Reminder added" else "Add reminder")
                    }
                }
                if (item.canExportCalendar) {
                    OutlinedButton(onClick = { onExportCalendar(item.eventId) }) {
                        Text(if (item.calendarExportPrepared) "Calendar ready" else "Export calendar")
                    }
                }
            }
            if (item.canCancel) {
                TextButton(onClick = { onRequestCancellation(item.eventId) }) { Text("Cancel RSVP") }
            }
        }
    }
}

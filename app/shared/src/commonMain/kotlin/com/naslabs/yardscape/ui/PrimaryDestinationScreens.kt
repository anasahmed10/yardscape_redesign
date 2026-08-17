package com.naslabs.yardscape.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SavedDestinationScreen(
    events: List<BrowseEventItem>,
    onEventSelected: (String) -> Unit,
    onUnsave: (String) -> Unit,
    onBrowse: () -> Unit,
    onMyRsvps: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag(YardScapeTestTags.SavedScreen)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(
                modifier = Modifier.padding(top = 18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Saved sales", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Saved public previews stay available during this mock session.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(modifier = Modifier.fillMaxWidth(), onClick = onMyRsvps) {
                    Text("My RSVPs")
                }
            }
        }
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
}

@Composable
fun HostDashboardScreen(
    events: List<HostEventItem>,
    onCreateEvent: () -> Unit,
    onEditEvent: (String) -> Unit,
    onManageAttendees: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(
                modifier = Modifier.padding(top = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Host tools", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Create and manage your sales in a workspace separated from public shopper previews.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(modifier = Modifier.fillMaxWidth(), onClick = onCreateEvent) {
                    Text("Create a sale")
                }
            }
        }
        items(events, key = { it.id }) { event ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEditEvent(event.id) },
                shape = MaterialTheme.shapes.small,
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(event.title, fontWeight = FontWeight.SemiBold)
                    Text("${event.statusLabel} • ${event.dateLabel}")
                    Text(
                        event.publicLocationLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onManageAttendees(event.id) },
                    ) { Text("Manage attendees") }
                }
            }
        }
    }
}

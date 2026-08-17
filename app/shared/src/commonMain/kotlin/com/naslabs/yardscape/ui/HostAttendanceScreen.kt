package com.naslabs.yardscape.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun HostAttendanceScreen(
    state: HostAttendanceState?,
    pendingAction: PendingHostAttendeeAction?,
    onBack: () -> Unit,
    onRequestAction: (eventId: String, shopperId: String, action: HostAttendeeAction) -> Boolean,
    onDismissAction: () -> Unit,
    onConfirmAction: () -> Boolean,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(Modifier.padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onBack) { Text("Back to Host") }
                Text("Manage attendees", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(state?.eventTitle ?: "Event unavailable", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (state != null) {
            item { HostAttendanceSummary(state) }
            if (state.attendees.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("No RSVPs yet", fontWeight = FontWeight.Bold)
                            Text("New attendance requests will appear here. No attendee information is shown publicly.")
                        }
                    }
                }
            } else {
                items(state.attendees, key = { it.shopperId }) { attendee ->
                    HostAttendeeCard(
                        eventId = state.eventId,
                        attendee = attendee,
                        atCapacity = state.isAtCapacity,
                        onRequestAction = onRequestAction,
                    )
                }
            }
            item {
                PrivacyNote("Review requests one at a time. Bulk location grants are intentionally unavailable because every grant exposes sensitive location data.")
            }
        }
    }

    pendingAction?.let { pending ->
        AlertDialog(
            onDismissRequest = onDismissAction,
            title = { Text(pending.action.confirmationTitle) },
            text = { Text("${pending.attendeeName}: ${pending.action.consequence}") },
            confirmButton = {
                Button(onClick = { onConfirmAction() }) { Text(pending.action.label) }
            },
            dismissButton = {
                TextButton(onClick = onDismissAction) { Text("Go back") }
            },
        )
    }
}

@Composable
private fun HostAttendanceSummary(state: HostAttendanceState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Attendance summary", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("${state.acceptedCount} accepted · ${state.requestedCount} awaiting review")
            Text("${state.activeLocationAccessCount} shoppers currently have exact-location access")
            Text(state.policy.approvalMode.label)
            Text(state.policy.attendeeCap?.let { "Attendee cap: $it" } ?: "No attendee cap")
            if (state.isAtCapacity) {
                Text(
                    "Capacity reached. Remove an attendee before accepting someone new. Revoking location alone does not free a spot.",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun HostAttendeeCard(
    eventId: String,
    attendee: HostAttendeeItem,
    atCapacity: Boolean,
    onRequestAction: (eventId: String, shopperId: String, action: HostAttendeeAction) -> Boolean,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(attendee.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(attendee.state.label, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            Text(attendee.state.guidance)
            Text(
                if (attendee.hasLocationAccess) "Exact-location access: Active" else "Exact-location access: None",
                fontWeight = FontWeight.Medium,
            )
            if (attendee.availableActions.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    attendee.availableActions.forEach { action ->
                        OutlinedButton(
                            enabled = action != HostAttendeeAction.Accept || !atCapacity,
                            onClick = { onRequestAction(eventId, attendee.shopperId, action) },
                        ) { Text(action.label) }
                    }
                }
            }
        }
    }
}

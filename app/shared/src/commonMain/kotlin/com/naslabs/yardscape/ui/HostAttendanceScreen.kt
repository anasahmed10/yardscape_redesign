package com.naslabs.yardscape.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun HostAttendanceScreen(
    state: HostAttendanceState?,
    pendingAction: PendingHostAttendeeAction?,
    onBack: () -> Unit,
    onRequestAction: (eventId: String, shopperId: String, action: HostAttendeeAction) -> Boolean,
    onMessageAttendee: suspend (eventId: String, shopperId: String) -> Boolean,
    onDismissAction: () -> Unit,
    onConfirmAction: () -> Boolean,
) {
    val coroutineScope = rememberCoroutineScope()
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 960.dp)
                    .fillMaxWidth()
                    .padding(horizontal = YardScapeDesign.spacing.large),
                verticalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.medium),
            ) {
                item { HostAttendanceHeader(state, onBack) }
                if (state != null) {
                    item { HostAttendanceSummary(state) }
                    if (state.attendeeRows.isEmpty()) {
                        item {
                            ShopperStatePanel(
                                title = "No RSVPs yet",
                                message = "New attendance requests will appear here. No attendee information is shown publicly.",
                            )
                        }
                    } else {
                        items(state.attendeeRows, key = { it.shopperId }) { attendee ->
                            HostAttendeeRow(
                                eventId = state.eventId,
                                attendee = attendee,
                                atCapacity = state.isAtCapacity,
                                onRequestAction = onRequestAction,
                                onMessageAttendee = {
                                    coroutineScope.launch { onMessageAttendee(state.eventId, attendee.shopperId) }
                                },
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
                        }
                    }
                    item {
                        PrivacyNote(
                            "Review requests one at a time. Bulk location grants are intentionally unavailable because every grant exposes sensitive location data.",
                        )
                    }
                }
            }
        }
    }

    pendingAction?.let { pending ->
        AlertDialog(
            onDismissRequest = onDismissAction,
            title = { Text(pending.action.confirmationTitle) },
            text = { Text("${pending.attendeeName}: ${pending.action.consequence}") },
            confirmButton = { Button(onClick = { onConfirmAction() }) { Text(pending.action.label) } },
            dismissButton = { TextButton(onClick = onDismissAction) { Text("Go back") } },
        )
    }
}

@Composable
private fun HostAttendanceHeader(state: HostAttendanceState?, onBack: () -> Unit) {
    Column(
        modifier = Modifier.padding(top = YardScapeDesign.spacing.large),
        verticalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.small),
    ) {
        TextButton(onClick = onBack) { Text("Back to Host") }
        state?.let { attendance ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.medium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ShopperEventArtwork(
                    presentation = ShopperEventArtworkPresentation(attendance.eventId, attendance.eventPhoto?.url),
                    modifier = Modifier.width(88.dp).height(88.dp),
                    height = 88.dp,
                )
                Column(verticalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.extraSmall)) {
                    Text("Manage attendees", style = MaterialTheme.typography.headlineMedium)
                    Text(attendance.eventTitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "Exact location stays protected until RSVP access is active.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } ?: run {
            Text("Manage attendees", style = MaterialTheme.typography.headlineMedium)
            Text("Event unavailable", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HostAttendanceSummary(state: HostAttendanceState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f),
    ) {
        Column(
            modifier = Modifier.padding(YardScapeDesign.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.small),
        ) {
            Text("Attendance summary", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.medium)) {
                state.summaryMetrics.forEach { metric ->
                    Column {
                        Text(metric.value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(metric.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (state.isAtCapacity) {
                Text(
                    "Capacity reached. Remove an attendee before accepting someone new. Revoking location alone does not free a spot.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun HostAttendeeRow(
    eventId: String,
    attendee: HostAttendeeItem,
    atCapacity: Boolean,
    onRequestAction: (eventId: String, shopperId: String, action: HostAttendeeAction) -> Boolean,
    onMessageAttendee: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = YardScapeDesign.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.small),
    ) {
        Text(attendee.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(attendee.state.label, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        Text(attendee.exactAccessLabel, style = MaterialTheme.typography.bodyMedium)
        Text(attendee.state.guidance, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.small)) {
            attendee.availableActions.forEach { action ->
                OutlinedButton(
                    modifier = Modifier.heightIn(min = 48.dp),
                    enabled = action != HostAttendeeAction.Accept || !atCapacity,
                    onClick = { onRequestAction(eventId, attendee.shopperId, action) },
                ) { Text(action.label) }
            }
            if (attendee.canMessageAttendee) {
                Button(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .semantics { contentDescription = "Message ${attendee.displayName}" },
                    onClick = onMessageAttendee,
                ) { Text("Message attendee") }
            }
        }
    }
}

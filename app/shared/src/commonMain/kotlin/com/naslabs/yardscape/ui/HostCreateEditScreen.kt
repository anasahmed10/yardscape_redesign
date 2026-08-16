package com.naslabs.yardscape.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.naslabs.yardscape.data.HostEventDraft
import com.naslabs.yardscape.data.MapSelectedLocation
import com.naslabs.yardscape.data.SeededYardSaleData
import com.naslabs.yardscape.data.withMapSelectedLocation

@Composable
fun HostCreateEditScreen(
    hostEvents: List<HostEventItem>,
    editorState: HostEditorState,
    onAddressSearch: (String) -> List<MapSelectedLocation>,
    onDraftChanged: (HostEventDraft) -> Unit,
    onNew: () -> Unit,
    onEdit: (String) -> Unit,
    onSaveDraft: () -> Unit,
    onPublish: () -> Unit,
    onCancelEvent: () -> Unit,
    onHideEvent: () -> Unit,
    onBack: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(
                modifier = Modifier.padding(top = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(onClick = onBack) {
                    Text("Back")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = "Host events",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = ForestInk,
                        )
                        Text(
                            text = "Draft, publish, and protect location access.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    OutlinedButton(onClick = onNew) {
                        Text("New")
                    }
                }
            }
        }

        item {
            HostEventForm(
                state = editorState,
                onAddressSearch = onAddressSearch,
                onDraftChanged = onDraftChanged,
                onSaveDraft = onSaveDraft,
                onPublish = onPublish,
                onCancelEvent = onCancelEvent,
                onHideEvent = onHideEvent,
            )
        }

        item {
            FormSectionLabel("Your listings")
        }

        items(hostEvents, key = { it.id }) { event ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEdit(event.id) },
                shape = RoundedCornerShape(8.dp),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(event.title, fontWeight = FontWeight.SemiBold)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        StatusLabel(text = event.statusLabel)
                        InfoChip(text = event.dateLabel)
                    }
                    Text(
                        text = event.publicLocationLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun HostEventForm(
    state: HostEditorState,
    onAddressSearch: (String) -> List<MapSelectedLocation>,
    onDraftChanged: (HostEventDraft) -> Unit,
    onSaveDraft: () -> Unit,
    onPublish: () -> Unit,
    onCancelEvent: () -> Unit,
    onHideEvent: () -> Unit,
) {
    val draft = state.draft
    Column(
        modifier = Modifier.padding(bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Create / Edit",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = ForestInk,
        )
        if (state.validationErrors.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    state.validationErrors.forEach { error ->
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }
        }

        FormSectionLabel("Listing basics")
        HostTextField("Title", draft.title) { onDraftChanged(draft.copy(title = it)) }
        HostTextField("Description", draft.description) { onDraftChanged(draft.copy(description = it)) }

        MapLocationPicker(
            selectedLocation = draft.selectedMapLocation,
            onAddressSearch = onAddressSearch,
            onLocationSelected = { location ->
                onDraftChanged(draft.withMapSelectedLocation(location))
            },
        )
        HostTextField("Access instructions", draft.accessInstructions.orEmpty()) {
            onDraftChanged(draft.copy(accessInstructions = it.ifBlank { null }))
        }

        FormSectionLabel("Sale schedule")
        HostTimePickerField(
            label = "Start time",
            value = draft.startsAtEpochMillis,
            fallbackValue = draft.endsAtEpochMillis
                ?: SeededYardSaleData.BASE_NOW_EPOCH_MILLIS + HOST_FORM_DEFAULT_DAY_OFFSET_MILLIS,
            onTimeSelected = { start ->
                onDraftChanged(draft.copy(startsAtEpochMillis = start))
            },
        )
        HostTimePickerField(
            label = "End time",
            value = draft.endsAtEpochMillis,
            fallbackValue = draft.startsAtEpochMillis
                ?: SeededYardSaleData.BASE_NOW_EPOCH_MILLIS + HOST_FORM_DEFAULT_DAY_OFFSET_MILLIS,
            onTimeSelected = { selectedEnd ->
                val start = draft.startsAtEpochMillis
                val end = if (start != null && selectedEnd <= start) {
                    selectedEnd + HOST_FORM_DEFAULT_DAY_OFFSET_MILLIS
                } else {
                    selectedEnd
                }
                onDraftChanged(draft.copy(endsAtEpochMillis = end))
            },
        )

        FormSectionLabel("Sale details")
        HostTextField("Categories", draft.categories.joinToString(", ")) {
            onDraftChanged(draft.copy(categories = it.toCsvList()))
        }
        HostTextField("Payment notes", draft.acceptedPaymentTypes.joinToString(", ")) {
            onDraftChanged(draft.copy(acceptedPaymentTypes = it.toCsvList()))
        }
        HostTextField("Accessibility notes", draft.accessibilityNotes.joinToString(", ")) {
            onDraftChanged(draft.copy(accessibilityNotes = it.toCsvList()))
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(modifier = Modifier.fillMaxWidth(), onClick = onSaveDraft) {
                Text("Save draft")
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onPublish,
                colors = ButtonDefaults.buttonColors(containerColor = Clay),
            ) {
                Text("Publish")
            }
        }
        if (state.savedEventId != null) {
            OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onHideEvent) {
                Text("Hide from search")
            }
            OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onCancelEvent) {
                Text("Cancel event")
            }
        }
    }
}

@Composable
private fun MapLocationPicker(
    selectedLocation: MapSelectedLocation?,
    onAddressSearch: (String) -> List<MapSelectedLocation>,
    onLocationSelected: (MapSelectedLocation) -> Unit,
) {
    var addressQuery by remember(selectedLocation?.providerPlaceId) {
        mutableStateOf(selectedLocation?.formattedAddress.orEmpty())
    }
    val autocompleteResults = remember(addressQuery) {
        onAddressSearch(addressQuery)
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FormSectionLabel("Map location")
        PrivacyNote("Search for the sale address with Maps autocomplete. Shoppers only see the approximate area until RSVP access is granted.")
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp,
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = selectedLocation?.displayName ?: "No map location selected",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ForestInk,
                )
                Text(
                    text = selectedLocation?.publicAreaDescription
                        ?: "Pick a Maps result to fill the private address, coordinates, city, region, postal code, and public area.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (selectedLocation != null) {
                    InfoChip(text = "${selectedLocation.publicNeighborhood} - ${selectedLocation.city}")
                }

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = addressQuery,
                    onValueChange = { addressQuery = it },
                    label = { Text("Search address") },
                    placeholder = { Text("Start typing a street address") },
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                )

                when {
                    addressQuery.trim().length < 3 -> Text(
                        text = "Enter at least 3 characters to search.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    autocompleteResults.isEmpty() -> Text(
                        text = "No address matches found.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    else -> autocompleteResults.forEach { location ->
                        MapLocationSuggestionButton(
                            location = location,
                            onLocationSelected = {
                                addressQuery = location.formattedAddress
                                onLocationSelected(location)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MapLocationSuggestionButton(
    location: MapSelectedLocation,
    onLocationSelected: (MapSelectedLocation) -> Unit,
) {
    OutlinedButton(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onLocationSelected(location) },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = location.displayName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = location.formattedAddress,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HostTimePickerField(
    label: String,
    value: Long?,
    fallbackValue: Long,
    onTimeSelected: (Long) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }
    val displayValue = value?.toHostClockTimeLabel() ?: "Select time"
    OutlinedButton(
        modifier = Modifier.fillMaxWidth(),
        onClick = { showPicker = true },
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = displayValue,
                    style = MaterialTheme.typography.bodyLarge,
                    color = ForestInk,
                )
            }
            Text("Change", style = MaterialTheme.typography.labelLarge)
        }
    }

    if (showPicker) {
        HostTimePickerDialog(
            title = label,
            initialEpochMillis = value ?: fallbackValue,
            onDismiss = { showPicker = false },
            onConfirm = { selectedHour, selectedMinute ->
                val anchor = value ?: fallbackValue
                onTimeSelected(anchor.withClockTime(selectedHour, selectedMinute))
                showPicker = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HostTimePickerDialog(
    title: String,
    initialEpochMillis: Long,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialEpochMillis.hostHourOfDay(),
        initialMinute = initialEpochMillis.hostMinuteOfHour(),
        is24Hour = false,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            TimePicker(state = timePickerState)
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(timePickerState.hour, timePickerState.minute)
                },
            ) {
                Text("Set")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun HostTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        shape = RoundedCornerShape(8.dp),
    )
}

private fun String.toCsvList(): List<String> =
    split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }

private fun Long.hostHourOfDay(): Int =
    (floorMod(HOST_FORM_DEFAULT_DAY_OFFSET_MILLIS) / HOST_FORM_MILLIS_PER_HOUR).toInt()

private fun Long.hostMinuteOfHour(): Int =
    ((floorMod(HOST_FORM_DEFAULT_DAY_OFFSET_MILLIS) % HOST_FORM_MILLIS_PER_HOUR) / HOST_FORM_MILLIS_PER_MINUTE).toInt()

private fun Long.withClockTime(hour: Int, minute: Int): Long {
    val dayStart = this - floorMod(HOST_FORM_DEFAULT_DAY_OFFSET_MILLIS)
    return dayStart + hour * HOST_FORM_MILLIS_PER_HOUR + minute * HOST_FORM_MILLIS_PER_MINUTE
}

private fun Long.floorMod(other: Long): Long {
    val mod = this % other
    return if (mod < 0) mod + other else mod
}

private const val HOST_FORM_MILLIS_PER_MINUTE = 60L * 1_000L
private const val HOST_FORM_MILLIS_PER_HOUR = 60L * HOST_FORM_MILLIS_PER_MINUTE
private const val HOST_FORM_DEFAULT_DAY_OFFSET_MILLIS = 24L * 60L * 60L * 1_000L

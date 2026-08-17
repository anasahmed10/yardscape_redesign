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
import com.naslabs.yardscape.domain.EventPhoto

@Composable
fun HostCreateEditScreen(
    hostEvents: List<HostEventItem>,
    editorState: HostEditorState,
    availablePhotos: List<EventPhoto>,
    onAddressSearch: (String) -> List<MapSelectedLocation>,
    onEditorStateChanged: (HostEditorState) -> Unit,
    onStepSelected: (HostEditorStep) -> Unit,
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
                availablePhotos = availablePhotos,
                onAddressSearch = onAddressSearch,
                onEditorStateChanged = onEditorStateChanged,
                onStepSelected = onStepSelected,
                onSaveDraft = onSaveDraft,
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

    editorState.pendingConfirmation?.let { action ->
        AlertDialog(
            onDismissRequest = {
                onEditorStateChanged(editorState.dismissConfirmation())
            },
            title = { Text(action.title) },
            text = { Text(action.message) },
            confirmButton = {
                Button(
                    onClick = {
                        when (action) {
                            HostConfirmationAction.Publish -> onPublish()
                            HostConfirmationAction.Hide -> onHideEvent()
                            HostConfirmationAction.Cancel -> onCancelEvent()
                        }
                    },
                ) { Text(action.confirmLabel) }
            },
            dismissButton = {
                TextButton(
                    onClick = { onEditorStateChanged(editorState.dismissConfirmation()) },
                ) { Text("Go back") }
            },
        )
    }
}

@Composable
private fun HostEventForm(
    state: HostEditorState,
    availablePhotos: List<EventPhoto>,
    onAddressSearch: (String) -> List<MapSelectedLocation>,
    onEditorStateChanged: (HostEditorState) -> Unit,
    onStepSelected: (HostEditorStep) -> Unit,
    onSaveDraft: () -> Unit,
) {
    val draft = state.draft
    fun updateDraft(updated: HostEventDraft) {
        onEditorStateChanged(state.copy(draft = updated, validationErrors = emptyList()))
    }
    Column(
        modifier = Modifier.padding(bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "${state.step.ordinal + 1} of ${HostEditorStep.entries.size}: ${state.step.label}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = ForestInk,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            HostEditorStep.entries.forEach { step ->
                if (step == state.step) {
                    Button(onClick = { onStepSelected(step) }) { Text(step.label) }
                } else {
                    OutlinedButton(onClick = { onStepSelected(step) }) { Text(step.label) }
                }
            }
        }
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

        when (state.step) {
            HostEditorStep.Basics -> {
                HostTextField("Title", draft.title) { updateDraft(draft.copy(title = it)) }
                HostTextField("Description", draft.description) { updateDraft(draft.copy(description = it)) }
            }
            HostEditorStep.Schedule -> {
                HostTimePickerField(
                    "Start time",
                    draft.startsAtEpochMillis,
                    draft.endsAtEpochMillis ?: SeededYardSaleData.BASE_NOW_EPOCH_MILLIS + HOST_FORM_DEFAULT_DAY_OFFSET_MILLIS,
                ) { updateDraft(draft.copy(startsAtEpochMillis = it)) }
                HostTimePickerField(
                    "End time",
                    draft.endsAtEpochMillis,
                    draft.startsAtEpochMillis ?: SeededYardSaleData.BASE_NOW_EPOCH_MILLIS + HOST_FORM_DEFAULT_DAY_OFFSET_MILLIS,
                ) { selectedEnd ->
                    val start = draft.startsAtEpochMillis
                    updateDraft(draft.copy(endsAtEpochMillis = if (start != null && selectedEnd <= start) selectedEnd + HOST_FORM_DEFAULT_DAY_OFFSET_MILLIS else selectedEnd))
                }
            }
            HostEditorStep.Location -> {
                MapLocationPicker(
                    selectedLocation = draft.selectedMapLocation,
                    onAddressSearch = onAddressSearch,
                    onLocationSelected = { updateDraft(draft.withMapSelectedLocation(it)) },
                )
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Protected location details", fontWeight = FontWeight.Bold)
                        Text("These fields never appear in the public preview.")
                        Text(draft.exactStreetAddress.ifBlank { "Select a map address" })
                        HostTextField("Private access instructions", draft.accessInstructions.orEmpty()) {
                            updateDraft(draft.copy(accessInstructions = it.ifBlank { null }))
                        }
                    }
                }
            }
            HostEditorStep.SaleDetails -> {
                HostTextField("Categories", draft.categories.joinToString(", ")) { updateDraft(draft.copy(categories = it.toCsvList())) }
                HostTextField("Payment notes", draft.acceptedPaymentTypes.joinToString(", ")) { updateDraft(draft.copy(acceptedPaymentTypes = it.toCsvList())) }
                HostTextField("Accessibility notes", draft.accessibilityNotes.joinToString(", ")) { updateDraft(draft.copy(accessibilityNotes = it.toCsvList())) }
            }
            HostEditorStep.Photos -> HostPhotosStep(draft, availablePhotos, ::updateDraft)
            HostEditorStep.RsvpSettings -> HostRsvpSettingsStep(state, onEditorStateChanged)
            HostEditorStep.Preview -> HostPreviewStep(
                state = state,
                onSaveDraft = onSaveDraft,
                onRequestConfirmation = { onEditorStateChanged(state.requestConfirmation(it)) },
            )
        }

        if (state.step != HostEditorStep.Preview) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.step.ordinal > 0) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = { onStepSelected(HostEditorStep.entries[state.step.ordinal - 1]) },
                    ) { Text("Back") }
                }
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = { onStepSelected(HostEditorStep.entries[state.step.ordinal + 1]) },
                ) { Text("Continue") }
            }
            TextButton(onClick = onSaveDraft) { Text("Save draft and leave later") }
        }
    }
}

@Composable
private fun HostPhotosStep(
    draft: HostEventDraft,
    availablePhotos: List<EventPhoto>,
    onDraftChanged: (HostEventDraft) -> Unit,
) {
    FormSectionLabel("Mock photo picker")
    Text(
        "Choose seeded photos now; a platform photo service can replace this picker later.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    availablePhotos.filter { candidate -> draft.photos.none { it.url == candidate.url } }.forEach { photo ->
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onDraftChanged(draft.copy(photos = draft.photos + photo)) },
        ) { Text("Add ${photo.description ?: "photo"}") }
    }
    draft.photos.forEachIndexed { index, photo ->
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Photo ${index + 1}", fontWeight = FontWeight.Bold)
                HostTextField("Caption", photo.description.orEmpty()) { caption ->
                    onDraftChanged(draft.copy(photos = draft.photos.replaceAt(index, photo.copy(description = caption.ifBlank { null }))))
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        enabled = index > 0,
                        onClick = { onDraftChanged(draft.copy(photos = draft.photos.move(index, index - 1))) },
                    ) { Text("Move up") }
                    OutlinedButton(
                        enabled = index < draft.photos.lastIndex,
                        onClick = { onDraftChanged(draft.copy(photos = draft.photos.move(index, index + 1))) },
                    ) { Text("Move down") }
                    TextButton(
                        onClick = { onDraftChanged(draft.copy(photos = draft.photos.filterIndexed { itemIndex, _ -> itemIndex != index })) },
                    ) { Text("Remove") }
                }
            }
        }
    }
}

@Composable
private fun HostRsvpSettingsStep(
    state: HostEditorState,
    onEditorStateChanged: (HostEditorState) -> Unit,
) {
    FormSectionLabel("Attendance policy")
    HostTextField("Attendee cap (optional)", state.attendeeCapInput) { input ->
        onEditorStateChanged(
            state.copy(
                attendeeCapInput = input,
                validationErrors = emptyList(),
            ),
        )
    }
    HostRsvpApprovalMode.entries.forEach { mode ->
        if (mode == state.approvalMode) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onEditorStateChanged(state.copy(approvalMode = mode, validationErrors = emptyList())) },
            ) { Text(mode.label) }
        } else {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onEditorStateChanged(state.copy(approvalMode = mode, validationErrors = emptyList())) },
            ) { Text(mode.label) }
        }
    }
    PrivacyNote("Attendee limits and manual approval are mock state for backend contract review; exact location remains protected until acceptance.")
}

@Composable
private fun HostPreviewStep(
    state: HostEditorState,
    onSaveDraft: () -> Unit,
    onRequestConfirmation: (HostConfirmationAction) -> Unit,
) {
    val preview = state.publicPreview()
    FormSectionLabel("Public shopper preview")
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(preview.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(preview.description)
            Text(preview.scheduleLabel)
            Text(preview.approximateLocationLabel)
            if (preview.categories.isNotEmpty()) Text(preview.categories.joinToString(" · "))
            preview.photoCaptions.forEach { Text("Photo: $it") }
            Text(preview.rsvpSummary, color = MaterialTheme.colorScheme.onSurfaceVariant)
            PrivacyNote("Preview contains approximate location only. Protected address and access instructions are omitted.")
        }
    }
    Button(modifier = Modifier.fillMaxWidth(), onClick = onSaveDraft) { Text("Save draft") }
    Button(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onRequestConfirmation(HostConfirmationAction.Publish) },
        colors = ButtonDefaults.buttonColors(containerColor = Clay),
    ) { Text("Review and publish") }
    if (state.savedEventId != null) {
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onRequestConfirmation(HostConfirmationAction.Hide) },
        ) { Text("Hide from search") }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onRequestConfirmation(HostConfirmationAction.Cancel) },
        ) { Text("Cancel event") }
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

private fun <T> List<T>.replaceAt(index: Int, value: T): List<T> =
    mapIndexed { itemIndex, item -> if (itemIndex == index) value else item }

private fun <T> List<T>.move(fromIndex: Int, toIndex: Int): List<T> {
    if (fromIndex !in indices || toIndex !in indices) return this
    val mutable = toMutableList()
    val item = mutable.removeAt(fromIndex)
    mutable.add(toIndex, item)
    return mutable
}

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

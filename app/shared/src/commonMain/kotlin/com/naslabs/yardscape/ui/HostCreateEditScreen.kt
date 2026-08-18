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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
    editorState: HostEditorState,
    nowEpochMillis: Long,
    availablePhotos: List<EventPhoto>,
    onAddressSearch: (String) -> List<MapSelectedLocation>,
    onEditorStateChanged: (HostEditorState) -> Unit,
    onStepSelected: (HostEditorStep) -> Unit,
    onNew: () -> Unit,
    onSaveDraft: () -> Unit,
    onPublish: () -> Unit,
    onCancelEvent: () -> Unit,
    onHideEvent: () -> Unit,
    onBack: () -> Unit,
) {
    val spacing = YardScapeDesign.spacing
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpanded = hostMarketplaceLayoutFor(maxWidth) == HostMarketplaceLayout.Expanded
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 1120.dp)
                    .fillMaxWidth()
                    .padding(horizontal = spacing.large),
                verticalArrangement = Arrangement.spacedBy(spacing.medium),
            ) {
                item {
                    Column(
                        modifier = Modifier.padding(top = spacing.large),
                        verticalArrangement = Arrangement.spacedBy(spacing.small),
                    ) {
                        TextButton(
                            modifier = Modifier.yardScapeInteractiveTarget(),
                            onClick = onBack,
                        ) { Text("Back to your sales") }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = if (editorState.savedEventId == null) "Create a sale" else "Edit sale",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "Build the public preview, then keep exact location access protected.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            OutlinedButton(
                                modifier = Modifier.yardScapeInteractiveTarget(),
                                onClick = onNew,
                            ) { Text("New sale") }
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
                        isExpanded = isExpanded,
                        nowEpochMillis = nowEpochMillis,
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
                    modifier = Modifier.yardScapeInteractiveTarget(),
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
                    modifier = Modifier.yardScapeInteractiveTarget(),
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
    isExpanded: Boolean,
    nowEpochMillis: Long,
) {
    val draft = state.draft
    fun updateDraft(updated: HostEventDraft) {
        onEditorStateChanged(state.copy(draft = updated, validationErrors = emptyList()))
    }
    Column(
        modifier = Modifier.padding(bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.medium),
    ) {
        HostEditorProgressIndicator(state.progress, onStepSelected)
        if (state.validationErrors.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.errorContainer,
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

        HostEditorStepContent(
            state = state,
            availablePhotos = availablePhotos,
            onAddressSearch = onAddressSearch,
            onDraftChanged = ::updateDraft,
            onEditorStateChanged = onEditorStateChanged,
            onSaveDraft = onSaveDraft,
            isExpanded = isExpanded,
            nowEpochMillis = nowEpochMillis,
        )

        if (state.progress.previousStep != null || state.step != HostEditorStep.Preview) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.progress.previousStep?.let { previousStep ->
                    OutlinedButton(
                        modifier = Modifier.weight(1f).yardScapeInteractiveTarget(),
                        onClick = { onStepSelected(previousStep) },
                    ) { Text("Back") }
                }
                if (state.step != HostEditorStep.Preview) {
                    Button(
                        modifier = Modifier.weight(1f).yardScapeInteractiveTarget(),
                        onClick = { onStepSelected(HostEditorStep.entries[state.step.ordinal + 1]) },
                    ) { Text("Continue") }
                }
            }
            if (state.step != HostEditorStep.Preview) {
                TextButton(modifier = Modifier.yardScapeInteractiveTarget(), onClick = onSaveDraft) { Text("Save draft and leave later") }
            }
        }
    }
}

@Composable
private fun HostEditorProgressIndicator(
    progress: HostEditorProgress,
    onStepSelected: (HostEditorStep) -> Unit,
) {
    Column(
        modifier = Modifier.semantics {
            contentDescription = "Step ${progress.currentStep} of ${progress.totalSteps}: ${progress.activeStep.label}"
        },
        verticalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.extraSmall),
    ) {
        Text(
            text = "Step ${progress.currentStep} of ${progress.totalSteps}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(progress.activeStep.label, style = MaterialTheme.typography.titleLarge, color = ForestInk)
        LinearProgressIndicator(
            progress = { progress.currentStep.toFloat() / progress.totalSteps },
            modifier = Modifier.fillMaxWidth(),
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.extraSmall),
            verticalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.extraSmall),
        ) {
            HostEditorStep.entries.forEach { step ->
                when {
                    step.ordinal < progress.activeStep.ordinal -> TextButton(
                        modifier = Modifier.yardScapeInteractiveTarget(),
                        onClick = { onStepSelected(step) },
                    ) { Text(step.label) }
                    step == progress.activeStep -> Text(step.label, style = MaterialTheme.typography.labelLarge)
                    else -> Text(step.label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun HostEditorStepContent(
    state: HostEditorState,
    availablePhotos: List<EventPhoto>,
    onAddressSearch: (String) -> List<MapSelectedLocation>,
    onDraftChanged: (HostEventDraft) -> Unit,
    onEditorStateChanged: (HostEditorState) -> Unit,
    onSaveDraft: () -> Unit,
    isExpanded: Boolean,
    nowEpochMillis: Long,
) {
    val draft = state.draft
    when (state.step) {
        HostEditorStep.Basics -> {
            HostTextField("Title", draft.title) { onDraftChanged(draft.copy(title = it)) }
            HostTextField("Description", draft.description) { onDraftChanged(draft.copy(description = it)) }
        }
        HostEditorStep.Schedule -> {
            HostTimePickerField("Start time", draft.startsAtEpochMillis, draft.endsAtEpochMillis ?: SeededYardSaleData.BASE_NOW_EPOCH_MILLIS + HOST_FORM_DEFAULT_DAY_OFFSET_MILLIS) {
                onDraftChanged(draft.copy(startsAtEpochMillis = it))
            }
            HostTimePickerField("End time", draft.endsAtEpochMillis, draft.startsAtEpochMillis ?: SeededYardSaleData.BASE_NOW_EPOCH_MILLIS + HOST_FORM_DEFAULT_DAY_OFFSET_MILLIS) { selectedEnd ->
                val start = draft.startsAtEpochMillis
                onDraftChanged(draft.copy(endsAtEpochMillis = if (start != null && selectedEnd <= start) selectedEnd + HOST_FORM_DEFAULT_DAY_OFFSET_MILLIS else selectedEnd))
            }
        }
        HostEditorStep.Location -> {
            MapLocationPicker(draft.selectedMapLocation, onAddressSearch) { onDraftChanged(draft.withMapSelectedLocation(it)) }
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Protected location details", style = MaterialTheme.typography.titleMedium)
                    Text("Only you and accepted attendees can use these details. They never appear in the shopper preview.")
                    Text(draft.exactStreetAddress.ifBlank { "Select a map address" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HostTextField("Private access instructions", draft.accessInstructions.orEmpty()) {
                        onDraftChanged(draft.copy(accessInstructions = it.ifBlank { null }))
                    }
                }
            }
        }
        HostEditorStep.SaleDetails -> {
            HostTextField("Categories", draft.categories.joinToString(", ")) { onDraftChanged(draft.copy(categories = it.toCsvList())) }
            HostTextField("Payment notes", draft.acceptedPaymentTypes.joinToString(", ")) { onDraftChanged(draft.copy(acceptedPaymentTypes = it.toCsvList())) }
            HostTextField("Accessibility notes", draft.accessibilityNotes.joinToString(", ")) { onDraftChanged(draft.copy(accessibilityNotes = it.toCsvList())) }
        }
        HostEditorStep.Photos -> HostPhotosStep(draft, availablePhotos, onDraftChanged)
        HostEditorStep.RsvpSettings -> HostRsvpSettingsStep(state, onEditorStateChanged)
        HostEditorStep.Preview -> HostPreviewStep(
            state = state,
            onSaveDraft = onSaveDraft,
            onRequestConfirmation = { onEditorStateChanged(state.requestConfirmation(it)) },
            isExpanded = isExpanded,
            nowEpochMillis = nowEpochMillis,
        )
    }
}

@Composable
private fun HostPhotosStep(
    draft: HostEventDraft,
    availablePhotos: List<EventPhoto>,
    onDraftChanged: (HostEventDraft) -> Unit,
) {
    FormSectionLabel("Sale photos")
    Text(
        "Choose the public photos shoppers will see. The first photo becomes the listing cover.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    availablePhotos.filter { candidate -> draft.photos.none { it.url == candidate.url } }.forEach { photo ->
        OutlinedButton(
            modifier = Modifier.fillMaxWidth().yardScapeInteractiveTarget(),
            onClick = { onDraftChanged(draft.copy(photos = draft.photos + photo)) },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HostPhotoArtwork(photo = photo, draftId = draft.id, size = 56.dp)
                Text("Add ${photo.description ?: "photo"}")
            }
        }
    }
    if (draft.photos.isEmpty()) {
        ShopperStatePanel(
            title = "Add a cover photo",
            message = "A clear photo helps shoppers recognize the sale preview.",
        )
    }
    draft.photos.forEachIndexed { index, photo ->
        Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.small), verticalAlignment = Alignment.CenterVertically) {
                    HostPhotoArtwork(photo = photo, draftId = draft.id, size = 72.dp)
                    Column {
                        Text(if (index == 0) "Cover photo" else "Photo ${index + 1}", style = MaterialTheme.typography.titleMedium)
                        Text("Shown in the public shopper preview", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                }
                HostTextField("Caption", photo.description.orEmpty()) { caption ->
                    onDraftChanged(draft.copy(photos = draft.photos.replaceAt(index, photo.copy(description = caption.ifBlank { null }))))
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        modifier = Modifier.yardScapeInteractiveTarget(),
                        enabled = index > 0,
                        onClick = { onDraftChanged(draft.copy(photos = draft.photos.move(index, index - 1))) },
                    ) { Text("Move up") }
                    OutlinedButton(
                        modifier = Modifier.yardScapeInteractiveTarget(),
                        enabled = index < draft.photos.lastIndex,
                        onClick = { onDraftChanged(draft.copy(photos = draft.photos.move(index, index + 1))) },
                    ) { Text("Move down") }
                    TextButton(
                        modifier = Modifier.yardScapeInteractiveTarget(),
                        onClick = { onDraftChanged(draft.copy(photos = draft.photos.filterIndexed { itemIndex, _ -> itemIndex != index })) },
                    ) { Text("Remove") }
                }
            }
        }
    }
}

@Composable
private fun HostPhotoArtwork(photo: EventPhoto, draftId: String?, size: androidx.compose.ui.unit.Dp) {
    Box(modifier = Modifier.width(size).height(size)) {
        ShopperEventArtwork(
            presentation = hostArtworkPresentationFor(draftId = draftId, photoReference = photo.url),
            modifier = Modifier.fillMaxSize(),
            height = size,
        )
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
                modifier = Modifier.fillMaxWidth().yardScapeInteractiveTarget(),
                onClick = { onEditorStateChanged(state.copy(approvalMode = mode, validationErrors = emptyList())) },
            ) { Text(mode.label) }
        } else {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth().yardScapeInteractiveTarget(),
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
    isExpanded: Boolean,
    nowEpochMillis: Long,
) {
    val preview = state.publicPreview(nowEpochMillis)
    FormSectionLabel("Public shopper preview")
    val previewContent: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ShopperEventArtwork(
                presentation = preview.toShopperEventArtworkPresentation(state.draft.id ?: "new-host-draft"),
                modifier = Modifier.fillMaxWidth(),
                height = 224.dp,
            )
            Text(preview.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(preview.description)
            preview.shopperDetailSections().forEach { (label, value) ->
                if (value.isNotBlank()) {
                    Text("$label · $value", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            PrivacyNote("Preview contains approximate location only. Protected address and access instructions are omitted.")
        }
    }
    val actions: @Composable () -> Unit = {
        Button(modifier = Modifier.fillMaxWidth().yardScapeInteractiveTarget(), onClick = onSaveDraft) { Text("Save draft") }
        Button(
            modifier = Modifier.fillMaxWidth().yardScapeInteractiveTarget(),
            onClick = { onRequestConfirmation(HostConfirmationAction.Publish) },
            colors = ButtonDefaults.buttonColors(containerColor = Clay),
        ) { Text("Review and publish") }
        if (state.savedEventId != null) {
            OutlinedButton(modifier = Modifier.fillMaxWidth().yardScapeInteractiveTarget(), onClick = { onRequestConfirmation(HostConfirmationAction.Hide) }) { Text("Hide from search") }
            OutlinedButton(modifier = Modifier.fillMaxWidth().yardScapeInteractiveTarget(), onClick = { onRequestConfirmation(HostConfirmationAction.Cancel) }) { Text("Cancel event") }
        }
    }
    if (isExpanded) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.extraLarge),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1.45f), verticalArrangement = Arrangement.spacedBy(8.dp)) { previewContent() }
            Column(modifier = Modifier.weight(0.8f), verticalArrangement = Arrangement.spacedBy(8.dp)) { actions() }
        }
    } else {
        previewContent()
        actions()
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
            shape = MaterialTheme.shapes.small,
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Search address" },
                    value = addressQuery,
                    onValueChange = { addressQuery = it },
                    label = { Text("Search address") },
                    placeholder = { Text("Start typing a street address") },
                    shape = MaterialTheme.shapes.small,
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
        modifier = Modifier.fillMaxWidth().yardScapeInteractiveTarget(),
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
        modifier = Modifier.fillMaxWidth().yardScapeInteractiveTarget(),
        onClick = { showPicker = true },
        shape = MaterialTheme.shapes.small,
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
                modifier = Modifier.yardScapeInteractiveTarget(),
                onClick = {
                    onConfirm(timePickerState.hour, timePickerState.minute)
                },
            ) {
                Text("Set")
            }
        },
        dismissButton = {
            TextButton(modifier = Modifier.yardScapeInteractiveTarget(), onClick = onDismiss) {
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
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = label },
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        shape = MaterialTheme.shapes.small,
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

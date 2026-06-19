package com.naslabs.yardscape

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.naslabs.yardscape.data.HostEventDraft
import com.naslabs.yardscape.data.MapSelectedLocation
import com.naslabs.yardscape.data.SeededYardSaleData
import com.naslabs.yardscape.data.withMapSelectedLocation
import com.naslabs.yardscape.ui.BrowseEventItem
import com.naslabs.yardscape.ui.EventDetailState
import com.naslabs.yardscape.ui.HostEditorState
import com.naslabs.yardscape.ui.HostEventItem
import com.naslabs.yardscape.ui.LocationRevealState
import com.naslabs.yardscape.ui.YardScapeAppState
import com.naslabs.yardscape.ui.YardScapeRoute
import com.naslabs.yardscape.ui.YardScapeTestTags
import com.naslabs.yardscape.ui.toDetailSections

@Composable
@Preview
fun App() {
    YardScapeTheme {
        val appState = remember { YardScapeAppState() }
        var route by remember { mutableStateOf(appState.route) }

        Surface(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .safeContentPadding()
                .fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            when (val currentRoute = route) {
                YardScapeRoute.Browse -> BrowseScreen(
                    events = appState.browseItems(),
                    onEventSelected = { eventId ->
                        appState.openEvent(eventId)
                        route = appState.route
                    },
                    onHostSelected = {
                        appState.openHostCreateEdit()
                        route = appState.route
                    },
                )

                is YardScapeRoute.EventDetail -> PublicEventDetailScreen(
                    state = appState.selectedEventDetailState(),
                    onBack = {
                        appState.returnToBrowse()
                        route = appState.route
                    },
                    onRsvp = {
                        appState.openRsvp(currentRoute.eventId)
                        route = appState.route
                    },
                )

                is YardScapeRoute.Rsvp -> RsvpScreen(
                    onConfirm = {
                        appState.confirmRsvp(currentRoute.eventId)
                        route = appState.route
                    },
                    onBack = {
                        appState.openEvent(currentRoute.eventId)
                        route = appState.route
                    },
                )

                is YardScapeRoute.HostCreateEdit -> {
                    var editorState by remember(currentRoute.eventId) {
                        mutableStateOf(appState.hostEditorState(currentRoute.eventId))
                    }
                    HostCreateEditScreen(
                        hostEvents = appState.hostEventItems(),
                        editorState = editorState,
                        locationSuggestions = appState.hostLocationSuggestions(),
                        onDraftChanged = { draft ->
                            editorState = editorState.copy(
                                draft = draft,
                                validationErrors = emptyList(),
                            )
                        },
                        onNew = {
                            appState.openHostCreateEdit()
                            route = appState.route
                        },
                        onEdit = { eventId ->
                            appState.openHostCreateEdit(eventId)
                            route = appState.route
                        },
                        onSaveDraft = {
                            editorState = appState.saveHostDraft(editorState.draft)
                        },
                        onPublish = {
                            editorState = appState.publishHostEvent(editorState.draft)
                        },
                        onCancelEvent = {
                            editorState.savedEventId?.let { eventId ->
                                appState.cancelHostEvent(eventId)
                                editorState = appState.hostEditorState(eventId)
                            }
                        },
                        onHideEvent = {
                            editorState.savedEventId?.let { eventId ->
                                appState.hideHostEvent(eventId)
                                editorState = appState.hostEditorState(eventId)
                            }
                        },
                        onBack = {
                            appState.returnToBrowse()
                            route = appState.route
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun YardScapeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Evergreen,
            onPrimary = Color.White,
            primaryContainer = MintMist,
            onPrimaryContainer = ForestInk,
            secondary = Clay,
            onSecondary = Color.White,
            secondaryContainer = PeachWash,
            onSecondaryContainer = CocoaInk,
            tertiary = MarketBlue,
            onTertiary = Color.White,
            tertiaryContainer = SkyWash,
            onTertiaryContainer = NavyInk,
            background = Linen,
            onBackground = ForestInk,
            surface = Color.White,
            onSurface = ForestInk,
            surfaceVariant = Stone,
            onSurfaceVariant = OliveText,
            surfaceContainer = Color.White,
            surfaceContainerHighest = Stone,
            outline = SageLine,
        ),
        content = content,
    )
}

private val Evergreen = Color(0xFF2F6F4E)
private val ForestInk = Color(0xFF16251D)
private val OliveText = Color(0xFF596457)
private val Linen = Color(0xFFF8F4EC)
private val MintMist = Color(0xFFDDEFE3)
private val SageLine = Color(0xFFC8D6C4)
private val Stone = Color(0xFFEDE7DC)
private val Clay = Color(0xFFC56247)
private val CocoaInk = Color(0xFF3B2117)
private val PeachWash = Color(0xFFF8D9C9)
private val MarketBlue = Color(0xFF386E7F)
private val NavyInk = Color(0xFF13262D)
private val SkyWash = Color(0xFFD9ECF0)
private val SunTag = Color(0xFFFFD166)
private val PhotoLeaf = Color(0xFF98C47B)
private val PhotoMarket = Color(0xFF73A6AD)
private val PhotoClay = Color(0xFFE09B72)

@Composable
private fun HostCreateEditScreen(
    hostEvents: List<HostEventItem>,
    editorState: HostEditorState,
    locationSuggestions: List<MapSelectedLocation>,
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

        item {
            HostEventForm(
                state = editorState,
                locationSuggestions = locationSuggestions,
                onDraftChanged = onDraftChanged,
                onSaveDraft = onSaveDraft,
                onPublish = onPublish,
                onCancelEvent = onCancelEvent,
                onHideEvent = onHideEvent,
            )
        }
    }
}

@Composable
private fun HostEventForm(
    state: HostEditorState,
    locationSuggestions: List<MapSelectedLocation>,
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

        HostTextField("Title", draft.title) { onDraftChanged(draft.copy(title = it)) }
        HostTextField("Description", draft.description) { onDraftChanged(draft.copy(description = it)) }
        HostTextField("Start epoch millis", draft.startsAtEpochMillis?.toString().orEmpty()) {
            onDraftChanged(draft.copy(startsAtEpochMillis = it.toLongOrNull()))
        }
        HostTextField("End epoch millis", draft.endsAtEpochMillis?.toString().orEmpty()) {
            onDraftChanged(draft.copy(endsAtEpochMillis = it.toLongOrNull()))
        }
        HostTextField("Categories", draft.categories.joinToString(", ")) {
            onDraftChanged(draft.copy(categories = it.toCsvList()))
        }
        HostTextField("Payment notes", draft.acceptedPaymentTypes.joinToString(", ")) {
            onDraftChanged(draft.copy(acceptedPaymentTypes = it.toCsvList()))
        }
        HostTextField("Accessibility notes", draft.accessibilityNotes.joinToString(", ")) {
            onDraftChanged(draft.copy(accessibilityNotes = it.toCsvList()))
        }

        MapLocationPicker(
            selectedLocation = draft.selectedMapLocation,
            suggestions = locationSuggestions,
            onLocationSelected = { location ->
                onDraftChanged(draft.withMapSelectedLocation(location))
            },
        )
        HostTextField("Access instructions", draft.accessInstructions.orEmpty()) {
            onDraftChanged(draft.copy(accessInstructions = it.ifBlank { null }))
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
    suggestions: List<MapSelectedLocation>,
    onLocationSelected: (MapSelectedLocation) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FormSectionLabel("Map location")
        PrivacyNote("Use Maps to select the sale address. Shoppers only see the approximate area until RSVP access is granted.")
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
                suggestions.forEach { location ->
                    MapLocationSuggestionButton(
                        location = location,
                        onLocationSelected = onLocationSelected,
                    )
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
        Text(
            text = "Use ${location.displayName}",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
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

@Composable
private fun BrowseScreen(
    events: List<BrowseEventItem>,
    onEventSelected: (String) -> Unit,
    onHostSelected: () -> Unit,
) {
    var selectedCategory by remember { mutableStateOf("All") }
    val categoryFilters = listOf("All") + events
        .flatMap { it.categoryLabels }
        .distinct()
        .sorted()
    val visibleEvents = if (selectedCategory == "All") {
        events
    } else {
        events.filter { selectedCategory in it.categoryLabels }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag(YardScapeTestTags.BrowseScreen)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            BrowseHero(
                eventCount = visibleEvents.size,
                onHostSelected = onHostSelected,
            )
        }

        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                categoryFilters.forEach { category ->
                    FilterChip(
                        label = category.replaceFirstChar { it.uppercase() },
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                    )
                }
            }
        }

        items(visibleEvents, key = { it.id }) { event ->
            EventPreviewCard(
                event = event,
                onClick = { onEventSelected(event.id) },
            )
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun BrowseHero(
    eventCount: Int,
    onHostSelected: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(top = 18.dp, bottom = 2.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
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
                    text = YardScapeConfig.appName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = ForestInk,
                )
                Text(
                    text = "$eventCount nearby sales",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(onClick = onHostSelected) {
                Text("Host")
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = Evergreen,
            contentColor = Color.White,
            shadowElevation = 2.dp,
        ) {
            Column(
                modifier = Modifier
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Evergreen, MarketBlue),
                        ),
                    )
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Find the good stuff before the signs go up.",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Browse public previews by area, date, and category. Exact addresses stay private until RSVP access is granted.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.86f),
                )
            }
        }
    }
}

@Composable
private fun EventPreviewCard(
    event: BrowseEventItem,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(YardScapeTestTags.browseEventCard(event.id))
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            EventPhotoPreview(
                title = event.title,
                description = event.photoDescription,
                seed = event.id,
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatusLabel(text = event.statusLabel)
                    InfoChip(text = event.dateLabel)
                }

                Text(
                    text = event.locationLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                event.categoryLabels.forEach { label ->
                    CategoryChip(label = label)
                }
            }
        }
    }
}

@Composable
private fun PublicEventDetailScreen(
    state: EventDetailState?,
    onBack: () -> Unit,
    onRsvp: () -> Unit,
) {
    if (state == null) {
        RoutePlaceholderScreen(title = "Event unavailable", onBack = onBack)
        return
    }
    val detail = state.detail

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(
                modifier = Modifier.padding(top = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                TextButton(onClick = onBack) {
                    Text("Back")
                }
                EventPhotoPreview(
                    title = detail.title,
                    description = detail.photos.firstOrNull()?.description,
                    seed = detail.id,
                )
                Text(
                    text = detail.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = ForestInk,
                )
                Text(
                    text = detail.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                detail.categories.forEach { label -> CategoryChip(label = label) }
            }
        }

        items(detail.toDetailSections(SeededYardSaleData.BASE_NOW_EPOCH_MILLIS)) { section ->
            DetailRow(label = section.first, value = section.second)
        }

        item {
            LocationAccessPanel(revealState = state.revealState)
        }

        item {
            Column(
                modifier = Modifier.padding(bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PrivacyNote(text = detail.rsvpPrompt)
                if (state.shouldShowRsvpAction) {
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(YardScapeTestTags.RsvpAction),
                        onClick = onRsvp,
                    ) {
                        Text("RSVP")
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationAccessPanel(revealState: LocationRevealState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(YardScapeTestTags.LocationAccessPanel),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (revealState) {
                is LocationRevealState.Revealed -> MintMist
                else -> SkyWash
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatusLabel(
                text = when (revealState) {
                    is LocationRevealState.Revealed -> "Access granted"
                    else -> "Privacy protected"
                },
            )
            Text(
                text = revealState.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = ForestInk,
            )
            Text(
                modifier = when (revealState) {
                    is LocationRevealState.Revealed -> Modifier.testTag(YardScapeTestTags.ExactLocationContent)
                    else -> Modifier
                },
                text = revealState.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RsvpScreen(
    onConfirm: () -> Unit,
    onBack: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                StatusLabel(text = "RSVP")
                Text(
                    text = "Confirm attendance",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = ForestInk,
                )
                Text(
                    text = "For this test workflow, RSVP is auto-accepted so you can verify the protected location reveal boundary.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(YardScapeTestTags.RsvpConfirmAction),
                    onClick = onConfirm,
                ) {
                    Text("Confirm RSVP")
                }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onBack,
                ) {
                    Text("Back to event")
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = ForestInk,
            )
        }
    }
}

@Composable
private fun RoutePlaceholderScreen(
    title: String,
    onBack: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Button(onClick = onBack) {
                Text("Back")
            }
        }
    }
}

@Composable
private fun CategoryChip(label: String) {
    Text(
        modifier = Modifier
            .background(
                color = PeachWash,
                shape = RoundedCornerShape(6.dp),
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        text = label.replaceFirstChar { it.uppercase() },
        style = MaterialTheme.typography.labelMedium,
        color = CocoaInk,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun StatusLabel(text: String) {
    Text(
        modifier = Modifier
            .background(
                color = SunTag,
                shape = RoundedCornerShape(6.dp),
            )
            .widthIn(max = 180.dp)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = CocoaInk,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun InfoChip(text: String) {
    Text(
        modifier = Modifier
            .background(
                color = Stone,
                shape = RoundedCornerShape(6.dp),
            )
            .widthIn(max = 220.dp)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = OliveText,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val background = if (selected) Evergreen else MaterialTheme.colorScheme.surface
    val content = if (selected) Color.White else ForestInk
    Text(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(background)
            .border(
                width = 1.dp,
                color = if (selected) Evergreen else SageLine,
                shape = RoundedCornerShape(6.dp),
            )
            .clickable(onClick = onClick)
            .widthIn(max = 160.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = content,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun EventPhotoPreview(
    title: String,
    description: String?,
    seed: String,
) {
    val accent = when (seed.length % 3) {
        0 -> PhotoLeaf
        1 -> PhotoMarket
        else -> PhotoClay
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(136.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(accent, accent.copy(alpha = 0.52f), Linen),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.72f), RoundedCornerShape(8.dp))
            .padding(14.dp),
        contentAlignment = Alignment.BottomStart,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = description ?: "Preview photo coming soon",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = ForestInk,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = ForestInk.copy(alpha = 0.72f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PrivacyNote(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MintMist,
    ) {
        Text(
            modifier = Modifier.padding(12.dp),
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = ForestInk,
        )
    }
}

@Composable
private fun FormSectionLabel(text: String) {
    Text(
        modifier = Modifier.padding(top = 4.dp),
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )
}

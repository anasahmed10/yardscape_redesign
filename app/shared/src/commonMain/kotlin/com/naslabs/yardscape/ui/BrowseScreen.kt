package com.naslabs.yardscape.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.naslabs.yardscape.map.PlatformMapCapability
import com.naslabs.yardscape.map.MapFallbackSurface
import com.naslabs.yardscape.map.PlatformMapState
import com.naslabs.yardscape.map.PlatformMapStyle
import com.naslabs.yardscape.map.PlatformMapSurface
import com.naslabs.yardscape.map.platformMapCapability
import com.naslabs.yardscape.map.platformMapSupportsComposeOverlay
import com.naslabs.yardscape.domain.MapViewport
import com.naslabs.yardscape.domain.ViewportCenter
import kotlinx.coroutines.delay

@Composable
fun BrowseScreen(
    state: ShopperDiscoveryState,
    mapState: MapDiscoveryState,
    dataAvailability: AppDataAvailability = AppDataAvailability.Available,
    onEventSelected: (String) -> Unit,
    onHostSelected: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onDateChanged: (DiscoveryDateFilter) -> Unit,
    onDistanceChanged: (DiscoveryDistanceFilter) -> Unit,
    onCategoryToggled: (String) -> Unit,
    onDisplayModeChanged: (DiscoveryDisplayMode) -> Unit,
    onResetFilters: () -> Unit,
    onRetryData: () -> Unit,
    onSavedToggled: (String) -> Unit,
    onMapViewportChanged: (MapViewport) -> Unit,
    onMapViewportSettled: () -> Unit,
    onSearchThisArea: () -> Unit,
    onMapEventSelected: (String?) -> Unit,
    onMapAvailabilityChanged: (MapAvailability) -> Unit,
    onUseMyLocation: () -> Unit,
    onSheetPositionChanged: (MapResultsSheetPosition) -> Unit,
) {
    val spacing = YardScapeDesign.spacing
    val presentation = state.browsePresentationFor(dataAvailability)
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag(YardScapeTestTags.BrowseScreen)
            .padding(horizontal = spacing.large),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
    ) {
        item {
            BrowseHero(
                eventCount = state.items.size,
                onHostSelected = onHostSelected,
            )
        }

        if (presentation.availability in setOf(
                ShopperBrowseAvailability.Loading,
                ShopperBrowseAvailability.OfflineCached,
                ShopperBrowseAvailability.RecoverableError,
            )
        ) {
            item {
                ShopperStatePanel(
                    title = presentation.title,
                    message = presentation.message,
                    actionLabel = presentation.actionLabel,
                    onAction = presentation.actionLabel?.let { onRetryData },
                )
            }
        }

        item {
            DiscoveryControls(
                state = state,
                onQueryChanged = onQueryChanged,
                onDateChanged = onDateChanged,
                onDistanceChanged = onDistanceChanged,
                onCategoryToggled = onCategoryToggled,
                onDisplayModeChanged = onDisplayModeChanged,
                onResetFilters = onResetFilters,
            )
        }

        when {
            presentation.availability == ShopperBrowseAvailability.EmptyNearby ||
                presentation.availability == ShopperBrowseAvailability.FilteredEmpty -> item {
                ShopperStatePanel(
                    title = presentation.title,
                    message = presentation.message,
                    actionLabel = presentation.actionLabel,
                    onAction = presentation.actionLabel?.let { onResetFilters },
                    modifier = Modifier.testTag(YardScapeTestTags.DiscoveryNoResults),
                )
            }
            state.displayMode == DiscoveryDisplayMode.Map -> item {
                MapDiscoveryExperience(
                    events = state.items,
                    savedEventIds = state.savedEventIds,
                    mapState = mapState,
                    onEventSelected = onEventSelected,
                    onSavedToggled = onSavedToggled,
                    onMapViewportChanged = onMapViewportChanged,
                    onMapViewportSettled = onMapViewportSettled,
                    onSearchThisArea = onSearchThisArea,
                    onMapEventSelected = onMapEventSelected,
                    onMapAvailabilityChanged = onMapAvailabilityChanged,
                    onUseMyLocation = onUseMyLocation,
                    onSheetPositionChanged = onSheetPositionChanged,
                )
            }
            else -> items(state.items, key = { it.id }) { event ->
                BrowseListEventCard(
                    event = event,
                    isSaved = event.id in state.savedEventIds,
                    onClick = { onEventSelected(event.id) },
                    onSavedToggle = { onSavedToggled(event.id) },
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun DiscoveryControls(
    state: ShopperDiscoveryState,
    onQueryChanged: (String) -> Unit,
    onDateChanged: (DiscoveryDateFilter) -> Unit,
    onDistanceChanged: (DiscoveryDistanceFilter) -> Unit,
    onCategoryToggled: (String) -> Unit,
    onDisplayModeChanged: (DiscoveryDisplayMode) -> Unit,
    onResetFilters: () -> Unit,
) {
    var showMoreFilters by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.small)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "Search sales" },
                value = state.filters.query,
                onValueChange = onQueryChanged,
                placeholder = { Text("Search neighborhoods") },
                singleLine = true,
            )
            OutlinedButton(
                modifier = Modifier.heightIn(min = 48.dp),
                onClick = { showMoreFilters = !showMoreFilters },
            ) {
                Text(if (showMoreFilters) "Less" else "Filters")
            }
        }
        FilterRow(
            options = DiscoveryDateFilter.entries,
            label = { it.label },
            selected = { it == state.filters.date },
            onSelected = onDateChanged,
        )
        if (showMoreFilters || state.filters.distance != DiscoveryDistanceFilter.Any || state.filters.categories.isNotEmpty()) {
            FilterRow(
                options = DiscoveryDistanceFilter.entries,
                label = { it.label },
                selected = { it == state.filters.distance },
                onSelected = onDistanceChanged,
            )
            if (state.availableCategories.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.availableCategories.forEach { category ->
                        FilterChip(
                            label = category.replaceFirstChar { it.uppercase() },
                            selected = category in state.filters.categories,
                            onClick = { onCategoryToggled(category) },
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        ) {
            DiscoveryDisplayMode.entries.forEach { mode ->
                FilterChip(
                    label = mode.name,
                    selected = state.displayMode == mode,
                    onClick = { onDisplayModeChanged(mode) },
                )
            }
        }
        if (state.filters.isActive) {
            OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onResetFilters) {
                Text("Reset search and filters")
            }
        }
    }
}

@Composable
private fun <T> FilterRow(
    options: List<T>,
    label: (T) -> String,
    selected: (T) -> Boolean,
    onSelected: (T) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            FilterChip(
                label = label(option),
                selected = selected(option),
                onClick = { onSelected(option) },
            )
        }
    }
}

@Composable
private fun MapDiscoveryExperience(
    events: List<BrowseEventItem>,
    savedEventIds: Set<String>,
    mapState: MapDiscoveryState,
    onEventSelected: (String) -> Unit,
    onSavedToggled: (String) -> Unit,
    onMapViewportChanged: (MapViewport) -> Unit,
    onMapViewportSettled: () -> Unit,
    onSearchThisArea: () -> Unit,
    onMapEventSelected: (String?) -> Unit,
    onMapAvailabilityChanged: (MapAvailability) -> Unit,
    onUseMyLocation: () -> Unit,
    onSheetPositionChanged: (MapResultsSheetPosition) -> Unit,
) {
    val defaultPresentation = mapPresentationFor(mapState.markers)
    val viewport = mapState.cameraViewportDraft ?: defaultPresentation.defaultViewport
    val presentation = mapPresentationFor(mapState.markers, viewport.zoomLevel)
    LaunchedEffect(mapState.cameraViewportDraft, mapState.viewportSearchReadiness) {
        if (mapState.viewportSearchReadiness == ViewportSearchReadiness.WaitingForDebounce) {
            delay(350)
            onMapViewportSettled()
        }
    }
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val expanded = maxWidth >= 760.dp
        if (expanded) {
            Row(
                modifier = Modifier.fillMaxWidth().height(600.dp),
                horizontalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.medium),
            ) {
                DiscoveryMapPanel(
                    modifier = Modifier.weight(1.45f).fillMaxSize(),
                    mapState = mapState,
                    platformState = PlatformMapState(
                        viewport = viewport,
                        markers = presentation.unclusteredMarkers,
                        clusters = presentation.clusters,
                        selectedEventId = mapState.selectedEventId,
                    ),
                    onMapViewportChanged = onMapViewportChanged,
                    onSearchThisArea = onSearchThisArea,
                    onMapEventSelected = onMapEventSelected,
                    onMapAvailabilityChanged = onMapAvailabilityChanged,
                    onUseMyLocation = onUseMyLocation,
                )
                NearbyResultList(
                    modifier = Modifier.weight(1f).fillMaxSize(),
                    events = events,
                    savedEventIds = savedEventIds,
                    selectedEventId = mapState.selectedEventId,
                    onEventSelected = onEventSelected,
                    onMapEventSelected = onMapEventSelected,
                    onSavedToggled = onSavedToggled,
                )
            }
        } else if (platformMapSupportsComposeOverlay()) {
            Box(modifier = Modifier.fillMaxWidth().height(620.dp)) {
                DiscoveryMapPanel(
                    modifier = Modifier.fillMaxSize(),
                    mapState = mapState,
                    platformState = PlatformMapState(
                        viewport = viewport,
                        markers = presentation.unclusteredMarkers,
                        clusters = presentation.clusters,
                        selectedEventId = mapState.selectedEventId,
                    ),
                    onMapViewportChanged = onMapViewportChanged,
                    onSearchThisArea = onSearchThisArea,
                    onMapEventSelected = onMapEventSelected,
                    onMapAvailabilityChanged = onMapAvailabilityChanged,
                    onUseMyLocation = onUseMyLocation,
                )
                MobileNearbySheet(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    position = mapState.sheetPosition,
                    events = events,
                    savedEventIds = savedEventIds,
                    selectedEventId = mapState.selectedEventId,
                    onPositionChanged = onSheetPositionChanged,
                    onEventSelected = onEventSelected,
                    onMapEventSelected = onMapEventSelected,
                    onSavedToggled = onSavedToggled,
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.small),
            ) {
                DiscoveryMapPanel(
                    modifier = Modifier.fillMaxWidth().height(420.dp),
                    mapState = mapState,
                    platformState = PlatformMapState(
                        viewport = viewport,
                        markers = presentation.unclusteredMarkers,
                        clusters = presentation.clusters,
                        selectedEventId = mapState.selectedEventId,
                    ),
                    onMapViewportChanged = onMapViewportChanged,
                    onSearchThisArea = onSearchThisArea,
                    onMapEventSelected = onMapEventSelected,
                    onMapAvailabilityChanged = onMapAvailabilityChanged,
                    onUseMyLocation = onUseMyLocation,
                )
                MobileNearbySheet(
                    modifier = Modifier.fillMaxWidth(),
                    position = mapState.sheetPosition,
                    events = events,
                    savedEventIds = savedEventIds,
                    selectedEventId = mapState.selectedEventId,
                    onPositionChanged = onSheetPositionChanged,
                    onEventSelected = onEventSelected,
                    onMapEventSelected = onMapEventSelected,
                    onSavedToggled = onSavedToggled,
                )
            }
        }
    }
}

@Composable
private fun DiscoveryMapPanel(
    modifier: Modifier,
    mapState: MapDiscoveryState,
    platformState: PlatformMapState,
    onMapViewportChanged: (MapViewport) -> Unit,
    onSearchThisArea: () -> Unit,
    onMapEventSelected: (String?) -> Unit,
    onMapAvailabilityChanged: (MapAvailability) -> Unit,
    onUseMyLocation: () -> Unit,
) {
    val isFallback = usesMapFallback(platformMapCapability(), mapState.mapAvailability)
    Column(
        modifier = modifier
            .testTag(YardScapeTestTags.DiscoveryMap)
            .semantics { contentDescription = "Approximate neighborhood map of nearby yard sales" },
        verticalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.small),
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.small),
            verticalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.small),
        ) {
            OutlinedButton(
                modifier = Modifier.heightIn(min = 48.dp),
                enabled = mapState.locationPermission != ApproximateLocationPermission.Requesting,
                onClick = onUseMyLocation,
            ) {
                Text(locationButtonLabel(mapState.locationPermission))
            }
            if (mapState.canSearchThisArea) {
                Button(
                    modifier = Modifier.heightIn(min = 48.dp),
                    onClick = onSearchThisArea,
                ) {
                    Text("Search this area")
                }
            }
            if (isFallback && platformMapCapability() == PlatformMapCapability.Interactive) {
                OutlinedButton(
                    modifier = Modifier.heightIn(min = 48.dp),
                    onClick = { onMapAvailabilityChanged(MapAvailability.Loading) },
                ) {
                    Text("Retry map")
                }
            }
        }
        if (isFallback) {
            MapFallbackSurface(
                state = platformState,
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
        } else {
            PlatformMapSurface(
                state = platformState,
                modifier = Modifier.fillMaxWidth().weight(1f),
                onViewportChanged = onMapViewportChanged,
                onMarkerSelected = onMapEventSelected,
                onClusterSelected = { clusterId ->
                    platformState.clusters.firstOrNull { it.id == clusterId }?.let { cluster ->
                        onMapViewportChanged(
                            MapViewport(
                                center = ViewportCenter(
                                    cluster.area.center.latitude,
                                    cluster.area.center.longitude,
                                ),
                                zoomLevel = (platformState.viewport.zoomLevel + 2.0).coerceAtMost(18.0),
                            ),
                        )
                    }
                },
                onMapLoaded = { onMapAvailabilityChanged(MapAvailability.Available) },
                onMapLoadFailed = { reason ->
                    onMapAvailabilityChanged(MapAvailability.Failed(reason ?: "Map tiles could not be loaded."))
                },
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        ) {
            Column(modifier = Modifier.padding(YardScapeDesign.spacing.small)) {
                Text("Approximate pins", style = MaterialTheme.typography.labelLarge)
                Text(
                    if (isFallback) "Map unavailable · List remains fully usable" else "No street addresses shown",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    PlatformMapStyle.OpenFreeMapLiberty.attribution,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun locationButtonLabel(permission: ApproximateLocationPermission): String = when (permission) {
    ApproximateLocationPermission.NotRequested -> "Use my location"
    ApproximateLocationPermission.Requesting -> "Locating…"
    ApproximateLocationPermission.Granted -> "Near me"
    ApproximateLocationPermission.Denied -> "Location denied"
    ApproximateLocationPermission.Unavailable -> "Location unavailable"
}

@Composable
private fun NearbyResultList(
    modifier: Modifier,
    events: List<BrowseEventItem>,
    savedEventIds: Set<String>,
    selectedEventId: String?,
    onEventSelected: (String) -> Unit,
    onMapEventSelected: (String?) -> Unit,
    onSavedToggled: (String) -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.small),
    ) {
        item {
            Text("Nearby sales", style = MaterialTheme.typography.headlineSmall)
            Text("${events.size} public previews", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(events, key = { it.id }) { event ->
            CompactMapResultCard(
                event = event,
                selected = event.id == selectedEventId,
                saved = event.id in savedEventIds,
                onSelect = { onMapEventSelected(event.id) },
                onOpen = { onEventSelected(event.id) },
                onSave = { onSavedToggled(event.id) },
            )
        }
    }
}

@Composable
private fun MobileNearbySheet(
    modifier: Modifier,
    position: MapResultsSheetPosition,
    events: List<BrowseEventItem>,
    savedEventIds: Set<String>,
    selectedEventId: String?,
    onPositionChanged: (MapResultsSheetPosition) -> Unit,
    onEventSelected: (String) -> Unit,
    onMapEventSelected: (String?) -> Unit,
    onSavedToggled: (String) -> Unit,
) {
    var dragDistance by remember { mutableFloatStateOf(0f) }
    val dragState = rememberDraggableState { delta -> dragDistance += delta }
    val height = when (position) {
        MapResultsSheetPosition.Collapsed -> 190.dp
        MapResultsSheetPosition.HalfExpanded -> 300.dp
        MapResultsSheetPosition.Expanded -> 470.dp
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .draggable(
                orientation = Orientation.Vertical,
                state = dragState,
                onDragStopped = {
                    val next = when {
                        dragDistance < -36f -> position.expandOneStep()
                        dragDistance > 36f -> position.collapseOneStep()
                        else -> position
                    }
                    dragDistance = 0f
                    onPositionChanged(next)
                },
            )
            .testTag(YardScapeTestTags.DiscoveryResultsSheet),
        shape = MaterialTheme.shapes.large,
        shadowElevation = 8.dp,
    ) {
        Column(modifier = Modifier.padding(YardScapeDesign.spacing.medium)) {
            OutlinedButton(
                modifier = Modifier.align(Alignment.CenterHorizontally).heightIn(min = 48.dp),
                onClick = {
                    onPositionChanged(
                        if (position == MapResultsSheetPosition.Expanded) {
                            MapResultsSheetPosition.Collapsed
                        } else {
                            position.expandOneStep()
                        },
                    )
                },
            ) {
                Text("${events.size} nearby sales")
            }
            NearbyResultList(
                modifier = Modifier.fillMaxSize(),
                events = events,
                savedEventIds = savedEventIds,
                selectedEventId = selectedEventId,
                onEventSelected = onEventSelected,
                onMapEventSelected = onMapEventSelected,
                onSavedToggled = onSavedToggled,
            )
        }
    }
}

private fun MapResultsSheetPosition.expandOneStep(): MapResultsSheetPosition = when (this) {
    MapResultsSheetPosition.Collapsed -> MapResultsSheetPosition.HalfExpanded
    MapResultsSheetPosition.HalfExpanded -> MapResultsSheetPosition.Expanded
    MapResultsSheetPosition.Expanded -> MapResultsSheetPosition.Expanded
}

private fun MapResultsSheetPosition.collapseOneStep(): MapResultsSheetPosition = when (this) {
    MapResultsSheetPosition.Collapsed -> MapResultsSheetPosition.Collapsed
    MapResultsSheetPosition.HalfExpanded -> MapResultsSheetPosition.Collapsed
    MapResultsSheetPosition.Expanded -> MapResultsSheetPosition.HalfExpanded
}

@Composable
private fun CompactMapResultCard(
    event: BrowseEventItem,
    selected: Boolean,
    saved: Boolean,
    onSelect: () -> Unit,
    onOpen: () -> Unit,
    onSave: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        ),
        onClick = onSelect,
    ) {
        Column(
            modifier = Modifier.padding(YardScapeDesign.spacing.small),
            verticalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.extraSmall),
        ) {
            Text(event.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(event.dateLabel, color = MaterialTheme.colorScheme.primary)
            Text(event.locationLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.small)) {
                Button(modifier = Modifier.heightIn(min = 48.dp), onClick = onOpen) { Text("View sale") }
                OutlinedButton(modifier = Modifier.heightIn(min = 48.dp), onClick = onSave) {
                    Text(if (saved) "Saved" else "Save")
                }
            }
        }
    }
}

@Composable
private fun BrowseAvailabilityCard(title: String, message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SkyWash),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun BrowseHero(
    eventCount: Int,
    onHostSelected: () -> Unit,
) {
    val spacing = YardScapeDesign.spacing
    Column(
        modifier = Modifier.padding(top = spacing.large),
        verticalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        ShopperSectionHeader(
            title = "Nearby sales",
            supportingText = "$eventCount public previews",
            actionLabel = "Host a sale",
            onAction = onHostSelected,
        )
        Text(
            text = "Browse public previews by area, date, and category. Exact addresses stay private until RSVP access is granted.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BrowseListEventCard(
    event: BrowseEventItem,
    isSaved: Boolean,
    onClick: () -> Unit,
    onSavedToggle: () -> Unit,
) {
    val actions = shopperBrowseEventActionsFor(isSaved)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            if (shopperBrowseListLayoutFor(maxWidth) == ShopperBrowseListLayout.Expanded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = YardScapeDesign.spacing.medium),
                    horizontalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.large),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.weight(0.9f)) {
                        ShopperEventArtwork(
                            presentation = event.toShopperEventArtworkPresentation(),
                            height = 220.dp,
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1.1f),
                        verticalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.medium),
                    ) {
                        BrowseEventMetadata(event)
                        BrowseEventActions(
                            eventId = event.id,
                            actions = actions,
                            onOpen = onClick,
                            onSave = onSavedToggle,
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = YardScapeDesign.spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.medium),
                ) {
                    ShopperEventArtwork(
                        presentation = event.toShopperEventArtworkPresentation(),
                        height = 200.dp,
                    )
                    BrowseEventMetadata(event)
                    BrowseEventActions(
                        eventId = event.id,
                        actions = actions,
                        onOpen = onClick,
                        onSave = onSavedToggle,
                    )
                }
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun BrowseEventMetadata(event: BrowseEventItem) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.small),
    ) {
        Text(
            text = event.title,
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.small),
            verticalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.small),
        ) {
            StatusLabel(text = event.statusLabel)
            InfoChip(text = event.dateLabel)
        }
        Text(
            text = event.locationLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = event.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.small),
            verticalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.small),
        ) {
            event.categoryLabels.forEach { label -> CategoryChip(label = label) }
        }
    }
}

@Composable
private fun BrowseEventActions(
    eventId: String,
    actions: ShopperBrowseEventActions,
    onOpen: () -> Unit,
    onSave: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .testTag(YardScapeTestTags.browseEventCard(eventId))
                .semantics { contentDescription = actions.openLabel },
            onClick = onOpen,
        ) {
            Text(actions.openLabel)
        }
        TextButton(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .semantics { contentDescription = actions.saveLabel },
            onClick = onSave,
        ) {
            Text(actions.saveLabel)
        }
    }
}

@Composable
internal fun EventPreviewCard(
    event: BrowseEventItem,
    isSaved: Boolean,
    onClick: () -> Unit,
    onSavedToggle: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("event-preview-${event.id}"),
        shape = MaterialTheme.shapes.small,
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
                presentation = event.toShopperEventArtworkPresentation(),
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.headlineSmall,
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
            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(YardScapeTestTags.browseEventCard(event.id)),
                onClick = onClick,
            ) {
                Text("View sale")
            }
            OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onSavedToggle) {
                Text(if (isSaved) "Unsave" else "Save sale")
            }
        }
    }
}

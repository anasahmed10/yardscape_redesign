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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naslabs.yardscape.YardScapeConfig
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
    onShowAllNearbySales: () -> Unit,
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
            .testTag(YardScapeTestTags.BrowseScreen),
        verticalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        item {
            BrowseMarketplaceHeader(
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
                    statusMessageKind = when (presentation.availability) {
                        ShopperBrowseAvailability.OfflineCached -> YardScapeStatusMessageKind.Offline
                        ShopperBrowseAvailability.RecoverableError -> YardScapeStatusMessageKind.Failure
                        else -> null
                    },
                    modifier = Modifier.padding(horizontal = spacing.large),
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
                modifier = Modifier.padding(horizontal = spacing.large),
            )
        }

        when {
            presentation.availability == ShopperBrowseAvailability.EmptyNearby ||
                presentation.availability == ShopperBrowseAvailability.EmptySearchArea ||
                presentation.availability == ShopperBrowseAvailability.FilteredEmpty -> item {
                ShopperStatePanel(
                    title = presentation.title,
                    message = presentation.message,
                    actionLabel = presentation.actionLabel,
                    onAction = when (presentation.availability) {
                        ShopperBrowseAvailability.EmptySearchArea -> onShowAllNearbySales
                        ShopperBrowseAvailability.FilteredEmpty -> onResetFilters
                        else -> null
                    },
                    modifier = Modifier
                        .padding(horizontal = spacing.large)
                        .testTag(YardScapeTestTags.DiscoveryNoResults),
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
                    onDisplayModeChanged = onDisplayModeChanged,
                )
            }
            else -> items(state.items, key = { it.id }) { event ->
                Box(modifier = Modifier.padding(horizontal = spacing.large)) {
                    BrowseListEventCard(
                        event = event,
                        isSaved = event.id in state.savedEventIds,
                        onClick = { onEventSelected(event.id) },
                        onSavedToggle = { onSavedToggled(event.id) },
                    )
                }
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
    modifier: Modifier = Modifier,
) {
    var showMoreFilters by remember { mutableStateOf(false) }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.small),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp)
                    .semantics { contentDescription = "Search sales" },
                value = state.filters.query,
                onValueChange = onQueryChanged,
                placeholder = { Text("Search neighborhoods") },
                leadingIcon = {
                    Icon(imageVector = SearchIcon, contentDescription = null)
                },
                shape = MaterialTheme.shapes.extraLarge,
                singleLine = true,
            )
            OutlinedButton(
                modifier = Modifier.heightIn(min = 52.dp),
                shape = MaterialTheme.shapes.extraLarge,
                onClick = { showMoreFilters = !showMoreFilters },
            ) {
                Text(if (showMoreFilters) "Less" else "Filters")
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.small)) {
            items(DiscoveryDateFilter.entries) { option ->
                FilterChip(
                    label = marketplaceDateLabelFor(option),
                    selected = option == state.filters.date,
                    onClick = { onDateChanged(option) },
                )
            }
        }
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
        if (state.displayMode == DiscoveryDisplayMode.List) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                marketplaceDisplayModeOrder().forEach { mode ->
                    FilterChip(
                        label = mode.name,
                        selected = state.displayMode == mode,
                        onClick = { onDisplayModeChanged(mode) },
                    )
                }
            }
        }
        if (state.filters.isActive) {
            OutlinedButton(modifier = Modifier.fillMaxWidth().yardScapeInteractiveTarget(), onClick = onResetFilters) {
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
    onDisplayModeChanged: (DiscoveryDisplayMode) -> Unit,
) {
    val defaultViewport = remember(mapState.markers) { defaultViewportFor(mapState.markers) }
    val viewport = mapState.cameraViewportDraft ?: defaultViewport
    val presentation = remember(mapState.markers, viewport.zoomLevel) {
        mapPresentationFor(mapState.markers, viewport.zoomLevel)
    }
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
                    onDisplayModeChanged = onDisplayModeChanged,
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
            val sheetLayout = mapSheetLayoutFor(mapState.sheetPosition)
            Box(modifier = Modifier.fillMaxWidth().height(450.dp)) {
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
                    onDisplayModeChanged = onDisplayModeChanged,
                    bottomOverlayClearance = sheetLayout.height + 8.dp,
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
                    onDisplayModeChanged = onDisplayModeChanged,
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
    onDisplayModeChanged: (DiscoveryDisplayMode) -> Unit,
    bottomOverlayClearance: androidx.compose.ui.unit.Dp = 0.dp,
) {
    val isFallback = usesMapFallback(platformMapCapability(), mapState.mapAvailability)
    Box(
        modifier = modifier
            .testTag(YardScapeTestTags.DiscoveryMap)
            .semantics { contentDescription = "Approximate neighborhood map of nearby yard sales" },
    ) {
        if (isFallback) {
            MapFallbackSurface(
                state = platformState,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            PlatformMapSurface(
                state = platformState,
                modifier = Modifier.fillMaxSize(),
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
        FlowRow(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(YardScapeDesign.spacing.small),
            horizontalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.small),
            verticalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.small),
        ) {
            OutlinedButton(
                modifier = Modifier.yardScapeInteractiveTarget(),
                enabled = mapState.locationPermission != ApproximateLocationPermission.Requesting,
                shape = MaterialTheme.shapes.extraLarge,
                onClick = onUseMyLocation,
            ) {
                Text(compactLocationButtonLabel(mapState.locationPermission))
            }
            if (mapState.canSearchThisArea) {
                Button(
                    modifier = Modifier.yardScapeInteractiveTarget(),
                    shape = MaterialTheme.shapes.extraLarge,
                    onClick = onSearchThisArea,
                ) {
                    Text("Search this area")
                }
            }
            if (isFallback && platformMapCapability() == PlatformMapCapability.Interactive) {
                OutlinedButton(
                    modifier = Modifier.yardScapeInteractiveTarget(),
                    shape = MaterialTheme.shapes.extraLarge,
                    onClick = { onMapAvailabilityChanged(MapAvailability.Loading) },
                ) {
                    Text("Retry map")
                }
            }
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(
                    start = YardScapeDesign.spacing.small,
                    end = YardScapeDesign.spacing.small,
                    bottom = YardScapeDesign.spacing.small + bottomOverlayClearance,
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Surface(
                modifier = Modifier.widthIn(max = 154.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                shadowElevation = 3.dp,
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                    Text("Approximate pins", style = MaterialTheme.typography.labelLarge)
                    Text(
                        if (isFallback) "Map unavailable · List remains usable" else "No addresses shown",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        PlatformMapStyle.OpenFreeMapLiberty.attribution,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
            DiscoveryModeSwitcher(
                selected = DiscoveryDisplayMode.Map,
                onDisplayModeChanged = onDisplayModeChanged,
            )
        }
    }
}

@Composable
private fun DiscoveryModeSwitcher(
    selected: DiscoveryDisplayMode,
    onDisplayModeChanged: (DiscoveryDisplayMode) -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
        shadowElevation = 4.dp,
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            marketplaceDisplayModeOrder().forEach { mode ->
                if (mode == selected) {
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ) {
                        TextButton(
                            modifier = Modifier.yardScapeInteractiveTarget(),
                            onClick = { onDisplayModeChanged(mode) },
                        ) {
                            Text(mode.name, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                } else {
                    TextButton(
                        modifier = Modifier.yardScapeInteractiveTarget(),
                        onClick = { onDisplayModeChanged(mode) },
                    ) {
                        Text(mode.name, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
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

private fun compactLocationButtonLabel(permission: ApproximateLocationPermission): String = when (permission) {
    ApproximateLocationPermission.NotRequested -> "Near me"
    else -> locationButtonLabel(permission)
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
        verticalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.extraSmall),
    ) {
        item {
            Column(
                modifier = Modifier.padding(bottom = YardScapeDesign.spacing.small),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    "Nearby sales",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                )
                Text(
                    "Showing ${events.size} sales nearby",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
    val accessibility = mapSheetAccessibilityFor(position)
    val layout = mapSheetLayoutFor(position)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(layout.height)
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
            .semantics {
                contentDescription = accessibility.label
                stateDescription = accessibility.stateDescription
                customActions = accessibility.actionLabels.map { label ->
                    CustomAccessibilityAction(label) {
                        onPositionChanged(
                            when (label) {
                                "Expand nearby sales" -> position.expandOneStep()
                                else -> position.collapseOneStep()
                            },
                        )
                        true
                    }
                }
            }
            .testTag(YardScapeTestTags.DiscoveryResultsSheet),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        shadowElevation = 8.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = YardScapeDesign.spacing.large, vertical = 8.dp)) {
            Surface(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(40.dp)
                    .height(4.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            ) {}
            Spacer(modifier = Modifier.height(YardScapeDesign.spacing.small))
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
    val actions = compactMapResultActionsFor(saved)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(YardScapeTestTags.mapResult(event.id))
            .semantics {
                this.selected = selected
                stateDescription = if (selected) "Selected" else "Not selected"
                customActions = listOf(
                    CustomAccessibilityAction("Select on map") {
                        onSelect()
                        true
                    },
                )
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onSelect,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = YardScapeDesign.spacing.small),
            horizontalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.width(126.dp)) {
                ShopperEventArtwork(
                    presentation = event.toShopperEventArtworkPresentation(),
                    height = 106.dp,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    event.title,
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 17.sp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    event.dateLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    event.locationLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    event.categoryLabels.take(3).forEach { label -> CategoryChip(label) }
                }
                TextButton(
                    modifier = Modifier.yardScapeInteractiveTarget(),
                    onClick = onOpen,
                ) {
                    Text(actions.openLabel)
                }
            }
            IconButton(
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = actions.saveLabel },
                onClick = onSave,
            ) {
                Icon(
                    imageVector = FavoriteBorderIcon,
                    contentDescription = null,
                    tint = if (saved) MaterialTheme.colorScheme.secondary else Clay,
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.24f))
    }
}

@Composable
private fun BrowseMarketplaceHeader(
    onHostSelected: () -> Unit,
) {
    val spacing = YardScapeDesign.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.large, vertical = spacing.small),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = YardScapeConfig.appName,
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 28.sp),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "Browse sales",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        OutlinedButton(
            modifier = Modifier.yardScapeInteractiveTarget(),
            shape = MaterialTheme.shapes.extraLarge,
            onClick = onHostSelected,
        ) {
            Text("Host a sale")
        }
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
                .yardScapeInteractiveTarget()
                .testTag(YardScapeTestTags.browseEventCard(eventId))
                .semantics { contentDescription = actions.openLabel },
            onClick = onOpen,
        ) {
            Text(actions.openLabel)
        }
        TextButton(
            modifier = Modifier
                .weight(1f)
                .yardScapeInteractiveTarget()
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
                    .yardScapeInteractiveTarget()
                    .testTag(YardScapeTestTags.browseEventCard(event.id)),
                onClick = onClick,
            ) {
                Text("View sale")
            }
            OutlinedButton(modifier = Modifier.fillMaxWidth().yardScapeInteractiveTarget(), onClick = onSavedToggle) {
                Text(if (isSaved) "Unsave" else "Save sale")
            }
        }
    }
}

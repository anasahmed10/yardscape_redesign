package com.naslabs.yardscape.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.naslabs.yardscape.YardScapeConfig

@Composable
fun BrowseScreen(
    state: ShopperDiscoveryState,
    dataAvailability: AppDataAvailability = AppDataAvailability.Available,
    onEventSelected: (String) -> Unit,
    onHostSelected: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onDateChanged: (DiscoveryDateFilter) -> Unit,
    onDistanceChanged: (DiscoveryDistanceFilter) -> Unit,
    onCategoryToggled: (String) -> Unit,
    onDisplayModeChanged: (DiscoveryDisplayMode) -> Unit,
    onResetFilters: () -> Unit,
    onSavedToggled: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag(YardScapeTestTags.BrowseScreen)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            BrowseHero(
                eventCount = state.items.size,
                onHostSelected = onHostSelected,
            )
        }

        when (dataAvailability) {
            AppDataAvailability.Available -> Unit
            AppDataAvailability.Offline -> item {
                BrowseAvailabilityCard(
                    title = "You're offline",
                    message = "Reconnect to refresh nearby sales. Previously loaded mock data stays visible.",
                )
            }
            is AppDataAvailability.RecoverableError -> item {
                BrowseAvailabilityCard(
                    title = "Couldn't refresh sales",
                    message = dataAvailability.message,
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
            state.hasNoNearbyEvents -> item {
                DiscoveryEmptyState(
                    title = "No nearby sales yet",
                    message = "Check again soon or use Host to add the first sale in this area.",
                    onReset = null,
                )
            }
            state.hasNoMatches -> item {
                DiscoveryEmptyState(
                    title = "No sales match those filters",
                    message = "Clear the filters to see every nearby public preview.",
                    onReset = onResetFilters,
                )
            }
            state.displayMode == DiscoveryDisplayMode.Map -> item {
                ApproximateMapPreview(
                    events = state.items,
                    savedEventIds = state.savedEventIds,
                    onEventSelected = onEventSelected,
                    onSavedToggled = onSavedToggled,
                )
            }
            else -> items(state.items, key = { it.id }) { event ->
                EventPreviewCard(
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
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = state.filters.query,
            onValueChange = onQueryChanged,
            label = { Text("Search sales") },
            placeholder = { Text("Keyword, neighborhood, or city") },
            singleLine = true,
        )
        FilterRow(
            options = DiscoveryDateFilter.entries,
            label = { it.label },
            selected = { it == state.filters.date },
            onSelected = onDateChanged,
        )
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
private fun DiscoveryEmptyState(title: String, message: String, onReset: (() -> Unit)?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(YardScapeTestTags.DiscoveryNoResults),
        colors = CardDefaults.cardColors(containerColor = SkyWash),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(message, style = MaterialTheme.typography.bodyMedium)
            if (onReset != null) {
                OutlinedButton(onClick = onReset) { Text("Show all sales") }
            }
        }
    }
}

@Composable
private fun ApproximateMapPreview(
    events: List<BrowseEventItem>,
    savedEventIds: Set<String>,
    onEventSelected: (String) -> Unit,
    onSavedToggled: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MintMist,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Approximate area map", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Provider-free preview. Pins use neighborhood and broad distance only.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            events.forEach { event ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEventSelected(event.id) },
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("● ${event.locationLabel}", fontWeight = FontWeight.SemiBold)
                        Text(event.title)
                        OutlinedButton(onClick = { onSavedToggled(event.id) }) {
                            Text(if (event.id in savedEventIds) "Unsave" else "Save")
                        }
                    }
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
internal fun EventPreviewCard(
    event: BrowseEventItem,
    isSaved: Boolean,
    onClick: () -> Unit,
    onSavedToggle: () -> Unit,
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
            OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onSavedToggle) {
                Text(if (isSaved) "Unsave" else "Save sale")
            }
        }
    }
}

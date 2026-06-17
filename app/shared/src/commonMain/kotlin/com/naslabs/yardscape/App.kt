package com.naslabs.yardscape

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.naslabs.yardscape.data.SeededYardSaleData
import com.naslabs.yardscape.ui.BrowseEventItem
import com.naslabs.yardscape.ui.EventDetailState
import com.naslabs.yardscape.ui.LocationRevealState
import com.naslabs.yardscape.ui.YardScapeAppState
import com.naslabs.yardscape.ui.YardScapeRoute
import com.naslabs.yardscape.ui.toDetailSections

@Composable
@Preview
fun App() {
    MaterialTheme {
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

                is YardScapeRoute.HostCreateEdit -> RoutePlaceholderScreen(
                    title = "Host Event",
                    onBack = {
                        appState.returnToBrowse()
                        route = appState.route
                    },
                )
            }
        }
    }
}

@Composable
private fun BrowseScreen(
    events: List<BrowseEventItem>,
    onEventSelected: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(
                modifier = Modifier.padding(top = 18.dp, bottom = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = YardScapeConfig.appName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Upcoming nearby sales",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        items(events, key = { it.id }) { event ->
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
private fun EventPreviewCard(
    event: BrowseEventItem,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = event.dateLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StatusLabel(text = event.statusLabel)
            }

            Text(
                text = event.locationLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

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
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(onClick = onBack) {
                    Text("Back")
                }
                Text(
                    text = detail.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = detail.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            PhotoStrip(descriptions = detail.photos.mapNotNull { it.description })
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
                Text(
                    text = detail.rsvpPrompt,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (state.shouldShowRsvpAction) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
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
private fun PhotoStrip(descriptions: List<String>) {
    val photoLabels = descriptions.ifEmpty { listOf("Sale photo coming soon") }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        photoLabels.forEach { description ->
            Box(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(8.dp),
                    )
                    .heightIn(min = 72.dp)
                    .padding(12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
    }
}

@Composable
private fun LocationAccessPanel(revealState: LocationRevealState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (revealState) {
                is LocationRevealState.Revealed -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceContainerHighest
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = revealState.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "RSVP",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "For this test workflow, RSVP is auto-accepted so you can verify the protected location reveal boundary.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(14.dp))
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onConfirm,
        ) {
            Text("Confirm RSVP")
        }
        Button(onClick = onBack) {
            Text("Back")
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
        )
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
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(6.dp),
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
    )
}

@Composable
private fun StatusLabel(text: String) {
    Text(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(6.dp),
            )
            .padding(horizontal = 8.dp, vertical = 5.dp),
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
    )
}

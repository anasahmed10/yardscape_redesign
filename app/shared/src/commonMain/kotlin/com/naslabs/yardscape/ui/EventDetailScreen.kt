package com.naslabs.yardscape.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.naslabs.yardscape.data.SeededYardSaleData

@Composable
fun PublicEventDetailScreen(
    state: EventDetailState?,
    onBack: () -> Unit,
    onRsvp: () -> Unit,
) {
    if (state == null) {
        RoutePlaceholderScreen(title = "Event unavailable", onBack = onBack)
        return
    }
    val detail = state.detail
    val spacing = YardScapeDesign.spacing

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = spacing.large),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
    ) {
        item {
            Column(
                modifier = Modifier.padding(top = 18.dp),
                verticalArrangement = Arrangement.spacedBy(spacing.medium),
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
                    color = MaterialTheme.colorScheme.onSurface,
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
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
                verticalArrangement = Arrangement.spacedBy(spacing.small),
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
            PrivacyNote(text = "Marketplace safety: visit only during listed hours, do not enter private areas, and leave if the situation feels unsafe.")
        }

        if (state.attendanceState == EventAttendanceState.AtCapacity) {
            item {
                PrivacyNote(text = "This sale is at capacity, so new RSVPs are paused.")
            }
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
internal fun LocationAccessPanel(revealState: LocationRevealState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(YardScapeTestTags.LocationAccessPanel),
        shape = MaterialTheme.shapes.small,
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

package com.naslabs.yardscape.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.naslabs.yardscape.data.SeededYardSaleData

internal data class LocationRevealPresentation(
    val statusLabel: String,
    val title: String,
    val supportingText: String,
    val exactAddressLabel: String? = null,
    val actionLabel: String? = null,
) {
    val hasActiveAccess: Boolean
        get() = exactAddressLabel != null && actionLabel != null
}

internal fun LocationRevealState.toLocationRevealPresentation(): LocationRevealPresentation =
    when (this) {
        is LocationRevealState.Revealed -> LocationRevealPresentation(
            statusLabel = "Access granted",
            title = title,
            supportingText = "Your accepted RSVP includes protected location access for the active sale window.",
            exactAddressLabel = message,
            actionLabel = "Directions",
        )
        else -> LocationRevealPresentation(
            statusLabel = "Privacy protected",
            title = title,
            supportingText = message,
        )
    }

@Composable
fun PublicEventDetailScreen(
    state: EventDetailState?,
    onBack: () -> Unit,
    onRsvp: () -> Unit,
    onDirections: () -> Unit,
    onReport: () -> Unit,
    onBlock: () -> Unit,
) {
    if (state == null) {
        RoutePlaceholderScreen(title = "Event unavailable", onBack = onBack)
        return
    }
    val detail = state.detail
    val spacing = YardScapeDesign.spacing
    val blockActionLabel =
        if (state.revealState is LocationRevealState.Blocked) "Review blocked host" else "Block host"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = spacing.large),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
    ) {
        item {
            Column(
                modifier = Modifier.padding(top = spacing.small),
                verticalArrangement = Arrangement.spacedBy(spacing.medium),
            ) {
                TextButton(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .semantics { contentDescription = "Back to previous screen" },
                    onClick = onBack,
                ) {
                    Text("Back")
                }
                ShopperEventArtwork(
                    presentation = detail.toShopperEventArtworkPresentation(),
                    height = 256.dp,
                )
                StatusLabel(text = "Public event preview")
                Text(
                    text = detail.title,
                    style = MaterialTheme.typography.headlineLarge,
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
            Column(
                verticalArrangement = Arrangement.spacedBy(spacing.medium),
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(spacing.small),
                    verticalArrangement = Arrangement.spacedBy(spacing.small),
                ) {
                    detail.categories.forEach { label -> CategoryChip(label = label) }
                }
                Text(
                    text = detail.rsvpPrompt,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                when {
                    state.attendanceState == EventAttendanceState.AtCapacity ->
                        ShopperStatePanel(
                            title = "Sale at capacity",
                            message = "New RSVPs are paused. Check back later or browse another nearby sale.",
                        )
                    state.shouldShowRsvpAction -> Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag(YardScapeTestTags.RsvpAction)
                            .semantics { contentDescription = "RSVP for this sale" },
                        onClick = onRsvp,
                    ) {
                        Text(
                            if (state.revealState is LocationRevealState.Pending) {
                                "Review RSVP"
                            } else {
                                "RSVP for this sale"
                            },
                        )
                    }
                }
            }
        }

        item {
            EventMetadataSection(
                sections = detail.toDetailSections(SeededYardSaleData.BASE_NOW_EPOCH_MILLIS)
                    .filter { (label, value) -> label != "Categories" && value.isNotBlank() },
            )
        }

        item {
            LocationAccessPanel(
                revealState = state.revealState,
                onDirections = onDirections,
            )
        }

        item {
            PrivacyNote(text = "Marketplace safety: visit only during listed hours, do not enter private areas, and leave if the situation feels unsafe.")
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                OutlinedButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .semantics { contentDescription = "Report sale" },
                    onClick = onReport,
                ) {
                    Text("Report sale")
                }
                TextButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .semantics { contentDescription = blockActionLabel },
                    onClick = onBlock,
                ) {
                    Text(blockActionLabel)
                }
            }
        }
        item {
            HorizontalDivider(
                modifier = Modifier.padding(bottom = spacing.large),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}

@Composable
private fun EventMetadataSection(sections: List<Pair<String, String>>) {
    val spacing = YardScapeDesign.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
        ShopperSectionHeader(
            title = "Sale details",
            supportingText = "Public information from the host",
        )
        sections.forEachIndexed { index, (label, value) ->
            Column(verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (index < sections.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
internal fun LocationAccessPanel(
    revealState: LocationRevealState,
    onDirections: () -> Unit,
) {
    val presentation = revealState.toLocationRevealPresentation()
    val accentColor = if (presentation.hasActiveAccess) Evergreen else MarketBlue
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, accentColor, MaterialTheme.shapes.medium)
            .testTag(YardScapeTestTags.LocationAccessPanel),
        shape = MaterialTheme.shapes.medium,
        color = if (presentation.hasActiveAccess) MintMist else SkyWash,
    ) {
        Column(
            modifier = Modifier.padding(YardScapeDesign.spacing.large),
            verticalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.small),
        ) {
            StatusLabel(text = presentation.statusLabel)
            Text(
                text = presentation.title,
                style = MaterialTheme.typography.titleLarge,
                color = ForestInk,
            )
            Text(
                text = presentation.supportingText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            presentation.exactAddressLabel?.let { exactAddress ->
                Text(
                    modifier = Modifier.testTag(YardScapeTestTags.ExactLocationContent),
                    text = exactAddress,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ForestInk,
                )
            }
            presentation.actionLabel?.let { actionLabel ->
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag(YardScapeTestTags.DirectionsAction)
                        .semantics { contentDescription = actionLabel },
                    onClick = onDirections,
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

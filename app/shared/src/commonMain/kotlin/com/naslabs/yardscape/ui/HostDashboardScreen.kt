package com.naslabs.yardscape.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal enum class HostMarketplaceLayout { Compact, Expanded }

internal fun hostMarketplaceLayoutFor(width: Dp): HostMarketplaceLayout =
    if (width >= 760.dp) HostMarketplaceLayout.Expanded else HostMarketplaceLayout.Compact

internal enum class HostEditorialSurface { Dashboard, Editor, Attendance }

internal data class HostEditorialSurfacePresentation(
    val artworkSize: Dp,
    val actionsInline: Boolean,
)

internal fun hostEditorialSurfacePresentationFor(
    surface: HostEditorialSurface,
    layout: HostMarketplaceLayout,
): HostEditorialSurfacePresentation =
    HostEditorialSurfacePresentation(
        artworkSize = if (surface == HostEditorialSurface.Dashboard) 128.dp else 96.dp,
        actionsInline = layout == HostMarketplaceLayout.Expanded,
    )

@Composable
fun HostDashboardScreen(
    events: List<HostEventItem>,
    onCreateEvent: () -> Unit,
    onEditEvent: (String) -> Unit,
    onManageAttendees: (String) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val layout = hostMarketplaceLayoutFor(maxWidth)
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 1120.dp)
                    .fillMaxWidth()
                    .testTag(YardScapeTestTags.HostDashboardScreen)
                    .padding(horizontal = YardScapeDesign.spacing.large),
                verticalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.medium),
            ) {
                item {
                    Column(
                        modifier = Modifier.padding(top = YardScapeDesign.spacing.large),
                        verticalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.small),
                    ) {
                        StatusLabel("Host workspace")
                        Text("Your sales", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Keep every sale polished for shoppers while exact location access stays protected.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(
                            modifier = Modifier
                                .then(if (layout == HostMarketplaceLayout.Compact) Modifier.fillMaxWidth() else Modifier.widthIn(min = 200.dp))
                                .heightIn(min = 48.dp)
                                .semantics { contentDescription = "Create a sale" },
                            onClick = onCreateEvent,
                        ) { Text("Create a sale") }
                    }
                }
                if (events.isEmpty()) {
                    item {
                        ShopperStatePanel(
                            title = "Ready when you are",
                            message = "Start a sale draft to choose its public details, RSVP rules, and protected location settings.",
                            actionLabel = "Create a sale",
                            onAction = onCreateEvent,
                        )
                    }
                } else {
                    items(events, key = { it.id }) { event ->
                        HostDashboardRow(
                            event = event,
                            layout = layout,
                            onEditEvent = onEditEvent,
                            onManageAttendees = onManageAttendees,
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
                    }
                }
            }
        }
    }
}

@Composable
private fun HostDashboardRow(
    event: HostEventItem,
    layout: HostMarketplaceLayout,
    onEditEvent: (String) -> Unit,
    onManageAttendees: (String) -> Unit,
) {
    val presentation = hostEditorialSurfacePresentationFor(HostEditorialSurface.Dashboard, layout)
    val actionModifier = Modifier.heightIn(min = 48.dp)
    val capacity = event.attendeeCap?.let { " of $it" }.orEmpty()
    val rsvpSummary = buildString {
        append("${event.acceptedRsvpCount}$capacity attending")
        if (event.pendingRsvpCount > 0) append(" · ${event.pendingRsvpCount} pending")
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(YardScapeDesign.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.medium),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.medium),
                verticalAlignment = Alignment.Top,
            ) {
                Box(modifier = Modifier.width(presentation.artworkSize).height(presentation.artworkSize)) {
                    ShopperEventArtwork(
                        presentation = ShopperEventArtworkPresentation(event.id, event.photoReference),
                        modifier = Modifier.fillMaxSize(),
                        height = presentation.artworkSize,
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.extraSmall),
                ) {
                    StatusLabel(event.statusLabel)
                    Text(event.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(event.dateLabel, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    Text(event.publicLocationLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(rsvpSummary, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (presentation.actionsInline) {
                Row(horizontalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.small)) {
                    OutlinedButton(
                        modifier = actionModifier.weight(1f).semantics { contentDescription = "Edit ${event.title}" },
                        onClick = { onEditEvent(event.id) },
                    ) { Text("Edit sale") }
                    Button(
                        modifier = actionModifier.weight(1f).semantics { contentDescription = "Manage attendees for ${event.title}" },
                        onClick = { onManageAttendees(event.id) },
                    ) { Text("Manage attendees") }
                }
            } else {
                OutlinedButton(
                    modifier = actionModifier.fillMaxWidth().semantics { contentDescription = "Edit ${event.title}" },
                    onClick = { onEditEvent(event.id) },
                ) { Text("Edit sale") }
                Button(
                    modifier = actionModifier.fillMaxWidth().semantics { contentDescription = "Manage attendees for ${event.title}" },
                    onClick = { onManageAttendees(event.id) },
                ) { Text("Manage attendees") }
            }
        }
    }
}

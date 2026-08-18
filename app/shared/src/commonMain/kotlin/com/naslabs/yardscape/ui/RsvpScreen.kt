package com.naslabs.yardscape.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun RsvpScreen(
    onConfirm: () -> Unit,
    onBack: () -> Unit,
) {
    val spacing = YardScapeDesign.spacing
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = spacing.large),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
    ) {
        item {
            TextButton(
                modifier = Modifier
                    .padding(top = spacing.small)
                    .heightIn(min = 48.dp)
                    .semantics { contentDescription = "Back to event" },
                onClick = onBack,
            ) {
                Text("Back")
            }
        }
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(spacing.large),
            ) {
                StatusLabel(text = "RSVP")
                Text(
                    text = "Join this yard sale",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Confirm that you plan to attend. This test workflow accepts the RSVP immediately and returns you to the event.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ShopperStatePanel(
                    title = "Protected location",
                    message = "The exact address and directions appear only while your accepted RSVP has active access.",
                )
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag(YardScapeTestTags.RsvpConfirmAction)
                        .semantics { contentDescription = "Confirm RSVP" },
                    onClick = onConfirm,
                ) {
                    Text("Confirm RSVP")
                }
                OutlinedButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .semantics { contentDescription = "Back to event" },
                    onClick = onBack,
                ) {
                    Text("Back to event")
                }
            }
        }
    }
}

package com.naslabs.yardscape.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun MessagesScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag(YardScapeTestTags.MessagesScreen)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Messages", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Your inbox is empty.")
                Text(
                    "Messages are available only for an active, accepted RSVP. RSVP cancellation, " +
                        "host revocation, blocking, cancellation, or expiry removes access.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Messaging delivery is not available in this preview.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

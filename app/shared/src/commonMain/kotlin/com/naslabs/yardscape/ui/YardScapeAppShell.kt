package com.naslabs.yardscape.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.naslabs.yardscape.domain.UserRole

@Composable
fun YardScapeAppShell(
    route: YardScapeRoute,
    activeUserRole: UserRole,
    onDestinationSelected: (YardScapePrimaryDestination) -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag(YardScapeTestTags.AppShell),
    ) {
        AppShellHeader(route = route, activeUserRole = activeUserRole)
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(modifier = Modifier.widthIn(max = 960.dp).fillMaxSize()) {
                content()
            }
        }
        NavigationBar {
            YardScapePrimaryDestination.entries.forEach { destination ->
                NavigationBarItem(
                    modifier = Modifier.testTag(YardScapeTestTags.primaryDestination(destination)),
                    selected = route.primaryDestination == destination,
                    onClick = { onDestinationSelected(destination) },
                    icon = {
                        Text(
                            text = destination.label.take(1),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    label = { Text(destination.label) },
                )
            }
        }
    }
}

@Composable
private fun AppShellHeader(route: YardScapeRoute, activeUserRole: UserRole) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = route.destinationLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = route.primaryDestination.contextLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "Mock ${activeUserRole.name.lowercase()} data",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

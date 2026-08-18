package com.naslabs.yardscape.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.naslabs.yardscape.YardScapeConfig

@Composable
fun YardScapeAppShell(
    route: YardScapeRoute,
    onDestinationSelected: (YardScapePrimaryDestination) -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag(YardScapeTestTags.AppShell),
    ) {
        AppShellHeader(route = route)
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
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag(YardScapeTestTags.primaryDestination(destination)),
                    selected = route.primaryDestination == destination,
                    onClick = { onDestinationSelected(destination) },
                    icon = {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = null,
                        )
                    },
                    label = { Text(destination.label) },
                )
            }
        }
    }
}

@Composable
private fun AppShellHeader(route: YardScapeRoute) {
    val spacing = YardScapeDesign.spacing
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = spacing.large, vertical = spacing.small),
            verticalArrangement = Arrangement.spacedBy(spacing.extraSmall),
        ) {
            Text(
                text = YardScapeConfig.appName,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = route.destinationLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val YardScapePrimaryDestination.icon: ImageVector
    get() = when (this) {
        YardScapePrimaryDestination.Browse -> Icons.Outlined.Search
        YardScapePrimaryDestination.MyFinds -> Icons.Outlined.FavoriteBorder
        YardScapePrimaryDestination.Host -> Icons.Outlined.AddCircleOutline
        YardScapePrimaryDestination.Messages -> Icons.Outlined.ChatBubbleOutline
        YardScapePrimaryDestination.Account -> Icons.Outlined.PersonOutline
    }

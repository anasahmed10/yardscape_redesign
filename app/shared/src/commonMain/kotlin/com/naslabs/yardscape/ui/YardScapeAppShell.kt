package com.naslabs.yardscape.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
internal fun YardScapeAppShell(
    route: YardScapeRoute,
    onDestinationSelected: (YardScapePrimaryDestination) -> Unit,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val marketplaceLayout = browseMarketplaceLayoutFor(maxWidth)
        val editorialLayout = marketplaceEditorialLayoutFor(maxWidth)
        val contentWidth = when (route) {
            YardScapeRoute.Browse -> 1280.dp
            else -> when (editorialLayout) {
                MarketplaceEditorialLayout.Compact -> maxWidth
                MarketplaceEditorialLayout.Expanded -> marketplaceEditorialContentWidthFor(maxWidth)
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .testTag(YardScapeTestTags.AppShell),
        ) {
            marketplaceEditorialHeaderFor(route)?.let { presentation ->
                MarketplaceEditorialHeader(
                    presentation = presentation,
                )
            }
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.TopCenter,
            ) {
                Box(modifier = Modifier.widthIn(max = contentWidth).fillMaxSize()) {
                    content()
                }
            }
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
            ) {
                YardScapePrimaryDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        modifier = Modifier
                            .heightIn(min = 56.dp)
                            .testTag(YardScapeTestTags.primaryDestination(destination)),
                        selected = route.primaryDestination == destination,
                        onClick = { onDestinationSelected(destination) },
                        icon = {
                            Icon(
                                imageVector = navigationIcon(destination),
                                contentDescription = null,
                            )
                        },
                        label = {
                            Text(marketplaceNavigationLabelFor(destination, marketplaceLayout))
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onSecondary,
                            selectedTextColor = MaterialTheme.colorScheme.secondary,
                            indicatorColor = MaterialTheme.colorScheme.secondary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        }
    }
}

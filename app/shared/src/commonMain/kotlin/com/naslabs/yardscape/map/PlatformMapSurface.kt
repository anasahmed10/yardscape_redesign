package com.naslabs.yardscape.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import com.naslabs.yardscape.YardScapeConfig
import com.naslabs.yardscape.domain.MapViewport
import com.naslabs.yardscape.domain.PublicEventMarker
import com.naslabs.yardscape.domain.PublicMapCluster

@Immutable
data class PlatformMapStyle(
    val styleUri: String,
    val attribution: String,
) {
    companion object {
        val OpenFreeMapLiberty = PlatformMapStyle(
            styleUri = YardScapeConfig.mapStyleUrl,
            attribution = YardScapeConfig.mapAttribution,
        )
    }
}

@Immutable
data class PlatformMapState(
    val viewport: MapViewport,
    val markers: List<PublicEventMarker> = emptyList(),
    val clusters: List<PublicMapCluster> = emptyList(),
    val selectedEventId: String? = null,
)

enum class PlatformMapCapability {
    Interactive,
    StaticFallback,
}

expect fun platformMapCapability(): PlatformMapCapability

expect fun platformMapSupportsComposeOverlay(): Boolean

@Composable
expect fun PlatformMapSurface(
    state: PlatformMapState,
    modifier: Modifier = Modifier,
    style: PlatformMapStyle = PlatformMapStyle.OpenFreeMapLiberty,
    onViewportChanged: (MapViewport) -> Unit = {},
    onMarkerSelected: (String) -> Unit = {},
    onClusterSelected: (String) -> Unit = {},
    onMapLoaded: () -> Unit = {},
    onMapLoadFailed: (String?) -> Unit = {},
)

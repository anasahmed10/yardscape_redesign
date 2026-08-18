package com.naslabs.yardscape.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.naslabs.yardscape.domain.MapViewport

actual fun platformMapCapability(): PlatformMapCapability = PlatformMapCapability.StaticFallback

actual fun platformMapSupportsComposeOverlay(): Boolean = true

@Composable
actual fun PlatformMapSurface(
    state: PlatformMapState,
    modifier: Modifier,
    style: PlatformMapStyle,
    onViewportChanged: (MapViewport) -> Unit,
    onMarkerSelected: (String) -> Unit,
    onClusterSelected: (String) -> Unit,
    onMapLoaded: () -> Unit,
    onMapLoadFailed: (String?) -> Unit,
) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val roadColor = Color(0xFFD7CCB8)
            repeat(6) { index ->
                val fraction = (index + 1) / 7f
                drawLine(
                    color = roadColor,
                    start = Offset(size.width * fraction, 0f),
                    end = Offset(size.width * (1f - fraction / 3f), size.height),
                    strokeWidth = 3f,
                )
                drawLine(
                    color = roadColor,
                    start = Offset(0f, size.height * fraction),
                    end = Offset(size.width, size.height * (fraction * 0.8f)),
                    strokeWidth = 3f,
                )
            }
            state.markers.forEachIndexed { index, _ ->
                val x = size.width * (0.2f + (index % 4) * 0.2f)
                val y = size.height * (0.3f + (index % 3) * 0.18f)
                drawCircle(Color(0xFF3478A0), radius = 9.dp.toPx(), center = Offset(x, y))
                drawCircle(Color.White, radius = 3.dp.toPx(), center = Offset(x, y))
            }
        }
        Text(
            text = "Interactive map unavailable in this browser. All nearby sales remain available in the list.",
            modifier = Modifier.align(Alignment.Center).padding(24.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = style.attribution,
            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

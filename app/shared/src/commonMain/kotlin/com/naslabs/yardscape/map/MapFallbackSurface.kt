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

@Composable
fun MapFallbackSurface(
    state: PlatformMapState,
    modifier: Modifier = Modifier,
    style: PlatformMapStyle = PlatformMapStyle.OpenFreeMapLiberty,
) {
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
        Canvas(Modifier.fillMaxSize()) {
            val roadColor = Color(0xFFD7CCB8)
            repeat(6) { index ->
                val fraction = (index + 1) / 7f
                drawLine(roadColor, Offset(size.width * fraction, 0f), Offset(size.width * (1f - fraction / 3f), size.height), 3f)
                drawLine(roadColor, Offset(0f, size.height * fraction), Offset(size.width, size.height * fraction * 0.8f), 3f)
            }
            state.markers.forEachIndexed { index, _ ->
                val center = Offset(
                    size.width * (0.2f + (index % 4) * 0.2f),
                    size.height * (0.3f + (index % 3) * 0.18f),
                )
                drawCircle(Color(0xFF3478A0), 12.dp.toPx(), center)
                drawCircle(Color.White, 4.dp.toPx(), center)
            }
        }
        Text(
            "Map unavailable. All nearby sales remain available in the list.",
            Modifier.align(Alignment.Center).padding(24.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            style.attribution,
            Modifier.align(Alignment.BottomEnd).padding(8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

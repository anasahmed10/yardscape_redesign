package com.naslabs.yardscape.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun DetailRow(label: String, value: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
internal fun RoutePlaceholderScreen(
    title: String,
    onBack: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Button(onClick = onBack) {
                Text("Back")
            }
        }
    }
}

@Composable
internal fun CategoryChip(label: String) {
    Text(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.extraSmall,
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        text = label.replaceFirstChar { it.uppercase() },
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
internal fun StatusLabel(text: String) {
    Text(
        modifier = Modifier
            .background(
                color = SunTag,
                shape = MaterialTheme.shapes.extraSmall,
            )
            .widthIn(max = 180.dp)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = CocoaInk,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
internal fun InfoChip(text: String) {
    Text(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.extraSmall,
            )
            .widthIn(max = 220.dp)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
internal fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val background = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val content = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Text(
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .background(background)
            .border(
                width = 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = MaterialTheme.shapes.extraSmall,
            )
            .clickable(onClick = onClick)
            .widthIn(max = 160.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = content,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
internal fun EventPhotoPreview(
    title: String,
    description: String?,
    seed: String,
) {
    val accent = when (seed.length % 3) {
        0 -> PhotoLeaf
        1 -> PhotoMarket
        else -> PhotoClay
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(136.dp)
            .clip(MaterialTheme.shapes.small)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(accent, accent.copy(alpha = 0.52f), Linen),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.72f), MaterialTheme.shapes.small)
            .padding(14.dp),
        contentAlignment = Alignment.BottomStart,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = description ?: "Preview photo coming soon",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = ForestInk,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = ForestInk.copy(alpha = 0.72f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun PrivacyNote(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Text(
            modifier = Modifier.padding(12.dp),
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
internal fun FormSectionLabel(text: String) {
    Text(
        modifier = Modifier.padding(top = 4.dp),
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )
}

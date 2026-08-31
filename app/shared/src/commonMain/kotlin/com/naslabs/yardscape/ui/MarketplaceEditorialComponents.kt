package com.naslabs.yardscape.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.naslabs.yardscape.YardScapeConfig

@Composable
internal fun MarketplaceEditorialHeader(
    presentation: MarketplaceEditorialHeaderPresentation,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag(YardScapeTestTags.EditorialHeader),
        color = MaterialTheme.colorScheme.background,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = YardScapeDesign.spacing.large,
                vertical = YardScapeDesign.spacing.small,
            ),
            horizontalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (presentation.showsBackNavigation && onBack != null) {
                MarketplaceEditorialBackNavigation(onBack = onBack)
            }
            Box(modifier = Modifier.weight(1f)) {
                androidx.compose.foundation.layout.Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = YardScapeConfig.appName,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = presentation.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
internal fun MarketplaceEditorialBackNavigation(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        modifier = modifier
            .heightIn(min = YardScapeMinimumInteractiveTarget)
            .testTag(YardScapeTestTags.EditorialBackNavigation)
            .semantics { contentDescription = "Back to previous screen" },
        onClick = onBack,
    ) {
        Text("Back", fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun <T> MarketplaceSegmentedControl(
    options: List<MarketplaceSegmentOption<T>>,
    onSelected: (T) -> Unit,
    testTagFor: (T) -> String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.small),
    ) {
        options.forEach { option ->
            val presentation = marketplaceSegmentPresentationFor(option.isSelected)
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = presentation.minimumHeight)
                    .testTag(testTagFor(option.value))
                    .semantics {
                        selected = presentation.isSelected
                        contentDescription = "${option.label}${if (presentation.isSelected) ", selected" else ""}"
                    },
                onClick = { onSelected(option.value) },
                shape = MaterialTheme.shapes.extraLarge,
                color = if (presentation.isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surface
                },
                contentColor = if (presentation.isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                border = BorderStroke(
                    1.dp,
                    if (presentation.isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                    },
                ),
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

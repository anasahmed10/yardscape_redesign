package com.naslabs.yardscape.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun YardScapeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Evergreen,
            onPrimary = Color.White,
            primaryContainer = MintMist,
            onPrimaryContainer = ForestInk,
            secondary = Clay,
            onSecondary = Color.White,
            secondaryContainer = PeachWash,
            onSecondaryContainer = CocoaInk,
            tertiary = MarketBlue,
            onTertiary = Color.White,
            tertiaryContainer = SkyWash,
            onTertiaryContainer = NavyInk,
            background = Linen,
            onBackground = ForestInk,
            surface = Color.White,
            onSurface = ForestInk,
            surfaceVariant = Stone,
            onSurfaceVariant = OliveText,
            surfaceContainer = Color.White,
            surfaceContainerHighest = Stone,
            outline = SageLine,
        ),
        content = content,
    )
}

internal val Evergreen = Color(0xFF2F6F4E)
internal val ForestInk = Color(0xFF16251D)
internal val OliveText = Color(0xFF596457)
internal val Linen = Color(0xFFF8F4EC)
internal val MintMist = Color(0xFFDDEFE3)
internal val SageLine = Color(0xFFC8D6C4)
internal val Stone = Color(0xFFEDE7DC)
internal val Clay = Color(0xFFC56247)
internal val CocoaInk = Color(0xFF3B2117)
internal val PeachWash = Color(0xFFF8D9C9)
internal val MarketBlue = Color(0xFF386E7F)
internal val NavyInk = Color(0xFF13262D)
internal val SkyWash = Color(0xFFD9ECF0)
internal val SunTag = Color(0xFFFFD166)
internal val PhotoLeaf = Color(0xFF98C47B)
internal val PhotoMarket = Color(0xFF73A6AD)
internal val PhotoClay = Color(0xFFE09B72)

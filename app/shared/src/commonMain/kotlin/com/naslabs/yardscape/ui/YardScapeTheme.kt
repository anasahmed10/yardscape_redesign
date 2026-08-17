package com.naslabs.yardscape.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class YardScapeSpacing(
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 12.dp,
    val large: Dp = 16.dp,
    val extraLarge: Dp = 24.dp,
    val section: Dp = 32.dp,
)

internal val DefaultYardScapeSpacing = YardScapeSpacing()
internal val LocalYardScapeSpacing = staticCompositionLocalOf { DefaultYardScapeSpacing }

object YardScapeDesign {
    val spacing: YardScapeSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalYardScapeSpacing.current
}

private val BaseTypography = Typography()

internal val YardScapeTypography = Typography(
    headlineLarge = BaseTypography.headlineLarge.copy(fontWeight = FontWeight.Bold),
    headlineMedium = BaseTypography.headlineMedium.copy(fontWeight = FontWeight.Bold),
    headlineSmall = BaseTypography.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = BaseTypography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = BaseTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = BaseTypography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
)

internal val YardScapeShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun YardScapeTheme(content: @Composable () -> Unit) {
    androidx.compose.runtime.CompositionLocalProvider(LocalYardScapeSpacing provides DefaultYardScapeSpacing) {
        MaterialTheme(
            colorScheme = YardScapeLightColorScheme,
            typography = YardScapeTypography,
            shapes = YardScapeShapes,
            content = content,
        )
    }
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

internal val YardScapeLightColorScheme = lightColorScheme(
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
)

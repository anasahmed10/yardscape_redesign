package com.naslabs.yardscape.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class YardScapeThemeTest {
    @Test
    fun spacingScaleProvidesStableMobileFirstSteps() {
        assertEquals(
            listOf(4.dp, 8.dp, 12.dp, 16.dp, 24.dp, 32.dp),
            DefaultYardScapeSpacing.run {
                listOf(extraSmall, small, medium, large, extraLarge, section)
            },
        )
    }

    @Test
    fun semanticColorRolesRetainBrandAndPrivacyContrast() {
        assertEquals(Evergreen, YardScapeLightColorScheme.primary)
        assertEquals(ForestInk, YardScapeLightColorScheme.onBackground)
        assertEquals(MintMist, YardScapeLightColorScheme.primaryContainer)
        assertEquals(ForestInk, YardScapeLightColorScheme.onPrimaryContainer)
        assertNotEquals(YardScapeLightColorScheme.primary, YardScapeLightColorScheme.onPrimary)
    }

    @Test
    fun semanticForegroundBackgroundPairsMeetWcagContrastMinimums() {
        val scheme = YardScapeLightColorScheme
        val normalTextMinimum = 4.5
        val largeTextOrUiMinimum = 3.0
        val rolePairs = listOf(
            ContrastRolePair("onPrimary / primary", scheme.onPrimary, scheme.primary, normalTextMinimum),
            ContrastRolePair("onPrimaryContainer / primaryContainer", scheme.onPrimaryContainer, scheme.primaryContainer, normalTextMinimum),
            ContrastRolePair("onSecondary / secondary", scheme.onSecondary, scheme.secondary, normalTextMinimum),
            ContrastRolePair("onSecondaryContainer / secondaryContainer", scheme.onSecondaryContainer, scheme.secondaryContainer, normalTextMinimum),
            ContrastRolePair("onTertiary / tertiary", scheme.onTertiary, scheme.tertiary, normalTextMinimum),
            ContrastRolePair("onTertiaryContainer / tertiaryContainer", scheme.onTertiaryContainer, scheme.tertiaryContainer, normalTextMinimum),
            ContrastRolePair("onError / error", scheme.onError, scheme.error, normalTextMinimum),
            ContrastRolePair("onErrorContainer / errorContainer", scheme.onErrorContainer, scheme.errorContainer, normalTextMinimum),
            ContrastRolePair("onBackground / background", scheme.onBackground, scheme.background, normalTextMinimum),
            ContrastRolePair("onSurface / surface", scheme.onSurface, scheme.surface, normalTextMinimum),
            ContrastRolePair("onSurfaceVariant / surfaceVariant", scheme.onSurfaceVariant, scheme.surfaceVariant, normalTextMinimum),
            ContrastRolePair("onSurface / surfaceContainer", scheme.onSurface, scheme.surfaceContainer, normalTextMinimum),
            ContrastRolePair("onSurface / surfaceContainerHighest", scheme.onSurface, scheme.surfaceContainerHighest, normalTextMinimum),
            ContrastRolePair("onSurfaceVariant / surfaceContainerHighest", scheme.onSurfaceVariant, scheme.surfaceContainerHighest, normalTextMinimum),
            ContrastRolePair("onSurfaceVariant / secondaryContainer", scheme.onSurfaceVariant, scheme.secondaryContainer, normalTextMinimum),
            ContrastRolePair("inverseOnSurface / inverseSurface", scheme.inverseOnSurface, scheme.inverseSurface, normalTextMinimum),
            ContrastRolePair("primary / surface", scheme.primary, scheme.surface, normalTextMinimum),
            ContrastRolePair("outline / surface", scheme.outline, scheme.surface, largeTextOrUiMinimum),
        )

        rolePairs.forEach { pair ->
            val ratio = contrastRatio(pair.foreground, pair.background)
            assertTrue(
                ratio >= pair.minimum,
                "${pair.name} contrast was ${ratio.formatRatio()}:1; expected at least ${pair.minimum}:1",
            )
        }
    }

    @Test
    fun sharedShapesCoverChipsCardsAndLargerSurfaces() {
        assertEquals(RoundedCornerShape(6.dp), YardScapeShapes.extraSmall)
        assertEquals(RoundedCornerShape(8.dp), YardScapeShapes.small)
        assertEquals(RoundedCornerShape(12.dp), YardScapeShapes.medium)
        assertEquals(RoundedCornerShape(24.dp), YardScapeShapes.extraLarge)
    }

    @Test
    fun typographyUsesPlatformSerifForDisplayAndSansSerifForUiCopy() {
        assertEquals(FontFamily.Serif, YardScapeTypography.displayLarge.fontFamily)
        assertEquals(FontFamily.Serif, YardScapeTypography.headlineMedium.fontFamily)
        assertEquals(FontFamily.SansSerif, YardScapeTypography.bodyLarge.fontFamily)
        assertEquals(FontFamily.SansSerif, YardScapeTypography.labelLarge.fontFamily)
    }

    @Test
    fun typographyEncodesWeightHierarchyWithoutFontBinaries() {
        assertEquals(FontWeight.Bold, YardScapeTypography.headlineMedium.fontWeight)
        assertEquals(FontWeight.SemiBold, YardScapeTypography.titleLarge.fontWeight)
        assertEquals(FontWeight.SemiBold, YardScapeTypography.labelLarge.fontWeight)
    }

    private data class ContrastRolePair(
        val name: String,
        val foreground: Color,
        val background: Color,
        val minimum: Double,
    )

    private fun contrastRatio(first: Color, second: Color): Double {
        val firstLuminance = first.relativeLuminance()
        val secondLuminance = second.relativeLuminance()
        return (max(firstLuminance, secondLuminance) + 0.05) /
            (min(firstLuminance, secondLuminance) + 0.05)
    }

    private fun Color.relativeLuminance(): Double =
        0.2126 * red.toDouble().linearized() +
            0.7152 * green.toDouble().linearized() +
            0.0722 * blue.toDouble().linearized()

    private fun Double.linearized(): Double =
        if (this <= 0.04045) {
            this / 12.92
        } else {
            ((this + 0.055) / 1.055).pow(2.4)
        }

    private fun Double.formatRatio(): String = ((this * 100.0).toInt() / 100.0).toString()
}

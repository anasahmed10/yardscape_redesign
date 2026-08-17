package com.naslabs.yardscape.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

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
    fun sharedShapesCoverChipsCardsAndLargerSurfaces() {
        assertEquals(RoundedCornerShape(6.dp), YardScapeShapes.extraSmall)
        assertEquals(RoundedCornerShape(8.dp), YardScapeShapes.small)
        assertEquals(RoundedCornerShape(12.dp), YardScapeShapes.medium)
        assertEquals(RoundedCornerShape(24.dp), YardScapeShapes.extraLarge)
    }

    @Test
    fun typographyEncodesHierarchyWithoutPlatformFonts() {
        assertEquals(FontWeight.Bold, YardScapeTypography.headlineMedium.fontWeight)
        assertEquals(FontWeight.SemiBold, YardScapeTypography.titleLarge.fontWeight)
        assertEquals(FontWeight.SemiBold, YardScapeTypography.labelLarge.fontWeight)
    }
}

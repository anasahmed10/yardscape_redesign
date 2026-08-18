package com.naslabs.yardscape.ui

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class YardScapeNavigationIconsTest {
    @Test
    fun primaryDestinationsUseDistinctRecognizableMaterialIcons() {
        val icons = YardScapePrimaryDestination.entries.map(::navigationIcon)

        assertEquals(
            listOf("Search", "FavoriteBorder", "AddCircleOutline", "ChatBubbleOutline", "PersonOutline"),
            icons.map { it.name },
        )
        assertEquals(icons.size, icons.distinctBy { it.name }.size)
        icons.forEach { icon ->
            assertEquals(24.dp, icon.defaultWidth)
            assertEquals(24.dp, icon.defaultHeight)
        }
    }
}

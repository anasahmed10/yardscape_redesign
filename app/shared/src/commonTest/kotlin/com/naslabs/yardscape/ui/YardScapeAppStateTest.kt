package com.naslabs.yardscape.ui

import com.naslabs.yardscape.data.SeededYardSaleData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class YardScapeAppStateTest {
    @Test
    fun browseIsInitialRouteAndShowsSeededEvents() {
        val state = YardScapeAppState()

        assertIs<YardScapeRoute.Browse>(state.route)
        assertEquals(
            listOf(
                SeededYardSaleData.FAMILY_GARAGE_EVENT_ID,
                SeededYardSaleData.ESTATE_TOOLS_EVENT_ID,
            ),
            state.browseItems().map { it.id },
        )
    }

    @Test
    fun selectingBrowseEventNavigatesToPublicDetail() {
        val state = YardScapeAppState()

        state.openEvent(SeededYardSaleData.FAMILY_GARAGE_EVENT_ID)

        assertEquals(
            YardScapeRoute.EventDetail(SeededYardSaleData.FAMILY_GARAGE_EVENT_ID),
            state.route,
        )
        assertNotNull(state.selectedPublicDetail())
    }

    @Test
    fun browseDisplayItemsDoNotContainExactAddresses() {
        val itemText = YardScapeAppState().browseItems().joinToString()

        assertFalse(itemText.contains("123 Cedar Street"))
        assertFalse(itemText.contains("418 Juniper Avenue"))
        assertFalse(itemText.contains("47.6101"))
        assertFalse(itemText.contains("-122.2142"))
    }
}

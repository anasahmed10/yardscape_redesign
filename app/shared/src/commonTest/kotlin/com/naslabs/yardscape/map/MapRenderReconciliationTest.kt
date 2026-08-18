package com.naslabs.yardscape.map

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MapRenderReconciliationTest {
    @Test
    fun reconciliationRetainsUnchangedPublicMarkerHandlesAndReportsSelectionChanges() {
        val previous = mapOf(
            "maple-1" to "maple marker",
            "old-mill-1" to "old mill marker",
        )
        val current = mapOf(
            "maple-1" to "maple marker",
            "cedar-1" to "cedar marker",
        )

        val result = reconcile(
            previous = previous,
            current = current,
            previousSelectedId = "maple-1",
            selectedId = "cedar-1",
        )

        assertEquals(listOf("cedar marker"), result.added)
        assertTrue(result.retainedIds.contains("maple-1"))
        assertEquals(setOf("old-mill-1"), result.removedIds)
        assertEquals(setOf("maple-1", "cedar-1"), result.selectionChangedIds)
    }
}

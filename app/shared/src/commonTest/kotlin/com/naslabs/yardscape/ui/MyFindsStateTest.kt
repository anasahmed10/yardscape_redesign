package com.naslabs.yardscape.ui

import com.naslabs.yardscape.data.SeededYardSaleData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MyFindsStateTest {
    @Test
    fun selectedSavedSectionDerivesSavedItemsWithoutRsvpItems() {
        val state = YardScapeAppState()
        val savedEventId = SeededYardSaleData.FAMILY_GARAGE_EVENT_ID
        state.toggleSavedEvent(savedEventId)

        val myFinds = state.myFindsState(MyFindsSection.Saved)

        assertEquals(MyFindsSection.Saved, myFinds.section)
        assertEquals(listOf(savedEventId), myFinds.savedItems.map { it.id })
        assertFalse(myFinds.isEmpty)
    }

    @Test
    fun selectedRsvpsSectionDerivesRsvpItems() {
        val state = YardScapeAppState()

        val myFinds = state.myFindsState(MyFindsSection.Rsvps)

        assertEquals(MyFindsSection.Rsvps, myFinds.section)
        assertTrue(myFinds.rsvpItems.isNotEmpty())
        assertFalse(myFinds.isEmpty)
    }

    @Test
    fun selectedSectionPresentationKeepsOnlyItsOwnEmptyState() {
        val state = YardScapeAppState()
        val saved = state.myFindsState(MyFindsSection.Saved).workspacePresentation()
        val rsvps = state.myFindsState(MyFindsSection.Rsvps).workspacePresentation()

        assertEquals(MyFindsSection.Saved, saved.selectedSection)
        assertEquals("Nothing saved yet", saved.emptyState?.title)
        assertEquals(MyFindsSection.Rsvps, rsvps.selectedSection)
        assertFalse(rsvps.rsvpGroups.isEmpty())
        assertEquals(null, rsvps.emptyState)
    }
}

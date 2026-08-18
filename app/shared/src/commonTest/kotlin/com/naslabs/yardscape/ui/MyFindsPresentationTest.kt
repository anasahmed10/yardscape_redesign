package com.naslabs.yardscape.ui

import com.naslabs.yardscape.domain.ExactAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MyFindsPresentationTest {
    @Test
    fun savedEmptyStateOffersBrowseExitAndMarksSavedSegmentSelected() {
        val presentation = MyFindsState(
            section = MyFindsSection.Saved,
            savedItems = emptyList(),
            rsvpItems = emptyList(),
        ).workspacePresentation()

        assertEquals(MyFindsSection.Saved, presentation.selectedSection)
        assertEquals("Saved", presentation.segments.single { it.section == MyFindsSection.Saved }.label)
        assertTrue(presentation.segments.single { it.section == MyFindsSection.Saved }.isSelected)
        assertEquals("Browse sales", presentation.emptyState?.actionLabel)
    }

    @Test
    fun rsvpEmptyStateOffersBrowseExitAndMarksRsvpSegmentSelected() {
        val presentation = MyFindsState(
            section = MyFindsSection.Rsvps,
            savedItems = emptyList(),
            rsvpItems = emptyList(),
        ).workspacePresentation()

        assertEquals(MyFindsSection.Rsvps, presentation.selectedSection)
        assertTrue(presentation.segments.single { it.section == MyFindsSection.Rsvps }.isSelected)
        assertEquals("Browse sales", presentation.emptyState?.actionLabel)
    }

    @Test
    fun rsvpPresentationGroupsItemsByTheirTruthfulState() {
        val requested = rsvpItem(ShopperRsvpUiState.Requested, RsvpGroup.ActionNeeded)
        val accepted = rsvpItem(ShopperRsvpUiState.Accepted, RsvpGroup.Upcoming, exactAddress())
        val expired = rsvpItem(ShopperRsvpUiState.Expired, RsvpGroup.History)

        val presentation = MyFindsState(
            section = MyFindsSection.Rsvps,
            savedItems = emptyList(),
            rsvpItems = listOf(expired, accepted, requested),
        ).workspacePresentation()

        assertEquals(
            listOf(RsvpGroup.ActionNeeded, RsvpGroup.Upcoming, RsvpGroup.History),
            presentation.rsvpGroups.map { it.group },
        )
        assertEquals(listOf(requested.eventId), presentation.rsvpGroups[0].items.map { it.eventId })
        assertEquals(listOf(accepted.eventId), presentation.rsvpGroups[1].items.map { it.eventId })
        assertEquals(listOf(expired.eventId), presentation.rsvpGroups[2].items.map { it.eventId })
    }

    @Test
    fun onlyActiveAcceptedRsvpExposesProtectedDirectionsAction() {
        val accepted = rsvpItem(
            state = ShopperRsvpUiState.Accepted,
            group = RsvpGroup.Upcoming,
            exactAddress = exactAddress(),
        )
        val unavailableStates = listOf(
            ShopperRsvpUiState.Full,
            ShopperRsvpUiState.Waitlisted,
            ShopperRsvpUiState.Declined,
            ShopperRsvpUiState.Cancelled,
            ShopperRsvpUiState.Revoked,
            ShopperRsvpUiState.Expired,
        )

        assertTrue(ShopperRsvpAction.Directions in accepted.visibleActions)
        unavailableStates.forEach { state ->
            val item = rsvpItem(state, RsvpGroup.History)
            assertFalse(ShopperRsvpAction.Directions in item.visibleActions, "$state must not expose directions")
        }
    }

    @Test
    fun cancelledRsvpOnlyKeepsThePublicEventExit() {
        val cancelled = rsvpItem(ShopperRsvpUiState.Cancelled, RsvpGroup.History)

        assertEquals(listOf(ShopperRsvpAction.OpenEvent), cancelled.visibleActions)
    }

    @Test
    fun unavailableAndWaitlistedRsvpsKeepOnlyTheirTruthfulActions() {
        val publicExitOnly = listOf(
            ShopperRsvpUiState.Full,
            ShopperRsvpUiState.Declined,
            ShopperRsvpUiState.Cancelled,
            ShopperRsvpUiState.Revoked,
            ShopperRsvpUiState.Expired,
        )

        publicExitOnly.forEach { state ->
            assertEquals(
                listOf(ShopperRsvpAction.OpenEvent),
                rsvpItem(state, RsvpGroup.History).visibleActions,
                "$state should retain only the public event exit",
            )
        }
        assertEquals(
            listOf(
                ShopperRsvpAction.OpenEvent,
                ShopperRsvpAction.AddReminder,
                ShopperRsvpAction.ExportCalendar,
                ShopperRsvpAction.CancelRsvp,
            ),
            rsvpItem(ShopperRsvpUiState.Waitlisted, RsvpGroup.ActionNeeded).visibleActions,
        )
    }

    @Test
    fun workspaceUsesCompactAndExpandedDensityAtTargetWidths() {
        assertEquals(MyFindsWorkspaceLayout.Compact, myFindsWorkspaceLayoutFor(390))
        assertEquals(MyFindsWorkspaceLayout.Expanded, myFindsWorkspaceLayoutFor(1440))
    }

    private fun rsvpItem(
        state: ShopperRsvpUiState,
        group: RsvpGroup,
        exactAddress: ExactAddress? = null,
    ) = ShopperRsvpItem(
        eventId = state.name,
        title = "Neighborhood sale",
        dateLabel = "Saturday",
        approximateLocationLabel = "Maple Ridge",
        state = state,
        group = group,
        exactAddress = exactAddress,
        reminderAdded = false,
        calendarExportPrepared = false,
    )

    private fun exactAddress() = ExactAddress(
        streetAddress = "12 Private Lane",
        city = "Portland",
        region = "OR",
        postalCode = "97205",
        latitude = 45.52,
        longitude = -122.68,
    )
}

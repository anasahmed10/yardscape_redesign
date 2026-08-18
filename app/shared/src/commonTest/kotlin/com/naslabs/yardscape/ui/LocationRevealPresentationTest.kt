package com.naslabs.yardscape.ui

import androidx.compose.ui.unit.dp
import com.naslabs.yardscape.domain.ExactAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocationRevealPresentationTest {
    private val protectedAddress = ExactAddress(
        streetAddress = "915 Privacy Lane",
        unit = "Gate B",
        city = "Bellevue",
        region = "WA",
        postalCode = "98004",
        latitude = 47.61,
        longitude = -122.20,
        accessInstructions = "Use the side entrance",
    )

    @Test
    fun activeAcceptedAccessPresentsExactAddressAndDirections() {
        val presentation = LocationRevealState.Revealed(protectedAddress)
            .toLocationRevealPresentation()

        assertEquals(
            "915 Privacy Lane\nGate B\nBellevue, WA 98004\nUse the side entrance",
            presentation.exactAddressLabel,
        )
        assertEquals("Directions", presentation.actionLabel)
        assertTrue(presentation.hasActiveAccess)
    }

    @Test
    fun unauthorizedRevealStatesNeverPresentExactAddressOrDirections() {
        val unauthorizedStates = listOf(
            LocationRevealState.NotRequested,
            LocationRevealState.Pending,
            LocationRevealState.Revoked,
            LocationRevealState.Expired,
            LocationRevealState.Cancelled,
            LocationRevealState.Blocked,
        )

        unauthorizedStates.forEach { revealState ->
            val presentation = revealState.toLocationRevealPresentation()

            assertNull(presentation.exactAddressLabel, "$revealState exposed protected location")
            assertNull(presentation.actionLabel, "$revealState exposed directions")
            assertFalse(presentation.hasActiveAccess, "$revealState was treated as active access")
            assertFalse(presentation.toString().contains(protectedAddress.streetAddress))
            assertFalse(presentation.toString().contains(protectedAddress.accessInstructions.orEmpty()))
        }
    }

    @Test
    fun shopperWorkflowContentStaysMobileWidthAndCapsExpandedReadingWidth() {
        assertEquals(390.dp, shopperWorkflowContentMaxWidthFor(390.dp))
        assertEquals(840.dp, shopperWorkflowContentMaxWidthFor(1_440.dp))
    }
}

package com.naslabs.yardscape.ui

import com.naslabs.yardscape.data.SeededMarketplaceMessagingRepository
import com.naslabs.yardscape.data.SeededMessagingConversation
import com.naslabs.yardscape.data.SeededMessagingMessage
import com.naslabs.yardscape.data.SeededYardSaleData
import com.naslabs.yardscape.domain.CLOSED_MARKETPLACE_MESSAGE_BODY
import com.naslabs.yardscape.domain.MessageDeliveryState
import com.naslabs.yardscape.domain.MessagingComposerAccess
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MarketplacePrivacyRegressionTest {
    @Test
    fun acceptedAccessLossClearsEveryProtectedProjectionTogether() = runTest {
        privacyTransitions.forEach { transition ->
            val fixture = acceptedFixture()

            transition.mutate(fixture.state)

            assertProtectedProjectionsCleared(fixture, transition.name)
            assertPublicDiscoveryRemainsUsableAndApproximate(fixture.state, transition)
            assertSelectionMatchesDiscoveryPolicy(fixture.state, transition)
        }
    }

    private suspend fun acceptedFixture(): AcceptedFixture {
        val opaqueTokens = ArrayDeque(
            listOf(
                "0000000000000000000000000000002a",
                "0000000000000000000000000000002b",
            ),
        )
        val state = YardScapeAppState(
            shopperId = SHOPPER_ID,
            messagingRepositoryFactory = { accessSource ->
                SeededMarketplaceMessagingRepository(
                    accessSource = accessSource,
                    initialConversations = listOf(
                        SeededMessagingConversation(
                            conversationKey = CONVERSATION_KEY,
                            messages = listOf(
                                SeededMessagingMessage(
                                    senderId = SeededYardSaleData.HOST_AVERY_ID,
                                    body = PRIVATE_MESSAGE,
                                    sentAtEpochMillis = SeededYardSaleData.BASE_NOW_EPOCH_MILLIS,
                                    deliveryState = MessageDeliveryState.SENT,
                                ),
                            ),
                        ),
                    ),
                    opaqueIdTokenSource = { opaqueTokens.removeFirst() },
                )
            },
        )
        assertIs<LocationRevealState.Revealed>(state.detailStateFor(EVENT_ID)?.revealState)
        assertNotNull(state.requestDirections(EVENT_ID))
        assertTrue(state.selectDiscoveryEvent(EVENT_ID))
        assertTrue(state.loadMessagingInbox())
        val conversationId = assertIs<MessagingInboxUiState.Loaded>(state.messagingInboxState)
            .threads.single().conversationId
        assertTrue(state.openMessageThread(conversationId))
        state.updateMessageDraft(PRIVATE_DRAFT)
        val opened = assertIs<MessagingThreadUiState.Loaded>(state.messagingThreadState).presentation
        assertTrue(opened.canCompose)
        assertEquals(PRIVATE_MESSAGE, opened.messages.single().body)
        assertEquals(PRIVATE_DRAFT, opened.draft)

        return AcceptedFixture(state)
    }

    private fun assertProtectedProjectionsCleared(fixture: AcceptedFixture, transitionName: String) {
        val state = fixture.state
        assertFalse(
            state.detailStateFor(EVENT_ID)?.revealState is LocationRevealState.Revealed,
            "$transitionName left exact location in event detail",
        )
        assertNull(
            state.myRsvpItems().firstOrNull { it.eventId == EVENT_ID }?.exactAddress,
            "$transitionName left exact location in My Finds",
        )
        assertNull(state.directionsEventId, "$transitionName left protected directions selected")

        val closed = assertIs<MessagingThreadUiState.Loaded>(
            state.messagingThreadState,
            "$transitionName discarded the closed-thread state instead of sanitizing it",
        ).presentation
        assertFalse(closed.canCompose, "$transitionName left the composer open")
        assertEquals("", closed.draft, "$transitionName left a private draft in memory")
        assertEquals(
            CLOSED_MARKETPLACE_MESSAGE_BODY,
            closed.messages.single().body,
            "$transitionName left private message content projected",
        )
        assertIs<MessagingComposerAccess.Closed>(closed.composerAccess, transitionName)
    }

    private fun assertPublicDiscoveryRemainsUsableAndApproximate(
        state: YardScapeAppState,
        transition: PrivacyTransition,
    ) {
        val publicItems = state.browseItems()
        val publicMarkers = state.mapDiscoveryState.markers
        assertTrue(publicItems.isNotEmpty(), "${transition.name} removed every usable Browse result")
        assertTrue(publicMarkers.isNotEmpty(), "${transition.name} removed every usable map result")
        publicItems.forEach { publicItem ->
            assertTrue(publicItem.title.isNotBlank(), transition.name)
            assertTrue(publicItem.locationLabel.isNotBlank(), transition.name)
            assertTrue(publicItem.neighborhood.isNotBlank(), transition.name)
            assertContainsNoExactLocation(publicItem.toString(), transition.name)
        }
        publicMarkers.forEach { publicMarker ->
            assertTrue(publicMarker.title.isNotBlank(), transition.name)
            assertTrue(publicMarker.area.displayLabel.isNotBlank(), transition.name)
            assertTrue(publicMarker.area.approximationRadiusMeters >= 500, transition.name)
            assertContainsNoExactLocation(publicMarker.toString(), transition.name)
        }

        if (transition.keepsAffectedEventDiscoverable) {
            val retainedItem = publicItems.single { it.id == EVENT_ID }
            val retainedMarker = publicMarkers.single { it.eventId == EVENT_ID }
            assertTrue(retainedItem.locationLabel.contains("Maple Ridge"), transition.name)
            assertTrue(retainedMarker.area.displayLabel.contains("Maple Ridge"), transition.name)
        } else {
            assertFalse(publicItems.any { it.id == EVENT_ID }, transition.name)
            assertFalse(publicMarkers.any { it.eventId == EVENT_ID }, transition.name)
            assertTrue(publicItems.any { it.id == OTHER_PUBLIC_EVENT_ID }, transition.name)
            assertTrue(publicMarkers.any { it.eventId == OTHER_PUBLIC_EVENT_ID }, transition.name)
        }
    }

    private fun assertSelectionMatchesDiscoveryPolicy(
        state: YardScapeAppState,
        transition: PrivacyTransition,
    ) {
        if (transition.keepsAffectedEventDiscoverable) {
            assertEquals(EVENT_ID, state.mapDiscoveryState.selectedEventId, transition.name)
            val selectedMarker = assertNotNull(state.mapDiscoveryState.selectedMarker, transition.name)
            assertEquals(EVENT_ID, selectedMarker.eventId, transition.name)
            assertTrue(selectedMarker.area.displayLabel.contains("Maple Ridge"), transition.name)
            assertTrue(selectedMarker.area.approximationRadiusMeters >= 500, transition.name)
            assertContainsNoExactLocation(selectedMarker.toString(), transition.name)
        } else {
            assertNull(state.mapDiscoveryState.selectedEventId, transition.name)
            assertNull(state.mapDiscoveryState.selectedMarker, transition.name)
        }
    }

    private fun assertContainsNoExactLocation(publicProjection: String, transitionName: String) {
        EXACT_LOCATION_TOKENS.forEach { protectedToken ->
            assertFalse(
                publicProjection.contains(protectedToken),
                "$transitionName leaked $protectedToken through public discovery",
            )
        }
    }

    private data class AcceptedFixture(val state: YardScapeAppState)

    private data class PrivacyTransition(
        val name: String,
        val keepsAffectedEventDiscoverable: Boolean = true,
        val mutate: (YardScapeAppState) -> Unit,
    )

    private companion object {
        const val EVENT_ID = SeededYardSaleData.FAMILY_GARAGE_EVENT_ID
        const val OTHER_PUBLIC_EVENT_ID = SeededYardSaleData.ESTATE_TOOLS_EVENT_ID
        const val SHOPPER_ID = SeededAttendeeIds.Accepted
        const val PRIVATE_MESSAGE = "Private pickup question before access changes"
        const val PRIVATE_DRAFT = "Private draft before access changes"

        val CONVERSATION_KEY = com.naslabs.yardscape.domain.MarketplaceConversationKey(EVENT_ID, SHOPPER_ID)
        val EXACT_LOCATION_TOKENS = listOf(
            "123 Cedar Street",
            "side gate",
            "47.6101",
            "-122.2015",
            "418 Juniper Avenue",
            "driveway tent",
            "47.6208",
            "-122.2142",
        )
        val privacyTransitions = listOf(
            PrivacyTransition("host revoke") { state ->
                assertTrue(state.revokeRsvpAccess(EVENT_ID))
            },
            PrivacyTransition("shopper RSVP cancellation") { state ->
                assertTrue(state.requestRsvpCancellation(EVENT_ID))
                assertTrue(state.confirmRsvpCancellation())
            },
            PrivacyTransition("access expiry") { state ->
                assertTrue(state.expireRsvpAccess(EVENT_ID))
            },
            PrivacyTransition("event cancellation", keepsAffectedEventDiscoverable = false) { state ->
                state.cancelHostEvent(EVENT_ID)
            },
            PrivacyTransition("host block", keepsAffectedEventDiscoverable = false) { state ->
                assertTrue(state.blockHostForEvent(EVENT_ID))
            },
            PrivacyTransition("sign-out") { state ->
                state.signOutMock()
            },
        )
    }
}

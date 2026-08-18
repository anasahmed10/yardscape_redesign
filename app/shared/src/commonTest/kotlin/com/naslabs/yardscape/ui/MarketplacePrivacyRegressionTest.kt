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
            assertPublicDiscoveryRemainsApproximate(fixture.state, transition.name)
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

    private fun assertPublicDiscoveryRemainsApproximate(state: YardScapeAppState, transitionName: String) {
        val publicProjection = buildString {
            append(state.browseItems())
            append(state.mapDiscoveryState.markers)
        }
        EXACT_LOCATION_TOKENS.forEach { protectedToken ->
            assertFalse(
                publicProjection.contains(protectedToken),
                "$transitionName leaked $protectedToken through public discovery",
            )
        }
        state.browseItems().firstOrNull { it.id == EVENT_ID }?.let { publicItem ->
            assertTrue(publicItem.locationLabel.contains("Maple Ridge"), transitionName)
            assertFalse(publicItem.locationLabel.contains("Street"), transitionName)
        }
    }

    private data class AcceptedFixture(val state: YardScapeAppState)

    private data class PrivacyTransition(
        val name: String,
        val mutate: (YardScapeAppState) -> Unit,
    )

    private companion object {
        const val EVENT_ID = SeededYardSaleData.FAMILY_GARAGE_EVENT_ID
        const val SHOPPER_ID = SeededAttendeeIds.Accepted
        const val PRIVATE_MESSAGE = "Private pickup question before access changes"
        const val PRIVATE_DRAFT = "Private draft before access changes"

        val CONVERSATION_KEY = com.naslabs.yardscape.domain.MarketplaceConversationKey(EVENT_ID, SHOPPER_ID)
        val EXACT_LOCATION_TOKENS = listOf(
            "123 Cedar Street",
            "side gate",
            "47.6101",
            "-122.2015",
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
            PrivacyTransition("event cancellation") { state ->
                state.cancelHostEvent(EVENT_ID)
            },
            PrivacyTransition("host block") { state ->
                assertTrue(state.blockHostForEvent(EVENT_ID))
            },
            PrivacyTransition("sign-out") { state ->
                state.signOutMock()
            },
        )
    }
}

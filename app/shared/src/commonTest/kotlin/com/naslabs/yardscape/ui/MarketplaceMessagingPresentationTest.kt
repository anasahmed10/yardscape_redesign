package com.naslabs.yardscape.ui

import androidx.compose.ui.unit.dp
import com.naslabs.yardscape.domain.EventPhoto
import com.naslabs.yardscape.domain.MarketplaceConversationKey
import com.naslabs.yardscape.domain.MarketplaceMessage
import com.naslabs.yardscape.domain.MarketplaceMessageThread
import com.naslabs.yardscape.domain.MessageDeliveryState
import com.naslabs.yardscape.domain.MessageThreadSummary
import com.naslabs.yardscape.domain.MessagingClosedReason
import com.naslabs.yardscape.domain.MessagingComposerAccess
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MarketplaceMessagingPresentationTest {
    @Test
    fun inboxRowsUsePublicArtworkUnreadSemanticsAndA48DpTarget() {
        val row = marketplaceInboxRowPresentation(
            MessageThreadSummary(
                conversationId = "conversation-0000002a",
                conversationKey = KEY,
                eventTitle = "Maple Ridge Family Garage Sale",
                eventPhoto = EventPhoto("seed://maple-ridge-driveway"),
                lastMessagePreview = "New message",
                lastMessageAtEpochMillis = 2_000L,
                unreadCount = 2,
                composerAccess = MessagingComposerAccess.Open,
            ),
        )

        assertEquals(ShopperArtworkResource.GarageSale, row.artwork.artwork.resource)
        assertEquals("2 unread messages", row.unreadLabel)
        assertEquals(
            "Open messages for Maple Ridge Family Garage Sale, 2 unread messages",
            row.contentDescription,
        )
        assertEquals(48.dp, row.minimumHeight)
        assertTrue(row.isUnread)
        assertTrue(row.isPhotoFirst)
        assertEquals("Event conversation", row.contextLabel)
    }

    @Test
    fun inboxPresentationDistinguishesEmptyLoadingAndRecoverableFailures() {
        assertIs<MarketplaceInboxPresentation.Loading>(
            marketplaceInboxPresentation(MessagingInboxUiState.Loading),
        )
        val empty = assertIs<MarketplaceInboxPresentation.Empty>(
            marketplaceInboxPresentation(MessagingInboxUiState.Loaded(emptyList())),
        )
        val offline = assertIs<MarketplaceInboxPresentation.Error>(
            marketplaceInboxPresentation(
                MessagingInboxUiState.Failed(MessagingFailureKind.Offline, "No connection"),
            ),
        )

        assertEquals("You're offline", offline.title)
        assertEquals("Try again", offline.actionLabel)
        assertEquals("Browse sales", empty.actionLabel)
    }

    @Test
    fun timelineOrdersMessagesAndExposesRetryOnlyForFailedOutboundMessages() {
        val presentation = marketplaceThreadPresentation(
            MessagingThreadPresentation(
                thread = thread(
                    messages = listOf(
                        message("message-0000002b", HOST_ID, "Host reply", 3_000L),
                        message("message-0000002a", SHOPPER_ID, "Try again", 2_000L, MessageDeliveryState.FAILED),
                    ),
                ),
            ),
            currentActorId = SHOPPER_ID,
        )

        assertEquals(listOf("message-0000002a", "message-0000002b"), presentation.messages.map { it.id })
        assertTrue(presentation.messages.first().showsRetry)
        assertTrue(presentation.messages.first().isRetryEnabled)
        assertEquals("Delivery failed. Retry", presentation.messages.first().deliveryLabel)
        assertFalse(presentation.messages.last().showsRetry)
        assertTrue(presentation.composer.isVisible)
        assertEquals(48.dp, presentation.composer.minimumHeight)
    }

    @Test
    fun closedThreadShowsItsReasonAndHidesComposerWhileSafetyActionsRemainAvailable() {
        val presentation = marketplaceThreadPresentation(
            MessagingThreadPresentation(
                thread = thread(
                    messages = listOf(
                        message(
                            "message-0000002a",
                            HOST_ID,
                            "Meet at 123 Cedar Street, unit 7. Use the side gate.",
                            2_000L,
                        ),
                    ),
                    composerAccess = MessagingComposerAccess.Closed(MessagingClosedReason.EVENT_CANCELLED),
                ),
                draft = "private draft",
            ),
            currentActorId = SHOPPER_ID,
        )

        assertFalse(presentation.composer.isVisible)
        assertEquals("This sale was cancelled", presentation.closedBanner?.title)
        assertTrue(presentation.showReportAction)
        assertTrue(presentation.showBlockAction)
        assertEquals(48.dp, presentation.eventAction.minimumHeight)
        assertEquals(
            "Message content hidden because this conversation is closed.",
            presentation.messages.single().body,
        )
        assertFalse(presentation.messages.single().body.contains("123 Cedar Street"))
        assertFalse(presentation.messages.single().body.contains("side gate"))
    }

    @Test
    fun threadPresentationDoesNotLeakProtectedFieldsThroughDiagnostics() {
        val presentation = marketplaceThreadPresentation(
            MessagingThreadPresentation(
                thread = thread(
                    messages = listOf(
                        message(
                            "message-0000002a",
                            SHOPPER_ID,
                            "Meet at 123 Cedar Street, unit 7. Gate code 1010.",
                            2_000L,
                        ),
                    ),
                ),
            ),
            currentActorId = SHOPPER_ID,
        )

        val diagnostic = presentation.toString()
        assertFalse(diagnostic.contains("123 Cedar Street"))
        assertFalse(diagnostic.contains("unit 7"))
        assertFalse(diagnostic.contains("Gate code"))
        assertNull(presentation.closedBanner)
    }

    @Test
    fun messagingLayoutUsesTheSharedCompactAndExpandedBreakpoint() {
        assertEquals(MarketplaceMessagingLayout.Compact, marketplaceMessagingLayoutFor(390.dp))
        assertEquals(MarketplaceMessagingLayout.Expanded, marketplaceMessagingLayoutFor(1440.dp))
        assertEquals(390.dp, marketplaceMessagingContentWidthFor(390.dp, 1_080.dp))
        assertEquals(1_080.dp, marketplaceMessagingContentWidthFor(1_440.dp, 1_080.dp))
        assertEquals(960.dp, marketplaceMessagingContentWidthFor(1_440.dp, 960.dp))
    }

    @Test
    fun threadActionsKeepOnePrimarySaleActionAndSeparateSafetyActions() {
        val presentation = marketplaceThreadPresentation(
            MessagingThreadPresentation(thread = thread()),
            currentActorId = SHOPPER_ID,
        )

        assertEquals(MarketplaceThreadActionTone.Primary, presentation.eventAction.tone)
        assertEquals(MarketplaceThreadActionTone.Neutral, presentation.reportActionTone)
        assertEquals(MarketplaceThreadActionTone.Destructive, presentation.blockActionTone)
        assertEquals(48.dp, presentation.safetyActionMinimumHeight)
    }

    @Test
    fun retryIsHiddenWhileAMessageOperationIsBusyOrConversationIsClosed() {
        val failedMessage = message("message-0000002a", SHOPPER_ID, "Try again", 2_000L, MessageDeliveryState.FAILED)
        val busy = marketplaceThreadPresentation(
            MessagingThreadPresentation(thread = thread(messages = listOf(failedMessage)), operation = MessagingOperationState.InProgress()),
            currentActorId = SHOPPER_ID,
        )
        val closed = marketplaceThreadPresentation(
            MessagingThreadPresentation(
                thread = thread(
                    messages = listOf(failedMessage),
                    composerAccess = MessagingComposerAccess.Closed(MessagingClosedReason.EVENT_CANCELLED),
                ),
            ),
            currentActorId = SHOPPER_ID,
        )

        assertFalse(busy.messages.single().showsRetry)
        assertFalse(busy.messages.single().isRetryEnabled)
        assertFalse(closed.messages.single().showsRetry)
        assertFalse(closed.messages.single().isRetryEnabled)
    }

    @Test
    fun deliveryStatusCopyAndToneReflectTheVisibleMessageActions() {
        val sent = marketplaceThreadPresentation(
            MessagingThreadPresentation(
                thread = thread(messages = listOf(message("message-0000002a", SHOPPER_ID, "On my way", 2_000L))),
            ),
            currentActorId = SHOPPER_ID,
        ).messages.single()
        val failedWhileBusy = marketplaceThreadPresentation(
            MessagingThreadPresentation(
                thread = thread(
                    messages = listOf(message("message-0000002b", SHOPPER_ID, "Try again", 2_000L, MessageDeliveryState.FAILED)),
                ),
                operation = MessagingOperationState.InProgress(),
            ),
            currentActorId = SHOPPER_ID,
        ).messages.single()
        val failedWhenClosed = marketplaceThreadPresentation(
            MessagingThreadPresentation(
                thread = thread(
                    messages = listOf(message("message-0000002c", SHOPPER_ID, "Try again", 2_000L, MessageDeliveryState.FAILED)),
                    composerAccess = MessagingComposerAccess.Closed(MessagingClosedReason.EVENT_CANCELLED),
                ),
            ),
            currentActorId = SHOPPER_ID,
        ).messages.single()

        assertEquals("Sent", sent.deliveryLabel)
        assertEquals(MarketplaceMessageDeliveryTone.Normal, sent.deliveryTone)
        assertEquals("Delivery failed", failedWhileBusy.deliveryLabel)
        assertEquals(MarketplaceMessageDeliveryTone.Error, failedWhileBusy.deliveryTone)
        assertEquals("Delivery failed. Messaging is closed.", failedWhenClosed.deliveryLabel)
        assertEquals(MarketplaceMessageDeliveryTone.Error, failedWhenClosed.deliveryTone)
    }

    private fun thread(
        messages: List<MarketplaceMessage> = emptyList(),
        composerAccess: MessagingComposerAccess = MessagingComposerAccess.Open,
    ) = MarketplaceMessageThread(
        conversationId = "conversation-0000002a",
        conversationKey = KEY,
        eventTitle = "Maple Ridge Family Garage Sale",
        eventPhoto = EventPhoto("seed://maple-ridge-driveway"),
        messages = messages,
        composerAccess = composerAccess,
    )

    private fun message(
        id: String,
        senderId: String,
        body: String,
        sentAt: Long,
        deliveryState: MessageDeliveryState = MessageDeliveryState.SENT,
    ) = MarketplaceMessage(
        id = id,
        conversationId = "conversation-0000002a",
        senderId = senderId,
        body = body,
        sentAtEpochMillis = sentAt,
        deliveryState = deliveryState,
    )

    private companion object {
        const val SHOPPER_ID = "shopper-1"
        const val HOST_ID = "host-1"
        val KEY = MarketplaceConversationKey(eventId = "event-1", shopperId = SHOPPER_ID)
    }
}

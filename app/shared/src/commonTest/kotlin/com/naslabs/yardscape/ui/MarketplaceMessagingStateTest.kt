package com.naslabs.yardscape.ui

import com.naslabs.yardscape.data.MarketplaceMessagingAccessSource
import com.naslabs.yardscape.data.MarketplaceMessagingRepository
import com.naslabs.yardscape.data.MessagingRepositoryResult
import com.naslabs.yardscape.data.SeededMarketplaceMessagingRepository
import com.naslabs.yardscape.data.SeededMessageOutcome
import com.naslabs.yardscape.data.SeededMessagingBehavior
import com.naslabs.yardscape.data.SeededYardSaleData
import com.naslabs.yardscape.domain.EventStatus
import com.naslabs.yardscape.domain.LocationVisibility
import com.naslabs.yardscape.domain.MarketplaceConversationKey
import com.naslabs.yardscape.domain.MarketplaceMessage
import com.naslabs.yardscape.domain.MarketplaceMessageThread
import com.naslabs.yardscape.domain.MarketplaceMessagingPolicy
import com.naslabs.yardscape.domain.MessageDeliveryState
import com.naslabs.yardscape.domain.MessageThreadSummary
import com.naslabs.yardscape.domain.MessagingAccessContext
import com.naslabs.yardscape.domain.MessagingActor
import com.naslabs.yardscape.domain.MessagingClosedReason
import com.naslabs.yardscape.domain.MessagingComposerAccess
import com.naslabs.yardscape.domain.RsvpStatus
import com.naslabs.yardscape.domain.UserRole
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MarketplaceMessagingStateTest {
    @Test
    fun loadInboxExposesLoadingAndTypedFailureState() = runTest {
        lateinit var state: MarketplaceMessagingState
        val repository = InboxFailureRepository {
            assertIs<MessagingInboxUiState.Loading>(state.inboxState)
        }
        state = MarketplaceMessagingState(repository, { SHOPPER }, ::composerAccess)

        state.loadInbox()

        val failed = assertIs<MessagingInboxUiState.Failed>(state.inboxState)
        assertEquals(MessagingFailureKind.Offline, failed.kind)
        assertTrue(failed.message.contains("offline", ignoreCase = true))
    }

    @Test
    fun inboxOpenAndMarkReadUseOpaqueIdentityAndUpdateUnreadState() = runTest {
        val repository = SeededMarketplaceMessagingRepository(MutableAccessSource())
        assertIs<MessagingRepositoryResult.Success<MarketplaceMessage>>(
            repository.sendMessage(KEY, HOST, "The bikes are ready.", NOW),
        )
        val state = MarketplaceMessagingState(repository, { SHOPPER }, ::composerAccess)

        state.loadInbox()
        val inbox = assertIs<MessagingInboxUiState.Loaded>(state.inboxState)
        val summary = inbox.threads.single()
        assertEquals(1, summary.unreadCount)
        assertFalse(summary.conversationId.contains(KEY.eventId))
        assertFalse(summary.conversationId.contains(KEY.shopperId))

        assertTrue(state.openThread(summary.conversationId))
        val opened = assertIs<MessagingThreadUiState.Loaded>(state.threadState)
        assertEquals(summary.conversationId, opened.presentation.conversationId)
        assertTrue(opened.presentation.canCompose)

        assertTrue(state.markCurrentThreadRead())
        assertEquals(
            0,
            assertIs<MessagingInboxUiState.Loaded>(state.inboxState).threads.single().unreadCount,
        )
    }

    @Test
    fun draftValidationFailedSendAndRetryAreObservable() = runTest {
        val repository = SeededMarketplaceMessagingRepository(
            accessSource = MutableAccessSource(),
            behavior = SeededMessagingBehavior(
                deliveryOutcomes = listOf(SeededMessageOutcome.Offline, SeededMessageOutcome.Success),
            ),
        )
        val conversationId = assertIs<MessagingRepositoryResult.Success<MarketplaceMessageThread>>(
            repository.threadFor(KEY, SHOPPER),
        ).value.conversationId
        val state = MarketplaceMessagingState(repository, { SHOPPER }, ::composerAccess)
        state.loadInbox()
        assertTrue(state.openThread(conversationId))

        state.updateDraft("   ")
        assertFalse(state.sendDraft(NOW))
        val validation = assertIs<MessagingOperationState.Failed>(
            assertIs<MessagingThreadUiState.Loaded>(state.threadState).presentation.operation,
        )
        assertEquals(MessagingFailureKind.Validation, validation.kind)
        assertEquals("   ", assertIs<MessagingThreadUiState.Loaded>(state.threadState).presentation.draft)

        state.updateDraft("  Can I bring a trailer?  ")
        assertFalse(state.sendDraft(NOW + 1))
        val failedSend = assertIs<MessagingThreadUiState.Loaded>(state.threadState).presentation
        assertEquals("", failedSend.draft)
        assertEquals(MessageDeliveryState.FAILED, failedSend.messages.single().deliveryState)
        assertEquals(
            MessagingFailureKind.Offline,
            assertIs<MessagingOperationState.Failed>(failedSend.operation).kind,
        )

        assertTrue(state.retryMessage(failedSend.messages.single().id, NOW + 2))
        val retried = assertIs<MessagingThreadUiState.Loaded>(state.threadState).presentation
        assertEquals(MessageDeliveryState.SENT, retried.messages.single().deliveryState)
        assertIs<MessagingOperationState.Completed>(retried.operation)
    }

    @Test
    fun closedPresentationNeverExposesAComposerOrPrivateDraftInDiagnostics() {
        val presentation = MessagingThreadPresentation(
            thread = MarketplaceMessageThread(
                conversationId = "conversation-0000002a",
                conversationKey = KEY,
                eventTitle = "Maple Ridge Family Garage Sale",
                eventPhoto = null,
                messages = listOf(
                    MarketplaceMessage(
                        id = "message-0000002a",
                        conversationId = "conversation-0000002a",
                        senderId = SHOPPER.userId,
                        body = "Meet at 123 Cedar Street by the side gate",
                        sentAtEpochMillis = NOW,
                        deliveryState = MessageDeliveryState.SENT,
                    ),
                ),
                composerAccess = MessagingComposerAccess.Closed(
                    MessagingClosedReason.LOCATION_ACCESS_REVOKED,
                ),
            ),
            draft = "Meet at 123 Cedar Street by the side gate",
        )

        assertFalse(presentation.canCompose)
        assertEquals(MessagingClosedReason.LOCATION_ACCESS_REVOKED, presentation.closedReason)
        val diagnosticText = buildString {
            append(presentation)
            append(MessagingThreadUiState.Loaded(presentation))
            append(
                MessagingInboxUiState.Loaded(
                    listOf(
                        MessageThreadSummary(
                            conversationId = presentation.conversationId,
                            conversationKey = KEY,
                            eventTitle = presentation.eventTitle,
                            eventPhoto = null,
                            lastMessagePreview = "New message",
                            lastMessageAtEpochMillis = NOW,
                            unreadCount = 1,
                            composerAccess = presentation.composerAccess,
                        ),
                    ),
                ),
            )
        }
        assertFalse(diagnosticText.contains("123 Cedar Street"))
        assertFalse(diagnosticText.contains("side gate"))
        assertFalse(diagnosticText.contains(KEY.eventId))
        assertFalse(diagnosticText.contains(KEY.shopperId))
    }

    @Test
    fun failureDiagnosticsDoNotEchoRepositoryMessages() {
        val privateFailure = "Server rejected directions to 123 Cedar Street by the side gate"
        val diagnosticText = listOf(
            MessagingOperationState.Failed(MessagingFailureKind.Server, privateFailure),
            MessagingInboxUiState.Failed(MessagingFailureKind.Server, privateFailure),
            MessagingThreadUiState.Failed(
                "conversation-0000002a",
                MessagingFailureKind.Server,
                privateFailure,
            ),
        ).joinToString()

        assertFalse(diagnosticText.contains("123 Cedar Street"))
        assertFalse(diagnosticText.contains("side gate"))
    }

    private class InboxFailureRepository(
        private val onInbox: () -> Unit,
    ) : MarketplaceMessagingRepository {
        override suspend fun inboxFor(actor: MessagingActor): MessagingRepositoryResult<List<MessageThreadSummary>> {
            onInbox()
            return MessagingRepositoryResult.Offline("Messaging is offline.")
        }

        override suspend fun threadFor(
            conversationKey: MarketplaceConversationKey,
            actor: MessagingActor,
        ): MessagingRepositoryResult<MarketplaceMessageThread> = error("Not used")

        override suspend fun sendMessage(
            conversationKey: MarketplaceConversationKey,
            actor: MessagingActor,
            body: String,
            sentAtEpochMillis: Long,
        ): MessagingRepositoryResult<MarketplaceMessage> = error("Not used")

        override suspend fun retryMessage(
            messageId: String,
            actor: MessagingActor,
            attemptedAtEpochMillis: Long,
        ): MessagingRepositoryResult<MarketplaceMessage> = error("Not used")

        override suspend fun markRead(
            conversationKey: MarketplaceConversationKey,
            actor: MessagingActor,
        ): MessagingRepositoryResult<Unit> = error("Not used")
    }

    private class MutableAccessSource : MarketplaceMessagingAccessSource {
        var context: MessagingAccessContext = openContext()

        override fun accessContextFor(key: MarketplaceConversationKey): MessagingAccessContext? =
            context.takeIf { it.conversationKey == key }
    }

    private companion object {
        val KEY = MarketplaceConversationKey(
            eventId = SeededYardSaleData.FAMILY_GARAGE_EVENT_ID,
            shopperId = SeededYardSaleData.SHOPPER_WITH_ACCEPTED_ACCESS_ID,
        )
        val SHOPPER = MessagingActor(KEY.shopperId, UserRole.SHOPPER)
        val HOST = MessagingActor(SeededYardSaleData.HOST_AVERY_ID, UserRole.HOST)
        const val NOW = SeededYardSaleData.BASE_NOW_EPOCH_MILLIS

        fun openContext(): MessagingAccessContext = MessagingAccessContext(
            conversationKey = KEY,
            hostId = HOST.userId,
            eventStatus = EventStatus.PUBLISHED,
            eventHasEnded = false,
            rsvpStatus = RsvpStatus.ACCEPTED,
            locationVisibility = LocationVisibility.RSVP_ACCEPTED,
            isBlocked = false,
        )

        fun composerAccess(
            key: MarketplaceConversationKey,
            actor: MessagingActor,
        ): MessagingComposerAccess = MarketplaceMessagingPolicy.composerAccess(
            openContext().copy(conversationKey = key),
            actor,
        )
    }
}

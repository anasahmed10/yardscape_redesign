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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
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
    fun accessTransitionImmediatelyHidesProtectedBodiesFromLoadedState() = runTest {
        var access: MessagingComposerAccess = MessagingComposerAccess.Open
        val protectedBody = "Meet at 123 Cedar Street by the side gate"
        val repository = ControllableRepository(
            thread = thread(
                messages = listOf(
                    MarketplaceMessage(
                        id = "message-0000002a",
                        conversationId = CONVERSATION_ID,
                        senderId = HOST.userId,
                        body = protectedBody,
                        sentAtEpochMillis = NOW,
                        deliveryState = MessageDeliveryState.SENT,
                    ),
                ),
            ),
        )
        val state = MarketplaceMessagingState(repository, { SHOPPER }, { _, _ -> access })
        state.loadInbox()
        assertTrue(state.openThread(CONVERSATION_ID))
        assertEquals(
            protectedBody,
            assertIs<MessagingThreadUiState.Loaded>(state.threadState).presentation.messages.single().body,
        )

        access = MessagingComposerAccess.Closed(MessagingClosedReason.LOCATION_ACCESS_REVOKED)
        state.synchronizeComposerAccess()

        val closed = assertIs<MessagingThreadUiState.Loaded>(state.threadState).presentation
        assertEquals(
            "Message content hidden because this conversation is closed.",
            closed.messages.single().body,
        )
        assertFalse(closed.thread.messages.single().body.contains("123 Cedar Street"))
        assertFalse(closed.thread.messages.single().body.contains("side gate"))
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
        assertFalse(diagnosticText.contains("conversation-0000002a"))
    }

    @Test
    fun repositoryClosedAccessIsNeverWidenedByAnOpenLocalProjection() = runTest {
        val repository = ControllableRepository(
            thread = thread(composerAccess = MessagingComposerAccess.Open),
            summaries = listOf(
                summary(
                    KEY,
                    CONVERSATION_ID,
                    MessagingComposerAccess.Closed(MessagingClosedReason.EVENT_CANCELLED),
                ),
            ),
        )
        val state = MarketplaceMessagingState(repository, { SHOPPER }, { _, _ -> MessagingComposerAccess.Open })

        assertTrue(state.loadInbox())
        assertTrue(state.openThread(CONVERSATION_ID))

        val presentation = assertIs<MessagingThreadUiState.Loaded>(state.threadState).presentation
        assertEquals(MessagingClosedReason.EVENT_CANCELLED, presentation.closedReason)
        assertFalse(presentation.canCompose)
    }

    @Test
    fun closeInvalidatesDelayedValidationAndNeverRestoresTheDraft() = runTest {
        val sendResult = CompletableDeferred<MessagingRepositoryResult<MarketplaceMessage>>()
        val repository = ControllableRepository(thread(), sendResult = sendResult)
        val state = MarketplaceMessagingState(repository, { SHOPPER }, ::composerAccess)
        state.loadInbox()
        state.openThread(CONVERSATION_ID)
        state.updateDraft("private draft")

        val send = async { state.sendDraft(NOW) }
        runCurrent()
        state.closeAndClear(MessagingClosedReason.LOCATION_ACCESS_REVOKED)
        sendResult.complete(MessagingRepositoryResult.ValidationFailure(listOf("Try again")))

        assertFalse(send.await())
        val presentation = assertIs<MessagingThreadUiState.Loaded>(state.threadState).presentation
        assertEquals("", presentation.draft)
        assertEquals(MessagingClosedReason.LOCATION_ACCESS_REVOKED, presentation.closedReason)
    }

    @Test
    fun laterOpenWinsWhenThreadLoadsCompleteOutOfOrder() = runTest {
        val first = CompletableDeferred<MessagingRepositoryResult<MarketplaceMessageThread>>()
        val second = CompletableDeferred<MessagingRepositoryResult<MarketplaceMessageThread>>()
        val secondKey = MarketplaceConversationKey("event-second", KEY.shopperId)
        val repository = ControllableRepository(
            thread(),
            summaries = listOf(summary(KEY, CONVERSATION_ID), summary(secondKey, SECOND_CONVERSATION_ID)),
            threadResults = ArrayDeque(listOf(first, second)),
        )
        val state = MarketplaceMessagingState(repository, { SHOPPER }, ::composerAccess)
        state.loadInbox()

        val olderOpen = async { state.openThread(CONVERSATION_ID) }
        runCurrent()
        val newerOpen = async { state.openThread(SECOND_CONVERSATION_ID) }
        runCurrent()
        second.complete(MessagingRepositoryResult.Success(thread(secondKey, SECOND_CONVERSATION_ID)))
        assertTrue(newerOpen.await())
        first.complete(MessagingRepositoryResult.Success(thread()))

        assertFalse(olderOpen.await())
        assertEquals(
            SECOND_CONVERSATION_ID,
            assertIs<MessagingThreadUiState.Loaded>(state.threadState).presentation.conversationId,
        )
    }

    @Test
    fun actorAndSessionChangeInvalidatesDelayedOldActorOpen() = runTest {
        var actor = SHOPPER
        var sessionVersion = 1L
        val delayed = CompletableDeferred<MessagingRepositoryResult<MarketplaceMessageThread>>()
        val repository = ControllableRepository(thread(), threadResults = ArrayDeque(listOf(delayed)))
        val state = MarketplaceMessagingState(repository, { actor }, ::composerAccess, { sessionVersion })
        state.loadInbox()

        val open = async { state.openThread(CONVERSATION_ID) }
        runCurrent()
        actor = HOST
        sessionVersion++
        state.closeAndClear(MessagingClosedReason.CONVERSATION_UNAVAILABLE)
        delayed.complete(MessagingRepositoryResult.Success(thread()))

        assertFalse(open.await())
        assertFalse(state.threadState is MessagingThreadUiState.Loaded &&
            (state.threadState as MessagingThreadUiState.Loaded).presentation.composerAccess is MessagingComposerAccess.Open)
    }

    @Test
    fun sendIsSerializedAndBusyPresentationCannotCompose() = runTest {
        val sendResult = CompletableDeferred<MessagingRepositoryResult<MarketplaceMessage>>()
        val repository = ControllableRepository(thread(), sendResult = sendResult)
        val state = MarketplaceMessagingState(repository, { SHOPPER }, ::composerAccess)
        state.loadInbox()
        state.openThread(CONVERSATION_ID)
        state.updateDraft("Can I bring a trailer?")

        val first = async { state.sendDraft(NOW) }
        runCurrent()
        assertFalse(assertIs<MessagingThreadUiState.Loaded>(state.threadState).presentation.canCompose)
        val second = async { state.sendDraft(NOW + 1) }
        runCurrent()

        assertFalse(second.await())
        assertEquals(1, repository.sendCount)
        sendResult.complete(MessagingRepositoryResult.ValidationFailure(listOf("Try again")))
        assertFalse(first.await())
    }

    @Test
    fun retryIsSerializedAndDelayedCompletionCannotWriteAfterClose() = runTest {
        val retryResult = CompletableDeferred<MessagingRepositoryResult<MarketplaceMessage>>()
        val repository = ControllableRepository(thread(), retryResult = retryResult)
        val state = MarketplaceMessagingState(repository, { SHOPPER }, ::composerAccess)
        state.loadInbox()
        state.openThread(CONVERSATION_ID)

        val first = async { state.retryMessage("message-0000002a", NOW) }
        runCurrent()
        val second = async { state.retryMessage("message-0000002a", NOW + 1) }
        runCurrent()
        assertFalse(second.await())
        assertEquals(1, repository.retryCount)

        state.closeAndClear(MessagingClosedReason.EVENT_CANCELLED)
        retryResult.complete(
            MessagingRepositoryResult.Success(
                MarketplaceMessage(
                    id = "message-0000002a",
                    conversationId = CONVERSATION_ID,
                    senderId = SHOPPER.userId,
                    body = "private body",
                    sentAtEpochMillis = NOW,
                    deliveryState = MessageDeliveryState.SENT,
                ),
            ),
        )
        assertFalse(first.await())
        assertEquals(
            MessagingClosedReason.EVENT_CANCELLED,
            assertIs<MessagingThreadUiState.Loaded>(state.threadState).presentation.closedReason,
        )
    }

    @Test
    fun delayedMarkReadCannotProjectAfterSessionClose() = runTest {
        val readResult = CompletableDeferred<MessagingRepositoryResult<Unit>>()
        val repository = ControllableRepository(thread(), readResult = readResult)
        val state = MarketplaceMessagingState(repository, { SHOPPER }, ::composerAccess)
        state.loadInbox()
        state.openThread(CONVERSATION_ID)

        val read = async { state.markCurrentThreadRead() }
        runCurrent()
        state.closeAndClear(MessagingClosedReason.CONVERSATION_UNAVAILABLE)
        readResult.complete(MessagingRepositoryResult.Success(Unit))

        assertFalse(read.await())
        assertEquals(
            MessagingClosedReason.CONVERSATION_UNAVAILABLE,
            assertIs<MessagingThreadUiState.Loaded>(state.threadState).presentation.closedReason,
        )
    }

    @Test
    fun unauthorizedMutationNarrowsOnlyTheMatchingThreadAndInboxSummary() = runTest {
        val sendResult = CompletableDeferred<MessagingRepositoryResult<MarketplaceMessage>>()
        val secondKey = MarketplaceConversationKey("event-second", KEY.shopperId)
        val repository = ControllableRepository(
            thread(),
            summaries = listOf(summary(KEY, CONVERSATION_ID), summary(secondKey, SECOND_CONVERSATION_ID)),
            sendResult = sendResult,
        )
        val state = MarketplaceMessagingState(repository, { SHOPPER }, ::composerAccess)
        state.loadInbox()
        state.openThread(CONVERSATION_ID)
        state.updateDraft("private draft")

        val send = async { state.sendDraft(NOW) }
        runCurrent()
        sendResult.complete(
            MessagingRepositoryResult.Unauthorized(MessagingClosedReason.LOCATION_ACCESS_REVOKED),
        )

        assertFalse(send.await())
        val presentation = assertIs<MessagingThreadUiState.Loaded>(state.threadState).presentation
        assertEquals("", presentation.draft)
        assertEquals(MessagingClosedReason.LOCATION_ACCESS_REVOKED, presentation.closedReason)
        val summaries = assertIs<MessagingInboxUiState.Loaded>(state.inboxState).threads
        assertEquals(
            MessagingClosedReason.LOCATION_ACCESS_REVOKED,
            assertIs<MessagingComposerAccess.Closed>(summaries.first().composerAccess).reason,
        )
        assertIs<MessagingComposerAccess.Open>(summaries.last().composerAccess)
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

    private class ControllableRepository(
        private val thread: MarketplaceMessageThread,
        private val summaries: List<MessageThreadSummary> = listOf(summary(thread.conversationKey, thread.conversationId)),
        private val sendResult: CompletableDeferred<MessagingRepositoryResult<MarketplaceMessage>>? = null,
        private val retryResult: CompletableDeferred<MessagingRepositoryResult<MarketplaceMessage>>? = null,
        private val readResult: CompletableDeferred<MessagingRepositoryResult<Unit>>? = null,
        private val threadResults: ArrayDeque<CompletableDeferred<MessagingRepositoryResult<MarketplaceMessageThread>>> = ArrayDeque(),
    ) : MarketplaceMessagingRepository {
        var sendCount: Int = 0
        var retryCount: Int = 0

        override suspend fun inboxFor(actor: MessagingActor) = MessagingRepositoryResult.Success(summaries)

        override suspend fun threadFor(
            conversationKey: MarketplaceConversationKey,
            actor: MessagingActor,
        ): MessagingRepositoryResult<MarketplaceMessageThread> =
            if (threadResults.isEmpty()) MessagingRepositoryResult.Success(thread)
            else threadResults.removeFirst().await()

        override suspend fun sendMessage(
            conversationKey: MarketplaceConversationKey,
            actor: MessagingActor,
            body: String,
            sentAtEpochMillis: Long,
        ): MessagingRepositoryResult<MarketplaceMessage> {
            sendCount++
            return sendResult?.await() ?: error("Unexpected send")
        }

        override suspend fun retryMessage(
            messageId: String,
            actor: MessagingActor,
            attemptedAtEpochMillis: Long,
        ): MessagingRepositoryResult<MarketplaceMessage> {
            retryCount++
            return retryResult?.await() ?: error("Unexpected retry")
        }

        override suspend fun markRead(
            conversationKey: MarketplaceConversationKey,
            actor: MessagingActor,
        ): MessagingRepositoryResult<Unit> = readResult?.await() ?: error("Unexpected mark read")
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
        const val CONVERSATION_ID = "conversation-0000002a"
        const val SECOND_CONVERSATION_ID = "conversation-0000002b"

        fun thread(
            key: MarketplaceConversationKey = KEY,
            conversationId: String = CONVERSATION_ID,
            composerAccess: MessagingComposerAccess = MessagingComposerAccess.Open,
            messages: List<MarketplaceMessage> = emptyList(),
        ) = MarketplaceMessageThread(
            conversationId = conversationId,
            conversationKey = key,
            eventTitle = "Sale",
            eventPhoto = null,
            messages = messages,
            composerAccess = composerAccess,
        )

        fun summary(
            key: MarketplaceConversationKey,
            conversationId: String,
            composerAccess: MessagingComposerAccess = MessagingComposerAccess.Open,
        ) = MessageThreadSummary(
            conversationId = conversationId,
            conversationKey = key,
            eventTitle = "Sale",
            eventPhoto = null,
            lastMessagePreview = null,
            lastMessageAtEpochMillis = null,
            unreadCount = 0,
            composerAccess = composerAccess,
        )

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

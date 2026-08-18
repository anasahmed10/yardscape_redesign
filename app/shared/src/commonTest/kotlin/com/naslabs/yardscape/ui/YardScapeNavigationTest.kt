package com.naslabs.yardscape.ui

import com.naslabs.yardscape.data.SeededYardSaleData
import com.naslabs.yardscape.data.MarketplaceMessagingRepository
import com.naslabs.yardscape.data.MessagingRepositoryResult
import com.naslabs.yardscape.domain.MarketplaceConversationKey
import com.naslabs.yardscape.domain.MarketplaceMessage
import com.naslabs.yardscape.domain.MarketplaceMessageThread
import com.naslabs.yardscape.domain.MessageThreadSummary
import com.naslabs.yardscape.domain.MessagingActor
import com.naslabs.yardscape.domain.MessagingClosedReason
import com.naslabs.yardscape.domain.MessagingComposerAccess
import com.naslabs.yardscape.domain.UserRole
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class YardScapeNavigationTest {
    @Test
    fun everyPrimaryDestinationIsReachableAndSelected() {
        val state = YardScapeAppState()

        YardScapePrimaryDestination.entries.forEach { destination ->
            state.navigateTo(destination)

            assertEquals(destination, state.activePrimaryDestination)
            assertEquals(destination, state.route.primaryDestination)
        }
    }

    @Test
    fun browseDetailRsvpAndBackPreserveEventContext() {
        val state = YardScapeAppState()
        val eventId = SeededYardSaleData.FAMILY_GARAGE_EVENT_ID

        state.openEvent(eventId)
        assertEquals(YardScapeRoute.EventDetail(eventId), state.route)
        assertEquals(YardScapePrimaryDestination.Browse, state.activePrimaryDestination)

        state.openRsvp(eventId)
        assertEquals(YardScapeRoute.Rsvp(eventId), state.route)
        assertTrue(state.navigateBack())
        assertEquals(YardScapeRoute.EventDetail(eventId), state.route)
        assertTrue(state.navigateBack())
        assertEquals(YardScapeRoute.Browse, state.route)
        assertFalse(state.navigateBack())
    }

    @Test
    fun hostEditorBackReturnsToSeparatedHostWorkspace() {
        val state = YardScapeAppState()

        state.navigateTo(YardScapePrimaryDestination.Host)
        state.openHostCreateEdit(SeededYardSaleData.DRAFT_EVENT_ID)

        assertEquals(YardScapePrimaryDestination.Host, state.activePrimaryDestination)
        assertTrue(state.navigateBack())
        assertEquals(YardScapeRoute.Host, state.route)
    }

    @Test
    fun messageThreadIsNestedUnderMessagesAndBackReturnsToInbox() = runTest {
        val conversationId = "conversation-0000002a"
        val state = messagingState(shopperId = SeededAttendeeIds.Accepted)

        state.loadMessagingInbox()
        assertTrue(state.openMessageThread(conversationId))

        assertEquals(YardScapePrimaryDestination.Messages, state.activePrimaryDestination)
        assertTrue(state.navigateBack())
        assertEquals(YardScapeRoute.Messages, state.route)
    }

    @Test
    fun signedOutMessageThreadPathAuthorizesAfterSignInWithoutParticipantIds() = runTest {
        val conversationId = "conversation-0000002a"
        val key = MarketplaceConversationKey(MESSAGE_EVENT_ID, SeededAttendeeIds.Accepted)
        val state = YardScapeAppState(
            shopperId = SeededAttendeeIds.Accepted,
            initialAccountStatus = MockSessionStatus.SignedOut,
            messagingRepository = TransitionMessagingRepository(key),
        )

        assertTrue(state.navigateToPath("/messages/$conversationId"))
        assertEquals(YardScapeRoute.Account, state.route)
        assertEquals(ProtectedAction.Messaging, state.pendingProtectedAction?.action)
        assertEquals(
            YardScapeRoute.MessageThread(conversationId),
            state.pendingProtectedAction?.resumeRoute,
        )

        state.signInMock(UserRole.SHOPPER)

        assertEquals(YardScapeRoute.Messages, state.route)
        assertTrue(state.resumePendingMessageThread())
        assertEquals(YardScapeRoute.MessageThread(conversationId), state.route)
        assertFalse(state.route.path.contains(SeededYardSaleData.FAMILY_GARAGE_EVENT_ID))
        assertFalse(state.route.path.contains(SeededYardSaleData.SHOPPER_WITH_ACCEPTED_ACCESS_ID))
    }

    @Test
    fun arbitraryConversationAndWrongRoleNeverCommitAThreadRoute() = runTest {
        val shopper = messagingState(shopperId = SeededAttendeeIds.Accepted)
        shopper.navigateTo(YardScapePrimaryDestination.Messages)

        assertFalse(shopper.openMessageThread("conversation-deadbeef"))
        assertEquals(YardScapeRoute.Messages, shopper.route)

        val wrongHost = messagingState(
            shopperId = SeededAttendeeIds.Accepted,
            activeUserRole = UserRole.HOST,
            hostId = "host-someone-else",
        )
        wrongHost.navigateTo(YardScapePrimaryDestination.Messages)
        assertFalse(wrongHost.openMessageThread(CONVERSATION_ID))
        assertEquals(YardScapeRoute.Messages, wrongHost.route)
    }

    @Test
    fun postSignInResumeWithNonParticipantRoleReturnsToMessages() = runTest {
        val key = MarketplaceConversationKey(MESSAGE_EVENT_ID, SeededAttendeeIds.Accepted)
        val state = YardScapeAppState(
            shopperId = key.shopperId,
            hostId = "host-someone-else",
            initialAccountStatus = MockSessionStatus.SignedOut,
            messagingRepository = TransitionMessagingRepository(key),
        )
        state.navigateToPath("/messages/$CONVERSATION_ID")

        state.signInMock(UserRole.HOST)
        assertEquals(YardScapeRoute.Messages, state.route)
        assertFalse(state.resumePendingMessageThread())
        assertEquals(YardScapeRoute.Messages, state.route)
    }

    @Test
    fun hostCannotOpenShopperSafetyFromOwnMessageThread() = runTest {
        val state = messagingState(
            shopperId = SeededAttendeeIds.Accepted,
            activeUserRole = UserRole.HOST,
        )
        state.loadMessagingInbox()
        assertTrue(state.openMessageThread(CONVERSATION_ID))

        state.openMessageThreadReport()
        assertEquals(YardScapeRoute.MessageThread(CONVERSATION_ID), state.route)
        state.openMessageThreadBlock()
        assertEquals(YardScapeRoute.MessageThread(CONVERSATION_ID), state.route)
    }

    @Test
    fun routeChangeWhileAuthorizationIsPendingCannotCommitTheThread() = runTest {
        val result = CompletableDeferred<MessagingRepositoryResult<MarketplaceMessageThread>>()
        val key = MarketplaceConversationKey(MESSAGE_EVENT_ID, SeededAttendeeIds.Accepted)
        val state = YardScapeAppState(
            shopperId = key.shopperId,
            messagingRepository = TransitionMessagingRepository(key, result),
        )
        state.loadMessagingInbox()

        val open = async { state.openMessageThread(CONVERSATION_ID) }
        runCurrent()
        state.navigateTo(YardScapePrimaryDestination.Browse)
        result.complete(MessagingRepositoryResult.Success(messageThread(key)))

        assertFalse(open.await())
        assertEquals(YardScapeRoute.Browse, state.route)
    }

    @Test
    fun signOutAndDifferentRoleReauthCannotProjectAnOldActorThread() = runTest {
        val result = CompletableDeferred<MessagingRepositoryResult<MarketplaceMessageThread>>()
        val key = MarketplaceConversationKey(MESSAGE_EVENT_ID, SeededAttendeeIds.Accepted)
        val state = YardScapeAppState(
            shopperId = key.shopperId,
            messagingRepository = TransitionMessagingRepository(key, result),
        )
        state.loadMessagingInbox()

        val open = async { state.openMessageThread(CONVERSATION_ID) }
        runCurrent()
        state.signOutMock()
        state.signInMock(UserRole.HOST)
        result.complete(MessagingRepositoryResult.Success(messageThread(key)))

        assertFalse(open.await())
        assertEquals(YardScapeRoute.Account, state.route)
        assertFalse(state.messagingThreadState is MessagingThreadUiState.Loaded &&
            (state.messagingThreadState as MessagingThreadUiState.Loaded).presentation.canCompose)
    }

    @Test
    fun signedOutMessagesDestinationResumesAtInboxAfterSignIn() {
        val state = YardScapeAppState(initialAccountStatus = MockSessionStatus.SignedOut)

        state.navigateTo(YardScapePrimaryDestination.Messages)

        assertEquals(YardScapeRoute.Account, state.route)
        assertEquals(ProtectedAction.Messaging, state.pendingProtectedAction?.action)
        assertEquals(YardScapeRoute.Messages, state.pendingProtectedAction?.resumeRoute)

        state.signInMock(UserRole.SHOPPER)

        assertEquals(YardScapeRoute.Messages, state.route)
    }

    @Test
    fun routePathsRoundTripForDeepLinkShapedState() {
        val routes = listOf(
            YardScapeRoute.Browse,
            YardScapeRoute.MyFinds(),
            YardScapeRoute.MyFinds(MyFindsSection.Rsvps),
            YardScapeRoute.Host,
            YardScapeRoute.Messages,
            YardScapeRoute.MessageThread("conversation-0000002a"),
            YardScapeRoute.Account,
            YardScapeRoute.EventDetail("event-123"),
            YardScapeRoute.Rsvp("event-123"),
            YardScapeRoute.EventSafety("event-123", ShopperSafetyAction.Report),
            YardScapeRoute.EventSafety("event-123", ShopperSafetyAction.Block),
            YardScapeRoute.HostCreateEdit(),
            YardScapeRoute.HostCreateEdit("event-123"),
            YardScapeRoute.HostAttendees("event-123"),
        )

        routes.forEach { route ->
            assertEquals(route, YardScapeRoute.fromPath(route.path))
        }
        assertEquals(YardScapeRoute.EventDetail("event-123"), YardScapeRoute.fromPath("/events/event-123?tab=overview"))
        assertNull(YardScapeRoute.fromPath("/private/location/123-cedar-street"))
    }

    @Test
    fun hostAttendeeManagementIsNestedUnderHostAndReturnsThere() {
        val state = YardScapeAppState(activeUserRole = com.naslabs.yardscape.domain.UserRole.HOST)
        val eventId = SeededYardSaleData.FAMILY_GARAGE_EVENT_ID

        assertTrue(state.openHostAttendees(eventId))
        assertEquals(YardScapeRoute.HostAttendees(eventId), state.route)
        assertEquals(YardScapePrimaryDestination.Host, state.activePrimaryDestination)
        assertTrue(state.navigateBack())
        assertEquals(YardScapeRoute.Host, state.route)
    }

    @Test
    fun myRsvpsIsNestedUnderMyFindsAndBackReturnsToBrowse() {
        val state = YardScapeAppState()

        state.openMyFinds(MyFindsSection.Rsvps)

        assertEquals(YardScapeRoute.MyFinds(MyFindsSection.Rsvps), state.route)
        assertEquals(YardScapePrimaryDestination.MyFinds, state.activePrimaryDestination)
        assertTrue(state.navigateBack())
        assertEquals(YardScapeRoute.Browse, state.route)
    }

    @Test
    fun navigationLabelsNeverContainProtectedLocationData() {
        val routes = listOf(
            YardScapeRoute.Browse,
            YardScapeRoute.MyFinds(),
            YardScapeRoute.Messages,
            YardScapeRoute.Host,
            YardScapeRoute.Account,
            YardScapeRoute.EventDetail(SeededYardSaleData.FAMILY_GARAGE_EVENT_ID),
            YardScapeRoute.Rsvp(SeededYardSaleData.FAMILY_GARAGE_EVENT_ID),
            YardScapeRoute.HostCreateEdit(SeededYardSaleData.DRAFT_EVENT_ID),
        )
        val labels = buildList {
            addAll(routes.map { it.destinationLabel })
            addAll(YardScapePrimaryDestination.entries.map { it.label })
            addAll(YardScapePrimaryDestination.entries.map { it.contextLabel })
        }.joinToString()

        assertFalse(labels.contains("123 Cedar Street"))
        assertFalse(labels.contains("418 Juniper Avenue"))
        assertFalse(labels.contains("47.6101"))
    }

    @Test
    fun appStateCanAcceptAValidPathWithoutExternalDeepLinkHandling() {
        val state = YardScapeAppState()

        assertTrue(state.navigateToPath("/host/events/${SeededYardSaleData.DRAFT_EVENT_ID}/edit"))
        assertEquals(YardScapeRoute.HostCreateEdit(SeededYardSaleData.DRAFT_EVENT_ID), state.route)
        assertFalse(state.navigateToPath("/unknown"))
        assertEquals(YardScapeRoute.HostCreateEdit(SeededYardSaleData.DRAFT_EVENT_ID), state.route)
    }

    @Test
    fun shopperRsvpCancellationRevocationAndExpirySynchronouslyCloseDraftedComposer() = runTest {
        suspend fun verify(mutate: (YardScapeAppState) -> Boolean) {
            val state = messagingState(shopperId = SeededAttendeeIds.Accepted)
            openDraftedThread(state)

            assertTrue(mutate(state))

            assertComposerClosedAndDraftCleared(state)
        }

        verify { state ->
            state.requestRsvpCancellation(MESSAGE_EVENT_ID) && state.confirmRsvpCancellation()
        }
        verify { state -> state.revokeRsvpAccess(MESSAGE_EVENT_ID) }
        verify { state -> state.expireRsvpAccess(MESSAGE_EVENT_ID) }
    }

    @Test
    fun hostRemoveAndRevokeSynchronouslyCloseDraftedComposer() = runTest {
        suspend fun verify(action: HostAttendeeAction) {
            val state = messagingState(
                shopperId = SeededAttendeeIds.Accepted,
                activeUserRole = UserRole.HOST,
            )
            openDraftedThread(state)

            assertTrue(state.requestHostAttendeeAction(MESSAGE_EVENT_ID, SeededAttendeeIds.Accepted, action))
            assertTrue(state.confirmHostAttendeeAction())

            assertComposerClosedAndDraftCleared(state)
        }

        verify(HostAttendeeAction.Revoke)
        verify(HostAttendeeAction.Remove)
    }

    @Test
    fun hostCancelAndHideSynchronouslyCloseDraftedComposer() = runTest {
        suspend fun verify(mutate: (YardScapeAppState) -> Unit) {
            val state = messagingState(
                shopperId = SeededAttendeeIds.Accepted,
                activeUserRole = UserRole.HOST,
            )
            openDraftedThread(state)

            mutate(state)

            assertComposerClosedAndDraftCleared(state)
        }

        verify { it.cancelHostEvent(MESSAGE_EVENT_ID) }
        verify { it.hideHostEvent(MESSAGE_EVENT_ID) }
    }

    @Test
    fun hostWideBlockSignOutAndSessionExpirySynchronouslyClearComposer() = runTest {
        val blocked = messagingState(shopperId = SeededAttendeeIds.Accepted)
        openDraftedThread(blocked)
        blocked.openMessageThreadBlock()
        blocked.requestBlockMutation()
        blocked.confirmBlockMutation()
        assertComposerClosedAndDraftCleared(blocked)

        blocked.openBlock(MESSAGE_EVENT_ID)
        blocked.requestBlockMutation()
        blocked.confirmBlockMutation()
        assertComposerClosedAndDraftCleared(blocked)
        assertIs<MessagingComposerAccess.Closed>(
            assertIs<MessagingThreadUiState.Loaded>(blocked.messagingThreadState)
                .presentation
                .composerAccess,
        )

        val signedOut = messagingState(shopperId = SeededAttendeeIds.Accepted)
        openDraftedThread(signedOut)
        signedOut.signOutMock()
        assertComposerClosedAndDraftCleared(signedOut)

        val expired = messagingState(shopperId = SeededAttendeeIds.Accepted)
        openDraftedThread(expired)
        expired.expireMockSession()
        assertComposerClosedAndDraftCleared(expired)
    }

    @Test
    fun reportFromThreadReturnsToTheSameOpaqueConversation() = runTest {
        val state = messagingState(shopperId = SeededAttendeeIds.Accepted)
        openDraftedThread(state)

        state.openMessageThreadReport()

        assertIs<YardScapeRoute.EventSafety>(state.route)
        assertTrue(state.navigateBack())
        assertEquals(YardScapeRoute.MessageThread(CONVERSATION_ID), state.route)
    }

    private fun messagingState(
        shopperId: String,
        activeUserRole: UserRole = UserRole.SHOPPER,
        hostId: String = SeededYardSaleData.HOST_AVERY_ID,
    ): YardScapeAppState {
        val key = MarketplaceConversationKey(MESSAGE_EVENT_ID, shopperId)
        return YardScapeAppState(
            shopperId = shopperId,
            hostId = hostId,
            activeUserRole = activeUserRole,
            messagingRepository = TransitionMessagingRepository(key),
        )
    }

    private suspend fun openDraftedThread(state: YardScapeAppState) {
        assertTrue(state.loadMessagingInbox())
        assertTrue(state.openMessageThread(CONVERSATION_ID))
        state.updateMessageDraft("A draft that must not survive access loss")
        val presentation = assertIs<MessagingThreadUiState.Loaded>(state.messagingThreadState).presentation
        assertTrue(presentation.canCompose)
        assertTrue(presentation.draft.isNotEmpty())
    }

    private fun assertComposerClosedAndDraftCleared(state: YardScapeAppState) {
        val presentation = assertIs<MessagingThreadUiState.Loaded>(state.messagingThreadState).presentation
        assertFalse(presentation.canCompose)
        assertEquals("", presentation.draft)
        val inbox = assertIs<MessagingInboxUiState.Loaded>(state.messagingInboxState)
        assertIs<MessagingComposerAccess.Closed>(inbox.threads.single().composerAccess)
    }

    private class TransitionMessagingRepository(
        private val key: MarketplaceConversationKey,
        private val threadResult: CompletableDeferred<MessagingRepositoryResult<MarketplaceMessageThread>>? = null,
    ) : MarketplaceMessagingRepository {
        private val thread = messageThread(key)

        override suspend fun inboxFor(actor: MessagingActor): MessagingRepositoryResult<List<MessageThreadSummary>> =
            MessagingRepositoryResult.Success(
                listOf(
                    MessageThreadSummary(
                        conversationId = CONVERSATION_ID,
                        conversationKey = key,
                        eventTitle = thread.eventTitle,
                        eventPhoto = null,
                        lastMessagePreview = null,
                        lastMessageAtEpochMillis = null,
                        unreadCount = 0,
                        composerAccess = MessagingComposerAccess.Open,
                    ),
                ),
            )

        override suspend fun threadFor(
            conversationKey: MarketplaceConversationKey,
            actor: MessagingActor,
        ): MessagingRepositoryResult<MarketplaceMessageThread> =
            threadResult?.await() ?: MessagingRepositoryResult.Success(thread)

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
        ): MessagingRepositoryResult<Unit> = MessagingRepositoryResult.Success(Unit)
    }

    private companion object {
        const val CONVERSATION_ID = "conversation-0000002a"
        const val MESSAGE_EVENT_ID = SeededYardSaleData.FAMILY_GARAGE_EVENT_ID

        fun messageThread(key: MarketplaceConversationKey) = MarketplaceMessageThread(
            conversationId = CONVERSATION_ID,
            conversationKey = key,
            eventTitle = "Maple Ridge Family Garage Sale",
            eventPhoto = null,
            messages = emptyList(),
            composerAccess = MessagingComposerAccess.Open,
        )
    }
}

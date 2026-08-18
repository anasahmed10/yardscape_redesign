package com.naslabs.yardscape.data

import com.naslabs.yardscape.domain.EventStatus
import com.naslabs.yardscape.domain.LocationVisibility
import com.naslabs.yardscape.domain.MARKETPLACE_MESSAGE_MAX_LENGTH
import com.naslabs.yardscape.domain.MarketplaceConversationKey
import com.naslabs.yardscape.domain.MarketplaceMessage
import com.naslabs.yardscape.domain.MarketplaceMessageThread
import com.naslabs.yardscape.domain.MessageDeliveryState
import com.naslabs.yardscape.domain.MessageThreadSummary
import com.naslabs.yardscape.domain.MessagingAccessContext
import com.naslabs.yardscape.domain.MessagingActor
import com.naslabs.yardscape.domain.MessagingClosedReason
import com.naslabs.yardscape.domain.MessagingComposerAccess
import com.naslabs.yardscape.domain.RsvpStatus
import com.naslabs.yardscape.domain.UserRole
import com.naslabs.yardscape.domain.toPublicPreview
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SeededMarketplaceMessagingRepositoryTest {
    @Test
    fun onlyLiveParticipantsCanOpenAndClosedExistingThreadStaysReadable() = runTest {
        val accessSource = MutableAccessSource(openContext())
        val repository = SeededMarketplaceMessagingRepository(accessSource = accessSource)

        val opened = successThread(repository.threadFor(KEY, SHOPPER))
        assertFalse(opened.conversationId.contains(KEY.eventId))
        assertFalse(opened.conversationId.contains(KEY.shopperId))
        assertEquals(MessagingComposerAccess.Open, opened.composerAccess)

        assertIs<MessagingRepositoryResult.Unauthorized>(
            repository.threadFor(KEY, SHOPPER.copy(userId = "shopper-other")),
        )
        assertIs<MessagingRepositoryResult.Unauthorized>(
            repository.threadFor(KEY, HOST.copy(userId = "host-other")),
        )

        accessSource.context = openContext().copy(eventStatus = EventStatus.CANCELLED)
        val closed = successThread(repository.threadFor(KEY, SHOPPER))
        assertEquals(
            MessagingComposerAccess.Closed(MessagingClosedReason.EVENT_CANCELLED),
            closed.composerAccess,
        )
        assertIs<MessagingRepositoryResult.Unauthorized>(
            repository.sendMessage(KEY, SHOPPER, "Is the sale still happening?", NOW),
        )

        val neverOpenedKey = MarketplaceConversationKey(
            eventId = SeededYardSaleData.ESTATE_TOOLS_EVENT_ID,
            shopperId = SHOPPER.userId,
        )
        accessSource.contexts[neverOpenedKey] = accessSource.context.copy(
            conversationKey = neverOpenedKey,
            hostId = SeededYardSaleData.HOST_MARIN_ID,
        )
        assertIs<MessagingRepositoryResult.Unauthorized>(
            repository.threadFor(neverOpenedKey, SHOPPER),
        )
    }

    @Test
    fun sentMessagesProduceUnreadCountsPerActorUntilMarkedRead() = runTest {
        val repository = SeededMarketplaceMessagingRepository(
            accessSource = MutableAccessSource(openContext()),
        )

        val sent = assertIs<MessagingRepositoryResult.Success<MarketplaceMessage>>(
            repository.sendMessage(KEY, HOST, "  Bikes are still available.  ", NOW),
        ).value
        assertEquals("Bikes are still available.", sent.body)
        assertEquals(0, successInbox(repository.inboxFor(HOST)).single().unreadCount)
        assertEquals(1, successInbox(repository.inboxFor(SHOPPER)).single().unreadCount)

        assertIs<MessagingRepositoryResult.Success<Unit>>(repository.markRead(KEY, SHOPPER))
        assertEquals(0, successInbox(repository.inboxFor(SHOPPER)).single().unreadCount)

        assertIs<MessagingRepositoryResult.Success<MarketplaceMessage>>(
            repository.sendMessage(KEY, SHOPPER, "Thanks!", NOW + 1),
        )
        assertEquals(1, successInbox(repository.inboxFor(HOST)).single().unreadCount)
        assertEquals(0, successInbox(repository.inboxFor(SHOPPER)).single().unreadCount)
    }

    @Test
    fun blankAndOversizedMessagesReturnValidationFailuresWithoutCreatingThread() = runTest {
        val repository = SeededMarketplaceMessagingRepository(
            accessSource = MutableAccessSource(openContext()),
        )

        assertIs<MessagingRepositoryResult.ValidationFailure>(
            repository.sendMessage(KEY, SHOPPER, "   ", NOW),
        )
        assertIs<MessagingRepositoryResult.ValidationFailure>(
            repository.sendMessage(
                KEY,
                SHOPPER,
                "a".repeat(MARKETPLACE_MESSAGE_MAX_LENGTH + 1),
                NOW,
            ),
        )
        assertTrue(successInbox(repository.inboxFor(SHOPPER)).isEmpty())
    }

    @Test
    fun offlineFailurePersistsAndRetryUpdatesSameMessageToSent() = runTest {
        val repository = SeededMarketplaceMessagingRepository(
            accessSource = MutableAccessSource(openContext()),
            behavior = SeededMessagingBehavior(
                deliveryOutcomes = listOf(
                    SeededMessageOutcome.Offline,
                    SeededMessageOutcome.Success,
                ),
            ),
        )

        assertIs<MessagingRepositoryResult.Offline>(
            repository.sendMessage(KEY, SHOPPER, "  Can I bring a trailer?  ", NOW),
        )
        val failed = successThread(repository.threadFor(KEY, SHOPPER)).messages.single()
        assertEquals("Can I bring a trailer?", failed.body)
        assertEquals(MessageDeliveryState.FAILED, failed.deliveryState)
        assertEquals(0, successInbox(repository.inboxFor(HOST)).single().unreadCount)

        val retried = assertIs<MessagingRepositoryResult.Success<MarketplaceMessage>>(
            repository.retryMessage(failed.id, SHOPPER, NOW + 1),
        ).value
        assertEquals(failed.id, retried.id)
        assertEquals(MessageDeliveryState.SENT, retried.deliveryState)

        val messages = successThread(repository.threadFor(KEY, SHOPPER)).messages
        assertEquals(1, messages.size)
        assertEquals(MessageDeliveryState.SENT, messages.single().deliveryState)
        assertEquals(1, successInbox(repository.inboxFor(HOST)).single().unreadCount)
    }

    @Test
    fun serverFailureIsTypedAndPersistsFailedMessage() = runTest {
        val repository = SeededMarketplaceMessagingRepository(
            accessSource = MutableAccessSource(openContext()),
            behavior = SeededMessagingBehavior(
                deliveryOutcomes = listOf(SeededMessageOutcome.ServerError),
            ),
        )

        assertIs<MessagingRepositoryResult.ServerError>(
            repository.sendMessage(KEY, HOST, "Please try again later.", NOW),
        )
        assertEquals(
            MessageDeliveryState.FAILED,
            successThread(repository.threadFor(KEY, HOST)).messages.single().deliveryState,
        )
    }

    @Test
    fun retryRechecksCancellationRevocationAndBlockWithoutMutatingFailure() = runTest {
        listOf(
            openContext().copy(eventStatus = EventStatus.CANCELLED) to
                MessagingClosedReason.EVENT_CANCELLED,
            openContext().copy(locationVisibility = LocationVisibility.REVOKED) to
                MessagingClosedReason.LOCATION_ACCESS_REVOKED,
            openContext().copy(isBlocked = true) to MessagingClosedReason.BLOCKED,
        ).forEach { (closedContext, expectedReason) ->
            val accessSource = MutableAccessSource(openContext())
            val repository = SeededMarketplaceMessagingRepository(
                accessSource = accessSource,
                behavior = SeededMessagingBehavior(
                    deliveryOutcomes = listOf(
                        SeededMessageOutcome.Offline,
                        SeededMessageOutcome.Success,
                    ),
                ),
            )
            assertIs<MessagingRepositoryResult.Offline>(
                repository.sendMessage(KEY, SHOPPER, "Will retry", NOW),
            )
            val messageId = successThread(repository.threadFor(KEY, SHOPPER)).messages.single().id

            accessSource.context = closedContext
            val denied = assertIs<MessagingRepositoryResult.Unauthorized>(
                repository.retryMessage(messageId, SHOPPER, NOW + 1),
            )

            assertEquals(expectedReason, denied.reason)
            assertEquals(
                MessageDeliveryState.FAILED,
                successThread(repository.threadFor(KEY, SHOPPER)).messages.single().deliveryState,
            )
        }
    }

    @Test
    fun projectionsAndDiagnosticStringsExcludeProtectedLocationAndPrivateBodies() = runTest {
        val protectedEvent = SeededYardSaleData.events
            .single { it.id == KEY.eventId }
            .let { event ->
                event.copy(
                    location = event.location.copy(
                        exactAddress = event.location.exactAddress.copy(unit = "Private Unit 7B"),
                    ),
                )
            }
        val repository = SeededMarketplaceMessagingRepository(
            accessSource = MutableAccessSource(openContext()),
            eventPreviews = listOf(protectedEvent.toPublicPreview()),
        )
        val privateBody =
            "Meet at 123 Cedar Street, Private Unit 7B; use the side gate by the blue planter."
        val message = assertIs<MessagingRepositoryResult.Success<MarketplaceMessage>>(
            repository.sendMessage(KEY, HOST, privateBody, NOW),
        ).value
        val summary = successInbox(repository.inboxFor(SHOPPER)).single()
        val thread = successThread(repository.threadFor(KEY, SHOPPER))

        assertEquals("Maple Ridge Family Garage Sale", summary.eventTitle)
        assertEquals("seed://maple-ridge-driveway", summary.eventPhoto?.url)
        listOf(
            "123 Cedar Street",
            "47.6101",
            "-122.2015",
            "Private Unit 7B",
            "side gate by the blue planter",
        ).forEach { protectedText ->
            assertFalse(summary.toString().contains(protectedText))
            assertFalse(thread.toString().contains(protectedText))
            assertFalse(message.toString().contains(protectedText))
        }
    }

    private fun successThread(
        result: MessagingRepositoryResult<MarketplaceMessageThread>,
    ): MarketplaceMessageThread =
        assertIs<MessagingRepositoryResult.Success<MarketplaceMessageThread>>(result).value

    private fun successInbox(
        result: MessagingRepositoryResult<List<MessageThreadSummary>>,
    ): List<MessageThreadSummary> =
        assertIs<MessagingRepositoryResult.Success<List<MessageThreadSummary>>>(
            result,
        ).value

    private class MutableAccessSource(initial: MessagingAccessContext) : MarketplaceMessagingAccessSource {
        val contexts = mutableMapOf(initial.conversationKey to initial)

        var context: MessagingAccessContext
            get() = contexts.getValue(KEY)
            set(value) {
                contexts[KEY] = value
            }

        override fun accessContextFor(key: MarketplaceConversationKey): MessagingAccessContext? =
            contexts[key]
    }

    private fun openContext(): MessagingAccessContext = MessagingAccessContext(
        conversationKey = KEY,
        hostId = SeededYardSaleData.HOST_AVERY_ID,
        eventStatus = EventStatus.PUBLISHED,
        eventHasEnded = false,
        rsvpStatus = RsvpStatus.ACCEPTED,
        locationVisibility = LocationVisibility.RSVP_ACCEPTED,
        isBlocked = false,
    )

    private companion object {
        val KEY = MarketplaceConversationKey(
            eventId = SeededYardSaleData.FAMILY_GARAGE_EVENT_ID,
            shopperId = SeededYardSaleData.SHOPPER_WITH_ACCEPTED_ACCESS_ID,
        )
        val SHOPPER = MessagingActor(KEY.shopperId, UserRole.SHOPPER)
        val HOST = MessagingActor(SeededYardSaleData.HOST_AVERY_ID, UserRole.HOST)
        const val NOW = SeededYardSaleData.BASE_NOW_EPOCH_MILLIS
    }
}

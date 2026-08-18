package com.naslabs.yardscape.data

import com.naslabs.yardscape.domain.MARKETPLACE_MESSAGE_MAX_LENGTH
import com.naslabs.yardscape.domain.EventPhoto
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
import com.naslabs.yardscape.domain.PublicEventPreview
import com.naslabs.yardscape.domain.UserRole
import com.naslabs.yardscape.domain.toPublicPreview

enum class SeededMessageOutcome {
    Success,
    Offline,
    ServerError,
}

data class SeededMessagingBehavior(
    val deliveryOutcomes: List<SeededMessageOutcome> = emptyList(),
)

class SeededMarketplaceMessagingRepository(
    private val accessSource: MarketplaceMessagingAccessSource,
    eventPreviews: List<PublicEventPreview> = SeededYardSaleData.events.map { it.toPublicPreview() },
    private val behavior: SeededMessagingBehavior = SeededMessagingBehavior(),
) : MarketplaceMessagingRepository {
    private val eventPreviewsById = eventPreviews.associateBy { it.id }
    private val conversationsByKey = linkedMapOf<MarketplaceConversationKey, ConversationRecord>()
    private var nextConversationNumber = 1
    private var nextMessageNumber = 1
    private var nextDeliveryOutcomeIndex = 0

    override suspend fun inboxFor(
        actor: MessagingActor,
    ): MessagingRepositoryResult<List<MessageThreadSummary>> =
        MessagingRepositoryResult.Success(
            conversationsByKey.values
                .filter { record -> actor in record.authorizedActors }
                .mapNotNull { record ->
                    val context = accessSource.accessContextFor(record.key) ?: return@mapNotNull null
                    if (!isParticipant(context, actor)) return@mapNotNull null
                    record.toSummary(
                        actor = actor,
                        access = MarketplaceMessagingPolicy.composerAccess(context, actor),
                    )
                }
                .sortedByDescending { it.lastMessageAtEpochMillis ?: Long.MIN_VALUE },
        )

    override suspend fun threadFor(
        conversationKey: MarketplaceConversationKey,
        actor: MessagingActor,
    ): MessagingRepositoryResult<MarketplaceMessageThread> {
        val context = accessSource.accessContextFor(conversationKey)
            ?: return unavailable()
        val access = MarketplaceMessagingPolicy.composerAccess(context, actor)
        val existing = conversationsByKey[conversationKey]

        if (access is MessagingComposerAccess.Open) {
            val record = existing ?: createConversation(conversationKey)
            record.authorizeParticipants(context)
            return MessagingRepositoryResult.Success(record.toThread(actor, access))
        }
        if (existing != null && actor in existing.authorizedActors && isParticipant(context, actor)) {
            return MessagingRepositoryResult.Success(existing.toThread(actor, access))
        }
        return MessagingRepositoryResult.Unauthorized(
            reason = (access as MessagingComposerAccess.Closed).reason,
        )
    }

    override suspend fun sendMessage(
        conversationKey: MarketplaceConversationKey,
        actor: MessagingActor,
        body: String,
        sentAtEpochMillis: Long,
    ): MessagingRepositoryResult<MarketplaceMessage> {
        val context = accessSource.accessContextFor(conversationKey)
            ?: return unavailable()
        val access = MarketplaceMessagingPolicy.composerAccess(context, actor)
        if (access is MessagingComposerAccess.Closed) {
            return MessagingRepositoryResult.Unauthorized(access.reason)
        }

        val normalizedBody = body.trim()
        val validationMessages = buildList {
            if (normalizedBody.isEmpty()) add("Enter a message before sending.")
            if (normalizedBody.length > MARKETPLACE_MESSAGE_MAX_LENGTH) {
                add("Messages must be $MARKETPLACE_MESSAGE_MAX_LENGTH characters or fewer.")
            }
        }
        if (validationMessages.isNotEmpty()) {
            return MessagingRepositoryResult.ValidationFailure(validationMessages)
        }

        val record = conversationsByKey[conversationKey] ?: createConversation(conversationKey)
        record.authorizeParticipants(context)
        val outcome = nextDeliveryOutcome()
        val message = MarketplaceMessage(
            id = nextOpaqueId(prefix = "message", number = nextMessageNumber++),
            conversationId = record.conversationId,
            senderId = actor.userId,
            body = normalizedBody,
            sentAtEpochMillis = sentAtEpochMillis,
            deliveryState = when (outcome) {
                SeededMessageOutcome.Success -> MessageDeliveryState.SENT
                SeededMessageOutcome.Offline,
                SeededMessageOutcome.ServerError,
                -> MessageDeliveryState.FAILED
            },
        )
        record.messages += message
        record.readByMessageId.getOrPut(message.id, ::mutableSetOf) += actor.userId
        return outcome.toRepositoryResult(message)
    }

    override suspend fun retryMessage(
        messageId: String,
        actor: MessagingActor,
        attemptedAtEpochMillis: Long,
    ): MessagingRepositoryResult<MarketplaceMessage> {
        val record = conversationsByKey.values.firstOrNull { candidate ->
            candidate.messages.any { it.id == messageId }
        } ?: return unavailable()
        val context = accessSource.accessContextFor(record.key) ?: return unavailable()
        val access = MarketplaceMessagingPolicy.composerAccess(context, actor)
        if (access is MessagingComposerAccess.Closed) {
            return MessagingRepositoryResult.Unauthorized(access.reason)
        }

        val messageIndex = record.messages.indexOfFirst { it.id == messageId }
        val current = record.messages[messageIndex]
        if (current.senderId != actor.userId) {
            return MessagingRepositoryResult.Unauthorized(MessagingClosedReason.NOT_PARTICIPANT)
        }
        if (current.deliveryState != MessageDeliveryState.FAILED) {
            return MessagingRepositoryResult.ValidationFailure(
                listOf("Only failed messages can be retried."),
            )
        }

        record.authorizeParticipants(context)
        val outcome = nextDeliveryOutcome()
        val updated = current.copy(
            sentAtEpochMillis = attemptedAtEpochMillis,
            deliveryState = when (outcome) {
                SeededMessageOutcome.Success -> MessageDeliveryState.SENT
                SeededMessageOutcome.Offline,
                SeededMessageOutcome.ServerError,
                -> MessageDeliveryState.FAILED
            },
        )
        record.messages[messageIndex] = updated
        record.readByMessageId.getOrPut(updated.id, ::mutableSetOf) += actor.userId
        return outcome.toRepositoryResult(updated)
    }

    override suspend fun markRead(
        conversationKey: MarketplaceConversationKey,
        actor: MessagingActor,
    ): MessagingRepositoryResult<Unit> {
        val record = conversationsByKey[conversationKey] ?: return unavailable()
        val context = accessSource.accessContextFor(conversationKey) ?: return unavailable()
        val access = MarketplaceMessagingPolicy.composerAccess(context, actor)
        if (access is MessagingComposerAccess.Open) record.authorizeParticipants(context)
        if (actor !in record.authorizedActors || !isParticipant(context, actor)) {
            val reason = (access as? MessagingComposerAccess.Closed)?.reason
                ?: MessagingClosedReason.NOT_PARTICIPANT
            return MessagingRepositoryResult.Unauthorized(reason)
        }
        record.visibleMessages(actor).forEach { message ->
            record.readByMessageId.getOrPut(message.id, ::mutableSetOf) += actor.userId
        }
        return MessagingRepositoryResult.Success(Unit)
    }

    private fun createConversation(key: MarketplaceConversationKey): ConversationRecord {
        val preview = eventPreviewsById[key.eventId]
        return ConversationRecord(
            conversationId = nextOpaqueId(prefix = "conversation", number = nextConversationNumber++),
            key = key,
            eventTitle = preview?.title ?: "Yard sale",
            eventPhoto = preview?.photos?.firstOrNull(),
        ).also { conversationsByKey[key] = it }
    }

    private fun nextDeliveryOutcome(): SeededMessageOutcome =
        behavior.deliveryOutcomes.getOrNull(nextDeliveryOutcomeIndex++) ?: SeededMessageOutcome.Success

    private fun unavailable(): MessagingRepositoryResult.Unauthorized =
        MessagingRepositoryResult.Unauthorized(MessagingClosedReason.CONVERSATION_UNAVAILABLE)

    private fun SeededMessageOutcome.toRepositoryResult(
        message: MarketplaceMessage,
    ): MessagingRepositoryResult<MarketplaceMessage> = when (this) {
        SeededMessageOutcome.Success -> MessagingRepositoryResult.Success(message)
        SeededMessageOutcome.Offline -> MessagingRepositoryResult.Offline()
        SeededMessageOutcome.ServerError -> MessagingRepositoryResult.ServerError()
    }

    private fun ConversationRecord.authorizeParticipants(context: MessagingAccessContext) {
        authorizedActors += MessagingActor(context.hostId, UserRole.HOST)
        authorizedActors += MessagingActor(context.conversationKey.shopperId, UserRole.SHOPPER)
    }

    private fun ConversationRecord.toSummary(
        actor: MessagingActor,
        access: MessagingComposerAccess,
    ): MessageThreadSummary {
        val visibleMessages = visibleMessages(actor)
        val lastMessage = visibleMessages.maxByOrNull { it.sentAtEpochMillis }
        return MessageThreadSummary(
            conversationId = conversationId,
            conversationKey = key,
            eventTitle = eventTitle,
            eventPhoto = eventPhoto,
            lastMessagePreview = lastMessage?.body,
            lastMessageAtEpochMillis = lastMessage?.sentAtEpochMillis,
            unreadCount = visibleMessages.count { message ->
                message.deliveryState == MessageDeliveryState.SENT &&
                    actor.userId !in readByMessageId[message.id].orEmpty()
            },
            composerAccess = access,
        )
    }

    private fun ConversationRecord.toThread(
        actor: MessagingActor,
        access: MessagingComposerAccess,
    ): MarketplaceMessageThread = MarketplaceMessageThread(
        conversationId = conversationId,
        conversationKey = key,
        eventTitle = eventTitle,
        eventPhoto = eventPhoto,
        messages = visibleMessages(actor),
        composerAccess = access,
    )

    private fun ConversationRecord.visibleMessages(actor: MessagingActor): List<MarketplaceMessage> =
        messages.filter { message ->
            message.deliveryState == MessageDeliveryState.SENT || message.senderId == actor.userId
        }

    private fun isParticipant(
        context: MessagingAccessContext,
        actor: MessagingActor,
    ): Boolean = when (actor.role) {
        UserRole.HOST -> actor.userId == context.hostId
        UserRole.SHOPPER -> actor.userId == context.conversationKey.shopperId
    }

    private fun nextOpaqueId(prefix: String, number: Int): String =
        "$prefix-${number.toString(16).padStart(8, '0')}"

    private data class ConversationRecord(
        val conversationId: String,
        val key: MarketplaceConversationKey,
        val eventTitle: String,
        val eventPhoto: EventPhoto?,
        val messages: MutableList<MarketplaceMessage> = mutableListOf(),
        val readByMessageId: MutableMap<String, MutableSet<String>> = mutableMapOf(),
        val authorizedActors: MutableSet<MessagingActor> = mutableSetOf(),
    )
}

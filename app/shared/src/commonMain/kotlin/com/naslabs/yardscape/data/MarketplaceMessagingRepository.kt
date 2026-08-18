package com.naslabs.yardscape.data

import com.naslabs.yardscape.domain.MarketplaceConversationKey
import com.naslabs.yardscape.domain.MarketplaceMessage
import com.naslabs.yardscape.domain.MarketplaceMessageThread
import com.naslabs.yardscape.domain.MessageThreadSummary
import com.naslabs.yardscape.domain.MessagingAccessContext
import com.naslabs.yardscape.domain.MessagingActor
import com.naslabs.yardscape.domain.MessagingClosedReason

interface MarketplaceMessagingRepository {
    suspend fun inboxFor(
        actor: MessagingActor,
    ): MessagingRepositoryResult<List<MessageThreadSummary>>

    suspend fun threadFor(
        conversationKey: MarketplaceConversationKey,
        actor: MessagingActor,
    ): MessagingRepositoryResult<MarketplaceMessageThread>

    suspend fun sendMessage(
        conversationKey: MarketplaceConversationKey,
        actor: MessagingActor,
        body: String,
        sentAtEpochMillis: Long,
    ): MessagingRepositoryResult<MarketplaceMessage>

    suspend fun retryMessage(
        messageId: String,
        actor: MessagingActor,
        attemptedAtEpochMillis: Long,
    ): MessagingRepositoryResult<MarketplaceMessage>

    suspend fun markRead(
        conversationKey: MarketplaceConversationKey,
        actor: MessagingActor,
    ): MessagingRepositoryResult<Unit>
}

interface MarketplaceMessagingAccessSource {
    fun accessContextFor(key: MarketplaceConversationKey): MessagingAccessContext?
}

sealed interface MessagingRepositoryResult<out T> {
    data class Success<T>(val value: T) : MessagingRepositoryResult<T>

    data class ValidationFailure(val messages: List<String>) : MessagingRepositoryResult<Nothing>

    data class Unauthorized(
        val reason: MessagingClosedReason,
        val message: String = "You cannot access this conversation.",
    ) : MessagingRepositoryResult<Nothing>

    data class Offline(
        val message: String = "You're offline. Your message was saved so you can retry it.",
    ) : MessagingRepositoryResult<Nothing>

    data class ServerError(
        val message: String = "The messaging service could not send this message. Try again.",
    ) : MessagingRepositoryResult<Nothing>
}

package com.naslabs.yardscape.domain

const val MARKETPLACE_MESSAGE_MAX_LENGTH: Int = 2_000

data class MarketplaceConversationKey(
    val eventId: String,
    val shopperId: String,
)

data class MessagingActor(
    val userId: String,
    val role: UserRole,
)

data class MessagingAccessContext(
    val conversationKey: MarketplaceConversationKey,
    val hostId: String,
    val eventStatus: EventStatus,
    val eventHasEnded: Boolean,
    val rsvpStatus: RsvpStatus?,
    val locationVisibility: LocationVisibility?,
    val isBlocked: Boolean,
)

sealed interface MessagingComposerAccess {
    data object Open : MessagingComposerAccess

    data class Closed(val reason: MessagingClosedReason) : MessagingComposerAccess
}

enum class MessagingClosedReason {
    CONVERSATION_UNAVAILABLE,
    NOT_PARTICIPANT,
    NOT_EVENT_HOST,
    RSVP_REQUIRED,
    RSVP_NOT_ACCEPTED,
    LOCATION_ACCESS_INACTIVE,
    LOCATION_ACCESS_REVOKED,
    LOCATION_ACCESS_EXPIRED,
    BLOCKED,
    EVENT_NOT_PUBLISHED,
    EVENT_CANCELLED,
    EVENT_COMPLETED,
    EVENT_HIDDEN,
    EVENT_ENDED,
}

enum class MessageDeliveryState {
    SENT,
    FAILED,
}

data class MarketplaceMessage(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val body: String,
    val sentAtEpochMillis: Long,
    val deliveryState: MessageDeliveryState,
) {
    override fun toString(): String =
        "MarketplaceMessage(id=$id, conversationId=$conversationId, senderId=$senderId, " +
            "bodyLength=${body.length}, sentAtEpochMillis=$sentAtEpochMillis, " +
            "deliveryState=$deliveryState)"
}

data class MessageThreadSummary(
    val conversationId: String,
    val conversationKey: MarketplaceConversationKey,
    val eventTitle: String,
    val eventPhoto: EventPhoto?,
    val lastMessagePreview: String?,
    val lastMessageAtEpochMillis: Long?,
    val unreadCount: Int,
    val composerAccess: MessagingComposerAccess,
) {
    override fun toString(): String =
        "MessageThreadSummary(conversationId=$conversationId, conversationKey=$conversationKey, " +
            "eventTitle=$eventTitle, hasEventPhoto=${eventPhoto != null}, " +
            "hasLastMessage=${lastMessagePreview != null}, " +
            "lastMessageAtEpochMillis=$lastMessageAtEpochMillis, unreadCount=$unreadCount, " +
            "composerAccess=$composerAccess)"
}

data class MarketplaceMessageThread(
    val conversationId: String,
    val conversationKey: MarketplaceConversationKey,
    val eventTitle: String,
    val eventPhoto: EventPhoto?,
    val messages: List<MarketplaceMessage>,
    val composerAccess: MessagingComposerAccess,
) {
    override fun toString(): String =
        "MarketplaceMessageThread(conversationId=$conversationId, " +
            "conversationKey=$conversationKey, eventTitle=$eventTitle, " +
            "hasEventPhoto=${eventPhoto != null}, messageCount=${messages.size}, " +
            "composerAccess=$composerAccess)"
}

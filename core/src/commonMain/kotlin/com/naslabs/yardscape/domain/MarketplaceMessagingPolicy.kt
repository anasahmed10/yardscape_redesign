package com.naslabs.yardscape.domain

object MarketplaceMessagingPolicy {
    fun composerAccess(
        context: MessagingAccessContext,
        actor: MessagingActor,
    ): MessagingComposerAccess {
        participantFailure(context, actor)?.let { return MessagingComposerAccess.Closed(it) }

        val closedReason = when {
            context.isBlocked -> MessagingClosedReason.BLOCKED
            context.eventStatus == EventStatus.CANCELLED -> MessagingClosedReason.EVENT_CANCELLED
            context.eventStatus == EventStatus.COMPLETED -> MessagingClosedReason.EVENT_COMPLETED
            context.eventStatus == EventStatus.HIDDEN -> MessagingClosedReason.EVENT_HIDDEN
            context.eventStatus != EventStatus.PUBLISHED -> MessagingClosedReason.EVENT_NOT_PUBLISHED
            context.eventHasEnded -> MessagingClosedReason.EVENT_ENDED
            context.rsvpStatus == null -> MessagingClosedReason.RSVP_REQUIRED
            context.rsvpStatus != RsvpStatus.ACCEPTED -> MessagingClosedReason.RSVP_NOT_ACCEPTED
            context.locationVisibility == LocationVisibility.REVOKED ->
                MessagingClosedReason.LOCATION_ACCESS_REVOKED
            context.locationVisibility == LocationVisibility.EXPIRED ->
                MessagingClosedReason.LOCATION_ACCESS_EXPIRED
            context.locationVisibility != LocationVisibility.RSVP_ACCEPTED ->
                MessagingClosedReason.LOCATION_ACCESS_INACTIVE
            else -> null
        }
        return closedReason?.let(MessagingComposerAccess::Closed)
            ?: MessagingComposerAccess.Open
    }

    private fun participantFailure(
        context: MessagingAccessContext,
        actor: MessagingActor,
    ): MessagingClosedReason? = when (actor.role) {
        UserRole.SHOPPER ->
            MessagingClosedReason.NOT_PARTICIPANT.takeIf {
                actor.userId != context.conversationKey.shopperId
            }
        UserRole.HOST ->
            MessagingClosedReason.NOT_EVENT_HOST.takeIf {
                actor.userId != context.hostId
            }
    }
}

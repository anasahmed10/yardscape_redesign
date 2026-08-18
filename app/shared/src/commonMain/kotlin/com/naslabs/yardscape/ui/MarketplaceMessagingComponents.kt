package com.naslabs.yardscape.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.naslabs.yardscape.domain.MarketplaceMessage
import com.naslabs.yardscape.domain.MessageDeliveryState
import com.naslabs.yardscape.domain.MessageThreadSummary
import com.naslabs.yardscape.domain.MessagingClosedReason
import com.naslabs.yardscape.domain.MessagingComposerAccess

internal const val MARKETPLACE_MESSAGING_MINIMUM_TARGET_DP = 48

internal enum class MarketplaceMessagingLayout {
    Compact,
    Expanded,
}

internal fun marketplaceMessagingLayoutFor(width: Dp): MarketplaceMessagingLayout =
    if (width >= 760.dp) MarketplaceMessagingLayout.Expanded else MarketplaceMessagingLayout.Compact

internal fun marketplaceMessagingContentWidthFor(availableWidth: Dp, maximumWidth: Dp): Dp =
    minOf(availableWidth, maximumWidth)

internal class MarketplaceMessagingLifecycle(
    private val onLoadInbox: suspend () -> Boolean,
    private val onResumePendingThread: suspend () -> Boolean,
) {
    private var handledAuthorizationSignal: Long? = null

    suspend fun handleAuthorizationSignal(
        signal: Long,
        hasPendingAuthorization: Boolean,
        inboxState: MessagingInboxUiState,
    ): Boolean {
        if (handledAuthorizationSignal == signal) return false
        handledAuthorizationSignal = signal
        return when {
            hasPendingAuthorization -> onResumePendingThread()
            inboxState is MessagingInboxUiState.Idle -> onLoadInbox()
            else -> false
        }
    }
}

internal sealed interface MarketplaceInboxPresentation {
    data object Loading : MarketplaceInboxPresentation
    data class Empty(val actionLabel: String = "Browse sales") : MarketplaceInboxPresentation
    data class Content(val rows: List<MarketplaceInboxRowPresentation>) : MarketplaceInboxPresentation
    data class Error(val title: String, val message: String, val actionLabel: String) : MarketplaceInboxPresentation
}

internal data class MarketplaceInboxRowPresentation(
    val conversationId: String,
    val title: String,
    val artwork: ShopperEventArtworkPresentation,
    val preview: String?,
    val lastActivityLabel: String?,
    val unreadLabel: String?,
    val minimumHeight: Dp = MARKETPLACE_MESSAGING_MINIMUM_TARGET_DP.dp,
) {
    val isUnread: Boolean
        get() = unreadLabel != null
    val contentDescription: String
        get() = listOfNotNull("Open messages for $title", unreadLabel).joinToString(", ")

    override fun toString(): String =
        "MarketplaceInboxRowPresentation(conversationId=$conversationId, title=$title, " +
            "hasPreview=${preview != null}, unread=$isUnread)"
}

internal data class MarketplaceMessageBubblePresentation(
    val id: String,
    val body: String,
    val isOutgoing: Boolean,
    val timeLabel: String,
    val deliveryLabel: String?,
    val deliveryTone: MarketplaceMessageDeliveryTone?,
    val showsRetry: Boolean,
    val isRetryEnabled: Boolean,
) {
    override fun toString(): String =
        "MarketplaceMessageBubblePresentation(id=$id, bodyLength=${body.length}, " +
            "isOutgoing=$isOutgoing, deliveryFailed=$showsRetry)"
}

internal enum class MarketplaceMessageDeliveryTone {
    Normal,
    Error,
}

internal data class MarketplaceClosedBannerPresentation(val title: String, val message: String)

internal data class MarketplaceComposerPresentation(
    val isVisible: Boolean,
    val isEnabled: Boolean,
    val sendLabel: String = "Send message",
    val minimumHeight: Dp = MARKETPLACE_MESSAGING_MINIMUM_TARGET_DP.dp,
)

internal data class MarketplaceEventActionPresentation(
    val label: String = "View sale",
    val minimumHeight: Dp = MARKETPLACE_MESSAGING_MINIMUM_TARGET_DP.dp,
)

internal data class MarketplaceThreadPresentation(
    val eventTitle: String,
    val artwork: ShopperEventArtworkPresentation,
    val messages: List<MarketplaceMessageBubblePresentation>,
    val composer: MarketplaceComposerPresentation,
    val closedBanner: MarketplaceClosedBannerPresentation?,
    val eventAction: MarketplaceEventActionPresentation = MarketplaceEventActionPresentation(),
    val showReportAction: Boolean,
    val showBlockAction: Boolean,
) {
    override fun toString(): String =
        "MarketplaceThreadPresentation(eventTitle=$eventTitle, messageCount=${messages.size}, " +
            "composerVisible=${composer.isVisible}, hasClosedBanner=${closedBanner != null}, " +
            "showReportAction=$showReportAction, showBlockAction=$showBlockAction)"
}

internal fun marketplaceInboxPresentation(state: MessagingInboxUiState): MarketplaceInboxPresentation = when (state) {
    MessagingInboxUiState.Idle,
    MessagingInboxUiState.Loading,
    -> MarketplaceInboxPresentation.Loading

    is MessagingInboxUiState.Loaded -> if (state.threads.isEmpty()) {
        MarketplaceInboxPresentation.Empty()
    } else {
        MarketplaceInboxPresentation.Content(state.threads.map(::marketplaceInboxRowPresentation))
    }

    is MessagingInboxUiState.Failed -> MarketplaceInboxPresentation.Error(
        title = when (state.kind) {
            MessagingFailureKind.Offline -> "You're offline"
            MessagingFailureKind.Server -> "Messages are unavailable"
            MessagingFailureKind.Unauthorized -> "Messages are unavailable"
            MessagingFailureKind.Validation -> "Messages need attention"
        },
        message = when (state.kind) {
            MessagingFailureKind.Offline -> "Reconnect and try loading your inbox again."
            MessagingFailureKind.Server -> "The inbox could not be loaded right now."
            MessagingFailureKind.Unauthorized -> "This inbox is no longer available to this account."
            MessagingFailureKind.Validation -> "The inbox could not be loaded. Try again."
        },
        actionLabel = "Try again",
    )
}

internal fun marketplaceInboxRowPresentation(summary: MessageThreadSummary): MarketplaceInboxRowPresentation =
    MarketplaceInboxRowPresentation(
        conversationId = summary.conversationId,
        title = summary.eventTitle,
        artwork = ShopperEventArtworkPresentation(
            eventId = summary.conversationId,
            photoReference = summary.eventPhoto?.url,
        ),
        preview = summary.lastMessagePreview,
        lastActivityLabel = summary.lastMessageAtEpochMillis?.let(::marketplaceMessageTimeLabel),
        unreadLabel = summary.unreadCount.takeIf { it > 0 }?.let { count ->
            if (count == 1) "1 unread message" else "$count unread messages"
        },
    )

internal fun marketplaceThreadPresentation(
    presentation: MessagingThreadPresentation,
    currentActorId: String,
    canUseSafetyActions: Boolean = true,
): MarketplaceThreadPresentation {
    val access = presentation.composerAccess
    val isClosed = access as? MessagingComposerAccess.Closed
    val canRetry = presentation.canCompose
    return MarketplaceThreadPresentation(
        eventTitle = presentation.eventTitle,
        artwork = ShopperEventArtworkPresentation(
            eventId = presentation.conversationId,
            photoReference = presentation.eventPhoto?.url,
        ),
        messages = presentation.messages
            .sortedWith(compareBy<MarketplaceMessage> { it.sentAtEpochMillis }.thenBy { it.id })
            .map { message -> message.toBubblePresentation(currentActorId, canRetry, isClosed != null) },
        composer = MarketplaceComposerPresentation(
            isVisible = isClosed == null,
            isEnabled = presentation.canCompose,
        ),
        closedBanner = isClosed?.reason?.toClosedBannerPresentation(),
        showReportAction = canUseSafetyActions,
        showBlockAction = canUseSafetyActions,
    )
}

private fun MarketplaceMessage.toBubblePresentation(
    currentActorId: String,
    canRetry: Boolean,
    isConversationClosed: Boolean,
): MarketplaceMessageBubblePresentation {
    val outgoing = senderId == currentActorId
    val failed = outgoing && deliveryState == MessageDeliveryState.FAILED
    val showsRetry = failed && canRetry
    val isClosed = failed && isConversationClosed
    return MarketplaceMessageBubblePresentation(
        id = id,
        body = body,
        isOutgoing = outgoing,
        timeLabel = marketplaceMessageTimeLabel(sentAtEpochMillis),
        deliveryLabel = when {
            showsRetry -> "Delivery failed. Retry"
            isClosed -> "Delivery failed. Messaging is closed."
            failed -> "Delivery failed"
            outgoing -> "Sent"
            else -> null
        },
        deliveryTone = when {
            failed -> MarketplaceMessageDeliveryTone.Error
            outgoing -> MarketplaceMessageDeliveryTone.Normal
            else -> null
        },
        showsRetry = showsRetry,
        isRetryEnabled = showsRetry,
    )
}

internal fun MessagingClosedReason.toClosedBannerPresentation(): MarketplaceClosedBannerPresentation = when (this) {
    MessagingClosedReason.CONVERSATION_UNAVAILABLE,
    MessagingClosedReason.NOT_PARTICIPANT,
    MessagingClosedReason.NOT_EVENT_HOST,
    -> MarketplaceClosedBannerPresentation(
        title = "Conversation unavailable",
        message = "This conversation is no longer available to this account.",
    )

    MessagingClosedReason.RSVP_REQUIRED -> MarketplaceClosedBannerPresentation(
        title = "RSVP required",
        message = "Messaging opens after an active RSVP is accepted.",
    )

    MessagingClosedReason.RSVP_NOT_ACCEPTED -> MarketplaceClosedBannerPresentation(
        title = "RSVP not accepted",
        message = "Messaging is available only for an accepted RSVP.",
    )

    MessagingClosedReason.LOCATION_ACCESS_INACTIVE -> MarketplaceClosedBannerPresentation(
        title = "Location access is inactive",
        message = "Messaging is closed because this RSVP no longer has active access.",
    )

    MessagingClosedReason.LOCATION_ACCESS_REVOKED -> MarketplaceClosedBannerPresentation(
        title = "Location access revoked",
        message = "The host revoked access for this RSVP, so messaging is closed.",
    )

    MessagingClosedReason.LOCATION_ACCESS_EXPIRED -> MarketplaceClosedBannerPresentation(
        title = "Location access expired",
        message = "Messaging closed when this sale's access window ended.",
    )

    MessagingClosedReason.BLOCKED -> MarketplaceClosedBannerPresentation(
        title = "Host blocked",
        message = "You blocked this host, so messaging is closed.",
    )

    MessagingClosedReason.EVENT_NOT_PUBLISHED -> MarketplaceClosedBannerPresentation(
        title = "Sale unavailable",
        message = "Messaging is closed while this sale is not published.",
    )

    MessagingClosedReason.EVENT_CANCELLED -> MarketplaceClosedBannerPresentation(
        title = "This sale was cancelled",
        message = "Messaging is closed for cancelled sales.",
    )

    MessagingClosedReason.EVENT_COMPLETED -> MarketplaceClosedBannerPresentation(
        title = "This sale is complete",
        message = "Messaging closed after this sale was completed.",
    )

    MessagingClosedReason.EVENT_HIDDEN -> MarketplaceClosedBannerPresentation(
        title = "Sale unavailable",
        message = "Messaging is closed while this sale is hidden.",
    )

    MessagingClosedReason.EVENT_ENDED -> MarketplaceClosedBannerPresentation(
        title = "Sale window ended",
        message = "Messaging closed when the sale window ended.",
    )
}

private fun marketplaceMessageTimeLabel(epochMillis: Long): String {
    val minutes = ((epochMillis / 60_000L) % (24L * 60L)).toInt()
    val hour = minutes / 60
    val minute = minutes % 60
    return "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
}

package com.naslabs.yardscape.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.naslabs.yardscape.domain.MessagingActor
import com.naslabs.yardscape.domain.UserRole
import kotlinx.coroutines.launch

@Composable
fun MessagesScreen(
    inboxState: MessagingInboxUiState,
    threadState: MessagingThreadUiState,
    isThreadRoute: Boolean,
    pendingAuthorizationSignal: Long,
    hasPendingAuthorization: Boolean,
    actor: MessagingActor,
    onLoadInbox: suspend () -> Boolean,
    onResumePendingThread: suspend () -> Boolean,
    onOpenThread: suspend (String) -> Boolean,
    onMarkRead: suspend () -> Boolean,
    onDraftChanged: (String) -> Unit,
    onSend: suspend () -> Boolean,
    onRetry: suspend (String) -> Boolean,
    onOpenEvent: (String) -> Unit,
    onReport: () -> Unit,
    onBlock: () -> Unit,
    onBack: () -> Unit,
    onBrowse: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val launchAction: (suspend () -> Boolean) -> Unit = { action -> scope.launch { action() } }
    val lifecycle = remember(onLoadInbox, onResumePendingThread) {
        MarketplaceMessagingLifecycle(onLoadInbox, onResumePendingThread)
    }
    LaunchedEffect(pendingAuthorizationSignal) {
        lifecycle.handleAuthorizationSignal(
            signal = pendingAuthorizationSignal,
            hasPendingAuthorization = hasPendingAuthorization,
            inboxState = inboxState,
        )
    }
    val loadedConversationId = (threadState as? MessagingThreadUiState.Loaded)?.presentation?.conversationId
    LaunchedEffect(isThreadRoute, loadedConversationId) {
        if (isThreadRoute && loadedConversationId != null) onMarkRead()
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().testTag(YardScapeTestTags.MessagesScreen),
        contentAlignment = Alignment.TopCenter,
    ) {
        val layout = marketplaceMessagingLayoutFor(maxWidth)
        val contentWidth = marketplaceMessagingContentWidthFor(
            availableWidth = maxWidth,
            maximumWidth = when {
                layout == MarketplaceMessagingLayout.Compact -> maxWidth
                isThreadRoute -> 960.dp
                else -> 1_080.dp
            },
        )
        if (isThreadRoute) {
            MessageThreadContent(
                state = threadState,
                actor = actor,
                contentWidth = contentWidth,
                onDraftChanged = onDraftChanged,
                onSend = onSend,
                onRetry = onRetry,
                onOpenEvent = onOpenEvent,
                onReport = onReport,
                onBlock = onBlock,
                onBack = onBack,
                launchAction = launchAction,
            )
        } else {
            MessagingInboxContent(
                state = inboxState,
                contentWidth = contentWidth,
                onOpenThread = onOpenThread,
                onRetry = onLoadInbox,
                launchAction = launchAction,
                onBrowse = onBrowse,
            )
        }
    }
}

@Composable
private fun MessagingInboxContent(
    state: MessagingInboxUiState,
    contentWidth: Dp,
    onOpenThread: suspend (String) -> Boolean,
    onRetry: suspend () -> Boolean,
    launchAction: (suspend () -> Boolean) -> Unit,
    onBrowse: () -> Unit,
) {
    val spacing = YardScapeDesign.spacing
    val presentation = marketplaceInboxPresentation(state)
    LazyColumn(
        modifier = Modifier.width(contentWidth)
            .fillMaxHeight()
            .padding(horizontal = spacing.large),
        verticalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        item {
            Column(modifier = Modifier.padding(top = spacing.large, bottom = spacing.medium)) {
                Text("Messages", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Event conversations stay available only while RSVP access is active.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        when (presentation) {
            MarketplaceInboxPresentation.Loading -> item { MessagingLoadingPanel("Loading messages") }
            is MarketplaceInboxPresentation.Empty -> item {
                ShopperStatePanel(
                    title = "No conversations yet",
                    message = "When an accepted RSVP opens a conversation, it will appear here.",
                    actionLabel = presentation.actionLabel,
                    onAction = onBrowse,
                )
            }
            is MarketplaceInboxPresentation.Error -> item {
                ShopperStatePanel(
                    title = presentation.title,
                    message = presentation.message,
                    actionLabel = presentation.actionLabel,
                    onAction = { launchAction(onRetry) },
                )
            }
            is MarketplaceInboxPresentation.Content -> items(presentation.rows, key = { it.conversationId }) { row ->
                InboxRow(row) { launchAction { onOpenThread(row.conversationId) } }
            }
        }
    }
}

@Composable
private fun InboxRow(row: MarketplaceInboxRowPresentation, onOpen: () -> Unit) {
    val spacing = YardScapeDesign.spacing
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(min = row.minimumHeight).clickable(onClick = onOpen)
            .semantics { contentDescription = row.contentDescription },
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(Modifier.padding(spacing.small), verticalAlignment = Alignment.CenterVertically) {
            ShopperEventArtwork(row.artwork, Modifier.width(96.dp), 80.dp)
            Spacer(Modifier.width(spacing.medium))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)) {
                Text(row.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                row.preview?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                row.lastActivityLabel?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            row.unreadLabel?.let { unread ->
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.extraLarge) {
                    Text(unread.substringBefore(' '), Modifier.padding(horizontal = spacing.small, vertical = spacing.extraSmall))
                }
            }
        }
    }
}

@Composable
private fun MessageThreadContent(
    state: MessagingThreadUiState,
    actor: MessagingActor,
    contentWidth: Dp,
    onDraftChanged: (String) -> Unit,
    onSend: suspend () -> Boolean,
    onRetry: suspend (String) -> Boolean,
    onOpenEvent: (String) -> Unit,
    onReport: () -> Unit,
    onBlock: () -> Unit,
    onBack: () -> Unit,
    launchAction: (suspend () -> Boolean) -> Unit,
) = when (state) {
    MessagingThreadUiState.Idle, is MessagingThreadUiState.Loading -> MessageThreadPanel { MessagingLoadingPanel("Opening conversation") }
    is MessagingThreadUiState.Failed -> MessageThreadPanel {
        ShopperStatePanel("Conversation unavailable", "This conversation could not be opened.", "Back to messages", onBack)
    }
    is MessagingThreadUiState.Loaded -> MessageThreadLoadedContent(
        state.presentation,
        marketplaceThreadPresentation(state.presentation, actor.userId, actor.role == UserRole.SHOPPER),
        contentWidth, onDraftChanged, onSend, onRetry, onOpenEvent, onReport, onBlock, onBack, launchAction,
    )
}

@Composable
private fun MessageThreadLoadedContent(
    state: MessagingThreadPresentation,
    presentation: MarketplaceThreadPresentation,
    contentWidth: Dp,
    onDraftChanged: (String) -> Unit,
    onSend: suspend () -> Boolean,
    onRetry: suspend (String) -> Boolean,
    onOpenEvent: (String) -> Unit,
    onReport: () -> Unit,
    onBlock: () -> Unit,
    onBack: () -> Unit,
    launchAction: (suspend () -> Boolean) -> Unit,
) {
    val spacing = YardScapeDesign.spacing
    LazyColumn(
        modifier = Modifier.width(contentWidth)
            .fillMaxHeight()
            .padding(horizontal = spacing.large),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
    ) {
        item {
            Column(Modifier.padding(top = spacing.medium)) {
                TextButton(onBack, Modifier.heightIn(min = 48.dp)) { Text("Back to messages") }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ShopperEventArtwork(presentation.artwork, Modifier.width(96.dp), 72.dp)
                    Spacer(Modifier.width(spacing.medium))
                    Column(Modifier.weight(1f)) {
                        Text(presentation.eventTitle, style = MaterialTheme.typography.headlineSmall)
                        Text("Event conversation", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
                    TextButton({ onOpenEvent(state.thread.conversationKey.eventId) }, Modifier.heightIn(min = 48.dp)) { Text("View sale") }
                    if (presentation.showReportAction) TextButton(onReport, Modifier.heightIn(min = 48.dp)) { Text("Report") }
                    if (presentation.showBlockAction) TextButton(onBlock, Modifier.heightIn(min = 48.dp)) { Text("Block") }
                }
            }
        }
        presentation.closedBanner?.let { item { ShopperStatePanel(it.title, it.message) } }
        items(presentation.messages, key = { it.id }) { message -> MessageBubble(message) { launchAction { onRetry(message.id) } } }
        if (presentation.composer.isVisible) item { Composer(state.draft, presentation.composer.isEnabled, onDraftChanged) { launchAction(onSend) } }
        (state.operation as? MessagingOperationState.Failed)?.let { failure ->
            item {
                Text(
                    when (failure.kind) {
                        MessagingFailureKind.Offline -> "Message not sent. Reconnect and retry."
                        MessagingFailureKind.Server -> "Message not sent. Try again."
                        MessagingFailureKind.Validation -> "Check your message and try again."
                        MessagingFailureKind.Unauthorized -> "Messaging access is no longer available."
                    },
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(message: MarketplaceMessageBubblePresentation, onRetry: () -> Unit) {
    val spacing = YardScapeDesign.spacing
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.isOutgoing) Arrangement.End else Arrangement.Start) {
        Surface(
            modifier = Modifier.widthIn(max = 520.dp),
            color = if (message.isOutgoing) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(Modifier.padding(spacing.medium), verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)) {
                Text(message.body)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(message.timeLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    message.deliveryLabel?.let {
                        Text(
                            it,
                            modifier = Modifier.padding(start = spacing.small),
                            style = MaterialTheme.typography.labelSmall,
                            color = when (message.deliveryTone) {
                                MarketplaceMessageDeliveryTone.Error -> MaterialTheme.colorScheme.error
                                MarketplaceMessageDeliveryTone.Normal,
                                null,
                                -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                    if (message.showsRetry) TextButton(onRetry, Modifier.heightIn(min = 48.dp)) { Text("Retry") }
                }
            }
        }
    }
}

@Composable
private fun Composer(draft: String, isEnabled: Boolean, onDraftChanged: (String) -> Unit, onSend: () -> Unit) {
    val spacing = YardScapeDesign.spacing
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Column(Modifier.padding(vertical = spacing.small), verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        OutlinedTextField(
            value = draft,
            onValueChange = onDraftChanged,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics { contentDescription = "Message composer" },
            enabled = isEnabled,
            label = { Text("Write a message") },
            minLines = 1,
            maxLines = 4,
        )
        Button(
            onClick = onSend,
            enabled = isEnabled && draft.isNotBlank(),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics { contentDescription = "Send message" },
        ) { Text("Send message") }
    }
}

@Composable
private fun MessageThreadPanel(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().padding(YardScapeDesign.spacing.large), contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun MessagingLoadingPanel(label: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.small), verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(Modifier.width(24.dp))
        Text(label)
    }
}

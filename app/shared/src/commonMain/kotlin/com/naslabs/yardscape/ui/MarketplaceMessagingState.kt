package com.naslabs.yardscape.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.naslabs.yardscape.data.MarketplaceMessagingRepository
import com.naslabs.yardscape.data.MessagingRepositoryResult
import com.naslabs.yardscape.domain.EventPhoto
import com.naslabs.yardscape.domain.MarketplaceConversationKey
import com.naslabs.yardscape.domain.MarketplaceMessage
import com.naslabs.yardscape.domain.MarketplaceMessageThread
import com.naslabs.yardscape.domain.MessageThreadSummary
import com.naslabs.yardscape.domain.MessagingActor
import com.naslabs.yardscape.domain.MessagingClosedReason
import com.naslabs.yardscape.domain.MessagingComposerAccess
import com.naslabs.yardscape.domain.withPrivacySafeAccess

enum class MessagingFailureKind {
    Validation,
    Unauthorized,
    Offline,
    Server,
}

sealed interface MessagingOperationState {
    data object Idle : MessagingOperationState
    data class InProgress(val messageId: String? = null) : MessagingOperationState {
        override fun toString(): String = "MessagingOperationState.InProgress(hasMessageId=${messageId != null})"
    }
    data class Completed(val messageId: String? = null) : MessagingOperationState {
        override fun toString(): String = "MessagingOperationState.Completed(hasMessageId=${messageId != null})"
    }
    data class Failed(val kind: MessagingFailureKind, val message: String) : MessagingOperationState {
        override fun toString(): String =
            "MessagingOperationState.Failed(kind=$kind, messageLength=${message.length})"
    }
}

sealed interface MessagingInboxUiState {
    data object Idle : MessagingInboxUiState
    data object Loading : MessagingInboxUiState

    data class Loaded(val threads: List<MessageThreadSummary>) : MessagingInboxUiState {
        override fun toString(): String = "MessagingInboxUiState.Loaded(threadCount=${threads.size})"
    }

    data class Failed(
        val kind: MessagingFailureKind,
        val message: String,
    ) : MessagingInboxUiState {
        override fun toString(): String =
            "MessagingInboxUiState.Failed(kind=$kind, messageLength=${message.length})"
    }
}

sealed interface MessagingThreadUiState {
    data object Idle : MessagingThreadUiState

    data class Loading(val conversationId: String) : MessagingThreadUiState {
        override fun toString(): String = "MessagingThreadUiState.Loading"
    }

    data class Loaded(val presentation: MessagingThreadPresentation) : MessagingThreadUiState {
        override fun toString(): String = "MessagingThreadUiState.Loaded($presentation)"
    }

    data class Failed(
        val conversationId: String,
        val kind: MessagingFailureKind,
        val message: String,
    ) : MessagingThreadUiState {
        override fun toString(): String =
            "MessagingThreadUiState.Failed(kind=$kind, messageLength=${message.length})"
    }
}

data class MessagingThreadPresentation(
    val thread: MarketplaceMessageThread,
    val draft: String = "",
    val operation: MessagingOperationState = MessagingOperationState.Idle,
) {
    val conversationId: String
        get() = thread.conversationId
    val eventTitle: String
        get() = thread.eventTitle
    val eventPhoto: EventPhoto?
        get() = thread.eventPhoto
    val messages: List<MarketplaceMessage>
        get() = thread.withPrivacySafeAccess(composerAccess).messages
    val composerAccess: MessagingComposerAccess
        get() = thread.composerAccess
    val canCompose: Boolean
        get() = composerAccess is MessagingComposerAccess.Open && operation !is MessagingOperationState.InProgress
    val closedReason: MessagingClosedReason?
        get() = (composerAccess as? MessagingComposerAccess.Closed)?.reason

    override fun toString(): String =
        "MessagingThreadPresentation(messageCount=${messages.size}, draftLength=${draft.length}, " +
            "composerAccess=$composerAccess, " +
            "operation=${operation.diagnosticName})"
}

class MarketplaceMessagingState(
    private val repository: MarketplaceMessagingRepository,
    private val actorSource: () -> MessagingActor,
    private val composerAccessFor: (MarketplaceConversationKey, MessagingActor) -> MessagingComposerAccess,
    private val sessionVersionSource: () -> Long = { 0L },
) {
    var inboxState: MessagingInboxUiState by mutableStateOf<MessagingInboxUiState>(MessagingInboxUiState.Idle)
        private set

    var threadState: MessagingThreadUiState by mutableStateOf<MessagingThreadUiState>(MessagingThreadUiState.Idle)
        private set

    private var invalidationGeneration: Long = 0L
    private var nextRequestIdentifier: Long = 0L
    private var latestInboxRequestIdentifier: Long = 0L
    private var latestThreadRequestIdentifier: Long = 0L

    suspend fun loadInbox(): Boolean {
        val request = beginRequest()
        inboxState = MessagingInboxUiState.Loading
        val result = repository.inboxFor(request.actor)
        if (!isCurrent(request)) return false
        return when (result) {
            is MessagingRepositoryResult.Success -> {
                inboxState = MessagingInboxUiState.Loaded(
                    result.value.map { it.withCurrentAccess(request.actor) },
                )
                true
            }
            else -> {
                inboxState = result.toInboxFailure()
                false
            }
        }
    }

    suspend fun openThread(conversationId: String): Boolean {
        val request = beginRequest(conversationId)
        threadState = MessagingThreadUiState.Loading(conversationId)
        val summary = currentSummary(conversationId, request)
        if (!isCurrent(request)) return false
        if (summary == null) {
            threadState = MessagingThreadUiState.Failed(
                conversationId,
                MessagingFailureKind.Unauthorized,
                "This conversation is unavailable.",
            )
            return false
        }
        val result = repository.threadFor(summary.conversationKey, request.actor)
        if (!isCurrent(request)) return false
        return when (result) {
            is MessagingRepositoryResult.Success -> {
                if (result.value.conversationKey != summary.conversationKey ||
                    result.value.conversationId != conversationId
                ) {
                    narrowAccess(summary.conversationKey, MessagingClosedReason.CONVERSATION_UNAVAILABLE)
                    threadState = MessagingThreadUiState.Failed(
                        conversationId,
                        MessagingFailureKind.Unauthorized,
                        "This conversation is unavailable.",
                    )
                    return false
                }
                val access = result.value.composerAccess
                    .narrowedBy(summary.composerAccess)
                    .narrowedBy(composerAccessFor(result.value.conversationKey, request.actor))
                val thread = result.value.withPrivacySafeAccess(
                    access = access,
                )
                if (!thread.isVisibleToParticipant()) {
                    narrowAccess(thread.conversationKey, thread.closedReasonOrUnavailable())
                    threadState = MessagingThreadUiState.Failed(
                        conversationId,
                        MessagingFailureKind.Unauthorized,
                        "This conversation is unavailable.",
                    )
                    return false
                }
                threadState = MessagingThreadUiState.Loaded(
                    MessagingThreadPresentation(thread),
                )
                true
            }
            is MessagingRepositoryResult.Unauthorized -> {
                narrowAccess(summary.conversationKey, result.reason)
                threadState = result.toThreadFailure(conversationId)
                false
            }
            else -> {
                threadState = result.toThreadFailure(conversationId)
                false
            }
        }
    }

    /**
     * Displays a thread whose opaque identity and access were just resolved by the repository.
     * This intentionally accepts a complete repository thread rather than an event/shopper key so
     * callers cannot build a route from participant identifiers.
     */
    fun openAuthorizedThread(thread: MarketplaceMessageThread): Boolean {
        val actor = actorSource()
        if (thread.composerAccess !is MessagingComposerAccess.Open ||
            composerAccessFor(thread.conversationKey, actor) !is MessagingComposerAccess.Open
        ) return false
        beginRequest(thread.conversationId)
        threadState = MessagingThreadUiState.Loaded(MessagingThreadPresentation(thread))
        return true
    }

    fun updateDraft(draft: String) {
        val loaded = threadState as? MessagingThreadUiState.Loaded ?: return
        if (!loaded.presentation.canCompose) return
        threadState = MessagingThreadUiState.Loaded(
            loaded.presentation.copy(draft = draft, operation = MessagingOperationState.Idle),
        )
    }

    suspend fun markCurrentThreadRead(): Boolean {
        val loaded = threadState as? MessagingThreadUiState.Loaded ?: return false
        if (loaded.presentation.operation is MessagingOperationState.InProgress) return false
        val key = loaded.presentation.thread.conversationKey
        val request = beginRequest(loaded.presentation.conversationId)
        val result = repository.markRead(key, request.actor)
        if (!isCurrent(request, key)) return false
        return when (result) {
            is MessagingRepositoryResult.Success -> {
                val current = (threadState as? MessagingThreadUiState.Loaded)?.presentation ?: return false
                threadState = MessagingThreadUiState.Loaded(
                    current.copy(operation = MessagingOperationState.Completed()),
                )
                refreshInbox(request, key)
                true
            }
            is MessagingRepositoryResult.Unauthorized -> {
                narrowAccess(key, result.reason, result.toOperationFailure())
                false
            }
            else -> {
                val currentDraft = (threadState as? MessagingThreadUiState.Loaded)?.presentation?.draft.orEmpty()
                restorePresentation(request, key, currentDraft, result.toOperationFailure())
                false
            }
        }
    }

    suspend fun sendDraft(sentAtEpochMillis: Long): Boolean {
        val loaded = threadState as? MessagingThreadUiState.Loaded ?: return false
        if (!loaded.presentation.canCompose) return false
        val key = loaded.presentation.thread.conversationKey
        val draft = loaded.presentation.draft
        val request = beginRequest(loaded.presentation.conversationId)
        threadState = MessagingThreadUiState.Loaded(
            loaded.presentation.copy(operation = MessagingOperationState.InProgress()),
        )
        val result = repository.sendMessage(key, request.actor, draft, sentAtEpochMillis)
        if (!isCurrent(request, key)) return false
        return when (result) {
            is MessagingRepositoryResult.Success -> {
                refreshThread(
                    request = request,
                    key = key,
                    draft = "",
                    operation = MessagingOperationState.Completed(result.value.id),
                )
                if (!isCurrent(request, key)) return false
                refreshInbox(request, key)
                isCurrent(request, key)
            }
            is MessagingRepositoryResult.ValidationFailure -> {
                restorePresentation(request, key, draft, result.toOperationFailure())
                false
            }
            is MessagingRepositoryResult.Offline,
            is MessagingRepositoryResult.ServerError,
            -> {
                refreshThread(request, key, draft = "", operation = result.toOperationFailure())
                if (isCurrent(request, key)) refreshInbox(request, key)
                false
            }
            is MessagingRepositoryResult.Unauthorized -> {
                narrowAccess(key, result.reason, result.toOperationFailure())
                false
            }
        }
    }

    suspend fun retryMessage(messageId: String, attemptedAtEpochMillis: Long): Boolean {
        val loaded = threadState as? MessagingThreadUiState.Loaded ?: return false
        if (!loaded.presentation.canCompose) return false
        val key = loaded.presentation.thread.conversationKey
        val request = beginRequest(loaded.presentation.conversationId)
        threadState = MessagingThreadUiState.Loaded(
            loaded.presentation.copy(operation = MessagingOperationState.InProgress(messageId)),
        )
        val result = repository.retryMessage(messageId, request.actor, attemptedAtEpochMillis)
        if (!isCurrent(request, key)) return false
        return when (result) {
            is MessagingRepositoryResult.Success -> {
                refreshThread(
                    request = request,
                    key = key,
                    draft = loaded.presentation.draft,
                    operation = MessagingOperationState.Completed(messageId),
                )
                if (!isCurrent(request, key)) return false
                refreshInbox(request, key)
                isCurrent(request, key)
            }
            is MessagingRepositoryResult.Unauthorized -> {
                narrowAccess(key, result.reason, result.toOperationFailure())
                false
            }
            else -> {
                refreshThread(
                    request = request,
                    key = key,
                    draft = loaded.presentation.draft,
                    operation = result.toOperationFailure(),
                )
                if (isCurrent(request, key)) refreshInbox(request, key)
                false
            }
        }
    }

    fun synchronizeComposerAccess() {
        val actor = actorSource()
        val loadedBefore = threadState as? MessagingThreadUiState.Loaded
        val nextThreadAccess = loadedBefore?.presentation?.thread?.let { thread ->
            thread.composerAccess.narrowedBy(composerAccessFor(thread.conversationKey, actor))
        }
        if (nextThreadAccess is MessagingComposerAccess.Closed) invalidatePendingWork()
        val inbox = inboxState as? MessagingInboxUiState.Loaded
        if (inbox != null) {
            inboxState = MessagingInboxUiState.Loaded(
                inbox.threads.map { it.withCurrentAccess(actor) },
            )
        }
        val loaded = threadState as? MessagingThreadUiState.Loaded ?: return
        val access = requireNotNull(nextThreadAccess)
        val draft = if (access is MessagingComposerAccess.Open) loaded.presentation.draft else ""
        threadState = MessagingThreadUiState.Loaded(
            loaded.presentation.copy(
                thread = loaded.presentation.thread.withPrivacySafeAccess(access),
                draft = draft,
                operation = if (access is MessagingComposerAccess.Open) {
                    loaded.presentation.operation
                } else {
                    MessagingOperationState.Idle
                },
            ),
        )
    }

    fun closeAndClear(reason: MessagingClosedReason) {
        invalidatePendingWork()
        val inbox = inboxState as? MessagingInboxUiState.Loaded
        if (inbox != null) {
            inboxState = MessagingInboxUiState.Loaded(
                inbox.threads.map { summary ->
                    summary.copy(composerAccess = MessagingComposerAccess.Closed(reason))
                },
            )
        }
        if (threadState is MessagingThreadUiState.Loaded) {
            closeComposer(reason, MessagingOperationState.Idle)
        } else {
            threadState = MessagingThreadUiState.Idle
        }
    }

    fun invalidatePendingWork() {
        invalidationGeneration++
        when (val current = threadState) {
            is MessagingThreadUiState.Loading -> threadState = MessagingThreadUiState.Idle
            is MessagingThreadUiState.Loaded -> {
                if (current.presentation.operation is MessagingOperationState.InProgress) {
                    threadState = MessagingThreadUiState.Loaded(
                        current.presentation.copy(draft = "", operation = MessagingOperationState.Idle),
                    )
                }
            }
            else -> Unit
        }
    }

    fun resetForNewSession() {
        invalidatePendingWork()
        inboxState = MessagingInboxUiState.Idle
        threadState = MessagingThreadUiState.Idle
    }

    private suspend fun currentSummary(
        conversationId: String,
        request: MessagingRequest,
    ): MessageThreadSummary? {
        (inboxState as? MessagingInboxUiState.Loaded)
            ?.threads
            ?.firstOrNull { it.conversationId == conversationId }
            ?.let { return it }
        val result = repository.inboxFor(request.actor)
        if (!isCurrent(request)) return null
        return when (result) {
            is MessagingRepositoryResult.Success -> {
                val current = result.value.map { it.withCurrentAccess(request.actor) }
                if (canWriteInbox(request)) inboxState = MessagingInboxUiState.Loaded(current)
                current.firstOrNull { it.conversationId == conversationId }
            }
            else -> {
                inboxState = result.toInboxFailure()
                null
            }
        }
    }

    private suspend fun refreshThread(
        request: MessagingRequest,
        key: MarketplaceConversationKey,
        draft: String,
        operation: MessagingOperationState,
    ) {
        val refreshed = repository.threadFor(key, request.actor)
        if (!isCurrent(request, key)) return
        when (refreshed) {
            is MessagingRepositoryResult.Success -> {
                if (refreshed.value.conversationKey != key ||
                    refreshed.value.conversationId != request.conversationId
                ) {
                    narrowAccess(key, MessagingClosedReason.CONVERSATION_UNAVAILABLE, operation)
                    return
                }
                val thread = refreshed.value.withCurrentAccess(request.actor)
                threadState = MessagingThreadUiState.Loaded(
                    MessagingThreadPresentation(
                        thread = thread,
                        draft = if (thread.composerAccess is MessagingComposerAccess.Open) draft else "",
                        operation = operation,
                    ),
                )
            }
            is MessagingRepositoryResult.Unauthorized ->
                narrowAccess(key, refreshed.reason, refreshed.toOperationFailure())
            else -> {
                val conversationId = (threadState as? MessagingThreadUiState.Loaded)
                    ?.presentation
                    ?.conversationId
                    .orEmpty()
                threadState = refreshed.toThreadFailure(conversationId)
            }
        }
    }

    private suspend fun refreshInbox(request: MessagingRequest, key: MarketplaceConversationKey) {
        val result = repository.inboxFor(request.actor)
        if (!isCurrent(request) || !canWriteInbox(request)) return
        when (result) {
            is MessagingRepositoryResult.Success -> inboxState = MessagingInboxUiState.Loaded(
                result.value.map { it.withCurrentAccess(request.actor) },
            )
            is MessagingRepositoryResult.Unauthorized ->
                narrowAccess(key, result.reason, result.toOperationFailure())
            else -> inboxState = result.toInboxFailure()
        }
    }

    private fun restorePresentation(
        request: MessagingRequest,
        key: MarketplaceConversationKey,
        draft: String,
        operation: MessagingOperationState,
    ) {
        if (!isCurrent(request, key)) return
        val current = threadState as? MessagingThreadUiState.Loaded ?: return
        threadState = MessagingThreadUiState.Loaded(current.presentation.copy(draft = draft, operation = operation))
    }

    private fun closeComposer(reason: MessagingClosedReason, operation: MessagingOperationState) {
        val loaded = threadState as? MessagingThreadUiState.Loaded ?: return
        threadState = MessagingThreadUiState.Loaded(
            loaded.presentation.copy(
                thread = loaded.presentation.thread.withPrivacySafeAccess(
                    MessagingComposerAccess.Closed(reason),
                ),
                draft = "",
                operation = operation,
            ),
        )
    }

    private fun narrowAccess(
        key: MarketplaceConversationKey,
        reason: MessagingClosedReason,
        operation: MessagingOperationState = MessagingOperationState.Idle,
    ) {
        val access = MessagingComposerAccess.Closed(reason)
        val inbox = inboxState as? MessagingInboxUiState.Loaded
        if (inbox != null) {
            inboxState = MessagingInboxUiState.Loaded(
                inbox.threads.map { summary ->
                    if (summary.conversationKey == key) summary.copy(composerAccess = access) else summary
                },
            )
        }
        val loaded = threadState as? MessagingThreadUiState.Loaded ?: return
        if (loaded.presentation.thread.conversationKey != key) return
        closeComposer(reason, operation)
    }

    private fun MarketplaceMessageThread.withCurrentAccess(actor: MessagingActor): MarketplaceMessageThread =
        withPrivacySafeAccess(composerAccess.narrowedBy(composerAccessFor(conversationKey, actor)))

    private fun MessageThreadSummary.withCurrentAccess(actor: MessagingActor): MessageThreadSummary =
        copy(composerAccess = composerAccess.narrowedBy(composerAccessFor(conversationKey, actor)))

    private fun MessagingComposerAccess.narrowedBy(local: MessagingComposerAccess): MessagingComposerAccess =
        when {
            this is MessagingComposerAccess.Closed -> this
            local is MessagingComposerAccess.Closed -> local
            else -> MessagingComposerAccess.Open
        }

    private fun MarketplaceMessageThread.isVisibleToParticipant(): Boolean =
        (composerAccess as? MessagingComposerAccess.Closed)?.reason !in setOf(
            MessagingClosedReason.CONVERSATION_UNAVAILABLE,
            MessagingClosedReason.NOT_PARTICIPANT,
            MessagingClosedReason.NOT_EVENT_HOST,
        )

    private fun MarketplaceMessageThread.closedReasonOrUnavailable(): MessagingClosedReason =
        (composerAccess as? MessagingComposerAccess.Closed)?.reason
            ?: MessagingClosedReason.CONVERSATION_UNAVAILABLE

    private fun beginRequest(conversationId: String? = null): MessagingRequest {
        val requestIdentifier = ++nextRequestIdentifier
        latestInboxRequestIdentifier = requestIdentifier
        if (conversationId != null) {
            latestThreadRequestIdentifier = requestIdentifier
        }
        return MessagingRequest(
            invalidationGeneration = invalidationGeneration,
            requestIdentifier = requestIdentifier,
            actor = actorSource(),
            sessionVersion = sessionVersionSource(),
            conversationId = conversationId,
        )
    }

    private fun isCurrent(request: MessagingRequest, key: MarketplaceConversationKey? = null): Boolean {
        if (request.invalidationGeneration != invalidationGeneration) return false
        val latestIdentifier = if (request.conversationId == null) {
            latestInboxRequestIdentifier
        } else {
            latestThreadRequestIdentifier
        }
        if (request.requestIdentifier != latestIdentifier) return false
        if (request.actor != actorSource() || request.sessionVersion != sessionVersionSource()) return false
        if (key == null) return true
        val loaded = threadState as? MessagingThreadUiState.Loaded ?: return false
        return loaded.presentation.thread.conversationKey == key &&
            loaded.presentation.conversationId == request.conversationId
    }

    private fun canWriteInbox(request: MessagingRequest): Boolean =
        request.requestIdentifier == latestInboxRequestIdentifier

    private data class MessagingRequest(
        val invalidationGeneration: Long,
        val requestIdentifier: Long,
        val actor: MessagingActor,
        val sessionVersion: Long,
        val conversationId: String?,
    )
}

private fun MessagingRepositoryResult<*>.toInboxFailure(): MessagingInboxUiState.Failed {
    val failure = toFailure()
    return MessagingInboxUiState.Failed(failure.kind, failure.message)
}

private fun MessagingRepositoryResult<*>.toThreadFailure(conversationId: String): MessagingThreadUiState.Failed {
    val failure = toFailure()
    return MessagingThreadUiState.Failed(conversationId, failure.kind, failure.message)
}

private fun MessagingRepositoryResult<*>.toOperationFailure(): MessagingOperationState.Failed {
    val failure = toFailure()
    return MessagingOperationState.Failed(failure.kind, failure.message)
}

private fun MessagingRepositoryResult<*>.toFailure(): MessagingFailure = when (this) {
    is MessagingRepositoryResult.ValidationFailure -> MessagingFailure(
        MessagingFailureKind.Validation,
        messages.joinToString(" "),
    )
    is MessagingRepositoryResult.Unauthorized -> MessagingFailure(MessagingFailureKind.Unauthorized, message)
    is MessagingRepositoryResult.Offline -> MessagingFailure(MessagingFailureKind.Offline, message)
    is MessagingRepositoryResult.ServerError -> MessagingFailure(MessagingFailureKind.Server, message)
    is MessagingRepositoryResult.Success -> error("A successful result is not a failure.")
}

private data class MessagingFailure(val kind: MessagingFailureKind, val message: String)

private val MessagingOperationState.diagnosticName: String
    get() = when (this) {
        MessagingOperationState.Idle -> "Idle"
        is MessagingOperationState.InProgress -> "InProgress"
        is MessagingOperationState.Completed -> "Completed"
        is MessagingOperationState.Failed -> "Failed"
    }

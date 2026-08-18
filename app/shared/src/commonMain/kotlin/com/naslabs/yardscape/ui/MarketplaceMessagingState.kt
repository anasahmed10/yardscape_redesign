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

enum class MessagingFailureKind {
    Validation,
    Unauthorized,
    Offline,
    Server,
}

sealed interface MessagingOperationState {
    data object Idle : MessagingOperationState
    data class InProgress(val messageId: String? = null) : MessagingOperationState
    data class Completed(val messageId: String? = null) : MessagingOperationState
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

    data class Loading(val conversationId: String) : MessagingThreadUiState

    data class Loaded(val presentation: MessagingThreadPresentation) : MessagingThreadUiState {
        override fun toString(): String = "MessagingThreadUiState.Loaded($presentation)"
    }

    data class Failed(
        val conversationId: String,
        val kind: MessagingFailureKind,
        val message: String,
    ) : MessagingThreadUiState {
        override fun toString(): String =
            "MessagingThreadUiState.Failed(conversationId=$conversationId, kind=$kind, " +
                "messageLength=${message.length})"
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
        get() = thread.messages
    val composerAccess: MessagingComposerAccess
        get() = thread.composerAccess
    val canCompose: Boolean
        get() = composerAccess is MessagingComposerAccess.Open
    val closedReason: MessagingClosedReason?
        get() = (composerAccess as? MessagingComposerAccess.Closed)?.reason

    override fun toString(): String =
        "MessagingThreadPresentation(conversationId=$conversationId, eventTitle=$eventTitle, " +
            "messageCount=${messages.size}, draftLength=${draft.length}, composerAccess=$composerAccess, " +
            "operation=${operation.diagnosticName})"
}

class MarketplaceMessagingState(
    private val repository: MarketplaceMessagingRepository,
    private val actorSource: () -> MessagingActor,
    private val composerAccessFor: (MarketplaceConversationKey, MessagingActor) -> MessagingComposerAccess,
) {
    var inboxState: MessagingInboxUiState by mutableStateOf<MessagingInboxUiState>(MessagingInboxUiState.Idle)
        private set

    var threadState: MessagingThreadUiState by mutableStateOf<MessagingThreadUiState>(MessagingThreadUiState.Idle)
        private set

    suspend fun loadInbox(): Boolean {
        inboxState = MessagingInboxUiState.Loading
        val actor = actorSource()
        return when (val result = repository.inboxFor(actor)) {
            is MessagingRepositoryResult.Success -> {
                inboxState = MessagingInboxUiState.Loaded(
                    result.value.map { it.withCurrentAccess(actor) },
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
        threadState = MessagingThreadUiState.Loading(conversationId)
        val actor = actorSource()
        val summary = currentSummary(conversationId, actor)
        if (summary == null) {
            threadState = MessagingThreadUiState.Failed(
                conversationId,
                MessagingFailureKind.Unauthorized,
                "This conversation is unavailable.",
            )
            return false
        }
        return when (val result = repository.threadFor(summary.conversationKey, actor)) {
            is MessagingRepositoryResult.Success -> {
                threadState = MessagingThreadUiState.Loaded(
                    MessagingThreadPresentation(result.value.withCurrentAccess(actor)),
                )
                true
            }
            else -> {
                threadState = result.toThreadFailure(conversationId)
                false
            }
        }
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
        val actor = actorSource()
        return when (val result = repository.markRead(loaded.presentation.thread.conversationKey, actor)) {
            is MessagingRepositoryResult.Success -> {
                threadState = MessagingThreadUiState.Loaded(
                    loaded.presentation.copy(operation = MessagingOperationState.Completed()),
                )
                loadInbox()
                true
            }
            else -> {
                threadState = MessagingThreadUiState.Loaded(
                    loaded.presentation.copy(operation = result.toOperationFailure()),
                )
                false
            }
        }
    }

    suspend fun sendDraft(sentAtEpochMillis: Long): Boolean {
        val loaded = threadState as? MessagingThreadUiState.Loaded ?: return false
        if (!loaded.presentation.canCompose) return false
        val actor = actorSource()
        val key = loaded.presentation.thread.conversationKey
        val draft = loaded.presentation.draft
        threadState = MessagingThreadUiState.Loaded(
            loaded.presentation.copy(operation = MessagingOperationState.InProgress()),
        )
        val result = repository.sendMessage(key, actor, draft, sentAtEpochMillis)
        return when (result) {
            is MessagingRepositoryResult.Success -> {
                refreshThread(
                    key = key,
                    actor = actor,
                    draft = "",
                    operation = MessagingOperationState.Completed(result.value.id),
                )
                loadInbox()
                true
            }
            is MessagingRepositoryResult.ValidationFailure -> {
                restorePresentation(draft, result.toOperationFailure())
                false
            }
            is MessagingRepositoryResult.Offline,
            is MessagingRepositoryResult.ServerError,
            -> {
                refreshThread(key, actor, draft = "", operation = result.toOperationFailure())
                loadInbox()
                false
            }
            is MessagingRepositoryResult.Unauthorized -> {
                closeComposer(result.reason, result.toOperationFailure())
                false
            }
        }
    }

    suspend fun retryMessage(messageId: String, attemptedAtEpochMillis: Long): Boolean {
        val loaded = threadState as? MessagingThreadUiState.Loaded ?: return false
        if (!loaded.presentation.canCompose) return false
        val actor = actorSource()
        val key = loaded.presentation.thread.conversationKey
        threadState = MessagingThreadUiState.Loaded(
            loaded.presentation.copy(operation = MessagingOperationState.InProgress(messageId)),
        )
        val result = repository.retryMessage(messageId, actor, attemptedAtEpochMillis)
        return when (result) {
            is MessagingRepositoryResult.Success -> {
                refreshThread(
                    key = key,
                    actor = actor,
                    draft = loaded.presentation.draft,
                    operation = MessagingOperationState.Completed(messageId),
                )
                loadInbox()
                true
            }
            is MessagingRepositoryResult.Unauthorized -> {
                synchronizeComposerAccess()
                val current = threadState as? MessagingThreadUiState.Loaded
                if (current?.presentation?.canCompose == true) {
                    closeComposer(result.reason, result.toOperationFailure())
                }
                false
            }
            else -> {
                refreshThread(
                    key = key,
                    actor = actor,
                    draft = loaded.presentation.draft,
                    operation = result.toOperationFailure(),
                )
                loadInbox()
                false
            }
        }
    }

    fun synchronizeComposerAccess() {
        val actor = actorSource()
        val inbox = inboxState as? MessagingInboxUiState.Loaded
        if (inbox != null) {
            inboxState = MessagingInboxUiState.Loaded(
                inbox.threads.map { it.withCurrentAccess(actor) },
            )
        }
        val loaded = threadState as? MessagingThreadUiState.Loaded ?: return
        val access = composerAccessFor(loaded.presentation.thread.conversationKey, actor)
        val draft = if (access is MessagingComposerAccess.Open) loaded.presentation.draft else ""
        threadState = MessagingThreadUiState.Loaded(
            loaded.presentation.copy(
                thread = loaded.presentation.thread.copy(composerAccess = access),
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
        val inbox = inboxState as? MessagingInboxUiState.Loaded
        if (inbox != null) {
            inboxState = MessagingInboxUiState.Loaded(
                inbox.threads.map { summary ->
                    summary.copy(composerAccess = MessagingComposerAccess.Closed(reason))
                },
            )
        }
        closeComposer(reason, MessagingOperationState.Idle)
    }

    private suspend fun currentSummary(
        conversationId: String,
        actor: MessagingActor,
    ): MessageThreadSummary? {
        (inboxState as? MessagingInboxUiState.Loaded)
            ?.threads
            ?.firstOrNull { it.conversationId == conversationId }
            ?.let { return it }
        return when (val result = repository.inboxFor(actor)) {
            is MessagingRepositoryResult.Success -> {
                val current = result.value.map { it.withCurrentAccess(actor) }
                inboxState = MessagingInboxUiState.Loaded(current)
                current.firstOrNull { it.conversationId == conversationId }
            }
            else -> {
                inboxState = result.toInboxFailure()
                null
            }
        }
    }

    private suspend fun refreshThread(
        key: MarketplaceConversationKey,
        actor: MessagingActor,
        draft: String,
        operation: MessagingOperationState,
    ) {
        when (val refreshed = repository.threadFor(key, actor)) {
            is MessagingRepositoryResult.Success -> {
                threadState = MessagingThreadUiState.Loaded(
                    MessagingThreadPresentation(
                        thread = refreshed.value.withCurrentAccess(actor),
                        draft = draft,
                        operation = operation,
                    ),
                )
            }
            else -> {
                val conversationId = (threadState as? MessagingThreadUiState.Loaded)
                    ?.presentation
                    ?.conversationId
                    .orEmpty()
                threadState = refreshed.toThreadFailure(conversationId)
            }
        }
    }

    private fun restorePresentation(draft: String, operation: MessagingOperationState) {
        val current = threadState as? MessagingThreadUiState.Loaded ?: return
        threadState = MessagingThreadUiState.Loaded(current.presentation.copy(draft = draft, operation = operation))
    }

    private fun closeComposer(reason: MessagingClosedReason, operation: MessagingOperationState) {
        val loaded = threadState as? MessagingThreadUiState.Loaded ?: return
        threadState = MessagingThreadUiState.Loaded(
            loaded.presentation.copy(
                thread = loaded.presentation.thread.copy(
                    composerAccess = MessagingComposerAccess.Closed(reason),
                ),
                draft = "",
                operation = operation,
            ),
        )
    }

    private fun MarketplaceMessageThread.withCurrentAccess(actor: MessagingActor): MarketplaceMessageThread =
        copy(composerAccess = composerAccessFor(conversationKey, actor))

    private fun MessageThreadSummary.withCurrentAccess(actor: MessagingActor): MessageThreadSummary =
        copy(composerAccess = composerAccessFor(conversationKey, actor))
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

package com.naslabs.yardscape.ui

import com.naslabs.yardscape.domain.ReportReason

enum class SafetyFailureKind {
    Validation,
    Offline,
    Server,
}

sealed interface ReportSubmissionState {
    data object Idle : ReportSubmissionState
    data class Submitted(val receiptId: String) : ReportSubmissionState
    data class Failed(val kind: SafetyFailureKind, val message: String) : ReportSubmissionState
}

enum class BlockMutation {
    Block,
    Unblock,
}

sealed interface BlockMutationState {
    data object Idle : BlockMutationState
    data class Completed(val isBlocked: Boolean, val message: String) : BlockMutationState
    data class Failed(val kind: SafetyFailureKind, val message: String) : BlockMutationState
}

data class ShopperSafetyUiState(
    val eventId: String,
    val eventTitle: String,
    val action: ShopperSafetyAction,
    val reason: ReportReason? = null,
    val details: String = "",
    val reportState: ReportSubmissionState = ReportSubmissionState.Idle,
    val isBlocked: Boolean = false,
    val pendingBlockMutation: BlockMutation? = null,
    val blockState: BlockMutationState = BlockMutationState.Idle,
)

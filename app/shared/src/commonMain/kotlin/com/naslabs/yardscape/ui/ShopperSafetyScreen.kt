package com.naslabs.yardscape.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.naslabs.yardscape.domain.REPORT_DETAILS_MAX_LENGTH
import com.naslabs.yardscape.domain.ReportReason

@Composable
fun ShopperSafetyScreen(
    state: ShopperSafetyUiState?,
    onBack: () -> Unit,
    onReasonChanged: (ReportReason) -> Unit,
    onDetailsChanged: (String) -> Unit,
    onSubmitReport: () -> Unit,
    onRequestBlockMutation: () -> Unit,
    onDismissBlockMutation: () -> Unit,
    onConfirmBlockMutation: () -> Unit,
) {
    if (state == null) {
        RoutePlaceholderScreen(title = "Safety action unavailable", onBack = onBack)
        return
    }
    val spacing = YardScapeDesign.spacing
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = shopperWorkflowContentMaxWidthFor(maxWidth))
                .fillMaxWidth()
                .fillMaxHeight()
                .testTag(YardScapeTestTags.ShopperSafetyScreen)
                .padding(horizontal = spacing.large),
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
        item {
            Column(
                modifier = Modifier.padding(top = spacing.large),
                verticalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                MarketplaceEditorialBackNavigation(
                    onBack = onBack,
                    contentDescription = "Back to event",
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(
                        modifier = Modifier.padding(spacing.large),
                        verticalArrangement = Arrangement.spacedBy(spacing.small),
                    ) {
                        StatusLabel("Safety action")
                        Text(state.action.label, style = MaterialTheme.typography.headlineMedium)
                        Text(state.eventTitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        when (state.action) {
            ShopperSafetyAction.Report -> item {
                ReportForm(
                    state = state,
                    onReasonChanged = onReasonChanged,
                    onDetailsChanged = onDetailsChanged,
                    onSubmit = onSubmitReport,
                )
            }
            ShopperSafetyAction.Block -> item {
                BlockForm(state = state, onRequestMutation = onRequestBlockMutation)
            }
        }
        if (state.action == ShopperSafetyAction.Report) {
            item {
                PrivacyNote(
                    "Do not include an exact address, access instructions, private contact details, or payment information in a report.",
                )
            }
        }
        }
    }

    state.pendingBlockMutation?.let { mutation ->
        AlertDialog(
            onDismissRequest = onDismissBlockMutation,
            title = { Text(if (mutation == BlockMutation.Block) "Block this host?" else "Unblock this host?") },
            text = {
                Text(
                    if (mutation == BlockMutation.Block) {
                        "Their sales will leave discovery, active directions will close, and protected location access will be revoked."
                    } else {
                        "Their sales can return to discovery, but the old RSVP and exact-location grant will not be restored."
                    },
                )
            },
            confirmButton = {
                Button(
                    modifier = Modifier.yardScapeInteractiveTarget(),
                    onClick = onConfirmBlockMutation,
                    colors = if (mutation == BlockMutation.Block) destructiveButtonColors() else ButtonDefaults.buttonColors(),
                ) {
                    Text(if (mutation == BlockMutation.Block) "Block host" else "Unblock host")
                }
            },
            dismissButton = { TextButton(modifier = Modifier.yardScapeInteractiveTarget(), onClick = onDismissBlockMutation) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ReportForm(
    state: ShopperSafetyUiState,
    onReasonChanged: (ReportReason) -> Unit,
    onDetailsChanged: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    val spacing = YardScapeDesign.spacing
    Card(
        modifier = Modifier.fillMaxWidth().testTag(YardScapeTestTags.ShopperSafetyActions),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(spacing.large), verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
            Text("What happened?", style = MaterialTheme.typography.titleLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
                verticalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                ReportReason.entries.forEach { reason ->
                    if (state.reason == reason) {
                        Button(modifier = Modifier.yardScapeInteractiveTarget(), onClick = { onReasonChanged(reason) }) { Text(reason.displayLabel) }
                    } else {
                        OutlinedButton(modifier = Modifier.yardScapeInteractiveTarget(), onClick = { onReasonChanged(reason) }) { Text(reason.displayLabel) }
                    }
                }
            }
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Optional report details" },
                value = state.details,
                onValueChange = { if (it.length <= REPORT_DETAILS_MAX_LENGTH) onDetailsChanged(it) },
                label = { Text("Optional details") },
                supportingText = { Text("${state.details.length}/$REPORT_DETAILS_MAX_LENGTH") },
                minLines = 3,
            )
            ReportFeedback(state.reportState)
            Button(modifier = Modifier.fillMaxWidth().yardScapeInteractiveTarget(), onClick = onSubmit) { Text("Submit mock report") }
        }
    }
}

@Composable
private fun ReportFeedback(state: ReportSubmissionState) {
    when (state) {
        ReportSubmissionState.Idle -> Unit
        is ReportSubmissionState.Submitted -> ShopperStatePanel(
            title = "Report received",
            message = "Your report is queued for review. Reference: ${state.receiptId}",
            statusMessageKind = YardScapeStatusMessageKind.Success,
        )
        is ReportSubmissionState.Failed -> ShopperStatePanel(
            title = "Report not submitted",
            message = state.message,
            statusMessageKind = when (state.kind) {
                SafetyFailureKind.Offline -> YardScapeStatusMessageKind.Offline
                else -> YardScapeStatusMessageKind.Failure
            },
        )
    }
}

@Composable
private fun BlockForm(state: ShopperSafetyUiState, onRequestMutation: () -> Unit) {
    val spacing = YardScapeDesign.spacing
    Card(
        modifier = Modifier.fillMaxWidth().testTag(YardScapeTestTags.ShopperSafetyActions),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(spacing.large), verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
            Text(
                if (state.isBlocked) "This host is blocked" else "Block this marketplace host",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                if (state.isBlocked) {
                    "Unblocking restores discovery only. It never restores the old RSVP or protected location grant."
                } else {
                    "Blocking hides this host's sales, closes directions, and revokes protected location access."
                },
            )
            when (val feedback = state.blockState) {
                BlockMutationState.Idle -> Unit
                is BlockMutationState.Completed -> ShopperStatePanel(
                    title = if (feedback.isBlocked) "Host blocked" else "Host unblocked",
                    message = feedback.message,
                    statusMessageKind = YardScapeStatusMessageKind.Success,
                )
                is BlockMutationState.Failed -> ShopperStatePanel(
                    title = "Action not completed",
                    message = feedback.message,
                    statusMessageKind = when (feedback.kind) {
                        SafetyFailureKind.Offline -> YardScapeStatusMessageKind.Offline
                        else -> YardScapeStatusMessageKind.Failure
                    },
                )
            }
            Button(
                modifier = Modifier.fillMaxWidth().yardScapeInteractiveTarget(),
                onClick = onRequestMutation,
                colors = if (state.isBlocked) ButtonDefaults.buttonColors() else destructiveButtonColors(),
            ) {
                Text(if (state.isBlocked) "Unblock host" else "Block host")
            }
        }
    }
}

@Composable
private fun destructiveButtonColors() = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.error,
    contentColor = MaterialTheme.colorScheme.onError,
)

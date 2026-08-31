package com.naslabs.yardscape.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.naslabs.yardscape.domain.RsvpEligibilityStatus

data class RsvpScreenState(
    val status: RsvpEligibilityStatus,
    val title: String,
    val message: String,
) {
    val canConfirm: Boolean
        get() = status == RsvpEligibilityStatus.ELIGIBLE
}

internal fun RsvpEligibilityStatus.toRsvpScreenState(): RsvpScreenState {
    val copy = when (this) {
        RsvpEligibilityStatus.ELIGIBLE -> "Join this yard sale" to
            "Confirm that you plan to attend. This test workflow accepts the RSVP immediately and returns you to the event."
        RsvpEligibilityStatus.BLOCKED -> "RSVP unavailable" to
            "You blocked this host, so this sale cannot accept your RSVP."
        RsvpEligibilityStatus.ACCESS_REVOKED -> "Access revoked" to
            "The host revoked your access. This RSVP cannot be submitted again."
        RsvpEligibilityStatus.ACCESS_EXPIRED -> "Access expired" to
            "Your access to this sale has expired. This RSVP cannot be submitted again."
        RsvpEligibilityStatus.EVENT_CANCELLED -> "Sale cancelled" to
            "This sale was cancelled and is no longer accepting RSVPs."
        RsvpEligibilityStatus.EVENT_COMPLETED -> "Sale completed" to
            "This sale is complete and is no longer accepting RSVPs."
        RsvpEligibilityStatus.EVENT_UNAVAILABLE -> "Sale unavailable" to
            "This sale is not currently available for RSVP."
        RsvpEligibilityStatus.EVENT_ENDED -> "Sale ended" to
            "The sale window has ended and RSVPs are closed."
        RsvpEligibilityStatus.AT_CAPACITY -> "Sale at capacity" to
            "This sale has reached its attendee limit and cannot accept another RSVP."
        RsvpEligibilityStatus.WAITLISTED -> "RSVP waitlisted" to
            "This RSVP is waiting for space and cannot be submitted again."
        RsvpEligibilityStatus.DECLINED -> "RSVP declined" to
            "This RSVP was declined and cannot be submitted again."
        RsvpEligibilityStatus.ALREADY_ACCEPTED -> "RSVP already accepted" to
            "Your RSVP is already accepted. Return to the event for your current access details."
    }
    return RsvpScreenState(status = this, title = copy.first, message = copy.second)
}

@Composable
fun RsvpScreen(
    state: RsvpScreenState,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
) {
    val spacing = YardScapeDesign.spacing
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = shopperWorkflowContentMaxWidthFor(maxWidth))
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = spacing.large)
                .testTag(YardScapeTestTags.RsvpScreen),
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
            item {
                Column(
                    modifier = Modifier.padding(top = spacing.small),
                    verticalArrangement = Arrangement.spacedBy(spacing.large),
                ) {
                    MarketplaceEditorialBackNavigation(
                        onBack = onBack,
                        contentDescription = "Back to event",
                    )
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(YardScapeTestTags.RsvpProtectedLocationCard),
                        shape = MaterialTheme.shapes.large,
                        color = if (state.canConfirm) MintMist else MaterialTheme.colorScheme.surface,
                    ) {
                        Column(
                            modifier = Modifier.padding(spacing.large),
                            verticalArrangement = Arrangement.spacedBy(spacing.medium),
                        ) {
                            StatusLabel(text = "RSVP")
                            Text(
                                text = state.title,
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (state.canConfirm) {
                        ShopperStatePanel(
                            title = "Protected location",
                            message = "The exact address and directions appear only while your accepted RSVP has active access.",
                            statusMessageKind = YardScapeStatusMessageKind.ClosedAccess,
                        )
                        Button(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag(YardScapeTestTags.RsvpConfirmAction)
                                .semantics { contentDescription = "Confirm RSVP" },
                            onClick = onConfirm,
                        ) {
                            Text("Confirm RSVP")
                        }
                    }
                    OutlinedButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .semantics { contentDescription = "Back to event" },
                        onClick = onBack,
                    ) {
                        Text("Back to event")
                    }
                }
            }
        }
    }
}

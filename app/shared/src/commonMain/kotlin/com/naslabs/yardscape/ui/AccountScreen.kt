package com.naslabs.yardscape.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.naslabs.yardscape.domain.UserRole

@Composable
fun AccountScreen(
    state: MockAccountState,
    onSignIn: (UserRole) -> Unit,
    onSignOut: () -> Unit,
    onExpireSession: () -> Unit,
    onViewProfile: (UserRole) -> Unit,
    onOpenSettings: (AccountSettingsSection) -> Unit,
    onPreferencesChanged: (NotificationPreferences) -> Unit,
) {
    val presentation = marketplaceAccountPresentation(state)
    val spacing = YardScapeDesign.spacing
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().testTag(YardScapeTestTags.AccountScreen),
        contentAlignment = Alignment.TopCenter,
    ) {
    LazyColumn(
        modifier = Modifier.width(marketplaceAccountContentWidthFor(maxWidth))
            .fillMaxSize()
            .padding(horizontal = spacing.large),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
    ) {
        item {
            Column(
                Modifier
                    .padding(top = spacing.large)
                    .testTag(YardScapeTestTags.AccountIntro),
                verticalArrangement = Arrangement.spacedBy(spacing.extraSmall),
            ) {
                Text(
                    if (state.isSignedIn) "Your local marketplace space" else "A privacy-first marketplace space",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    "Mock profiles and local session state only. No real credentials are stored.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        when (state.sessionStatus) {
            MockSessionStatus.SignedOut -> item {
                SignedOutCard(
                    title = presentation.heading,
                    message = state.signInReason
                        ?: "Public sale previews remain available. Sign in only when you need a protected action.",
                    onSignIn = onSignIn,
                )
            }
            MockSessionStatus.Expired -> item {
                SignedOutCard(
                    title = presentation.heading,
                    message = "Protected location and private account data were cleared. Sign in again to continue; public browsing is still available.",
                    onSignIn = onSignIn,
                )
            }
            MockSessionStatus.SignedIn -> {
                item {
                    ProfileCard(state.viewedProfile ?: state.activeProfile)
                }
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(modifier = Modifier.yardScapeInteractiveTarget(), onClick = { onViewProfile(UserRole.SHOPPER) }) { Text("Shopper profile") }
                        OutlinedButton(modifier = Modifier.yardScapeInteractiveTarget(), onClick = { onViewProfile(UserRole.HOST) }) { Text("Host profile") }
                    }
                }
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AccountSettingsSection.entries.forEach { section ->
                            OutlinedButton(modifier = Modifier.yardScapeInteractiveTarget(), onClick = { onOpenSettings(section) }) { Text(section.label) }
                        }
                    }
                }
                item {
                    AccountSettingsContent(
                        state = state,
                        onExpireSession = onExpireSession,
                        onPreferencesChanged = onPreferencesChanged,
                    )
                }
                item {
                    TextButton(modifier = Modifier.yardScapeInteractiveTarget(), onClick = onSignOut) { Text("Sign out") }
                }
            }
        }
        item {
            PrivacyNote("Marketplace safety: meet only during listed sale hours, keep conversations in the app, and report pressure to share private contact or payment details.")
        }
    }
    }
}

@Composable
private fun SignedOutCard(title: String, message: String, onSignIn: (UserRole) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(YardScapeDesign.spacing.large), verticalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.medium)) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Text(message)
            Button(modifier = Modifier.fillMaxWidth().yardScapeInteractiveTarget(), onClick = { onSignIn(UserRole.SHOPPER) }) {
                Text("Use mock shopper session")
            }
            OutlinedButton(modifier = Modifier.fillMaxWidth().yardScapeInteractiveTarget(), onClick = { onSignIn(UserRole.HOST) }) {
                Text("Use mock host session")
            }
        }
    }
}

@Composable
private fun ProfileCard(profile: MockAccountProfile?) {
    if (profile == null) return
    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(YardScapeDesign.spacing.large), verticalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.small)) {
            Text("Your marketplace profile", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(profile.displayName, style = MaterialTheme.typography.headlineSmall)
            Text(profile.role.name.lowercase().replaceFirstChar { it.uppercase() } + " profile", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Confirmed facts", style = MaterialTheme.typography.titleSmall)
            profile.verificationFacts.forEach { Text("• $it") }
            Text("Community activity", style = MaterialTheme.typography.titleSmall)
            profile.communitySignals.forEach { Text("• $it") }
            Text(
                "These signals offer context, not identity, safety, or transaction guarantees.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AccountSettingsContent(
    state: MockAccountState,
    onExpireSession: () -> Unit,
    onPreferencesChanged: (NotificationPreferences) -> Unit,
) {
    when (state.selectedSettingsSection) {
        AccountSettingsSection.Sessions -> EditorialSettingsCard("Sessions") {
            Column(verticalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.small)) {
                Text("Mock session", style = MaterialTheme.typography.titleMedium)
                Text("This local session contains no real credential, token, or production account data.")
                OutlinedButton(modifier = Modifier.yardScapeInteractiveTarget(), onClick = onExpireSession) { Text("Simulate session expiry") }
            }
        }
        AccountSettingsSection.Privacy -> EditorialSettingsCard("Privacy and location") {
            Column(verticalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.small)) {
                Text("Public browsing uses approximate areas. Exact addresses require an active accepted RSVP and are cleared on sign-out or session expiry.")
                Text("Report and block controls will use the same signed-in safety gate without exposing your action publicly.")
            }
        }
        AccountSettingsSection.Notifications -> EditorialSettingsCard("Notifications") {
            Column(verticalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.small)) {
                PreferenceRow("RSVP updates", state.notificationPreferences.rsvpUpdates) {
                    onPreferencesChanged(state.notificationPreferences.copy(rsvpUpdates = it))
                }
                PreferenceRow("Sale reminders", state.notificationPreferences.saleReminders) {
                    onPreferencesChanged(state.notificationPreferences.copy(saleReminders = it))
                }
                PreferenceRow("Community safety tips", state.notificationPreferences.communityTips) {
                    onPreferencesChanged(state.notificationPreferences.copy(communityTips = it))
                }
                Text("Preferences are local mock state; no notification service is contacted.")
            }
        }
        null -> Text("Choose an account setting to review.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EditorialSettingsCard(title: String, content: @Composable () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(YardScapeDesign.spacing.large), verticalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.small)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
private fun PreferenceRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(
            modifier = Modifier.yardScapeInteractiveTarget(),
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

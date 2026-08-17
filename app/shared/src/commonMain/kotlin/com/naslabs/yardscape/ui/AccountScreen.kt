package com.naslabs.yardscape.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(Modifier.padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Account", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Mock profiles and local session state only. No real credentials are stored.")
            }
        }
        when (state.sessionStatus) {
            MockSessionStatus.SignedOut -> item {
                SignedOutCard(
                    title = "Browse without an account",
                    message = state.signInReason
                        ?: "Public sale previews remain available. Sign in only when you need a protected action.",
                    onSignIn = onSignIn,
                )
            }
            MockSessionStatus.Expired -> item {
                SignedOutCard(
                    title = "Session expired safely",
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
                        OutlinedButton(onClick = { onViewProfile(UserRole.SHOPPER) }) { Text("Shopper profile") }
                        OutlinedButton(onClick = { onViewProfile(UserRole.HOST) }) { Text("Host profile") }
                    }
                }
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AccountSettingsSection.entries.forEach { section ->
                            OutlinedButton(onClick = { onOpenSettings(section) }) { Text(section.label) }
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
                    TextButton(onClick = onSignOut) { Text("Sign out") }
                }
            }
        }
        item {
            PrivacyNote("Marketplace safety: meet only during listed sale hours, keep conversations in the app, and report pressure to share private contact or payment details.")
        }
    }
}

@Composable
private fun SignedOutCard(title: String, message: String, onSignIn: (UserRole) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(message)
            Button(modifier = Modifier.fillMaxWidth(), onClick = { onSignIn(UserRole.SHOPPER) }) {
                Text("Use mock shopper session")
            }
            OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { onSignIn(UserRole.HOST) }) {
                Text("Use mock host session")
            }
        }
    }
}

@Composable
private fun ProfileCard(profile: MockAccountProfile?) {
    if (profile == null) return
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(profile.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(profile.role.name.lowercase().replaceFirstChar { it.uppercase() } + " profile")
            Text("Confirmed facts", fontWeight = FontWeight.Bold)
            profile.verificationFacts.forEach { Text("• $it") }
            Text("Community activity — not identity verification", fontWeight = FontWeight.Bold)
            profile.communitySignals.forEach { Text("• $it") }
            Text(
                "These signals may help with context, but they do not guarantee identity, safety, or transaction quality.",
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
        AccountSettingsSection.Sessions -> Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Mock session", fontWeight = FontWeight.Bold)
                Text("This local session contains no real credential, token, or production account data.")
                OutlinedButton(onClick = onExpireSession) { Text("Simulate session expiry") }
            }
        }
        AccountSettingsSection.Privacy -> Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Privacy and location", fontWeight = FontWeight.Bold)
                Text("Public browsing uses approximate areas. Exact addresses require an active accepted RSVP and are cleared on sign-out or session expiry.")
                Text("Report and block controls will use the same signed-in safety gate without exposing your action publicly.")
            }
        }
        AccountSettingsSection.Notifications -> Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
private fun PreferenceRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

package com.naslabs.yardscape.ui

import com.naslabs.yardscape.domain.UserRole

enum class MockSessionStatus {
    SignedOut,
    SignedIn,
    Expired,
}

enum class AccountSettingsSection(val label: String) {
    Sessions("Sessions"),
    Privacy("Privacy and location"),
    Notifications("Notifications"),
}

enum class ProtectedAction(val label: String) {
    Rsvp("RSVP and request a protected location"),
    RevealLocation("View a protected exact location"),
    HostManagement("Manage a hosted event"),
    Messaging("Use event messages"),
    Report("Report unsafe content"),
    Block("Block a marketplace member"),
}

sealed interface ProtectedActionDecision {
    data object Allowed : ProtectedActionDecision
    data class SignInRequired(val message: String) : ProtectedActionDecision
}

data class MockAccountProfile(
    val id: String,
    val displayName: String,
    val role: UserRole,
    val verificationFacts: List<String>,
    val communitySignals: List<String>,
)

data class NotificationPreferences(
    val rsvpUpdates: Boolean = true,
    val saleReminders: Boolean = true,
    val communityTips: Boolean = false,
)

data class MockAccountState(
    val sessionStatus: MockSessionStatus,
    val activeProfile: MockAccountProfile?,
    val viewedProfile: MockAccountProfile?,
    val selectedSettingsSection: AccountSettingsSection? = null,
    val notificationPreferences: NotificationPreferences = NotificationPreferences(),
    val signInReason: String? = null,
) {
    val isSignedIn: Boolean
        get() = sessionStatus == MockSessionStatus.SignedIn && activeProfile != null
}

interface AccountSessionController {
    fun stateFor(status: MockSessionStatus, role: UserRole): MockAccountState
    fun signIn(role: UserRole, preferences: NotificationPreferences): MockAccountState
    fun profileFor(role: UserRole): MockAccountProfile
    fun gate(state: MockAccountState, action: ProtectedAction): ProtectedActionDecision
}

class SeededAccountSessionController : AccountSessionController {
    override fun stateFor(status: MockSessionStatus, role: UserRole): MockAccountState {
        val profile = profileFor(role)
        return MockAccountState(
            sessionStatus = status,
            activeProfile = profile.takeIf { status == MockSessionStatus.SignedIn },
            viewedProfile = profile,
        )
    }

    override fun signIn(role: UserRole, preferences: NotificationPreferences): MockAccountState {
        val profile = profileFor(role)
        return MockAccountState(
            sessionStatus = MockSessionStatus.SignedIn,
            activeProfile = profile,
            viewedProfile = profile,
            notificationPreferences = preferences,
        )
    }

    override fun profileFor(role: UserRole): MockAccountProfile = when (role) {
        UserRole.SHOPPER -> MockAccountProfile(
            id = "mock-shopper-profile",
            displayName = "Jordan Lee",
            role = role,
            verificationFacts = listOf("Email confirmed for this mock session"),
            communitySignals = listOf("3 RSVPs completed", "Joined 2 neighborhood sales"),
        )
        UserRole.HOST -> MockAccountProfile(
            id = "mock-host-profile",
            displayName = "Avery Morgan",
            role = role,
            verificationFacts = listOf("Email confirmed for this mock session", "Host contact method on file"),
            communitySignals = listOf("Hosted 3 sales", "2 attendees left positive feedback"),
        )
    }

    override fun gate(state: MockAccountState, action: ProtectedAction): ProtectedActionDecision =
        if (state.isSignedIn) {
            ProtectedActionDecision.Allowed
        } else {
            ProtectedActionDecision.SignInRequired(
                "Sign in to ${action.label.lowercase()}. Public sale browsing remains available without an account.",
            )
        }
}

data class PendingProtectedAction(
    val action: ProtectedAction,
    val resumeRoute: YardScapeRoute? = null,
)

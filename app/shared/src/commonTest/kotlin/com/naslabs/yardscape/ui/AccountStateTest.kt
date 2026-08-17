package com.naslabs.yardscape.ui

import com.naslabs.yardscape.data.SeededYardSaleData
import com.naslabs.yardscape.domain.UserRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AccountStateTest {
    @Test
    fun signedOutVisitorsCanBrowseButProtectedActionsRequestSignIn() {
        val state = YardScapeAppState(initialAccountStatus = MockSessionStatus.SignedOut)
        val eventId = SeededYardSaleData.FAMILY_GARAGE_EVENT_ID

        assertTrue(state.browseItems().isNotEmpty())
        assertNotNull(state.detailStateFor(eventId))
        assertTrue(state.myRsvpItems().isEmpty())
        assertIs<ProtectedActionDecision.SignInRequired>(state.protectedActionDecision(ProtectedAction.Report))
        assertIs<ProtectedActionDecision.SignInRequired>(state.protectedActionDecision(ProtectedAction.Block))

        state.openRsvp(eventId)

        assertEquals(YardScapeRoute.Account, state.route)
        assertEquals(ProtectedAction.Rsvp, state.pendingProtectedAction?.action)
        assertTrue(state.accountState.signInReason.orEmpty().contains("Public sale browsing remains available"))
    }

    @Test
    fun mockSignInResumesTheProtectedWorkflowWithoutRealCredentials() {
        val eventId = SeededYardSaleData.FAMILY_GARAGE_EVENT_ID
        val state = YardScapeAppState(
            initialAccountStatus = MockSessionStatus.SignedOut,
            initialRoute = YardScapeRoute.EventDetail(eventId),
        )

        state.openRsvp(eventId)
        state.signInMock(UserRole.SHOPPER)

        assertEquals(MockSessionStatus.SignedIn, state.accountState.sessionStatus)
        assertEquals(UserRole.SHOPPER, state.activeUserRole)
        assertEquals(YardScapeRoute.EventDetail(eventId), state.route)
        assertNull(state.pendingProtectedAction)
        assertIs<ProtectedActionDecision.Allowed>(state.protectedActionDecision(ProtectedAction.Rsvp))
        assertFalse(state.accountState.toString().contains("password", ignoreCase = true))
        assertFalse(state.accountState.toString().contains("token", ignoreCase = true))

        state.signOutMock()
        state.signInMock(UserRole.HOST)
        assertEquals(UserRole.HOST, state.activeUserRole)
        assertEquals(UserRole.HOST, state.accountState.activeProfile?.role)
    }

    @Test
    fun sessionExpiryClearsProtectedLocationStateAndHostData() {
        val acceptedEventId = SeededYardSaleData.ESTATE_TOOLS_EVENT_ID
        val shopper = YardScapeAppState(
            shopperId = SeededYardSaleData.SHOPPER_WITH_ACCEPTED_ACCESS_ID,
            initialRoute = YardScapeRoute.EventDetail(acceptedEventId),
        )
        assertIs<LocationRevealState.Revealed>(shopper.detailStateFor(acceptedEventId)?.revealState)
        assertNotNull(shopper.requestDirections(acceptedEventId))

        shopper.expireMockSession()

        assertEquals(MockSessionStatus.Expired, shopper.accountState.sessionStatus)
        assertEquals(YardScapeRoute.Account, shopper.route)
        assertNull(shopper.directionsEventId)
        assertTrue(shopper.myRsvpItems().isEmpty())
        val signedOutDetail = assertNotNull(shopper.detailStateFor(acceptedEventId))
        assertFalse(signedOutDetail.revealState is LocationRevealState.Revealed)
        assertFalse(signedOutDetail.toString().contains("418 Juniper Avenue"))

        val host = YardScapeAppState(activeUserRole = UserRole.HOST)
        assertTrue(host.hostEventItems().isNotEmpty())
        host.signOutMock()
        assertTrue(host.hostEventItems().isEmpty())
        assertEquals(0, host.pendingAttendeeCount(SeededYardSaleData.FAMILY_GARAGE_EVENT_ID))
    }

    @Test
    fun profilesSeparateConfirmedFactsFromCommunitySignals() {
        val controller = SeededAccountSessionController()

        UserRole.entries.forEach { role ->
            val profile = controller.profileFor(role)
            assertTrue(profile.verificationFacts.isNotEmpty())
            assertTrue(profile.communitySignals.isNotEmpty())
            assertTrue(profile.verificationFacts.intersect(profile.communitySignals.toSet()).isEmpty())
            assertFalse(profile.toString().contains("identity verified", ignoreCase = true))
            assertFalse(profile.toString().contains("background check", ignoreCase = true))
        }
    }

    @Test
    fun accountSettingsAndNotificationPreferencesRemainLocalState() {
        val state = YardScapeAppState()
        val preferences = NotificationPreferences(
            rsvpUpdates = false,
            saleReminders = true,
            communityTips = true,
        )

        state.openAccountSettings(AccountSettingsSection.Notifications)
        state.updateNotificationPreferences(preferences)

        assertEquals(AccountSettingsSection.Notifications, state.accountState.selectedSettingsSection)
        assertEquals(preferences, state.accountState.notificationPreferences)
    }

    @Test
    fun internalProtectedPathsCannotBypassTheSessionGate() {
        val state = YardScapeAppState(initialAccountStatus = MockSessionStatus.SignedOut)
        val eventId = SeededYardSaleData.FAMILY_GARAGE_EVENT_ID

        listOf(
            YardScapeRoute.MyRsvps.path,
            YardScapeRoute.Rsvp(eventId).path,
            YardScapeRoute.HostCreateEdit(eventId).path,
            YardScapeRoute.HostAttendees(eventId).path,
        ).forEach { path ->
            assertTrue(state.navigateToPath(path))
            assertEquals(YardScapeRoute.Account, state.route)
            assertNotNull(state.pendingProtectedAction)
        }

        state.confirmRsvp(eventId)
        assertEquals(YardScapeRoute.Account, state.route)
        assertTrue(state.myRsvpItems().isEmpty())
    }
}

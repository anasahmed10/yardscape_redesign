package com.naslabs.yardscape

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.naslabs.yardscape.ui.AccountScreen
import com.naslabs.yardscape.ui.BrowseScreen
import com.naslabs.yardscape.ui.HostDashboardScreen
import com.naslabs.yardscape.ui.HostCreateEditScreen
import com.naslabs.yardscape.ui.HostAttendanceScreen
import com.naslabs.yardscape.ui.MessagesScreen
import com.naslabs.yardscape.ui.MyFindsScreen
import com.naslabs.yardscape.ui.MyFindsSection
import com.naslabs.yardscape.ui.PublicEventDetailScreen
import com.naslabs.yardscape.ui.RsvpScreen
import com.naslabs.yardscape.ui.ShopperSafetyScreen
import com.naslabs.yardscape.ui.YardScapeAppShell
import com.naslabs.yardscape.ui.YardScapeAppState
import com.naslabs.yardscape.ui.YardScapePrimaryDestination
import com.naslabs.yardscape.ui.YardScapeRoute
import com.naslabs.yardscape.ui.YardScapeTheme
import com.naslabs.yardscape.ui.ApproximateLocationPermission
import com.naslabs.yardscape.map.ApproximateLocationResult
import com.naslabs.yardscape.map.rememberApproximateLocationProvider
import com.naslabs.yardscape.domain.MapViewport
import com.naslabs.yardscape.domain.ViewportCenter
import kotlinx.coroutines.launch

@Composable
@Preview
fun App() {
    App(remember { YardScapeAppState() })
}

@Composable
fun App(appState: YardScapeAppState) {
    val locationProvider = rememberApproximateLocationProvider()
    val coroutineScope = rememberCoroutineScope()
    val safeAreaModifier = if (appState.route == YardScapeRoute.Browse) {
        Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical))
    } else {
        Modifier.safeContentPadding()
    }
    YardScapeTheme {
        Surface(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .then(safeAreaModifier)
                .fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            YardScapeAppShell(
                route = appState.route,
                onDestinationSelected = appState::navigateTo,
            ) {
                when (val currentRoute = appState.route) {
                    YardScapeRoute.Browse -> BrowseScreen(
                        state = appState.discoveryState(),
                        mapState = appState.mapDiscoveryState,
                        dataAvailability = appState.dataAvailability,
                        onEventSelected = appState::openEvent,
                        onHostSelected = {
                            appState.navigateTo(YardScapePrimaryDestination.Host)
                        },
                        onQueryChanged = appState::updateDiscoveryQuery,
                        onDateChanged = appState::updateDiscoveryDate,
                        onDistanceChanged = appState::updateDiscoveryDistance,
                        onCategoryToggled = appState::toggleDiscoveryCategory,
                        onDisplayModeChanged = appState::updateDiscoveryDisplayMode,
                        onResetFilters = appState::clearDiscoveryFilters,
                        onRetryData = appState::retryBrowseData,
                        onSavedToggled = { appState.toggleSavedEvent(it) },
                        onMapViewportChanged = appState::updateMapCameraViewport,
                        onMapViewportSettled = appState::settleMapCameraViewport,
                        onSearchThisArea = appState::searchMapCameraArea,
                        onShowAllNearbySales = appState::showAllNearbySales,
                        onMapEventSelected = appState::selectDiscoveryEvent,
                        onMapAvailabilityChanged = appState::updateMapAvailability,
                        onSheetPositionChanged = appState::updateMapResultsSheetPosition,
                        onUseMyLocation = {
                            if (appState.requestApproximateLocation()) coroutineScope.launch {
                                when (val result = locationProvider.requestApproximateLocation()) {
                                    is ApproximateLocationResult.Available -> {
                                        appState.updateApproximateLocationPermission(ApproximateLocationPermission.Granted)
                                        appState.updateMapCameraViewport(
                                            MapViewport(
                                                center = ViewportCenter(
                                                    latitude = result.center.latitude,
                                                    longitude = result.center.longitude,
                                                ),
                                                zoomLevel = 12.0,
                                            ),
                                        )
                                        appState.settleMapCameraViewport()
                                    }
                                    ApproximateLocationResult.PermissionDenied -> {
                                        appState.updateApproximateLocationPermission(ApproximateLocationPermission.Denied)
                                    }
                                    ApproximateLocationResult.Unavailable -> {
                                        appState.updateApproximateLocationPermission(ApproximateLocationPermission.Unavailable)
                                    }
                                }
                            }
                        },
                    )

                    is YardScapeRoute.MyFinds -> MyFindsScreen(
                        state = appState.myFindsState(currentRoute.section),
                        pendingCancellationEventId = appState.pendingRsvpCancellationEventId,
                        onSectionSelected = appState::openMyFinds,
                        onEventSelected = appState::openEvent,
                        onUnsave = { appState.toggleSavedEvent(it) },
                        onBrowse = { appState.navigateTo(YardScapePrimaryDestination.Browse) },
                        onRequestCancellation = appState::requestRsvpCancellation,
                        onDismissCancellation = appState::dismissRsvpCancellation,
                        onConfirmCancellation = appState::confirmRsvpCancellation,
                        onAddReminder = appState::addMockReminder,
                        onExportCalendar = appState::prepareMockCalendarExport,
                        onDirections = appState::requestDirections,
                    )
                    YardScapeRoute.Host -> HostDashboardScreen(
                        events = appState.hostEventItems(),
                        onCreateEvent = { appState.openHostCreateEdit() },
                        onEditEvent = appState::openHostCreateEdit,
                        onManageAttendees = { appState.openHostAttendees(it) },
                    )
                    YardScapeRoute.Messages,
                    is YardScapeRoute.MessageThread,
                    -> MessagesScreen(
                        inboxState = appState.messagingInboxState,
                        threadState = appState.messagingThreadState,
                        isThreadRoute = currentRoute is YardScapeRoute.MessageThread,
                        pendingAuthorizationSignal = appState.pendingMessageThreadAuthorizationSignal,
                        hasPendingAuthorization = appState.hasPendingMessageThreadAuthorization,
                        actor = appState.currentMessagingActor,
                        onLoadInbox = appState::loadMessagingInbox,
                        onResumePendingThread = appState::resumePendingMessageThread,
                        onOpenThread = appState::openMessageThread,
                        onMarkRead = appState::markCurrentMessageThreadRead,
                        onDraftChanged = appState::updateMessageDraft,
                        onSend = appState::sendMessageDraft,
                        onRetry = appState::retryMessage,
                        onOpenEvent = appState::openEvent,
                        onReport = appState::openMessageThreadReport,
                        onBlock = appState::openMessageThreadBlock,
                        onBack = { appState.navigateBack() },
                        onBrowse = { appState.navigateTo(YardScapePrimaryDestination.Browse) },
                    )
                    YardScapeRoute.Account -> AccountScreen(
                        state = appState.accountState,
                        onSignIn = appState::signInMock,
                        onSignOut = appState::signOutMock,
                        onExpireSession = appState::expireMockSession,
                        onViewProfile = appState::viewMockProfile,
                        onOpenSettings = appState::openAccountSettings,
                        onPreferencesChanged = appState::updateNotificationPreferences,
                    )

                    is YardScapeRoute.EventDetail -> PublicEventDetailScreen(
                        state = appState.selectedEventDetailState(),
                        onBack = { appState.navigateBack() },
                        onRsvp = { appState.openRsvp(currentRoute.eventId) },
                        onDirections = { appState.requestDirections(currentRoute.eventId) },
                        onReport = { appState.openReport(currentRoute.eventId) },
                        onBlock = { appState.openBlock(currentRoute.eventId) },
                    )

                    is YardScapeRoute.Rsvp -> RsvpScreen(
                        state = appState.rsvpScreenStateFor(currentRoute.eventId),
                        onConfirm = { appState.confirmRsvp(currentRoute.eventId) },
                        onBack = { appState.navigateBack() },
                    )

                    is YardScapeRoute.EventSafety -> ShopperSafetyScreen(
                        state = appState.shopperSafetyState,
                        onBack = { appState.navigateBack() },
                        onReasonChanged = appState::updateSafetyReportReason,
                        onDetailsChanged = appState::updateSafetyReportDetails,
                        onSubmitReport = appState::submitSafetyReport,
                        onRequestBlockMutation = appState::requestBlockMutation,
                        onDismissBlockMutation = appState::dismissBlockMutation,
                        onConfirmBlockMutation = appState::confirmBlockMutation,
                    )

                    is YardScapeRoute.HostCreateEdit -> HostEditorRoute(
                        appState = appState,
                        route = currentRoute,
                    )

                    is YardScapeRoute.HostAttendees -> HostAttendanceScreen(
                        state = appState.hostAttendanceState(currentRoute.eventId),
                        pendingAction = appState.pendingHostAttendeeAction,
                        onBack = { appState.navigateBack() },
                        onRequestAction = appState::requestHostAttendeeAction,
                        onMessageAttendee = appState::openHostAttendeeMessage,
                        onDismissAction = appState::dismissHostAttendeeAction,
                        onConfirmAction = appState::confirmHostAttendeeAction,
                    )
                }
            }
        }
    }
}

@Composable
private fun HostEditorRoute(appState: YardScapeAppState, route: YardScapeRoute.HostCreateEdit) {
    val sessionSignal = appState.hostEditorSessionSignal
    var editorState by remember(appState, route.eventId, sessionSignal) {
        mutableStateOf(appState.hostEditorState(route.eventId))
    }
    HostCreateEditScreen(
        editorState = editorState,
        nowEpochMillis = appState.nowEpochMillis,
        availablePhotos = appState.availableHostPhotos(),
        onAddressSearch = appState::searchHostLocations,
        onEditorStateChanged = { updated ->
            editorState = appState.rememberHostEditorState(updated)
        },
        onStepSelected = { step -> editorState = appState.moveHostEditor(editorState, step) },
        onNew = { appState.openHostCreateEdit() },
        onSaveDraft = { editorState = appState.saveHostDraft(editorState) },
        onPublish = { editorState = appState.publishHostEvent(editorState) },
        onCancelEvent = {
            editorState.savedEventId?.let { eventId ->
                appState.cancelHostEvent(eventId)
                editorState = appState.rememberHostEditorState(editorState.copy(pendingConfirmation = null))
            }
        },
        onHideEvent = {
            editorState.savedEventId?.let { eventId ->
                appState.hideHostEvent(eventId)
                editorState = appState.rememberHostEditorState(editorState.copy(pendingConfirmation = null))
            }
        },
        onBack = { appState.navigateBack() },
    )
}

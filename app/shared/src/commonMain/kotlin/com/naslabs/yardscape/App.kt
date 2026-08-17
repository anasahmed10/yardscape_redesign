package com.naslabs.yardscape

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.naslabs.yardscape.ui.AccountDestinationScreen
import com.naslabs.yardscape.ui.BrowseScreen
import com.naslabs.yardscape.ui.HostDashboardScreen
import com.naslabs.yardscape.ui.HostCreateEditScreen
import com.naslabs.yardscape.ui.HostAttendanceScreen
import com.naslabs.yardscape.ui.MyRsvpsScreen
import com.naslabs.yardscape.ui.PublicEventDetailScreen
import com.naslabs.yardscape.ui.RsvpScreen
import com.naslabs.yardscape.ui.SavedDestinationScreen
import com.naslabs.yardscape.ui.YardScapeAppShell
import com.naslabs.yardscape.ui.YardScapeAppState
import com.naslabs.yardscape.ui.YardScapePrimaryDestination
import com.naslabs.yardscape.ui.YardScapeRoute
import com.naslabs.yardscape.ui.YardScapeTheme

@Composable
@Preview
fun App() {
    App(remember { YardScapeAppState() })
}

@Composable
fun App(appState: YardScapeAppState) {
    YardScapeTheme {
        Surface(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .safeContentPadding()
                .fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            YardScapeAppShell(
                route = appState.route,
                activeUserRole = appState.activeUserRole,
                onDestinationSelected = appState::navigateTo,
            ) {
                when (val currentRoute = appState.route) {
                    YardScapeRoute.Browse -> BrowseScreen(
                        state = appState.discoveryState(),
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
                        onSavedToggled = { appState.toggleSavedEvent(it) },
                    )

                    YardScapeRoute.Saved -> SavedDestinationScreen(
                        events = appState.savedItems(),
                        onEventSelected = appState::openEvent,
                        onUnsave = { appState.toggleSavedEvent(it) },
                        onBrowse = { appState.navigateTo(YardScapePrimaryDestination.Browse) },
                        onMyRsvps = appState::openMyRsvps,
                    )
                    YardScapeRoute.MyRsvps -> MyRsvpsScreen(
                        items = appState.myRsvpItems(),
                        pendingCancellationEventId = appState.pendingRsvpCancellationEventId,
                        onBack = { appState.navigateBack() },
                        onEventSelected = appState::openEvent,
                        onRequestCancellation = appState::requestRsvpCancellation,
                        onDismissCancellation = appState::dismissRsvpCancellation,
                        onConfirmCancellation = appState::confirmRsvpCancellation,
                        onAddReminder = appState::addMockReminder,
                        onExportCalendar = appState::prepareMockCalendarExport,
                        onDirections = { appState.requestDirections(it) },
                    )
                    YardScapeRoute.Host -> HostDashboardScreen(
                        events = appState.hostEventItems(),
                        onCreateEvent = { appState.openHostCreateEdit() },
                        onEditEvent = appState::openHostCreateEdit,
                        onManageAttendees = { appState.openHostAttendees(it) },
                    )
                    YardScapeRoute.Account -> AccountDestinationScreen(appState.activeUserRole)

                    is YardScapeRoute.EventDetail -> PublicEventDetailScreen(
                        state = appState.selectedEventDetailState(),
                        onBack = { appState.navigateBack() },
                        onRsvp = { appState.openRsvp(currentRoute.eventId) },
                    )

                    is YardScapeRoute.Rsvp -> RsvpScreen(
                        onConfirm = { appState.confirmRsvp(currentRoute.eventId) },
                        onBack = { appState.navigateBack() },
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
    var editorState by remember(appState, route.eventId) {
        mutableStateOf(appState.hostEditorState(route.eventId))
    }
    HostCreateEditScreen(
        hostEvents = appState.hostEventItems(),
        editorState = editorState,
        availablePhotos = appState.availableHostPhotos(),
        onAddressSearch = appState::searchHostLocations,
        onEditorStateChanged = { updated ->
            editorState = appState.rememberHostEditorState(updated)
        },
        onStepSelected = { step -> editorState = appState.moveHostEditor(editorState, step) },
        onNew = { appState.openHostCreateEdit() },
        onEdit = appState::openHostCreateEdit,
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

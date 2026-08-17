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
                    )
                    YardScapeRoute.Host -> HostDashboardScreen(
                        events = appState.hostEventItems(),
                        onCreateEvent = { appState.openHostCreateEdit() },
                        onEditEvent = appState::openHostCreateEdit,
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
        onAddressSearch = appState::searchHostLocations,
        onDraftChanged = { draft ->
            editorState = editorState.copy(draft = draft, validationErrors = emptyList())
        },
        onNew = { appState.openHostCreateEdit() },
        onEdit = appState::openHostCreateEdit,
        onSaveDraft = { editorState = appState.saveHostDraft(editorState.draft) },
        onPublish = { editorState = appState.publishHostEvent(editorState.draft) },
        onCancelEvent = {
            editorState.savedEventId?.let { eventId ->
                appState.cancelHostEvent(eventId)
                editorState = appState.hostEditorState(eventId)
            }
        },
        onHideEvent = {
            editorState.savedEventId?.let { eventId ->
                appState.hideHostEvent(eventId)
                editorState = appState.hostEditorState(eventId)
            }
        },
        onBack = { appState.navigateBack() },
    )
}

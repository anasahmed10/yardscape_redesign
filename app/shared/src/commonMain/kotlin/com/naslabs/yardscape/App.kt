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
import com.naslabs.yardscape.ui.BrowseScreen
import com.naslabs.yardscape.ui.HostCreateEditScreen
import com.naslabs.yardscape.ui.PublicEventDetailScreen
import com.naslabs.yardscape.ui.RsvpScreen
import com.naslabs.yardscape.ui.YardScapeAppState
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
        var route by remember(appState) { mutableStateOf(appState.route) }

        Surface(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .safeContentPadding()
                .fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            when (val currentRoute = route) {
                YardScapeRoute.Browse -> BrowseScreen(
                    events = appState.browseItems(),
                    dataAvailability = appState.dataAvailability,
                    onEventSelected = { eventId ->
                        appState.openEvent(eventId)
                        route = appState.route
                    },
                    onHostSelected = {
                        appState.openHostCreateEdit()
                        route = appState.route
                    },
                )

                is YardScapeRoute.EventDetail -> PublicEventDetailScreen(
                    state = appState.selectedEventDetailState(),
                    onBack = {
                        appState.returnToBrowse()
                        route = appState.route
                    },
                    onRsvp = {
                        appState.openRsvp(currentRoute.eventId)
                        route = appState.route
                    },
                )

                is YardScapeRoute.Rsvp -> RsvpScreen(
                    onConfirm = {
                        appState.confirmRsvp(currentRoute.eventId)
                        route = appState.route
                    },
                    onBack = {
                        appState.openEvent(currentRoute.eventId)
                        route = appState.route
                    },
                )

                is YardScapeRoute.HostCreateEdit -> {
                    var editorState by remember(appState, currentRoute.eventId) {
                        mutableStateOf(appState.hostEditorState(currentRoute.eventId))
                    }
                    HostCreateEditScreen(
                        hostEvents = appState.hostEventItems(),
                        editorState = editorState,
                        onAddressSearch = appState::searchHostLocations,
                        onDraftChanged = { draft ->
                            editorState = editorState.copy(
                                draft = draft,
                                validationErrors = emptyList(),
                            )
                        },
                        onNew = {
                            appState.openHostCreateEdit()
                            route = appState.route
                        },
                        onEdit = { eventId ->
                            appState.openHostCreateEdit(eventId)
                            route = appState.route
                        },
                        onSaveDraft = {
                            editorState = appState.saveHostDraft(editorState.draft)
                        },
                        onPublish = {
                            editorState = appState.publishHostEvent(editorState.draft)
                        },
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
                        onBack = {
                            appState.returnToBrowse()
                            route = appState.route
                        },
                    )
                }
            }
        }
    }
}

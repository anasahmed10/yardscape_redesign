package com.naslabs.yardscape

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import com.naslabs.yardscape.data.SeededYardSaleData
import com.naslabs.yardscape.ui.AppDataAvailability
import com.naslabs.yardscape.ui.DiscoveryDisplayMode
import com.naslabs.yardscape.ui.MapAvailability
import com.naslabs.yardscape.ui.YardScapeAppState
import com.naslabs.yardscape.ui.YardScapePrimaryDestination
import com.naslabs.yardscape.ui.YardScapeTestTags
import com.naslabs.yardscape.ui.YardScapeTheme
import com.naslabs.yardscape.ui.ShopperSafetyScreen
import com.naslabs.yardscape.ui.HostAttendanceScreen
import com.naslabs.yardscape.ui.HostAttendancePolicy
import com.naslabs.yardscape.ui.HostAttendeeAction
import com.naslabs.yardscape.ui.HostAttendanceState
import com.naslabs.yardscape.ui.PendingHostAttendeeAction
import org.junit.Rule
import org.junit.Test

class MarketplaceAccessibilityTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun mapResultsSheetNamesItsStateAndProvidesKeyboardAccessibleActions() {
        val appState = YardScapeAppState().apply {
            updateMapAvailability(MapAvailability.Failed("Accessibility test fallback"))
        }
        composeRule.setContent { App(appState) }
        composeRule.onNodeWithTag(YardScapeTestTags.DiscoveryResultsSheet)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Collapsed"))
            .assert(hasCustomAction("Expand nearby sales"))
            .assertHeightIsAtLeast(48.dp)
        val visibleSaleTag = YardScapeTestTags.mapResult(SeededYardSaleData.FAMILY_GARAGE_EVENT_ID)
        composeRule.onNodeWithTag(YardScapeTestTags.DiscoveryResultsSheet)
            .performScrollToNode(hasTestTag(visibleSaleTag))
        composeRule.onNodeWithTag(visibleSaleTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Not selected"))
            .assert(hasCustomAction("Select on map"))
        val expandAction = composeRule.onAllNodesWithTag(YardScapeTestTags.DiscoveryResultsSheet)
            .fetchSemanticsNodes()
            .single()
            .config[SemanticsActions.CustomActions]
            .single { it.label == "Expand nearby sales" }
        composeRule.runOnIdle {
            check(expandAction.action.invoke())
        }
        composeRule.onNodeWithTag(YardScapeTestTags.DiscoveryResultsSheet)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Partially expanded"))
            .assert(hasCustomAction("Expand nearby sales"))
            .assert(hasCustomAction("Collapse nearby sales"))
        val selectAction = composeRule.onAllNodesWithTag(visibleSaleTag)
            .fetchSemanticsNodes()
            .single()
            .config[SemanticsActions.CustomActions]
            .single { it.label == "Select on map" }
        composeRule.runOnIdle {
            check(selectAction.action.invoke())
        }
        composeRule.onNodeWithTag(YardScapeTestTags.DiscoveryResultsSheet)
            .performScrollToNode(hasTestTag(visibleSaleTag))
        composeRule.onNodeWithTag(visibleSaleTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Selected"))
    }

    @Test
    fun primaryMarketplaceControlsMeetTheSharedMinimumTarget() {
        val appState = YardScapeAppState().apply {
            updateDiscoveryDisplayMode(DiscoveryDisplayMode.List)
        }
        composeRule.setContent { App(appState) }
        composeRule.onNodeWithText("List").assertHeightIsAtLeast(48.dp)
        val familySaleTag = YardScapeTestTags.browseEventCard(SeededYardSaleData.FAMILY_GARAGE_EVENT_ID)
        composeRule.onNodeWithTag(YardScapeTestTags.BrowseScreen)
            .performScrollToNode(hasTestTag(familySaleTag))
        composeRule.onNodeWithTag(familySaleTag)
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.onNodeWithTag(YardScapeTestTags.RsvpAction)
            .assertHeightIsAtLeast(48.dp)

        composeRule.onNodeWithTag(YardScapeTestTags.primaryDestination(YardScapePrimaryDestination.Messages))
            .performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Browse sales").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Browse sales").assertHeightIsAtLeast(48.dp)

        composeRule.onNodeWithTag(YardScapeTestTags.primaryDestination(YardScapePrimaryDestination.Host))
            .performClick()
        composeRule.onNodeWithContentDescription("Create a sale").assertHeightIsAtLeast(48.dp)

        composeRule.onNodeWithTag(YardScapeTestTags.primaryDestination(YardScapePrimaryDestination.Account))
            .performClick()
        composeRule.onNodeWithTag(YardScapeTestTags.AccountScreen)
            .performScrollToNode(androidx.compose.ui.test.hasText("Sign out"))
        composeRule.onNodeWithText("Sign out").assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun offlineBrowseStatusUsesAPoliteLiveRegion() {
        composeRule.setContent {
            App(YardScapeAppState(dataAvailability = AppDataAvailability.Offline))
        }

        composeRule.onAllNodes(hasPoliteLiveRegion()).assertCountEquals(1)
    }

    @Test
    fun unavailableSafetyRouteKeepsItsBackActionAtTheSharedMinimumTarget() {
        composeRule.setContent {
            YardScapeTheme {
                ShopperSafetyScreen(
                    state = null,
                    onBack = {},
                    onReasonChanged = {},
                    onDetailsChanged = {},
                    onSubmitReport = {},
                    onRequestBlockMutation = {},
                    onDismissBlockMutation = {},
                    onConfirmBlockMutation = {},
                )
            }
        }

        composeRule.onNodeWithText("Back").assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun hostWorkflowKeepsEditorialSurfacesAndNestedNavigationAtSharedTargetSize() {
        composeRule.setContent { App(YardScapeAppState()) }

        composeRule.onNodeWithTag(YardScapeTestTags.primaryDestination(YardScapePrimaryDestination.Host))
            .performClick()
        composeRule.onNodeWithTag(YardScapeTestTags.HostDashboardScreen)
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Create a sale")
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.onNodeWithTag(YardScapeTestTags.HostEditorScreen)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(YardScapeTestTags.EditorialBackNavigation)
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun attendanceConfirmationKeepsBothChoicesAtTheSharedMinimumTarget() {
        composeRule.setContent {
            YardScapeTheme {
                HostAttendanceScreen(
                    state = HostAttendanceState(
                        eventId = "event",
                        eventTitle = "Test sale",
                        eventPhoto = null,
                        policy = HostAttendancePolicy(),
                        attendees = emptyList(),
                    ),
                    pendingAction = PendingHostAttendeeAction(
                        eventId = "event",
                        shopperId = "shopper",
                        attendeeName = "Test shopper",
                        action = HostAttendeeAction.Revoke,
                    ),
                    onBack = {},
                    onRequestAction = { _, _, _ -> true },
                    onMessageAttendee = { _, _ -> true },
                    onDismissAction = {},
                    onConfirmAction = { true },
                )
            }
        }

        composeRule.onNodeWithText("Revoke location").assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithText("Go back").assertHeightIsAtLeast(48.dp)
    }

    private fun hasCustomAction(label: String): SemanticsMatcher =
        SemanticsMatcher("has custom action $label") { node ->
            node.config.contains(SemanticsActions.CustomActions) &&
                node.config[SemanticsActions.CustomActions].any { action -> action.label == label }
        }

    private fun hasPoliteLiveRegion(): SemanticsMatcher =
        SemanticsMatcher("has polite live region") { node ->
            node.config.contains(SemanticsProperties.LiveRegion) &&
                node.config[SemanticsProperties.LiveRegion] == LiveRegionMode.Polite
        }
}

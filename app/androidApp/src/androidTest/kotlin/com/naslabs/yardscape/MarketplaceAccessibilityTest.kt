package com.naslabs.yardscape

import androidx.activity.compose.setContent
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.naslabs.yardscape.ui.AppDataAvailability
import com.naslabs.yardscape.ui.YardScapeAppState
import com.naslabs.yardscape.ui.YardScapePrimaryDestination
import com.naslabs.yardscape.ui.YardScapeTestTags
import com.naslabs.yardscape.ui.YardScapeTheme
import com.naslabs.yardscape.ui.ShopperSafetyScreen
import org.junit.Rule
import org.junit.Test

class MarketplaceAccessibilityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun mapResultsSheetNamesItsStateAndProvidesKeyboardAccessibleActions() {
        composeRule.onNodeWithTag(YardScapeTestTags.DiscoveryResultsSheet)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Collapsed"))
            .assert(hasCustomAction("Expand nearby sales"))
            .assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithTag(YardScapeTestTags.mapResult("family-garage-sale"))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Not selected"))
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
        composeRule.onNodeWithTag(YardScapeTestTags.mapResult("family-garage-sale"))
            .performClick()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Selected"))
    }

    @Test
    fun primaryMarketplaceControlsMeetTheSharedMinimumTarget() {
        composeRule.onNodeWithText("List").performClick()
        composeRule.onNodeWithTag(YardScapeTestTags.browseEventCard("family-garage-sale"))
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
        composeRule.onNodeWithText("Sign out").assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun offlineBrowseStatusUsesAPoliteLiveRegion() {
        composeRule.activity.setContent {
            App(YardScapeAppState(dataAvailability = AppDataAvailability.Offline))
        }

        composeRule.onAllNodes(hasPoliteLiveRegion()).assertCountEquals(1)
    }

    @Test
    fun unavailableSafetyRouteKeepsItsBackActionAtTheSharedMinimumTarget() {
        composeRule.activity.setContent {
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

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
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.naslabs.yardscape.ui.AppDataAvailability
import com.naslabs.yardscape.ui.YardScapeAppState
import com.naslabs.yardscape.ui.YardScapePrimaryDestination
import com.naslabs.yardscape.ui.YardScapeTestTags
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
            .assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithTag(YardScapeTestTags.primaryDestination(YardScapePrimaryDestination.Host))
            .assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithTag(YardScapeTestTags.primaryDestination(YardScapePrimaryDestination.Account))
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun offlineBrowseStatusUsesAPoliteLiveRegion() {
        composeRule.activity.setContent {
            App(YardScapeAppState(dataAvailability = AppDataAvailability.Offline))
        }

        composeRule.onAllNodes(hasPoliteLiveRegion()).assertCountEquals(1)
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

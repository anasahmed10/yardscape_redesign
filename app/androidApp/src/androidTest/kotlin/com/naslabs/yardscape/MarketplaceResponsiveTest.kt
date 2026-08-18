package com.naslabs.yardscape

import androidx.activity.compose.setContent
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.naslabs.yardscape.data.SeededYardSaleData
import com.naslabs.yardscape.ui.YardScapeAppState
import com.naslabs.yardscape.ui.YardScapePrimaryDestination
import com.naslabs.yardscape.ui.YardScapeTestTags
import org.junit.Rule
import org.junit.Test

class MarketplaceResponsiveTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun primaryWorkflowsRemainReachableAndInsideCompactRuntimeBounds() {
        val appState = YardScapeAppState()
        composeRule.activity.setContent {
            Layout(
                content = { App(appState) },
                modifier = Modifier.testTag(COMPACT_HARNESS_TAG),
            ) { measurables, constraints ->
                val compactWidth = 390.dp.roundToPx()
                check(constraints.maxWidth >= compactWidth) {
                    "Android test host must fit the explicit 390dp compact harness"
                }
                val placeable = measurables.single().measure(
                    constraints.copy(minWidth = compactWidth, maxWidth = compactWidth),
                )
                layout(compactWidth, placeable.height) {
                    placeable.placeRelative(0, 0)
                }
            }
        }
        composeRule.onNodeWithTag(COMPACT_HARNESS_TAG)
            .assertIsDisplayed()
            .assertWidthIsEqualTo(390.dp)
        val shellBounds = composeRule.onNodeWithTag(YardScapeTestTags.AppShell)
            .assertIsDisplayed()
            .fetchSemanticsNode().boundsInRoot

        YardScapePrimaryDestination.entries.forEach { destination ->
            composeRule.onNodeWithTag(YardScapeTestTags.primaryDestination(destination))
                .assertIsDisplayed()
                .assertHeightIsAtLeast(48.dp)
                .also { node -> assertInside(shellBounds, node.fetchSemanticsNode().boundsInRoot, destination.label) }
                .performClick()
            composeRule.runOnIdle { check(appState.activePrimaryDestination == destination) }
        }

        composeRule.onNodeWithTag(
            YardScapeTestTags.primaryDestination(YardScapePrimaryDestination.Browse),
        ).performClick()
        composeRule.onNodeWithText("List").assertHeightIsAtLeast(48.dp).performClick()
        composeRule.onNodeWithTag(
            YardScapeTestTags.browseEventCard(SeededYardSaleData.FAMILY_GARAGE_EVENT_ID),
        ).performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp).performClick()
        composeRule.onNodeWithTag(YardScapeTestTags.RsvpAction)
            .performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp).performClick()
        composeRule.onNodeWithTag(YardScapeTestTags.RsvpConfirmAction)
            .performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp)

        composeRule.onNodeWithTag(
            YardScapeTestTags.primaryDestination(YardScapePrimaryDestination.MyFinds),
        ).performClick()
        composeRule.onNodeWithText("RSVPs").assertHeightIsAtLeast(48.dp).performClick()
        composeRule.onNodeWithContentDescription("Cancel RSVP")
            .performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp).performClick()
        composeRule.onNodeWithText("Keep RSVP")
            .assertIsDisplayed().assertHeightIsAtLeast(48.dp).performClick()

        composeRule.onNodeWithTag(
            YardScapeTestTags.primaryDestination(YardScapePrimaryDestination.Host),
        ).performClick()
        composeRule.onNodeWithContentDescription("Create a sale")
            .performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp).performClick()
        composeRule.onNodeWithText("Continue")
            .performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp)

        composeRule.onNodeWithTag(
            YardScapeTestTags.primaryDestination(YardScapePrimaryDestination.Messages),
        ).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Browse sales").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Browse sales")
            .performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp).performClick()

        composeRule.onNodeWithTag(
            YardScapeTestTags.primaryDestination(YardScapePrimaryDestination.Account),
        ).performClick()
        composeRule.onNodeWithText("Sign out")
            .performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp)

        composeRule.onNodeWithTag(
            YardScapeTestTags.primaryDestination(YardScapePrimaryDestination.Browse),
        ).performClick()
        composeRule.onNodeWithTag(YardScapeTestTags.BrowseScreen).assertIsDisplayed()
    }

    private fun assertInside(container: Rect, child: Rect, label: String) {
        check(child.left >= container.left) { "$label starts outside the app shell" }
        check(child.top >= container.top) { "$label starts above the app shell" }
        check(child.right <= container.right) { "$label ends outside the app shell" }
        check(child.bottom <= container.bottom) { "$label ends below the app shell" }
    }

    private companion object {
        const val COMPACT_HARNESS_TAG = "marketplace-compact-harness"
    }
}

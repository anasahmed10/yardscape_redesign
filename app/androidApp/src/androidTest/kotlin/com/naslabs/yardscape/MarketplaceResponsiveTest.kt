package com.naslabs.yardscape

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import com.naslabs.yardscape.data.SeededYardSaleData
import com.naslabs.yardscape.ui.YardScapeAppState
import com.naslabs.yardscape.ui.DiscoveryDisplayMode
import com.naslabs.yardscape.ui.YardScapePrimaryDestination
import com.naslabs.yardscape.ui.YardScapeTestTags
import org.junit.Rule
import org.junit.Test

class MarketplaceResponsiveTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun primaryWorkflowsRemainReachableAtCompactRuntimeBounds() {
        val appState = YardScapeAppState().apply {
            updateDiscoveryDisplayMode(DiscoveryDisplayMode.List)
        }
        composeRule.setContent {
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
        composeRule.onNodeWithTag(YardScapeTestTags.AppShell).assertIsDisplayed()
        composeRule.onNodeWithText("YardScape").assertIsDisplayed()
        composeRule.onNodeWithText("Browse sales").assertIsDisplayed()
        composeRule.onNodeWithText("Host a sale").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithContentDescription("Search sales")
            .assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithText("All dates")
            .assertIsDisplayed().assertHeightIsAtLeast(48.dp).assertIsSelected()
        composeRule.onNodeWithText("Today").assertIsNotSelected()
        composeRule.onNodeWithText("Saved").assertIsDisplayed()

        YardScapePrimaryDestination.entries.forEach { destination ->
            composeRule.onNodeWithTag(YardScapeTestTags.primaryDestination(destination))
                .assertHeightIsAtLeast(48.dp)
            composeRule.runOnIdle {
                appState.navigateTo(destination)
                check(appState.activePrimaryDestination == destination) {
                    "Expected ${destination.name}, was ${appState.activePrimaryDestination.name}"
                }
            }
        }

        composeRule.runOnIdle { appState.navigateTo(YardScapePrimaryDestination.Browse) }
        composeRule.onNodeWithTag(YardScapeTestTags.BrowseScreen)
            .performScrollToNode(hasText("List"))
        composeRule.onNodeWithText("List").assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithTag(
            YardScapeTestTags.browseEventCard(SeededYardSaleData.FAMILY_GARAGE_EVENT_ID),
        ).performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp).performClick()
        composeRule.onNodeWithTag(YardScapeTestTags.EventDetailScreen)
            .performScrollToNode(hasTestTag(YardScapeTestTags.RsvpAction))
        composeRule.onNodeWithTag(YardScapeTestTags.RsvpAction)
            .assertIsDisplayed().assertHeightIsAtLeast(48.dp).performClick()
        composeRule.onNodeWithTag(YardScapeTestTags.RsvpScreen)
            .performScrollToNode(hasTestTag(YardScapeTestTags.RsvpConfirmAction))
        composeRule.onNodeWithTag(YardScapeTestTags.RsvpConfirmAction)
            .assertIsDisplayed().assertHeightIsAtLeast(48.dp)

        composeRule.onNodeWithTag(
            YardScapeTestTags.primaryDestination(YardScapePrimaryDestination.MyFinds),
        ).performClick()
        composeRule.onNodeWithText("RSVPs").assertHeightIsAtLeast(48.dp).performClick()
        composeRule.onNodeWithContentDescription("Cancel RSVP")
            .performScrollTo()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
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

        composeRule.runOnIdle { appState.navigateTo(YardScapePrimaryDestination.Account) }
        composeRule.onNodeWithTag(YardScapeTestTags.AccountScreen)
            .performScrollToNode(hasText("Sign out"))
        composeRule.onNodeWithText("Sign out")
            .assertIsDisplayed().assertHeightIsAtLeast(48.dp)

        composeRule.runOnIdle { appState.navigateTo(YardScapePrimaryDestination.Browse) }
        composeRule.onNodeWithTag(YardScapeTestTags.BrowseScreen).assertIsDisplayed()
    }

    @Test
    fun compactNonBrowseShellUsesEditorialHeaderAndSemanticSavedSelection() {
        val appState = YardScapeAppState().apply {
            navigateTo(YardScapePrimaryDestination.MyFinds)
        }
        composeRule.setContent {
            Layout(
                content = { App(appState) },
                modifier = Modifier.testTag(COMPACT_HARNESS_TAG),
            ) { measurables, constraints ->
                val compactWidth = 390.dp.roundToPx()
                val placeable = measurables.single().measure(
                    constraints.copy(minWidth = compactWidth, maxWidth = compactWidth),
                )
                layout(compactWidth, placeable.height) {
                    placeable.placeRelative(0, 0)
                }
            }
        }

        composeRule.onNodeWithTag("editorial-header").assertIsDisplayed()
        composeRule.onAllNodesWithText("YardScape").assertCountEquals(0)
        composeRule.onAllNodesWithText("My Finds").assertCountEquals(1)
        composeRule.onNodeWithTag("editorial-segment-saved")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .assertIsSelected()
        composeRule.onNodeWithTag("editorial-segment-rsvps")
            .assertHeightIsAtLeast(48.dp)
            .assertIsNotSelected()

        composeRule.runOnIdle { appState.openEvent(SeededYardSaleData.FAMILY_GARAGE_EVENT_ID) }
        composeRule.onNodeWithTag("editorial-back-navigation")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
    }
    private companion object {
        const val COMPACT_HARNESS_TAG = "marketplace-compact-harness"
    }
}

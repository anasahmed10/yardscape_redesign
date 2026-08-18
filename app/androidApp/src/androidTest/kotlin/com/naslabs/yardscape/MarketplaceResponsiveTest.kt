package com.naslabs.yardscape

import androidx.activity.compose.setContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
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
        composeRule.activity.setContent { App(appState) }
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
        composeRule.onNodeWithTag(YardScapeTestTags.BrowseScreen).assertIsDisplayed()
    }

    private fun assertInside(container: Rect, child: Rect, label: String) {
        check(child.left >= container.left) { "$label starts outside the app shell" }
        check(child.top >= container.top) { "$label starts above the app shell" }
        check(child.right <= container.right) { "$label ends outside the app shell" }
        check(child.bottom <= container.bottom) { "$label ends below the app shell" }
    }
}

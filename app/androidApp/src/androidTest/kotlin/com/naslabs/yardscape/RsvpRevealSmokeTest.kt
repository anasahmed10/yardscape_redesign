package com.naslabs.yardscape

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.naslabs.yardscape.data.SeededYardSaleData
import com.naslabs.yardscape.ui.YardScapeTestTags
import org.junit.Rule
import org.junit.Test

class RsvpRevealSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun browseDetailRsvpRevealShowsExactAddressOnlyAfterAcceptance() {
        composeRule.onNodeWithTag(YardScapeTestTags.BrowseScreen)
            .assertIsDisplayed()

        composeRule.onNodeWithTag(
            YardScapeTestTags.browseEventCard(SeededYardSaleData.FAMILY_GARAGE_EVENT_ID),
        ).performClick()

        composeRule.onNodeWithTag(YardScapeTestTags.LocationAccessPanel)
            .assertIsDisplayed()
        composeRule.onAllNodesWithText("123 Cedar Street", substring = true)
            .assertCountEquals(0)

        composeRule.onNodeWithTag(YardScapeTestTags.RsvpAction)
            .performClick()
        composeRule.onNodeWithTag(YardScapeTestTags.RsvpConfirmAction)
            .performClick()

        composeRule.onNodeWithTag(YardScapeTestTags.ExactLocationContent)
            .assertIsDisplayed()
            .assertTextContains("123 Cedar Street", substring = true)
    }
}

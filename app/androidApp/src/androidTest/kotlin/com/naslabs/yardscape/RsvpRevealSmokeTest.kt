package com.naslabs.yardscape

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.activity.compose.setContent
import com.naslabs.yardscape.data.MarketplaceMessagingAccessSource
import com.naslabs.yardscape.data.SeededMarketplaceMessagingRepository
import com.naslabs.yardscape.data.SeededMessageOutcome
import com.naslabs.yardscape.data.SeededMessagingBehavior
import com.naslabs.yardscape.data.SeededYardSaleData
import com.naslabs.yardscape.domain.EventStatus
import com.naslabs.yardscape.domain.LocationVisibility
import com.naslabs.yardscape.domain.MarketplaceConversationKey
import com.naslabs.yardscape.domain.MessagingAccessContext
import com.naslabs.yardscape.domain.MessagingActor
import com.naslabs.yardscape.domain.RsvpStatus
import com.naslabs.yardscape.domain.UserRole
import com.naslabs.yardscape.ui.YardScapeTestTags
import com.naslabs.yardscape.ui.YardScapeAppState
import com.naslabs.yardscape.ui.YardScapePrimaryDestination
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class RsvpRevealSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun browseDetailRsvpRevealShowsExactAddressOnlyAfterAcceptance() {
        val appState = YardScapeAppState()
        composeRule.activity.setContent { App(appState) }

        composeRule.onNodeWithTag(YardScapeTestTags.BrowseScreen)
            .assertIsDisplayed()

        composeRule.onNodeWithTag(
            YardScapeTestTags.browseEventCard(SeededYardSaleData.FAMILY_GARAGE_EVENT_ID),
        ).performClick()

        composeRule.onNodeWithTag(YardScapeTestTags.LocationAccessPanel)
            .assertIsDisplayed()
        composeRule.onAllNodesWithText("123 Cedar Street", substring = true)
            .assertCountEquals(0)
        composeRule.onAllNodesWithTag(YardScapeTestTags.DirectionsAction)
            .assertCountEquals(0)

        composeRule.onNodeWithTag(YardScapeTestTags.RsvpAction)
            .performClick()
        composeRule.onNodeWithTag(YardScapeTestTags.RsvpConfirmAction)
            .performClick()

        composeRule.onNodeWithTag(YardScapeTestTags.ExactLocationContent)
            .assertIsDisplayed()
            .assertTextContains("123 Cedar Street", substring = true)
        composeRule.onNodeWithTag(YardScapeTestTags.DirectionsAction)
            .assertIsDisplayed()

        composeRule.runOnIdle {
            check(appState.revokeRsvpAccess(SeededYardSaleData.FAMILY_GARAGE_EVENT_ID))
        }
        composeRule.onAllNodesWithTag(YardScapeTestTags.ExactLocationContent)
            .assertCountEquals(0)
        composeRule.onAllNodesWithTag(YardScapeTestTags.DirectionsAction)
            .assertCountEquals(0)
        composeRule.onAllNodesWithText("123 Cedar Street", substring = true)
            .assertCountEquals(0)
        composeRule.onNodeWithText("Access revoked").assertIsDisplayed()
    }

    @Test
    fun acceptedMessagingSupportsFailedRetryAndClosesComposerAfterAccessRevocation() {
        val conversationKey = MarketplaceConversationKey(
            SeededYardSaleData.ESTATE_TOOLS_EVENT_ID,
            SeededYardSaleData.SHOPPER_WITH_ACCEPTED_ACCESS_ID,
        )
        val repository = SeededMarketplaceMessagingRepository(
            accessSource = object : MarketplaceMessagingAccessSource {
                override fun accessContextFor(key: MarketplaceConversationKey): MessagingAccessContext? =
                    key.takeIf { it == conversationKey }?.let {
                        MessagingAccessContext(
                            conversationKey = conversationKey,
                            hostId = SeededYardSaleData.HOST_MARIN_ID,
                            eventStatus = EventStatus.PUBLISHED,
                            eventHasEnded = false,
                            rsvpStatus = RsvpStatus.ACCEPTED,
                            locationVisibility = LocationVisibility.RSVP_ACCEPTED,
                            isBlocked = false,
                        )
                    }
            },
            behavior = SeededMessagingBehavior(
                deliveryOutcomes = listOf(SeededMessageOutcome.Offline, SeededMessageOutcome.Success),
            ),
        )
        runBlocking {
            repository.sendMessage(
                conversationKey,
                MessagingActor(conversationKey.shopperId, UserRole.SHOPPER),
                "Will this fit in a hatchback?",
                SeededYardSaleData.BASE_NOW_EPOCH_MILLIS,
            )
        }
        val appState = YardScapeAppState(
            shopperId = conversationKey.shopperId,
            messagingRepository = repository,
        )

        composeRule.activity.setContent { App(appState) }
        composeRule.runOnIdle { appState.navigateTo(YardScapePrimaryDestination.Messages) }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Marin Estate Tools and Records").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Marin Estate Tools and Records")
            .performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasContentDescription("Message composer")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Retry").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Retry").fetchSemanticsNodes().isEmpty()
        }
        composeRule.onAllNodesWithText("Sent").assertCountEquals(1)
        composeRule.onNodeWithContentDescription("Message composer")
            .performTextInput("Can I bring a trailer?")
        composeRule.onNodeWithContentDescription("Send message").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Sent").fetchSemanticsNodes().size == 2
        }
        composeRule.onAllNodesWithText("Can I bring a trailer?").assertCountEquals(1)
        composeRule.onNodeWithContentDescription("Message composer")
            .performTextInput("Private draft that must close")
        composeRule.onAllNodesWithText("Private draft that must close").assertCountEquals(1)

        composeRule.runOnIdle { appState.revokeRsvpAccess(conversationKey.eventId) }
        composeRule.onAllNodes(hasContentDescription("Message composer")).assertCountEquals(0)
        composeRule.onAllNodesWithText("Private draft that must close").assertCountEquals(0)
        composeRule.onNodeWithText("Location access revoked").assertIsDisplayed()
    }
}

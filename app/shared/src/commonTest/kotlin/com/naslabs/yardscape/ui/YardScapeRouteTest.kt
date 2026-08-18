package com.naslabs.yardscape.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class YardScapeRouteTest {
    @Test
    fun findPathsResolveToTheMatchingMyFindsSection() {
        assertEquals(
            YardScapeRoute.MyFinds(MyFindsSection.Saved),
            YardScapeRoute.fromPath("/finds"),
        )
        assertEquals(
            YardScapeRoute.MyFinds(MyFindsSection.Saved),
            YardScapeRoute.fromPath("/saved"),
        )
        assertEquals(
            YardScapeRoute.MyFinds(MyFindsSection.Rsvps),
            YardScapeRoute.fromPath("/rsvps"),
        )
    }

    @Test
    fun detailRoutesPreserveMyFindsAsTheirOrigin() {
        val route = YardScapeRoute.EventDetail(
            eventId = "event-123",
            origin = YardScapePrimaryDestination.MyFinds,
        )

        assertEquals(YardScapePrimaryDestination.MyFinds, route.primaryDestination)
    }

    @Test
    fun messageThreadPathUsesOnlyTheOpaqueConversationIdentity() {
        val route = YardScapeRoute.MessageThread("conversation-0000002a")

        assertEquals("/messages/conversation-0000002a", route.path)
        assertEquals(YardScapePrimaryDestination.Messages, route.primaryDestination)
        assertEquals(route, YardScapeRoute.fromPath(route.path))
        assertFalse(route.path.contains("event-family-garage"))
        assertFalse(route.path.contains("shopper-accepted"))
    }

    @Test
    fun conversationIdsAcceptOnlyCanonicalOpaqueValues() {
        assertEquals(
            "conversation-0000002a",
            MarketplaceConversationId.parse("conversation-0000002a")?.value,
        )

        listOf(
            "event-family-garage",
            "shopper-accepted",
            "conversation-event-family-garage",
            "conversation-0000002A",
            "conversation-123",
            "conversation-0000002a/extra",
            "conversation-0000002a?event=secret",
            "conversation-0000002a#shopper",
            ".",
            "..",
            "%2e%2e",
        ).forEach { candidate ->
            assertNull(MarketplaceConversationId.parse(candidate), candidate)
            assertNull(YardScapeRoute.fromPath("/messages/$candidate"), candidate)
        }
    }
}

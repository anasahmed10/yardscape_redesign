package com.naslabs.yardscape.ui

import kotlin.test.Test
import kotlin.test.assertEquals

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
}

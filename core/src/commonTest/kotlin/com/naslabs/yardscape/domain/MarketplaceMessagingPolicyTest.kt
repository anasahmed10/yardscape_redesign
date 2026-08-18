package com.naslabs.yardscape.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MarketplaceMessagingPolicyTest {
    @Test
    fun acceptedShopperWithActiveRevealCanComposeForOwnPublishedEvent() {
        assertEquals(
            MessagingComposerAccess.Open,
            MarketplaceMessagingPolicy.composerAccess(openContext(), shopper()),
        )
    }

    @Test
    fun shopperCannotComposeForAnotherShoppersConversation() {
        val access = MarketplaceMessagingPolicy.composerAccess(
            openContext(),
            shopper(userId = "shopper-other"),
        )

        assertClosed(MessagingClosedReason.NOT_PARTICIPANT, access)
    }

    @Test
    fun missingOrNonAcceptedRsvpClosesShopperComposer() {
        assertClosed(
            MessagingClosedReason.RSVP_REQUIRED,
            MarketplaceMessagingPolicy.composerAccess(
                openContext(rsvpStatus = null, locationVisibility = null),
                shopper(),
            ),
        )

        listOf(
            RsvpStatus.REQUESTED,
            RsvpStatus.WAITLISTED,
            RsvpStatus.FULL,
            RsvpStatus.DECLINED,
            RsvpStatus.CANCELLED,
            RsvpStatus.REMOVED,
        ).forEach { status ->
            assertClosed(
                MessagingClosedReason.RSVP_NOT_ACCEPTED,
                MarketplaceMessagingPolicy.composerAccess(
                    openContext(rsvpStatus = status),
                    shopper(),
                ),
            )
        }
    }

    @Test
    fun inactiveRevokedOrExpiredRevealClosesShopperComposer() {
        listOf(
            LocationVisibility.PUBLIC_APPROXIMATION to MessagingClosedReason.LOCATION_ACCESS_INACTIVE,
            LocationVisibility.RSVP_REQUESTED to MessagingClosedReason.LOCATION_ACCESS_INACTIVE,
            LocationVisibility.REVOKED to MessagingClosedReason.LOCATION_ACCESS_REVOKED,
            LocationVisibility.EXPIRED to MessagingClosedReason.LOCATION_ACCESS_EXPIRED,
        ).forEach { (visibility, expectedReason) ->
            assertClosed(
                expectedReason,
                MarketplaceMessagingPolicy.composerAccess(
                    openContext(locationVisibility = visibility),
                    shopper(),
                ),
            )
        }
    }

    @Test
    fun blockedConversationClosesComposer() {
        assertClosed(
            MessagingClosedReason.BLOCKED,
            MarketplaceMessagingPolicy.composerAccess(openContext(isBlocked = true), shopper()),
        )
    }

    @Test
    fun unavailableOrEndedEventClosesComposer() {
        listOf(
            EventStatus.DRAFT to MessagingClosedReason.EVENT_NOT_PUBLISHED,
            EventStatus.CANCELLED to MessagingClosedReason.EVENT_CANCELLED,
            EventStatus.COMPLETED to MessagingClosedReason.EVENT_COMPLETED,
            EventStatus.HIDDEN to MessagingClosedReason.EVENT_HIDDEN,
        ).forEach { (status, expectedReason) ->
            assertClosed(
                expectedReason,
                MarketplaceMessagingPolicy.composerAccess(openContext(eventStatus = status), shopper()),
            )
        }

        assertClosed(
            MessagingClosedReason.EVENT_ENDED,
            MarketplaceMessagingPolicy.composerAccess(openContext(eventHasEnded = true), shopper()),
        )
    }

    @Test
    fun owningHostCanComposeButAnotherHostCannot() {
        assertEquals(
            MessagingComposerAccess.Open,
            MarketplaceMessagingPolicy.composerAccess(openContext(), host()),
        )
        assertClosed(
            MessagingClosedReason.NOT_EVENT_HOST,
            MarketplaceMessagingPolicy.composerAccess(
                openContext(),
                host(userId = "host-other"),
            ),
        )
    }

    private fun assertClosed(
        expectedReason: MessagingClosedReason,
        access: MessagingComposerAccess,
    ) {
        assertEquals(
            expectedReason,
            assertIs<MessagingComposerAccess.Closed>(access).reason,
        )
    }

    private fun shopper(userId: String = SHOPPER_ID): MessagingActor =
        MessagingActor(userId = userId, role = UserRole.SHOPPER)

    private fun host(userId: String = HOST_ID): MessagingActor =
        MessagingActor(userId = userId, role = UserRole.HOST)

    private fun openContext(
        eventStatus: EventStatus = EventStatus.PUBLISHED,
        eventHasEnded: Boolean = false,
        rsvpStatus: RsvpStatus? = RsvpStatus.ACCEPTED,
        locationVisibility: LocationVisibility? = LocationVisibility.RSVP_ACCEPTED,
        isBlocked: Boolean = false,
    ): MessagingAccessContext = MessagingAccessContext(
        conversationKey = MarketplaceConversationKey(
            eventId = EVENT_ID,
            shopperId = SHOPPER_ID,
        ),
        hostId = HOST_ID,
        eventStatus = eventStatus,
        eventHasEnded = eventHasEnded,
        rsvpStatus = rsvpStatus,
        locationVisibility = locationVisibility,
        isBlocked = isBlocked,
    )

    private companion object {
        const val EVENT_ID = "event-1"
        const val SHOPPER_ID = "shopper-1"
        const val HOST_ID = "host-1"
    }
}

# Mock Account, Trust, and Safety Surfaces

The shared account workspace models the states needed to test authenticated product flows without collecting credentials or implying that production authentication exists. Public Browse and public Event Detail remain usable while signed out. RSVP, protected-location access, host management, reporting, and blocking all use one `AccountSessionController` gate so future repositories can replace the seeded implementation without moving policy into platform UI.

## Session states

| State | Public access | Protected access |
| --- | --- | --- |
| Signed out | Browse and public Event Detail remain available. | The app explains why sign-in is needed and remembers a safe public return path. |
| Signed in | Public surfaces plus role-appropriate mock workflows. | Protected actions are allowed by the mock gate; underlying RSVP and location policy still decides whether an exact address may be shown. |
| Expired | Browse and public Event Detail remain available. | Directions, RSVP management, attendee actions, and cached exact-location presentation are cleared. |

The mock sign-in buttons choose a seeded shopper or host profile. They do not accept, store, or transmit passwords, tokens, phone numbers, or email addresses. Notification switches are local UI state only.

## Trust language

Profiles intentionally separate confirmed facts from community activity:

- Confirmed facts describe only the seeded mock-session fact, such as a confirmed contact method.
- Community activity describes participation, such as hosted sales or completed RSVPs.
- Neither group is presented as an identity guarantee, background check, endorsement, or promise of safety.

This distinction should remain explicit when issue #25 introduces real authentication. Server-provided assurance facts should use precise provenance and must not be inferred from ratings or marketplace activity.

## Safety placement

General marketplace guidance appears in Account. A shorter reminder appears next to the high-risk exact-location area on Event Detail: visit only during listed hours, stay out of private areas, and leave if a situation feels unsafe. This is product guidance for the mock workflow, not final legal or store copy.

Report and Block are mock-backed protected actions. Report requires a reason, accepts up to 500 optional characters, and distinguishes validation, offline, server-error, and successful receipt states. Its UI warns testers not to enter exact addresses, access instructions, private contact details, or payment information.

Blocking requires confirmation. A successful block immediately removes every event from that host from discovery, closes active directions, revokes affected RSVP reveal grants, and suppresses exact locations even if repository mutation lags. Unblocking restores discovery only; it never restores the former RSVP or exact-location grant. Failed mutations leave the previous state intact and never display success copy.

These controls use `ShopperSafetyRepository`, currently backed by deterministic seeded results. The interface keeps report and block mutations separate so authenticated API implementations can preserve moderation privacy and block-list semantics without moving those rules into platform UI.

## Backend follow-ups

The authenticated app and API should preserve these boundaries:

- Represent anonymous, active, and expired sessions explicitly.
- Return a safe post-sign-in destination, never a URL containing exact location data.
- Clear protected location, directions, and host-attendee caches on expiry and sign-out.
- Keep public event retrieval independent from authentication availability.
- Provide assurance facts separately from reputation or activity signals.
- Let reporting and blocking reuse the same authenticated-action contract while keeping their moderation data separate.
- Treat the authenticated user's block list as an overriding deny in browse, detail, RSVP, directions, and protected-location responses.
- Make unblock idempotent and discovery-only; require a new RSVP before issuing any new location grant.
- Return an opaque report receipt and field-level validation without echoing potentially sensitive report details.

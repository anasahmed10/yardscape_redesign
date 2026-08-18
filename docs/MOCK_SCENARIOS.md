# Mock Scenario Catalog

The shared mock scenario catalog gives UX tests and Compose previews deterministic app states without a backend. Each launch creates a fresh `YardSaleEventRepository`, then injects it with the scenario's user role, startup route, availability, and capacity policy into `YardScapeAppState`.

Production Android, iOS, JS, and Wasm entry points still call `App()` and contain no scenario selector or debug control.

## Launching a scenario

Use a scenario from common tests or a local debug-only preview:

```kotlin
val appState = MockScenarioCatalog.createAppState(MockScenarioId.PendingRsvp)

@Composable
fun PendingRsvpPreview() {
    App(appState)
}
```

Do not replace a production entry point with a scenario launch. A future debug menu can call the same catalog without changing repository or privacy policy logic.

## Catalog

| Scenario | Intended assertions |
| --- | --- |
| New shopper | Starts on Browse; no RSVP grants exact-location access. |
| Populated browse | Two upcoming public previews appear; previews contain approximate areas only. |
| No nearby events | Browse is empty; host controls remain available. |
| Pending RSVP | Detail reports a pending request; exact location stays hidden. |
| Accepted location access | My RSVPs shows accepted access with the protected location and directions entry point. |
| Waitlisted RSVP | My RSVPs reports waitlisted and keeps exact location hidden. |
| Declined RSVP | My RSVPs reports declined and keeps exact location hidden. |
| Cancelled RSVP | My RSVPs reports shopper cancellation and clears exact location. |
| Revoked location access | My RSVPs reports revoked access and clears exact location. |
| Expired location access | My RSVPs reports expired access after the sale and clears exact location. |
| Accepted location access | The accepted shopper can see the protected exact location. |
| Revoked location access | Detail reports revocation; exact location stays hidden. |
| Expired location access | Detail reports expiry after the sale; exact location stays hidden. |
| Cancelled event | Detail reports cancellation; cancellation suppresses exact location. |
| Event at capacity | Detail reports capacity, disables the RSVP action, and My RSVPs represents the full state. |
| Host with drafts | Starts in the host editor with a draft available. |
| Host with pending attendees | The host event has one pending attendee without a reveal grant. |
| Signed-out account | Public browsing remains available; protected actions explain why sign-in is required. |
| Expired account session | Protected state is cleared while public browsing remains available. |
| Shopper profile | Confirmed facts remain separate from community activity. |
| Host profile | Host trust language avoids identity or safety guarantees. |
| Report validation | A missing reason is rejected and no success is claimed. |
| Report offline | The failed submission stays retryable and does not alter discovery. |
| Report server error | The service failure stays visible and does not claim receipt. |
| Block host | Confirmation explains the consequence; success removes every sale from that host and clears protected access. |
| Block offline | Failure leaves discovery, RSVP, and location state unchanged. |
| Offline | Browse reports offline state while privacy-safe seeded previews remain visible. |
| Recoverable refresh error | Browse reports a recoverable error and retry guidance. |

## Messaging catalog

Messaging scenarios document the route reached after repository authorization, the active actor,
composer state, absence of protected location data, and the recovery action. Route descriptions
refer only to an authorized opaque conversation ID; they never include an event or shopper ID.

| Scenario | Expected route | Actor | Composer | Protected data | Recovery action |
| --- | --- | --- | --- | --- | --- |
| Accepted unread messages | Messages / authorized thread | Shopper | Open | Absent | Mark the unread message read. |
| Failed send with retry | Messages / authorized thread | Shopper | Open | Absent | Retry the failed message. |
| Cancelled RSVP messages | Messages / closed thread | Shopper | Closed: RSVP not accepted | Absent | Return to Messages. |
| Revoked access messages | Messages / closed thread | Shopper | Closed: location access revoked | Absent | Return to Messages. |
| Expired access messages | Messages / closed thread | Shopper | Closed: location access expired | Absent | Return to Messages. |
| Cancelled event messages | Messages / closed thread | Shopper | Closed: event cancelled | Absent | Return to Messages. |
| Blocked messages | Messages | Shopper | Closed: blocked | Absent | Return to Messages. |
| Signed-out messages | Account | Signed out | Sign-in required | Absent | Sign in to load Messages. |
| Host-owned messages | Host attendees → Messages / authorized thread | Owning host | Open | Absent | Use Message attendee. |

Scenario metadata contains no protected fixture values. Protected locations remain behind the repository interface, and app-visible detail state contains an exact address only for an accepted, active mock session (or the signed-in owning host editor). A successful block is an overriding local deny for every event from that host; unblocking restores discovery only and never revives the previous RSVP or reveal grant. Pending-attendee counts are also gated to the signed-in event owner.

The host attendee screen is a host-only workspace. It uses a public event photo/title projection,
host-only RSVP state, and a policy-derived exact-access label. `Message attendee` is shown only
when the active host has open conversation access; selection resolves the conversation through the
repository and routes solely with the returned opaque ID. It cannot expose the exact address,
coordinates, units, private instructions, or attendee information through shopper/public models.

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
| Offline | Browse reports offline state while privacy-safe seeded previews remain visible. |
| Recoverable refresh error | Browse reports a recoverable error and retry guidance. |

Scenario metadata contains no protected fixture values. Protected locations remain behind the repository interface, and app-visible detail state contains an exact address only for the accepted-access scenario (or the owning host editor). Pending-attendee counts are also gated to the event's owning host role.

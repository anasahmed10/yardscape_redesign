# Shared App Shell

The shared Compose shell keeps shopper and host mock workflows reachable on Android, iOS, JS, and Wasm without assuming a backend session.

## Primary destinations

| Destination | Context | Current purpose |
| --- | --- | --- |
| Browse | Shopper workspace | Public previews and the existing Detail → RSVP flow. |
| Saved | Shopper workspace | Session-persistent saved public previews plus the nested My RSVPs lifecycle view. |
| Host | Host workspace | A distinct dashboard for creating and editing host-owned events. |
| Account | Account workspace | Stable entry point for upcoming mock profile, trust, and safety surfaces. |

The active primary destination remains selected for nested routes. My RSVPs (`/rsvps`) keeps Saved selected and returns to Saved on Back. Event Detail and RSVP retain either Browse or Saved as their shopper origin; host create/edit belongs to Host. Back from RSVP returns to the same event, back from Event Detail returns to its shopper origin, and back from host create/edit returns to the Host dashboard.

## Route-shaped state

`YardScapeRoute` exposes stable path-shaped state and `YardScapeRoute.fromPath` parses supported internal paths:

- `/browse`
- `/saved`
- `/host`
- `/account`
- `/events/{eventId}`
- `/events/{eventId}/rsvp`
- `/host/events/new`
- `/host/events/{eventId}/edit`

`YardScapeAppState.navigateToPath` updates internal navigation for tests and future platform adapters. It does not register Android intents, browser URLs, or iOS universal links.

Destination and context labels are fixed product copy. They never include event addresses, coordinates, access instructions, or other protected location content.

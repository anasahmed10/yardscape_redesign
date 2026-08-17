# Mock RSVP lifecycle

The Saved workspace links to a nested My RSVPs destination at `/rsvps`. It groups deterministic mock RSVP records into Needs attention, Upcoming, and Past and closed sections while preserving the four-item primary app shell.

The shared UI represents requested, accepted, full, waitlisted, declined, cancelled, revoked, and expired states. Each card gives the shopper a next action. Reminder and calendar export controls update local mock state only and do not call an external service.

## Protected location rules

- Public card content always uses the approximate public location.
- The protected exact address and directions entry point appear only when the RSVP is accepted, the reveal grant is active, the event is published, and the event has not ended.
- Cancelling an RSVP requires confirmation and immediately replaces the reveal grant with public-only visibility.
- Host revocation, shopper blocking, event cancellation, and expiry immediately clear the exact address and any pending directions state.
- Full, waitlisted, requested, declined, cancelled, revoked, and expired records never expose protected location data.

The scenario catalog and shared common tests cover every lifecycle state and the stale-address clearing transitions.

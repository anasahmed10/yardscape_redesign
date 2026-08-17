# Mock host attendance management

Each host event has a nested attendee workspace at `/host/events/{eventId}/attendees`. The host dashboard is the only entry point; public Browse and Event Detail models continue to contain no attendee identities or counts.

The deterministic seeded event covers requested, accepted, waitlisted, declined, shopper-cancelled, host-removed, reveal-revoked, and expired rows. The summary separates accepted attendance from active exact-location access so a host can see the privacy consequence of every state.

## Actions and privacy

- Accept grants protected exact-location access immediately, but is disabled when the attendee cap is reached.
- Decline keeps exact location hidden.
- Remove ends attendance and revokes exact-location access.
- Revoke keeps attendance accepted while removing exact-location access immediately. Because attendance remains accepted, revocation does not free capacity.
- Every action is reviewed individually in a confirmation dialog that explains its location-access consequence.
- Bulk grants are intentionally absent; guidance tells hosts to review sensitive grants one at a time.

The empty state explains where future requests appear. Capacity guidance uses shopper-facing language and avoids database or API terminology.

## Backend findings

Ticket #28 should model accept, decline, remove, and reveal revocation as explicit idempotent transitions with authorization, audit events, and a response containing both attendance status and reveal status. The app needs both states in one response to avoid briefly showing stale access.

Ticket #43 should count accepted attendance independently from reveal access, enforce caps atomically when accepting, and return a capacity conflict that can be shown as waitlist guidance. Revoking reveal access must not free a spot; removing attendance should.

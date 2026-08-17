# Backend Contract Freeze

This document translates the reviewed mock UI into the initial backend contract. It freezes behavior and privacy boundaries, not endpoint spelling. Network repositories should be suspend-based, serializable, authenticated where noted, and return typed validation, offline, unauthorized, conflict, and server outcomes.

## Invariants

1. Public preview and detail DTOs contain approximate area only. Exact address, coordinates, access notes, attendee identities, and counts are absent.
2. Protected location is a separate authenticated response granted only for an accepted, active, unblocked RSVP while the event is active.
3. RSVP cancellation, host revocation/removal, event cancellation, expiry, session expiry, and blocking invalidate reveal access immediately and idempotently.
4. Attendee caps and acceptance are enforced atomically. A stale client cannot overbook or issue a reveal grant.
5. Block state is an overriding deny for discovery, detail, RSVP mutation, directions, and reveal. Unblock restores discovery only.
6. Report responses use opaque receipt IDs and never echo report text. Logs and moderation storage treat report details as potentially sensitive.
7. Authentication resume data is a typed internal destination containing an event ID and Browse/Saved origin, never an exact address or arbitrary external URL.

## UI-to-contract map

| UI action/state | Repository operation | Request needs | Response needs | Primary ticket |
| --- | --- | --- | --- | --- |
| Browse/search/filter/map | `publicPreviews(query)` | date range, radius or broad area, categories, cursor | public preview list, next cursor; approximate area only | #42 |
| Public detail | `publicEventDetail(eventId)` | stable event ID | public detail, status, host trust facts; no protected fields | #33, #34 |
| Save/unsave | local-first preference repository, later account sync | event ID, desired saved state | idempotent saved state | future follow-up |
| Submit/cancel RSVP | `submitRsvp`, `cancelRsvp` | event ID; identity from session | RSVP state, policy version, reveal eligibility; conflict when full | #28, #43 |
| My RSVPs | `rsvpsForShopper` | authenticated cursor/filter | lifecycle state and public event summary; protected location excluded | #28, #34 |
| Reveal/directions | `exactLocationFor` | event ID; identity from session | short-lived protected address payload plus expiry; authorization failures do not include address | #27, #28 |
| Host draft/edit/publish/hide/cancel | `saveHostEvent`, `hideHostEvent`, `cancelHostEvent` | structured draft, optimistic version, selected private location | validation by editor section, status, updated version, privacy-safe preview | #26, #33, #34 |
| Photo selection/upload | photo repository | event ID, upload metadata, ordering, alt/caption text | moderation/storage state and stable asset ID; no public URL until allowed | #44 |
| Attendee list/policy | `rsvpsForEvent`, policy read/update | owner-authenticated event ID, cap, approval mode, version | private attendee rows, counts, policy version | #28, #43 |
| Accept/decline/revoke/remove | corresponding attendee mutation | event ID, shopper ID, expected policy/version | new RSVP/reveal state and counts; idempotent result | #28, #43 |
| Session/sign-in/expiry | session repository | credentials or refresh proof outside UI route state | anonymous/active/expired state, role, minimal profile/assurance facts | #25 |
| Submit report | `submitReport` | event ID, required reason enum, optional details ≤500 | opaque receipt ID or field validation; no detail echo | #29 |
| Block/unblock host | `blockHostForEvent`, `unblockHostForEvent` | event ID; identity from session | host ID, every affected event ID, resulting block state | #45 |

## DTO boundaries

- `PublicEventPreviewDto`: event ID, title, sale window, status, approximate neighborhood/city, broad distance, category tags, privacy-safe photo metadata, and allowed host trust facts.
- `PublicEventDetailDto`: preview fields plus public description, payments, accessibility notes, RSVP policy summary, and capacity availability. No exact address or attendee data.
- `RsvpDto`: event ID, shopper-visible lifecycle state, reveal eligibility, updated timestamp, and policy version. It does not embed exact location.
- `ProtectedLocationDto`: exact address and access instructions, reveal expiry, and a revocable grant identifier. It is returned only from the protected endpoint.
- `HostEventDraftDto`: structured editor sections, private selected location, photo asset IDs/order, cap, approval mode, and optimistic version.
- `HostAttendeeDto`: minimal private attendee display facts, RSVP state, reveal state, and allowed actions. It never appears in a public response.
- `SafetyReportRequestDto` / `ReceiptDto`: reason enum and bounded details in; opaque receipt ID and status out.
- `BlockedHostUpdateDto`: host ID, all affected event IDs, and final block state so clients can apply an immediate deny before cache cleanup.

## Error model

All mutations need stable categories that the shared UI can map without parsing prose: `validation`, `unauthorized`, `forbidden`, `not_found`, `conflict`, `offline`, `rate_limited`, and `server`. Validation should identify fields or editor sections. Conflict responses for RSVP/cap and optimistic host edits should include the current safe state, never protected data the caller is no longer authorized to see.

## Implementation sequence

Normalize suspend repository APIs and serialization first (#34, #33), then establish persistence/auth (#23–#26), geospatial public reads (#42), RSVP/reveal policy (#27, #28, #43), photo handling (#44), and report/block safety APIs (#29, #45). Each implementation must retain the mock scenario as a contract test until an API-backed equivalent covers the same happy and failure states.

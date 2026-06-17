# Post-MVP Backend Design

## Purpose
This design turns the post-MVP roadmap into an implementation plan for moving YardScape from seeded or in-memory data to a privacy-aware backend with real accounts, PostgreSQL/PostGIS persistence, geospatial search, moderation, and auditability.

The design keeps the MVP trust boundary intact: public discovery can use approximate area data, but exact addresses, precise coordinates, access instructions, attendee lists, and reveal history stay private and access-controlled.

## Design Goals
- Support Android first, with web and future iOS using the same API/session model.
- Store public event discovery data separately from protected exact-location data.
- Make exact-location reveal decisions auditable, revocable, expirable, and deletable according to a retention policy.
- Keep the migration from seeded data incremental, so the shared app can switch repository implementations without a broad rewrite.
- Provide moderation and abuse-reporting hooks before real location data is collected at scale.

## Auth And Sessions
Use account-based auth before persistent event creation is opened beyond trusted testers.

Recommended first implementation:
- Email magic link or one-time code sign-in for Android and web.
- Short-lived access tokens issued by the Ktor backend.
- Rotating refresh tokens stored server-side as hashed token records.
- Device/session records per login so users can revoke sessions.
- Future iOS can use the same token exchange and refresh flow.

Session tables:
- `users`: stable account id, display name, contact email or phone, role flags, verification state, timestamps, disabled/deleted markers.
- `user_profiles`: public profile fields and trust signals that can appear on event previews.
- `sessions`: hashed refresh token id, user id, platform, device label, created time, last used time, expires time, revoked time.
- `auth_challenges`: one-time sign-in challenges with hashed code, delivery target, expiry, consumed time, and rate-limit metadata.

API impact:
- Public `GET /events` can remain unauthenticated.
- RSVP, exact-location reveal, host event mutation, reports, blocks, and account actions require authentication.
- Server-side caller identity should be resolved before route handlers call event/reveal policy logic.
- Do not put role or reveal authorization decisions only in client state.

## PostgreSQL And PostGIS Data Model
Use PostgreSQL as the source of truth and PostGIS for proximity queries. Prefer UUID primary keys and server-side timestamps. Keep exact location fields out of public query projections.

Core tables:
- `yard_sale_events`: id, host user id, title, description, sale window, status, published time, cancelled time, hidden time, created/updated timestamps.
- `event_public_locations`: event id, neighborhood, city, region, approximate area description, optional broad distance label seed, and optional approximate geography.
- `event_private_locations`: event id, encrypted or access-restricted street address, unit/gate, city, region, postal code, precise PostGIS point, access instructions, created/updated timestamps.
- `event_categories`: event id and category key.
- `event_payment_types`: event id and payment type.
- `event_accessibility_notes`: event id and note text.
- `event_photos`: event id, storage key, moderation status, alt/description text, public visibility flag, timestamps.
- `rsvps`: id, event id, shopper user id, status, location visibility, requested time, accepted time, revoked time, expired time, cancelled time.
- `blocks`: blocker user id, blocked user id or host id, reason category, created time.
- `reports`: id, reporter user id, target type, target id, reason, freeform details, status, assigned reviewer id, timestamps.
- `moderation_actions`: id, report id, reviewer id, action type, notes, created time.
- `sensitive_audit_logs`: id, actor user id, subject user id, event id, action type, request id, metadata, created time, retention/delete-after time.

PostGIS fields:
- Store public approximate search geometry separately from precise event coordinates.
- Use `geography(Point, 4326)` or equivalent for precise private coordinates.
- Use a coarse public point or generated area bucket for public search and map previews.
- Index public search geometry with GiST for radius and map-window queries.
- Index private points only for host-owned/admin workflows; never use private points in public response DTOs.

Privacy-sensitive storage:
- Treat `event_private_locations`, `rsvps.location_visibility`, access instructions, report details, and audit metadata as sensitive.
- Keep public DTO queries backed by explicit projections or views that cannot include private columns.
- Consider column-level encryption or application-managed encryption for exact street address and access instructions before production.
- Restrict admin queries with least privilege and audit every sensitive lookup.

## API Changes
Keep the existing MVP shape but make caller identity and privacy projections explicit.

Public APIs:
- `GET /events`: returns public previews only, backed by `yard_sale_events` plus `event_public_locations`.
- `GET /events/{id}`: returns public detail only. Authenticated callers should still fetch exact address through the location endpoint to keep the reveal audit boundary clear.

Authenticated shopper APIs:
- `POST /events/{id}/rsvps`: creates or updates RSVP state.
- `DELETE /events/{id}/rsvps/me`: cancels RSVP and revokes exact-location access.
- `GET /events/{id}/location`: checks reveal policy, writes an audit log for allowed or denied sensitive reveal attempts, and returns exact location only when allowed.
- `POST /reports`: reports an event, host, attendee, photo, or message.
- `POST /blocks`: blocks a host or shopper where applicable.

Authenticated host APIs:
- `POST /events`: creates a draft with private location stored only in protected tables.
- `PATCH /events/{id}`: updates host-owned event fields.
- `POST /events/{id}/publish`: publishes when required public and private fields are valid.
- `POST /events/{id}/cancel`: cancels event, revokes active location access, and writes audit records.
- `POST /events/{id}/rsvps/{rsvpId}/accept`: accepts an RSVP and grants reveal access.
- `POST /events/{id}/rsvps/{rsvpId}/revoke`: revokes reveal access and writes audit records.

Admin/moderation APIs:
- `GET /moderation/reports`: reviewer queue.
- `POST /moderation/reports/{id}/actions`: records action and status changes.
- `POST /moderation/events/{id}/hide`: hides unsafe events from search.
- `GET /admin/audit/events/{id}`: limited audit view for authorized reviewers only.

## Exact Location Reveal Policy
Exact location is visible only when all of these are true:
- The caller is authenticated.
- The event is published and not cancelled, hidden, or completed.
- The sale window has not ended.
- The caller has an RSVP with accepted status.
- The RSVP location visibility is accepted and not revoked, expired, or cancelled.
- The host has not removed the attendee or closed access for abuse/safety reasons.

Every `GET /events/{id}/location` request should produce a `sensitive_audit_logs` row with one of:
- `LOCATION_REVEAL_ALLOWED`
- `LOCATION_REVEAL_DENIED_NOT_AUTHENTICATED`
- `LOCATION_REVEAL_DENIED_NOT_ACCEPTED`
- `LOCATION_REVEAL_DENIED_REVOKED`
- `LOCATION_REVEAL_DENIED_EXPIRED`
- `LOCATION_REVEAL_DENIED_EVENT_INACTIVE`

Audit metadata may include request id, platform, coarse IP region, user agent family, and policy decision inputs. Do not store more request metadata than is needed for safety review.

## Moderation And Reporting Flow
Reporting should be available before broad alpha testing.

Flow:
1. Shopper or host creates a report with target type, target id, reason category, and optional details.
2. Server stores the report and writes `REPORT_CREATED` audit metadata.
3. Reported events/photos can be auto-hidden only for high-confidence safety cases; otherwise they enter review.
4. Reviewers inspect public event data first. Access to exact address or reveal logs requires elevated reviewer permission and creates an audit record.
5. Reviewer actions can hide an event, remove a photo, disable RSVP, revoke attendee access, warn a user, suspend an account, or mark a report as no action.
6. The reporter receives only safe status language, not private enforcement details.

Moderation data should support queues by status, target type, severity, duplicate target count, and recent report velocity.

## Retention, Deletion, And Export
Define retention windows before collecting real user locations.

Baseline recommendations for alpha:
- Exact address remains editable for upcoming events and hidden from normal attendee access after the event ends.
- Exact-location reveal audit logs are retained long enough for safety review, then purged or anonymized.
- Cancelled event access is revoked immediately.
- Deleted accounts keep minimal tombstone ids only where needed for fraud, abuse, or legal compliance.
- User export should include profile, hosted events, RSVPs, reports they filed, and audit entries where disclosure is safe.
- User deletion should remove or anonymize profile/contact data while preserving event safety records required by policy.

Production launch requires counsel-reviewed retention windows for users, events, RSVPs, reports, photos, reveal logs, admin actions, and deleted accounts.

## Migration And Rollout Plan
1. Add database runtime config, Flyway, and a local PostgreSQL/PostGIS development profile.
2. Create migrations for users, sessions, events, public/private locations, RSVPs, reports, moderation actions, and sensitive audit logs.
3. Add repository interfaces on the server that mirror the seeded repository behavior.
4. Seed development data through migrations or a dev-only seed task using the existing MVP data.
5. Move Ktor routes from in-memory/seeded data to persistent repositories behind the same DTO privacy boundary.
6. Add auth middleware and authenticated route tests for RSVP, host mutations, reports, and exact-location reveal.
7. Switch shared app API clients to authenticated calls for RSVP, location reveal, and host flows.
8. Add admin/moderation routes behind role checks.
9. Add retention jobs for expired location access, ended events, old challenges, and audit purge/anonymization.
10. Gate any real-user pilot on legal/privacy review, abuse reporting, and account deletion support.

## Follow-Up Tickets
- Add PostgreSQL/PostGIS and Flyway local development runtime.
- Create initial persistence migrations for events, public/private locations, RSVPs, users, sessions, reports, and audit logs.
- Implement Ktor auth challenge/session flow for Android and web.
- Replace server seeded event storage with persistent repositories.
- Add exact-location reveal audit logging and denial reason tests.
- Add host RSVP accept/revoke APIs and attendee access tests.
- Add report creation and moderation queue APIs.
- Add retention jobs for ended events, expired reveal access, old auth challenges, and audit log purge/anonymization.
- Add account export/delete planning and implementation tickets after legal policy review.

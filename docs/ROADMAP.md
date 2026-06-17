# YardScape Roadmap

## Summary
YardScape should become an event-first yard sale marketplace. The first milestone is a testable Android workflow that proves discovery, RSVP, and protected location reveal. Later milestones add persistence, trust, moderation, platform parity, and production hardening.

## Phase 1: Testable MVP
Goal: prove the core loop with realistic seeded data before investing in the full marketplace platform.

- Replace the generated sample UI with a shared Compose workflow: Browse, Event Detail, RSVP, and Host Create/Edit.
- Use seeded fake yard sale events first, including a mix of public previews, RSVP-required events, cancelled events, and completed events.
- Add shared domain models for `YardSaleEvent`, `EventLocation`, `Rsvp`, `UserProfile`, `EventStatus`, and `LocationVisibility`.
- Keep public event previews limited to approximate location, sale window, categories, photos, and host trust signals.
- Add a clear RSVP state transition: not RSVPed, requested, accepted, revoked, expired.
- Reveal exact address only through an explicit accepted RSVP state, even if MVP auto-accepts test RSVPs.
- Add host event creation and editing with draft, publish, cancel, and hide-from-search states.
- Add initial Ktor API routes for event list, event detail, RSVP, exact location reveal, create, update, and cancel.
- Add Android workflow validation for Browse -> Detail -> RSVP -> Location Revealed.

Exit criteria:
- Android debug build runs.
- A tester can browse seeded events, RSVP, and see exact location only after RSVP acceptance.
- A tester can create or edit a host event in the local workflow.
- Server tests cover public event DTOs not leaking exact address.

## Phase 2: Improvement Roadmap
Goal: turn the testable workflow into a useful alpha for a limited community.

- Add account basics: email or phone sign-in, profile, session handling, and sign-out.
- Add persistence with PostgreSQL and PostGIS for radius, neighborhood, and map-style searches.
- Add database migrations with Flyway.
- Add Ktor Client networking in shared code and replace seeded data with API-backed repositories.
- Add host RSVP approval mode, attendee caps, RSVP revocation, and exact-address expiry after event end.
- Add event photo uploads with moderation-ready storage paths.
- Add report, block, and basic moderation queues.
- Add reminders, saved searches, watchlist, calendar export, and "today near me" filters.
- Add web browsing and RSVP parity after Android workflow stabilizes.
- Add iOS build validation and shared UI polish.
- Add route planning hooks through platform map intents without storing unnecessary route data.

Exit criteria:
- A limited group can create, find, RSVP to, and attend events using real accounts.
- Exact location access is auditable and revocable.
- Basic abuse reporting and moderation triage exist.
- Android and server CI checks run on Pull Requests.

## Phase 3: Production Readiness
Goal: prepare YardScape for a public beta and store review.

- Complete privacy hardening: data retention, account deletion, location access expiry, audit logs, and export/delete flows.
- Add rate limiting, spam prevention, abuse detection, report review workflows, and support escalation.
- Add crash reporting, structured server logging, uptime checks, and privacy-reviewed analytics.
- Add production signing gates, release build configuration, secret management, and environment separation.
- Add store-ready Privacy Policy, Terms of Service, marketplace safety guidance, and location-data disclosures after legal review.
- Add admin tools for event moderation, account review, takedowns, and support lookup.
- Add onboarding, empty states, accessibility polish, localization readiness, and offline/error states.
- Seed launch communities and test local density before broad rollout.

Exit criteria:
- Legal/privacy docs are approved.
- Production secrets and signing are managed outside the repository.
- Store submission builds pass.
- Moderation, support, deletion, and privacy workflows are operational.

## Feature Backlog
- Neighborhood sale pages and community-wide yard sale events.
- Map clusters and route planner for a morning of sales.
- Host verification, shopper no-show tracking, and post-event host ratings.
- Private invite-only sales and event check-in code.
- Featured sales, promoted community events, and optional monetization.
- Accessibility notes, parking notes, accepted payment types, weather contingency, and item category previews.

# Host and Messaging Workflows Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign YardScape's host dashboard, editor, and attendee tools and add a mock/local, event-scoped inbox and conversation workflow whose composer is available only to authorized participants.

**Architecture:** Put pure messaging models and access policy in `core`, and keep the suspend-friendly local repository plus observable Compose state in `app/shared`. Every send and retry re-evaluates access inside the repository, while `YardScapeAppState` synchronously invalidates an open composer when RSVP, block, session, or event state changes. Host and messaging screens reuse the merged marketplace theme, local event artwork, lightweight sections, and existing privacy-safe public projections.

**Tech Stack:** Kotlin 2.4, Compose Multiplatform 1.11, Material 3, coroutines, common tests, seeded in-memory repositories.

**Spec:** `docs/superpowers/specs/2026-08-18-yardscape-marketplace-redesign-design.md`

## Global Constraints

- Preserve the approved warm linen, evergreen, clay, muted-map, serif-title, sans-UI, photo-forward visual system.
- Keep messaging mock/local and backend-free; add no paid service, analytics, remote image loading, or production commitment.
- Message models, routes, summaries, logs, and previews never contain exact addresses, coordinates, units, access instructions, attendee lists, or phone numbers.
- A shopper may compose only for their own accepted RSVP with active `RSVP_ACCEPTED` visibility on a published, unended, unblocked event. Host ownership is independently verified.
- Cancellation, revocation, expiry, event cancellation/hiding/completion, blocking, sign-out, and session expiry synchronously close the visible composer. Repository send and retry re-check the same policy.
- Closed existing threads may remain readable and reportable, but an unauthorized actor cannot create or open a never-authorized thread.
- Preserve the five primary destinations, public/protected location boundary, map state, `/saved` and `/rsvps` compatibility, and KMP buildability.
- Named interactive targets are at least 48 dp and layouts remain usable at 390 x 844 and 1440 x 900.

---

### Task 1: Messaging domain policy and seeded repository

**Files:**
- Create: `core/src/commonMain/kotlin/com/naslabs/yardscape/domain/MarketplaceMessagingModels.kt`
- Create: `core/src/commonMain/kotlin/com/naslabs/yardscape/domain/MarketplaceMessagingPolicy.kt`
- Create: `core/src/commonTest/kotlin/com/naslabs/yardscape/domain/MarketplaceMessagingPolicyTest.kt`
- Create: `app/shared/src/commonMain/kotlin/com/naslabs/yardscape/data/MarketplaceMessagingRepository.kt`
- Create: `app/shared/src/commonMain/kotlin/com/naslabs/yardscape/data/SeededMarketplaceMessagingRepository.kt`
- Create: `app/shared/src/commonTest/kotlin/com/naslabs/yardscape/data/SeededMarketplaceMessagingRepositoryTest.kt`

**Interfaces:**
- Produce `MarketplaceConversationKey(eventId, shopperId)`, `MessagingActor(userId, role)`, `MessageThreadSummary`, `MarketplaceMessage`, `MessageDeliveryState`, `MessagingComposerAccess`, and `MessagingClosedReason`.
- Produce `MarketplaceMessagingPolicy.composerAccess(context, actor)` from a public-safe `MessagingAccessContext` containing IDs/status flags but no protected address.
- Produce typed `MessagingRepositoryResult.Success`, `ValidationFailure`, `Unauthorized`, `Offline`, and `ServerError`.
- Produce suspend-friendly `MarketplaceMessagingRepository.inboxFor`, `threadFor`, `sendMessage`, `retryMessage`, and `markRead` methods. The seeded implementation receives a live `MarketplaceMessagingAccessSource` and re-checks it on every thread open, send, and retry.

- [ ] Write policy tests for shopper accepted/open, wrong shopper, missing/not-accepted/cancelled RSVP, revoked, expired, blocked, unpublished/cancelled/completed/hidden/ended event, and owning/non-owning host.
- [ ] Run the focused core test and record the expected RED for missing messaging types/policy.
- [ ] Implement the minimal pure models and policy. `REQUESTED`, `WAITLISTED`, `FULL`, `DECLINED`, `CANCELLED`, `REMOVED`, revoked, and expired shopper states are closed.
- [ ] Write repository tests for participant authorization, unread counts per actor, message validation, send success, deterministic failure persistence, retry success, and retry denial after cancellation/revocation/block.
- [ ] Run the repository test and record the expected RED for missing repository implementation.
- [ ] Implement the seeded repository with opaque conversation IDs, public photo/title projection, private message bodies, and mutation-time policy checks.
- [ ] Assert `MessageThreadSummary`, thread, and message `toString()` values never contain seeded exact address, coordinates, unit, or access-instruction text.
- [ ] Run focused core/shared tests, `git diff --check`, self-review, and commit `feat: add privacy-safe marketplace messaging repository`.

### Task 2: Observable messaging state, routes, and transition invalidation

**Files:**
- Create: `app/shared/src/commonMain/kotlin/com/naslabs/yardscape/ui/MarketplaceMessagingState.kt`
- Modify: `app/shared/src/commonMain/kotlin/com/naslabs/yardscape/ui/YardScapeNavigation.kt`
- Modify: `app/shared/src/commonMain/kotlin/com/naslabs/yardscape/ui/AccountState.kt`
- Modify: `app/shared/src/commonTest/kotlin/com/naslabs/yardscape/ui/YardScapeRouteTest.kt`
- Modify: `app/shared/src/commonTest/kotlin/com/naslabs/yardscape/ui/YardScapeNavigationTest.kt`
- Create: `app/shared/src/commonTest/kotlin/com/naslabs/yardscape/ui/MarketplaceMessagingStateTest.kt`

**Interfaces:**
- Consume Task 1 repository and access policy.
- Add `YardScapeRoute.MessageThread(conversationId)` with `/messages/{opaqueConversationId}` and `Messages` as its primary destination.
- Produce observable inbox/thread/loading/error state, draft updates, unread/read transitions, send/retry results, and a pure `MessagingThreadPresentation` that exposes `canCompose` only for `MessagingComposerAccess.Open`.
- Add `ProtectedAction.Messaging`. Signed-out access resumes after sign-in without putting shopper/event identity in the route.

- [ ] Add route tests for inbox/thread parsing, opaque path identity, back navigation, primary destination, and sign-in resume.
- [ ] Add state tests for loading/inbox/thread/error, draft validation, mark-read, send/retry, and closed-reason presentation.
- [ ] Add transition tests proving RSVP cancellation, host revoke/remove, expiry, event cancel/hide, host-wide block, sign-out, and session expiry synchronously remove the visible composer and clear message drafts.
- [ ] Run focused tests and record the intended RED failures.
- [ ] Implement the smallest route/state integration. Use pure app-state access calculation for synchronous UI invalidation and retain repository enforcement for mutations.
- [ ] Ensure unblock restores discovery only and never revives a thread whose RSVP access was revoked.
- [ ] Run full shared host tests and Android debug assembly, self-review, and commit `feat: add gated messaging navigation state`.

### Task 3: Production inbox and conversation UI

**Files:**
- Replace: `app/shared/src/commonMain/kotlin/com/naslabs/yardscape/ui/MessagesScreen.kt`
- Create: `app/shared/src/commonMain/kotlin/com/naslabs/yardscape/ui/MarketplaceMessagingComponents.kt`
- Modify: `app/shared/src/commonMain/kotlin/com/naslabs/yardscape/App.kt`
- Create: `app/shared/src/commonTest/kotlin/com/naslabs/yardscape/ui/MarketplaceMessagingPresentationTest.kt`
- Modify: `app/androidApp/src/androidTest/kotlin/com/naslabs/yardscape/RsvpRevealSmokeTest.kt`

**Interfaces:**
- Consume Task 2 observable state and callbacks; no repository calls occur directly from composables.
- Produce photo-led inbox rows, unread badges, empty/loading/offline/server-error states, conversation timeline, delivery state, retry, closed banner, event link, report/block entry, and a 48 dp composer/send action.

- [ ] Write presentation tests for row artwork, unread semantics, message ordering, failed delivery/retry, closed reasons, composer/action visibility, empty exits, and absence of protected fields.
- [ ] Run focused tests and record the intended RED for missing presentation APIs.
- [ ] Implement responsive inbox/thread surfaces using `ShopperEventArtwork`, lightweight separators, existing theme tokens, and a centered expanded-width layout.
- [ ] Wire async load/open/read/send/retry with `LaunchedEffect` and the existing Compose coroutine scope. Prevent duplicate loads and duplicate sends.
- [ ] Wire shopper Report/Block from a thread through existing safety state and return to the same thread when still accessible; closed blocked threads return to Messages.
- [ ] Extend Android smoke source coverage for accepted compose, failed send/retry, and composer disappearance after cancellation or block.
- [ ] Run focused/full shared tests, Android assembly/androidTest compilation, self-review, and commit `feat: build marketplace messaging experience`.

### Task 4: Host dashboard and editor redesign

**Files:**
- Create: `app/shared/src/commonMain/kotlin/com/naslabs/yardscape/ui/HostDashboardScreen.kt`
- Modify: `app/shared/src/commonMain/kotlin/com/naslabs/yardscape/ui/PrimaryDestinationScreens.kt`
- Modify: `app/shared/src/commonMain/kotlin/com/naslabs/yardscape/ui/HostCreateEditScreen.kt`
- Modify: `app/shared/src/commonMain/kotlin/com/naslabs/yardscape/ui/HostEditorState.kt`
- Modify: `app/shared/src/commonMain/kotlin/com/naslabs/yardscape/ui/YardScapeNavigation.kt`
- Modify: `app/shared/src/commonTest/kotlin/com/naslabs/yardscape/ui/HostEditorFlowTest.kt`
- Create: `app/shared/src/commonTest/kotlin/com/naslabs/yardscape/ui/HostMarketplacePresentationTest.kt`

**Interfaces:**
- Extend `HostEventItem` with a public-safe photo reference and host-only counts/progress; do not copy protected address into the dashboard model.
- Produce photo-led listing rows, explicit Edit/Attendees actions, useful empty state, durable seven-step progress, selected-photo management, and shopper-faithful approximate-only preview.

- [ ] Write focused tests for dashboard action state, draft progress, responsive compact/expanded decision, preview photo mapping, and protected-field exclusion.
- [ ] Run focused tests and record the intended RED failures.
- [ ] Extract and redesign the dashboard with flat rows, whitespace, status/RSVP metrics, 48 dp actions, and real local artwork.
- [ ] Redesign the seven-step editor with a persistent progress indicator, one active step, visible validation, production photo management, and clearer save/publish hierarchy.
- [ ] Use serial mobile layout and a capped/two-pane expanded layout where it improves form/preview readability; preserve existing state and callbacks.
- [ ] Run focused/full shared tests and Android assembly, self-review, and commit `feat: redesign host dashboard and editor`.

### Task 5: Attendee management, host messaging entry, scenarios, and platform validation

**Files:**
- Modify: `app/shared/src/commonMain/kotlin/com/naslabs/yardscape/ui/HostAttendanceScreen.kt`
- Modify: `app/shared/src/commonMain/kotlin/com/naslabs/yardscape/ui/HostAttendanceState.kt`
- Modify: `app/shared/src/commonMain/kotlin/com/naslabs/yardscape/App.kt`
- Modify: `app/shared/src/commonMain/kotlin/com/naslabs/yardscape/scenarios/MockScenarioCatalog.kt`
- Modify: `app/shared/src/commonTest/kotlin/com/naslabs/yardscape/ui/HostAttendanceStateTest.kt`
- Modify: `app/shared/src/commonTest/kotlin/com/naslabs/yardscape/scenarios/MockScenarioCatalogTest.kt`
- Modify: `docs/MOCK_SCENARIOS.md`

**Interfaces:**
- Add `Message attendee` only for an open host-owned conversation; repository authorization remains decisive.
- Produce accepted/unread, failed-send/retry, cancelled, revoked, expired, event-cancelled, blocked, signed-out, and host-owned messaging scenarios.

- [ ] Write focused tests for flattened attendance groups, summary metrics, access labels, message-action visibility, capacity, and every privacy transition.
- [ ] Run focused tests and record the intended RED failures.
- [ ] Implement a photo/event header, compact summary metrics, flatter attendee rows, clear exact-access state, 48 dp actions, and host message entry.
- [ ] Add deterministic messaging scenarios and document expected route, actor, composer state, protected-data absence, and recovery action.
- [ ] Run `:core:testAndroidHostTest`, `:app:shared:testAndroidHostTest`, Android debug/androidTest compilation, JS/Wasm compatibility distribution, iOS simulator tests, and unsigned Xcode simulator build.
- [ ] Self-review the whole task diff for exact-location leaks, stale composers, route identity, responsive layout, and cross-platform behavior; commit `feat: complete host and messaging workflows`.


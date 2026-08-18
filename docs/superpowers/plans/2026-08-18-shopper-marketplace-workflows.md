# Shopper Marketplace Workflows Implementation Plan

> **For agentic workers:** Implement task-by-task with focused TDD, a task review after each commit, and one final whole-branch review.

**Goal:** Apply the approved production marketplace design to shopper Browse list mode, event detail, My Finds, RSVP, protected reveal, saved states, and shopper loading, empty, offline, and error states for issue #71.

**Architecture:** Keep presentation decisions and privacy-sensitive action visibility in shared Kotlin. Compose public shopper surfaces from `PublicEventPreview`-derived UI state, and keep protected exact-location rendering confined to the existing authorized RSVP reveal path. Preserve the merged #70 map flow and five-destination shell.

**Tech Stack:** Kotlin 2.4, Compose Multiplatform 1.11, Material 3, common tests, Android smoke coverage.

**Spec:** `docs/superpowers/specs/2026-08-18-yardscape-marketplace-redesign-design.md`

## Global Constraints

- Adopt the approved visual system unchanged: warm linen surfaces, evergreen actions, clay save accents, muted map colors, strong yard-sale photography, serif event titles, sans-serif UI text, and official icon assets.
- Public UI never exposes exact addresses, protected coordinates, access notes, unit information, or directions before active accepted RSVP authorization.
- Preserve the five destinations and `/saved` and `/rsvps` compatibility behavior from issue #69.
- Preserve map state, filtering, selection, recovery, and privacy behavior from issue #70.
- Interactive targets are at least 48 dp, controls have clear semantics, and layouts remain usable at 390 x 844 and 1440 x 900.
- Keep Android, iOS, JS, and Wasm source sets buildable; do not add a network image service or backend work.

---

### Task 1: Shopper presentation foundation

**Files:**
- Modify: `app/shared/src/commonMain/kotlin/com/naslabs/yardscape/ui/YardScapeComponents.kt`
- Create: `app/shared/src/commonMain/kotlin/com/naslabs/yardscape/ui/ShopperMarketplaceComponents.kt`
- Create: six compressed photos under `app/shared/src/commonMain/composeResources/drawable/`
- Create: `docs/PHOTO_ATTRIBUTIONS.md`
- Create: `app/shared/src/commonTest/kotlin/com/naslabs/yardscape/ui/ShopperMarketplacePresentationTest.kt`

**Interfaces:**
- Produce reusable photo-forward public event artwork, lightweight section/state panels, and responsive presentation decisions for compact and expanded shopper layouts.
- Components consume privacy-safe public preview or shopper UI state only and expose named actions with 48 dp minimum targets. Seed photo references map deterministically to six bundled royalty-free resources with source, license, alt-text, and pre-launch review notes.

- [x] Write focused tests for compact/expanded decisions and public-only presentation metadata, and capture the expected red failure.
- [x] Implement reusable shopper components using the existing theme, typography, bundled/resource artwork, and official navigation/icon assets.
- [x] Keep section hierarchy lightweight; avoid nested-card density and placeholder-style chrome.
- [x] Run focused and full shared Android host tests.
- [x] Self-review and commit with `feat: add shopper marketplace presentation foundation`.

### Task 2: Browse list and shopper availability states

**Files:**
- Modify: `app/shared/src/commonMain/kotlin/com/naslabs/yardscape/ui/BrowseScreen.kt`
- Modify: `app/shared/src/commonMain/kotlin/com/naslabs/yardscape/ui/ShopperDiscoveryState.kt`
- Modify availability/scenario behavior only: `app/shared/src/commonMain/kotlin/com/naslabs/yardscape/ui/YardScapeNavigation.kt`
- Modify: `app/shared/src/commonTest/kotlin/com/naslabs/yardscape/ui/ShopperDiscoveryStateTest.kt`
- Create: `app/shared/src/commonTest/kotlin/com/naslabs/yardscape/ui/ShopperBrowsePresentationTest.kt`

**Interfaces:**
- Consume Task 1 shopper components without changing the map platform boundary or `MapDiscoveryExperience` behavior.
- Produce distinct loading, empty-nearby, filtered-empty, offline-cached, and recoverable-error presentations.

- [ ] Add focused tests for availability-state distinctions, responsive list presentation, and save/open action state.
- [ ] Redesign list mode with photographic hierarchy, concise metadata, lightweight separators, and accessible action order.
- [ ] Keep map mode synchronized and fully usable; do not restructure the platform map implementation.
- [ ] Verify public Browse presentation contains no protected fields.
- [ ] Run focused and full shared Android host tests, then commit with `feat: redesign shopper browse states`.

### Task 3: Event detail, RSVP, and protected reveal

**Files:**
- Modify: `app/shared/src/commonMain/kotlin/com/naslabs/yardscape/ui/EventDetailScreen.kt`
- Modify: `app/shared/src/commonMain/kotlin/com/naslabs/yardscape/ui/RsvpScreen.kt`
- Modify: `app/shared/src/commonTest/kotlin/com/naslabs/yardscape/ui/ShopperRsvpStateTest.kt`
- Modify: `app/androidApp/src/androidTest/kotlin/com/naslabs/yardscape/RsvpRevealSmokeTest.kt`
- Create: `app/shared/src/commonTest/kotlin/com/naslabs/yardscape/ui/LocationRevealPresentationTest.kt`

**Interfaces:**
- Consume Task 1 shopper components and existing `EventDetailState` and `ShopperRsvpItem` privacy-safe state.
- Preserve exact-location reveal, expiry, revocation, cancellation, block, directions, and navigation invariants.

- [ ] Add focused tests covering RSVP action visibility and every reveal state before UI changes.
- [ ] Implement photo-first detail hierarchy, concise public metadata, a clear RSVP action, and lighter section separation.
- [ ] Redesign protected reveal as a separate high-salience panel with directions only for active accepted access.
- [ ] Confirm exact address text and directions never render in public, expired, revoked, cancelled, or blocked states.
- [ ] Run focused/full shared tests and Android debug assembly, then commit with `feat: redesign detail and protected reveal`.

### Task 4: My Finds saved and RSVP workspace

**Files:**
- Modify: `app/shared/src/commonMain/kotlin/com/naslabs/yardscape/ui/MyFindsScreen.kt`
- Modify: `app/shared/src/commonMain/kotlin/com/naslabs/yardscape/ui/MyFindsState.kt`
- Modify: `app/shared/src/commonMain/kotlin/com/naslabs/yardscape/ui/ShopperRsvpState.kt`
- Delete: `app/shared/src/commonMain/kotlin/com/naslabs/yardscape/ui/MyRsvpsScreen.kt`
- Modify: `app/shared/src/commonTest/kotlin/com/naslabs/yardscape/ui/MyFindsStateTest.kt`
- Modify: `app/shared/src/commonTest/kotlin/com/naslabs/yardscape/ui/ShopperRsvpStateTest.kt`
- Create: `app/shared/src/commonTest/kotlin/com/naslabs/yardscape/ui/MyFindsPresentationTest.kt`

**Interfaces:**
- Consume Task 1 event artwork and lightweight state panels while retaining the existing My Finds route and section model.
- Produce accessible Saved/RSVP segmented state with useful empty exits and privacy-safe RSVP actions.

- [ ] Add focused tests for selected-section semantics, empty exits, RSVP grouping, cancellation, reveal eligibility, and action visibility.
- [ ] Implement a production-level Saved and RSVPs workspace with consistent event rows and responsive density.
- [ ] Ensure cancellation immediately removes reveal/directions access and section switching preserves navigation state.
- [ ] Remove the unreferenced duplicate `MyRsvpsScreen` implementation.
- [ ] Run shared tests, Android debug assembly, JS/Wasm compatibility distribution, and iOS simulator tests.
- [ ] Self-review and commit with `feat: redesign My Finds shopper workspace`.

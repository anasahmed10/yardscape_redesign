# Marketplace QA Hardening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close the UI-first redesign with measurable accessibility, responsive, privacy, map-performance, visual, and cross-platform evidence without changing the approved YardScape visual language.

**Architecture:** Keep reusable accessibility and performance decisions in shared Kotlin, with platform-specific runtime proof isolated to Android, JS, Wasm, and iOS validation surfaces. Extend existing presentation/state seams instead of rebuilding screens, and keep exact-location assertions at the domain/repository boundary as well as the rendered workflow boundary.

**Tech Stack:** Kotlin 2.4.0, Compose Multiplatform 1.11.1, Material 3, Compose UI tests, MapLibre Compose/JS, Kotlin coroutines test, GitHub Actions.

**Spec:** `docs/superpowers/specs/2026-08-18-yardscape-marketplace-redesign-design.md`

## Global Constraints

- Preserve the approved warm linen, evergreen, clay, market-blue, serif-title, sans-serif-control visual system; this ticket hardens behavior and evidence and does not redesign it.
- Interactive targets are at least `48.dp`; map actions have named semantics and the list remains a complete accessible alternative.
- Validate compact layout at `390 x 844` and expanded layout at `1440 x 900`, including browser `200%` zoom evidence.
- Public, map, inbox, scenario, and closed-thread projections never contain exact address, protected coordinates, unit/gate information, private instructions, or attendee lists.
- Map viewport work remains debounced, broad results remain clustered, map rendering is isolated from result-sheet scrolling, and bundled image decoding is bounded and documented.
- Android, iOS, JS, and Wasm must remain buildable; OpenFreeMap attribution remains visible on interactive and fallback maps.
- Runtime claims must name their evidence. TalkBack, VoiceOver, browser keyboard/zoom, contrast, and reduced-motion checks that cannot be automated are recorded as manual evidence or an explicit limitation, never implied from screenshots.

---

### Task 1: Shared accessibility and responsive controls

**Files:**
- Create: `app/shared/src/commonMain/kotlin/com/naslabs/yardscape/ui/YardScapeAccessibility.kt`
- Create: `app/shared/src/commonTest/kotlin/com/naslabs/yardscape/ui/YardScapeAccessibilityTest.kt`
- Modify: `app/shared/src/commonMain/kotlin/com/naslabs/yardscape/ui/YardScapeComponents.kt`
- Modify: `app/shared/src/commonMain/kotlin/com/naslabs/yardscape/ui/BrowseScreen.kt`
- Modify: `app/shared/src/commonMain/kotlin/com/naslabs/yardscape/ui/AccountScreen.kt`
- Modify: `app/shared/src/commonMain/kotlin/com/naslabs/yardscape/ui/ShopperSafetyScreen.kt`
- Modify: `app/shared/src/commonMain/kotlin/com/naslabs/yardscape/ui/HostCreateEditScreen.kt`
- Modify: `app/shared/src/commonMain/kotlin/com/naslabs/yardscape/ui/MessagesScreen.kt`
- Modify: `app/androidApp/src/androidTest/kotlin/com/naslabs/yardscape/MarketplaceAccessibilityTest.kt`

**Interfaces:**
- Produce `YardScapeMinimumInteractiveTarget = 48.dp` and `Modifier.yardScapeInteractiveTarget()` for shared controls.
- Produce pure `MapSheetAccessibilityState` and `mapSheetAccessibilityFor(position)` with label, state description, and legal expand/collapse actions.
- Produce reusable status semantics using `liveRegion = LiveRegionMode.Polite` for asynchronous success, failure, offline, and closed-access messages.

- [ ] Add focused tests that require the `48.dp` token, deterministic sheet action/state copy, and named live-region presentation; run them first and record the expected compilation failures.

```kotlin
assertEquals(48.dp, YardScapeMinimumInteractiveTarget)
assertEquals(listOf("Expand nearby sales"), mapSheetAccessibilityFor(MapResultsSheetPosition.Collapsed).actionLabels)
assertEquals("Expanded", mapSheetAccessibilityFor(MapResultsSheetPosition.Expanded).stateDescription)
```

- [ ] Implement the shared modifier/presentation seam, expose expand/collapse as semantic custom actions in addition to drag, and apply it to account, safety, dialog, message, host, and map actions missing an explicit target. Preserve all colors, type, shapes, spacing, copy intent, and navigation hierarchy.

```kotlin
internal fun Modifier.yardScapeInteractiveTarget(): Modifier =
    this.heightIn(min = YardScapeMinimumInteractiveTarget)
```

- [ ] Add Android Compose assertions for named map/list controls, custom sheet actions, selected/state descriptions, polite status announcements, and `>= 48.dp` bounds on critical Browse, RSVP, Messages, Host, and Account controls.
- [ ] Run focused shared tests, all shared Android host tests, Android debug assembly, and Android test compilation; self-review semantics for duplicate announcements or unreachable controls.
- [ ] Commit `feat: harden marketplace accessibility controls`.

### Task 2: Map performance, fallback, and render isolation

**Files:**
- Create: `app/shared/src/commonMain/kotlin/com/naslabs/yardscape/map/MapRenderReconciliation.kt`
- Create: `app/shared/src/commonTest/kotlin/com/naslabs/yardscape/map/MapRenderReconciliationTest.kt`
- Modify: `app/shared/src/commonMain/kotlin/com/naslabs/yardscape/ui/MapDiscoveryPresentation.kt`
- Modify: `app/shared/src/commonMain/kotlin/com/naslabs/yardscape/ui/BrowseScreen.kt`
- Modify: `app/shared/src/jsMain/kotlin/com/naslabs/yardscape/map/PlatformMapSurface.js.kt`
- Modify: `app/shared/src/wasmJsMain/kotlin/com/naslabs/yardscape/map/PlatformMapSurface.wasmJs.kt`
- Modify: `app/shared/src/commonTest/kotlin/com/naslabs/yardscape/ui/MapDiscoveryPresentationTest.kt`
- Modify: `app/shared/src/commonTest/kotlin/com/naslabs/yardscape/ui/MapDiscoveryStateTest.kt`

**Interfaces:**
- Produce deterministic spatial-bucket clustering whose cluster IDs remain derived only from sorted public event IDs.
- Produce `MapRenderReconciliation<T>` with added, retained, removed, and selection-changed public IDs; JS uses it to avoid rebuilding unchanged markers.
- Browse computes `MapDiscoveryPresentation` once per stable marker/zoom input with `remember` and passes the same value to the map and list.

- [ ] Add RED tests with at least `1_000` public markers proving deterministic clustering, every event represented exactly once, stable IDs under input reordering, debounce readiness, and reconciliation that retains unchanged markers.

```kotlin
val renderedIds = presentation.clusters.flatMap { it.eventIds } + presentation.unclusteredMarkers.map { it.eventId }
assertEquals(markers.map { it.eventId }.toSet(), renderedIds.toSet())
assertTrue(reconcile(previous, current).retainedIds.contains("maple-1"))
```

- [ ] Replace repeated list removal/scans with deterministic bucket candidates, compute presentation once in Compose, and reconcile JS marker handles by public ID. Selection-only changes update state without removing/recreating retained markers.
- [ ] Keep fallback copy, retry, list alternative, and OpenFreeMap/OpenMapTiles/OpenStreetMap attribution visible and named on Wasm and failure states.
- [ ] Run focused map tests, shared host tests, JS/Wasm compile/distribution, Android assembly, and iOS simulator tests; record deterministic fixture size and build/runtime baselines in the task report.
- [ ] Commit `perf: isolate marketplace map rendering`.

### Task 3: Cross-surface privacy and responsive regression matrix

**Files:**
- Create: `app/shared/src/commonTest/kotlin/com/naslabs/yardscape/ui/MarketplacePrivacyRegressionTest.kt`
- Modify: `app/shared/src/commonTest/kotlin/com/naslabs/yardscape/ui/YardScapeNavigationTest.kt`
- Modify: `app/shared/src/commonTest/kotlin/com/naslabs/yardscape/ui/ShopperRsvpStateTest.kt`
- Modify: `app/shared/src/commonTest/kotlin/com/naslabs/yardscape/ui/HostAttendanceStateTest.kt`
- Modify: `app/shared/src/commonTest/kotlin/com/naslabs/yardscape/ui/MarketplaceMessagingStateTest.kt`
- Modify: `app/androidApp/src/androidTest/kotlin/com/naslabs/yardscape/RsvpRevealSmokeTest.kt`
- Create: `app/androidApp/src/androidTest/kotlin/com/naslabs/yardscape/MarketplaceResponsiveTest.kt`

**Interfaces:**
- Produce a single table-driven privacy transition fixture covering revoke, RSVP cancellation, expiry, event cancellation, block, and sign-out from an initially accepted RSVP.
- The matrix asserts exact-location content, directions, selected private workflow state, and composer access disappear together while public map/list previews remain approximate.

- [ ] Write the table-driven shared regression test first and capture RED for any transition that leaves protected state, directions, map selection, or composer access alive.

```kotlin
transitions.forEach { transition ->
    val state = acceptedFixture().apply(transition.mutate)
    assertNull(state.protectedLocationFor(eventId), transition.name)
    assertFalse(state.messagingState.canCompose, transition.name)
    assertFalse(state.browseItems.any { it.publicLocation.displayLabel.contains("Street") }, transition.name)
}
```

- [ ] Fix only demonstrated state-projection gaps; do not widen public DTOs or move protected fields into UI models.
- [ ] Extend Android workflow tests to prove reveal-before/after transitions and composer closure, and add compact runtime reachability/bounds checks. Keep `390 x 844`, `1440 x 900`, and `200%` zoom as browser runtime evidence where Android instrumentation cannot faithfully emulate a browser viewport.
- [ ] Run focused privacy/navigation tests, full shared host tests, Android assembly, and Android test compilation; self-review every changed model for protected-field leakage.
- [ ] Commit `test: lock marketplace privacy transitions`.

### Task 4: Runtime design QA, platform evidence, CI, and durable documentation

**Files:**
- Modify: `.github/workflows/platform-compatibility.yml`
- Modify: `docs/PLATFORM_VALIDATION.md`
- Modify: `docs/MAP_DISCOVERY.md`
- Modify: `docs/MOCK_FLOW_USABILITY_REVIEW.md`
- Modify: `docs/ROADMAP.md`
- Create: `docs/QA_HARDENING.md`
- Create/update: `docs/audit-assets/qa-73-*.png`

**Interfaces:**
- Platform CI runs the combined JS/Wasm distribution plus portable `jsBrowserTest` and `wasmJsBrowserTest` where supported.
- `docs/QA_HARDENING.md` records evidence by platform, viewport, assistive technology/keyboard mode, contrast pair, reduced-motion setting, map fallback, latency/build baseline, result, and limitation.

- [ ] Add the portable browser test tasks to platform CI and document why Android emulator, TalkBack, VoiceOver, Xcode, and interactive map smoke checks remain local/manual unless dedicated runner cost is approved.
- [ ] Run the app in the in-app browser without Playwright CLI. Capture fresh Browse and representative workflow screenshots at `390 x 844` and `1440 x 900`; inspect every saved file, then combine the approved reference and matched prototype screenshot for visual comparison. Fix visible clipping, hierarchy, padding, type, border, crop, or attribution mismatches without changing the approved visual style.
- [ ] Exercise keyboard navigation and `200%` browser zoom for Browse/map-list, detail/RSVP, My Finds, Host, Messages, and Account. Record named blockers rather than claiming unobserved TalkBack/VoiceOver or reduced-motion behavior.
- [ ] Measure WCAG contrast for every theme foreground/background role pair in code and document ratios; add a deterministic token test that fails below `4.5:1` for normal text and `3:1` for large text/UI where applicable.
- [ ] Record map debounce/clustering fixture time, JS/Wasm production bundle sizes, bundled image dimensions/bytes, and map/list/fallback observations. Avoid flaky wall-clock assertions in unit tests.
- [ ] Run `:core:testAndroidHostTest`, `:app:shared:testAndroidHostTest`, `:server:test`, Android debug/androidTest compilation, JS/Wasm browser tests and compatibility distribution, iOS simulator tests, unsigned Xcode simulator build, and `check`. Run Android instrumentation when an emulator is available; otherwise record it explicitly as unavailable.
- [ ] Self-review the full issue diff, run `git diff --check` and a secret scan, update docs/roadmap with the exact evidence and remaining manual launch gates, and commit `docs: complete marketplace QA hardening`.

# Marketplace QA hardening checkpoint

Evidence dates: August 18 and August 25, 2026. This document records the checks exercised in issues #73 and #78. It is a checkpoint, not a WCAG or platform-conformance claim.

## Runtime evidence

| Surface | Evidence | Result | Limitation |
| --- | --- | --- | --- |
| JS Browse, 390 × 844 | [`qa-73-mobile-browse.png`](./audit-assets/qa-73-mobile-browse.png) | Map and approximate public markers rendered; no exact address appeared. | The JS interop map keeps the results sheet after the map instead of overlaying it. |
| JS RSVP, 390 × 844 | [`qa-73-mobile-rsvp.png`](./audit-assets/qa-73-mobile-rsvp.png) | RSVP copy and protected-location boundary remained readable and reachable. | No screen reader was exercised. |
| JS Browse, 1440 × 900 | [`qa-73-desktop-browse.png`](./audit-assets/qa-73-desktop-browse.png) | Map/list split rendered with approximate areas and existing bundled UI. | Interactive tile loading remains a local/manual smoke check. |
| JS RSVP, 1440 × 900 | [`qa-73-desktop-rsvp.png`](./audit-assets/qa-73-desktop-rsvp.png) | Wide layout preserved hierarchy and the protected-location explanation. | No VoiceOver or TalkBack evidence. |
| Wasm fallback, 390 × 844 | [`qa-73-mobile-wasm-fallback.png`](./audit-assets/qa-73-mobile-wasm-fallback.png), [`scrolled`](./audit-assets/qa-73-mobile-wasm-fallback-scrolled.png) | The fallback, complete list alternative, privacy copy, and OpenFreeMap/OpenMapTiles/OpenStreetMap attribution remained present. A runtime-found overlap was fixed by reserving the sheet height above map attribution. | Static fallback only; it does not prove interactive map behavior. |
| Approved-reference comparison | [`qa-73-mobile-browse-comparison.png`](./audit-assets/qa-73-mobile-browse-comparison.png) | Warm linen/evergreen/clay hierarchy, map-first discovery, and bottom destinations remain aligned with the approved direction. | The reference uses a custom map-sheet composition that JS Compose interop cannot faithfully overlay. Accessibility-sized controls also wrap where the reference's smaller controls do not. |

Keyboard Tab focus was visibly exercised on Browse/map-list, event detail/RSVP, My Finds, Host, Messages, and Account. Compose Web exposed semantic names for these routes, but its canvas-backed controls did not expose the active semantic descendant through `document.activeElement`; therefore this run does not claim complete keyboard order or focus restoration.

The in-app Browser did not honor native `Command`+`+` zoom shortcuts. Its supported page-scale control was exercised at exactly `2.0` on Browse, RSVP, My Finds, Host, Messages, and Account, producing a 195 × 422 visual viewport from the 390 × 844 test viewport. [`qa-73-mobile-browse-zoom-200.png`](./audit-assets/qa-73-mobile-browse-zoom-200.png) records that magnified state. This proves the exercised magnification path only, not native browser-zoom reflow conformance.

## Native accessibility and reduced-motion evidence

Issue #78 used a Pixel 9 Pro AVD at an exact 390 x 844 dp override (1170 x 2532 px at 480 dpi), Android 17/API 37.1 preview, and TalkBack package version code 60201234. TalkBack touch exploration was enabled while animations were disabled (`window_animation_scale`, `transition_animation_scale`, and `animator_duration_scale` all set to `0`). The emulator was restored to its physical size, disabled accessibility state, and default animation behavior after the run.

| Surface | Runtime result | Evidence |
| --- | --- | --- |
| Browse map/list | The TalkBack accessibility tree exposed named Host, search, filter, date, map/list, location, map attribution, approximate pin, result, and bottom-navigation targets. MapLibre remained rendered with reduced motion, and its public nodes contained neighborhood labels only. | [`Runtime semantics`](./audit-assets/78-runtime-semantics.txt) |
| Detail, RSVP, reveal | Before confirmation, the accessibility tree contained the RSVP privacy explanation and no protected address, directions action, or private instruction. After accepted RSVP, the protected address, side-gate instruction, and named Directions action appeared together. | [`Runtime semantics`](./audit-assets/78-runtime-semantics.txt), [`detail`](./audit-assets/78-android-talkback-detail.jpg), [`accepted detail`](./audit-assets/78-android-talkback-reveal.jpg) |
| My Finds | Saved/RSVP segments exposed selected state and distinct names; the empty state remained readable. | [`78-android-talkback-myfinds.jpg`](./audit-assets/78-android-talkback-myfinds.jpg) |
| Host | Create, edit, and attendee-management actions exposed contextual names; public host copy continued to describe approximate areas only. | [`78-android-talkback-host.jpg`](./audit-assets/78-android-talkback-host.jpg) |
| Messages | Empty state and Browse recovery action were named; copy explained that conversations require active RSVP access. | [`78-android-talkback-messages.jpg`](./audit-assets/78-android-talkback-messages.jpg) |
| Safety and Account | Report/block routes and bottom destinations were named and keyboard reachable. Account trust copy remained grouped separately from identity verification. | [`Block Host route`](./audit-assets/78-android-talkback-block-host.jpg), [`account`](./audit-assets/78-android-talkback-account.jpg) |

Keyboard traversal with TalkBack moved focus through Compose controls and activated controls with Enter. Exercised interactive controls measured at least 144 px on this 480 dpi target, equal to the shared 48 dp minimum. The address/privacy transition was checked directly in the runtime accessibility hierarchy and is recorded in the compact text artifact; screenshots are contextual visual evidence only.

Android connected instrumentation could not reach app assertions on the only installed system image. All seven tests failed inside Espresso 3.7.0 because the API 37.1 preview removed `android.hardware.input.InputManager.getInstance`; the app had already built and installed successfully. This is an environment/runner incompatibility, not an app assertion failure. Stable-image TalkBack instrumentation, dialog focus restoration, polite live-region speech, and post-revocation spoken output remain tracked in #82.

The iPhone 17 Pro Simulator ran iOS 26.5 under Xcode 26.6. The unsigned app built, installed, and launched. Its runtime accessibility hierarchy exposed distinct labels and selected values for Browse/map-list, detail, Host, and all five destinations; the pre-RSVP detail hierarchy contained no protected address. Reduce Motion was enabled through the simulator accessibility preference, the app was relaunched, and Browse plus MapLibre remained available without a crash or blocked interaction. [`Runtime semantics`](./audit-assets/78-runtime-semantics.txt) records the preference and hierarchy results; [`78-ios-reduced-motion.jpg`](./audit-assets/78-ios-reduced-motion.jpg) records the rendered state.

Spoken VoiceOver output is not claimed: Apple's accessibility testing guidance says VoiceOver is unavailable on iOS Simulator and requires physical hardware. Physical-device reading order, announcements, focus restoration, reduced-motion gestures, and privacy-closing transitions remain tracked in #83.

## Contrast evidence

`YardScapeThemeTest.semanticForegroundBackgroundPairsMeetWcagContrastMinimums` computes WCAG relative luminance deterministically from the actual `ColorScheme` tokens. Normal-text pairs require at least `4.5:1`; the outline/UI pair requires at least `3.0:1`.

| Foreground / background | Ratio | Threshold |
| --- | ---: | ---: |
| onPrimary / primary | 5.99:1 | 4.5:1 |
| onPrimaryContainer / primaryContainer | 13.32:1 | 4.5:1 |
| onSecondary / secondary | 4.59:1 | 4.5:1 |
| onSecondaryContainer / secondaryContainer | 11.13:1 | 4.5:1 |
| onTertiary / tertiary | 5.67:1 | 4.5:1 |
| onTertiaryContainer / tertiaryContainer | 12.81:1 | 4.5:1 |
| onError / error | 6.54:1 | 4.5:1 |
| onErrorContainer / errorContainer | 12.77:1 | 4.5:1 |
| onBackground / background | 14.54:1 | 4.5:1 |
| onSurface / surface | 15.95:1 | 4.5:1 |
| onSurfaceVariant / surfaceVariant | 5.04:1 | 4.5:1 |
| onSurface / surfaceContainer | 15.95:1 | 4.5:1 |
| onSurface / surfaceContainerHighest | 12.96:1 | 4.5:1 |
| onSurfaceVariant / surfaceContainerHighest | 5.04:1 | 4.5:1 |
| onSurfaceVariant / secondaryContainer | 4.66:1 | 4.5:1 |
| inverseOnSurface / inverseSurface | 11.65:1 | 4.5:1 |
| primary / surface | 5.99:1 | 4.5:1 |
| outline / surface | 3.01:1 | 3.0:1 |

The original clay/white pair measured `4.02:1`, and the original sage outline/white pair measured `1.51:1`. The smallest style-preserving corrections used in this checkpoint are clay `#B85A42` and sage `#899987`.

## Deterministic baselines

- The map viewport debounce remains the deterministic `350 ms` product value; no wall-clock unit assertion was added.
- The 1,000-public-marker clustering fixture completed in `0.028 s` in the fresh Android host-test XML. This is an observation, not a test threshold.
- JS production: `webApp.js` 5,423,751 bytes; Skiko Wasm 8,652,729 bytes.
- Wasm production: `webApp.js` 537,259 bytes; app Wasm 4,624,037 bytes; Skiko Wasm 8,652,729 bytes.
- All six bundled marketplace JPEGs are 1200 × 800: book 295,963 bytes; clothing 312,649; flea 238,100; furniture 287,876; garage 189,945; swap 219,716.

Portable `:app:shared:jsBrowserTest` and `:app:shared:wasmJsBrowserTest` ran headlessly and reliably on this host, so the platform workflow now runs them before the combined JS/Wasm distribution build. Paid macOS or emulator runners were not added.

## Remaining launch gates

- Run the full validation matrix, unsigned Xcode simulator build, `check`, `git diff --check`, and repository secret scan.
- Run Android TalkBack instrumentation and spoken-output checks on a stable supported image (#82).
- Exercise VoiceOver on physical iOS hardware (#83).
- Exercise native browser zoom/reflow and finish focus-restoration/live-region checks on supported targets.
- Update `PLATFORM_VALIDATION.md`, `MAP_DISCOVERY.md`, `MOCK_FLOW_USABILITY_REVIEW.md`, and `ROADMAP.md` with final, fully validated evidence.

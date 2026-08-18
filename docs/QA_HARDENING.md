# Marketplace QA hardening checkpoint

Evidence date: August 18, 2026. This document records only checks exercised in the current issue #73 run. It is a checkpoint, not a WCAG or platform-conformance claim.

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

TalkBack, VoiceOver, reduced-motion preference behavior, Android instrumentation, and native browser-zoom reflow were not exercised in this checkpoint.

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
- Run Android instrumentation on an available emulator; `adb` was unavailable during this checkpoint.
- Exercise TalkBack, VoiceOver, native browser zoom/reflow, reduced motion, and focus restoration on supported devices.
- Update `PLATFORM_VALIDATION.md`, `MAP_DISCOVERY.md`, `MOCK_FLOW_USABILITY_REVIEW.md`, and `ROADMAP.md` with final, fully validated evidence.

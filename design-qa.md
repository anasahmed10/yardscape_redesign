# YardScape Browse Design QA

- Source visual truth: `docs/audit-assets/88-approved-browse-reference.png`
- Implementation screenshot: `docs/audit-assets/88-android-browse-final.png`
- Combined comparison: `docs/audit-assets/88-browse-comparison.png`
- Viewport: Android 390 × 844 dp, light theme, default Browse Map state
- Source pixels: 852 × 1846; implementation pixels: 1170 × 2532 at 3× density
- Normalization: implementation scaled to 852 × 1846 for the side-by-side comparison; Android-owned status/navigation chrome is preserved and excluded from fidelity findings

## Full-view comparison evidence

The final composition matches the reference hierarchy: branded header and host action, prominent search and filter controls, one-line date pills, map-first discovery, floating approximate-location disclosure, Map/List switch, rounded results sheet, photographic sale rows, clay save accents, and five compact destinations. Live OpenFreeMap geography replaces the illustrative map while retaining the same map/result proportions and privacy boundary.

The combined full-view image is sufficient for the typography, controls, map overlays, result artwork, and navigation to remain readable at the normalized size; no separate focused crop was required.

## Required fidelity surfaces

- Fonts and typography: serif display treatment is used for the wordmark and sale titles; sans-serif UI copy uses reduced compact sizing without clipping the four date choices.
- Spacing and layout: compact content spans the safe horizontal viewport, the map and results sheet share the first screen, and the sheet no longer falls below the fold.
- Colors and tokens: linen, evergreen, white, gray-green, and clay map to the approved visual language. Public map markers now use evergreen.
- Image quality: existing compressed marketplace photography is used with consistent crops and descriptive alternatives; no placeholder or generated imagery was introduced.
- Copy and content: Browse, host, search, filters, date options, privacy disclosure, nearby results, and compact Saved navigation use production-facing language.
- Accessibility and behavior: 48 dp targets, semantic labels and selected filter state, sheet resize actions, live-map attribution, and the list alternative remain intact.

## Comparison history

1. Pass 1 found P1 hierarchy drift: the 620 dp compact map placed the nearby-sales sheet below the visible viewport. The compact map was reduced and the sheet now overlays the map above persistent navigation.
2. Pass 2 found P2 control and overlay drift: date pills clipped, privacy and mode controls were hidden below the sheet, and public pins used blue. Pills were compacted, the bottom overlays were lifted above the sheet, and markers were changed to evergreen.
3. Pass 3 found a P2 selected-state defect: the Map label used the selected container color and was unreadable. The selected label now uses `onPrimary`; the final comparison shows the complete segmented control.
4. Review found the custom date pills no longer exposed their selected state and the earlier evidence had been captured before the live map stabilized. Selected semantics are now device-tested, and the final screenshot was recaptured after tiles, attribution, and approximate markers rendered.

## Follow-up polish

- P3: production OpenFreeMap cartography is more saturated and detailed than the illustrative reference map.
- P3: the live seeded photos and event copy differ from the fictional reference listings.
- P3: the collapsed sheet intentionally previews one result at 390 × 844; users can expand it with the existing drag and accessibility actions.

final result: passed

## Host editorial refresh

- Source visual language: `docs/audit-assets/88-approved-browse-reference.png`
- Compact captures: `docs/audit-assets/92-android-host-dashboard.png`, `docs/audit-assets/92-android-host-editor.png`, and `docs/audit-assets/92-android-host-attendees.png`
- Viewport: Android compact runtime at 1280 × 2856 px (480 dpi; approximately 427 × 952 dp), light theme.

The Host workspace translates—not copies—the approved Browse marketplace language. The dashboard now leads with a clear editorial section, warm photo-first sale cards, evergreen actions, clay selected navigation, and deliberately separated host controls. The editor has a single screen-owned Back affordance, readable seven-step progress, a white form group, and a compact action surface. Attendance maintains the same soft grouped surface, clear status labels, capacity warning, and strongly separated attendee controls.

Visual QA found no P0–P2 issues in hierarchy, clipping, action affordance, or the compact navigation overlap. The public dashboard and preview path continue to use approximate place labels only; the editor keeps protected address/instruction fields inside its private-location step, and attendance never exposes an address. Existing shared host presentation tests cover public-preview redaction and artwork fallback. The new runtime accessibility assertion covers the dashboard Create action and nested editor Back target; emulator instrumentation is separately blocked by Android 17's test-runtime `InputManager.getInstance` incompatibility before tests can execute.

Follow-up P3: a fully completed preview-state capture should be refreshed using a deterministic host-editor fixture when the Android instrumentation environment is upgraded; the production preview UI and its redaction logic are covered by the existing common tests.

final result: passed

## Shopper editorial refresh

- Source visual truth: `docs/audit-assets/88-approved-browse-reference.png`
- Compact captures: `91-android-detail-editorial.png`, `91-android-rsvp-protected.png`, `91-android-reveal-protected.png`, `91-android-safety-report.png`, and `91-android-my-finds-rsvps.png`
- Viewport: Android 390 × 844 dp, light theme, captured through the Android Compose workflow smoke harness at 3× density.

Visual inspection found no P0-P2 issue. Detail now keeps photography, public status, title, and concise supporting copy together in the first card. RSVP uses a focused mint card and separate privacy surface; the authorized reveal is visually distinct and the seeded exact address appears only there. Safety uses calm white form groups with neutral reason choices, while destructive actions remain deferred to their dedicated hierarchy. My Finds now uses the approved compact Browse-like row: a fixed photo with concise metadata on the right, followed by one full-width protected or approximate access label and a compact, scroll-reachable action flow without exposing the address.

Privacy and accessibility review: public Detail and My Finds surfaces show public/approximate language only. The address and directions action are still absent before acceptance and after revocation, as exercised by the focused Android smoke test. Shared target helpers and editorial segments retain 48 dp targets, and new success/closed-access state panels use polite live-region semantics.

final result: passed

## Shared editorial foundations

- Source visual truth: `docs/audit-assets/88-approved-browse-reference.png`
- Implementation capture: `docs/audit-assets/90-android-my-finds-editorial.png`
- Representative surface: compact **My Finds** at 390 × 844 dp, light theme; implementation pixels are 1170 × 2532 at 3× density.
- Shared implementation: primary roots own one linen editorial serif route title, evergreen selected segment, white unselected segment, clay selected navigation accent, 48 dp interaction targets, and a 960 dp expanded reading-width cap. The YardScape wordmark is intentionally limited to Browse.
- Privacy check: this foundation handles route chrome and selection only; it receives neither event addresses nor precise coordinates. Existing public/protected location surfaces remain unchanged.
- Accessibility check: primary navigation retains labels and selected semantics; My Finds segments now use semantic selected state with 48 dp minimum targets. Nested workflows retain their existing screen-owned back actions until #91–#93 migrate each workflow.
- Comparison result: the compact route extends the approved Browse language without copying its map-specific controls. The redundant app wordmark was removed from the non-Browse roots, leaving one editorial My Finds heading and a clear Saved/RSVPs hierarchy. The shared shell deliberately does not render on nested routes, avoiding duplicate route titles or back actions while those workflows migrate individually in #91–#93. No P0-P2 visual, privacy, or accessibility issue remains in the refreshed capture.

final result: passed

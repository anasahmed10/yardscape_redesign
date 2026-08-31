# Ticket #92 — Host visual refresh report

## Delivered

- Restyled the Host dashboard into a warm photo-led workspace with clear status, public-area context, RSVP progress, and compact/expanded action treatment.
- Restyled the seven-step Create/Edit flow with an editorial nested header, a single back control, visible progress, grouped form surfaces, and a focused action panel.
- Restyled shopper Preview into an explicitly labeled photo-first public card. It keeps the existing approximate-only metadata and excludes protected address/instruction content.
- Restyled attendance into a clear editorial header, capacity summary, state-labeled attendee cards, and accessible action grouping.
- Added a testable compact/expanded Host presentation policy, focused common coverage, Android semantic/48 dp coverage, design-system guidance, and compact runtime captures.

## Validation

- PASS: `ANDROID_HOME=/Users/anas/Library/Android/sdk ./gradlew :app:shared:testAndroidHostTest --tests com.naslabs.yardscape.ui.HostMarketplacePresentationTest`
- PASS earlier in this task: `:app:shared:testAndroidHostTest` and `:app:androidApp:assembleDebug`.
- PASS earlier in this task: JS/Wasm compatibility and iOS simulator test command completed after compilation; pre-existing dependency/deprecation warnings remained unchanged.
- BLOCKED: Android connected instrumentation starts but every existing test fails before assertions on the Android 17 emulator because its test runtime reflects the removed `android.hardware.input.InputManager.getInstance()` method. The new test compiled with the suite; this is environment-wide and not introduced by this change.
- PASS: `git diff --check`.

## Visual evidence

- `docs/audit-assets/92-android-host-dashboard.png`
- `docs/audit-assets/92-android-host-editor.png`
- `docs/audit-assets/92-android-host-attendees.png`

The completed-preview state remains covered by the shared preview/redaction tests. A deterministic editor fixture should be used to refresh its specific runtime screenshot once the emulator instrumentation environment is upgraded.

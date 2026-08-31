# Ticket #92 — Host visual refresh report

## Delivered

- Restyled the Host dashboard into a warm photo-led workspace with clear status, public-area context, RSVP progress, and compact/expanded action treatment.
- Restyled the seven-step Create/Edit flow with an editorial nested header, a single back control, visible progress, grouped form surfaces, and a focused action panel.
- Restyled shopper Preview into an explicitly labeled photo-first public card. It keeps the existing approximate-only metadata and excludes protected address/instruction content.
- Restyled attendance into a clear editorial header, capacity summary, state-labeled attendee cards, and accessible action grouping. Accept remains primary; decline, remove, and revoke use an error hierarchy.
- Made Hide a neutral secondary preview action, Cancel visibly destructive, and used error-colored confirmation actions for all irreversible/revoking host mutations.
- Added a testable compact/expanded Host presentation policy, focused common coverage, Android semantic/48 dp coverage, design-system guidance, and compact runtime captures.

## Validation

- PASS: `ANDROID_HOME=/Users/anas/Library/Android/sdk ./gradlew :app:shared:testAndroidHostTest --tests com.naslabs.yardscape.ui.HostAttendanceStateTest`
- PASS: `ANDROID_HOME=/Users/anas/Library/Android/sdk ./gradlew :app:androidApp:compileDebugAndroidTestKotlin`
- PASS earlier in this task: `:app:shared:testAndroidHostTest` and `:app:androidApp:assembleDebug`.
- PASS earlier in this task: JS/Wasm compatibility and iOS simulator test command completed after compilation; pre-existing dependency/deprecation warnings remained unchanged.
- BLOCKED: Android connected instrumentation starts but every existing test fails before assertions on the Android 17 emulator because its test runtime reflects the removed `android.hardware.input.InputManager.getInstance()` method. The new test compiled with the suite; this is environment-wide and not introduced by this change.
- PASS: `git diff --check`.

## Visual evidence

- `docs/audit-assets/92-android-host-dashboard.png`
- `docs/audit-assets/92-android-host-editor.png`
- `docs/audit-assets/92-android-host-preview.png`
- `docs/audit-assets/92-android-host-preview-actions.png`
- `docs/audit-assets/92-android-host-attendees.png`

All Host captures are exactly 390 × 844 dp (1170 × 2532 px at 480 dpi). The preview runtime evidence now covers both the photo-first public card and its Hide/Cancel controls.

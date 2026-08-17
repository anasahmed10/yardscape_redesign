# Platform Compatibility Validation

Android remains YardScape's fastest product-validation target, while shared Kotlin and Compose changes must keep JS, Wasm, and iOS buildable. Use the narrowest checks below and report unavailable host tooling instead of silently treating Android success as cross-platform proof.

## Trigger matrix

| Change | Required checks |
| --- | --- |
| Documentation, server-only code, or an isolated Android entry point | Use the existing docs, server, or Android check only. |
| `core/commonMain`, `app/shared/commonMain`, KMP dependencies, or shared Gradle configuration | Shared Android host tests, Android debug assembly when UI changes, combined JS/Wasm distribution, and the iOS simulator test on Apple Silicon macOS. |
| `app/webApp`, `jsMain`, or `wasmJsMain` behavior | Combined JS/Wasm distribution; manually launch each changed browser target when interaction behavior changed. |
| `iosMain`, Swift, Xcode project, or xcconfig | iOS simulator tests plus an unsigned Xcode simulator build. |
| Release-sensitive or broad platform changes | All applicable checks above plus the repository-wide `check` task. |

Android-only validation is sufficient when the change is isolated to `app/androidApp` and does not alter common contracts, dependencies, resources, or shared UI. A shared-code change is not Android-only merely because Android is the first product target.

## Commands

Run from the repository root.

Shared Android proof:

```bash
./gradlew --no-daemon :app:shared:testAndroidHostTest :app:androidApp:assembleDebug
```

Combined JS and Wasm production compatibility build:

```bash
./gradlew --no-daemon :app:webApp:composeCompatibilityBrowserDistribution
```

Run browser-portable shared tests when common logic, serialization, repositories, or navigation changes:

```bash
./gradlew --no-daemon :app:shared:jsBrowserTest :app:shared:wasmJsBrowserTest
```

On Apple Silicon macOS, compile and run shared iOS simulator tests:

```bash
./gradlew --no-daemon :app:shared:iosSimulatorArm64Test
```

For changes to the iOS wrapper or Xcode project, also run:

```bash
xcodebuild \
  -project app/iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -configuration Debug \
  -sdk iphonesimulator \
  -destination 'generic/platform=iOS Simulator' \
  CODE_SIGNING_ALLOWED=NO \
  build
```

The repository currently defines ARM64 iOS device and simulator targets. iOS simulator execution therefore requires Apple Silicon and Xcode. On Linux or Windows, record the iOS check as unavailable and rely on a maintainer or periodic macOS validation before merging platform-sensitive work.

Manual browser smoke servers are intentionally not CI commands because they remain running until stopped:

```bash
./gradlew :app:webApp:jsBrowserDevelopmentRun
./gradlew :app:webApp:wasmJsBrowserDevelopmentRun
```

The separate Platform compatibility workflow performs the combined web build on relevant pull requests, pushes to `main`, manual dispatches, and a weekly schedule. iOS remains a local/manual check so unattended validation does not consume macOS hosted-runner capacity without maintainer approval.

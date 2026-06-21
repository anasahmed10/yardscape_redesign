# YardScape

YardScape is a Kotlin Multiplatform yard sale marketplace. It treats each yard sale as a time-bound local event: shoppers can browse public previews, open event details, RSVP, and only then see the exact location when the event policy allows it.

The current MVP proves the core loop with shared Compose UI, seeded app data, and a Ktor API that keeps public event data separate from protected location reveal flows.

## Privacy Boundary

YardScape handles home-adjacent location data, so the main product rule is simple:

- Public event previews show approximate area only.
- Public API responses must not include exact addresses, precise coordinates, unit numbers, gate codes, host phone numbers, private notes, or attendee lists.
- Exact location access is granted only through an explicit accepted RSVP or equivalent location-access state.
- Location access expires after cancellation, revocation, or event completion.

See [docs/PRIVACY_GUIDELINES.md](./docs/PRIVACY_GUIDELINES.md) for the detailed rules.

## Tech Stack

| Layer | Technology |
| --- | --- |
| Language | Kotlin 2.4.0 |
| UI | Compose Multiplatform 1.11.1 and Material 3 |
| Android | Android Gradle Plugin 9.0.1, min SDK 24, target SDK 36 |
| Web | Kotlin JS and Wasm browser targets |
| iOS | Xcode app wrapping the shared Compose framework |
| Backend | Ktor 3.5.0 |

## Project Layout

```text
app/
|-- androidApp/  # Android app entry point, manifest, resources, smoke tests
|-- iosApp/      # Xcode iOS app wrapping the shared framework
|-- shared/      # Shared Compose UI, app state, repositories, platform bridges
|-- webApp/      # Kotlin web entry point and browser resources
core/            # Pure shared domain models, policy, and utilities
server/          # Ktor server application and server tests
docs/            # Product, roadmap, privacy, and backend design docs
```

Important shared domain and policy code lives in [core/src/commonMain/kotlin/com/naslabs/yardscape/domain](./core/src/commonMain/kotlin/com/naslabs/yardscape/domain). Shared UI and app state live in [app/shared/src/commonMain/kotlin/com/naslabs/yardscape](./app/shared/src/commonMain/kotlin/com/naslabs/yardscape).

## MVP Workflow

The first testable workflow is:

1. Browse upcoming yard sale events by date, approximate area, and categories.
2. Open a public event detail without seeing the exact address.
3. RSVP for the event.
4. Reveal the exact location only after RSVP acceptance.
5. Create or edit host events in the local workflow.

The MVP may auto-accept RSVPs for testing, but the protected location boundary remains explicit in domain logic, app state, and server behavior.

## Local Setup

Use Java 21 and run commands from the repository root.

On Windows:

```powershell
.\gradlew.bat :app:androidApp:assembleDebug
.\gradlew.bat :app:shared:testAndroidHostTest
.\gradlew.bat :server:test
```

On macOS or Linux:

```bash
./gradlew :app:androidApp:assembleDebug
./gradlew :app:shared:testAndroidHostTest
./gradlew :server:test
```

## Running Apps

```powershell
# Android debug build
.\gradlew.bat :app:androidApp:assembleDebug

# Ktor server
.\gradlew.bat :server:run

# Web app, Wasm target
.\gradlew.bat :app:webApp:wasmJsBrowserDevelopmentRun

# Web app, JS target
.\gradlew.bat :app:webApp:jsBrowserDevelopmentRun
```

For iOS, open [app/iosApp](./app/iosApp) in Xcode and run the app from there.

## Validation

Use the narrowest command that proves the change:

| Change type | Command |
| --- | --- |
| Shared domain or privacy policy logic | `.\gradlew.bat :app:shared:testAndroidHostTest` |
| Shared Compose UI or Android workflow | `.\gradlew.bat :app:shared:testAndroidHostTest` and `.\gradlew.bat :app:androidApp:assembleDebug` |
| Server routes or API behavior | `.\gradlew.bat :server:test` |
| Broad or release-sensitive changes | `.\gradlew.bat check` |

The Android RSVP reveal smoke test is available with a connected device or emulator:

```powershell
.\gradlew.bat :app:androidApp:connectedDebugAndroidTest
```

It opens the seeded Browse screen, selects the Maple Ridge event, confirms the exact address is hidden before RSVP, submits the MVP auto-accepted RSVP, and verifies the exact address appears only after acceptance.

## API Shape

The Ktor API separates public event data from protected location data:

- `GET /events`: public event previews with approximate location only.
- `GET /events/{id}`: public event detail without exact address.
- `POST /events/{id}/rsvps`: create or update an RSVP.
- `GET /events/{id}/location`: exact location only when the caller is authorized.
- `POST /events`: create a host event draft.
- `PATCH /events/{id}`: update host-owned event details.
- `POST /events/{id}/cancel`: cancel a host-owned event and revoke location access.

## Roadmap

The roadmap lives in [docs/ROADMAP.md](./docs/ROADMAP.md). Post-MVP backend planning lives in [docs/POST_MVP_BACKEND_DESIGN.md](./docs/POST_MVP_BACKEND_DESIGN.md).

Near-term priorities include persistence with PostgreSQL/PostGIS, Flyway migrations, shared Ktor Client networking, RSVP review controls, reveal audit logs, abuse reporting, and iOS/web compatibility checks.

## Agent Notes

Detailed contributor and agent instructions live in [AGENTS.md](./AGENTS.md). In short: keep product rules in shared Kotlin, preserve platform boundaries, avoid leaking exact addresses through public surfaces, update tests when behavior changes, and keep generated files, credentials, signing files, and machine-specific state out of commits.

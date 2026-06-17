# YardScape - Agent Instructions

## Project Overview
YardScape is a Kotlin Multiplatform yard sale marketplace where each yard sale is treated as a time-bound local event instead of a traditional item listing. Shoppers discover public event previews, RSVP, and only then see the exact location when the event rules allow it.

The app is Android-first for the initial testable workflow, with shared Compose Multiplatform UI, a Ktor backend, a web target, and an iOS app target that should remain buildable as the product matures.

## Tech Stack
| Layer | Technology |
| --- | --- |
| Language | Kotlin 2.4.0 |
| UI | Compose Multiplatform 1.11.1 and Material 3 |
| Android | Android Gradle Plugin 9.0.1, min SDK 24, target SDK 36 |
| Web | Kotlin JS and Wasm browser targets |
| iOS | Xcode app wrapping the shared Compose framework |
| Backend | Ktor 3.5.0 server module |
| Persistence roadmap | PostgreSQL with PostGIS after the in-memory or seeded-data MVP |
| Migrations roadmap | Flyway |
| Networking roadmap | Ktor Client in shared code |
| Serialization roadmap | kotlinx.serialization for API DTOs |
| Dependency injection roadmap | Koin |

## Architecture Rules
These rules are mandatory for every agent working in this repository.

1. Keep product and business rules in shared Kotlin, preferring `core/src/commonMain` for pure models and policy logic and `app/shared/src/commonMain` for shared UI and app state.
2. Do not leak Android, iOS, browser, or server platform APIs into common code. Use `expect`/`actual` only when a platform boundary is truly required.
3. Treat exact yard sale addresses as sensitive location data. Public event surfaces must show approximate area only.
4. Reveal exact locations only to RSVP users who are allowed by the event visibility policy. Hosts must be able to revoke access.
5. Do not hardcode display branding in new code. Use the shared brand/config source and the `yardscape.appName` Gradle property where platform build configuration is involved.
6. Keep server API contracts privacy-aware: public event DTOs must not include exact address fields.
7. Keep generated files, build outputs, credentials, local IDE state, signing files, and machine-specific paths out of commits.
8. Never commit secrets, API keys, database URLs, signing material, or production credentials.
9. Use Material 3 through the shared theme once the design system is introduced. Avoid ad hoc styling that future shared UI cannot reuse.
10. Add or update tests whenever changing domain behavior, location privacy rules, API behavior, or navigation-critical UI state.
11. Prefer small, focused changes that preserve existing module boundaries.
12. Do not introduce paid services, public legal commitments, store deployment changes, or broad architecture rewrites without explicit maintainer approval.

## Privacy And Safety Rules
YardScape's trust model depends on limiting location disclosure.

- Public listings may include neighborhood, city, approximate map area, broad distance, sale window, photos, description, category tags, accepted payment types, accessibility notes, and host trust signals.
- Public listings must not include full street address, precise coordinates, gate codes, unit numbers, host phone number, private instructions, or attendee list.
- Exact address access should be granted only after RSVP acceptance. The MVP may auto-accept RSVPs for testing, but code must keep the location reveal boundary explicit.
- Exact address access should expire after the event ends, after cancellation, or after host revocation.
- Hosts need cancel, unpublish, hide from search, attendee cap, RSVP review, and attendee removal controls in the roadmap.
- Shoppers need report, block, reminder, and RSVP cancellation controls in the roadmap.
- Production work must include rate limiting, abuse reporting, moderation review, audit logging for sensitive location reveal events, account deletion, and data retention policy.
- Privacy Policy, Terms of Service, location-data disclosures, and marketplace safety language must be reviewed by qualified counsel before production launch.

## Source Layout
```text
app/
|-- androidApp/  # Android application entry point, manifest, resources
|-- iosApp/      # Xcode iOS application wrapping the shared framework
|-- shared/      # Shared Compose UI, app state, and platform bridges
|-- webApp/      # Kotlin web entry point and browser resources
core/            # Pure shared domain models, policy, and utilities
server/          # Ktor server application and server tests
docs/            # Product, roadmap, privacy, and operating documentation
```

## Planned App Workflow
The initial testable flow should prove the marketplace loop before adding account complexity.

| Step | Area | Purpose | Primary Exit |
| --- | --- | --- | --- |
| 1 | Browse | Show upcoming yard sale events by date, distance, neighborhood, and category tags. | Open public event detail. |
| 2 | Event Detail | Show public details without exact address. | RSVP for the event. |
| 3 | RSVP | Request or confirm attendance and explain when exact location becomes visible. | Reveal exact location if accepted. |
| 4 | Host Create/Edit | Let a host draft, publish, edit, cancel, and manage RSVP access for a sale. | Return to host event detail. |

## Domain Model Roadmap
Prefer stable names close to these concepts unless implementation evidence suggests a better local pattern.

| Model | Purpose |
| --- | --- |
| `YardSaleEvent` | Event title, description, sale window, categories, photos, host, status, and location visibility. |
| `EventLocation` | Approximate public location plus protected exact address fields. |
| `Rsvp` | Shopper attendance state and exact-location access state. |
| `UserProfile` | Minimal host or shopper profile, verification state, and trust signals. |
| `EventStatus` | Draft, published, cancelled, completed, hidden. |
| `LocationVisibility` | Public approximation, RSVP requested, RSVP accepted, revoked, expired. |

## API Roadmap
The Ktor API should separate public and private event data.

- `GET /events`: list public event previews with approximate location only.
- `GET /events/{id}`: return public event detail with no exact address unless the authenticated caller has active reveal access.
- `POST /events/{id}/rsvps`: create or update an RSVP.
- `GET /events/{id}/location`: return exact location only when the caller is authorized.
- `POST /events`: create a host event draft.
- `PATCH /events/{id}`: update host-owned event details.
- `POST /events/{id}/cancel`: cancel a host-owned event and revoke location access.

## Autonomous Work Policy
Agents may complete normal feature, bug fix, test, and documentation work while the maintainer is away when the request is clear and reasonably scoped.

- Start from a GitHub issue unless the maintainer explicitly says the task is exploratory-only or bootstrap-only.
- Create agent-owned branches with the `codex/<issue-number>-<short-slug>` format after the issue exists. Use a short `codex/<short-description>` branch only for bootstrap work that cannot reasonably start from an issue.
- Target Pull Requests to `main` until a release branch is created and this file is updated.
- Inspect the current implementation before editing. Do not ask for file paths, package names, commands, or architecture facts that can be discovered locally.
- Implement the smallest useful change, add or update tests when behavior changes, run the matching validation commands, commit, push, open a Pull Request, review the resulting diff, make it ready for review if it was created as a draft, and merge it when validation and merge requirements are satisfied.
- Agents are expected to complete normal queue tickets end-to-end on their own, including PR review and merge, when the work is within the approved autonomous scope and does not involve the restricted areas listed below.
- Reference the ticket from the PR body with `Refs #<issue-number>`.
- Use sensible defaults for routine implementation details and note them in the final report.
- Interrupt the user only for product, security, privacy, cost, account, legal, release, or architecture decisions that cannot be inferred safely.

Agents must not merge or ship changes involving secrets, signing, store deployment, paid service setup, account ownership, legal/privacy commitments, destructive git operations, or broad architecture rewrites without explicit approval.

## Local Commands
Use the Windows commands below from the repository root.

```powershell
.\gradlew.bat :app:androidApp:assembleDebug
.\gradlew.bat :app:shared:testAndroidHostTest
.\gradlew.bat :server:test
.\gradlew.bat check
```

Useful run commands:

```powershell
.\gradlew.bat :server:run
.\gradlew.bat :app:webApp:wasmJsBrowserDevelopmentRun
.\gradlew.bat :app:webApp:jsBrowserDevelopmentRun
```

## Validation Matrix
Use the narrowest validation that proves the change.

| Change type | Required validation |
| --- | --- |
| Docs-only changes | No Gradle task required unless commands, generated examples, workflows, or release instructions changed; markdown-only edits may be merged in a PR without test execution. |
| Core domain or privacy policy logic | `.\gradlew.bat :app:shared:testAndroidHostTest` and any focused common tests that apply. |
| Shared Compose UI or Android workflow | `.\gradlew.bat :app:shared:testAndroidHostTest` and `.\gradlew.bat :app:androidApp:assembleDebug`. |
| Server routes or API behavior | `.\gradlew.bat :server:test`. |
| Web app behavior | `.\gradlew.bat :app:webApp:wasmJsBrowserDevelopmentRun` for manual smoke testing when feasible. |
| Broad or release-sensitive changes | `.\gradlew.bat check` plus targeted platform checks. |

## GitHub And Release Workflow
- The repository is expected to live at `anasahmed10/yardscape` or the canonical GitHub name chosen by the maintainer, with private visibility during bootstrap.
- The default branch is `main`.
- Pull Requests target `main` until a release branch exists.
- The `Agent validation` GitHub Actions workflow runs on Pull Requests to `main` and pushes to `main`. It maps to the validation matrix by running shared Android host tests, server tests, and Android debug assembly as separate jobs.
- CI must use debug/test Gradle tasks only. Do not require production credentials, release signing material, store deployment setup, or paid services for unattended PR validation.
- When a release branch is created, update this section and target feature/fix PRs there.
- Agents may merge their own PRs for normal queue work after local validation, GitHub checks if configured, secret scanning, and self-review are complete. If GitHub does not allow self-approval, record that limitation and proceed when repository rules permit merging.
- Do not merge automatically until local validation, GitHub checks if configured, secret scanning, and review expectations are satisfied.
- Prefer squash merges with concise summaries for agent-owned branches.

## Agent Workflow
1. Read this file before making changes.
2. Confirm there is a GitHub issue for the task, or record that the maintainer marked it exploratory or bootstrap-only.
3. Inspect relevant code and docs before proposing or editing.
4. Keep work in the smallest useful scope.
5. Preserve the privacy boundary between public approximate location and protected exact location.
6. Add or update tests when behavior changes.
7. Run the most focused validation command that proves the change.
8. Open the PR, review the diff and checks, resolve any issues, make the PR ready, and merge it when repository rules allow.
9. Update the roadmap or milestone table in this file or the relevant docs when the completed change advances, closes, or reshapes planned work.
10. Report commands that could not be run and why.
11. Update this file when a durable architectural or workflow rule changes.

## Current Milestones
| # | Milestone | Status |
| --- | --- | --- |
| 1 | Initial Kotlin Multiplatform scaffold | Done |
| 2 | Agent instructions, privacy guidance, and roadmap | Done |
| 3 | Configurable display name baseline | Done |
| 4 | Domain models and seeded MVP data | Planned |
| 5 | Shared Compose Browse -> Detail -> RSVP flow | Planned |
| 6 | Ktor MVP API with protected exact-location reveal | Planned |
| 7 | Android workflow smoke test | In Review |

# Marketplace Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Establish the approved marketplace shell, five-destination navigation, and combined My Finds workflow for issue #69.

**Architecture:** Keep navigation and workflow state in shared Kotlin. Replace the current Saved destination with My Finds while parsing legacy paths into typed section state. Keep Messages as an honest empty inbox surface until issue #72 supplies repository behavior.

**Tech Stack:** Kotlin 2.4, Compose Multiplatform 1.11, Material 3, common tests.

**Spec:** `docs/superpowers/specs/2026-08-18-yardscape-marketplace-redesign-design.md`

## Global Constraints

- Public UI never exposes exact address data before active RSVP authorization.
- Primary destinations are Browse, My Finds, Host, Messages, and Account in that order.
- `/saved` opens My Finds Saved and `/rsvps` opens My Finds RSVPs.
- Use existing YardScape theme colors and shared spacing tokens.
- Keep Android, iOS, JS, and Wasm source sets buildable.

---

### Task 1: Navigation and My Finds state

**Files:**
- Modify: `app/shared/src/commonMain/kotlin/com/naslabs/yardscape/ui/YardScapeNavigation.kt`
- Create: `app/shared/src/commonMain/kotlin/com/naslabs/yardscape/ui/MyFindsState.kt`
- Test: `app/shared/src/commonTest/kotlin/com/naslabs/yardscape/ui/YardScapeRouteTest.kt`
- Test: `app/shared/src/commonTest/kotlin/com/naslabs/yardscape/ui/MyFindsStateTest.kt`

**Interfaces:**
- Produce `enum class MyFindsSection { Saved, Rsvps }`.
- Produce `YardScapeRoute.MyFinds(section: MyFindsSection)` and `YardScapeRoute.Messages`.
- Replace `YardScapePrimaryDestination.Saved` with `MyFinds`; add `Messages`.

- [ ] Write route tests proving `/finds`, `/saved`, and `/rsvps` resolve to the correct section and that detail routes may originate from My Finds.
- [ ] Run the focused tests and confirm they fail because the new types do not exist.
- [ ] Implement the navigation types, compatibility parsing, destination labels, and My Finds derived state.
- [ ] Run the focused tests and all shared Android host tests.
- [ ] Commit with `feat: establish marketplace navigation`.

### Task 2: My Finds and Messages surfaces

**Files:**
- Create: `app/shared/src/commonMain/kotlin/com/naslabs/yardscape/ui/MyFindsScreen.kt`
- Create: `app/shared/src/commonMain/kotlin/com/naslabs/yardscape/ui/MessagesScreen.kt`
- Modify: `app/shared/src/commonMain/kotlin/com/naslabs/yardscape/App.kt`
- Modify: `app/shared/src/commonTest/kotlin/com/naslabs/yardscape/ui/YardScapeAppStateTest.kt`

**Interfaces:**
- Consume `MyFindsSection`, existing saved items, and existing RSVP items.
- Produce named actions for switching sections and opening public detail.

- [ ] Add state tests proving destination changes preserve the requested My Finds section and clear no protected state.
- [ ] Run the focused tests and confirm the new route handling fails.
- [ ] Implement a segmented My Finds screen by composing existing privacy-safe item models, plus an empty Messages inbox that explains RSVP gating without promising backend delivery.
- [ ] Wire both routes through `App.kt`, remove obsolete separate primary-route wiring, and run shared tests.
- [ ] Commit with `feat: add My Finds and messages destinations`.

### Task 3: Production marketplace shell

**Files:**
- Modify: `app/shared/src/commonMain/kotlin/com/naslabs/yardscape/ui/YardScapeAppShell.kt`
- Modify: `app/shared/src/commonMain/kotlin/com/naslabs/yardscape/ui/YardScapeTheme.kt`
- Modify: `app/shared/src/commonMain/kotlin/com/naslabs/yardscape/ui/BrowseScreen.kt`
- Modify: `app/shared/build.gradle.kts`
- Test: `app/shared/src/commonTest/kotlin/com/naslabs/yardscape/ui/YardScapeThemeTest.kt`

**Interfaces:**
- Consume the five primary destinations.
- Produce icon-and-label navigation with at least 48 dp targets and no mock-data badge.

- [ ] Extend theme tests for the serif display/sans body split and existing semantic palette.
- [ ] Run the theme test and confirm it fails on the current typography.
- [ ] Add the official Compose Material icon dependency, implement recognizable destination icons, simplify the header, apply serif display typography, and remove duplicate Browse hero chrome without changing privacy copy.
- [ ] Run shared tests, Android debug assembly, JS/Wasm compatibility distribution, and iOS simulator tests.
- [ ] Commit with `feat: polish marketplace shell`.

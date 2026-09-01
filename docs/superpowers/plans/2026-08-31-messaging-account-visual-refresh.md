# Messaging and Account Visual Refresh Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Apply YardScape's approved warm editorial marketplace system to Messages and Account without changing routes, repository behavior, or privacy policy.

**Architecture:** Shared presentation policy owns responsive widths, target sizes, and action hierarchy. Shared Compose screens consume those policies and existing state models, so private-message access and account session behavior are unchanged.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Material 3, shared Android host tests.

**Spec:** GitHub issue #93 and `docs/UI_DESIGN_SYSTEM.md`

## Global Constraints

- Preserve RSVP-gated messaging and never show protected addresses in inboxes or message diagnostics.
- Reuse the shared theme, local marketplace photography, 48 dp interactive targets, and root shell titles.
- Do not modify navigation routes, domain models, repositories, backend contracts, or dependencies.

---

### Task 1: Establish visible presentation policy

**Files:**
- Create: `app/shared/src/commonMain/kotlin/com/naslabs/yardscape/ui/MarketplaceAccountComponents.kt`
- Modify: `app/shared/src/commonMain/kotlin/com/naslabs/yardscape/ui/MarketplaceMessagingComponents.kt`
- Test: `app/shared/src/commonTest/kotlin/com/naslabs/yardscape/ui/AccountStateTest.kt`
- Test: `app/shared/src/commonTest/kotlin/com/naslabs/yardscape/ui/MarketplaceMessagingPresentationTest.kt`

- [x] Write failing policy tests for account states, responsive width, photo-first inbox rows, and action tones.
- [x] Run focused shared tests and verify unresolved policy symbols fail compilation.
- [x] Add the minimal account and message presentation properties.
- [x] Re-run focused shared tests successfully.

### Task 2: Restyle screen surfaces

**Files:**
- Modify: `app/shared/src/commonMain/kotlin/com/naslabs/yardscape/ui/MessagesScreen.kt`
- Modify: `app/shared/src/commonMain/kotlin/com/naslabs/yardscape/ui/AccountScreen.kt`

- [x] Apply warm grouped surfaces, photographic inbox rows, single nested Back navigation, clear sale/report/block hierarchy, and compact composer treatment.
- [x] Apply warm account access/profile/settings cards and retain signed-out, expired, notifications, privacy, and session states.
- [x] Preserve all existing actions and semantic descriptions.

### Task 3: Validate and document

**Files:**
- Modify: `docs/UI_DESIGN_SYSTEM.md`
- Modify: `design-qa.md`
- Create: `docs/audit-assets/93-android-*.png`

- [x] Run shared Android host tests, Android debug assembly, web compatibility, and iOS simulator tests.
- [x] Capture compact Android evidence for inbox and account overview/settings; the seeded default inbox is empty, while shared presentation tests cover retry and closed access states.
- [x] Record visual, accessibility, and privacy findings in design QA.

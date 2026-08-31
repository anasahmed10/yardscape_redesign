# Shared Visual Foundations Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Extend the approved Browse marketplace visual language into reusable shared Compose foundations and an editorial non-Browse shell.

**Architecture:** Keep business state and routes unchanged. Add internal presentation policies and composables in the shared UI layer, then make the app shell consume them so later workflow tickets can adopt the same visual grammar without duplicating tokens or layout logic.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Material 3.

**Spec:** GitHub issue #90 and the approved Browse reference at `docs/audit-assets/88-approved-browse-reference.png`.

## Global Constraints

- Preserve every route, repository interface, domain model, backend contract, and privacy boundary.
- Reuse the existing six marketplace photos and navigation icons; add no dependencies or generated assets.
- Keep Android, iOS, JS, and Wasm buildability.
- Maintain 48 dp targets, semantic selection, keyboard navigation, and screen-reader labels.
- Work test-first and update `docs/UI_DESIGN_SYSTEM.md`.

---

### Task 1: Shared editorial components and application shell

**Files:**
- Create or extend focused shared UI component and presentation-policy files under `app/shared/src/commonMain/kotlin/com/naslabs/yardscape/ui/`.
- Modify `YardScapeAppShell.kt`, `YardScapeTheme.kt`, and only supporting shared component/icon files required by the new foundations.
- Test presentation policies in `app/shared/src/commonTest/kotlin/com/naslabs/yardscape/ui/` and compact shell behavior in `MarketplaceResponsiveTest.kt`.
- Update `docs/UI_DESIGN_SYSTEM.md` and root `design-qa.md`.

**Interfaces:**
- Produce internal composables for editorial headers/back navigation, event photography, section/status/privacy cards, segmented controls, state panels, and responsive content widths.
- Produce pure compact/expanded presentation policies that later tickets can test without rendering Compose.
- Do not expose new public, domain, repository, or API interfaces.

- [ ] Write focused failing common and Android UI tests for editorial headers, responsive policy, semantic selection, compact navigation, and 48 dp targets.
- [ ] Run the focused tests and confirm they fail because the new presentation behavior is absent.
- [ ] Implement the minimal shared tokens, policies, components, and non-Browse editorial shell needed to pass.
- [ ] Run shared host tests and the focused Android UI test until green.
- [ ] Run Android assembly, JS/Wasm compatibility distribution, and iOS simulator tests.
- [ ] Capture a representative 390x844 non-Browse screen, compare it with the approved visual language, and update `design-qa.md` with `final result: passed` only when no P0-P2 issue remains.
- [ ] Review privacy, accessibility, and diff scope; commit with a focused message.

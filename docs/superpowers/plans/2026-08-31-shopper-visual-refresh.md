# Shopper Visual Refresh Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Apply the approved Browse marketplace visual language to Event Detail, RSVP/reveal, shopper safety, and My Finds across all current states.

**Architecture:** Reuse the shared editorial foundations from #90 and existing shopper state/presentation models. Keep state transitions and privacy policy untouched; change only internal presentation policies and shared Compose hierarchy.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Material 3.

**Spec:** GitHub issue #91, `docs/audit-assets/88-approved-browse-reference.png`, and `docs/UI_DESIGN_SYSTEM.md`.

## Global Constraints

- Preserve routes, repositories, domain models, backend contracts, save/RSVP/safety behavior, and exact-location access rules.
- Exact addresses appear only in the existing authorized protected reveal state.
- Reuse the six bundled marketplace photos and existing icon assets; add no dependency or generated imagery.
- Keep Android, iOS, JS, and Wasm buildability, 48 dp targets, keyboard use, semantic states, and screen-reader actions.
- Cover happy, loading, empty, offline, validation, blocked, cancelled, revoked, expired, and success states.

---

### Task 1: Shopper marketplace workflow restyle

**Files:**
- Modify the focused shopper screens and their existing presentation/component helpers under `app/shared/src/commonMain/kotlin/com/naslabs/yardscape/ui/`.
- Add or update common presentation tests and focused Android UI tests.
- Update `docs/UI_DESIGN_SYSTEM.md`, `design-qa.md`, and shopper screenshot evidence under `docs/audit-assets/`.

**Interfaces:**
- Consume the root editorial shell, responsive width policies, photography, status/privacy cards, segmented control, and state panels introduced by #90.
- Produce only internal shopper presentation policies and components; no public/domain/repository/API interface changes.

- [x] Write focused failing tests for photo-first detail hierarchy, compact/expanded shopper layout policy, protected reveal distinction, My Finds card treatment, safety action hierarchy, unique titles/back actions, semantic states, and 48 dp targets.
- [x] Run focused tests and observe expected failures before production edits.
- [x] Restyle Event Detail with hero photography, concise metadata, tags, host/trust information, privacy status, and a prominent RSVP or directions action.
- [x] Restyle RSVP and authorized reveal states with warm focused cards and explicit privacy language without leaking protected data.
- [x] Restyle report/block and all failure/success states with calm grouped surfaces and clear destructive-action hierarchy.
- [x] Restyle My Finds Saved/RSVP cards using the Browse photo-row grammar and distinct protected-location states.
- [x] Run shared host tests, focused Android instrumentation, Android assembly, JS/Wasm compatibility, and iOS simulator tests.
- [x] Capture 390x844 representative detail, RSVP/reveal, safety, and My Finds states; update visual QA and fix all P0-P2 findings.
- [x] Review privacy/accessibility/diff scope and commit.

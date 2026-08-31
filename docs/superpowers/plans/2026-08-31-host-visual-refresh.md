# Host Visual Refresh Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restyle the shared Host dashboard, seven-step editor, shopper preview, and attendee management screens with YardScape’s approved warm marketplace language while preserving their existing workflow behavior.

**Architecture:** Reuse the shared editorial tokens, photo artwork, privacy panels, and 48 dp target helper. Add small Host presentation helpers only where layouts need a durable, testable compact/expanded policy; the existing app state and routes remain untouched. Compose screens own their nested back navigation so the root shell does not duplicate it.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Material 3, existing local marketplace artwork.

**Spec:** GitHub issue #92 and `docs/UI_DESIGN_SYSTEM.md`

## Global Constraints

- Keep all work in shared Compose; no new dependencies, routes, domain models, backend work, or generated photos.
- Public host previews and dashboard metadata must not expose exact addresses, coordinates, or private instructions.
- Preserve the seven editor steps, confirmations, attendance transitions, messaging gates, semantics, and 48 dp interactive targets.
- Use the existing warm linen, evergreen, clay, serif-display, local-photo vocabulary at compact and expanded widths.

---

### Task 1: Host editorial surfaces

**Files:**
- Modify: `app/shared/src/commonMain/kotlin/com/naslabs/yardscape/ui/HostDashboardScreen.kt`
- Modify: `app/shared/src/commonMain/kotlin/com/naslabs/yardscape/ui/HostCreateEditScreen.kt`
- Modify: `app/shared/src/commonMain/kotlin/com/naslabs/yardscape/ui/HostAttendanceScreen.kt`
- Modify: `app/shared/src/commonTest/kotlin/com/naslabs/yardscape/ui/HostMarketplacePresentationTest.kt`
- Modify: `app/androidApp/src/androidTest/kotlin/com/naslabs/yardscape/MarketplaceAccessibilityTest.kt`
- Modify: `docs/UI_DESIGN_SYSTEM.md`
- Modify: `design-qa.md`

**Interfaces:**
- Consumes: `HostMarketplaceLayout`, `ShopperEventArtwork`, `MarketplaceEditorialBackNavigation`, `HostEditorState`, and `HostAttendanceState`.
- Produces: `hostEditorialSurfacePresentationFor(surface, layout)` for testable dashboard/editor/attendance composition and host screens using it.

- [ ] **Step 1: Write the failing presentation test**

```kotlin
@Test
fun hostEditorialSurfacesKeepPhotoLedCompactCardsAndTwoColumnExpandedActions() {
    val compact = hostEditorialSurfacePresentationFor(HostEditorialSurface.Dashboard, HostMarketplaceLayout.Compact)
    val expanded = hostEditorialSurfacePresentationFor(HostEditorialSurface.Dashboard, HostMarketplaceLayout.Expanded)

    assertEquals(128.dp, compact.artworkSize)
    assertFalse(compact.actionsInline)
    assertTrue(expanded.actionsInline)
}
```

- [ ] **Step 2: Run the focused test and confirm it fails because the presentation API does not exist.**

Run: `./gradlew :app:shared:testAndroidHostTest --tests com.naslabs.yardscape.ui.HostMarketplacePresentationTest`

- [ ] **Step 3: Implement the presentation API and apply it to dashboard cards, editor group surfaces, preview, and attendee rows.**

```kotlin
internal enum class HostEditorialSurface { Dashboard, Editor, Attendance }

internal data class HostEditorialSurfacePresentation(
    val artworkSize: Dp,
    val actionsInline: Boolean,
)

internal fun hostEditorialSurfacePresentationFor(
    surface: HostEditorialSurface,
    layout: HostMarketplaceLayout,
) = HostEditorialSurfacePresentation(
    artworkSize = if (surface == HostEditorialSurface.Dashboard) 128.dp else 96.dp,
    actionsInline = layout == HostMarketplaceLayout.Expanded,
)
```

- [ ] **Step 4: Run the focused test and confirm it passes.**

- [ ] **Step 5: Add focused Android assertions for Host navigation labels and touch targets; capture 390×844 dashboard, editor, preview, and attendee screenshots.**

- [ ] **Step 6: Run validation and commit.**

Run: `./gradlew :app:shared:testAndroidHostTest`, `./gradlew :app:androidApp:assembleDebug`, `./gradlew :app:webApp:composeCompatibilityBrowserDistribution`, and `./gradlew :app:shared:iosSimulatorArm64Test`.

Commit: `feat: restyle host marketplace workflows`

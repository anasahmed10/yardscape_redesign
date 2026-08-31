# Shared UI Design System

YardScape uses one Material 3 theme from shared Compose. Android, iOS, JS, and Wasm all enter through `App()`, which applies `YardScapeTheme` before rendering navigation or workflow screens. New UI should consume semantic Material roles and shared tokens instead of introducing platform-specific colors, fonts, or shapes.

## Foundation

| Token group | Convention |
| --- | --- |
| Color | Evergreen is the primary action color; clay and market blue provide secondary and tertiary roles; linen and white provide background and surface roles. Public and protected-location states use semantic container/content pairs so text remains associated with its surface. |
| Typography | Compose's cross-platform system typeface is retained. Headlines are bold; titles and large labels are semibold. Screens should choose a Material text style before adding a one-off weight. |
| Spacing | `YardScapeDesign.spacing` exposes 4, 8, 12, 16, 24, and 32 dp steps. Use 16 dp for normal screen/card padding, 12 dp for section gaps, and 8 dp for compact groups. |
| Shape | Extra-small 6 dp shapes are chips; small 8 dp shapes are cards and controls; 12, 16, and 24 dp shapes are reserved for progressively larger surfaces. |

Non-Browse destinations use the shared editorial shell: one linen-surface serif route title, five-destination navigation, and a 960 dp reading-width cap on expanded layouts. Nested routes receive the same title with a semantic 48 dp back action. The YardScape wordmark belongs only to Browse's map-led header and full-width discovery composition.

`SunTag` and the seeded photo-preview colors are extended decorative tokens. They should not be used for body text, protected-location policy, errors, or primary actions.

## Component conventions

- Use `MaterialTheme.colorScheme` roles rather than raw palette values when a semantic role exists.
- Use `MaterialTheme.shapes` for cards, fields, chips, and clipping so component geometry changes centrally.
- Keep exact addresses out of public components regardless of their visual treatment.
- Use the shared privacy note component for short contextual disclosure; do not treat it as final legal copy.
- Keep branding text sourced from `YardScapeConfig` and the `yardscape.appName` Gradle property. The theme contains visual tokens, not a hardcoded display name.
- Shared editorial foundations are internal Compose primitives: `MarketplaceEditorialHeader`, `MarketplaceEditorialBackNavigation`, and `MarketplaceSegmentedControl`. Existing `ShopperEventArtwork`, `ShopperSectionHeader`, `StatusLabel`, `PrivacyNote`, and `ShopperStatePanel` remain the corresponding reusable event photography, section, status, privacy, and state surfaces.
- Segmented selections and all shell navigation targets expose selected semantics and retain a 48 dp minimum interactive target.

The initial theme is deliberately light-only. Dark theme or dynamic color should be introduced as focused cross-platform work, with contrast checks on Android, iOS, JS, and Wasm rather than through an Android-only API.

## Browse marketplace composition

The compact Browse destination uses the approved map-led marketplace composition rather than the standard shell header. Its branded header, search and filter row, date pills, approximate map, floating privacy disclosure, Map/List switch, rounded results sheet, photo-first event rows, and five-item navigation form one continuous discovery surface.

- Keep Browse photography local, compressed, consistently cropped, and descriptive to assistive technology.
- Use serif type for the YardScape wordmark and event titles; use sans-serif Material typography for controls and metadata.
- Keep evergreen for discovery actions and map markers, clay for save and selected-navigation accents, linen for the page, and white for the results sheet.
- Compact navigation may display the compatibility destination `MyFinds` as **Saved**. Its route, state, and accessibility semantics remain unchanged.
- The live map may differ from illustrative geography in design references, but it must retain approximate public areas, required attribution, an accessible list alternative, and explicit optional location permission.

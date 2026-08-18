# Cross-platform map discovery implementation plan

Issue: #70

Branch: `codex/70-map-discovery`

Base: `main` at `1b2d120`

## Tasks

1. Add privacy-safe `PublicMapArea` and map viewport/cluster models in `core`, with tests proving public models have no protected address or exact-coordinate fields.
2. Add shared `MapDiscoveryState` behavior for viewport changes, debounced “Search this area”, pin/result selection, filter synchronization, restoration, permission denial, offline/tile failure, and blocked-event removal.
3. Add an `ApproximateLocationProvider` boundary and platform map capability. Android/iOS/JS render MapLibre with configurable OpenFreeMap Liberty styling; Wasm renders the same controls and a polished non-interactive fallback. Permission requests occur only after “Use my location”.
4. Replace the placeholder Browse map with synchronized responsive discovery: mobile map/results sheet, desktop split view, list alternative, clusters, selected result, attribution, retry/fallback, and accessible controls.
5. Validate common tests, Android debug, JS/Wasm compatibility, iOS simulator, and unsigned Xcode simulator build; document map/privacy/configuration behavior.

## Rulings

- Use MapLibre Compose `0.13.1`, the current stable release on Maven Central as of 2026-08-18. The requested `0.14.x` line is not published; do not use authenticated snapshots.
- OpenFreeMap style URI is configuration-backed and defaults to `https://tiles.openfreemap.org/styles/liberty` for mock-production development.
- Public centers are intentionally coarse and independent from protected `ExactAddress` coordinates.

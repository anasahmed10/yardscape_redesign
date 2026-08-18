# Map discovery

YardScape map discovery displays only neighborhood-level public areas. `PublicMapArea` enforces a minimum 500 m approximation radius and is deliberately separate from `ExactAddress`; map markers, clusters, viewport state, and public DTOs must never carry street addresses, units, access instructions, or protected coordinates.

## Platform behavior

- Android and iOS use MapLibre Compose, while JS uses MapLibre's JS bindings directly with accessible DOM markers. This avoids the Compose library's currently unimplemented JS source/layer APIs while keeping the same OpenFreeMap Liberty style and shared discovery state.
- Wasm uses the same Browse layout with a non-interactive illustrated fallback and complete list results.
- Android and iOS keep the draggable results sheet over the map. JS attaches the same draggable sheet immediately below the interactive map because Compose web interop elements render above the Compose canvas.
- The map style and attribution are centralized in `YardScapeConfig`. The development default is `https://tiles.openfreemap.org/styles/liberty`.
- OpenFreeMap is approved only for mock-production development. Tile hosting, quotas, caching, and launch SLA require a separate production review.

## Location permission

Device location is optional. No platform asks for location when Browse or the map is composed; the request begins only after **Use my location** is activated. Android requests coarse location only, iOS uses reduced/kilometer accuracy without temporary full-accuracy authorization, and JS calls browser geolocation only from that explicit action. Denial or unavailability leaves the list workflow intact.

## State and recovery

Filters, the latest searched viewport, selected sale, map/list mode, and mobile sheet position live in shared state and survive detail navigation. Camera movement is debounced before **Search this area** becomes available; confirming it spatially filters both the public markers and list results. Tile failures and offline state unmount the live map, show the fallback and a retry action, and keep matching public previews accessible in the list.

Required attribution stays visible on every map or fallback surface: OpenFreeMap, OpenMapTiles, and OpenStreetMap contributors.

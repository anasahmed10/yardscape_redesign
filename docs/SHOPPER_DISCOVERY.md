# Mock Shopper Discovery

The shared mock discovery experience validates how shoppers find and revisit public yard sale previews before YardScape commits to a geospatial backend query contract.

## Discovery paths

Shoppers can combine:

- keyword, category, neighborhood, or city search;
- Today, Tomorrow, or Weekend date filters;
- broad two-mile and five-mile distance filters;
- one or more public category filters;
- list and lightweight approximate-map modes.

The map mode is intentionally provider-free. It groups events using public neighborhood and broad distance labels and does not read or display exact addresses or precise coordinates.

Filters are shared session state in `YardScapeAppState`. A no-match result offers a one-action reset, while a truly empty catalog explains that no nearby sales are currently available.

## Saved sales

Saving stores only public event IDs in the in-memory app session. Saved cards are rebuilt from privacy-safe public previews and remain available when filters, display mode, or primary routes change. Unknown or non-public event IDs cannot be added through the saved-state API.

The Saved primary destination supports opening a public Event Detail or removing an event. Detail and RSVP routes retain Saved as their origin so Back returns to the saved list. Exact location reveal remains exclusively within the existing RSVP-authorized detail state.

These UI models are exploratory and deliberately do not define the future PostGIS search API for issue #42.

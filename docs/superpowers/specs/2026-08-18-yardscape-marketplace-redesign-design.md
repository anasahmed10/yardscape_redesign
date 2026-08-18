# YardScape Production Marketplace Redesign

## Goal

Turn the validated mock workflows into a credible cross-platform yard-sale marketplace while preserving the exact-location privacy boundary. The approved visual source is the second generated concept at:
`/Users/anas/.codex/generated_images/01a012cb-6d6e-7983-badc-5b64ab7b849a/exec-f89ead4c-5136-4833-b993-922d628280be.png`.

## Visual direction

- Preserve the existing linen, evergreen, clay, and market-blue palette.
- Use a friendly serif for the YardScape wordmark and event titles, with a legible sans-serif for controls and body copy.
- Prefer photography, lightweight separators, and whitespace over nested cards.
- Use recognizable icons with text labels in primary navigation.
- Mobile Browse uses a map with a draggable nearby-results sheet. Expanded layouts use a persistent list/map split.

## Information architecture

Primary destinations are Browse, My Finds, Host, Messages, and Account. My Finds contains Saved and RSVPs sections. Existing `/saved` and `/rsvps` links remain accepted and open the matching My Finds section.

Messages are event-scoped and available only to signed-in shoppers with an accepted, active, unblocked RSVP. Blocking, RSVP cancellation, reveal revocation, event cancellation, and expiry close the composer immediately.

## Privacy and map rules

Public map models contain an explicitly approximate neighborhood area and never protected coordinates, street address fields, access notes, or unit information. Device location is optional and requested only after the user taps Use my location. List discovery remains complete when permission, map loading, or tile loading fails.

Android and iOS use MapLibre Compose, JS uses the MapLibre web implementation, and Wasm retains a polished non-interactive map fallback. OpenFreeMap is the mock-production basemap behind a configurable style URL with visible attribution; production hosting remains a later launch decision.

## Accessibility and responsive behavior

Interactive targets are at least 48 dp. Every map action has a named semantic action and the result list is a complete accessible alternative. Validate mobile at 390 x 844 and expanded web at 1440 x 900, including keyboard order, TalkBack, VoiceOver, 200% zoom, contrast, and reduced-motion behavior.

## Delivery

Implementation is split across GitHub issues #69 through #73. Each issue produces an independently reviewed and mergeable PR.

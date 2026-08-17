# Multi-step mock host editor

The shared host create/edit journey is split into seven steps: Basics, Schedule, Location, Sale details, Photos, RSVP settings, and Preview. Forward navigation validates each intervening step and moves the host to the first field group that needs attention. Backward navigation keeps the current session state.

Draft sessions are cached in shared app state. After saving and leaving the editor, selecting the draft resumes its current step, mock photos, attendee cap, and approval mode. Publishing still writes through the seeded repository, while the mock-only attendee settings remain an application concern until backend policy contracts are introduced.

## Privacy and confirmation boundaries

- The Location step separates the approximate public area from the protected street address and access instructions.
- The Preview model contains only public-safe fields, captions, and RSVP policy summary. It has no exact address, coordinates, or private instructions.
- Publish, hide, and cancel each require a dedicated confirmation action.
- Hide and cancel retain the repository's immediate RSVP location-revocation behavior.

## Mock integrations and backend findings

`HostPhotoPicker` is an injectable application interface. The seeded implementation supplies deterministic mock choices, while selection order, captions, and removal live in the draft. Photo storage ticket #44 should preserve client ordering and captions, return stable photo identifiers, and support removal without exposing storage-provider details to shared UI.

The attendee cap and auto-accept/manual-review controls are session-scoped mock policy. Backend ticket #43 should define whether capacity counts requested, waitlisted, or accepted RSVPs; make cap changes deterministic under concurrency; and return an explicit approval policy in host event contracts without adding protected location data to public DTOs.

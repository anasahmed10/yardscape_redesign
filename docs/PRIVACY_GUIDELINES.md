# YardScape Privacy Guidelines

## Guiding Principle
YardScape handles home-adjacent location data. The product must make discovery easy without exposing a host's exact address to the public internet.

## Location Visibility
- Public event listings show approximate area only: neighborhood, city, approximate map circle, or broad distance.
- Public APIs must not include exact address, unit number, precise coordinates, gate code, host phone number, private notes, or attendee list.
- Exact address is revealed only after RSVP acceptance or another explicit location-access grant.
- MVP auto-accept is acceptable for testing only if the code still models the protected reveal boundary.
- Exact location access expires when the event ends, the event is cancelled, the host revokes RSVP access, or the attendee cancels RSVP.
- Cancelled and completed events should not continue exposing exact addresses.

## Host Controls
Hosts should be able to:

- Save drafts before publishing.
- Publish, edit, cancel, and hide events from search.
- Choose auto-accept or manual RSVP approval after MVP.
- Set attendee caps and close RSVPs.
- Revoke an attendee's exact-location access.
- Remove sensitive photos or details.
- Report abuse or suspicious behavior.

## Shopper Controls
Shoppers should be able to:

- RSVP and cancel RSVP.
- Understand when exact location will become visible.
- Save events and set reminders without revealing location early.
- Report an event or host.
- Block a host or attendee where applicable.
- Delete their account and associated personal data according to the retention policy.

## Data Retention
- Exact location reveal logs should be retained long enough for safety and abuse review, then purged or anonymized according to the approved policy.
- Exact addresses for completed events should be hidden from normal app access after expiry.
- Cancelled event locations should be revoked immediately from attendee access.
- Production must define retention windows for event data, RSVP data, location reveal logs, reports, uploaded photos, and deleted accounts.

## Abuse Prevention
- Rate limit event creation, RSVP requests, search queries, exact-location reveal requests, reports, and image uploads.
- Add moderation hooks for reported events, suspicious event volume, duplicate locations, and repeated no-shows or revocations.
- Audit sensitive actions: RSVP acceptance, exact-location reveal, revocation, cancellation, report creation, and admin review.
- Prefer least-privilege admin tools with logged access to sensitive user and location data.

## Legal And Policy Review
Before production launch, YardScape needs attorney-reviewed:

- Privacy Policy.
- Terms of Service.
- Location-data disclosure.
- User safety guidance.
- Marketplace and prohibited-content policy.
- Account deletion and data retention language.

Do not ship public production flows that collect real user location data without this review.

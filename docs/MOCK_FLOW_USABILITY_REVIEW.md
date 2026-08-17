# Mock Flow Usability Review

Review date: August 17, 2026. This is the UI-first gate before renewed backend implementation.

## Scope and evidence

The current Wasm app was exercised in the in-app browser at 1440 × 900 and 390 × 844. The review covered shopper discovery, saved sales, RSVP and reveal, RSVP cancellation, host creation, attendee management, account/trust, reporting, and blocking. The ordered screenshots in [`audit-assets`](./audit-assets/) were captured during this run; all names and addresses shown are seeded mock data.

## Walkthrough

| Step | Screen and evidence | Health | Result |
| --- | --- | --- | --- |
| 1 | Web Browse ([before](./audit-assets/01-web-browse.png), [after](./audit-assets/23-web-browse-final.png)) | Improved during review | Discovery and privacy framing are clear. Shared content is now capped at 960 dp on wide screens so cards and actions no longer stretch edge to edge. |
| 2 | Event Detail and actions ([02](./audit-assets/02-web-event-detail.png), [03](./audit-assets/03-web-detail-actions.png)) | Good | Public detail stays approximate; RSVP, report, and block are discoverable without competing with the protected-location panel. |
| 3 | Report and validation ([04](./audit-assets/04-web-report.png), [05](./audit-assets/05-web-report-validation.png)) | Good | Reason selection, bounded details, explicit non-success errors, and privacy guidance form a complete recovery path. |
| 4 | Mobile Browse ([06](./audit-assets/06-mobile-browse.png)) | Good | Controls reflow without clipping and the first result remains reachable. Search and form fields now expose explicit accessibility names. |
| 5 | Mobile Detail and RSVP ([07](./audit-assets/07-mobile-detail-actions.png), [08](./audit-assets/08-mobile-bottom-action.png), [09](./audit-assets/09-mobile-rsvp.png)) | Good | Bottom actions remain reachable above navigation; confirmation clearly explains the mock auto-accept behavior. |
| 6 | Protected reveal ([10](./audit-assets/10-mobile-location-reveal.png)) | Privacy-safe | Exact data appears only after acceptance. This screenshot intentionally contains a seeded mock address and must not be reused as public listing art. |
| 7 | Block and confirmation ([before](./audit-assets/11-mobile-block.png), [after](./audit-assets/24-mobile-block-final.png), [confirmation](./audit-assets/12-mobile-block-confirmation.png), [blocked](./audit-assets/13-mobile-blocked.png)) | Improved during review | Consequences are explicit and stale reveal is cleared. Report-only privacy copy was removed from block/unblock screens. |
| 8 | Host dashboard and creation ([14](./audit-assets/14-mobile-host-dashboard.png), [15](./audit-assets/15-mobile-host-create.png), [16](./audit-assets/16-mobile-host-validation.png)) | Good | The seven-step editor communicates progress and supports inline recovery. Host cards now use explicit Edit and Manage actions instead of nested click targets. |
| 9 | Attendee management ([17](./audit-assets/17-mobile-host-attendees.png)) | Good with contract dependency | Capacity, approval mode, reveal count, and destructive consequences are visible. Server enforcement must make capacity and acceptance one atomic policy decision. |
| 10 | Account and trust ([18](./audit-assets/18-mobile-account.png)) | Good | Mock status, confirmed facts, and community activity are clearly separated from identity or safety guarantees. |
| 11 | Saved states ([19](./audit-assets/19-mobile-saved.png), [20](./audit-assets/20-mobile-saved-populated.png)) | Improved during review | Empty and populated states are actionable. Sale cards now expose separate View and Save/Unsave controls instead of nested buttons. |
| 12 | RSVP lifecycle and cancellation ([21](./audit-assets/21-mobile-my-rsvps.png), [22](./audit-assets/22-mobile-rsvp-cancel.png)) | Good | State, next action, and immediate reveal-loss consequence are explicit. |

## Happy and failure paths

| Workflow | Happy path | Failure or loss path |
| --- | --- | --- |
| Discovery | Search/filter public previews, open detail, save a sale. | Empty results, no nearby events, offline, and recoverable refresh error have deterministic scenarios. |
| Saved and RSVP | Save, open My RSVPs, confirm attendance, reveal when authorized. | Pending, waitlisted, declined, full, cancelled, revoked, expired, and event-cancelled states keep exact data hidden. |
| Host editor | Complete steps, save draft, preview, and publish. | Required-field validation, invalid schedule/address/cap, hide, cancel, and resumable draft states are represented. |
| Attendees | Review, accept, revoke, decline, or remove one attendee with confirmation. | Capacity disables unsafe acceptance; every destructive action states its location consequence. |
| Account | Browse anonymously and resume a typed protected destination after mock sign-in. | Sign-out and expiry clear drafts, directions, confirmations, attendee state, and protected location presentation. |
| Report | Select a reason and receive an opaque mock receipt. | Validation, offline, and server failures remain visibly unsubmitted and retryable. |
| Block | Confirm a block, hide every sale from that host, and clear all affected reveal access. | Offline/server failure is non-mutating; unblock restores discovery only and never restores the old RSVP grant. |

Automated coverage for states that cannot all coexist in one browser session lives in `MockScenarioCatalogTest`, `ShopperSafetyFlowTest`, `ShopperRsvpStateTest`, `HostEditorFlowTest`, and `HostAttendanceStateTest`.

## Findings applied

- Constrained shared content width on large screens while retaining full-width mobile layouts.
- Replaced nested clickable sale and host cards with separate named actions.
- Added explicit semantics names to search, report details, address search, and host editor text fields.
- Removed report-specific data-entry guidance from block/unblock screens.
- Reworded the shell badge as mock role data, so it no longer reads like the current workflow role.

## Accessibility limits and follow-ups

Screenshots confirm responsive reflow, visible labels, readable errors, and generally large targets. Browser semantics confirmed named buttons and now-named text fields. This does not establish WCAG conformance: keyboard order, focus restoration after dialogs, screen-reader announcements, dynamic type/200% zoom, reduced motion, and measured color contrast still require device or assistive-technology testing. The Compose web semantics tree stopped exposing descendants after one completed modal mutation during this audit; this should be retested against future Compose upgrades and with Android TalkBack before production.

## Gate decision

The mock workflows are coherent enough to freeze the initial repository and DTO requirements in [Backend Contract Freeze](./BACKEND_CONTRACT_FREEZE.md). Backend tickets must implement those validated states and privacy invariants rather than inventing alternate behavior independently.

# Constraints

No SPEC-type documents were ingested. As the orchestrator directed, the constraints below come from the PRD (/Users/LeonChen/Documents/ClickMe_Android/docs/PORT_ROADMAP.md): the "Do not port" list, the iOS issues not to copy, release lead-time constraints, and cross-cutting API-contract rules the PRD states. The constraint types go beyond the standard set (api-contract | schema | nfr | protocol) with `port-exclusion`, `ios-defect` and `release-process`.

Count: 17 (port-exclusion 5, ios-defect 5, release-process 3, api-contract 4)

---

## Do not port (type: port-exclusion)
source: docs/PORT_ROADMAP.md, "Do not port"

### CON-no-port-preview-secrets
- source: docs/PORT_ROADMAP.md, "Do not port"
- type: port-exclusion
- content: Do not port `Debug/PreviewSecrets.swift`. It contains a hard-coded JWT. Rotate it. (Rotation is an action for the backend/iOS owners. It is outside Android code, and the PRD does not assign it to a week.)

### CON-no-port-preview-nav-harness
- source: docs/PORT_ROADMAP.md, "Do not port"
- type: port-exclusion
- content: Do not port `PreviewNavHarness`.

### CON-no-port-debug-logging
- source: docs/PORT_ROADMAP.md, "Do not port"
- type: port-exclusion
- content: Do not port the iOS debug logging. Use OkHttp's logging interceptor in debug builds only.

### CON-no-port-call-ready
- source: docs/PORT_ROADMAP.md, "Do not port" and Week 4 §5
- type: port-exclusion
- content: Do not port the unused `Call/Ready` screen. Nothing in iOS uses it.

### CON-no-port-quiet-hours-card
- source: docs/PORT_ROADMAP.md, "Do not port"
- type: port-exclusion
- content: Do not port the unused `NotificationQuietHoursCard`.

---

## iOS issues not to copy (type: ios-defect)
source: docs/PORT_ROADMAP.md, "iOS issues found during the survey (report back)"
Each issue is also to be reported back to the iOS side. Every item is consistent with the LOCKED ADR rule that iOS is the source of truth (DEC-ios-source-of-truth); see INGEST-CONFLICTS.md, INFO "iOS-defect fixes are consistent with the locked ADR".

### CON-ios-join-button-noop
- source: docs/PORT_ROADMAP.md, iOS issues list and Week 4 §2
- type: ios-defect
- content: On iOS, the Join button in Client Upcoming Booking Details does nothing when opened from My Bookings (`MyBookingsView.swift:108` doesn't pass `onJoinCall`). Android must wire Join (REQ-upcoming-booking-details).
- verified at ingest: the iOS call site passes only `onReschedule` and `onCancel`.

### CON-ios-overtime-test-value
- source: docs/PORT_ROADMAP.md, iOS issues list and Week 4 §5
- type: ios-defect
- content: The iOS overtime cutoff is still the 5-minute test value. Android uses 3600s (REQ-call).
- verified at ingest: ios-reference/ClickMe2026/Views/Screens/Call/Calling/MeetingCallViewModel.swift says "TEMP: cutoff is 5 min for testing; revert to 3600 (1 hour) for production."

### CON-ios-report-reason-labels
- source: docs/PORT_ROADMAP.md, iOS issues list and Week 6 §2
- type: ios-defect
- content: iOS sends report reasons as display labels instead of enum values. Android sends the six enum values (REQ-report-block-unblock).
- backed by: backend-reference/docs/ios/USER-REPORTING.md (`reason` is an enum of six values; an invalid value returns `422 VALIDATION_ERROR`).

### CON-ios-chat-no-blocked-state
- source: docs/PORT_ROADMAP.md, iOS issues list and Week 6 §2
- type: ios-defect
- content: iOS chat doesn't show a blocked state. Android disables the composer and shows a banner when blocked (REQ-report-block-unblock).
- backed by: backend-reference/docs/ios/CHAT-UNBLOCK.md.

### CON-ios-cancel-comment-dropped
- source: docs/PORT_ROADMAP.md, iOS issues list
- type: ios-defect
- content: iOS collects the cancellation comment but never sends it.
- open item: the PRD only says to report this back. It does not say whether the Android cancellation flow (REQ-client-cancellation) should collect and send the comment, or leave the field out. Check the `cancelClientBooking` contract during planning.

---

## Release and process (type: release-process)

### CON-play-org-account-lead-time
- source: docs/PORT_ROADMAP.md, "Play Store account (start this week)" and "Biggest schedule risks" #4
- type: release-process
- content: Publish from an organization Play developer account. It needs a D-U-N-S number (a new one can take a few days to several weeks) and Google's organization verification (can take days). This is an outside wait you can't control. Start in Week 1. Prepare the listing, data safety form and foreground-service declaration ahead of time, because policy review can take several days. The 14-day closed test for personal accounts does not apply.

### CON-play-store-required-features
- source: docs/PORT_ROADMAP.md, "Scope at a glance", Week 6 §2, Week 7 §2, "If you need to cut further"
- type: release-process
- content: Store-required features that must ship and must never be cut: account deletion (plus an account-deletion URL in Play Console), report/block, and support contact (`mailto:` link). The Stripe and call paths are also never cut.

### CON-foreground-service-microphone
- source: docs/PORT_ROADMAP.md, Week 4 §5 and Week 8
- type: release-process
- content: The call runs in a foreground service of type `microphone`. Play Console needs a foreground service (microphone) declaration and video before release.

---

## Cross-cutting API contract (type: api-contract)
Stated in the PRD and governed by the LOCKED DEC-backend-contract (backend-reference/docs/ios/ is authoritative).

### CON-api-refresh-token-reuse
- source: docs/PORT_ROADMAP.md, Week 1 §2
- type: api-contract
- content: The backend revokes the session if a refresh token is reused. Token refresh must be single-flight (an OkHttp `Authenticator` guarded by a Mutex), retry once, and log out if the refresh itself fails. The realtime socket must reconnect with the new token after each refresh (Week 2 §4).

### CON-api-dates-microseconds
- source: docs/PORT_ROADMAP.md, Week 1 §2
- type: api-contract
- content: The backend sends ISO-8601 dates with microseconds. JSON is snake_case. Errors use the envelope `{status, code?, message}`, plus the `errors[]` and status-only shapes. Unknown error codes map to null.

### CON-api-money-units
- source: docs/PORT_ROADMAP.md, Week 3 §4 and Week 7 §1
- type: api-contract
- content: Money is in minor units everywhere except `payment-intent` amounts. JPY has no decimal places. Payout transactions are in cents, and `earnings_breakdown` is in dollars. `net_amount` can be null; check `fee_source` first.

### CON-api-production-backend
- source: docs/PORT_ROADMAP.md, Week 1 §1 and Week 6 §3
- type: api-contract
- content: All environments point at the production backend: `https://clickme-backend-production.up.railway.app/api/v1`. Push registration uses `POST /notifications/tokens` with `platform: "android"`, which the backend schema accepts.

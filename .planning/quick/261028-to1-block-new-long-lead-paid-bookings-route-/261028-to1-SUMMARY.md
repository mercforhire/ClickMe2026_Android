---
phase: quick-261028-to1
plan: 01
subsystem: payments
tags: [booking, stripe, push, build-config, network-security]
requires: []
provides:
  - paid booking window (6 days) for paid topics
  - payment_actions push routing
  - dev BASE_URL override and debug localhost cleartext
  - local Stripe device-test runbook
affects: [make-a-booking, push, build]
key-files:
  created:
    - app/src/debug/AndroidManifest.xml
    - app/src/debug/res/xml/network_security_config.xml
    - .planning/phases/07-payment-methods-paid-booking/07-LOCAL-STRIPE-TESTING.md
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/BookingSlots.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingUiState.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/PaidFlowError.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/subviews/MakeABookingTimeSlotsCard.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/push/PushEvent.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/push/PushDestination.kt
    - app/build.gradle.kts
decisions:
  - "User decision 2026-10-28 (option B): block new long-lead paid bookings for launch; Android deviates from iOS on purpose"
  - "payment_actions push routes to the client booking detail only; no recover-payment"
status: complete
commits: 3
plan_head_before: 80c3d364dcc535d2561ab9d34f68de344a72e044
plan_head_after: 32827dd
actuals:
  tokens: 60000
  tasks: 3
  commits: 3
completed: 2026-10-28
---

# Quick 261028-to1: Block new long-lead paid bookings, route payment_actions push, local Stripe test backend

**For a paid topic only slots at most 6 days (144 h) ahead can be booked, a long-lead 422 is an error dialog, the `payment_actions` push opens the client booking detail, and the dev flavor can target a local sandbox backend.**

## What was built

- **Task 1 (801d64f).** `PAID_BOOKING_MAX_LEAD` (144 h) with a required `latestStart` parameter on `isSelectable` and `openDays`, so the compiler found every call site. `MakeABookingUiState.latestSelectableStart` is derived from the selected topic. The ViewModel gates `selectSlot`, `applyAvailability`, `book()` and `loadTopics` (a slot held while topics loaded is cleared once a paid topic applies). `syncClock` passes null, because the window only widens over time. The slots card dims out-of-window chips and shows the paid-window note. A 422 `LONG_LEAD_REQUIRES_SAVED_CARD` maps to the paid-window dialog and resets the flow. The long-lead confirm function, the preferred-card helper, the saved-cards field and the no-saved-card string were removed.
- **Task 2 (eeaa55a).** `PAYMENT_ACTION_REQUIRED` event_type fallback and `payment_actions` routed to `BookingDetail(id, Client)`. The dev flavor `BASE_URL` comes from `clickme.devBaseUrl` in `local.properties` (validated, warns when set); the prod flavor always uses `productionBaseUrl`. Debug-only network security config permits cleartext to `localhost` and `127.0.0.1` only.
- **Task 3 (32827dd).** Runbook `07-LOCAL-STRIPE-TESTING.md`; REQUIREMENTS PAY-04 amended (traceability row, Out of Scope row, footer); STATE Decisions, Blockers/Concerns (Phase 7 sub-bullets, SC-3 amendment, new cross-team follow-ups entry); `07-BACKEND-QUESTIONS.md` decision section.

## Verification

- `testDevDebugUnitTest`: 3629 tests, 0 failures, 0 skipped (dev flavor).
- `ktlintCheck`, `assembleDevDebug`, `lintDevDebug`: pass. `scripts/check-design-rules.sh`: `Design rules: OK`.
- Removed-path grep (`performLongLeadBooking|preferredPaymentMethodId|savedPaymentMethods|make_a_booking_error_no_saved_card`) over `app/src/main` and `app/src/test`: 0 hits (12 at HEAD).
- With `clickme.devBaseUrl=http://localhost:3000/api/v1`: dev BuildConfig `BASE_URL` is the localhost URL, prod BuildConfig stays production; the devDebug merged manifest has `networkSecurityConfig`; the prodRelease merged manifest has neither `networkSecurityConfig` nor `usesCleartextTraffic`. Override removed: dev is production again. `local.properties` restored (no `clickme.devBaseUrl` line).
- Manual stray-quote check (`...api/v1"`): configuration failed with "clickme.devBaseUrl in local.properties must be an http(s) URL without spaces, quotes or backslashes". File restored.
- `socketUrl("http://localhost:3000/api/v1")` is `http://localhost:3000` (new test), so `RealtimeModule`'s `socketUrl(AppConfig.baseUrl)` yields that for the local backend. `socketUrl` itself needed no change.
- Tests added: BookingSlotsTest (4), MakeABookingUiStateTest (3), MakeABookingViewModelPaidTest (422 test plus 3 window tests, 3 old long-lead tests removed), PaidFlowErrorTest (1), MakeABookingCalendarSlotsTest (3), PushEventTest (1, table size now 19), PushRouterTest (1 plus rows), RealtimeUrlTest (1, BuildConfig test relaxed to allow only loopback http).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] ktlint line length and `java.net.URI` resolution**
- **Found during:** Tasks 1 and 2
- **Issue:** Several new one-liners exceeded the 120 column limit; in `build.gradle.kts` `java.net.URI` resolved against Gradle's `java` extension.
- **Fix:** Added a `latest` local and split expressions in `MakeABookingViewModel`/`MakeABookingUiState`; `import java.net.URI` in the build script.
- **Commits:** 801d64f, eeaa55a

The plan's step to move paid-topic test slots more than 6 days out was not needed: no other existing test selected such a slot.

**Total deviations:** 1 auto-fixed (blocking). **Impact:** none on behavior.

## Known Stubs

None.

## Threat Flags

None. The debug cleartext surface and the `clickme.devBaseUrl` override are the plan's own T-261028-to1-01 and -02.

## Open items

- Device runs (paid window on a phone, 3DS, decline then retry, payment_actions tap) follow the runbook and stay open per the deferred-manual-testing preference. They need a uat expert with a sandbox Connect account (backend item).
- iOS follow-up and optional backend server-side long-lead gate are recorded in STATE Blockers/Concerns.

## Self-Check: PASSED

All created files exist and commits 801d64f, eeaa55a and 32827dd are in the branch history.

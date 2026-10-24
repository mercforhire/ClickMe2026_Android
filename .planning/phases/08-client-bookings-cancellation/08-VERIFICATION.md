---
phase: 08-client-bookings-cancellation
verified: 2026-10-23T00:00:00Z
status: human_needed
score: 4/4 roadmap success criteria verified in code (device checks outstanding)
overrides_applied: 0
gaps: []
human_verification:
  - test: "SC-1 My Bookings on the Samsung SM-A075F (devDebug, client login)"
    expected: "Upcoming and Past tabs; a completed-but-not-ended session stays in Upcoming; Past labels read Completed, Cancelled or Missed; pull-to-refresh works; an iOS action refreshes the list through booking.update; a quiet reload happens on tab return."
    why_human: "Device and realtime behavior, with the iOS app as a second account. Already logged as [Phase 8] DEFERRED (human) in STATE.md."
  - test: "SC-2 Upcoming Booking Details"
    expected: "pending_approval shows the 'Waiting for expert to accept' banner; confirmed shows the inert Join placeholder; a Skype or Zoom link is shown and copied; tapping a Today's Sessions card on Explore opens the details."
    why_human: "Visual check and cross-screen navigation on a device."
  - test: "SC-3 Cancel, free booking and held paid booking, with a comment"
    expected: "The booking moves to Past as Cancelled, the result snackbar shows, and the iOS expert sees the cancellation. The paid case is blocked on the test-mode Stripe backend."
    why_human: "Needs a live backend and the iOS expert app. The backend does not store the reason, so the comment cannot be confirmed from the iOS side."
  - test: "SC-4 Reschedule response, with the iOS expert proposing"
    expected: "Android accepts and declines (with and without a reason) and the booking leaves pending_reschedule. PAYMENT_METHOD_REQUIRED recovery with a saved card and with none (blocked on the test-mode Stripe backend). Booking Summary Book Again."
    why_human: "Needs the iOS expert to start the proposal. The card branch needs the test-mode Stripe backend."
  - test: "Visual and system checks"
    expected: "200% font scale on My Bookings, Upcoming Details with a reschedule card, Cancel step 2 and Summary. Rotation and process death on Cancel step 2. Back during an in-flight cancel. JPY refund snackbar formatting."
    why_human: "Visual and device-only behavior."
---

# Phase 8: Client Bookings & Cancellation Verification Report

**Phase Goal:** A client can track their bookings, cancel with a refund preview, and respond to an expert's reschedule proposal.
**Verified:** 2026-10-23
**Status:** human_needed. Every success criterion is implemented, wired and covered by green tests. The only open items are the device and cross-platform checks the project policy defers (STATE.md "[Phase 8] DEFERRED (human)").
**Re-verification:** No, initial verification.

## Goal Achievement

### Observable Truths (ROADMAP Success Criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | My Bookings shows Upcoming and Past tabs. A completed booking whose end time hasn't passed stays in Upcoming. Past items are labeled completed, cancelled or missed. | VERIFIED | `mybookings/BookingBuckets.kt` `buildBookingLists` pulls `isRejoinableCompleted` rows (`status == completed && endTime.isAfter(now)`) out of the Past bucket and into Upcoming. `BookingStatusDisplay.kt` `pastStatusDisplay` maps completed to Completed, cancelled and declined to Cancelled, missed and expired to Missed, and drops anything else. `MyBookingsViewModel` loads both buckets on every load. `HomeClientScreen.kt:98` mounts `MyBookingsScreen` as the Bookings tab root. |
| 2 | Upcoming Booking Details shows the "Waiting for expert" banner on pending bookings, plus the join card and Cancel. There is no Reschedule action. | VERIFIED | `UpcomingBookingScreen.kt` renders the banner (`upcoming_booking_pending_title` = "Waiting for expert to accept"), the join card (inert until Phase 9, by design) and Cancel behind `details.canCancel`. `CANCELLABLE_BOOKING_STATUSES` = confirmed, pending_approval, pending_reschedule. No Reschedule string or button exists in the screen or the strings. The screen is registered in `ClientBookingManagementEntries.kt` and mounted in `HomeClientScreen.kt:150`. Explore's Today's Sessions opens it through `onOpenBooking = navigator::openUpcomingBooking`. |
| 3 | The client cancels with a reason after seeing the refund preview. The booking moves to Past as cancelled, and the iOS expert sees the cancellation. | VERIFIED in code (iOS-side visibility deferred) | `ClientCancellationViewModel`: two steps (Reason, Confirm) with state saved in `SavedStateHandle`. `refundPreview` follows the 24h rule. `confirm()` is single-flight and sends `repository.cancel(bookingId, cancelReason(reason, comment))`, which posts `POST client/bookings/{id}/cancel` with body `{reason}`. The route and `cancelSchema` match `backend-reference/src/routes/clientBooking.js:33`. On success it posts `BookingsLaunch(BookingsTab.Past, notice)` and sets `isFinished`. `onCancelled` is `navigator::showMyBookings`, which resets both stacks and selects Bookings. 404 and 409 return to Upcoming. Whether the iOS expert sees it is a backend behavior and needs the device check. |
| 4 | When the iOS expert proposes a reschedule, the Android client accepts or declines it from Booking Summary, and the booking leaves `pending_reschedule`. | VERIFIED in code (live round-trip deferred) | `RescheduleResponder.accept` sends `{}` first, then on PAYMENT_METHOD_REQUIRED retries once with the default or first saved card. `decline` sends the trimmed reason. Endpoints `accept-reschedule` and `decline-reschedule` match the backend routes (`clientBooking.js:36-37`). `RescheduleResponseController` reloads the booking after the response. One `RescheduleResponseCard` is used by both `BookingSummaryScreen` and `UpcomingBookingScreen`, and both screens are mounted in `HomeClientScreen.kt`. |

**Score:** 4/4 verified in code. SC-3 (iOS expert visibility) and SC-4 (live round-trip) also need the deferred device check.

### Required Artifacts

All of the roughly 45 paths declared in the PLAN `must_haves.artifacts` exist and are substantive (14 to 601 lines each). None is missing or a stub. The 86-file review set was read by the code reviewer, and this check found no stubs among the artifacts sampled.

### Key Link Verification

| From | To | Status | Details |
|------|----|--------|---------|
| Bookings tab root | `MyBookingsScreen` | WIRED | `HomeClientScreen.kt:98`, with `onOpenUpcoming`, `onOpenPast`, `onExploreExperts`. |
| Upcoming card | `UpcomingBookingScreen` | WIRED | `openUpcomingBooking` is `navigate(UpcomingBookingKey)`. The Explore Today's Sessions card uses the same function. |
| Past card | `BookingSummaryScreen` | WIRED | `openBookingSummary`. |
| `UpcomingBookingScreen` Cancel | `ClientCancellationScreen` | WIRED | `navigator.navigate(CancelBookingKey(id))`. |
| Cancel success | My Bookings (Past tab, notice) | WIRED | `BookingsLaunchRequest.post`, then `showMyBookings` (`resetAndSelect`). |
| `ClientBookingsRepository.cancel`, `acceptReschedule`, `declineReschedule` | `ClickMeApi` and backend | WIRED | Paths and verbs match the backend routes. |
| Reschedule card | Payment Methods (recovery) | WIRED | `onOpenPaymentMethods` navigates to `PaymentMethodsKey`, and the screens reload on resume. |

### Data-Flow Trace (Level 4)

| Artifact | Data | Source | Real data | Status |
|----------|------|--------|-----------|--------|
| `MyBookingsViewModel` | upcoming and past lists | `GET client/bookings` for both types, then `buildBookingLists` | Yes | FLOWING |
| `UpcomingBookingViewModel` and `BookingSummaryViewModel` | booking detail | `repository.detail(id)` (`GET client/bookings/{id}`) | Yes | FLOWING |
| `ClientCancellationViewModel` | detail and refund preview | `repository.detail`, then `refundPreview(detail, clock)` | Yes | FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Phase 8 unit tests (mybookings, cancellation, bookingsummary, upcomingbooking, MoneyTest, HomeShellsTest) | `./gradlew --offline :app:testDevDebugUnitTest --tests ...` | 329 tests, 0 failures, 0 errors, 0 skipped. BUILD SUCCESSFUL. | PASS |
| Full gate (dev and prod 1888 tests, ktlint, lint, assembleDevDebug, R8 assembleProdRelease) | Not re-run. Taken from the 08-15 report and STATE.md. | n/a | SKIP |

### Probe Execution

Step 7c: SKIPPED. The phase declares no `probe-*.sh` probes.

### Requirements Coverage

| Requirement | Source plans | Status | Evidence |
|-------------|--------------|--------|----------|
| BOOK-05 My Bookings, tabs, rejoin rule, Past labels | 08-01, 02, 06, 07, 12 | SATISFIED | `BookingBuckets.kt`, `BookingStatusDisplay.kt`, `MyBookingsViewModel`, `MyBookingsScreen`. |
| BOOK-06 Upcoming Booking Details (banner, join card, Cancel, no Reschedule) | 08-01, 02, 10, 13 | SATISFIED | `UpcomingBookingScreen`, `UpcomingBookingViewModel`. |
| BOOK-07 Cancel with reason picker and refund preview, comment inside `reason` | 08-01, 03, 04, 08, 11 | SATISFIED | `ClientCancellationViewModel`, `CancellationReason`, `RefundPreview`. |
| BOOK-08 Minimal Booking Summary | 08-02, 06, 09 | SATISFIED | `BookingSummaryScreen`, `BookingSummaryViewModel`. |
| BOOK-09 Accept or decline an expert's reschedule | 08-01, 04, 05, 06, 09, 10, 13 | SATISFIED | `RescheduleResponse.kt`, `RescheduleResponseCard`. |

All five IDs listed in PLAN frontmatter appear in REQUIREMENTS.md and are marked Complete there. REQUIREMENTS.md maps no other ID to Phase 8, so there are no orphaned requirements.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `UpcomingBookingStatusRow.kt` | 50 | `XXXXXXXX` | Info | Not a debt marker. It is a KDoc example of the "ID: #XXXXXXXX" format. |

No TBD, FIXME or TODO markers in the Phase 8 sources. The Join controls are intentionally inert, marked `PHASE-9`, and the roadmap puts Join in Phase 9 (CALL-01). This is not a stub against Phase 8.

### Code Review Warnings (08-REVIEW.md, 6 warnings, 0 critical)

None of the six breaks a success criterion. They are edge-case or hardening issues, and the happy path of each criterion is implemented and tested.

| ID | Issue | Effect on success criteria |
|----|-------|----------------------------|
| WR-01 | The post-accept reload can be cancelled by a concurrent `booking.update` or resume reload (I confirmed that `startLoad` cancels `loadJob` and that `reload` is `startLoad(true).join()`). The stale card briefly stays actionable. If the newer quiet load fails, the stale card stays until the next trigger. | SC-4 still holds in the normal case. The newer load lands within moments and the booking leaves `pending_reschedule`. A race window remains. |
| WR-02 | An idempotent 200 shows "accepted" or "declined" even when the booking was resolved some other way. | Wrong notice in rare multi-device or expert-withdrew cases only. |
| WR-03 | Book Again, then Request Sent, then "Back to Home" leaves Request Sent on the Bookings stack. | Hits a Book Again path (BOOK-08 extra), not a stated success criterion. |
| WR-04 | Today's Sessions on Explore is not refreshed after a cancel, accept or decline begun from it. | My Bookings is correct. Only the Explore strip is stale until pull-to-refresh. |
| WR-05 | `Money.formatMajor` is wrong for 3-decimal currencies and for zero-decimal currencies outside the backend's 7-code set. | The refund branch is dormant on today's backend (the hold is voided, so `refund_amount` is 0). The common JPY and CAD cases are right. |
| WR-06 | The Accept dialog stays open if the proposal changes underneath it, and it doesn't show the time being accepted. | Hardening. Accept still works. |

Recommendation: fix WR-01 and WR-06 before Phase 9 depends on this screen (the review notes that the fixes must be made in both detail screens). Fix WR-03 and WR-04 in a Phase 15 polish pass or earlier. Track WR-02 and WR-05 as follow-ups.

### Human Verification Required

The five items are in the frontmatter. They match the non-blocking deferred list in STATE.md under "[Phase 8] DEFERRED (human)" and are not treated as gaps. The Stripe card-based checks are blocked on the test-mode backend, as logged under "[Phase 7] Stripe key and mode".

### Gaps Summary

No blocking gaps. All four success criteria and all five requirements (BOOK-05 to BOOK-09) are implemented, wired from the app shell, backed by real network calls that match the backend routes, and covered by a green test run. I re-ran 329 Phase 8 unit tests with 0 failures, and the 08-15 full-gate result is recorded as green. The status is `human_needed` because the device and cross-platform checks are outstanding, and the project policy defers them.

---

_Verified: 2026-10-23_
_Verifier: Claude (gsd-verifier)_

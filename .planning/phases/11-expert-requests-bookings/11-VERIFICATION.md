---
phase: 11-expert-requests-bookings
verified: 2026-10-26T20:00:00Z
status: human_needed
score: 4/4 must-haves verified
covered_files:
  - .planning/phases/11-expert-requests-bookings/11-01-PLAN.md
  - .planning/phases/11-expert-requests-bookings/11-01-SUMMARY.md
  - .planning/phases/11-expert-requests-bookings/11-02-PLAN.md
  - .planning/phases/11-expert-requests-bookings/11-02-SUMMARY.md
  - .planning/phases/11-expert-requests-bookings/11-03-PLAN.md
  - .planning/phases/11-expert-requests-bookings/11-03-SUMMARY.md
  - .planning/phases/11-expert-requests-bookings/11-04-PLAN.md
  - .planning/phases/11-expert-requests-bookings/11-04-SUMMARY.md
  - .planning/phases/11-expert-requests-bookings/11-05-PLAN.md
  - .planning/phases/11-expert-requests-bookings/11-05-SUMMARY.md
  - .planning/phases/11-expert-requests-bookings/11-06-PLAN.md
  - .planning/phases/11-expert-requests-bookings/11-06-SUMMARY.md
  - .planning/phases/11-expert-requests-bookings/11-07-PLAN.md
  - .planning/phases/11-expert-requests-bookings/11-07-SUMMARY.md
  - .planning/phases/11-expert-requests-bookings/11-08-PLAN.md
  - .planning/phases/11-expert-requests-bookings/11-08-SUMMARY.md
  - .planning/phases/11-expert-requests-bookings/11-09-PLAN.md
  - .planning/phases/11-expert-requests-bookings/11-09-SUMMARY.md
  - .planning/phases/11-expert-requests-bookings/11-10-PLAN.md
  - .planning/phases/11-expert-requests-bookings/11-10-SUMMARY.md
  - .planning/phases/11-expert-requests-bookings/11-11-PLAN.md
  - .planning/phases/11-expert-requests-bookings/11-11-SUMMARY.md
  - .planning/phases/11-expert-requests-bookings/11-12-PLAN.md
  - .planning/phases/11-expert-requests-bookings/11-12-SUMMARY.md
  - .planning/phases/11-expert-requests-bookings/11-13-PLAN.md
  - .planning/phases/11-expert-requests-bookings/11-13-SUMMARY.md
  - .planning/phases/11-expert-requests-bookings/11-14-PLAN.md
  - .planning/phases/11-expert-requests-bookings/11-14-SUMMARY.md
  - .planning/phases/11-expert-requests-bookings/11-15-PLAN.md
  - .planning/phases/11-expert-requests-bookings/11-15-SUMMARY.md
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/cancellation/ExpertCancellationViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/confirmed/BookingConfirmedScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/confirmed/CalendarIntent.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/ExpertDashboardScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/ExpertDashboardViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/home/HomeExpertScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/incomingrequests/IncomingRequestsViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/RequestDecisionViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/ExpertBookingsViewModel.kt
covered_digest: "v3:sha256:673ba07cfd601f29b79f0a21216659729c3892ebfeb47980305f252dacb8acb9"
behavior_unverified: 0
overrides_applied: 0
human_verification:
  - test: "SC-2 / SC-3 cross-client: an iOS client books; the Android expert sees the request, accepts (with and without a message) and declines (each of the four reasons); then the iOS client and the Android expert both join the call inside the join window (Dashboard card, Upcoming card, Summary)."
    expected: "Request appears in Incoming Requests; Confirmed screen shows; the iOS client sees the decline; both sides are in the same Agora call (PRD Week 5 checkpoint)."
    why_human: "Needs a live iOS client and a device with microphone/Agora; cannot be exercised by unit tests or grep. Recorded as non-blocking in STATE.md ([Phase 11] DEFERRED (human), items b and c)."
  - test: "SC-4 cross-client: the iOS client proposes a reschedule; the expert accepts and declines it from the Summary; produce a SLOT_UNAVAILABLE with two back-to-back bookings; cancel a free and a held paid booking."
    expected: "Accept/decline succeed; the SLOT_UNAVAILABLE case shows 'That time is no longer available' and the booking stays; cancel lands on the Past tab with the snackbar."
    why_human: "Needs a second client and real backend state (30-minute session buffer). The error mapping and flows are covered by unit tests; the live backend path is not."
  - test: "SC-1 device run: Dashboard shows pending count, reschedule banner, Today's Sessions with Join, weekly earnings and the payout onboarding card; the card opens a Stripe Custom Tab and the status refreshes on return; 'Add to Calendar' opens the calendar editor with the right title, times and description."
    expected: "As described; no flicker on quiet reloads; 200% font scale and TalkBack are usable."
    why_human: "Visual, real-time and external-service behaviour (Stripe-hosted onboarding, calendar app)."
  - test: "Prohibition review (judgment): the Phase 11 plans forbid logging client messages, decline/cancel reasons, client names or the Stripe onboarding URL on expert screens."
    expected: "No logging of that data."
    why_human: "unverified-prohibition: the plan tagged this test-tier, but no automated negative test enforces it. My grep of the expert screen packages found no Log./Timber/println calls, so this is a non-authoritative pass. Human review recommended."
---

# Phase 11: Expert Requests & Bookings Verification Report

**Phase Goal:** An Android expert can see their workload, accept requests, manage bookings and join sessions, so the expert side works end to end.
**Verified:** 2026-10-26T20:00:00Z
**Status:** human_needed
**Re-verification:** No, initial verification

## Goal Achievement

The code delivers every roadmap success criterion. Each screen is real (not a stub), wired into the expert shell, backed by repositories that call endpoints which exist in `backend-reference`, and covered by passing tests. The remaining items need a device or a live iOS client, which the user has deferred as non-blocking.

### Observable Truths

| # | Truth (ROADMAP success criterion) | Status | Evidence |
|---|-----------------------------------|--------|----------|
| 1 | Dashboard shows pending request count, upcoming sessions, weekly earnings text and a payout onboarding card | VERIFIED (code) | `ExpertDashboardRoute` is the Dashboard tab root in `HomeExpertScreen.kt`. `ExpertDashboardViewModel.startLoad` fetches upcoming bookings and pending requests in parallel, then weekly earnings, payout summary and Connect status. `applyLoaded` sets `pendingCount = pending.size`, `todaysSessions`, `rescheduleCount`, `weekly`, `payoutSummary` and `onboarding`. `ExpertDashboardScreen` renders `PendingRequestsBanner`, `RescheduleRequestsBanner`, `TodaysSessionsSection`, `FinancialSummarySection` (EarningsCard + `PayoutOnboardingCard`) and the tools grid. The card mints a Stripe link behind `CallGuard` and opens it in a Custom Tab. The Availability tool pushes `AvailabilityKey(AvailabilityOrigin.Dashboard)`. Live Stripe and visual checks are in human verification. |
| 2 | Android expert sees an iOS client's request in Incoming Requests, accepts with an optional message or declines with a reason code, sees Confirmed, adds the session to the device calendar | VERIFIED (code) | Dashboard banner and tools navigate to `IncomingRequestsKey`, then `RequestDecisionKey`, then `BookingConfirmedKey`. All are registered in `HomeExpertScreen.detailEntries`. `RequestDecisionViewModel.accept` calls `BookingRequestsRepository.accept` (`POST expert/booking-requests/{id}/accept`, body `{}` or `{message}`) and the screen does `replaceTop(BookingConfirmedKey)`. `decline` sends `reason_code` in `CONFLICT/OUT_OF_SCOPE/UNAVAILABLE/OTHER` and a non-empty `reason_text` (the label when the note is blank). Both match the backend's strict `declineSchema` and `acceptSchema`. `ALREADY_PROCESSED` has its own dialog and refresh. `CalendarIntent.kt` builds an `ACTION_INSERT` intent on `CalendarContract.Events.CONTENT_URI` and handles no-handler with a snackbar. No calendar permission is requested. |
| 3 | Expert Bookings (Upcoming/Past) shows Join inside the join window and the expert can join the call | VERIFIED (code); cross-client call needs a human | `ExpertBookingsViewModel` loads both buckets. `buildExpertBookingLists` applies the two-bucket and rejoinable-completed rules. `ExpertUpcomingCard` renders the shared `JoinCallButton`, enabled by `isWithinJoinWindow(...)` against the shared `JoinClock`. `join()` calls `CallStarter.startCall` with the client as peer. The same call exists on the Dashboard card and the Summary. Same pattern as the client side (button always shown, enabled only in the window). |
| 4 | Expert can accept or decline a client's reschedule request, sees a clear message on `409 SLOT_UNAVAILABLE`, and can cancel a booking | VERIFIED (code) | `ExpertBookingSummaryViewModel.acceptReschedule` / `declineReschedule` are guarded by `CallGuard.attemptModifyBooking(RespondToReschedule)` and run the shared `RescheduleResponseController` through `ExpertRescheduleResponder`, which calls `expert/bookings/{id}/accept-reschedule` and `/decline-reschedule`. `toExpertRescheduleFailure` maps `ErrorCode.SlotUnavailable` to `expert_reschedule_error_slot_unavailable` ("That time is no longer available...") with a reload. `ErrorCode.SlotUnavailable` exists. Cancel: Summary `attemptCancel` pushes `ExpertCancelKey`; `ExpertCancellationViewModel.confirm` is guarded, posts `expert/bookings/{id}/cancel` and hands the result to the Bookings Past tab. |

**Score:** 4/4 truths verified at code level (0 present, behavior-unverified). Cross-client and device confirmation is carried in `human_verification`.

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `screens/expertmode/dashboard/*` (route, VM, screen, financials, logic, subviews) | EXP-05 | VERIFIED | Substantive (310-line VM, 274-line screen), wired in `HomeExpertScreen`, data flows from the repositories. |
| `screens/expertmode/incomingrequests/*` | EXP-06 | VERIFIED | Entries registered; the repository calls the real API. |
| `screens/expertmode/requestdecision/*` | EXP-06 | VERIFIED | Accept and decline sheets, VM, error mapping, earnings. |
| `screens/expertmode/confirmed/*` | EXP-06, EXP-07 | VERIFIED | Screen, calendar intent, Back to Home posts a Dashboard reload. |
| `screens/expertmode/upcomingbookings/*` | EXP-08 | VERIFIED | Buckets, VM, cards, Join. |
| `screens/expertmode/bookingsummary/*` | EXP-08, EXP-09 | VERIFIED | Summary, reschedule responder and errors. |
| `screens/expertmode/cancellation/*` | EXP-09 | VERIFIED | Two-step cancel, refund preview, error mapping. |
| `network/ClickMeApi.kt`, `ApiRequestUrls.kt`, 3 repositories | 14 expert endpoints | VERIFIED | Every path matches a route in `backend-reference/src/routes` (`bookingRequests.js`, `expertBooking.js`, `earnings.js`, `connect.js`, `payouts.js`). |
| `COVERAGE.md` | Opt-outs recorded | VERIFIED | Opt-outs are explicit and reasoned. |
| `.planning/STATE.md` deferred list | `[Phase 11] DEFERRED (human)` | VERIFIED | Present with items a to e. |
| `11-VALIDATION.md` | `nyquist_compliant: true` | VERIFIED | `status: complete`, `nyquist_compliant: true`. |

### Key Link Verification

| From | To | Via | Status |
|------|----|-----|--------|
| `HomeExpertScreen` | Dashboard and Bookings roots | `ExpertDashboardRoute` / `ExpertBookingsRoute` | WIRED |
| `HomeExpertScreen.detailEntries` | 5 feature entry registrations | `incomingRequestsEntries`, `requestDecisionEntries`, `bookingConfirmedEntries`, `expertBookingSummaryEntries`, `expertCancelEntries` | WIRED |
| Dashboard | Incoming Requests, Summary, Availability | `navigator.navigate(IncomingRequestsKey / ExpertBookingSummaryKey / AvailabilityKey(Dashboard))` | WIRED |
| Incoming Requests | Request Decision | `IncomingRequestRow.toDecisionKey()` | WIRED |
| Request Decision | Confirmed | `replaceTop(BookingConfirmedKey)` | WIRED |
| Summary | Cancel | `ExpertCancelKey(bookingId, origin)` after the guard | WIRED |
| Join (Dashboard, Bookings, Summary) | Phase 9 call stack | `CallStarter.startCall` | WIRED |
| Reschedule and cancel | Phase 9 guard | `CallGuard.attemptModifyBooking` | WIRED |

### Data-Flow Trace (Level 4)

| Artifact | Data | Source | Real data | Status |
|----------|------|--------|-----------|--------|
| Dashboard pending count | `pending.size` | `GET expert/booking-requests` | Yes | FLOWING |
| Dashboard weekly earnings | `WeeklyEarningsData.netAmount` | `GET expert/earnings/weekly` | Yes | FLOWING |
| Payout card | `resolvePayoutOnboarding(status, summary, was400)` | `GET expert/connect/status`, `payouts/summary` | Yes | FLOWING |
| Bookings lists | `buildExpertBookingLists` | `GET expert/bookings?type=upcoming/past` | Yes | FLOWING |
| Summary | `ExpertSummaryDetails` | `GET expert/bookings/{id}/details` | Yes | FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Phase 11 unit and Robolectric tests | `./gradlew --offline testDevDebugUnitTest --tests "...screens.expertmode.*" --tests "...network.*Expert*" --tests "...BookingRequestsApiTest" --tests "...RescheduleResponseCardTest" --tests "...ExpertNavigationTest" --tests "...HomeShellsTest"` | BUILD SUCCESSFUL; 58 result files, 769 tests, 0 failures, 0 errors, 0 skipped | PASS |
| Orchestrator gate | `testDevDebugUnitTest ktlintCheck assembleDevDebug`, plus the 11-15 full gate (both flavors, lint, R8) | Reported green by the caller; not re-run here | NOT RE-RUN |

### Probe Execution

No probes declared by the phase. Skipped.

### Requirements Coverage

Every ID in the PLAN frontmatter appears in REQUIREMENTS.md, and none is orphaned. All five are mapped to Phase 11 and marked Complete there.

| Requirement | Source plans | Description | Status | Evidence |
|-------------|--------------|-------------|--------|----------|
| EXP-05 | 11-01, 02, 03, 10, 14, 15 | Dashboard: pending count, upcoming sessions, weekly earnings, payout card | SATISFIED | Truth 1 |
| EXP-06 | 11-01, 02, 03, 05, 06, 07, 12, 15 | Incoming Requests, accept with message or decline with reason, Confirmed | SATISFIED | Truth 2. The Android side sends the message; the backend discards it (see Gaps Summary, WR-08). |
| EXP-07 | 11-01, 02, 07, 15 | Add to calendar via `Intent.ACTION_INSERT` | SATISFIED | `CalendarIntent.kt`; `CalendarIntentTest` |
| EXP-08 | 11-01, 02, 03, 04, 08, 10, 11, 15 | Upcoming and Past bookings, Join inside the window | SATISFIED | Truth 3 |
| EXP-09 | 11-01, 02, 03, 08, 09, 13, 15 | Accept or decline reschedule (409 SLOT_UNAVAILABLE), cancel | SATISFIED | Truth 4 |

### Prohibitions (must_haves.prohibitions, plan 11-15)

| Prohibition | Tier | Result |
|-------------|------|--------|
| MUST NOT log client message, decline or cancel reasons, client names or the Stripe URL | test | Flagged as unverified-prohibition. My grep for `Log.`, `Timber` and `println` across the expert screen packages found none, so this is a non-authoritative pass. No automated negative test exists. Human review recommended (see human_verification). |
| MUST NOT expose deferred controls (Message button, start-a-reschedule, private note, View Profile, Payouts navigation) | test | Not found in the expert screens. Dashboard Payouts and Profile Settings show a "coming soon" snackbar rather than navigating. Only a KDoc comment mentions View Profile. |

### Anti-Patterns Found

| File | Pattern | Severity | Impact |
|------|---------|----------|--------|
| Phase 11 sources | `TBD` / `FIXME` / `XXX` | None found | The debt-marker gate is clear. |
| Phase 11 sources | Stubs, placeholders, hardcoded empty data | None found | Screens are substantive and data-backed. |

### Human Verification Required

See the `human_verification` frontmatter. In summary:

1. **Cross-client request, accept, decline and join (SC-2, SC-3).** Needs the iOS client and a device.
2. **Reschedule response, `SLOT_UNAVAILABLE` and cancel against live backend state (SC-4).**
3. **Dashboard, Stripe onboarding Custom Tab and calendar editor on a device (SC-1, EXP-07).**
4. **Logging prohibition.** Judgment review, no automated enforcement.

These match the user's `[Phase 11] DEFERRED (human)` list in STATE.md and are non-blocking by the user's rule.

### Gaps Summary

No gaps block the phase goal. All four roadmap success criteria and all five requirements are implemented, wired and test-covered. The unit tests are the only runtime evidence available here.

Advisory findings from `11-REVIEW.md`. All eight warnings are still `open` in `11-REVIEW-DISPOSITION.md`, and none undermines a must-have:

- **WR-08 (closest to a success criterion).** SC-2 says "accept with an optional message". Android collects and sends the message, but the backend validates and discards it. The expert is shown a field whose content is never delivered. This is a backend limitation recorded in STATE.md as a backend note, and the Android side is correct. Recommend hiding or caveating the field, or fixing the backend, before release.
- **WR-02.** A lost-race `ALREADY_CANCELLED` is shown as a successful cancel without verification. The cancel flow itself works.
- **WR-03.** The cards omit the "≈" qualifier for `expert_default` earnings estimates. The Request Decision screen handles it correctly.
- **WR-01, WR-04, WR-05, WR-06, WR-07.** Dashboard and decision robustness issues: a stale Stripe link can open unprompted, a failed quiet reload wipes the financial figures, the soft financial fetches gate first paint, the decision screen does not re-check expiry, and the reschedule banner count can disagree with the Bookings list.

Recommend triaging these warnings into a follow-up gap-closure or polish phase (Phase 15) rather than blocking Phase 12.

---

_Verified: 2026-10-26T20:00:00Z_
_Verifier: Claude (gsd-verifier)_

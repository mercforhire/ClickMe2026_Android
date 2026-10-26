---
phase: 11-expert-requests-bookings
verified: 2026-10-26T23:30:00Z
status: human_needed
score: 3/4 must-haves verified
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
  - .planning/phases/11-expert-requests-bookings/11-16-PLAN.md
  - .planning/phases/11-expert-requests-bookings/11-16-SUMMARY.md
  - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/AgoraEngineConfig.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/AgoraManager.kt
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
  - app/src/test/java/com/uptrendinvestments/clickmeapp/managers/AgoraEngineConfigTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/managers/AgoraManagerCreateEngineTest.kt
covered_digest: "v3:sha256:4a0230e3ae66015a3e79ae31ce5305d0b7d25701cdadd2481785a0a74ef07220"
behavior_unverified: 1
overrides_applied: 0
re_verification:
  previous_status: human_needed
  previous_score: 4/4
  gaps_closed:
    - "G-11-1: every in-app voice join failed with 'Couldn't join session' (NPE in AgoraManager.createEngine, bare context resolved to RtcEngineConfig.getContext())"
  gaps_remaining: []
  regressions: []
behavior_unverified_items:
  - truth: "SC-3: the expert can join the call (Dashboard card, Upcoming card, Summary) and both sides end up in the same Agora call"
    test: "On the Samsung SM-A075F (-s R8YYC1QFA6H, devDebug, production backend) with the iOS app as peer, tap Join from the Dashboard card, an Upcoming card and the Summary inside the join window; capture adb logcat -s ClickMeCall CallService RtcEngine."
    expected: "No 'engine create failed' or 'engine create returned null' line; joinChannel returns 0; onJoinChannelSuccess then the remote user joins; audio routes to the speaker; mute and speaker toggles work; leave, destroy and a second rejoin work."
    why_human: "The fix is proven only up to the SDK factory boundary on the JVM (the factory receives a non-null application context, app id and handler). The native engine, joinChannel, audio route and leave/destroy/rejoin have never run on a device. UAT test 1 failed at engine creation, so nothing past it has ever been exercised."
human_verification:
  - test: "G-11-1 device re-check (also 11-UAT test 1; 09-HUMAN-UAT items 1, 2 and 5): expert and client voice join end to end with an iOS peer, as described in behavior_unverified_items."
    expected: "Both sides in the same Agora call; leave/rejoin clean."
    why_human: "Native Agora runtime, microphone, foreground service and a live iOS peer. Recorded as non-blocking DEFERRED (human) in STATE.md ([Phase 11] G-11-1 entry and item (c))."
  - test: "Residual UAT-passed items stay passed (11-UAT tests 2, 3, 4: reschedule/SLOT_UNAVAILABLE/cancel, Dashboard/Stripe/calendar, prohibition review). No further action unless the user wants a regression pass."
    expected: "Unchanged."
    why_human: "Device-only; already confirmed by the user in UAT."
---

# Phase 11: Expert Requests & Bookings Verification Report

**Phase Goal:** An Android expert can see their workload, accept requests, manage bookings and join sessions, so the expert side works end to end.
**Verified:** 2026-10-26T23:30:00Z
**Status:** human_needed
**Re-verification:** Yes, after gap closure plan 11-16 (G-11-1)

## Re-verification: G-11-1 (voice join failed at engine creation)

UAT (`11-UAT.md` test 1) found that every in-app voice join, client and expert, failed with "Couldn't join session". The root cause was in `AgoraManager.createEngine()`: inside `RtcEngineConfig().apply { mContext = context.applicationContext }` the bare `context` resolved to the SDK's `getContext()` (null) instead of the injected application context, so an NPE was thrown before the engine existed.

**The gap is closed in code.** Evidence I checked directly, not from the SUMMARY:

- Pre-fix code (`git show 865e6cd:.../AgoraManager.kt`, line 131) has the exact `RtcEngineConfig().apply { mContext = context.applicationContext ...}` shape the debug session named.
- Current `AgoraManager.createEngine()` reads `val appContext = context.applicationContext` outside any config scope, then calls `buildRtcEngineConfig(appContext, BuildConfig.AGORA_APP_ID, handler)`. That helper (`AgoraEngineConfig.kt`) takes a parameter named `appContext`, builds `RtcEngineConfig()` and sets `mContext`, `mAppId` and `mEventHandler` by plain assignment, with no receiver scope. A repo-wide grep finds `RtcEngineConfig(` only in that helper, and `io.agora` only in the three `managers` files, so there is no second copy of the bug.
- Wiring: the single `@Inject` constructor passes `RtcEngine.create(config)`; all six Join sites go through `CallCenter` to `CallEngine.join` to this one path, so one fix covers client and expert.
- A null `RtcEngine.create` result now returns false with the single log line `engine create returned null`; both catch blocks (`Exception`, `LinkageError`) log `describeEngineFailure(e)` (simple class name plus top frame, no message or cause); a non-zero `joinChannel` code is logged as an integer.
- Regression test: `AgoraManagerCreateEngineTest` drives `join()` through `createEngine` to the factory seam and asserts the factory is called once with `mContext === app.applicationContext`, the app id and a non-null handler, that the only warning is `engine create returned null`, and that no sentinel token, channel, uid or message appears in any log line. On the pre-fix code the NPE fires while building the config, so the factory call count would be 0 and the test fails (the review confirmed this; 11-16-SUMMARY records the RED run as `expected:<1> but was:<0>`).
- I re-ran the two test classes: `AgoraManagerCreateEngineTest` 3 tests and `AgoraEngineConfigTest` 4 tests, 0 failures, 0 errors, 0 skipped. The caller reports the post-merge gate on main as passed (`testDevDebugUnitTest`, `ktlintCheck`, `assembleDevDebug`, 2954 tests); I did not re-run the full gate.

**What is still unproven.** The fix is exercised only up to the SDK factory boundary on the JVM. The native `RtcEngine.create`, the four audio setup calls, `joinChannel`, `onJoinChannelSuccess`, the remote join, the audio route, mute and speaker, and leave/destroy/rejoin have never run on a device. UAT test 1 failed at engine creation, so no run has ever gotten past it. A pre-existing gap of this kind is a runtime-behavior claim that presence and unit tests cannot confirm, so truth 3 is carried as present, wired and behavior-unverified, and the device re-check stays required for the voice join path.

## Goal Achievement

The code delivers every roadmap success criterion. Each screen is real (not a stub), wired into the expert shell, backed by repositories that call endpoints which exist in `backend-reference`, and covered by passing tests. The remaining items need a device or a live iOS client, which the user has deferred as non-blocking.

### Observable Truths

| # | Truth (ROADMAP success criterion) | Status | Evidence |
|---|-----------------------------------|--------|----------|
| 1 | Dashboard shows pending request count, upcoming sessions, weekly earnings text and a payout onboarding card | VERIFIED (code) | `ExpertDashboardRoute` is the Dashboard tab root in `HomeExpertScreen.kt`. `ExpertDashboardViewModel.startLoad` fetches upcoming bookings and pending requests in parallel, then weekly earnings, payout summary and Connect status. `applyLoaded` sets `pendingCount = pending.size`, `todaysSessions`, `rescheduleCount`, `weekly`, `payoutSummary` and `onboarding`. `ExpertDashboardScreen` renders `PendingRequestsBanner`, `RescheduleRequestsBanner`, `TodaysSessionsSection`, `FinancialSummarySection` (EarningsCard + `PayoutOnboardingCard`) and the tools grid. The card mints a Stripe link behind `CallGuard` and opens it in a Custom Tab. The Availability tool pushes `AvailabilityKey(AvailabilityOrigin.Dashboard)`. Live Stripe and visual checks are in human verification. |
| 2 | Android expert sees an iOS client's request in Incoming Requests, accepts with an optional message or declines with a reason code, sees Confirmed, adds the session to the device calendar | VERIFIED (code) | Dashboard banner and tools navigate to `IncomingRequestsKey`, then `RequestDecisionKey`, then `BookingConfirmedKey`. All are registered in `HomeExpertScreen.detailEntries`. `RequestDecisionViewModel.accept` calls `BookingRequestsRepository.accept` (`POST expert/booking-requests/{id}/accept`, body `{}` or `{message}`) and the screen does `replaceTop(BookingConfirmedKey)`. `decline` sends `reason_code` in `CONFLICT/OUT_OF_SCOPE/UNAVAILABLE/OTHER` and a non-empty `reason_text` (the label when the note is blank). Both match the backend's strict `declineSchema` and `acceptSchema`. `ALREADY_PROCESSED` has its own dialog and refresh. `CalendarIntent.kt` builds an `ACTION_INSERT` intent on `CalendarContract.Events.CONTENT_URI` and handles no-handler with a snackbar. No calendar permission is requested. |
| 3 | Expert Bookings (Upcoming/Past) shows Join inside the join window and the expert can join the call | PRESENT_BEHAVIOR_UNVERIFIED (Join UI, gating and call start VERIFIED in code; G-11-1 engine-creation defect fixed and JVM-tested; the real Agora join has never run on a device) | `ExpertBookingsViewModel` loads both buckets. `buildExpertBookingLists` applies the two-bucket and rejoinable-completed rules. `ExpertUpcomingCard` renders the shared `JoinCallButton`, enabled by `isWithinJoinWindow(...)` against the shared `JoinClock`. `join()` calls `CallStarter.startCall` with the client as peer. The same call exists on the Dashboard card and the Summary. Same pattern as the client side (button always shown, enabled only in the window). |
| 4 | Expert can accept or decline a client's reschedule request, sees a clear message on `409 SLOT_UNAVAILABLE`, and can cancel a booking | VERIFIED (code) | `ExpertBookingSummaryViewModel.acceptReschedule` / `declineReschedule` are guarded by `CallGuard.attemptModifyBooking(RespondToReschedule)` and run the shared `RescheduleResponseController` through `ExpertRescheduleResponder`, which calls `expert/bookings/{id}/accept-reschedule` and `/decline-reschedule`. `toExpertRescheduleFailure` maps `ErrorCode.SlotUnavailable` to `expert_reschedule_error_slot_unavailable` ("That time is no longer available...") with a reload. `ErrorCode.SlotUnavailable` exists. Cancel: Summary `attemptCancel` pushes `ExpertCancelKey`; `ExpertCancellationViewModel.confirm` is guarded, posts `expert/bookings/{id}/cancel` and hands the result to the Bookings Past tab. |

**Score:** 3/4 truths verified (1 present, behavior-unverified: truth 3, the live Agora join). Cross-client and device confirmation is carried in `human_verification`.

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
| G-11-1 regression: `AgoraManagerCreateEngineTest` + `AgoraEngineConfigTest` | `./gradlew --offline :app:testDevDebugUnitTest --tests "*AgoraManagerCreateEngineTest" --tests "*AgoraEngineConfigTest"` | exit 0; 3 + 4 tests, 0 failures, 0 errors, 0 skipped | PASS |
| Orchestrator gate | `testDevDebugUnitTest ktlintCheck assembleDevDebug`, plus the 11-15 full gate (both flavors, lint, R8) | Reported green by the caller (post-merge on main, 2954 tests); not re-run here | NOT RE-RUN |

### Probe Execution

No probes declared by the phase. Skipped.

### Requirements Coverage

Every ID in the PLAN frontmatter appears in REQUIREMENTS.md, and none is orphaned. All five are mapped to Phase 11 and marked Complete there.

| Requirement | Source plans | Description | Status | Evidence |
|-------------|--------------|-------------|--------|----------|
| EXP-05 | 11-01, 02, 03, 10, 14, 15 | Dashboard: pending count, upcoming sessions, weekly earnings, payout card | SATISFIED | Truth 1 |
| EXP-06 | 11-01, 02, 03, 05, 06, 07, 12, 15 | Incoming Requests, accept with message or decline with reason, Confirmed | SATISFIED | Truth 2. The Android side sends the message; the backend discards it (see Gaps Summary, WR-08). |
| EXP-07 | 11-01, 02, 07, 15 | Add to calendar via `Intent.ACTION_INSERT` | SATISFIED | `CalendarIntent.kt`; `CalendarIntentTest` |
| EXP-08 | 11-01, 02, 03, 04, 08, 10, 11, 15, 16 | Upcoming and Past bookings, Join inside the window | SATISFIED in code; live join pending device re-check | Truth 3. 11-16 (gap closure, `requirements: [EXP-08]`) fixed the engine-creation defect; the device run is a deferred human item. |
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

See the `human_verification` and `behavior_unverified_items` frontmatter. In summary:

1. **G-11-1 voice join device re-check (SC-3, EXP-08).** Samsung SM-A075F `-s R8YYC1QFA6H`, devDebug, production backend, iOS app as peer; Join from the Dashboard card, an Upcoming card and the Summary; capture `adb -s R8YYC1QFA6H logcat -s ClickMeCall CallService RtcEngine`. Pass means no `engine create failed` / `engine create returned null` line, `joinChannel` returns 0, `onJoinChannelSuccess` and the remote join arrive, audio is on the speaker, mute and speaker work, and leave/destroy/rejoin are clean. A `joinChannel failed code=N` line carries the Agora error code. This also clears 11-UAT test 1 and 09-HUMAN-UAT items 1, 2 and 5. Non-blocking by the user's rule (STATE.md, `[Phase 11] G-11-1` entry).
2. The other UAT items passed on device (tests 2, 3, 4) and need no further action.

### Gaps Summary

No gaps block the phase goal in code. All four roadmap success criteria and all five requirements are implemented, wired and test-covered, and the one UAT blocker (G-11-1) is closed in code with a JVM regression test. The verdict stays `human_needed` rather than `passed` because the voice join has never run past engine creation on a device, so SC-3 "the expert can join the call" is unproven at runtime.

New advisory from `11-16-REVIEW.md` (1 warning, 2 info, 0 critical), not blocking:

- **11-16 WR-01.** If `RtcEngine.create` succeeds but one of the four audio setup calls throws, `engine` is never assigned, so `destroy()` (guarded by `engine != null`) never releases the live native engine and a retry may collide with it. Same shape as before the fix, not a regression; worth a follow-up since it is the same failure class G-11-1 exposed.
- **11-16 IN-01.** `ChannelMediaOptions().apply {}` in `join` has the same SDK-getter shadowing hazard for `token` if a future change reads it inside the block. No live bug today.

Advisory findings from `11-REVIEW.md`. All eight warnings are still `open` in `11-REVIEW-DISPOSITION.md`, and none undermines a must-have:

- **WR-08 (closest to a success criterion).** SC-2 says "accept with an optional message". Android collects and sends the message, but the backend validates and discards it. The expert is shown a field whose content is never delivered. This is a backend limitation recorded in STATE.md as a backend note, and the Android side is correct. Recommend hiding or caveating the field, or fixing the backend, before release.
- **WR-02.** A lost-race `ALREADY_CANCELLED` is shown as a successful cancel without verification. The cancel flow itself works.
- **WR-03.** The cards omit the "≈" qualifier for `expert_default` earnings estimates. The Request Decision screen handles it correctly.
- **WR-01, WR-04, WR-05, WR-06, WR-07.** Dashboard and decision robustness issues: a stale Stripe link can open unprompted, a failed quiet reload wipes the financial figures, the soft financial fetches gate first paint, the decision screen does not re-check expiry, and the reschedule banner count can disagree with the Bookings list.

Recommend triaging these warnings into a follow-up gap-closure or polish phase (Phase 15) rather than blocking Phase 12.

---

_Verified: 2026-10-26T20:00:00Z_
_Verifier: Claude (gsd-verifier)_

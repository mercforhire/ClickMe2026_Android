---
phase: 11-expert-requests-bookings
plan: 10
subsystem: ui
tags: [dashboard, viewmodel, compose, join, realtime, wave-2]

requires:
  - phase: 11-expert-requests-bookings
    provides: "11-01 repositories and fixtures, 11-02 route keys and ExpertLaunchRequest, 11-03 ExpertRescheduleBadge and clientDisplayName"
provides:
  - Dashboard tab root rendering the Hilt-backed ExpertDashboardScreen (hero, pending and reschedule banners, Today's Sessions with Join)
  - ExpertDashboardViewModel hard-fetch state machine with quiet, pull and loud loads and four refresh triggers
  - DashboardLogic (dashboardFirstName, expertTodaysSessions, clientRescheduleCount, hasClientProposedReschedule)
  - DashboardSkeleton whose lower blocks 11-14 reuses
affects: [11-14]

actuals:
  tokens: 21400
  tasks: 3
  commits: 3
plan_head_before: 2d62a572d451277a2d07d2d2715b4a38af23cb66
plan_head_after: 8fbf733
commits: 3

tech-stack:
  added: []
  patterns:
    - "Tab-root ViewModel with LoadMode Loud/Quiet/Pull and a cancel-on-restart loadJob (latest wins), copied from MyBookingsViewModel"
    - "Banner container composable (DashboardBanner) shared by the pending and reschedule banners; AnimatedVisibility keeps the last positive count while animating out"
    - "Hand-off consumed before the first load and collected for later posts, so a pre-construction launch never causes a second initial load"

key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/DashboardLogic.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/ExpertDashboardUiState.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/ExpertDashboardViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/ExpertDashboardScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/subviews/DashboardHeader.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/subviews/TodaysSessionsSection.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/subviews/DashboardSkeleton.kt
    - app/src/main/res/values/strings_expert_dashboard.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/DashboardLogicTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/ExpertDashboardScreenTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/ExpertDashboardViewModelTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/ExpertDashboardRoute.kt

key-decisions:
  - "The pending count is the requests list size (C2); no pagination total is read"
  - "The reschedule banner and the per-card reschedule badge use one predicate (hasClientProposedReschedule), so only client proposals the expert can act on count"
  - "View Calendar and the reschedule banner post BookingsLaunch(Upcoming) to ExpertLaunchRequest.bookings before selecting the tab; the screen does the post through the ViewModel, the route only navigates"
  - "Failed state sits inside a PullToRefreshBox too, so a pull can recover it"

patterns-established:
  - "Screen tests build the real ViewModel over SignupTestHarness plus FakeClickMeApi handlers with an UnconfinedTestDispatcher main rule, and scroll to a bottom-of-column control before clicking it"

requirements-completed: [EXP-05, EXP-08]

coverage:
  - id: D1
    description: "Dashboard greets the expert by first name (auth, then profile, first word, 'there' when blank) and shows the pending request count from the list size; the banner opens Incoming Requests and stays tappable at zero"
    requirement: "EXP-05"
    verification:
      - kind: automated_ui
        ref: "ExpertDashboardScreenTest#greetingAndPendingCountFromTheApi, onePendingIsSingular, zeroPendingBannerStaysTappable"
        status: pass
      - kind: unit
        ref: "DashboardLogicTest#firstNameIsTheTrimmedFirstWord, firstNameFallsBackToTheProfileName, blankNamesGiveNull"
        status: pass
    human_judgment: false
  - id: D2
    description: "Today's Sessions lists joinable bookings starting today in the device zone, sorted by start, with Starting now badge, Join as its own target (client as peer) and the card opening the Summary; empty card at zero"
    requirement: "EXP-08"
    verification:
      - kind: unit
        ref: "DashboardLogicTest (11 tests: midnight edges, status filter, ordering, kept-after-end)"
        status: pass
      - kind: automated_ui
        ref: "ExpertDashboardScreenTest#todaysSessionCardWithJoin, noSessionsShowsEmptyCard"
        status: pass
      - kind: unit
        ref: "ExpertDashboardViewModelTest#joinStartsTheCallWithTheClientAsPeer"
        status: pass
    human_judgment: false
  - id: D3
    description: "Reschedule banner counts only client-proposed pending_reschedule bookings; it and View Calendar select the Bookings tab on Upcoming"
    requirement: "EXP-08"
    verification:
      - kind: automated_ui
        ref: "ExpertDashboardScreenTest#rescheduleBannerOnlyWhenClientProposed, rescheduleBannerHiddenWhenTheExpertProposedIt"
        status: pass
      - kind: unit
        ref: "ExpertDashboardViewModelTest#openBookingsUpcomingPostsTheUpcomingHandOff"
        status: pass
    human_judgment: false
  - id: D4
    description: "Either hard fetch failing shows the error state with Retry; the Dashboard reloads quietly on screen shown, realtime booking.update, Back to Home and pull, with latest-wins cancellation"
    requirement: "EXP-05"
    verification:
      - kind: unit
        ref: "ExpertDashboardViewModelTest (14 tests: bookingsFailureIsHard, requestsFailureIsHard, realtime burst, backToHomeLaunchReloads, pullDuringQuietReloadKeepsOneLoadInFlight)"
        status: pass
      - kind: automated_ui
        ref: "ExpertDashboardScreenTest#hardFailureShowsTheErrorStateAndRetryRecovers"
        status: pass
    human_judgment: false
  - id: D5
    description: "Visual fit of the banners, the Starting now badge wrapping and the skeleton on a device at large font scales"
    verification: []
    human_judgment: true
    rationale: "Layout, wrapping and animation timing are not asserted by Robolectric tests; verified at the phase UAT on a device"

duration: 75min
completed: 2026-10-26
status: complete
---

# Phase 11 Plan 10: Expert Dashboard Workload Summary

**Expert Dashboard tab root with first-name hero, pending-request and client-reschedule banners, Today's Sessions with the shared Join button, a skeleton and hard-failure state, and latest-wins refresh on resume, realtime booking updates, Back to Home and pull-to-refresh.**

## Performance

- **Duration:** about 75 min, of which a cold first Gradle build took 12 min under seven parallel executors
- **Completed:** 2026-10-26
- **Tasks:** 3 (one tracer, two expansion)
- **Files modified:** 12 (11 created, 1 modified)

## Accomplishments

- Tracer: the greeting and "{n} Pending Requests" / "1 Pending Request" / "No pending requests" proven end to end through the real screen, ViewModel, repositories and FakeClickMeApi; the banner opens Incoming Requests. Tracer verify was green before expanding (⚡ Tracer verified end-to-end).
- Today's Sessions: `expertTodaysSessions` mirrors the client filter on the expert DTO; the card has the Starting now badge, the client-proposed reschedule badge and `JoinCallButton` as its own click target with the client as call peer (D-14 site 2). "View Calendar" and the "N Reschedule Requests" banner (client proposals only, D-12) post the Upcoming hand-off and select the Bookings tab.
- State machine: hard load of both fetches, `Loud`/`Quiet`/`Pull` modes, cancel-on-restart `loadJob`, a realtime collector that never reads the payload, and the `ExpertLaunchRequest.dashboard` hand-off consumed before the first load and collected for later posts.

## Task Commits

1. **Task 1: Greeting and pending count (tracer)** - `adf1c3b` (feat)
2. **Task 2: Today's Sessions, reschedule banner, View Calendar** - `702d3b1` (feat)
3. **Task 3: Skeleton, hard failure, refresh triggers** - `8fbf733` (feat)

**Plan metadata:** docs commit following this file.

## Verification

- Task 1: `*ExpertDashboardScreenTest` + ktlintCheck, 3/3.
- Task 2: `*DashboardLogicTest` (11) + `*ExpertDashboardScreenTest` (7) + ktlintCheck.
- Task 3 / plan level: `*ExpertDashboardViewModelTest` (14), `*ExpertDashboardScreenTest` (8), `*DashboardLogicTest` (11), `*HomeShellsTest` (44), ktlintCheck all exit 0; `bash scripts/check-design-rules.sh` prints "Design rules: OK".
- Acceptance greps: `totalCount` in the ViewModel = 0; `RESCHEDULE_INITIATED_BY_CLIENT` in DashboardLogic = 2 (at least 1); `dashboard_session_join` in TodaysSessionsSection = 1; `@Test` in DashboardLogicTest = 11 (at least 9); `launchRequest.dashboard` in the ViewModel = 3 (at least 2); `filterIsInstance<RealtimeEvent.BookingUpdate>` = 1; `Log.x(` = 0.

## Decisions Made

- The reschedule predicate lives once in `DashboardLogic.hasClientProposedReschedule` and feeds both the banner count and the card badge.
- The hero first name reads the auth user first, then the profile, as specified; `userManager.profile.value?.let { it.personalDetails.firstName }` because `personalDetails` is non-null.
- `DashboardSkeleton` already includes the Financial Summary and Expert Tools blocks so plan 11-14 does not change the first-load layout.

## Deviations from Plan

None - plan executed as written, with these process notes (not deviations):

- Tasks are `tdd="true"` but introduce new symbols, so a RED run would only be a compile failure (INVALID_RED). Tests and implementation were committed together per task, each run green first, as in 11-01 to 11-03.
- Task 1 acceptance `grep -c 'IncomingRequestsKey' ExpertDashboardRoute.kt` prints 2 (the import line and the use), not 1. The intent (the route navigates to it) is met; the literal count was not forced with a fully qualified name.
- The plan lists a `dashboard_loading` string; the existing `content_loading` ("Loading") already backs `SkeletonGroup`, so no duplicate string was added.
- Screen tests scroll to the Join button before clicking it, since it sits below the Robolectric viewport.

## Issues Encountered

- First Gradle run took 12 min (cold, seven parallel builds); later runs 1 to 2 min.
- Two ktlint chain-wrapping failures in tests were fixed before the commits.

## Known Stubs

None. The Dashboard has no Financial Summary, payout onboarding card or Expert Tools grid yet; those are plan 11-14's scope by design and are not placeholders in this plan's UI.

## Threat Flags

None. T-11-31 (bursts and overlapping pulls) is mitigated by the cancel-on-restart `loadJob` and covered by `aBurstOfRealtimeUpdatesEndsWithOneCompletedLoad` and `pullDuringQuietReloadKeepsOneLoadInFlight`. T-11-32: the realtime payload is never read and the ViewModel has no logging (grep gate = 0). T-11-33 accepted: Join is a UX gate, `POST /join` is authoritative.

## Next Phase Readiness

Plan 11-14 can add the Financial Summary, onboarding card and Expert Tools grid inside the `Loaded` column of `ExpertDashboardContent` and extend `ExpertDashboardViewModel` and the three test classes.

## Self-Check: PASSED

- All 11 created files and the modified route exist on disk; `git status` is clean after the task commits.
- Commits `adf1c3b`, `702d3b1`, `8fbf733` are ancestors of HEAD; `git rev-list --count 2d62a57..HEAD` = 3 before this summary commit.

---
*Phase: 11-expert-requests-bookings*
*Completed: 2026-10-26*

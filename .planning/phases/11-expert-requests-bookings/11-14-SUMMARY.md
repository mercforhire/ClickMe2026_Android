---
phase: 11-expert-requests-bookings
plan: 14
subsystem: ui
tags: [dashboard, earnings, stripe-connect, custom-tab, tools-grid, wave-3]

requires:
  - phase: 11-expert-requests-bookings
    provides: "11-01 ExpertFinancialRepository and Connect fakes, 11-02 AvailabilityKey and TopicsRoot, 11-03 RestrictedAction.SetUpPayouts, 11-10 Dashboard ViewModel, screen, skeleton and tests"
provides:
  - "Financial Summary on the expert Dashboard: weekly net earnings card, available payout and next payout line"
  - "Payout onboarding card that mints a single-use Stripe Connect link behind CallGuard and opens it in a Custom Tab"
  - "Six-cell Expert Tools grid with the Dashboard navigation targets"
  - "PayoutOnboarding, resolvePayoutOnboarding and isAllowedOnboardingUrl (DashboardFinancials.kt)"
affects: [11-15, 14-payouts]

actuals:
  tokens: 15000
  tasks: 3
  commits: 3
plan_head_before: 12a38f1c33f95e9eb5883046a694c426121ff444
plan_head_after: 8169c29
commits: 3

tech-stack:
  added: []
  patterns:
    - "Soft financial fetches run in parallel after the hard workload fetches of every load; each failure becomes null and never changes the load state"
    - "One-shot secret URL in UI state: read by a LaunchedEffect keyed on it, cleared immediately, then opened"
    - "Tools grid maps an ExpertTool enum to targets in the Content composable, so placeholder cells only swap a handler later"

key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/DashboardFinancials.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/subviews/FinancialSummarySection.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/subviews/ExpertToolsGrid.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/DashboardFinancialsTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/ExpertDashboardUiState.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/ExpertDashboardViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/ExpertDashboardScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/ExpertDashboardRoute.kt
    - app/src/main/res/values/strings_expert_dashboard.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/ExpertDashboardViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/ExpertDashboardScreenTest.kt

key-decisions:
  - "Connect status is authoritative; a 400 payout summary only means onboarding is required when the status fetch itself failed"
  - "Soft fetches run after the hard fetches succeed, in the same cancellable load job, so a hard failure never starts them and latest-wins cancellation still covers them"
  - "A tapped onboarding link is ignored while one is minting or while an unconsumed URL is held, and the CallGuard check runs after those re-entry checks so repeat taps never queue extra call dialogs"
  - "The Unknown state shows the Payout schedule unavailable line as well as the dash, matching the UI-SPEC row; the Required state shows neither the next-payout row nor an available amount"

patterns-established:
  - "Tests compute expected currency text with Money.format instead of hard-coding symbols, since the shared formatter renders CAD as CA$ in en-US"

requirements-completed: [EXP-05]

coverage:
  - id: D1
    description: "Financial Summary shows the weekly net amount (minor units via Money.format) with a sessions plural, a formatted zero for an empty week and a dash only when the fetch failed; the gross amount is never shown"
    requirement: "EXP-05"
    verification:
      - kind: automated_ui
        ref: "ExpertDashboardScreenTest#weeklyEarningsShowsNetAndSessions, oneSessionIsSingular, jpyAmountHasNoDecimals, zeroWeekShowsZeroNotDash, weeklyFailureShowsDash"
        status: pass
    human_judgment: false
  - id: D2
    description: "Available payout and next payout line, with manual-schedule, malformed-date and summary-unavailable fallbacks"
    requirement: "EXP-05"
    verification:
      - kind: automated_ui
        ref: "ExpertDashboardScreenTest#onboardedShowsAvailablePayoutAndNextDate, noNextPayoutDateReadsManualSchedule, malformedNextPayoutDateReadsManualSchedule, onboardedWithoutSummaryShowsDashAndUnavailable, unknownShowsDashAndNoCard"
        status: pass
    human_judgment: false
  - id: D3
    description: "Payout onboarding resolution from Connect status and payout summary, with refresh-link versus first-onboarding minting"
    requirement: "EXP-05"
    verification:
      - kind: unit
        ref: "DashboardFinancialsTest (8 resolvePayoutOnboarding cases)"
        status: pass
      - kind: unit
        ref: "ExpertDashboardViewModelTest#incompleteAccountMintsRefreshLink, noAccountMintsOnboard, statusRefetchedOnResume, financialFailuresNeverFailTheLoad"
        status: pass
    human_judgment: false
  - id: D4
    description: "Guarded, single-flight, https-only link minting; server message shown on failure; suspended account shows no dialog; the URL is one-shot and never persisted or logged"
    requirement: "EXP-05"
    verification:
      - kind: unit
        ref: "ExpertDashboardViewModelTest#guardBlocksMint, repeatTapsWhileMintingAreIgnored, nonHttpsUrlIsRejected, mintFailureShowsServerMessage, suspendedAccountShowsNoDialog, nothingToMintWhenOnboarded"
        status: pass
      - kind: unit
        ref: "DashboardFinancialsTest#httpsWithAHostIsAllowed, plainHttpIsRejected, scriptAndIntentSchemesAreRejected, anHttpsUrlWithoutAHostIsRejected, malformedStringsAreRejected"
        status: pass
      - kind: automated_ui
        ref: "ExpertDashboardScreenTest#incompleteAccountShowsTheOnboardingCard, aFailedMintShowsTheServerMessageInADialog"
        status: pass
    human_judgment: false
  - id: D5
    description: "Expert Tools grid has six Role.Button cells in order; four navigate (Requests, Bookings Upcoming, Availability from the Dashboard, Topics) and two show a coming-soon snackbar"
    requirement: "EXP-05"
    verification:
      - kind: automated_ui
        ref: "ExpertDashboardScreenTest#toolsGridTargets, placeholderToolsShowComingSoon, payoutsToolShowsComingSoon, toolCellsAreButtonsWithLabels"
        status: pass
    human_judgment: false
  - id: D6
    description: "The Stripe hosted onboarding actually opening in a Custom Tab on a device, and JPY or long-locale amounts wrapping in the earnings card at large font scales"
    verification: []
    human_judgment: true
    rationale: "Robolectric cannot render a real Custom Tab or Stripe's hosted page, and wrapping at large font scales is a layout judgment (UI-SPEC backstop); verified at phase UAT on a device"

duration: 70min
completed: 2026-10-26
status: complete
---

# Phase 11 Plan 14: Dashboard Financial Summary, Payout Onboarding and Expert Tools Summary

**Expert Dashboard now shows weekly net earnings and payout figures in minor units, a Complete payout setup card that mints a single-use Stripe Connect link behind the call guard and opens it https-only in a Custom Tab, and the six-cell Expert Tools grid.**

## Performance

- **Duration:** about 70 min
- **Completed:** 2026-10-26
- **Tasks:** 3 (one tracer, two expansion)
- **Files modified:** 11 (4 created, 7 modified)

## Accomplishments

- Tracer (Task 1): Connect status drives `PayoutOnboarding` (Required with refresh link, Required with first onboarding, Onboarded, Unknown). `startPayoutSetup()` checks re-entry, runs `CallGuard.attempt(SetUpPayouts)`, mints the right link, accepts only an https URL with a host, and exposes it as a one-shot `onboardingUrl` that the screen consumes and opens in a Custom Tab. Tracer verify was green end to end before expanding.
- Financial Summary (Task 2): `EarningsCard` shows "This week", the net amount, "{n} sessions" with a merged accessibility description, "Available payout" and the next payout line. A fetch failure reads a dash, a zero week reads a formatted zero.
- Expert Tools (Task 3): `ExpertTool` enum and `ExpertToolsGrid` with equal-height cells; Dashboard route passes `onOpenAvailability` (Dashboard origin) and `onOpenTopics`.

## Task Commits

1. **Task 1: Payout onboarding with guarded link minting (tracer)** - `7a6e993` (feat)
2. **Task 2: Weekly earnings and payout figures** - `1789ef4` (feat)
3. **Task 3: Expert Tools grid** - `8169c29` (feat)

**Plan metadata:** docs commit following this file.

## Files Created/Modified

- `dashboard/DashboardFinancials.kt` - `PayoutOnboarding`, `resolvePayoutOnboarding`, `isAllowedOnboardingUrl`
- `dashboard/ExpertDashboardUiState.kt`, `ExpertDashboardViewModel.kt` - financial state, soft fetches, `startPayoutSetup`, `consumeOnboardingUrl`, `dismissOnboardingError`; constructor gains `ExpertFinancialRepository` and `CallGuard` before `JoinClock`
- `dashboard/ExpertDashboardScreen.kt`, `ExpertDashboardRoute.kt` - Custom Tab launcher, error dialog, tool click mapping, coming-soon snackbar, new route callbacks
- `dashboard/subviews/FinancialSummarySection.kt`, `ExpertToolsGrid.kt` - the new UI blocks
- `res/values/strings_expert_dashboard.xml` - financial, onboarding and tools strings
- Tests: `DashboardFinancialsTest` (13), `ExpertDashboardViewModelTest` (24), `ExpertDashboardScreenTest` (24)

## Decisions Made

See `key-decisions` in the frontmatter. In short: Connect status is authoritative, soft fetches follow the hard ones in the same cancellable job, re-entry checks run before the call guard, and the Unknown state keeps the "Payout schedule unavailable" line.

## Deviations from Plan

None that change behavior. Process notes:

- Tasks are `tdd="true"` but add new symbols and constructor parameters, so a RED run would only be a compile failure (INVALID_RED). Tests and implementation were committed together per task, each run green first, as in 11-01 to 11-10.
- Plan test expectations of "$127.50" and "$50.00" do not match the shared formatter, which renders CAD as "CA$" in en-US (see `MoneyTest`). The screen tests build the expected text with `Money.format(...)` so they assert the real formatter output, not a hard-coded symbol.
- Task 1 acceptance `grep -c 'rememberCustomTabLauncher' ExpertDashboardScreen.kt` prints 2 (the import and the call), not 1. The intent (the screen uses the launcher) is met, same as the 11-10 `IncomingRequestsKey` note.
- The plan's `placeholderToolsShowComingSoon` is split into two tests (Profile Settings, Payouts): a second `showSnackbar` queues behind the first, so asserting both in one test never sees the second.
- Existing 11-10 tests were updated for the two new ViewModel constructor parameters and the two new screen callbacks.

## Issues Encountered

- Two ktlint failures (line length, multiline expression) in the first Task 1 run were fixed before the commit; `ktlintFormat` fixed import order in the screen test before the Task 3 commit.
- The tool sandbox rejected some large heredoc commands, so the larger edits used the Write and Edit tools instead.

## Known Stubs

- `ExpertTool.ProfileSettings` and `ExpertTool.Payouts` show a "{tool} is coming soon" snackbar instead of navigating. Intentional per D-11 and UI-SPEC A2; Phase 14 replaces the handlers (`dashboard/ExpertDashboardScreen.kt`, `onToolClick`). These do not block EXP-05.

## Threat Flags

None. Mitigations in the plan's register are in place: T-11-44 (the URL lives only in UI state until consumed; grep gate for `Log.x(`, `savedStateHandle`, `DataStore`, `SharedPreferences` in the ViewModel prints 0), T-11-45 (`isAllowedOnboardingUrl` https plus host, tested), T-11-46 (`isMintingLink` and held-URL re-entry guard, `repeatTapsWhileMintingAreIgnored`), T-11-47 (`CallGuard.attempt(SetUpPayouts)` before minting, `guardBlocksMint`), T-11-48 (status authoritative, truth-table tests).

## Verification

- Task 3 verify chain exits 0: `*ExpertDashboardScreenTest` (24), `*ExpertDashboardViewModelTest` (24), `*DashboardFinancialsTest` (13), `*DashboardLogicTest` (11), `*HomeShellsTest` (44), `ktlintCheck`, `:app:assembleDevDebug`.
- `bash scripts/check-design-rules.sh` prints "Design rules: OK".
- Acceptance greps: `RestrictedAction.SetUpPayouts` in the ViewModel = 1; forbidden logging or persistence tokens in the ViewModel = 0; `Money.format(` in FinancialSummarySection = 2; `grossAmount` or `/ 100` there = 0; `@Test` in DashboardFinancialsTest = 13; `AvailabilityOrigin.Dashboard` in the route = 1; tool enum entries = 6.

## Next Phase Readiness

The Dashboard is complete for EXP-05. Phase 14 owns the Payouts screen, Profile Settings and the `clickme://connect/*` return links; the coming-soon handlers in `onToolClick` and the Custom Tab launch are the seams to extend.

## Self-Check: PASSED

- Created files exist on disk: DashboardFinancials.kt, FinancialSummarySection.kt, ExpertToolsGrid.kt, DashboardFinancialsTest.kt.
- Commits `7a6e993`, `1789ef4`, `8169c29` are ancestors of HEAD; `git rev-list --count 12a38f1..HEAD` = 3 before this summary commit.

---
*Phase: 11-expert-requests-bookings*
*Completed: 2026-10-26*

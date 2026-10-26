---
phase: 11-expert-requests-bookings
plan: 04
subsystem: ui
tags: [expert-bookings, viewmodel, compose, join, realtime, wave-2]

requires:
  - phase: 11-expert-requests-bookings
    provides: "11-01 ExpertBookingsRepository, ExpertBookingItem DTOs, FakeClickMeApi expert handlers and fixtures"
  - phase: 11-expert-requests-bookings
    provides: "11-02 ExpertBookingSummaryKey/ExpertBookingOrigin, AvailabilityOrigin.Bookings, ExpertLaunchRequest.bookings, ExpertBookingsRoute"
  - phase: 11-expert-requests-bookings
    provides: "11-03 ExpertRescheduleBadge, clientDisplayName, strings_expert_shared.xml"
  - phase: 08-client-booking-management
    provides: "MyBookingsViewModel state machine, JOINABLE_BOOKING_STATUSES, pastStatusDisplay, BookingsTab/BookingsLaunch/BookingsNotice"
  - phase: 09-call-experience
    provides: "CallStarter, JoinClock, isWithinJoinWindow, isStartingNow, JoinCallButton"
provides:
  - "Expert Bookings tab Upcoming list with Join (EXP-08, D-14, D-15) wired into the tab root"
  - "buildExpertBookingLists / ExpertBookingLists / ExpertPastRow / isRejoinableCompleted bucket rules"
  - "ExpertBookingsViewModel with two-bucket load, three refresh triggers, saved tab, post-cancel hand-off consume"
  - "ExpertBookingsContent stateless screen (tabs, pull-to-refresh, skeleton, failure state) and ExpertUpcomingCard"
  - "ExpertBookingsSkeleton with an Upcoming and a Past shape for 11-11"
affects: [11-11, 11-09, 11-13]

actuals:
  tokens: 19000
  tasks: 3
  commits: 5
plan_head_before: 2d62a572d451277a2d07d2d2715b4a38af23cb66
plan_head_after: 8e23e3a417586cb58207e29ad4cab3300f370c93
commits: 5

tech-stack:
  added: []
  patterns:
    - "Expert twin of the Phase 8 My Bookings state machine: Loud / Quiet / Pull load modes, loadJob + loadIsCurrent coalescing, latest-wins on realtime bursts"
    - "Stateless Content composable with callbacks for snackbar consumption so a later plan adds snackbars without a signature change"

key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/ExpertBookingBuckets.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/ExpertBookingsUiState.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/ExpertBookingsViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/ExpertBookingsScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/subviews/ExpertUpcomingCard.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/subviews/ExpertBookingsSkeleton.kt
    - app/src/main/res/values/strings_expert_bookings.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/ExpertBookingsScreenTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/ExpertBookingBucketsTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/ExpertBookingsViewModelTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/ExpertBookingsRoute.kt

key-decisions:
  - "Earnings on the card come only from earnings_breakdown.net_amount; the currency falls back to the fee currency and the whole column is omitted when neither exists (never gross, never a guessed currency)"
  - "Upcoming excludes pending_approval and rows with endTime <= now; completed rows with endTime > now move from the Past bucket into Upcoming (rejoin rule)"
  - "ExpertBookingsContent keeps onNoticeConsumed and onRefreshFailureConsumed in its signature but does not render snackbars; 11-11 owns both snackbars"
  - "Skeleton description reuses content_loading instead of a new expert_bookings_loading string"

patterns-established:
  - "Expert list ViewModels reuse BookingsLaunch / BookingsNotice / BookingsTab from Phase 8 rather than defining expert copies"

requirements-completed: [EXP-08]

coverage:
  - id: D1
    description: "Upcoming list from GET expert/bookings (upcoming + past, page 1, limit 50) rendered as cards with client, topic, net earnings, date and time"
    requirement: EXP-08
    verification:
      - kind: automated_ui
        ref: "ExpertBookingsScreenTest#upcomingCardsRenderFromTheApi"
        status: pass
      - kind: unit
        ref: "ExpertBookingsViewModelTest#loadsBothBucketsTogether"
        status: pass
    human_judgment: false
  - id: D2
    description: "Join on an Upcoming card starts the shared call with the client as peer; card body opens the summary with origin Bookings"
    requirement: EXP-08
    verification:
      - kind: automated_ui
        ref: "ExpertBookingsScreenTest#joinStartsTheCallWithTheClientAsPeer, cardTapOpensSummary"
        status: pass
      - kind: unit
        ref: "ExpertBookingsViewModelTest#joinRecordsTheStartRequestWithTheClientAsPeer"
        status: pass
    human_judgment: false
  - id: D3
    description: "Bucket rules: pending_approval and ended rows excluded, rejoinable completed moved to Upcoming, sorting, Past labels, unknown status dropped, de-dupe"
    requirement: EXP-08
    verification:
      - kind: unit
        ref: "ExpertBookingBucketsTest (9 tests)"
        status: pass
    human_judgment: false
  - id: D4
    description: "First load shows the skeleton; a hard failure shows a server or connection message with a working Retry"
    requirement: EXP-08
    verification:
      - kind: automated_ui
        ref: "ExpertBookingsScreenTest#firstLoadShowsSkeleton, hardFailureShowsTheServerMessageWithRetry, aNetworkFailureShowsTheConnectionMessage"
        status: pass
    human_judgment: false
  - id: D5
    description: "Quiet reload on realtime booking.update (burst latest-wins) and on screen shown, pull failure flagged, quiet failure silent, post-cancel launch applied once, saved tab restored"
    requirement: EXP-08
    verification:
      - kind: unit
        ref: "ExpertBookingsViewModelTest (20 tests)"
        status: pass
    human_judgment: false
  - id: D6
    description: "Join button flips at the join-window edges without a tap (JoinClock tick) and the card reads correctly at large font scales"
    requirement: EXP-08
    verification:
      - kind: unit
        ref: "JoinWindowTest (edges, unchanged, 11 tests)"
        status: pass
    human_judgment: true
    rationale: "The 30 s tick flipping a visible button and FlowRow wrapping at large fonts are not asserted by a test; verify on device at phase UAT (UI-SPEC UI Considerations: Join window clock)"

duration: 50min
completed: 2026-10-26
status: complete
---

# Phase 11 Plan 04: Expert Bookings Upcoming List with Join Summary

**Expert Bookings tab now lists Upcoming sessions (confirmed, pending_reschedule, in_progress, plus rejoinable completed) from both server buckets, with net earnings, reschedule badge and a Join button that starts the shared call with the client as peer, quiet-reloading on realtime updates, return to the tab and pull.**

## Performance

- **Duration:** about 50 min (most of it Gradle runs)
- **Completed:** 2026-10-26
- **Tasks:** 3 (one tracer, two expansion), all `tdd="true"`
- **Files modified:** 11 (10 created, 1 modified)

## Accomplishments

- Tracer: the Bookings tab root renders `ExpertBookingsScreen` over a real ViewModel and `ExpertBookingsRepository`; Upcoming cards show client, topic, net earnings, date and time range, the Starting now badge and the reschedule badge. Join is its own target and calls `CallStarter.startCall` with `peerId = item.client.id`. The tracer `<verify>` was re-run green before expanding.
- `buildExpertBookingLists` ports iOS `isUpcoming` / `isRejoinableCompleted` / `mapPastIfApplicable` with the strictly-after end check, pending_approval exclusion and de-dupe; pinned by 9 tests.
- First load shows `ExpertBookingsSkeleton` (Upcoming and Past shapes); a hard failure shows `EmptyState` with the server message or "Check your connection and try again." and a Retry that refetches.
- ViewModel refresh triggers: quiet reload on `RealtimeEvent.BookingUpdate` (payload never read, bursts latest-wins), on `onScreenShown`, pull with `refreshFailed`, saved tab under `KEY_SELECTED_TAB`, and `ExpertLaunchRequest.bookings` consumed in `init` and while alive and applied exactly once.

## Task Commits

1. **Task 1: Upcoming sessions with Join, API to tab root (tracer)** - `7ebc1d5` (test, RED), `b5093ff` (feat)
2. **Task 2: Bucket rules, skeleton and failure state** - `fa4a111` (feat; rules were already implemented in Task 1 per the plan, tests pinned them)
3. **Task 3: Refresh triggers, saved tab, post-cancel hand-off** - `aeb2795` (test, RED), `8e23e3a` (feat)

**Plan metadata:** docs commit following this file.

## Files Created/Modified

- `E/upcomingbookings/ExpertBookingBuckets.kt` - bucket rules and list types
- `E/upcomingbookings/ExpertBookingsUiState.kt` - load states and UI state
- `E/upcomingbookings/ExpertBookingsViewModel.kt` - state machine, join, hand-off
- `E/upcomingbookings/ExpertBookingsScreen.kt` - Hilt screen and stateless content with tabs, pull-to-refresh, skeleton, failure
- `E/upcomingbookings/ExpertBookingsRoute.kt` - tab root wired to Summary (origin Bookings) and Availability (origin Bookings)
- `E/upcomingbookings/subviews/ExpertUpcomingCard.kt`, `ExpertBookingsSkeleton.kt`
- `res/values/strings_expert_bookings.xml`
- Tests: `ExpertBookingsScreenTest` (7), `ExpertBookingBucketsTest` (9), `ExpertBookingsViewModelTest` (20)

(`E/` = `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/`.)

## Decisions Made

- Net earnings only, from `earnings_breakdown.net_amount`; currency from the breakdown, else the fee currency, else the column is hidden. Gross never stands in under an "Earnings" label.
- Reused `BookingsLaunch`, `BookingsNotice` and `BookingsTab` from Phase 8 instead of expert copies, because `ExpertLaunchRequest.bookings` (11-02) is already typed to them.
- `ExpertBookingsContent` accepts `onNoticeConsumed` / `onRefreshFailureConsumed` (suppressed as unused) so 11-11 adds the snackbars without changing the signature it was told to expect.

## Deviations from Plan

### Auto-fixed / minor adjustments

**1. [Rule 3 - Blocking] Skipped the new `expert_bookings_loading` string**
- **Found during:** Task 2
- **Issue:** `SkeletonGroup` already exposes the existing `content_loading` ("Loading") description, so a second string would be unused.
- **Fix:** Reused `content_loading`; noted in the strings file header.
- **Files modified:** `strings_expert_bookings.xml`

**2. Process note: RED for Tasks 1 and 3 was a compile failure**
- The new symbols did not exist, so the RED test commits (`7ebc1d5`, `aeb2795`) fail to compile by construction; no separate failing Gradle run was made (same approach as 11-03). Task 2's bucket tests passed immediately because the rules were implemented in Task 1 as the plan specifies; its skeleton and failure screen tests were written before the implementation.

**Total deviations:** 1 minor auto-fix. **Impact:** none on behavior.

## Issues Encountered

- `ktlint` rejected a long catch-branch line in the ViewModel and a chained call in a test; both were reformatted and re-verified.
- The shared per-plan ledger file under `.git/worktrees/` was writable this time; `plan_head_before` is the base SHA `2d62a57`.

## Authentication Gates

None.

## Known Stubs

None. The Past tab renders an empty list by design until 11-11 adds the Past cards, empty states and the snackbars; the plan and 11-11 both record this hand-off. No data is mocked.

## Threat Flags

None. T-11-11 (realtime payload never read or logged), T-11-12 (latest-wins burst, tested) are mitigated as planned; T-11-13 accepted (join window is a UX gate, the server enforces on `POST /join`).

## Next Phase Readiness

- 11-11 can add `ExpertPastCard`, empty states and both snackbars to `ExpertBookingsContent`; the skeleton already has `past = true`, and the ViewModel already exposes `notice`, `refreshFailed`, `consumeNotice()` and `consumeRefreshFailure()`.
- `requirements-completed: [EXP-08]` copied from the plan; the shared-ID gate is left to the orchestrator (REQUIREMENTS.md not touched).

## Self-Check: PASSED

- All 10 created files and the modified Route exist on disk (git status clean after commits).
- Commits `7ebc1d5`, `b5093ff`, `fa4a111`, `aeb2795`, `8e23e3a` are ancestors of HEAD; `git rev-list --count 2d62a57..HEAD` = 5 before this summary commit.
- Task 3 verify: `*ExpertBookingsViewModelTest` (20), `*ExpertBookingsScreenTest` (7), `*ExpertBookingBucketsTest` (9), `*JoinWindowTest` (11), `ktlintCheck` all pass; `scripts/check-design-rules.sh` prints "Design rules: OK".
- Acceptance greps: `ExpertBookingOrigin.Bookings` in Route = 1; `peerId = item.client.id` = 1; `grossAmount` in card = 0; `@Test` in bucket test = 9; `JOINABLE_BOOKING_STATUSES` in buckets = 2; `filterIsInstance<RealtimeEvent.BookingUpdate>` = 1; `launchRequest.bookings` = 3; `Log.x(` = 0.

---
*Phase: 11-expert-requests-bookings*
*Completed: 2026-10-26*

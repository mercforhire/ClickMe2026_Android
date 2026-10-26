---
phase: 11-expert-requests-bookings
plan: 07
subsystem: ui
tags: [compose, calendar-intent, confirmed, hilt, navigation3]

requires:
  - phase: 11-02
    provides: BookingConfirmedKey, ExpertLaunchRequest.dashboard slot, Navigator.showDashboard, empty bookingConfirmedEntries
  - phase: 11-03
    provides: meetingTypeLabel, meetingTypeIcon, clientDisplayName
provides:
  - Booking Confirmed screen (check badge, headline, details card, actions, entry animation)
  - CalendarEventSpec / buildCalendarInsertIntent / Context.launchCalendarInsert (ACTION_INSERT, no permission)
  - BookingConfirmedViewModel.backToHome posting DashboardLaunch
  - entry<BookingConfirmedKey> registered with the bottom bar hidden
affects: [11-12 request decision accept (replaces itself with BookingConfirmedKey), expert dashboard reload consumer]

actuals:
  tokens: 9300
  tasks: 3
  commits: 3

tech-stack:
  added: []
  patterns:
    - "Calendar insert by ACTION_INSERT intent only; ActivityNotFoundException is the single no-handler check (no resolveActivity, no <queries>)"
    - "Phase 6 staggered entry copied (saved played flag, Animatable per block, actions enabled at the end)"

key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/confirmed/CalendarIntent.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/confirmed/BookingConfirmedScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/confirmed/BookingConfirmedViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/confirmed/subviews/ConfirmedDetailsCard.kt
    - app/src/main/res/values/strings_booking_confirmed.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/confirmed/CalendarIntentTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/confirmed/BookingConfirmedScreenTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/confirmed/BookingConfirmedEntries.kt

key-decisions:
  - "BookingConfirmedContent takes an optional snackbarHostState so the stateful Screen owns the no-calendar snackbar while the content stays stateless"
  - "Add to Calendar double-tap guard is a launching flag cleared on lifecycle resume (and on no-handler), not a timer"
  - "Tests simulate a device without a calendar app with a ContextWrapper whose startActivity throws ActivityNotFoundException, because Robolectric does not throw for unresolved intents"

patterns-established:
  - "Stateful Screen wraps a stateless Content with injectable zone/timeFormat for deterministic Robolectric tests"

requirements-completed: [EXP-06, EXP-07]

duration: ~35min
completed: 2026-10-26
status: complete
plan_head_before: 2d62a572d451277a2d07d2d2715b4a38af23cb66
plan_head_after: 46d79d51ba9a00d92ec813089f3e32da5742d1b8
commits: 3

coverage:
  - id: D1
    description: "Add to Calendar builds an ACTION_INSERT intent on Events.CONTENT_URI with begin/end millis, title and description (meeting type plus trimmed client message), no location or reminder, and no calendar permission"
    requirement: EXP-07
    verification:
      - kind: unit
        ref: "CalendarIntentTest#specCarriesTitleTimesAndDescription, #blankMessageIsOmitted, #intentHasActionDataAndExtrasOnly"
        status: pass
    human_judgment: false
  - id: D2
    description: "No calendar app: ActivityNotFoundException is caught and a 'No calendar app found' snackbar shows"
    requirement: EXP-07
    verification:
      - kind: unit
        ref: "CalendarIntentTest#noHandlerCallsCallbackWithoutCrashing; BookingConfirmedScreenTest#noCalendarAppShowsSnackbar"
        status: pass
    human_judgment: false
  - id: D3
    description: "Booking Confirmed shows badge, headline naming the client, details card and the two actions from the key only"
    requirement: EXP-06
    verification:
      - kind: unit
        ref: "BookingConfirmedScreenTest#showsHeadlineAndDetails"
        status: pass
    human_judgment: false
  - id: D4
    description: "Back to Home posts a Dashboard reload and navigates to the Dashboard"
    requirement: EXP-06
    verification:
      - kind: unit
        ref: "BookingConfirmedScreenTest#backToHomePostsDashboardReload"
        status: pass
    human_judgment: false
  - id: D5
    description: "Entry animation: buttons enable at the end of the animation and at once with animations off; Calendar app handoff on a real device"
    verification:
      - kind: unit
        ref: "BookingConfirmedScreenTest#buttonsEnabledAfterEntry, #entryAnimationIsNotReplayedAfterRestoration"
        status: pass
    human_judgment: true
    rationale: "UI-SPEC marks the entry animation as a device-verified backstop; the real calendar editor opening with the right event needs a device with a calendar app"
---

# Phase 11 Plan 07: Booking Confirmed and Add to Calendar Summary

**Booking Confirmed screen (reused Phase 6 check badge, client-named headline, details card, staggered entry) with an `ACTION_INSERT` calendar hand-off that needs no permission and a Back to Home that posts a Dashboard reload.**

## Performance

- **Duration:** ~35 min (Gradle runs dominate: 3-4 min each)
- **Tasks:** 3
- **Files:** 7 created, 1 modified

## Accomplishments

- `CalendarIntent.kt`: pure spec and intent builders, plus `Context.launchCalendarInsert` that catches `ActivityNotFoundException` (no `resolveActivity`, no manifest queries, no calendar permission; manifest untouched).
- `BookingConfirmedScreen` / `BookingConfirmedContent`: layout per UI-SPEC surface 4, entry animation copied from Request Sent, `rememberSaveable played`, buttons disabled until the last block lands, snackbar host, double-tap guard on Add to Calendar.
- `BookingConfirmedViewModel.backToHome()` posts `DashboardLaunch`; entry registered with `BottomBar.hidden()`, Back to Home wired to `navigator::showDashboard`, system Back to `goBack`.
- 11 new tests (CalendarIntentTest 5, BookingConfirmedScreenTest 6), ktlintCheck and design-rules script green; `ExpertNavigationTest` still passes.

## Task Commits

1. **Task 1: Confirmed route and safe Add to Calendar intent** - `f01e47e` (feat; RED run with a stub showed 5/5 assertion failures before GREEN, folded into one commit)
2. **Task 2: Confirmed layout and entry animation** - `183caee` (feat)
3. **Task 3: Back to Home posts a Dashboard reload** - `46d79d5` (feat)

## Decisions Made

- `BookingConfirmedContent` has an extra optional `snackbarHostState` parameter beyond the planned signature, so the stateful screen can show the no-calendar snackbar.
- Robolectric does not throw `ActivityNotFoundException` for unresolved intents, so tests inject a throwing `ContextWrapper` (via `LocalContext`) to model "no calendar app".

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Acceptance grep `showDashboard` count in BookingConfirmedEntries.kt**
- **Issue:** The plan expects `grep -c 'showDashboard'` to print 1, but a Kotlin extension function used as `navigator::showDashboard` needs an import line, so the minimum is 2 (import + usage).
- **Fix:** Removed the extra KDoc mention; count is 2. Intent of the criterion (Back to Home wired to `showDashboard`) is met.
- **Commit:** 46d79d5

**2. [Process] RED and GREEN not committed separately for Task 1**
- Task 1 is `tdd="true"` but the plan is `type: execute`, so no plan-level gate applies. RED was run with a stub implementation (5/5 failed on assertions, not on build errors) and then folded with GREEN into one atomic task commit.

**Total deviations:** 1 minor acceptance-criterion adjustment, 1 process note. **Impact:** none on behavior.

## Issues Encountered

- First test compile failed because `LocalContext.current` was read inside an object expression in `setContent`; hoisted into a `remember`ed named class. No product code affected.

## Known Stubs

None.

## Threat Flags

None. No new network endpoints, permissions or manifest changes; mitigations T-11-20/21/22 implemented (plain string extras on a fixed URI, no calendar permission with `grep -c CALENDAR AndroidManifest.xml` = 0, caught `ActivityNotFoundException`).

## User Setup Required

None.

## Open Items (device-only, not blocking)

- Verify on a Samsung device: entry animation ordering, calendar editor opens prefilled with the right title, times (device zone) and description, and the no-handler snackbar with the calendar app disabled.

## Next Phase Readiness

- 11-12 (Request Decision accept) can `replaceTop(BookingConfirmedKey(...))`; the destination is real.
- The Dashboard ViewModel must consume `ExpertLaunchRequest.dashboard` to reload after Back to Home (owned by the Dashboard plan).

## Self-Check: PASSED

- Files exist: CalendarIntent.kt, BookingConfirmedScreen.kt, BookingConfirmedViewModel.kt, subviews/ConfirmedDetailsCard.kt, strings_booking_confirmed.xml, both test classes (verified on disk, built and tested).
- Commits f01e47e, 183caee, 46d79d5 are ancestors of HEAD.
- Verify: `testDevDebugUnitTest --tests *BookingConfirmedScreenTest --tests *CalendarIntentTest --tests *ExpertNavigationTest ktlintCheck` exit 0; `scripts/check-design-rules.sh` prints "Design rules: OK".

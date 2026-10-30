---
phase: 15-polish-hardening
plan: 16
subsystem: ui
tags: [settings, payouts, help, refresh, large-screen, state-audit, tracer]
status: complete

requires:
  - phase: 15-01
    provides: RefreshFailedSnackbarEffect, ErrorState extension
  - phase: 15-02
    provides: Modifier.contentMaxWidth()
provides:
  - RefreshFailedSnackbarEffect wiring (Retry = refresh) on Payouts, Activity and Help
  - contentMaxWidth() on Payouts, Activity, Help, Payment Methods and Account Security
  - Payment Methods row-shaped first-load skeleton (replaces spinner plus text)
  - Account Security focus on the first newly failing field
  - State, form and large-window tests on five test classes
affects: [15-17, 15-18, 15-19, 15-20]

actuals:
  tokens: 11000
  tasks: 2
  commits: 2
plan_head_before: 06c3c8dee2a7c7d0424119fd91b79b04a5da663d
plan_head_after: c81a28dde090f55ebaa4f4aa8c7b70f3e8338423

key-files:
  created: []
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/payouts/PayoutsScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/payouts/TransactionsScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/help/HelpScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/paymentmethods/PaymentMethodsScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/accountsecurity/AccountSecurityScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/accountsecurity/subviews/AccountSecurityCards.kt
    - app/src/main/res/values/strings_payouts.xml
    - app/src/main/res/values/strings_payouts_activity.xml
    - app/src/main/res/values/strings_help.xml
    - app/src/main/res/values/strings_payment_methods.xml

key-decisions:
  - "The three screens use the shared common_refresh_failed copy; their per-screen refresh-failed strings (including Activity's 'Showing what was loaded') were removed, as in 15-12"
  - "Payouts' link-failed snackbar now uses the default (Short) duration without the SnackbarDuration.Short literal, so the Short refresh snackbar is gone as the plan requires while the unrelated link message keeps its duration"
  - "Account Security focus is UI-only: the card tracks which fields already showed an error and focuses the first newly failing one, so typing (which only clears a message) never steals focus; no ViewModel or state change"

patterns-established:
  - "Lists with contentPadding apply contentMaxWidth() per item (rows, footers); scroll areas and pull-to-refresh stay full width"

requirements-completed: [POL-01, POL-03, POL-04]

coverage:
  - id: D1
    description: "A failed Payouts, Activity or Help refresh keeps the content and shows the snackbar with Retry that re-runs the refresh"
    requirement: POL-01
    verification:
      - kind: unit
        ref: "PayoutsScreenTest#refreshFailedShowsRetry; TransactionsScreenTest#refreshFailedShowsRetry; HelpScreenTest#refreshFailedShowsRetry"
        status: pass
    human_judgment: false
  - id: D2
    description: "First-load skeleton, shipped empty state and named error with Retry on Payouts, Activity, Help and Payment Methods"
    requirement: POL-03
    verification:
      - kind: unit
        ref: "PayoutsScreenTest#stateCells, #stateCellsNamedErrorRetries; TransactionsScreenTest#listCells, #emptyState, #failedFirstPageShowsMessageAndRetry, #listCellsOneRowHasNoEmptyHeading; HelpScreenTest#stateCells, #failedLoadKeepsContactUs; PaymentMethodsScreenTest#stateCells, #emptyShowsInvitationAddButtonAndTrustCopy, #failedShowsHeadingMessageAndRetry"
        status: pass
    human_judgment: false
  - id: D3
    description: "Account Security: in-flight ignores repeat taps, wrong password shows inline with focus on the field, other failures show the dialog and keep every typed value"
    requirement: POL-04
    verification:
      - kind: unit
        ref: "AccountSecurityScreenTest#inFlightIgnoresTaps, #wrongPasswordFocusesField, #otherFailureDialogKeepsValues"
        status: pass
    human_judgment: false
  - id: D4
    description: "The five screens cap their content at 600 dp and center it on a 900 dp window"
    requirement: POL-03
    verification:
      - kind: unit
        ref: "PayoutsScreenTest#capped; TransactionsScreenTest#capped; HelpScreenTest#capped; PaymentMethodsScreenTest#capped, #cappedEmpty; AccountSecurityScreenTest#capped"
        status: pass
    human_judgment: false
  - id: D5
    description: "On-device look of the capped layouts, TalkBack announcement of the Retry snackbar, keyboard and focus movement on the wrong-password path"
    requirement: POL-04
    verification: []
    human_judgment: true
    rationale: "Device and TalkBack behaviour is not asserted by Robolectric tests; device testing is deferred by the user"

completed: 2026-10-29
---

# Phase 15 Plan 16: Payouts and data-loading settings screens to the state standard Summary

**Payouts, Activity and Help now offer a Retry snackbar on a failed refresh, Payment Methods loads with a row skeleton, Account Security focuses the failing field, and all five screens are capped at 600 dp.**

## Tasks

| Task | Name | Commit |
| ---- | ---- | ------ |
| 1 (tracer) | Payouts and Activity Retry snackbar, 600 dp caps | `fda64cad` |
| 2 | Help Retry snackbar, Payment Methods skeleton, Account Security focus, caps | `c81a28dd` |

**Tracer gate:** Task 1 `<verify>` (PayoutsViewModelTest 23, PayoutsScreenTest 25, TransactionsScreenTest 10, TransactionsViewModelTest 10) and the whole-project ktlintCheck were green before Task 2 started. Tracer verified end-to-end, expanding.

## Per-screen audit (D-08)

| Screen | Archetype | Result | Cells that gained a test |
| ------ | --------- | ------ | ------------------------ |
| Payouts | B (detail) | skeleton, "Couldn't load payouts" + Retry compliant; snackbar converted (was Short, action-less) | refreshFailedShowsRetry, stateCells, named error Retry, capped |
| Activity | A (list) | skeleton, empty "No activity yet", "Couldn't load activity" + Retry compliant; snackbar converted | refreshFailedShowsRetry, listCells, one row has no empty heading, capped |
| Help | A (list) | skeleton and "Couldn't load help" + Retry (Contact Us kept) compliant; snackbar converted | refreshFailedShowsRetry, stateCells, capped |
| Payment Methods | A (list) | error with Retry and "No saved cards yet" compliant; first load was a spinner with text, converted to a skeleton | stateCells, capped, cappedEmpty |
| Account Security | D (static form) | in-flight and dialog contract already held; focus on the failing field was missing, added | inFlightIgnoresTaps, wrongPasswordFocusesField, otherFailureDialogKeepsValues, capped |

Quiet reloads stay silent: Payouts' resume reload and Activity's refresh path use the existing latest-wins ViewModel modes, unchanged.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing critical functionality] Payment Methods first load was a spinner, not a skeleton**
- **Found during:** Task 2 audit (must_have: "first-load skeleton"; UI-SPEC archetype A and the rule against spinners replacing skeletons)
- **Issue:** `LoadingBody` showed a `CircularProgressIndicator` with "Loading payment methods…".
- **Fix:** Replaced with a `SkeletonGroup` of four 72 dp row blocks (the real row min height), capped at 600 dp. TalkBack hears the shared "Loading". Removed the now-unused strings `payment_methods_loading` and `payment_methods_loading_a11y`; the old test `loadingShowsMessageUnderTheAppBar` became `stateCells`.
- **Files modified:** `PaymentMethodsScreen.kt`, `strings_payment_methods.xml`, `PaymentMethodsScreenTest.kt`
- **Commit:** `c81a28dd`

**2. [Rule 2 - Missing critical functionality] Account Security did not move focus to the failing field**
- **Found during:** Task 2 (`wrongPasswordFocusesField`; UI-SPEC "Submit errors and in-flight" requires focus on the first failing field). The plan said to change code only if a test fails; there was no focus code at all.
- **Fix:** `ChangePasswordCard` gives each field a `FocusRequester` and focuses the first field whose error is new (tracked locally), so typing never pulls focus back. No ViewModel change. The same path also focuses the first field that fails local validation.
- **Files modified:** `AccountSecurityCards.kt`
- **Commit:** `c81a28dd`

**3. [Rule 1 - Bug] Refresh copy mismatches**
- **Issue:** Activity's "Showing what was loaded." and the per-screen strings were inconsistent with a Retry action. The three screens now use the shared `common_refresh_failed` copy (same wording the spec names); the three per-screen strings were removed. Existing `HelpScreenTest#refreshFailureShowsSnackbarAndConsumesFlag` already asserted the shared text and still passes.
- **Commit:** `fda64cad`, `c81a28dd`

**4. [Test adjustment] `inFlightIgnoresTaps`**
- A disabled field exposes no SetText action, so the test asserts zero text-input nodes while in flight (and exactly one API call after three taps) instead of three disabled fields.

**Total deviations:** 4, none beyond the plan's intent.

### TDD note

The tasks are `tdd="true"` in a `type: execute` plan, so the plan-level gate sequence does not apply. Tests and implementation were committed together per task (as in 15-01 and 15-12); no separate failing RED commit. Most new cases for already-compliant screens pass against existing behaviour by design.

## Verification Results

- Task 1: `PayoutsViewModelTest` 23/23, `PayoutsScreenTest` 25/25, `TransactionsScreenTest` 10/10, `TransactionsViewModelTest` 10/10.
- Task 2: `HelpScreenTest` 12/12, `HelpViewModelTest` 7/7, `PaymentMethodsScreenTest` 15/15, `AccountSecurityScreenTest` 13/13, `AccountSecurityViewModelTest` 14/14.
- Whole-project `ktlintCheck` BUILD SUCCESSFUL on both tasks. `bash scripts/check-design-rules.sh` prints "Design rules: OK".
- Acceptance greps: `RefreshFailedSnackbarEffect(` == 1 in each of PayoutsScreen, TransactionsScreen, HelpScreen; `SnackbarDuration.Short` == 0 in PayoutsScreen; `contentMaxWidth()` present in all three Task 2 screen files.

## Known Stubs

None.

## Threat Flags

None. T-15-35 holds: error bodies carry only `toSectionMessage()` text, no amounts.

## Open Items (device checks deferred by the user)

- On a device with the network off, pull to refresh on Payouts, Activity and Help: content stays, one snackbar "Couldn't refresh. Check your connection and try again." with "Retry" appears, Retry reloads, and TalkBack announces it.
- On a tablet or resizable window at 900 dp and wider: the five screens show a centered 600 dp column while app bars and pull-to-refresh span the window.
- Account Security: with a wrong current password, the cursor lands in the Current Password field and the keyboard stays up; on a server error the "Couldn't update password" dialog keeps all three typed values after "Got it".
- Review the Payment Methods skeleton against the loaded card rows on a device.

## Self-Check: PASSED

- FOUND commits: `fda64cad`, `c81a28dd`
- FOUND files: all 10 main/resource files and 5 test files listed above are modified in the commits.

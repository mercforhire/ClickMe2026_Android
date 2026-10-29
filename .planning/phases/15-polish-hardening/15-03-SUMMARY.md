---
phase: 15-polish-hardening
plan: 03
subsystem: ui
tags: [insets, design-rules, ime, sheet, restoration, tracer]

requires:
  - phase: 08-reschedule
    provides: RescheduleResponseCard and its decline flow (client and expert viewers)
provides:
  - DeclineSheet (ModalBottomSheet) replacing the reschedule DeclineDialog, with saveable open state and reason
  - DeclineReasonSaver, so a restored decline reason passes the typing cap
  - Insets rules (a)(b)(c) and the AlertDialog text-field rule (R7) in scripts/check-design-rules.sh
  - scripts/check-design-rules-selftest.sh fixture self-test
affects: [15-05, 15-10, 15-15, 15-17, 15-19]

actuals:
  tokens: 9500
  tasks: 2
  commits: 3

plan_head_before: 283f0109be4ba4e6fb5984231ad407ad3bc15e88
plan_head_after: dfa5382593b7616f356e484c326d56798ce73e5c

tech-stack:
  added: []
  patterns:
    - "Free text never lives in an AlertDialog: use a ModalBottomSheet whose scrollable body sits under imePadding (R6, R7)"
    - "// insets-ok: <reason> opt-out on or directly above a flagged line for the insets guard"

key-files:
  created:
    - scripts/check-design-rules-selftest.sh
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/components/RescheduleResponseCard.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/components/RescheduleResponseCardTest.kt
    - scripts/check-design-rules.sh

key-decisions:
  - "Decline reason restore cap lives in a custom Saver (DeclineReasonSaver) passed as rememberSaveable stateSaver, so the cap applies on restore as well as on change and is unit-testable"
  - "Sheet body modifier order is imePadding then verticalScroll (matching DeclineRequestSheet), so the scroll viewport shrinks with the keyboard"
  - "Rule (a) uses the paren-matching perl form; no opt-out comments were needed at HEAD"

requirements-completed: [POL-03]

coverage:
  - id: D1
    description: "Declining a reschedule opens a ModalBottomSheet with the reason field in an imePadding scrollable body, not an AlertDialog"
    requirement: POL-03
    verification:
      - kind: unit
        ref: "RescheduleResponseCardTest#declineOpensSheetNotDialog"
        status: pass
    human_judgment: true
    rationale: "Keyboard never covering the field or Decline button needs a real device IME to confirm (device testing deferred)"
  - id: D2
    description: "The decline sheet reopens with its typed reason after recreation, and a restored value is capped at 500"
    requirement: POL-03
    verification:
      - kind: unit
        ref: "RescheduleResponseCardTest#declineSheetRestoresReason"
        status: pass
      - kind: unit
        ref: "RescheduleResponseCardTest#restoredReasonIsCapped"
        status: pass
    human_judgment: false
  - id: D3
    description: "check-design-rules.sh enforces insets rules (a)(b)(c) and the AlertDialog text-field rule with an insets-ok opt-out, passes on app/src, and a self-test proves each rule"
    requirement: POL-03
    verification:
      - kind: other
        ref: "bash scripts/check-design-rules.sh && bash scripts/check-design-rules-selftest.sh"
        status: pass
    human_judgment: false

duration: 70min
completed: 2026-10-29
status: complete
---

# Phase 15 Plan 03: Decline Sheet and Insets Guard Summary

**Reschedule decline reason moved from an AlertDialog to an imePadding ModalBottomSheet with capped restoration, plus four new insets/dialog rules in the design-rules gate with a fixture self-test.**

## Performance

- **Duration:** about 70 min (most of it Gradle compile and test runs, including one Kotlin daemon OOM retry)
- **Tasks:** 2 (Task 1 tracer with TDD, Task 2 auto)
- **Files:** 4 (1 created, 3 modified)

## Accomplishments

- `DeclineSheet` (ModalBottomSheet, `skipPartiallyExpanded`) replaces `DeclineDialog` in `RescheduleResponseCard.kt`; title, message, reason field (tag `reschedule_decline_reason`), counter and Keep/Decline footer live in an `imePadding()` + `verticalScroll` column tagged `reschedule_decline_sheet`. Client Upcoming Details, client Booking Summary and expert Booking Summary all use the shared card, so all three flows are covered. The accept dialog and payment dialogs stay AlertDialogs (no text field).
- `showDecline` and `declineText` stay `rememberSaveable`; `declineText` now uses `DeclineReasonSaver`, which caps a restored value at `DECLINE_REASON_MAX_CHARS` (500), closing threat T-15-05.
- `scripts/check-design-rules.sh` gains: (a) Scaffold lambda padding not applied (perl recursive paren match), (b) zero-inset Scaffold without nav/IME handling, (c) Screen/Sheet text field without `imePadding`/`verticalScroll`/`LazyColumn`, (R7) text field inside an AlertDialog. Main sources only; opt-out via `// insets-ok: <reason>`. Passes on `app/src` ("Design rules: OK") with zero opt-outs in main sources.
- `scripts/check-design-rules-selftest.sh` builds temp fixtures (a_bad, a_unused, a_optout, b_bad, b_optout, c_bad, r7_bad, r7_optout, good, good_dialog) and prints "Design rules self-test: OK". Also confirmed the R7 rule flags the pre-change `RescheduleResponseCard.kt` at line 445 (matches the planner baseline).

## Task Commits

1. **Task 1 RED:** `092514a5` test(15-03): add failing tests for decline reschedule sheet
2. **Task 1 GREEN:** `39f55424` feat(15-03): decline reschedule in a keyboard-safe sheet
3. **Task 2:** `dfa53825` feat(15-03): add insets and dialog text-field rules to design-rules gate

## TDD Gate Compliance

- **RED:** 7 of 25 tests in `RescheduleResponseCardTest` failed with assertion errors on the planned behavior (sheet tag absent, saver not capping). The RED commit includes two minimal stubs so the tests compile: `RESCHEDULE_DECLINE_SHEET_TAG` and an uncapped `DeclineReasonSaver`. Semantic assessment: the target tests executed and failed on the planned assertions (missing sheet node, restore returning 600 chars), not on setup or compile faults. The `gsd_run check tdd-red-evidence` classifier was not run (helper unavailable in this shell), so RED evidence is by JUnit result plus manual inspection.
- **GREEN:** `39f55424`, all 25 tests pass, plus the three host screen test classes (127 tests in the filtered run).
- **REFACTOR:** none.
- **Tracer gate:** `<verify>` re-run end to end after the GREEN commit; tests green. `ktlintCheck` as a whole fails on pre-existing files only (see Deferred Issues); no ktlint finding in files this plan touched.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Restore cap needed a wiring that is testable**
- **Found during:** Task 1 (restoredReasonIsCapped)
- **Issue:** A saved value cannot be injected oversized through `StateRestorationTester`, so `declineText.take(cap)` at the use site would be untestable.
- **Fix:** Added `DeclineReasonSaver` (restore = `capNotes(it, DECLINE_REASON_MAX_CHARS)`) as the `stateSaver`; `restoredReasonIsCapped` tests the saver directly.
- **Files modified:** RescheduleResponseCard.kt, RescheduleResponseCardTest.kt
- **Commit:** 092514a5, 39f55424

**2. [Rule 1 - Test fix] `isDialog()` assertion on the sheet**
- **Found during:** Task 1 GREEN run
- **Issue:** `declineOpensSheetNotDialog` first asserted no title under `isDialog()`, but a ModalBottomSheet is itself hosted in a dialog window, so the assertion failed.
- **Fix:** Assert the title exists inside the sheet tag and that exactly one node carries it.
- **Commit:** 39f55424

**3. [Plan detail] Modifier order**
- Plan listed `verticalScroll().imePadding()`; implemented `imePadding().verticalScroll()` as in the `DeclineRequestSheet` analog so the viewport shrinks with the keyboard.

**4. [Plan detail] Self-test cwd**
- The self-test runs the rules from the temp dir so the existing repo-wide Stripe key scan does not walk `app/build`; otherwise 10 runs took about 2 minutes.

**Total deviations:** 2 auto-fixed, 2 minor implementation choices. **Impact:** none on scope.

## Deferred Issues

- `ktlintCheck` fails at HEAD in files outside this plan: `AccountSecurityViewModel.kt:208`, `DeleteAccountViewModelTest.kt:35`, `ClientProfileSettingsViewModelTest.kt:98` (and possibly more). Not touched here; the plan's `ktlintCheck` verify step cannot be fully green until they are fixed. The files this plan changed are lint-clean.

## Open Items (device checks deferred)

- On a device, open Decline from client Upcoming Details, client Booking Summary and expert Booking Summary; with the IME up, confirm the reason field, counter and Decline button stay visible and the sheet scrolls.
- Rotate or kill the process with the sheet open and text typed; confirm it reopens with the text.

## Known Stubs

None.

## Threat Flags

None.

## Self-Check: PASSED

- Files exist: RescheduleResponseCard.kt, RescheduleResponseCardTest.kt, check-design-rules.sh, check-design-rules-selftest.sh
- Commits found in history: 092514a5, 39f55424, dfa53825
- `bash scripts/check-design-rules.sh` -> "Design rules: OK"; `bash scripts/check-design-rules-selftest.sh` -> "Design rules self-test: OK"
- Acceptance greps: `DeclineDialog` 0, `ModalBottomSheet(` 1, `.imePadding()` 1, `insets-ok` in script 4, AlertDialog heading 1, `insets-ok` in app main 0

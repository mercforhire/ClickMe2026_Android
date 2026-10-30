---
phase: 15-polish-hardening
plan: 14
subsystem: expert-mode
tags: [expert, request-decision, sheets, restoration, focus, large-screen, tracer]
status: complete

requires: [15-01, 15-02]
provides:
  - "Request Decision: accept message and decline note pass the 1000 cap on restore; sheet restoration proven for Activity recreation"
  - "Booking Confirmed: headline takes focus once on entry; final state at once with animations off (tested)"
  - "contentMaxWidth() on Request Decision (content and footer row), Booking Confirmed, Expert Cancellation, Topic Editor"
affects: [15-17]

tech-stack:
  added: []
  patterns:
    - "Process-death text tamper test: capture a SaveableStateRegistry via LocalSaveableStateRegistry, performSave(), swap the saved MutableState for an oversized one, recompose from a new registry (rememberSaveable keeps each mutableStateOf as a saved MutableState)"
    - "Motion-off test: runComposeUiTest(effectContext = MotionDurationScale with scaleFactor 0) in its own test class"
    - "Testing a modifier-order width: the tag goes after contentMaxWidth() so semantics bounds report the capped width"

key-files:
  created: []
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/RequestDecisionScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/subviews/RequestActionFooter.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/confirmed/BookingConfirmedScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/confirmed/subviews/ConfirmedDetailsCard.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/cancellation/ExpertCancellationScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/editor/TopicEditorScreen.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/RequestDecisionScreenTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/confirmed/BookingConfirmedScreenTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/cancellation/ExpertCancellationScreenTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/topics/editor/TopicEditorScreenTest.kt

key-decisions:
  - "Request Decision caps at the point of use (capNotes on the saveable text before it reaches the sheet and the accept/decline call) instead of in a custom Saver, so any restored value, including a tampered one, is bounded"
  - "Booking Confirmed focus: ConfirmedHeadline gets a headlineModifier parameter; the screen passes focusRequester + focusable so the heading Text itself is the focus target"
  - "Large-window cap is applied to the whole scroll column/body (Booking Confirmed, Topic Editor), the trade-off 15-06 accepted for Tags: touch-scrolling outside the 600 dp column does not scroll"
  - "Topic Editor needed no loading or error conversion: it has no edit-mode prefetch"

patterns-established:
  - "Footers keep a full-width Surface and cap only the inner row/column with contentMaxWidth()"

requirements-completed: [POL-01, POL-02, POL-03, POL-04]

duration: ~2.5 h wall clock (first Gradle run 5 min cold; later runs 30 s to 1 min warm)
completed: 2026-10-29
actuals:
  tokens: 7000
  tasks: 3
  commits: 6
plan_head_before: 5f300fbc18b9d4138556b6f7bd307297273a290f
plan_head_after: e0a5ae5ea0c5ac950e77633de996285d49e96815
---

# Phase 15 Plan 14: Expert detail and form screens Summary

Request Decision restores its Accept and Decline sheets with their input, never confirms by itself after a restore and bounds restored text at the existing 1000 cap; Booking Confirmed leads TalkBack with its headline; Request Decision, Booking Confirmed, Expert Cancellation and Topic Editor cap their content at 600 dp.

## Tasks

| Task | Name | Commits |
| ---- | ---- | ------- |
| 1 (tracer, tdd) | Request Decision restoration, cap on restore, width cap | 17544dfe (RED), e1c9ec57 (GREEN) |
| 2 (tdd) | Booking Confirmed headline focus; Expert Cancellation state, restore, cap | eb903dc4 (RED), 35852ad8 (GREEN) |
| 3 (tdd) | Topic Editor load cells, IME, cap | cf00a103 (RED), e0a5ae5e (GREEN) |

## RED / GREEN evidence

- Task 1 RED (17544dfe): `capped` failed ("Request content is 900.0 dp wide, expected <= 600"). `restoredNoteCapped` and `restoredMessageCapped` failed, but at that commit they failed for a test-fixture reason (the saved state holds `MutableState` objects, not strings, so my string swap never reopened the sheet). I fixed the swap in the GREEN commit and re-proved RED by temporarily replacing `capNotes(...)` with the raw values: both tests failed again, this time on the intended assertion (no "1000/1000" counter; the restored text was 1500 characters). Semantic assessment: valid RED after the fix; the committed RED alone was only partly valid.
- Task 1 GREEN (e1c9ec57): 37/37 `RequestDecisionScreenTest`, 26/26 `RequestDecisionViewModelTest`, `ktlintCheck` pass. Tracer gate: the Task 1 verify passed end to end before Tasks 2 and 3 expanded.
- Task 2 RED (eb903dc4): `BookingConfirmedScreenTest.headlineFocusedOnEntry`, `headlineFocusedAgainOnRecreation`, `capped` and `ExpertCancellationScreenTest.capped` failed on the plan's assertions. The main edits for Booking Confirmed were written before the tests; RED was proven by temporarily removing the two behaviour lines (`contentMaxWidth()` and the `headlineModifier` argument), then restoring them in 35852ad8.
- Task 3 RED (cf00a103): `TopicEditorScreenTest.capped` failed. Load-cell and `focusedFieldAboveIme` tests pin existing behaviour and passed from the start, as the plan anticipated.
- Pinning tests that passed from the start (behaviour existed): `declineSheetSurvivesRestore`, `acceptSheetSurvivesRestore`, `closedSheetsStayClosedAfterRestore`, `firstLoadShowsSkeleton`, `failedLoadShowsNamedErrorWithRetry`, `expiredRequestIsReadOnly`, `animationsOffShowsFinalState`, Expert Cancellation `stateCells`, `failedLoadShowsNamedErrorWithRetry`, `confirmNeverAutoConfirmsAfterRestore`, `confirmStepRecreatedDoesNotCallConfirm`, and the Topic Editor load cells.

## Verification

- Final combined run: `BookingConfirmedScreenTest` (9), `BookingConfirmedMotionTest` (1), `ExpertCancellationScreenTest` (16), `ExpertCancellationViewModelTest` (18), `TopicEditorScreenTest` (22), `TopicEditorViewModelTest` (39), `RequestDecisionScreenTest` (37) plus whole-project `ktlintCheck`: BUILD SUCCESSFUL, 0 failures.
- `bash scripts/check-design-rules.sh`: "Design rules: OK".
- Acceptance greps: `contentMaxWidth()` present in RequestDecisionScreen.kt, ExpertCancellationScreen.kt, TopicEditorScreen.kt and BookingConfirmedScreen.kt (baseline 0); `FocusRequester` in BookingConfirmedScreen.kt (baseline 0).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Topic Editor has no edit-mode prefetch**
- **Found during:** Task 3 (read of `TopicEditorKey` and `TopicEditorViewModel`)
- **Issue:** The plan, citing UI-SPEC archetype D, expects skeleton fields while an edit prefetches and an `ErrorState` replacing the form body when the prefetch fails. The editor never fetches the topic: Edit is seeded from the list item in the nav key (Phase 10 D-07; there is no `GET /expert/topics/:id`). UI-SPEC archetype D says "skeleton fields only if the form prefetches", so the rule holds vacuously. Adding a fetch would be a new endpoint and an architectural change.
- **Fix:** No production change. Tests pin the truth that applies: an edit form shows its seeded fields at once with no form-level loading node, a blank create form has no loading state, and the one real load (the Expertise tag section) shows its "Loading tags…" row, keeps the app bar and form on failure, and its Retry re-runs the load.
- **Files modified:** `TopicEditorScreenTest.kt`
- **Commit:** cf00a103

**2. [Rule 3 - Blocking] Two supporting files outside the plan's file list**
- `subviews/RequestActionFooter.kt` (cap the footer's inner row/column, tag the footer surface) and `confirmed/subviews/ConfirmedDetailsCard.kt` (`ConfirmedHeadline` gets a `headlineModifier` so the heading Text carries the focus requester) were needed to reach the behaviour; both are additive.
- **Commits:** e1c9ec57, 35852ad8

**3. [Plan adaptation] Test shape**
- The plan names `stateCells` for Request Decision as one test; it is three tests (`firstLoadShowsSkeleton`, `failedLoadShowsNamedErrorWithRetry`, `expiredRequestIsReadOnly`) so a failure names its cell. The Request Decision error block (`FailedBody`: EmptyState with "Couldn't load request", the failure body and Retry, in a polite live region) and the Expert Cancellation `FailedBody` were audited as compliant and left unchanged.
- `restoredNoteCapped` is built by tampering a saved registry (see patterns), because the sheets cap input on typing and a restored value is otherwise indistinguishable from typed text. A twin `restoredMessageCapped` covers the Accept message.
- `animationsOffShowsFinalState` lives in its own class `BookingConfirmedMotionTest` (same file) because `runComposeUiTest` must not run alongside a `createComposeRule` rule.

## Open Items (deferred device checks)

- Real-device TalkBack check that Booking Confirmed starts on its headline and that Back from the focus restores sensibly.
- Real-device "Don't keep activities" check through Request Decision with the Decline sheet open: reason, note and the sheet return; nothing is sent.
- Visual check of the 600 dp cap on a tablet or resized window for the four screens: app bars and the Request Decision footer background full width, Topic Editor form centred.
- Device check of the Topic Editor description field above a real IME (the Robolectric test dispatches synthetic insets and scrolls the field into the shrunk viewport).
- Animations-off device check for Booking Confirmed (the test sets a zero MotionDurationScale, not the system developer option).

## Known Stubs

None.

## Threat Flags

None. T-15-29 (restored text over the cap) and T-15-30 (restored confirm acting without a tap) are mitigated and tested (`restoredNoteCapped`, `restoredMessageCapped`, `declineSheetSurvivesRestore`, `acceptSheetSurvivesRestore`, `confirmNeverAutoConfirmsAfterRestore`).

## Self-Check: PASSED

- Modified files present: RequestDecisionScreen.kt, RequestActionFooter.kt, BookingConfirmedScreen.kt, ConfirmedDetailsCard.kt, ExpertCancellationScreen.kt, TopicEditorScreen.kt and the four test classes FOUND.
- Commits 17544dfe, e1c9ec57, eb903dc4, 35852ad8, cf00a103, e0a5ae5e are ancestors of HEAD.

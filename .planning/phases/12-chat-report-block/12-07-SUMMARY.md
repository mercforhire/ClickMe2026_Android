---
phase: 12-chat-report-block
plan: 07
subsystem: chat
tags: [report, block, unblock, moderation, compose]
status: complete

requires:
  - phase: 12-chat-report-block
    provides: "12-01 ChatsRepository/UserReportReason, 12-02 BlockState/ChatSessionState, 12-03 ChatReportBlockKey/ChatLaunchRequest/returnToConversations"
provides:
  - "ReportAndBlockViewModel / ReportAndBlockScreen / ReportAndBlockContent / ReportAndBlockRoute body"
  - "ReportCard, BlockCard (CHAT-UNBLOCK section 5 matrix)"
  - "components/safety: ReportReasonGroup, ReportDetailsField, UserReportReason.labelRes, BlockUserDialog (shared with the profile, 12-10)"
  - "SafetyErrors: SafetyDialog, toReportFailure, toBlockFailure, toUnblockFailure, isAccountSuspended"
  - "capText (surrogate-safe UTF-16 cap, reused by the composer in 12-08)"
affects: [12-08, 12-10]

key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/reportandblock/ReportAndBlockViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/reportandblock/ReportAndBlockScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/reportandblock/subviews/ReportCard.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/reportandblock/subviews/BlockCard.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/SafetyErrors.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/TextCap.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/components/safety/ReportReasonGroup.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/components/safety/BlockUserDialog.kt
    - app/src/main/res/values/strings_report_user.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/chat/reportandblock/ReportAndBlockViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/chat/reportandblock/ReportAndBlockScreenTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/components/safety/BlockUserDialogTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/chat/SafetyErrorsTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/reportandblock/ReportAndBlockRoute.kt
    - app/src/main/res/values/strings_chat_report_block.xml

key-decisions:
  - "Unblock is guarded in the ViewModel as well as hidden in the card: unblock() is a no-op unless BlockState.showsUnblock (T-12-21)"
  - "On 403 NOT_BLOCKER the stale session blocked-by-me mark is cleared too, so a later scan cannot resurrect Unblock"
  - "The block-state init applies the session blocked-by-me mark immediately (before the scan), so a thread blocked this session shows Unblock even if the scan fails or omits the thread"
  - "ACCOUNT_SUSPENDED shows no dialog here (the global suspended-account dialog owns it); mappers stay total and the ViewModel checks isAccountSuspended()"
  - "A failed Block closes the confirm dialog and shows the mapped error dialog"

requirements-completed: [SAFE-01, SAFE-02, SAFE-03, SAFE-04]

duration: ~55min
completed: 2026-10-27
commits: 3
plan_head_before: e0a298fc91d76d676bd22914ec24bae368e65356
plan_head_after: 7822e23c704c8aab945e8cdf6e5304ffaefbde16
actuals:
  tokens: 23000
  tasks: 3
  commits: 3
---

# Phase 12 Plan 07: Report & Block screen Summary

**Combined Report & Block screen from a chat thread: six enum report reasons with optional capped details, the CHAT-UNBLOCK section 5 Block card (block with confirm, unblock for the blocker only with a NOT_BLOCKER latch), mapped error dialogs, an in-flight Back lock, and the shared reason group, details field, block dialog and error mappers for the Expert Profile (12-10).**

## What was built

- **Task 1 (tracer), `3b18a41`:** `capText` (UTF-16, surrogate-safe), `ReportReasonGroup` / `ReportDetailsField` / `UserReportReason.labelRes` (spec labels, not iOS labels), `ReportCard` (disabled Submit with "Choose a reason first", spinner, 200 ms cross-fade to the polite "Thanks, we'll review this." row), the report path of `ReportAndBlockViewModel` (SavedStateHandle mirror of reason wire value and details), screen and route body. The report goes out as `ChatActionRequest("report", wire, trimmed details)`; blank details are omitted by the repository.
- **Task 2, `eadc41a`:** scan on open (`findThread`, limit 100, silent on failure), `BlockCard` per the matrix (Block / Unblock tonal / "You can't reply" with no button), shared `BlockUserDialog` (Cancel default focus, spinner, non-dismissible in flight), block exit (`markBlockedByMe`, `ConversationsNotice.Blocked`, `Blocked` event -> `returnToConversations`), unblock (`clearBlockedByMe`, `ThreadNotice.Unblocked`, `Unblocked` event -> `goBack`), NOT_BLOCKER latch.
- **Task 3, `7822e23`:** `SafetyErrors.kt` (report 429 / 404, block and unblock 404, NOT_BLOCKER server message with fixed-copy fallback, network, generic), `SafetyDialog` state and `AuthErrorDialog` rendering, `backEnabled = !inFlight` plus `BackHandler(enabled = inFlight)`, self-thread hides every action button (and the ViewModel ignores the actions).

## Verification

- `:app:testDevDebugUnitTest` for `*SafetyErrorsTest` (7), `*ReportAndBlockViewModelTest` (18), `*ReportAndBlockScreenTest` (11), `*BlockUserDialogTest` (4): all pass; `ktlintCheck` passes.
- `bash scripts/check-design-rules.sh`: "Design rules: OK".
- Acceptance greps: `reportInChat`, `markBlockedByMe`, `ErrorCode.NotBlocker`, `returnToConversations`, `DestructiveOutlinedButton`, `TooManyRequests`, `backEnabled`, `Role.RadioButton` present; no `Log.x(` in the ViewModel; no "fake profile" in `strings_report_user.xml`.
- Tracer gate: Task 1 round trip (reason tap, details, Submit -> recorded `report/scam_or_fraud/trimmed` action -> Thanks row) verified end to end before Tasks 2 and 3; no `blocking-human` gate.

## Prohibitions covered by tests

- Only enum wire values are sent (`reportSendsWireValueAndShowsThanks`, `blankDetailsAreOmitted`).
- Unblock never offered or accepted without `showsUnblock` (`unblockIsIgnoredWhenTheCallerIsNotTheBlocker`, `notBlockerLatchesUnblockOff`, `blockedByThePeerShowsTheBannerAndNoButton`).
- Submitted / blocked / unblocked results only after a server success (`blockFailureDoesNotMarkOrNotify`, `reportErrorKeepsInputs`).

## Deviations from Plan

### TDD ordering

**1. [Process] Tests and implementation committed together per task, no separate RED commit**
- The plan is `type: execute`; each test file names symbols that did not exist before the implementation, so a RED run would be a compile failure (INVALID_RED), not an assertion failure. Each task commit contains its tests and all pass.

### Auto-fixed Issues

**2. [Rule 2 - Missing critical functionality] ViewModel-level guard for Unblock and stale-mark clearing**
- **Found during:** Task 2
- **Issue:** The plan only hid the Unblock button; a stale call could still send `unblock` for a thread the caller does not own, and a session blocked-by-me mark could outlive a NOT_BLOCKER answer.
- **Fix:** `unblock()` returns unless `block.showsUnblock`; on NOT_BLOCKER `clearBlockedByMe` is called.
- **Files modified:** ReportAndBlockViewModel.kt
- **Commit:** eadc41a

**3. [Rule 2 - Missing critical functionality] ACCOUNT_SUSPENDED handled globally**
- **Found during:** Task 3
- **Issue:** Showing a local "Couldn't ..." dialog on a 403 ACCOUNT_SUSPENDED would stack on the global suspended-account dialog.
- **Fix:** `isAccountSuspended()` helper; the ViewModel sets no dialog for it.
- **Commit:** 7822e23

**4. [Minor] 404 also matches `ErrorCode.NotFound`** in the mappers (code first, then status, per the RESEARCH note).

## Known Stubs

None.

## Threat Flags

None. The ViewModel has no logging (T-12-22); no new network surface.

## Deferred / Open Items

- On-device check of the Back lock with predictive back and the Block flow's landing on Conversations with the "Blocked {name}" notice needs the Conversations screen (12-05) and thread (12-09); manual testing is deferred per project policy. The consumer side of `ConversationsNotice.Blocked` and `ThreadNotice.Unblocked` belongs to those plans.
- A scan that returns after the user already blocked would overwrite the local state with the server's; the server state is the same, so this is benign.

## Self-Check: PASSED

All created files exist and commits 3b18a41, eadc41a, 7822e23 are ancestors of HEAD.

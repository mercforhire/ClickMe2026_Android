---
phase: 12-chat-report-block
plan: 10
subsystem: chat
tags: [expert-profile, report-sheet, block-dialog, moderation]
status: complete

requires:
  - phase: 12-chat-report-block
    provides: "12-01 ChatsRepository.initiate/block/reportUser, 12-02 ChatSessionState.markBlockedByMe, 12-04 ChatEntryViewModel/toChatEntryError on the profile, 12-07 ReportReasonGroup/ReportDetailsField/BlockUserDialog/SafetyErrors"
provides:
  - "ProfileSafetyViewModel (report sheet and block dialog state machine for the Expert Profile)"
  - "ReportUserSheet / ReportUserSheetContent (ModalBottomSheet port of iOS ReportUserSheet)"
  - "Expert Profile More options menu, report sheet, block dialog, error dialog and snackbar wiring"
affects: [12-15]

key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/ProfileSafetyViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/components/safety/ReportUserSheet.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/ProfileSafetyViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/components/safety/ReportUserSheetTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/ExpertProfileScreen.kt
    - app/src/main/res/values/strings_report_user.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/ExpertProfileScreenTest.kt

key-decisions:
  - "Block failures and initiate failures both go through failureDialog, which returns null for ACCOUNT_SUSPENDED (the global dialog owns it), matching 12-07"
  - "A blank expert name falls back to 'You blocked this user' for the snackbar; the dialog already has its own blank-name fallback"
  - "The snackbar is consumed before it is shown so a recomposition cannot re-show it; showSnackbar suspends until dismissed"
  - "Block confirm dialog state is not saved across process death (only the report sheet, reason and details are, per the plan)"

patterns-established:
  - "Stateless ReportUserSheetContent separate from the ModalBottomSheet window so Robolectric tests render it directly (same split as DeclineRequestSheet)"

requirements-completed: [SAFE-01, SAFE-02, SAFE-04]

duration: ~75min
completed: 2026-10-27
commits: 3
plan_head_before: daa12dd1d9ff48e83c44a9ae369cb3bb3b9ce2c5
plan_head_after: 4794fb901e97fcfbea4f14e89cc596c25d4641cf
actuals:
  tokens: 14000
  tasks: 3
  commits: 3
---

# Phase 12 Plan 10: Report and Block from the Expert Profile Summary

**Expert Profile top-bar More options menu with a Report bottom sheet (enum reason via users/{id}/report, thank-you snackbar) and a Block confirm that initiates the thread, blocks it, marks it blocked this session and stays on the profile, with mapped error dialogs and in-flight dismissal locks.**

## What was built

- **Task 1 (tracer), `f285f23`:** `ProfileSafetyViewModel` (report path, `SavedStateHandle` mirror of sheet open, reason wire value and details), `ReportUserSheet` / `ReportUserSheetContent`, the 48dp More options `DropdownMenu` (Report this profile, Block {name}) and `SnackbarHost` in `ExpertProfileContent` (`showSafetyMenu`, `onReport`, `onBlock`, `snackbarHostState`), and the seven new strings.
- **Task 2, `1b311c6`:** `confirmBlock(expertId, name)`: single flight, `repository.initiate` then `repository.block`, `markBlockedByMe(threadId)`, closes the dialog, "You blocked {name}" snackbar, no navigation. `BlockUserDialog` and the `AuthErrorDialog` error surface wired into the screen. Initiate 404 shows "This user is unavailable." (`toChatEntryError`), block failures use `toBlockFailure`.
- **Task 3, `4794fb9`:** report failures through `toReportFailure` shown over the still-open sheet with inputs intact; `dismissReport` ignored in flight; sheet `confirmValueChange` refuses Hidden and `onDismissRequest` is a no-op while submitting; Submit shows a spinner with "Submitting", Cancel disabled; buttons stack with Submit first at font scale 1.5 or more, or below 320dp.

## Verification

- `:app:testDevDebugUnitTest` for `*ProfileSafetyViewModelTest` (10), `*ReportUserSheetTest` (5), `*ExpertProfileScreenTest`, `*ExpertProfileViewModelTest` (unchanged): all pass; `ktlintCheck` passes.
- `bash scripts/check-design-rules.sh`: "Design rules: OK".
- Acceptance greps: `reportUser`, `skipPartiallyExpanded`, `profile_menu_report`, `markBlockedByMe`, `repository.initiate`, `BlockUserDialog`, `toReportFailure`, `confirmValueChange` present; no `Log.x(` in the ViewModel.
- Tracer gate: Task 1 round trip (reason, trimmed details, Submit, recorded `("e1", ReportUserRequest("impersonation","fake"))`, snackbar) verified before Tasks 2 and 3; no `blocking-human` gate.

## Deviations from Plan

**1. [Process] Tests and implementation committed together per task, no separate RED commit.** Tests reference symbols that do not exist before the implementation, so a RED run is a compile failure (INVALID_RED), as in 12-04 and 12-07.

**2. [Rule 2 - Missing critical functionality] ACCOUNT_SUSPENDED handled globally.** Report, block and initiate failures set no local dialog for `ACCOUNT_SUSPENDED`, avoiding a stacked "Couldn't ..." dialog over the global suspended-account dialog (same as 12-07). Commit 4794fb9 (report path) and 1b311c6 (block path, via the shared helper added in 4794fb9).

**3. [Minor] ViewModel constructor.** Task 1 introduced the ViewModel with `(SavedStateHandle, ChatsRepository)`; `ChatSessionState` was added in Task 2 when it was first needed, avoiding an unused dependency in the tracer commit.

**4. [Minor] Dedicated blank-name snackbar string** `profile_blocked_snackbar_fallback` added; not in the plan.

## Known Stubs

None.

## Threat Flags

None. T-12-30 mitigated (in-flight guard and sheet lock, 429 mapped to "Please try again later."). T-12-32 mitigated (no logging in the ViewModel, details never echoed). T-12-31 accepted as planned (session mark covers the empty thread).

## Deferred / Open Items

- On-device checks (sheet swipe/Back lock while submitting, rotation with the sheet open, the real Message tap landing on a blocked thread after a profile block, TalkBack focus on the sheet title) need a device and the thread screen (12-09); manual testing is deferred per project policy.
- Re-reporting the same user inside the server's window is not specially handled; the server's answer is shown through the generic mapping.

## Self-Check: PASSED

All created files exist and commits f285f23, 1b311c6, 4794fb9 are ancestors of HEAD.

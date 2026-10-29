---
phase: 14-payouts-account-settings
plan: 05
subsystem: account-deletion, root-overlay, error-mapping
tags: [account-deletion, play-policy, one-way, farewell, session-teardown, call-guard]
requires: [14-01, 14-02]
provides:
  - "DeleteAccountKey, DeleteAccountScreen, DeleteAccountContent, DeleteAccountViewModel (Warning and Reason steps)"
  - "AccountDeletionState singleton and AccountDeletionFarewellHost rendered above the route switch in ClickMeApp"
  - "ErrorCode.ClientHasUpcomingBookings"
  - "strings_delete_account.xml"
affects: [14-14]
tech-stack:
  added: []
  patterns: ["root-level singleton flag drives an overlay that survives session teardown", "touch-swallowing full-screen overlay"]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/deleteaccount/DeleteAccountKey.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/deleteaccount/DeleteAccountReason.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/deleteaccount/DeleteAccountRepository.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/deleteaccount/AccountDeletionState.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/deleteaccount/DeleteAccountViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/deleteaccount/DeleteAccountScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/deleteaccount/subviews/DeleteAccountComponents.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/deleteaccount/DeleteAccountFarewell.kt
    - app/src/main/res/values/strings_delete_account.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/settings/deleteaccount/DeleteAccountViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/settings/deleteaccount/DeleteAccountScreenTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/settings/deleteaccount/AccountDeletionFarewellTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/ClickMeApp.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/enums/ErrorCode.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/network/ApiErrorParserTest.kt
key-decisions:
  - "Farewell hosted in a root Box in ClickMeApp (last child) and driven by AccountDeletionState, not isLoggedIn (RESEARCH Pitfall 3)"
  - "One isDeleting flag covers delete-request and DELETE; it stays set after success so the spinner sits behind the Farewell"
  - "The Farewell root swallows pointer input so Login underneath a forced logout is never tappable"
  - "ACCOUNT_SUSPENDED shows nothing on the form (the global suspended dialog owns it)"
requirements-completed: [PROF-04]
status: complete
duration: ~75min
completed: 2026-10-29
actuals:
  tokens: 18900
  tasks: 3
  commits: 4
plan_head_before: 4b40aff415a7e5278fcf17fe862865fb5524c4e7
plan_head_after: 84af1274d5e70358f53f1fa0a515337661818160
---

# Phase 14 Plan 05: Delete Account Summary

In-app account deletion ported from iOS: a Warning form, a Reason plus password form that runs `POST user/account/delete-request` then `DELETE user/account` on one guarded confirm, and a root-level Farewell that the post-deletion session teardown cannot remove.

## What was built

- **Data layer.** `DeleteAccountReason` (four wire values), `DeleteAccountRepository` (token returned to the caller, never stored), `AccountDeletionState` (`@Singleton` `StateFlow<Boolean>`), `DeleteAccountKey`.
- **ViewModel.** `DeleteAccountViewModel` with `DeleteStep { Warning, Reason }`. `continueFromWarning()` and `confirmDeletion()` both go through `CallGuard.attempt(RestrictedAction.DeleteAccount)`. `confirmDeletion()` sets `isDeleting` synchronously, so a second tap sends nothing; feedback is trimmed and sent as null when blank; the comments clamp at 1000 (`FEEDBACK_MAX`). State `toString()` redacts the password; no `SavedStateHandle`.
- **Error mapping** keyed by which call failed: 401/403 on the request step and 400 `INVALID_TOKEN` on the DELETE go under the password field; 429, 409 (server message verbatim), network and everything else open "Couldn't delete account". After a 409 the form stays intact and a retry mints a new token.
- **Screen.** Two steps in `AnimatedContent`, shared `DeleteAccountWarningHeader`, a `selectableGroup` of four `Role.RadioButton` rows, multi-line comments with a counter from 800, masked password, pinned `DeleteAccountBottomBar` (Keep my account / Delete Account, spinner in flight). Back arrow disabled and system Back swallowed while in flight; system Back on Reason returns to Warning. No Pause card (D-06).
- **Farewell.** `DeleteAccountFarewell` (headline takes focus, pinned "Back to Login"), `AccountDeletionFarewellViewModel.backToLogin()` (logs out only when still logged in, then clears the flag, re-entrancy guarded), `AccountDeletionFarewellHost` (fade in, Back swallowed). `ClickMeApp` now wraps its body in a `Box` and renders the host last.
- `ErrorCode.ClientHasUpcomingBookings("CLIENT_HAS_UPCOMING_BOOKINGS")` in the Android-only block (count test updated 68 to 69).

## TDD evidence

- **Task 1 RED** (commit c7d6f32): `DeleteAccountViewModelTest` and `AccountDeletionFarewellTest` compiled against inert stubs and failed on their planned assertions (`happyPathRunsBothCallsThenShowsFarewell`, `blankFeedbackIsOmitted`, `canConfirmNeedsReasonAndPassword`, `backToLoginLogsOutThenHides`, `farewellRendersTheIosCopy`: 5 of 6 failed; `confirmWithoutAReasonSendsNothing` passed legitimately against the no-op stub). Semantic assessment: each failure was an assertion on the planned behavior, not a build, fixture or discovery fault. `gsd_run check tdd-red-evidence` was not run (JUnit XML record not produced in the format it requires); the evidence is the Gradle report.
- **Task 2 RED**: with `ErrorCode` added, four ViewModel error tests failed on the generic-dialog behavior (`requestStepAuthErrorsGoUnderPassword`, `expiredTokenGoesUnderPassword`, `conflictShowsServerMessageAndRetryMintsNewToken`, `rateLimitAndNetworkDialogs`). Two screen tests failed because the test clicked rows below the Robolectric viewport; fixed in the test with `performScrollTo()` (test defect, not product). RED and GREEN for this task share one commit (f7b9c72).
- **Task 3 RED**: `farewellOutlivesForcedLogout` failed with `expected:<0> but was:<1>`: a tap on the Login route underneath reached it through the Farewell (real bug). Fixed with `pointerInput(Unit) {}` on the Farewell root (84af127). The other three Task 3 tests passed on arrival because Task 1's design already satisfied them (expected: the Farewell was built independent of `isLoggedIn`).

## Verification

- `DeleteAccountViewModelTest` (13), `DeleteAccountScreenTest` (11), `AccountDeletionFarewellTest` (6), `AppViewModelTest`, `ApiErrorParserTest` pass; `ktlintCheck`, `:app:assembleDevDebug` and `scripts/check-design-rules.sh` ("Design rules: OK") pass.
- Acceptance greps: `AccountDeletionFarewellHost(` in ClickMeApp.kt = 1; `markDeleted()` in the ViewModel = 1; `SavedStateHandle` = 0; "pause" in strings_delete_account.xml = 0; `BackHandler` in the Farewell file >= 1; `FEEDBACK_MAX = 1000` declared once.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Farewell let touches fall through to the route underneath**
- **Found during:** Task 3 (RED test `farewellOutlivesForcedLogout`)
- **Issue:** A plain `Box` with a background is not a pointer target, so after a forced logout the Login screen under the Farewell could be tapped.
- **Fix:** `Modifier.pointerInput(Unit) {}` on the Farewell root.
- **Files modified:** DeleteAccountFarewell.kt
- **Commit:** 84af127

**2. [Rule 3 - Blocking] ApiErrorParserTest pins the ErrorCode count**
- **Found during:** Task 2
- **Issue:** Adding `ClientHasUpcomingBookings` breaks `errorCode_has65IosCasesThenAndroidAdditions` and the wire-order test (68 to 69). The file is not in the plan's `files_modified`.
- **Fix:** Updated the count, the Android-only wire list and added a decode assertion.
- **Files modified:** ApiErrorParserTest.kt
- **Commit:** f7b9c72

**3. [Plan shape] `DeleteAccountScreen` parameter order**
- `DeleteAccountScreen(onBack, modifier, viewModel = hiltViewModel())` (viewModel last, Kotlin convention) rather than viewModel first. 14-14 must call it with `onBack = ...`.

**4. [Plan shape] Extra test tags and components**
- Added `delete_confirm` (Reason form's Delete Account), `delete_comments`, `delete_reason_<wire>`, and `DeleteAccountCommentsField` / `DeleteAccountPasswordField` / `DeleteAccountReasonSection` row tags. The top bar title and the button share the text "Delete Account", so tests need tags to address the button.

## Known Stubs

None.

## Deferred / Open Items

- **Backstop (device, deferred by project policy):** after a real DELETE the Farewell stays visible through the session teardown and Back to Login reaches Login; process death between DELETE and response leaves the next launch on Login. Logged as open manual-test items.
- **Autofill:** `ValidatedField` defaults secure fields to `ContentType.Password`; the UI-SPEC asks for no autofill hint on the deletion password. The shared component has no "none" option, so this is left as is. Minor, candidate for 14-16.
- **Registration:** `DeleteAccountKey` is not registered in any NavDisplay and Account Security does not link to it yet; that is 14-14.
- **Parallel-merge note:** `ApiErrorParserTest` pins the ErrorCode count and wire list; if another wave-2 plan also adds an ErrorCode, the orchestrator will need to reconcile the count (69 here) at merge.

## Threat Flags

None. The only new surface is the root overlay, which makes no request.

## Self-Check: PASSED

- All created files listed above exist; commits c7d6f32, ca8a938, f7b9c72, 84af127 are ancestors of HEAD.

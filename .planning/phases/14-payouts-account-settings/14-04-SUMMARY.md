---
phase: 14-payouts-account-settings
plan: 04
subsystem: settings
tags: [account-security, change-password, validation, call-guard, compose]
requires:
  - phase: 14-01
    provides: ClickMeApi.updatePassword, UpdatePasswordRequest, FakeClickMeApi updatePassword hooks
  - phase: 14-02
    provides: RestrictedAction.ChangePassword and RestrictedAction.DeleteAccount call-guard copy
provides:
  - AccountSecurityKey nav route
  - AccountSecurityScreen(onBack, onOpenDeleteAccount) with change-password and Delete Account entry cards
  - AccountSecurityViewModel, AccountSecurityRepository, validateChangePassword
affects: [14-14 shell wiring (registers AccountSecurityKey, wires onOpenDeleteAccount to DeleteAccountKey)]
tech-stack:
  added: []
  patterns:
    - Memory-only password state with a redacting UiState toString and no SavedStateHandle
    - Status-based server error mapping (403 field, 422 envelope fields, 429 dialog)
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/accountsecurity/AccountSecurityKey.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/accountsecurity/AccountSecurityRepository.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/accountsecurity/AccountSecurityViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/accountsecurity/AccountSecurityScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/accountsecurity/subviews/AccountSecurityCards.kt
    - app/src/main/res/values/strings_account_security.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/settings/accountsecurity/AccountSecurityViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/settings/accountsecurity/AccountSecurityScreenTest.kt
  modified: []
key-decisions:
  - "Local validation shows only the first failing message (iOS order), on its own field; the call guard runs after validation passes"
  - "429 and every non-field failure use one dialog titled Couldn't update password with the matching body"
  - "ACCOUNT_SUSPENDED responses show nothing here (the global suspended-account dialog owns them)"
patterns-established:
  - "validateChangePassword returns Pair<PasswordField, @StringRes Int>? so the order is unit-testable without Android resources"
requirements-completed: [PROF-03]
status: complete
duration: 55min
completed: 2026-10-29
commits: 4
plan_head_before: 4b40aff415a7e5278fcf17fe862865fb5524c4e7
plan_head_after: 8c6fee357c210eb62b566334deeaaa4872ff6ffa
actuals:
  tokens: 21000
  tasks: 2
  commits: 4
---

# Phase 14 Plan 04: Account Security Summary

**Account Security screen with iOS-validated change-password (PATCH user/password, 403/422/429 mapping, wipe on success) and a call-guarded Delete Account entry card.**

## What was built

- **Change Password card**: heading, rule line, three masked `ValidatedField`s (built-in "Show password" / "Hide password" toggles), `PasswordStrengthMeter` under New Password, full-width `AsyncSaveButton` "Update Password" (56dp, spinner in flight).
- **Delete Account entry card**: `surfaceContainerLow`, `medium`, 1dp `outlineVariant`, `heading()`, full-width `DestructiveOutlinedButton`.
- **ViewModel**: `submit()` ignores taps unless `saveState == Idle`, validates in the iOS order, then runs `CallGuard.attempt(ChangePassword)`, then sends once. Success clears the three values and opens the "Password updated" dialog. `requestDeleteAccount()` returns `CallGuard.attempt(DeleteAccount)`; the screen calls `onOpenDeleteAccount` only when it is true.
- **Error mapping**: 403 puts the server message (else "Current password is incorrect.") under Current Password; 422 places `current_password` / `new_password` entries on their fields, falling back to the dialog when none match; 429 and everything else open the dialog ("Too many attempts..." or "Couldn't update your password...").
- **In-flight lock**: Back arrow disabled and system Back swallowed while saving; the button ignores taps unless Idle.
- **Privacy (T-14-10)**: passwords live in `AccountSecurityUiState` only; `toString()` redacts them; no `SavedStateHandle` (grep count 0); fields wiped on success.

## Task Commits

1. Task 1 RED: `1def7ae` test (compile-failing tests, production types absent, per the 14-03 precedent)
2. Task 1 GREEN: `3e3d089` feat - change password with validation and success dialog
3. Task 2 RED: `15fa01a` test - 3 assertion failures confirmed (forbiddenMapsToCurrentField, validationEnvelopeMapsFields, tooManyAttemptsDialog)
4. Task 2 GREEN: `8c6fee3` feat - field and dialog error mapping

## Verification

- `testDevDebugUnitTest --tests '*AccountSecurityViewModelTest' --tests '*AccountSecurityScreenTest' --tests '*SetNewPassword*' ktlintCheck :app:assembleDevDebug`: BUILD SUCCESSFUL (ViewModel test 9 tests, Screen test 11 tests, 0 failures).
- `bash scripts/check-design-rules.sh`: "Design rules: OK".
- Acceptance greps: `RestrictedAction.ChangePassword` in the view model >= 1, `RestrictedAction.DeleteAccount` == 1, `"new_password"` >= 1, `SavedStateHandle` == 0, `account_security_update` string present.

## Deviations from Plan

### Auto-fixed / additions

**1. [Rule 2 - Missing] Added three field placeholder strings**
- Issue: the plan's string list had labels but no placeholders; `ValidatedField` requires `placeholder`.
- Fix: added `account_security_current_hint`, `account_security_new_hint`, `account_security_confirm_hint` ("Enter your current password", "Enter your new password", "Confirm your new password").
- Files: `strings_account_security.xml`. Commit: `3e3d089`.

**2. [Ordering] `requestDeleteAccount()` implemented in Task 1**
- The Task 1 screen wires `onDeleteAccount` through it, so the guarded method landed with Task 1 (plan baseline said Task 2). Tests for it are in Task 2's RED commit. No behavior difference.

**3. [Rule 2 - Missing] ACCOUNT_SUSPENDED handling**
- A suspended-account 403 would otherwise appear as "Current password is incorrect." under the field; it now returns silently like `toDialogError()` (Phase 2 D-08).

**4. Spec detail**
- The seven validation-message strings are named `account_security_error_*` (the plan left names open); `account_security_show_password` / `hide_password` were not added because `ValidatedField` already exposes "Show password" / "Hide password".

## Authentication Gates

None.

## Known Stubs

None.

## Threat Flags

None. No new network surface beyond the planned `PATCH user/password`.

## Open items for the orchestrator / later plans

- 14-14 must register `AccountSecurityKey` and wire `onOpenDeleteAccount` to `DeleteAccountKey`.
- PROF-03 remains unclassified for the verifier (plan assumption).
- Manual device check of the keyboard flow (IME Next / Done, TalkBack announcements) is deferred per the Phase 2 manual-testing note.

## Self-Check: PASSED

All eight created files exist; commits `1def7ae`, `3e3d089`, `15fa01a`, `8c6fee3` are ancestors of HEAD.

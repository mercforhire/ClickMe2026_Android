---
phase: 03-login-password-recovery-realtime
plan: 02
subsystem: auth-ui-shared
tags: [validation, compose, components, password-strength, strings]
requires: []
provides:
  - screens.login.FieldError (Res | Text), asString(), toDialogError(), fieldMessage()
  - screens.login.AuthValidators (login, forgot, reset-code, new password, confirm, sanitizeCode)
  - screens.login.AuthErrorDialog and AuthTopAppBar
  - components.PasswordStrength and PasswordStrengthMeter
  - ValidatedField trailingBadge and contentType parameters
  - Motion.SAVED_CONFIRMATION_HOLD_MS, STRENGTH_METER_VISIBILITY_MS, STRENGTH_SEGMENT_COLOR_MS
  - res/values/strings_auth.xml
affects: [03-login, 03-forgot-password, 03-set-new-password, phase-4-signup]
tech-stack:
  added: []
  patterns: [FieldError sealed type (string resource for local, verbatim for server), aliased semantics import]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/FieldError.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/AuthValidators.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/AuthErrorDialog.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/AuthTopAppBar.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/components/PasswordStrength.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/components/PasswordStrengthMeter.kt
    - app/src/main/res/values/strings_auth.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/login/AuthValidatorsTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/components/PasswordStrengthTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/components/AuthComponentsTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/components/ValidatedField.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/ui/theme/Motion.kt
key-decisions:
  - "Empty strength state exposes the 'Strength:' caption as its content description (no level word to announce)"
  - "Meter semantics use clearAndSetSemantics so TalkBack reads one phrase"
requirements-completed: []
duration: ~25 min
completed: 2026-10-20
---

# Phase 3 Plan 02: Shared auth components Summary

Shared Phase 3 building blocks: iOS-ported validators with string-resource messages, password strength scoring and a token-only meter, backward-compatible ValidatedField badge and autofill parameters, and the shared error dialog and back-arrow app bar.

## Tasks

| Task | Commits |
|------|---------|
| 1. FieldError, AuthValidators, validation copy | RED 71e3926, GREEN 6a86abe |
| 2. PasswordStrength and meter, Motion tokens | RED 33e20b1, GREEN 3e2f3b8 |
| 3. ValidatedField params, AuthErrorDialog, AuthTopAppBar | RED 96d44f8, GREEN 88eb856 |

## Verification

- AuthValidatorsTest 10, PasswordStrengthTest 6, AuthComponentsTest 6, ControlsTest 14 (unchanged), all passing.
- `ktlintCheck`, `lintDevDebug` pass; `scripts/check-design-rules.sh` prints "Design rules: OK".

## Deviations from Plan

None in behavior. Two small build fixes during Task 3: the aliased `contentType` import had to move to the end of the import block for ktlint, and `onNode` is a rule member, not an import (test file). RED commits for tasks are compile-failing tests (the types did not exist yet), which is the expected RED state.

Note: `scripts/check-design-rules.sh` and gradle were run in the worktree, not the main checkout. The worktree HEAD was reset to the expected base 32a8b88 at startup as instructed.

## Known Stubs

None.

## Threat Flags

None. T-06 (dialog shows only `userMessage`) and T-07 (ContentType.Password default for secure fields) are implemented as planned.

## Deferred Items

None. REQUIREMENTS.md not touched: AUTH-01 and AUTH-02 are completed by the Wave 2 screen plans, not by these shared pieces.

## Self-Check: PASSED

All created files exist and commits 71e3926, 6a86abe, 33e20b1, 3e2f3b8, 96d44f8, 88eb856 are in the branch log.

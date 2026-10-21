---
phase: 04-signup-onboarding-mode-switch
plan: 03
subsystem: auth-shared-components
tags: [compose, validators, shared-components, backward-compatible]
requires: []
provides:
  - AuthValidators.signupEmail / signupPassword
  - AuthErrorDialog title parameter
  - AuthTopAppBar nullable onBack, containerColor, actions
  - Avatar image (ImageBitmap) parameter, 160 dp size, AVATAR_LOCAL_IMAGE_TAG
affects: [signup screens in later phase 04 plans]
tech-stack:
  added: []
  patterns: [composable default parameter for title, nullable callback hides nav icon]
key-files:
  created: []
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/AuthValidators.kt
    - app/src/main/res/values/strings_auth.xml
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/AuthErrorDialog.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/AuthTopAppBar.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/components/avatar/Avatar.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/login/AuthValidatorsTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/components/AuthComponentsTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/components/avatar/AvatarTest.kt
decisions:
  - "Signup password follows the server rule (8+, letter, digit), not iOS length-only (D-02)"
  - "Avatar local preview uses ImageBitmap rather than a Coil File model (removes RESEARCH A7)"
metrics:
  tasks: 2
  files: 8
  completed: 2026-10-21
---

# Phase 4 Plan 03: Signup shared components Summary

Signup validators (server password rule) plus backward-compatible extensions to AuthErrorDialog (title), AuthTopAppBar (no back arrow, transparent container, actions) and Avatar (160 dp, local ImageBitmap).

## Tasks

| Task | Name | Commit |
| ---- | ---- | ------ |
| 1 | Signup validators (D-02) | 13a124e |
| 2 | AuthErrorDialog title, AuthTopAppBar options, Avatar 160 dp and local image | 5d6da5b |

## Verification

- AuthValidatorsTest, AuthComponentsTest, AvatarTest, AuthStaticScreensTest, LoginScreenTest, ForgotPasswordScreenTest and SetNewPasswordScreenTest pass; existing tests are unchanged.
- ktlintCheck and lintDevDebug exit 0; `scripts/check-design-rules.sh` prints "Design rules: OK".

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Robolectric legacy graphics cannot create ImageBitmap**
- **Found during:** Task 2
- **Issue:** `ImageBitmap(4, 4)` threw an NPE in Robolectric's default legacy graphics mode.
- **Fix:** Annotated the `localImage_isShownInsteadOfRemoteImage` test with `@GraphicsMode(GraphicsMode.Mode.NATIVE)`.
- **Files modified:** AvatarTest.kt
- **Commit:** 5d6da5b

Also ran `ktlintTestSourceSetFormat` to fix import order in the two extended test files.

## Manual checks (deferred)

None required for this plan.

## Known Stubs

None.

## Threat Flags

None.

## Self-Check: PASSED

---
phase: 04-signup-onboarding-mode-switch
plan: 07
subsystem: signup
tags: [repository, accumulator, state, patch, hydration]
requires:
  - phase: 04-01
    provides: Setup/profile DTOs, ClickMeApi endpoints, FakeClickMeApi
  - phase: 04-02
    provides: Countries.normalize, Timezones.detected
  - phase: 04-05
    provides: PhotoProcessor, FakePhotoProcessor
provides:
  - SignupSetupRepository singleton (draft, hydration, per-step saves, avatar, email verification, publish, reset)
  - SignupDraft completion and publish rules, SetupStep enum, HydrationState
  - Shared signup strings (strings_signup_common.xml)
  - SignupTestHarness for every step ViewModel test
affects: [04-06, 04-10, 04-11, 04-12, 04-13, 04-14, 04-15, 04-16]
tech-stack:
  added: []
  patterns:
    - "App-scoped @Singleton repository shared by entry-scoped ViewModels"
    - "Generation counter drops late responses after reset (mirrors UserManager.sessionGeneration)"
    - "Single-flight hydrate via Deferred started in appScope"
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/SetupStep.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/SignupDraft.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/SignupSetupRepository.kt
    - app/src/main/res/values/strings_signup_common.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/signup/SignupDraftTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/signup/SignupSetupRepositoryTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/testing/SignupTestHarness.kt
  modified: []
key-decisions:
  - "Server wins on hydrate for lists and avatar; blank/null server strings keep the existing draft value (so the device timezone default survives)"
  - "publish() does not reset the repository; the Review ViewModel calls reset() after a successful publish"
  - "hydrate() returns false (instead of throwing CancellationException) when a reset cancelled the shared run and the caller is still active"
requirements-completed: [SIGN-02, SIGN-03, SIGN-04, SIGN-05]
duration: 25min
completed: 2026-10-21
---

# Phase 4 Plan 07: SignupSetupRepository Summary

**App-scoped signup accumulator (draft, single-flight hydration, partial PATCH saves with the D-25 list rule, publish with 422 missing_fields mapping, session-end reset) plus SetupStep/SignupDraft rules and a shared test harness.**

## Accomplishments
- `SetupStep`, `HydrationState`, `SignupDraft` encode the server's publish rules (province optional, photo required, verified email counts toward progress but not publish) and the `missing_fields` mapping.
- `SignupSetupRepository` implements D-04 (send only filled fields, update draft on success), D-05 (full publish payload, normalized country code), D-06 (blocked steps), D-09/D-10 (hydrate from profile + email status, no English seeded), D-14/D-25 (`listFieldFor`), and the persistent verification send state for plan 04-15.
- Reset on the `isLoggedIn` true to false transition (never on the initial value) and on explicit `reset()`; clears draft, hydration, send state and photo working files; a generation counter plus cancellation stops a late hydrate from repopulating the draft.
- `SignupTestHarness` signs in through `userManager.login` only (no dependency on plan 04-06's `signup`).

## Task Commits
1. Task 1: SetupStep, SignupDraft, strings - `9446613`
2. Task 2: SignupSetupRepository and SignupTestHarness - `085a5a7`

## Verification
- `SignupDraftTest` 8 tests and `SignupSetupRepositoryTest` 27 tests pass; `ktlintCheck` passes.
- Acceptance greps: no `userManager.signup`, one `userManager.login(`, one `setupCompleted = true`, one `createFormData("image"`, no Log/println in the repository.

## Deviations from Plan
- Tests and implementation for each task were written together and committed once per task, rather than as separate RED and GREEN commits. Tests were run against the final implementation.
- The `createFormData` call was kept on one line (value extracted to a local) so the literal acceptance grep matches after ktlint formatting.

None otherwise: no auto-fix rules triggered.

## Known Stubs
None.

## Threat Flags
None. T-04-18 to T-04-21 mitigations implemented as planned (reset on logout and after publish, `listFieldFor` guard, typed DTO only, no logging).

## Manual checks (deferred by project policy)
- On device: leave Verify Email and re-enter within 30 s and confirm resend stays disabled (consumed by plan 04-15).
- On device: log out then start a new signup and confirm no previous name, photo or email appears.

## Self-Check: PASSED
All seven created files exist; commits `9446613` and `085a5a7` exist; ktlintCheck passes.

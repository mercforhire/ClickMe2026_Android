---
phase: 03-login-password-recovery-realtime
plan: 01
subsystem: network
tags: [retrofit, moshi, public-endpoints, password-recovery, wave-0]
requires: []
provides:
  - ApiRequestUrls.FORGOT_PASSWORD / RESET_PASSWORD
  - ClickMeApi.forgotPassword / resetPassword
  - ForgotPasswordRequest, ResetPasswordRequest, SuccessMessageResponse
  - FakeClickMeApi.forgotPasswordHandler / resetPasswordHandler plus call counters
affects: [03-02, 03-03, password-recovery UI plans]
tech-stack:
  added: []
  patterns: [Moshi codegen DTOs, scriptable fake with call counters]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/SuccessMessageResponse.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/network/ApiRequestUrls.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/network/ClickMeApi.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/requests/AuthRequests.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/testing/FakeClickMeApi.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/network/ApiRequestUrlsTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/network/ClickMeApiTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/network/NetworkStackTest.kt
key-decisions:
  - "Forgot body key is `identity` (iOS and backend), reset uses password_confirmation via @Json"
  - "Recovery responses use a {status, message} envelope (SuccessMessageResponse), not SuccessDataResponse"
requirements-completed: []
duration: ~25 min
completed: 2026-10-20
---

# Phase 3 Plan 01: Password recovery network layer Summary

Public `auth/password/forgot` and `auth/password/reset` Retrofit endpoints with Moshi DTOs, a `{status, message}` response type, and FakeClickMeApi handlers, with MockWebServer tests pinning the JSON bodies and the absence of a bearer token.

## Tasks

| Task | Commits | Result |
|------|---------|--------|
| 1: endpoints, DTOs, fake handlers (TDD) | 1718cb5 (RED), 4335b41 (GREEN) | Constants, API methods, DTOs, fake extended; ApiRequestUrls/LoginViewModel/UserManager tests pass |
| 2: MockWebServer tests | 2bfd8ac | 3 ClickMeApiTest tests, 1 NetworkStackTest test, ApiRequestUrlsTest additions; ktlintCheck passes |

## Deviations from Plan

**1. [Rule 1 - Bug] ktlint spacing error in FakeClickMeApi**
- **Found during:** Task 2 ktlintCheck
- **Issue:** My Task 1 edit left `getMe():SuccessDataResponse` with no space after the colon.
- **Fix:** restored the space.
- **Files modified:** FakeClickMeApi.kt
- **Commit:** 2bfd8ac

**2. [Rule 3 - Blocking] NetworkStackTest dispatcher had no routes for the recovery paths**
- **Issue:** The MockWebServer dispatcher returned 404 for unknown paths, so the new no-bearer test needed `forgot` and `reset` routes.
- **Fix:** added both routes to the existing dispatcher in NetworkStackTest.
- **Commit:** 2bfd8ac

**3. Worktree base correction.** The worktree started at d1217fb, an ancestor of the expected base 32a8b88; it was clean and was reset to 32a8b88 per the startup check.

No production-code deviations.

## Requirements

AUTH-02 is only partially addressed (network layer). It is deliberately NOT marked complete and REQUIREMENTS.md is untouched; it completes when the recovery UI plans land.

## Deferred / Open Items

None for this plan. No human checkpoints.

## Known Stubs

None.

## Threat Flags

None. T-03-01 (no bearer on public endpoints) and T-03-04 (exact JSON body) are covered by NetworkStackTest and ClickMeApiTest.

## Self-Check: PASSED

All listed files exist; commits 1718cb5, 4335b41, 2bfd8ac present; gradle tests for ClickMeApiTest, NetworkStackTest, ApiRequestUrlsTest, LoginViewModelTest, UserManagerTest and ktlintCheck passed.

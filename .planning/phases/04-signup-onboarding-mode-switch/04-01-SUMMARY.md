---
phase: 04-signup-onboarding-mode-switch
plan: 01
subsystem: network
tags: [retrofit, moshi, dto, multipart, network]
requires: []
provides:
  - Signup, email-verification, expert-setup, meta and avatar DTOs
  - 9 new ClickMeApi endpoints and 8 ApiRequestUrls constants
  - ApiException.Http.missingFields from the top-level missing_fields field
  - FakeClickMeApi handlers, counters and setupProfileRequests
affects: [04-02, 04-03, 04-04, 04-05, 04-07]
tech-stack:
  added: []
  patterns:
    - "Partial PATCH body via nullable Moshi fields with no serializeNulls; empty list serializes as []"
    - "toString redaction on credential-bearing DTOs"
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/requests/SignupRequests.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/SignupData.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/SetupExpertProfileData.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/EmailStatusData.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/AvatarUploadData.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/MetaPayloads.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/ExpertProfileData.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/ApiErrorEnvelope.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/network/ApiException.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/network/ApiErrorParser.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/network/ApiRequestUrls.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/network/ClickMeApi.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/testing/FakeClickMeApi.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/network/ApiRequestUrlsTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/network/ClickMeApiTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/network/ResponseDecodingTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/network/ApiErrorParserTest.kt
key-decisions:
  - "No NetworkFactory change: absence of serializeNulls is what makes the PATCH omit unset keys, pinned by exact-body tests"
  - "ExpertProfileData keeps setupCompleted first and defaults every new field so existing call sites compile"
requirements-completed: [SIGN-01, SIGN-02, SIGN-03, SIGN-04, SIGN-05]
duration: ~25 min
completed: 2026-10-21
---

# Phase 4 Plan 01: Network contract for signup and onboarding Summary

Retrofit/Moshi contract for the whole of Phase 4: nine new endpoints (signup, email resend/verify/status, partial expert-profile PATCH, languages and expertise-tag lookups, multipart avatar upload and delete), the full `GET /expert/profile` DTO, top-level `missing_fields` error parsing, and scriptable FakeClickMeApi handlers.

## Tasks

| Task | Name | Commits |
| ---- | ---- | ------- |
| 1 | DTOs and top-level missing_fields error field | 1ffbb0c (RED tests), 5417276 (GREEN) |
| 2 | Endpoints, URL constants, fake handlers, wire-format tests | 597974d |

Task 2's tests and implementation went into one commit because the new tests cannot compile apart from the interface and the fake (RESEARCH Pitfall 8).

## Verification

- Full `:app:testDevDebugUnitTest` and `ktlintCheck` pass.
- Wire formats pinned by MockWebServer: signup body `{"email","password","role":"expert"}`; PATCH `{"first_name":"Ada"}`, `{"location":{"city":"Toronto"}}`, `{"expertise_tags":[]}`; multipart part `image` / `avatar.jpg` / `image/jpeg`.
- Acceptance counts: 16 `suspend fun` in ClickMeApi and 16 overrides in the fake, 0 `serializeNulls` in NetworkFactory, 4 `@JsonClass` in MetaPayloads.

## Deviations from Plan

None. The plan executed as written. Minor housekeeping: ran `ktlintFormat` to fix import order in the test files and removed redundant `!!` warnings in the new decoding test.

## Known Stubs

None.

## Threat Flags

None. All surface matches the plan's threat model (T-04-01..04 mitigated: typed DTO with null-omission, redacting `toString` on SignupRequest and SignupData, `/auth/signup` public while the other new paths need a bearer, optional `missing_fields`).

## Open manual checks

None for this plan (no UI). Real-backend signup and avatar calls are exercised by later plans' device testing, which is deferred by project policy.

## Self-Check: PASSED

All created files exist; commits 1ffbb0c, 5417276 and 597974d are on the branch.

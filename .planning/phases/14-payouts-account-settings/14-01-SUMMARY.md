---
phase: 14-payouts-account-settings
plan: 01
subsystem: network
tags: [retrofit, moshi, account-deletion, profile, faq, transactions, tax-profile, wave-0]

requires:
  - phase: 11-expert-earnings-payouts
    provides: PayoutSummaryData, PaginationMeta, MINOR-unit money conventions
provides:
  - Nine Phase 14 Retrofit endpoints on ClickMeApi (profile PATCH, change password, delete-request, delete account, meeting link, FAQs, earnings transactions, tax profile get/save)
  - Moshi request and response types, with PII-redacting toString on every secret-bearing type
  - FakeClickMeApi handlers and counters for all nine endpoints, and SettingsFixtures payload builders
affects: [14-02, 14-03, 14-04, 14-05, 14-06, payouts, account-settings, help]

actuals:
  tokens: 20000
  tasks: 3
  commits: 6

tech-stack:
  added: []
  patterns:
    - "DELETE with a JSON body via @HTTP(method = DELETE, hasBody = true)"
    - "Request types omit null properties (no serializeNulls); groups present means full replace for languages"
    - "toString redaction on password, deletion token, TIN, dob, legal name and account identifier types"

key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/requests/AccountRequests.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/requests/ProfileRequests.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/requests/TaxProfileRequests.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/DeletionRequestData.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/FaqData.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/EarningsTransactionsData.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/TaxProfileData.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/network/AccountApiTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/network/ProfileHelpApiTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/network/PayoutsTaxApiTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/testing/SettingsFixtures.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/network/ApiRequestUrls.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/network/ClickMeApi.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/ExpertFinancialData.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/ExpertProfileData.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/testing/FakeClickMeApi.kt

key-decisions:
  - "Account deletion DELETE uses @HTTP(hasBody = true); proven by a MockWebServer test asserting method, path and body"
  - "payout_status is a raw nullable String so settled_direct and unknown values both decode"
  - "TaxProfileAddress.regionOrProvince reads region with a province fallback (RESEARCH A9)"

patterns-established:
  - "Shared-file rule: all Phase 14 network, DTO and fake edits land in this plan so later plans never touch ClickMeApi.kt, ApiRequestUrls.kt or FakeClickMeApi.kt"

requirements-completed: [PROF-02, PROF-03, PROF-04, PROF-05, PAYOUT-01, PAYOUT-03, PAYOUT-04]

duration: 25min
completed: 2026-10-29
status: complete
plan_head_before: 466e755dd0e79f915ed6bf75f28b935266bf41b6
plan_head_after: c6ad4dc42a68b5d586458bb5822bd82018e95bb3
---

# Phase 14 Plan 01: Network Contract Summary

**Nine Phase 14 Retrofit endpoints with exact-wire MockWebServer tests, including an HTTP DELETE that carries a JSON body, plus FakeClickMeApi handlers and SettingsFixtures for every later plan.**

## Accomplishments

- Account deletion round-trips over the wire: `POST user/account/delete-request` returns a token, `DELETE user/account` sends `{deletion_token, reason_category, [feedback_details], confirm_permanent_action:true}` through `@HTTP(hasBody = true)`. Wrong password (401, no code), 409 `EXPERT_HAS_UPCOMING_BOOKINGS`, 400 `INVALID_TOKEN` and 429 all reach `ApiException.Http`.
- `PATCH user/password`, `PATCH user/profile` (null groups omitted, languages always a full replace, no email key), `PATCH expert/profile` meeting link (only `professional_details.meeting_url`), `GET help/faqs` (lenient lists).
- `GET expert/earnings/transactions` with page/limit, MINOR-unit Long amounts and nullable rows; `payout_method` on the payout summary; `GET/POST expert/tax-profile` with a strict body and `regionOrProvince`.
- `ExpertProfileData.professionalDetails.meetingUrl` added; a body without `professional_details` still decodes.
- FakeClickMeApi now scripts and counts all nine endpoints (76 handlers, baseline 67); `SettingsFixtures.kt` builds the payloads.

## Task Commits

TDD RED/GREEN per task:

1. Task 1 (tracer): RED `9e7ebd1`, GREEN `f55b1d1` (account deletion + change password, fake handlers)
2. Task 2: RED `3056f59`, GREEN `e627633` (profile PATCH, meeting link, FAQs)
3. Task 3: RED `9dae5c8`, GREEN `c6ad4dc` (ledger, payout method, tax profile, fixtures)

Tests: AccountApiTest 4, ProfileHelpApiTest 7, PayoutsTaxApiTest 6, all green; ExpertFinancialApiTest, ExpertDashboardViewModelTest, ResponseDecodingTest, ExpertApiTest, ApiRequestUrlsTest stay green. `ktlintCheck`, `assembleDevDebug` and `scripts/check-design-rules.sh` pass.

## Tracer gate

The tracer slice (Task 1) re-verified end to end by the same MockWebServer tests before expansion; the DELETE-with-body risk (RESEARCH A3) is resolved: Retrofit sends method DELETE with the JSON body.

## Deviations from Plan

None - plan executed exactly as written. Minor: `updatePassword` and its Fake handler were added in Task 1 as the plan directed; its wire tests live in `ProfileHelpApiTest` (Task 2).

## Known Stubs

None.

## Threat Flags

None. T-14-01 (toString redaction) is covered by `secretsNeverPrintFromToString` and `taxTypesRedactPii`; T-14-02 and T-14-03 by the exact-body and DELETE wire tests.

## Self-Check: PASSED

- All eleven created files and five modified files exist on disk.
- Commits 9e7ebd1, f55b1d1, 3056f59, e627633, 9dae5c8, c6ad4dc are ancestors of HEAD.

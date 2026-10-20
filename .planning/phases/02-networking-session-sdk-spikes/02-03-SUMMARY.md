---
phase: 02-networking-session-sdk-spikes
plan: 03
subsystem: networking
tags: [errors, error-codes, auth-paths, tdd, moshi, retrofit]

requires:
  - phase: 02-networking-session-sdk-spikes
    provides: Moshi codegen, mockwebserver3 and MainDispatcherRule wiring (plan 02-01)
provides:
  - ErrorCode enum with the 65 iOS cases, fromWire and isTokenError
  - Lenient ApiErrorEnvelope/FieldErrorDto DTO covering all three backend error shapes
  - Sealed ApiException (Http, Network, Decoding) with userMessage
  - ApiErrorParser (parseEnvelope, codeOf, httpError, from) and apiCall wrapper
  - ApiRequestUrls path constants and needAuthToken (encoded-path allowlist)
affects: [02-06, 02-07, 02-11, 02-13]

tech-stack:
  added: []
  patterns: [Lenient error parsing via runCatching, string code kept raw alongside nullable enum, encoded-path auth allowlist]

key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/enums/ErrorCode.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/ApiErrorEnvelope.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/network/ApiException.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/network/ApiErrorParser.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/network/ApiCall.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/network/ApiRequestUrls.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/network/ApiErrorParserTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/network/ApiRequestUrlsTest.kt
  modified: []

key-decisions:
  - "Http exception message is 'HTTP <status> <rawCode>' only, so logs never carry server payloads (T-02-08)"
  - "ApiErrorParser owns a private Moshi so it stays independent of the shared MoshiProvider (parallel with 02-02)"
  - "Non-IO, non-HTTP throwables map to ApiException.Decoding rather than leaking"

patterns-established:
  - "Unknown backend codes: ErrorCode is null, rawCode retained, userMessage still shown"
  - "Every network call goes through apiCall; CancellationException is always rethrown"

requirements-completed: [FND-04, FND-05]

duration: n/a
completed: 2026-10-20
---

# Phase 2 Plan 03: Error Handling and Auth Paths Summary

**65-case ErrorCode, lenient error envelope parsing into a sealed ApiException, an apiCall wrapper, and the five-path bearer allowlist (ApiRequestUrls.needAuthToken) tested on the encoded path.**

## Accomplishments

- ErrorCode ports all 65 iOS cases in iOS order with identical wire strings; the test lists the 65 strings verbatim and asserts order, uniqueness and round trip. Unknown codes map to null and the raw string is kept on `ApiException.Http.rawCode`.
- One `ApiErrorEnvelope` DTO decodes the standard, field-error (`errors[]`) and status-only shapes plus the optional `detail` map (PAYMENT_REQUIRES_ACTION `stripe_account`).
- HTML, blank, null and wrongly shaped (`[1,2,3]`) bodies parse to an Http error with null code and never throw (T-02-06).
- `ApiErrorParser.from` maps HttpException, IOException (Network), everything else (Decoding) and passes ApiException through unchanged; `apiCall` rethrows CancellationException.
- `needAuthToken` allows exactly five public paths and tests the encoded path, so `?next=1` cannot flip the answer; `/auth/email/*` stays authed (T-02-07).

## Task Commits

1. Task 1 RED: failing ErrorCode/ApiErrorParser tests - `e99fa41` (test)
2. Task 1 GREEN: ErrorCode, envelope, ApiException, ApiErrorParser, apiCall - `47f265c` (feat)
3. Task 2 RED: failing ApiRequestUrls tests - `52f08b3` (test)
4. Task 2 GREEN: ApiRequestUrls constants and needAuthToken - `4d6e4de` (feat)

## Verification

- `./gradlew :app:testDevDebugUnitTest --tests '*ApiRequestUrlsTest' --tests '*ApiErrorParserTest' ktlintCheck` passes: ApiErrorParserTest 21 tests, ApiRequestUrlsTest 5 tests (18 assertions over the path table), 0 failures; ktlintCheck clean.
- Acceptance greps: 65 ErrorCode cases, 0 `EnumJsonAdapter` code references, 0 `"/auth/email` literals, 6 `const val`.

## Deviations from Plan

None to behavior. Two minor notes:
- ktlint required re-wrapping of test argument lists and two over-long lines; `ktlintFormat` plus manual line splits fixed it before the GREEN commit (no semantic change).
- The Task 2 RED commit does not compile (as expected for RED, the class under test does not exist yet).

## Known Stubs

None.

## Threat Flags

None. T-02-06, T-02-07 and T-02-08 are mitigated and covered by tests.

## Self-Check: PASSED

- All eight plan files exist; commits e99fa41, 47f265c, 52f08b3, 4d6e4de are in git history.

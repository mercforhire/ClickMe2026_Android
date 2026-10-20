---
phase: 02-networking-session-sdk-spikes
plan: 07
subsystem: networking
tags: [retrofit, datastore, hilt, session-storage, tdd]

requires:
  - phase: 02-networking-session-sdk-spikes
    provides: DTOs and MoshiProvider (02-02), ApiRequestUrls and ApiErrorParser (02-03)
provides:
  - ClickMeApi Retrofit interface for login, me, user/profile, expert/profile, expert/topics
  - SessionStore (Preferences DataStore) for cached profile, current mode, onboarding flag
  - IoDispatcher / ApplicationScope qualifiers and SessionModule Hilt bindings
  - testPreferencesDataStore test helper
affects: [02-10, 02-11, 02-13]

tech-stack:
  added: []
  patterns: [DataStore with ReplaceFileCorruptionHandler, nullable storedMode() to detect a saved preference, Retrofit tested against MockWebServer]

key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/network/ClickMeApi.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/SessionStore.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/di/Qualifiers.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/di/SessionModule.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/testing/TestDataStores.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/network/ClickMeApiTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/managers/SessionStoreTest.kt
  modified: []

key-decisions:
  - "Refresh is deliberately absent from ClickMeApi; the bare-client TokenRefresher owns it"
  - "SessionStore.clear() uses a single dataStore.edit { clear() } so profile, mode and onboarding vanish atomically"
  - "SessionStore is @Singleton with @Inject constructor so UserManager can inject it directly"

patterns-established:
  - "Retrofit contract tests: real Retrofit + MoshiConverterFactory against mockwebserver3, asserting method, encoded path, Content-Type and body"
  - "DataStore tests use TemporaryFolder + backgroundScope via testPreferencesDataStore"

requirements-completed: [FND-03, FND-07]

duration: n/a
completed: 2026-10-20
---

# Phase 2 Plan 07: ClickMeApi and SessionStore Summary

**Typed Retrofit surface for the five Phase 2 endpoints plus a DataStore-backed SessionStore (cached profile, mode, onboarding) with Hilt bindings for DataStore, IO dispatcher and application scope.**

## Accomplishments

- `ClickMeApi` declares exactly five suspend functions using `ApiRequestUrls` constants (relative paths under `/api/v1/`), decoding through `SuccessDataResponse<T>`.
- `ClickMeApiTest` (6 tests) asserts method, encoded path, JSON Content-Type and body for login, and decoding for me, user/profile, expert/profile and expert/topics; a 422 surfaces as `HttpException` that `ApiErrorParser.from` maps to `ApiException.Http(422)` with the field error.
- `SessionStore` stores `current_mode` (wire value), `has_seen_onboarding` and `cached_profile_json` (full `UserProfileData` via Moshi). `storedMode()` returns null when unset or unknown; undecodable profile JSON returns null; `clear()` is one edit.
- `SessionModule` provides a singleton DataStore (file `clickme_session`, corruption handler resets to empty), `@IoDispatcher` and a singleton `@ApplicationScope`.

## Task Commits

1. Task 1 RED: `f1ee3ca` (test) - ClickMeApi contract tests
2. Task 1 GREEN: `4cc6b6a` (feat) - ClickMeApi interface
3. Task 2 RED: `9d1397b` (test) - SessionStore tests and DataStore test helper
4. Task 2 GREEN: `e9a4ffe` (feat) - SessionStore, qualifiers, SessionModule

## Verification

- `./gradlew :app:testDevDebugUnitTest --tests '*SessionStoreTest' --tests '*ClickMeApiTest' ktlintCheck assembleDevDebug` exits 0: ClickMeApiTest 6/6, SessionStoreTest 7/7, ktlint clean, Hilt processes the new module.
- Acceptance greps: 5 `suspend fun`, 5 `@GET/@POST(ApiRequestUrls.`, 0 non-comment `refresh` in ClickMeApi; each of the three DataStore key strings appears once in SessionStore, 0 token keys, 0 `pricingLimits`; one `@InstallIn(SingletonComponent::class)`.

## Deviations from Plan

None to behavior. Note: the worktree started at an older commit than the wave base `346dfeb`; it was reset to it per startup instructions (tree was clean, HEAD was an ancestor).

## Issues Encountered

None. The RED commits were made without a separate failing-run (the missing `ClickMeApi` / `SessionStore` types make them fail to compile by construction).

## Known Stubs

None.

## Threat Flags

None. T-02-24 (no tokens or pricing limits stored; single-edit clear), T-02-25 (ReplaceFileCorruptionHandler, null on garbage JSON) and T-02-26 (method/path/body assertions) are mitigated and tested.

## Self-Check: PASSED

- All seven plan files exist; commits f1ee3ca, 4cc6b6a, 9d1397b, e9a4ffe exist in history.

---
phase: 05-explore-expert-profile
plan: 04
subsystem: cache
tags: [datastore, ttl, categories, hilt, moshi]
requires:
  - phase: 05-explore-expert-profile
    provides: "05-01 ClickMeApi.getCategories, CategoryItem, FakeClickMeApi.categoriesCalls; 05-02 wall java.time.Clock (ClockModule), MutableClock"
provides:
  - "managers.CategoryStore: fetch() (7-day TTL) and refresh() (always network), single-flight"
  - "di.CategoryCache qualifier and CategoryCacheModule (clickme_categories DataStore)"
  - "testPreferencesDataStore(scope, folder, fileName) for multi-store tests"
affects: [05-08]
tech-stack:
  added: []
  patterns:
    - "Second qualified Preferences DataStore file for data that must survive logout"
    - "Injected wall Clock for persisted TTL; negative age is stale"
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/CategoryStore.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/di/CategoryCacheModule.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/managers/CategoryStoreTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/di/Qualifiers.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/testing/TestDataStores.kt
key-decisions:
  - "Cache is not registered with SessionDataCleaner and lives in its own file, so SessionStore.clear() cannot wipe it (D-08)"
  - "ApiException is not caught in the store; the sheet ViewModel (05-08) maps it to Retry (D-06)"
requirements-completed: [DISC-02]
metrics:
  tasks: 2
  completed: 2026-10-21
---

# Phase 5 Plan 04: CategoryStore Summary

Port of iOS `CategoryStore` as a Hilt singleton: `GET /categories` behind a 7-day cache (memory, then DataStore, then network) in its own logout-proof `clickme_categories` DataStore file.

## Tasks

| Task | Name | Commit |
| ---- | ---- | ------ |
| 1 | CategoryStore with TTL, single-flight, failure safety, tests | 4151007 |
| 2 | CategoryCacheModule providing the qualified DataStore | 111d8d7 |

## Verification

- `CategoryStoreTest`: 11 tests, 0 failures (first fetch, no-network within 6d23h from memory and from a new instance over disk, refetch at TTL+1ms, refresh always fetches, failed refresh keeps old cache and throws `ApiException.Network`, failed first fetch caches nothing, negative age stale, corrupt JSON miss, concurrent single-flight, survives `SessionStore.clear()`). `SessionStoreTest` still passes.
- `:app:assembleDevDebug` and `ktlintCheck` pass (no Hilt DuplicateBindings).
- Acceptance greps: TTL_MS literal 1, `@CategoryCache` in CategoryStore 1, `CategoryStore.FILE_NAME` in module 1, SessionModule unchanged, no MonotonicClock/elapsedRealtime in CategoryStore, no CategoryStore reference in SessionDataCleaner or UserManager.

## Deviations from Plan

None in behavior. Process note: Task 1's tests and implementation were committed together (no separate RED commit) because the test source set cannot compile without `CategoryStore`. The `testPreferencesDataStore` change is backward compatible.

## Known Stubs

None.

## Threat Flags

None. T-05-11 (typed decode, corrupt JSON is a miss), T-05-13 (negative age stale, wall clock) and T-05-14 (Mutex single-flight) are mitigated and tested.

## Self-Check: PASSED

CategoryStore.kt, CategoryCacheModule.kt, CategoryStoreTest.kt exist; commits 4151007 and 111d8d7 are on the branch.

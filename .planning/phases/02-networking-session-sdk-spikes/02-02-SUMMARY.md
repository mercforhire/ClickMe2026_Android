---
phase: 02-networking-session-sdk-spikes
plan: 02
subsystem: networking
tags: [moshi, json, dto, dates, tdd]

requires:
  - phase: 02-networking-session-sdk-spikes
    provides: Moshi codegen (ksp) wiring and MainDispatcherRule from plan 02-01
provides:
  - MoshiProvider (codegen-only Moshi) and InstantAdapter (ISO-8601 to java.time.Instant)
  - UserRole / AppMode enums with reflection-free fromWire lookup
  - Auth request bodies and SuccessDataResponse envelope
  - AuthUser, LoginData, RefreshTokenData, MeData (+PricingLimits, MinSessionAmount), UserProfileData, ExpertProfileData, MyExpertTopicsPayload, PaginationMeta
affects: [02-03, 02-04, 02-05, 02-06, 02-07, 02-08]

tech-stack:
  added: []
  patterns: [Moshi codegen with explicit @Json names, raw-string roles with fromWire lookup, computed get() properties ignored by codegen]

key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/network/InstantAdapter.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/network/MoshiProvider.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/enums/UserRole.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/enums/AppMode.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/requests/AuthRequests.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/SuccessDataResponse.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/PaginationMeta.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/AuthUser.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/LoginData.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/RefreshTokenData.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/MeData.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/UserProfileData.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/ExpertProfileData.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/MyExpertTopicsPayload.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/network/InstantAdapterTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/network/ResponseDecodingTest.kt
  modified: []

key-decisions:
  - "UserProfileData is a full iOS mirror (resolved Q1) so it can be cached as the offline profile"
  - "Roles stay raw strings on DTOs; typed access via userRoles / UserRole.fromWire (R8-safe, unknown roles never fail)"
  - "expires_at / expires_in decode as Long; ids are String (no UUID adapter)"

patterns-established:
  - "DTOs: @JsonClass(generateAdapter = true) plus @Json(name=) on every multi-word field; no KotlinJsonAdapterFactory"
  - "Computed get() properties on DTOs (no backing field) are ignored by codegen"

requirements-completed: [FND-03]

duration: n/a
completed: 2026-10-20
---

# Phase 2 Plan 02: Moshi JSON Layer Summary

**Codegen-only Moshi layer with a java.time ISO-8601 Instant adapter and the auth, /me, /user/profile, expert-lookup and pagination DTOs, verified by 28 literal-JSON tests.**

## Performance

- **Tasks:** 2 of 2 (both TDD, RED then GREEN)
- **Tests:** InstantAdapterTest 9, ResponseDecodingTest 19, all passing; `ktlintCheck` passes

## Accomplishments

- InstantAdapter uses `OffsetDateTime.parse(raw).toInstant()`: handles micro/milli/no/4-digit/9-digit fractions with `Z` or numeric offsets, rejects space-separated and offset-less strings. The iOS regex truncation is not ported.
- All 17 DTO classes carry `@JsonClass(generateAdapter = true)`; no `KotlinJsonAdapterFactory` or `EnumJsonAdapter` in `app/src/main` (greps return 0).
- `UserProfileData` round-trips through Moshi (needed for the DataStore cache); nested fields all nullable, `languages` defaults to empty.
- `MeData.pricingLimits` is decode-only (KDoc forbids persistence), `PaginationMeta.hasMoreOrDerived` falls back to `currentPage < totalPages`.

## Task Commits

1. **Task 1 RED:** `2d4207b` (test) - failing adapter and auth DTO tests
2. **Task 1 GREEN:** `648cc5b` (feat) - provider, adapter, enums, envelope, auth DTOs
3. **Task 2 RED:** `2c91d40` (test) - failing /me, profile, expert, pagination tests
4. **Task 2 GREEN:** `6520ddf` (feat) - remaining DTOs; includes ktlintFormat adjustments

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] ktlint formatting violations**
- **Found during:** Task 2 verification (`ktlintCheck`)
- **Issue:** Multiline chained calls in ResponseDecodingTest, and enum constructor parameter style in UserRole/AppMode, failed ktlint.
- **Fix:** Ran `ktlintFormat`; behavior unchanged.
- **Files modified:** ResponseDecodingTest.kt, UserRole.kt, AppMode.kt
- **Commit:** 6520ddf

**2. [Note] Worktree base reset**
The worktree started at an older commit than the required base `464746e`; reset to it per the startup instructions before any work (tree was clean).

Otherwise the plan was executed as written. Test counts exceed the acceptance minimums (9 >= 6, 19 >= 14).

## Issues Encountered

None. The benign Moshi codegen kapt deprecation warning (RESEARCH Pitfall 11) appears and was not chased.

## Known Stubs

None. `MyExpertTopicsPayload.topics` is intentionally untyped (`Map<String, Any?>`); Phase 10 replaces it with `ExpertTopicItem`, and `ExpertProfileData` is intentionally partial until Phase 4.

## Threat Flags

None. T-02-03 (unknown roles, extra keys, all-null profile covered by tests), T-02-04 (no reflection factories) and T-02-05 (pricingLimits not persisted) are mitigated as planned. R8 release smoke remains in plan 02-15.

## Next Phase Readiness

Plans 02-03 onward can decode through `MoshiProvider.moshi` and use these DTOs for the Retrofit API, session store and launch logic.

## Self-Check: PASSED

- All 16 listed files exist; commits 2d4207b, 648cc5b, 2c91d40, 6520ddf exist in history.

---
*Phase: 02-networking-session-sdk-spikes*
*Completed: 2026-10-20*

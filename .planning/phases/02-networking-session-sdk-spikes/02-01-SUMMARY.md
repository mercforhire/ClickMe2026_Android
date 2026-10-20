---
phase: 02-networking-session-sdk-spikes
plan: 01
subsystem: infra
tags: [gradle, dependencies, supply-chain, moshi, datastore, security-crypto, mockwebserver, spike-inputs]

requires:
  - phase: 01-foundation
    provides: Gradle project, version catalog, dev/prod flavors, ktlint, Hilt/KSP wiring
provides:
  - Version catalog entries for the six new libraries (stripe-paymentsheet and agora-voice-sdk catalog-only, for the spike modules)
  - ":app wiring for Moshi codegen (ksp), DataStore Preferences, security-crypto and test-only mockwebserver3"
  - "versionName 0.1.0 / versionCode 1 (D-17)"
  - android.permission.INTERNET in the app manifest
  - MainDispatcherRule test helper for ViewModel tests
affects: [02-02, 02-03, 02-04, 02-05, 02-08, 02-09, 02-12]

tech-stack:
  added: [moshi-kotlin-codegen 1.15.2, datastore-preferences 1.2.1, security-crypto 1.1.0, mockwebserver3 5.5.0, stripe paymentsheet 23.24.0 (catalog only), agora voice-sdk 4.6.4 (catalog only)]
  patterns: [Moshi codegen via ksp, MainDispatcherRule shared TestDispatcher for runTest]

key-files:
  created:
    - app/src/test/java/com/uptrendinvestments/clickmeapp/testing/MainDispatcherRule.kt
  modified:
    - gradle/libs.versions.toml
    - app/build.gradle.kts
    - app/src/main/AndroidManifest.xml

key-decisions:
  - "Stripe and Agora stay out of :app (D-10); they are catalog entries consumed only by the spike modules"
  - "Task 3 (spike inputs file) deferred by the user; tracked as a pending item in STATE.md"

patterns-established:
  - "Test coroutine dispatching: MainDispatcherRule(testDispatcher) paired with runTest(rule.testDispatcher)"

requirements-completed: []
requirements-enabled: [FND-07, FND-13, FND-14, REL-03]  # build wiring only; not marked complete in REQUIREMENTS.md, later plans deliver them

duration: n/a
completed: 2026-10-20
---

# Phase 2 Plan 01: Build Wiring Summary

**Six first-party Maven coordinates human-approved and wired into the catalog and :app (Moshi codegen, DataStore, security-crypto, MockWebServer3), version 0.1.0, INTERNET permission and a shared MainDispatcherRule; the spike inputs file is deferred by the user.**

## Performance

- **Tasks:** 2 of 3 executed; Task 3 deferred by user decision
- **Completed:** 2026-10-20

## Accomplishments

- Package legitimacy gate passed: the user replied "approved" for all six coordinates as pinned (paymentsheet 23.24.0, voice-sdk 4.6.4, moshi-kotlin-codegen 1.15.2, mockwebserver3 5.5.0, security-crypto 1.1.0, datastore-preferences 1.2.1). This resolves the RESEARCH [ASSUMED] status (T-02-SC).
- Catalog now carries all six libraries; `:app` adds `ksp(libs.moshi.kotlin.codegen)`, DataStore, security-crypto and test-only mockwebserver3. Stripe and Agora are not on `:app` (D-10).
- `versionName = "0.1.0"`, `versionCode = 1` (D-17); `android.permission.INTERNET` declared.
- `MainDispatcherRule` added to the test source set.
- Verification green: `ktlintCheck`, `assembleDevDebug` and `testDevDebugUnitTest` all pass.

## Task Commits

1. **Task 1: Package legitimacy gate** - no commit (human-verify; user approved all six coordinates)
2. **Task 2: Catalog entries, :app dependencies, version 0.1.0, INTERNET permission, MainDispatcherRule** - `0829823` (chore)
3. **Task 3: Collect spike inputs into ~/.clickme/spike-inputs.properties** - DEFERRED, no commit

## Files Created/Modified

- `gradle/libs.versions.toml` - four new versions, six new libraries
- `app/build.gradle.kts` - ksp Moshi codegen, DataStore, security-crypto, mockwebserver3, version 0.1.0
- `app/src/main/AndroidManifest.xml` - INTERNET permission
- `app/src/test/java/com/uptrendinvestments/clickmeapp/testing/MainDispatcherRule.kt` - JUnit4 TestWatcher for `Dispatchers.setMain/resetMain`

## Decisions Made

- Followed the plan as written; Stripe and Agora kept out of `:app` per D-10.
- Task 3 deferred (see below).

## Deviations from Plan

### Deferred by user

**Task 3 (checkpoint:human-action) - spike inputs file DEFERRED (user decision 2026-10-20).** The user said: "defer manual testing for now, continue with the execution." `~/.clickme/` exists but is empty; no properties file was created and none of the eight `spike.*` keys exist yet.

- The user plans to generate the test accounts and the booking through the backend agent rather than through iOS.
- An iOS device or simulator peer (logged in as the expert) is still needed live for the Agora spike run (plan 02-12).
- The file is REQUIRED before plan 02-09 (Stripe device run) and plan 02-12 (Agora device run). Key contract is unchanged: `spike.client.email`, `spike.stripe.expert_id`, `spike.stripe.topic_id`, `spike.stripe.selected_date`, `spike.stripe.time_slot`, `spike.stripe.start_time`, `spike.stripe.duration_minutes`, `spike.agora.booking_id`. No password, token or card number goes in the file.
- Plan 02-01 is marked complete for tracking since all code tasks are done; the open human action is tracked as a pending item in STATE.md.

None otherwise - Tasks 1 and 2 executed as written.

## Issues Encountered

None. The benign Moshi codegen kapt deprecation warning (RESEARCH Pitfall 11) was not chased.

## Known Stubs

None.

## Threat Flags

None. T-02-SC mitigated by the Task 1 gate. T-02-01 is not yet exercised (no inputs file exists).

## User Setup Required

Before plans 02-09 and 02-12: create `~/.clickme/spike-inputs.properties` with the eight keys listed above (see Task 3 in 02-01-PLAN.md for how to pick each value).

## Next Phase Readiness

Plans 02-02 onward can compile against Moshi codegen, DataStore, security-crypto, MockWebServer3 and MainDispatcherRule. The spike device runs (02-09, 02-12) are gated on the deferred inputs file.

## Self-Check: PASSED

- 0829823 exists in git history; the four plan files exist.

---
*Phase: 02-networking-session-sdk-spikes*
*Completed: 2026-10-20*

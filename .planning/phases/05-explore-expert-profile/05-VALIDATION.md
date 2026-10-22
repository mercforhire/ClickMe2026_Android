---
phase: 5
slug: explore-expert-profile
status: complete
nyquist_compliant: true
wave_0_complete: true
created: 2026-10-21
---

# Phase 5 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4.13.2 + Robolectric 4.17 + kotlinx-coroutines-test 1.11.0 + mockwebserver3 5.5.0 + Compose UI test (`createComposeRule` under `RobolectricTestRunner`) |
| **Config file** | `app/src/test/resources/robolectric.properties`; `unitTests.isIncludeAndroidResources = true` in `app/build.gradle.kts` |
| **Quick run command** | `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDevDebugUnitTest --tests '<pattern>'` |
| **Full suite command** | `./gradlew ktlintCheck lintDevDebug lintProdDebug testDevDebugUnitTest testProdDebugUnitTest assembleDevDebug assembleProdDebug assembleProdRelease && bash scripts/check-design-rules.sh` |
| **Estimated runtime** | ~30 s targeted; several minutes full |

---

## Sampling Rate

- **After every task commit:** Run the targeted `--tests` pattern for the files touched
- **After every plan wave:** Run `./gradlew testDevDebugUnitTest ktlintCheck && bash scripts/check-design-rules.sh`
- **Before `/gsd-verify-work`:** Full suite must be green on both flavors, including `assembleProdRelease`
- **Max feedback latency:** ~60 seconds

---

## Per-Task Verification Map

Filled at planning time (one row per plan task; tests are written first inside each `tdd="true"` task). 05-11 sets the final statuses.

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 05-01-T1 | 05-01 | 1 | DISC-01/02/03 | T-05-01 | Lenient decoding of optional fields | unit | `./gradlew :app:testDevDebugUnitTest --tests '*ResponseDecodingTest'` | ✅ extended | ✅ green |
| 05-01-T2 | 05-01 | 1 | DISC-01/02/03 | T-05-02, T-05-03 | Ids/slugs encoded; no new public path | MockWebServer + unit | `./gradlew :app:testDevDebugUnitTest --tests '*ApiRequestUrlsTest' --tests '*ClickMeApiTest'` | ✅ extended | ✅ green |
| 05-02-T1 | 05-02 | 1 | DISC-01, DISC-03 | T-05-06, T-05-07 | Unknown currency safe; Free first | unit + Compose | `./gradlew :app:testDevDebugUnitTest --tests '*MoneyTest' --tests '*PriceTextTest'` | ✅ | ✅ green |
| 05-02-T2 | 05-02 | 1 | DISC-01 | T-05-08 | Unknown slug -> fallback icon | unit | `./gradlew :app:testDevDebugUnitTest --tests '*CategoryIconMapTest'` | ✅ | ✅ green |
| 05-03-T1 | 05-03 | 1 | DISC-01, DISC-03 | T-05-09 | Server text as plain Text | Compose | `./gradlew :app:testDevDebugUnitTest --tests '*Phase5ComponentsTest'` | ✅ | ✅ green |
| 05-03-T2 | 05-03 | 1 | DISC-02 | — | — | Compose | `./gradlew :app:testDevDebugUnitTest --tests '*PickerComponentsTest'` | ✅ extended | ✅ green |
| 05-04-T1 | 05-04 | 2 | DISC-02 | T-05-11, T-05-13, T-05-14 | Corrupt cache = miss; negative age stale; single-flight | unit (real DataStore) | `./gradlew :app:testDevDebugUnitTest --tests '*CategoryStoreTest'` | ✅ | ✅ green |
| 05-04-T2 | 05-04 | 2 | DISC-02 | T-05-12 | Own logout-proof file, public data only | build | `./gradlew :app:assembleDevDebug :app:testDevDebugUnitTest --tests '*CategoryStoreTest'` | ✅ | ✅ green |
| 05-05-T1 | 05-05 | 2 | DISC-01 | T-05-18 | Caller-scoped bookings only | unit | `./gradlew :app:testDevDebugUnitTest --tests '*TodaysSessionsFilterTest'` | ✅ | ✅ green |
| 05-05-T2 | 05-05 | 2 | DISC-01 | T-05-15, T-05-16, T-05-17 | Server slugs only; latest wins; no logging | unit (fake API) | `./gradlew :app:testDevDebugUnitTest --tests '*ExploreViewModelTest'` | ✅ | ✅ green |
| 05-06-T1 | 05-06 | 2 | DISC-03 | T-05-19, T-05-20, T-05-21 | 404 -> Unavailable; independent loads | unit (fake API) | `./gradlew :app:testDevDebugUnitTest --tests '*ExpertProfileViewModelTest'` | ✅ | ✅ green |
| 05-06-T2 | 05-06 | 2 | DISC-03 | — | — | Compose | `./gradlew :app:testDevDebugUnitTest --tests '*MakeABookingStubScreenTest'` | ✅ | ✅ green |
| 05-07-T1 | 05-07 | 3 | DISC-01 | T-05-23, T-05-25 | Join Call always disabled | Compose | `./gradlew :app:testDevDebugUnitTest --tests '*ExploreSectionsTest'` | ✅ | ✅ green |
| 05-07-T2 | 05-07 | 3 | DISC-01 | T-05-23, T-05-24 | Plain Text; avatars via Coil | Compose | `./gradlew :app:testDevDebugUnitTest --tests '*ExploreSectionsTest'` | ✅ | ✅ green |
| 05-08-T1 | 05-08 | 3 | DISC-02 | T-05-26 | Search is local only | unit | `./gradlew :app:testDevDebugUnitTest --tests '*CategoriesSheetViewModelTest'` | ✅ | ✅ green |
| 05-08-T2 | 05-08 | 3 | DISC-02 | T-05-27 | Plain Text rows | Compose | `./gradlew :app:testDevDebugUnitTest --tests '*AllCategoriesSheetTest'` | ✅ | ✅ green |
| 05-09-T1 | 05-09 | 3 | DISC-03 | T-05-31 | Book disabled when not bookable | Compose | `./gradlew :app:testDevDebugUnitTest --tests '*ExpertProfileScreenTest'` | ✅ | ✅ green |
| 05-09-T2 | 05-09 | 3 | DISC-03 | T-05-29, T-05-30 | Unavailable hides all profile data | Compose | `./gradlew :app:testDevDebugUnitTest --tests '*ExpertProfileScreenTest'` | ✅ | ✅ green |
| 05-10-T1 | 05-10 | 4 | DISC-01, DISC-02, DISC-03 | — | — | Compose | `./gradlew :app:testDevDebugUnitTest --tests '*ExploreScreenTest'` | ✅ | ✅ green |
| 05-10-T2 | 05-10 | 4 | DISC-01, DISC-03 | T-05-32, T-05-33, T-05-34 | Keys carry no secrets; R8 build | Compose + build | `./gradlew :app:testDevDebugUnitTest --tests '*HomeShellsTest'` + `assembleProdRelease` | ✅ extended | ✅ green |
| 05-11-T1 | 05-11 | 5 | all | T-05-35, T-05-36, T-05-37 | Full gate + deferred-control sweeps | full suite | Full suite command (above) | n/a | ✅ green |
| 05-11-T2 | 05-11 | 5 | all | — | — | doc check | `grep -c '\[Phase 5\] DEFERRED (human)' .planning/STATE.md` | n/a | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [x] `FakeClickMeApi` — `clientHomeHandler`, `clientBookingsHandler`, `categoriesHandler`, `expertDetailsHandler`, `expertReviewsHandler` with call counters (`categoriesCalls`) and last `category` arg
- [x] JSON fixtures — home with/without `featured_topics`, free topic, details with `is_bookable` true/false, reviews with null `overall_rating`, bookings in several statuses
- [x] `MutableClock : java.time.Clock` test helper + wall-clock `Clock` provider in `ClockModule`
- [x] `HomeShellsTest` stays green via default-valued shell slots
- [x] New test files: `ExploreViewModelTest`, `ExploreScreenTest`, `TodaysSessionsFilterTest`, `CategoryIconMapTest`, `CategoryStoreTest`, `CategoriesSheetViewModelTest`, `AllCategoriesSheetTest`, `ExpertProfileViewModelTest`, `ExpertProfileScreenTest`, `MakeABookingStubScreenTest`, `MoneyTest`, `Phase5ComponentsTest`

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Live `featured_topics` carries `is_free` | DISC-01 | Needs live backend response | Log in as client on dev, open Explore, confirm free topics show "Free" (deferred (non-blocking), see STATE.md SC-1) |
| Explore & Expert Profile visual fidelity vs iOS | DISC-01, DISC-03 | Visual check on device | Compare side-by-side on Samsung test device (deferred (non-blocking) per project preference, see STATE.md) |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency < 60s
- [x] `nyquist_compliant` flag set to true in frontmatter

**Gate evidence (05-11):** 1056 tests per flavor, 0 failures (Phase 4 baseline 876); ktlint, lint, assembleDevDebug/ProdDebug/ProdRelease (R8) and design rules green.

**Approval:** approved 2026-10-21 (automated gate green; device checks deferred, non-blocking)

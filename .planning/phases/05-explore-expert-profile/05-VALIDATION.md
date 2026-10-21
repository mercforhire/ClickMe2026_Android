---
phase: 5
slug: explore-expert-profile
status: draft
nyquist_compliant: false
wave_0_complete: false
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

To be filled by the planner/executor from the Phase Requirements → Test Map in `05-RESEARCH.md` (Validation Architecture section).

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 5-xx-xx | — | — | DISC-01/02/03 | — | — | unit / Compose | see RESEARCH.md map | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `FakeClickMeApi` — `clientHomeHandler`, `clientBookingsHandler`, `categoriesHandler`, `expertDetailsHandler`, `expertReviewsHandler` with call counters (`categoriesCalls`) and last `category` arg
- [ ] JSON fixtures — home with/without `featured_topics`, free topic, details with `is_bookable` true/false, reviews with null `overall_rating`, bookings in several statuses
- [ ] `MutableClock : java.time.Clock` test helper + wall-clock `Clock` provider in `ClockModule`
- [ ] `HomeShellsTest` stays green via default-valued shell slots
- [ ] New test files: `ExploreViewModelTest`, `ExploreScreenTest`, `TodaysSessionsFilterTest`, `CategoryIconMapTest`, `CategoryStoreTest`, `CategoriesSheetViewModelTest`, `AllCategoriesSheetTest`, `ExpertProfileViewModelTest`, `ExpertProfileScreenTest`, `MakeABookingStubScreenTest`, `MoneyTest`, `Phase5ComponentsTest`

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Live `featured_topics` carries `is_free` | DISC-01 | Needs live backend response | Log in as client on dev, open Explore, confirm free topics show "Free" |
| Explore & Expert Profile visual fidelity vs iOS | DISC-01, DISC-03 | Visual check on device | Compare side-by-side on Samsung test device (deferred per project preference) |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 60s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending

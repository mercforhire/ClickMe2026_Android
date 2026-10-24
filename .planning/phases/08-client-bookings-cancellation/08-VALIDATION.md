---
phase: 8
slug: client-bookings-cancellation
status: approved
nyquist_compliant: true
wave_0_complete: true
created: 2026-10-23
---

# Phase 8 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4.13.2 + Robolectric 4.17 + kotlinx-coroutines-test 1.11.0 + Compose `ui-test-junit4` + okhttp-mockwebserver3 |
| **Config file** | none — `app/build.gradle.kts` `testOptions.unitTests.isIncludeAndroidResources = true`; fakes in `app/src/test/.../testing/` |
| **Quick run command** | `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDevDebugUnitTest --tests '<pattern>'` |
| **Full suite command** | `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDevDebugUnitTest ktlintCheck assembleDevDebug && bash scripts/check-design-rules.sh` (final wave adds `lintDevDebug lintProdDebug testProdDebugUnitTest assembleProdRelease`) |
| **Estimated runtime** | ~60 s targeted; several minutes full |

---

## Sampling Rate

- **After every task commit:** Run the task's targeted `--tests` pattern
- **After every plan wave:** Run `./gradlew testDevDebugUnitTest ktlintCheck && bash scripts/check-design-rules.sh`
- **Before `/gsd-verify-work`:** Full suite green on both flavors plus `assembleProdRelease`
- **Max feedback latency:** 60 seconds (targeted)

---

## Per-Task Verification Map

Filled in by the planner (2026-10-23). Requirement → test map from 08-RESEARCH.md, with the plan and task that create each test. Every task has an `<automated>` verify; no three consecutive tasks lack one. Statuses flip to green in 08-15.

| Req ID | Behavior | Plan / Task | Test Type | Automated Command | File Exists | Status |
|--------|----------|-------------|-----------|-------------------|-------------|--------|
| BOOK-06/07/09 | DTO decode (slim/full reschedule, auth expiry, join link, can_join, cancel major units, idempotent 200) and request encoding | 08-01 T1 | unit | `--tests '*ResponseDecodingTest'` | ✅ extend | ✅ green |
| BOOK-07/09 | Cancel / accept / decline wire bodies, paths, verbs; URL consts | 08-01 T2 | MockWebServer | `--tests '*ClickMeApiTest' --tests '*ApiRequestUrlsTest'` | ✅ extend | ✅ green |
| BOOK-07/09 | Repository pass-through and error codes | 08-01 T3 | unit | `--tests '*ClientBookingsRepositoryTest'` | ❌ W0 (08-01) | ✅ green |
| BOOK-06/08 | Status / payment-pill / cancellable tables (D-14, D-15) | 08-02 T1 | unit | `--tests '*BookingStatusDisplayTest'` | ❌ W0 (08-02) | ✅ green |
| BOOK-05 | Bucket/rejoin rule, labels, sort, unique ids | 08-02 T2 | unit | `--tests '*BookingBucketsTest'` | ❌ W0 (08-02) | ✅ green |
| BOOK-07 | One-shot post-cancel launch holder (D-07) | 08-02 T3 | unit | `--tests '*BookingsLaunchRequestTest'` | ❌ W0 (08-02) | ✅ green |
| BOOK-07 | Cancel reason merge/cap (D-05) | 08-03 T1 | unit | `--tests '*CancellationReasonTest'` | ❌ W0 (08-03) | ✅ green |
| BOOK-07 | Refund preview rows + strict 24h boundary (D-06) | 08-03 T2 | unit | `--tests '*RefundPreviewTest'` | ❌ W0 (08-03) | ✅ green |
| BOOK-07 | `Money.formatMajor` incl. zero-decimal set (A1) | 08-03 T3 | unit | `--tests '*MoneyTest'` | ✅ extend | ✅ green |
| BOOK-07/09 | Copy contract (5 string files) | 08-04 T1 | build | `:app:processDevDebugResources` | n/a | ✅ green |
| BOOK-07/09 | Reschedule + cancel error mapping | 08-04 T2 | unit | `--tests '*RescheduleErrorsTest'` | ❌ W0 (08-04) | ✅ green |
| BOOK-09 | Auth-window disclosure, proposal model (D-03) | 08-05 T1 | unit | `--tests '*RescheduleDisclosureTest'` | ❌ W0 (08-05) | ✅ green |
| BOOK-09 | Accept with PAYMENT_METHOD_REQUIRED one-shot retry; decline reason (D-02, D-04) | 08-05 T2 | unit | `--tests '*RescheduleResponseTest'` | ❌ W0 (08-05) | ✅ green |
| BOOK-09 | Controller: in-flight guard, reload-before-outcome, dialogs, notices | 08-05 T3 | unit | `--tests '*RescheduleResponseTest'` | (08-05) | ✅ green |
| BOOK-05/08 | `formatProposalSlot`, status badge, destructive button | 08-06 T1 | unit + Compose | `--tests '*BookingFormattingTest' --tests '*BookingStatusBadgeTest'` | ✅ extend / ❌ W0 (08-06) | ✅ green |
| BOOK-09 | Reschedule response card + dialogs | 08-06 T2 | Compose | `--tests '*RescheduleResponseCardTest'` | ❌ W0 (08-06) | ✅ green |
| BOOK-05 | MyBookings VM two-bucket load, retry, pull-to-refresh, saved tab | 08-07 T1 | unit | `--tests '*MyBookingsViewModelTest'` | ❌ W0 (08-07) | ✅ green |
| BOOK-05 | MyBookings VM resume coalescing, realtime reload, launch request | 08-07 T2 | unit | `--tests '*MyBookingsViewModelTest'` | (08-07) | ✅ green |
| BOOK-07 | Cancellation VM load, steps, SavedStateHandle restore | 08-08 T1 | unit | `--tests '*ClientCancellationViewModelTest'` | ❌ W0 (08-08) | ✅ green |
| BOOK-07 | Cancellation VM confirm, single flight, result hand-off, errors | 08-08 T2 | unit | `--tests '*ClientCancellationViewModelTest'` | (08-08) | ✅ green |
| BOOK-05 | My Bookings cards and states | 08-09 T1 | Compose | `--tests '*MyBookingsScreenTest'` | ❌ W0 (08-09) | ✅ green |
| BOOK-05 | My Bookings screen: tabs, lists, snackbars | 08-09 T2 | Compose | `--tests '*MyBookingsScreenTest'` | (08-09) | ✅ green |
| BOOK-06 | Upcoming details mapping | 08-10 T1 | unit | `--tests '*UpcomingBookingViewModelTest'` | ❌ W0 (08-10) | ✅ green |
| BOOK-06/09 | Upcoming VM quiet reload, own-id realtime, reschedule delegation | 08-10 T2 | unit | `--tests '*UpcomingBookingViewModelTest'` | (08-10) | ✅ green |
| BOOK-07 | Cancellation step subviews and refund rows | 08-11 T1 | Compose | `--tests '*ClientCancellationScreenTest'` | ❌ W0 (08-11) | ✅ green |
| BOOK-07 | Cancellation screen Back rules, in-flight lock, errors, completion | 08-11 T2 | Compose | `--tests '*ClientCancellationScreenTest'` | (08-11) | ✅ green |
| BOOK-08/09 | Booking Summary mapping + VM | 08-12 T1 | unit | `--tests '*BookingSummaryViewModelTest'` | ❌ W0 (08-12) | ✅ green |
| BOOK-08/09 | Booking Summary screen | 08-12 T2 | Compose | `--tests '*BookingSummaryScreenTest'` | ❌ W0 (08-12) | ✅ green |
| BOOK-06 | Upcoming details subviews (status, pill, cards, tz note, join, banner) | 08-13 T1 | Compose | `--tests '*UpcomingBookingScreenTest'` | ❌ W0 (08-13) | ✅ green |
| BOOK-06/09 | Upcoming details screen (order, Cancel gating, reschedule dialogs) | 08-13 T2 | Compose | `--tests '*UpcomingBookingScreenTest'` | (08-13) | ✅ green |
| BOOK-05 | Today's Sessions card click → Upcoming details; Join unchanged | 08-14 T1 | Compose | `--tests '*ExploreSectionsTest' --tests '*ExploreScreenTest'` | ✅ extend | ✅ green |
| BOOK-06/07/08 | Navigator helpers; resetAndSelect clears both stacks | 08-14 T2 | unit | `--tests '*BookingFlowNavigationTest'` | ✅ extend | ✅ green |
| BOOK-05..09 | Routes with hidden bottom bar, post-cancel reset from both origins, Payment Methods, Book Again | 08-14 T3 | Robolectric nav | `--tests '*HomeShellsTest'` (replaces the earlier `HomeTabScaffoldTest` placeholder) | ✅ extend | ✅ green |
| all | Full gate both flavors + R8 + design rules; scope audit | 08-15 T1 | build | full suite command | n/a | ✅ green |
| BOOK-07/09 | Repository pass-through and error codes (file created in 08-01) | 08-01 T3 | unit | `--tests '*ClientBookingsRepositoryTest'` | ✅ | ✅ green |
| BOOK-06/08 | `BookingStatusDisplayTest` (status / payment-pill / cancellable tables), created in 08-02 | 08-02 T1 | unit | `--tests '*BookingStatusDisplayTest'` | ✅ | ✅ green |
| BOOK-07 | `BookingsLaunchRequestTest` (one-shot post-cancel launch holder), created in 08-02 | 08-02 T3 | unit | `--tests '*BookingsLaunchRequestTest'` | ✅ | ✅ green |
| BOOK-05/08 | `BookingStatusBadgeTest` (status badge), created in 08-06 | 08-06 T1 | Compose | `--tests '*BookingStatusBadgeTest'` | ✅ | ✅ green |
| BOOK-08/09 | `BookingSummaryViewModelTest`, created in 08-12 | 08-12 T1 | unit | `--tests '*BookingSummaryViewModelTest'` | ✅ | ✅ green |
| BOOK-06 | `UpcomingBookingViewModelTest` also covers the details mapping | 08-10 T1/T2 | unit | `--tests '*UpcomingBookingViewModelTest'` | ✅ | ✅ green |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

Note: the routes row (08-14 T3) is covered by `HomeShellsTest` + `BookingFlowNavigationTest`, not `HomeTabScaffoldTest`.

---

## Wave 0 Requirements

- [x] `FakeClickMeApi` — cancel/accept/decline handlers + counters + recorded requests; `clientBookingsQueries` (08-01 T2)
- [x] `FakeRealtimeConnection` — `emit(event)` (08-01 T2)
- [x] Booking fixtures with new detail fields (`reschedule`, `authorization_expires_at`, `join_link`, `can_join`) in `BookingManagementFixtures.kt` (08-01 T1)
- [x] New test files listed above

No new framework or dependency install.

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| iOS expert sees Android client's cancellation | BOOK-07 | Cross-platform, needs two devices + backend | Cancel on Android; confirm iOS expert app shows cancelled |
| iOS expert proposes reschedule; Android accepts/declines | BOOK-09 | Cross-platform; card-retry path needs test-mode Stripe backend | Propose on iOS; respond on Android; confirm status leaves `pending_reschedule` |

Per project memory: log these as open items, do not block execution.

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency < 60s
- [x] Frontmatter nyquist flag set to compliant

**Approval:** approved 2026-10-23

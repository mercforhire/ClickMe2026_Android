---
phase: 8
slug: client-bookings-cancellation
status: draft
nyquist_compliant: false
wave_0_complete: false
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

Filled in by plans; requirement → test map below is the contract (from 08-RESEARCH.md).

| Req ID | Behavior | Test Type | Automated Command | File Exists | Status |
|--------|----------|-----------|-------------------|-------------|--------|
| BOOK-05 | Bucket/rejoin rule, labels, sort | unit | `--tests '*BookingBucketsTest'` | ❌ W0 | ⬜ pending |
| BOOK-05 | MyBookings VM two-bucket load, quiet reload, realtime reload | unit | `--tests '*MyBookingsViewModelTest'` | ❌ W0 | ⬜ pending |
| BOOK-05 | MyBookings screen states | Compose | `--tests '*MyBookingsScreenTest'` | ❌ W0 | ⬜ pending |
| BOOK-05 | Today's Sessions card click → UpcomingBookingKey | Compose | `--tests '*ExploreSectionsTest'` | ✅ extend | ⬜ pending |
| BOOK-06 | Detail DTO decode new fields | unit | `--tests '*ResponseDecodingTest'` | ✅ extend | ⬜ pending |
| BOOK-06 | Upcoming details screen (banner/join/cancel, no reschedule) | Compose | `--tests '*UpcomingBookingScreenTest'` | ❌ W0 | ⬜ pending |
| BOOK-06 | Upcoming VM quiet reload | unit | `--tests '*UpcomingBookingViewModelTest'` | ❌ W0 | ⬜ pending |
| BOOK-07 | Cancel reason merge/cap | unit | `--tests '*CancellationReasonTest'` | ❌ W0 | ⬜ pending |
| BOOK-07 | Refund preview rows + 24h boundary | unit | `--tests '*RefundPreviewTest'` | ❌ W0 | ⬜ pending |
| BOOK-07 | `Money.formatMajor` | unit | `--tests '*MoneyTest'` | ✅ extend | ⬜ pending |
| BOOK-07 | Cancellation VM steps/errors | unit | `--tests '*ClientCancellationViewModelTest'` | ❌ W0 | ⬜ pending |
| BOOK-07 | Cancellation screen | Compose | `--tests '*ClientCancellationScreenTest'` | ❌ W0 | ⬜ pending |
| BOOK-07 | Cancel wire body/path | MockWebServer | `--tests '*ClickMeApiTest' --tests '*ApiRequestUrlsTest'` | ✅ extend | ⬜ pending |
| BOOK-08 | Booking Summary screen | Compose | `--tests '*BookingSummaryScreenTest'` | ❌ W0 | ⬜ pending |
| BOOK-09 | Accept with PAYMENT_METHOD_REQUIRED retry | unit | `--tests '*RescheduleResponseTest'` | ❌ W0 | ⬜ pending |
| BOOK-09 | Reschedule error mapping | unit | `--tests '*RescheduleErrorsTest'` | ❌ W0 | ⬜ pending |
| BOOK-09 | Auth-window disclosure | unit | `--tests '*RescheduleDisclosureTest'` | ❌ W0 | ⬜ pending |
| BOOK-09 | Decline body | unit + MockWebServer | `--tests '*ClickMeApiTest'` | ✅ extend | ⬜ pending |
| BOOK-09 | Reschedule response card | Compose | `--tests '*RescheduleResponseCardTest'` | ❌ W0 | ⬜ pending |
| BOOK-06/07/08 | Routes, hidden bottom bar, resetAndSelect after cancel | Robolectric nav | `--tests '*BookingFlowNavigationTest' --tests '*HomeTabScaffoldTest'` | ✅ extend | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `FakeClickMeApi` — cancel/accept/decline handlers + counters; `clientBookingsHandler(type)`
- [ ] `FakeRealtimeConnection` — `emit(event)`
- [ ] Booking fixtures with new detail fields (`reschedule`, `authorization_expires_at`, `join_link`, `can_join`)
- [ ] New test files listed above

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

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 60s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending

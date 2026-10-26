---
phase: "11"
slug: "expert-requests-bookings"
# status lifecycle: draft (seeded by plan-phase) → validated (set by validate-phase §6)
# audit-milestone §5.5 distinguishes NOT-VALIDATED (draft) from PARTIAL (validated + nyquist_compliant: false) (#2117)
status: complete
nyquist_compliant: true
wave_0_complete: true
created: "2026-10-25"
---

# Phase 11 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution. Source: `11-RESEARCH.md` § Validation Architecture.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4 + Robolectric + Compose UI test (`createComposeRule`) + kotlinx-coroutines-test + mockwebserver3 (all existing) |
| **Config file** | `app/build.gradle.kts`; `app/src/test/resources/robolectric.properties`; shared helpers in `app/src/test/.../testing/` (`MainDispatcherRule`, `FakeClickMeApi`, `FakeCallGuard`, `FakeCallStarter`) |
| **Quick run command** | `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDevDebugUnitTest --tests "com.uptrendinvestments.clickmeapp.screens.expertmode.*"` |
| **Full suite command** | `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDevDebugUnitTest testProdDebugUnitTest ktlintCheck assembleDevDebug assembleProdDebug && bash scripts/check-design-rules.sh` (run lint in a separate invocation; never `gradlew --stop`) |
| **Estimated runtime** | ~180 seconds (quick run ~60 seconds) |

---

## Sampling Rate

- **After every task commit:** Run the task's `<automated>` command (the single new or changed test class, `--tests "*<Name>"`) plus `ktlintCheck`.
- **After every plan wave:** Run `--tests "com.uptrendinvestments.clickmeapp.screens.expertmode.*" --tests "*network.*" --tests "*components.*"`, then `ktlintCheck assembleDevDebug` and `bash scripts/check-design-rules.sh`.
- **Before `/gsd-verify-work`:** The full suite must be green.
- **Max feedback latency:** 180 seconds.

---

## Per-Task Verification Map

Plan and task IDs filled at planning (2026-10-26). Requirement-to-test targets come from RESEARCH; the rows after the first 18 are the extra test classes the plans create. Plan 11-15 set the Status column from the gate run (2947 tests per flavor, 0 failures, 0 skipped).

| Requirement | Behavior | Test Type | Test class | Plan / Task | File Exists | Status |
|-------------|----------|-----------|------------|-------------|-------------|--------|
| EXP-05 | Dashboard VM: the counts, the today filter, soft-fail vs hard-fail fetches, onboarding mint, re-fetch on resume | unit | `*ExpertDashboardViewModelTest` | 11-10 T3; 11-14 T1 | ✅ | ✅ green |
| EXP-05 | Weekly, Connect and payout wire decode (minor-unit `Long`) | unit (MockWebServer) | `*ExpertFinancialApiTest` | 11-01 T3 | ✅ | ✅ green |
| EXP-05 | Dashboard screen states | Robolectric compose | `*ExpertDashboardScreenTest` | 11-10 T1-T2; 11-14 T2-T3 | ✅ | ✅ green |
| EXP-06 | Requests VM: sort, expired rule, quiet reload | unit | `*IncomingRequestsViewModelTest` | 11-05 T3 | ✅ | ✅ green |
| EXP-06 | Decision VM: accept and decline bodies, single in-flight request, `ALREADY_PROCESSED` | unit | `*RequestDecisionViewModelTest` | 11-06 T3; 11-12 T1-T3 | ✅ | ✅ green |
| EXP-06 | Earnings-section branches and money conversion (JPY) | unit + compose | `*RequestEarningsSectionTest` | 11-06 T2 | ✅ | ✅ green |
| EXP-06 | Accept and decline wire JSON; list and detail decode | unit (MockWebServer) | `*BookingRequestsApiTest` | 11-01 T1 | ✅ | ✅ green |
| EXP-07 | Calendar intent builder; no-handler path | Robolectric unit | `*CalendarIntentTest` | 11-07 T1 | ✅ | ✅ green |
| EXP-07 | Confirmed screen actions | compose | `*BookingConfirmedScreenTest` | 11-07 T2-T3 | ✅ | ✅ green |
| EXP-08 | Bucket builder (excludes `pending_approval`, requires `endTime > now`, includes rejoinable sessions) | unit (pure) | `*ExpertBookingBucketsTest` | 11-04 T2 | ✅ | ✅ green |
| EXP-08 | Bookings VM: both buckets, quiet reload, join request | unit | `*ExpertBookingsViewModelTest` | 11-04 T3 | ✅ | ✅ green |
| EXP-08 | Join-window rule (reused) | unit | `*JoinWindowTest` | 11-04 T3 (regression) | ✅ | ✅ green |
| EXP-09 | Summary VM: `/details`, guarded reschedule responses | unit | `*ExpertBookingSummaryViewModelTest` | 11-08 T3; 11-13 T3 | ✅ | ✅ green |
| EXP-09 | Expert reschedule error mapper (`SLOT_UNAVAILABLE`) | unit (pure) | `*ExpertRescheduleErrorsTest` | 11-13 T2 | ✅ | ✅ green |
| EXP-09 | Expert variant of the reschedule card, with no client regression | compose | `*RescheduleResponseCardTest` (extend) | 11-03 T1 | ✅ | ✅ green |
| EXP-09 | Cancel VM and refund preview (Held / None / dormant Captured rows) | unit | `*ExpertCancellationViewModelTest`, `*ExpertRefundPreviewTest` | 11-09 T3, 11-09 T2 | ✅ | ✅ green |
| EXP-05/08/09 | URL constants (incl. `/details`), nav keys and origins | unit | `*ApiRequestUrlsTest`, `*ExpertNavigationTest` | 11-01 T3, 11-02 T2 | ✅ | ✅ green |
| All | The shell still builds with the new tab-root slots | Robolectric | `*HomeShellsTest` (extend) | 11-02 T1 | ✅ | ✅ green |
| EXP-08/09 | Expert bookings feed, `/details` path, cancel and reschedule bodies | unit (MockWebServer) | `*ExpertBookingsApiTest` | 11-01 T2 | ✅ | ✅ green |
| All | One-shot hand-off slots are independent and consumed once | unit | `*ExpertLaunchRequestTest` | 11-02 T3 | ✅ | ✅ green |
| EXP-05/09 | Guard copy for SetUpPayouts / RespondToReschedule | Robolectric compose | `*CallDialogsTest` (extend) | 11-03 T2 | ✅ | ✅ green |
| EXP-05/08/09 | Shared expert copy and the reschedule badge | Robolectric compose | `*ExpertSharedUiTest` | 11-03 T3 | ✅ | ✅ green |
| EXP-08 | Bookings screen: Upcoming, Past, empty states, snackbars | Robolectric compose | `*ExpertBookingsScreenTest` | 11-04 T1-T2; 11-11 T1-T3 | ✅ | ✅ green |
| EXP-06 | Requests screen and logic: cards, expiry, earnings, states | Robolectric compose + unit | `*IncomingRequestsScreenTest`, `*IncomingRequestsLogicTest` | 11-05 T1-T2 | ✅ | ✅ green |
| EXP-06 | Decision screen: blocks, footer, sheets, locks, expired | Robolectric compose | `*RequestDecisionScreenTest` | 11-06 T1; 11-12 T1-T3 | ✅ | ✅ green |
| EXP-06 | Decline codes, wire labels, label fallback, caps | unit | `*DeclineReasonTest` | 11-12 T2 | ✅ | ✅ green |
| EXP-08/09 | Summary screen: details, fee, Join, Cancel entry, reschedule card | Robolectric compose | `*ExpertBookingSummaryScreenTest` | 11-08 T1-T2; 11-13 T1, T3 | ✅ | ✅ green |
| EXP-09 | Cancel screen end to end and the refund line | Robolectric compose | `*ExpertCancellationScreenTest` | 11-09 T1-T2 | ✅ | ✅ green |
| EXP-05 | Today's Sessions boundaries, reschedule count, first name | unit (pure) | `*DashboardLogicTest` | 11-10 T2 | ✅ | ✅ green |
| EXP-05 | Payout onboarding truth table and https-only URL rule | unit (pure) | `*DashboardFinancialsTest` | 11-14 T2 | ✅ | ✅ green |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [x] `FakeClickMeApi`: handlers, call counters and recorded-request lists for every new `ClickMeApi` method. Without them the test source set will not compile.
- [x] `testing/ExpertBookingFixtures.kt`: expert list item, detail, request item and detail, and breakdown variants.
- [x] Update every exhaustive `when` over `RestrictedAction` in tests and in `CallDialogs.kt`.
- [x] Framework install: none needed.

---

## Manual-Only Verifications

These are deferred. Per the project preference, they are logged as open items and nothing blocks on them. The checklist lives in `.planning/STATE.md` under "[Phase 11] DEFERRED (human)" (items a to e map to the rows below).

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Accept and decline a request booked by an iOS client | EXP-06 | Needs a live cross-platform booking | Book from iOS, then accept or decline on the Android Samsung test device |
| Confirmed screen and calendar insert | EXP-07 | Needs the device calendar app | Accept, tap Add to Calendar, check the event fields |
| Custom Tab onboarding round trip | EXP-05 | Needs Stripe-hosted onboarding | Tap the payout card, finish or abandon, return, check the status refresh |
| Join from all three entry points with the iOS client | EXP-08 | Needs a live Agora call | Inside the join window, join from Dashboard, Bookings and Summary |
| Reschedule accept/decline and `SLOT_UNAVAILABLE` | EXP-09 | Needs back-to-back bookings (30-minute buffer) | Arrange a conflicting slot, accept, check the message |
| Expert cancel | EXP-09 | Live booking state | Cancel, then check the Past tab and the snackbar |
| Layout at 200% font scale | All | Visual check | Check every UI-SPEC screen at 200% |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency < 180s
- [x] The nyquist flag is set to true in the frontmatter

**Approval:** approved 2026-10-26 (plan 11-15 gate green on both flavors, lint and R8 `assembleProdRelease` in separate invocations)

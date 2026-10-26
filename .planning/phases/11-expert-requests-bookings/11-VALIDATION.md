---
phase: "11"
slug: "expert-requests-bookings"
# status lifecycle: draft (seeded by plan-phase) → validated (set by validate-phase §6)
# audit-milestone §5.5 distinguishes NOT-VALIDATED (draft) from PARTIAL (validated + nyquist_compliant: false) (#2117)
status: draft
nyquist_compliant: false
wave_0_complete: false
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

The planner fills in the plan and task IDs. Requirement-to-test targets come from RESEARCH:

| Requirement | Behavior | Test Type | Test class | File Exists | Status |
|-------------|----------|-----------|------------|-------------|--------|
| EXP-05 | Dashboard VM: the counts, the today filter, soft-fail vs hard-fail fetches, onboarding mint, re-fetch on resume | unit | `*ExpertDashboardViewModelTest` | ❌ W0 | ⬜ pending |
| EXP-05 | Weekly, Connect and payout wire decode (minor-unit `Long`) | unit (MockWebServer) | `*ExpertFinancialApiTest` | ❌ W0 | ⬜ pending |
| EXP-05 | Dashboard screen states | Robolectric compose | `*ExpertDashboardScreenTest` | ❌ W0 | ⬜ pending |
| EXP-06 | Requests VM: sort, expired rule, quiet reload | unit | `*IncomingRequestsViewModelTest` | ❌ W0 | ⬜ pending |
| EXP-06 | Decision VM: accept and decline bodies, single in-flight request, `ALREADY_PROCESSED` | unit | `*RequestDecisionViewModelTest` | ❌ W0 | ⬜ pending |
| EXP-06 | Earnings-section branches and money conversion (JPY) | unit + compose | `*RequestEarningsSectionTest` | ❌ W0 | ⬜ pending |
| EXP-06 | Accept and decline wire JSON; list and detail decode | unit (MockWebServer) | `*BookingRequestsApiTest` | ❌ W0 | ⬜ pending |
| EXP-07 | Calendar intent builder; no-handler path | Robolectric unit | `*CalendarIntentTest` | ❌ W0 | ⬜ pending |
| EXP-07 | Confirmed screen actions | compose | `*BookingConfirmedScreenTest` | ❌ W0 | ⬜ pending |
| EXP-08 | Bucket builder (excludes `pending_approval`, requires `endTime > now`, includes rejoinable sessions) | unit (pure) | `*ExpertBookingBucketsTest` | ❌ W0 | ⬜ pending |
| EXP-08 | Bookings VM: both buckets, quiet reload, join request | unit | `*ExpertBookingsViewModelTest` | ❌ W0 | ⬜ pending |
| EXP-08 | Join-window rule (reused) | unit | `*JoinWindowTest` | ✅ | ⬜ pending |
| EXP-09 | Summary VM: `/details`, guarded reschedule responses | unit | `*ExpertBookingSummaryViewModelTest` | ❌ W0 | ⬜ pending |
| EXP-09 | Expert reschedule error mapper (`SLOT_UNAVAILABLE`) | unit (pure) | `*ExpertRescheduleErrorsTest` | ❌ W0 | ⬜ pending |
| EXP-09 | Expert variant of the reschedule card, with no client regression | compose | `*RescheduleResponseCardTest` (extend) | ✅ | ⬜ pending |
| EXP-09 | Cancel VM and refund preview (Held / None / dormant Captured rows) | unit | `*ExpertCancellationViewModelTest`, `*ExpertRefundPreviewTest` | ❌ W0 | ⬜ pending |
| EXP-05/08/09 | URL constants (incl. `/details`), nav keys and origins | unit | `*ApiRequestUrlsTest`, `*ExpertNavigationTest` | ✅ / ❌ W0 | ⬜ pending |
| All | The shell still builds with the new tab-root slots | Robolectric | `*HomeShellsTest` (extend) | ✅ | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `FakeClickMeApi`: handlers, call counters and recorded-request lists for every new `ClickMeApi` method. Without them the test source set will not compile.
- [ ] `testing/ExpertBookingFixtures.kt`: expert list item, detail, request item and detail, and breakdown variants.
- [ ] Update every exhaustive `when` over `RestrictedAction` in tests and in `CallDialogs.kt`.
- [ ] Framework install: none needed.

---

## Manual-Only Verifications

These are deferred. Per the project preference, log them as open items and do not block on them.

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

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 180s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending

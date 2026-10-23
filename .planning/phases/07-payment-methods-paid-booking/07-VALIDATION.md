---
phase: 7
slug: payment-methods-paid-booking
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-10-22
---

# Phase 7 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4.13.2 + Robolectric + kotlinx-coroutines-test 1.11.0 + Compose ui-test-junit4 + okhttp-mockwebserver3 |
| **Config file** | none — `app/build.gradle.kts` `testOptions.unitTests.isIncludeAndroidResources = true`; fakes in `app/src/test/.../testing/` |
| **Quick run command** | `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDevDebugUnitTest --tests '<pattern>'` |
| **Full suite command** | `./gradlew ktlintCheck lintDevDebug lintProdDebug testDevDebugUnitTest testProdDebugUnitTest assembleDevDebug assembleProdDebug assembleProdRelease && bash scripts/check-design-rules.sh` |
| **Estimated runtime** | ~30–60 s targeted; several minutes full |

---

## Sampling Rate

- **After every task commit:** Run the task's targeted `--tests` pattern
- **After every plan wave:** Run `./gradlew testDevDebugUnitTest ktlintCheck && bash scripts/check-design-rules.sh`
- **Before `/gsd-verify-work`:** Full suite must be green (both flavors, including `assembleProdRelease` for R8 with Stripe)
- **Max feedback latency:** 60 seconds

---

## Per-Task Verification Map

Populated by the planner/executor from the Phase Requirements → Test Map in 07-RESEARCH.md.

| Req ID | Behavior | Test Type | Automated Command | File Exists | Status |
|--------|----------|-----------|-------------------|-------------|--------|
| PAY-01 | List/empty/failed; set default; delete; busy rows; alerts | unit + Compose | `--tests '*PaymentMethodsViewModelTest' --tests '*PaymentMethodsScreenTest' --tests '*PaymentMethodRowTest'` | ❌ W0 | ⬜ pending |
| PAY-02 | Add card via setup-intent + sheet; Completed/Canceled/Failed handling | unit (VM) | `--tests '*PaymentMethodsViewModelTest'` | ❌ W0 | ⬜ pending |
| PAY-02/06 | New endpoints, `{}` bodies, DTO decoding | MockWebServer | `--tests '*ClickMeApiTest' --tests '*ResponseDecodingTest' --tests '*ApiRequestUrlsTest'` | extend | ⬜ pending |
| PAY-03 | Short-lead order, attempt-id reuse, single flight, pm read failure | unit (VM) | `--tests '*MakeABookingViewModelPaidTest'` | ❌ W0 | ⬜ pending |
| PAY-04 | Long-lead saved card; no-card gate → Payment Methods | unit (VM + UiState) | `--tests '*MakeABookingViewModelPaidTest' --tests '*MakeABookingUiStateTest'` | ❌ W0 / extend | ⬜ pending |
| PAY-05 | 402 → 3DS challenge → confirm retry; no loop | unit (VM) | `--tests '*MakeABookingViewModelScaTest'` | ❌ W0 | ⬜ pending |
| PAY-06 | All 15 paid-flow error codes + unknown | unit (pure) | `--tests '*PaidFlowErrorTest'` | ❌ W0 | ⬜ pending |
| PAY-07 | Minor units incl. JPY; lifecycle card; Request Sent | unit + Compose | `--tests '*MoneyTest' --tests '*PaymentLifecycleCardTest' --tests '*BookingRequestSentScreenTest'` | partial | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `FakeClickMeApi` — handlers + call counters/body recording for `paymentIntent`, `confirmBooking`, `getPaymentMethods`, `createSetupIntent`, `deletePaymentMethod`, `setDefaultPaymentMethod`
- [ ] `FakeStripePaymentMethodReader` in `testing/`
- [ ] `BookingTestFixtures` — paid topic helpers, saved-card fixtures, `newViewModel` with payment repository + reader params
- [ ] Test files: `PaymentMethodsViewModelTest`, `PaymentMethodsScreenTest`, `MakeABookingViewModelPaidTest`, `MakeABookingViewModelScaTest`, `PaidFlowErrorTest`, `PaymentLifecycleCardTest`
- [ ] No new framework installs

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| PaymentSheet setup mode on device | PAY-02 | Real Stripe SDK UI; needs matching `pk_test_` key (D-01/D-02) | Add test card 4242…, confirm it appears and can be set default/removed |
| FlowController short-lead booking, iOS expert receives request | PAY-03 | Cross-device, live backend | Book short-lead paid slot; check iOS expert inbox; retry after forced failure, no duplicate |
| 3DS challenge via PaymentLauncher | PAY-05 | Stripe challenge UI | Use 4000 0027 6000 3184; pass challenge; booking confirms |
| Rotation / dark appearance / 200% font / JPY topic | PAY-03..07 | Visual + config-change behavior | Rotate during sheet/challenge; verify no double launch |

*Logged as open items per manual-testing-deferred policy (D-03).*

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 60s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending

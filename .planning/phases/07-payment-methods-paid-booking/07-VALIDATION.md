---
phase: 7
slug: payment-methods-paid-booking
status: complete
nyquist_compliant: true
wave_0_complete: true
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

Commands below omit the `JAVA_HOME=... ./gradlew :app:testDevDebugUnitTest` prefix where they are `--tests` patterns. "W0" = the plan's own task creates the test file first (RED) before the code.

| Task ID | Plan | Wave | Requirement | Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|----------|-----------|-------------------|-------------|--------|
| 07-01-T1 | 01 | 1 | PAY-02, PAY-03, PAY-05 | Stripe dependency in :app, PaymentConfiguration.init, R8 release | build | `./gradlew assembleDevDebug assembleProdRelease` | n/a | ✅ green |
| 07-01-T2 | 01 | 1 | PAY-02, PAY-03 | pm_ reader seam, shared sheet config, dark appearance (colorsLight == colorsDark) | unit (Robolectric) | `--tests '*ClickMePaymentSheetStyleTest'` | W0 | ✅ green |
| 07-02-T1 | 02 | 1 | PAY-01..04, PAY-07 | DTO encoding (strict keys, omitted nulls, no booking_attempt_id on confirm), bare PI decode with decimal total, redacted toString | unit | `--tests '*ResponseDecodingTest'` | extend | ✅ green |
| 07-02-T2 | 02 | 1 | PAY-01..04 | Six endpoints: verbs, paths, `{}` bodies, auth required | MockWebServer | `--tests '*ClickMeApiTest' --tests '*ApiRequestUrlsTest'` | extend | ✅ green |
| 07-02-T3 | 02 | 1 | PAY-01..05 | Repositories; 402 detail preserved; long-lead 422 code | unit | `--tests '*PaymentRepositoriesTest'` | W0 | ✅ green |
| 07-03-T1 | 03 | 1 | PAY-03 | Step model, StripeRequest redaction, UTC Locale.ROOT strings | unit | `--tests '*PaidBookingFlowTest'` | W0 | ✅ green |
| 07-03-T2 | 03 | 1 | PAY-05, PAY-06 | handlePaidFlowError table (15 codes, suspended, unknown, network/decoding, D-19 variant, D-15 hard failure) | unit (pure) | `--tests '*PaidFlowErrorTest'` | W0 | ✅ green |
| 07-04-T1 | 04 | 1 | PAY-07 | Lifecycle card, both stages, Done semantics | Compose | `--tests '*PaymentLifecycleCardTest'` | W0 | ✅ green |
| 07-04-T2 | 04 | 1 | PAY-01, PAY-07 | Info sheet body, info link, ProfileMenuRow | Compose | `--tests '*PaymentInfoSheetTest' --tests '*ProfileMenuRowTest'` | W0 | ✅ green |
| 07-05-T1 | 05 | 2 | PAY-02 | D-01/D-02 open item in STATE.md | grep | `grep -c '\[Phase 7\] Stripe key and mode' .planning/STATE.md` | n/a | ✅ green |
| 07-05-T2 | 05 | 2 | PAY-02 | User supplies mode + pk_test key (non-blocking human-action) | manual | — (resume signal or "defer") | n/a | ⚠️ deferred (human, D-03): no key answer during the run; OPEN in STATE.md |
| 07-05-T3 | 05 | 2 | PAY-02 | Dev key swap or recorded outcome; no secret key | build + grep | `./gradlew assembleDevDebug && bash scripts/check-design-rules.sh` | n/a | ✅ green (defer branch: no key swap, outcome recorded) |
| 07-06-T1 | 06 | 2 | PAY-01 | Route key, UI state types, all Payment Methods strings | compile | `./gradlew :app:compileDevDebugKotlin ktlintCheck` | n/a | ✅ green |
| 07-06-T2 | 06 | 2 | PAY-01, PAY-02 | Load/retry, busy rule, server-truth reloads, three alert titles, add card, setup reuse | unit | `--tests '*PaymentMethodsViewModelTest'` | W0 | ✅ green |
| 07-07-T1 | 07 | 2 | PAY-04 | Card availability load, silent refresh, CTA derivation, first-topic default | unit | `--tests '*MakeABookingViewModelTest' --tests '*MakeABookingUiStateTest' --tests '*MakeABookingViewModelBookTest'` | extend | ✅ green |
| 07-07-T2 | 07 | 2 | PAY-04, PAY-07 | CTA label switch, paid note removed, JPY Summary price | Compose | `--tests '*MakeABookingFormCardsTest' --tests '*MakeABookingScreenTest'` | extend | ✅ green |
| 07-08-T1 | 08 | 2 | PAY-07 | formatSessionWhen in viewer zone and device time format | unit | `--tests '*BookingFormattingTest'` | extend | ✅ green |
| 07-08-T2 | 08 | 2 | PAY-07 | Paid lifecycle card on Request Sent; free unchanged | Compose | `--tests '*BookingRequestSentScreenTest'` | extend | ✅ green |
| 07-09-T1 | 09 | 3 | PAY-01, PAY-03 | Stripe result mapping, D-18 confirm rule, brand display, expiry | unit | `--tests '*StripeResultMappingTest' --tests '*PaymentMethodBrandDisplayTest'` | W0 | ✅ green |
| 07-09-T2 | 09 | 3 | PAY-01, PAY-02 | Screen states, rows, menu, remove dialog, busy, add-card host consume-once | Compose | `--tests '*PaymentMethodRowTest' --tests '*PaymentMethodsScreenTest'` | W0 | ✅ green |
| 07-10-T1 | 10 | 3 | PAY-03, PAY-04 | Short-lead and long-lead sequences, sheet outcomes, attempt id (D-11, D-17), setup reuse | unit | `--tests '*MakeABookingViewModelPaidTest'` | W0 | ✅ green |
| 07-10-T2 | 10 | 3 | PAY-06 | Failure semantics, D-16 unknown outcome, D-19 variant, recovery record | unit | `--tests '*MakeABookingViewModelPaidTest'` | W0 | ✅ green |
| 07-11-T1 | 11 | 4 | PAY-05 | 402 -> challenge (memory), retry once with same pm_/pi_, loop guard | unit | `--tests '*MakeABookingViewModelScaTest'` | W0 | ✅ green |
| 07-11-T2 | 11 | 4 | PAY-05, PAY-06 | 3DS cancel / failure / blank account (D-15) | unit | `--tests '*MakeABookingViewModelScaTest'` | W0 | ✅ green |
| 07-12-T1 | 12 | 4 | PAY-04 | CTA routes to Payment Methods; return refresh keeps selections | Compose | `--tests '*MakeABookingScreenTest'` | extend | ✅ green |
| 07-12-T2 | 12 | 4 | PAY-01 | PaymentMethodsKey registered once, Profile row, booking-gate push, Nav3 re-entry (A10) | Compose/nav | `--tests '*HomeShellsTest'` | extend | ✅ green |
| 07-13-T1 | 13 | 5 | PAY-03, PAY-05 | Stripe host slot: request consumed once, outcomes routed (fake host); real host compiles | Compose + build | `--tests '*MakeABookingScreenTest'` then `./gradlew assembleDevDebug` | extend | ✅ green |
| 07-13-T2 | 13 | 5 | PAY-06, PAY-07 | Info link and sheet on the form, payment reference in the dialog | Compose | `--tests '*MakeABookingScreenTest'` | extend | ✅ green |
| 07-14-T1 | 14 | 6 | all | Full suite incl. R8 with Stripe, scope and safety sweeps | full | full suite command above | n/a | ✅ green (1535 tests per flavor, 0 failures) |
| 07-14-T2 | 14 | 6 | all | Validation sign-off, deferred device checks, follow-ups | grep | `grep -c 'nyquist_compliant: t' 07-VALIDATION.md && grep -c '\[Phase 7\] DEFERRED (human)' .planning/STATE.md` | n/a | ✅ green |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

Each item is created by the first task that needs it (test written first, RED):

- [x] `FakeClickMeApi` — handlers, call counters and recorded bodies for `getPaymentMethods`, `createPaymentMethodSetupIntent`, `deletePaymentMethod`, `setDefaultPaymentMethod`, `createPaymentIntent`, `confirmBooking` (07-02-T2)
- [x] `FakeStripePaymentMethodReader` in `testing/` (07-01-T2)
- [x] `BookingTestFixtures` — `cardDto`, `stubCards` (07-07-T1); `paidFixture`, `newViewModel` with reader and monotonic clock (07-10-T1)
- [x] Test files: `ClickMePaymentSheetStyleTest`, `PaymentRepositoriesTest`, `PaidBookingFlowTest`, `PaidFlowErrorTest`, `PaymentLifecycleCardTest`, `PaymentInfoSheetTest`, `ProfileMenuRowTest`, `PaymentMethodsViewModelTest`, `StripeResultMappingTest`, `PaymentMethodBrandDisplayTest`, `PaymentMethodRowTest`, `PaymentMethodsScreenTest`, `MakeABookingViewModelPaidTest`, `MakeABookingViewModelScaTest`
- [x] No new framework installs

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| PaymentSheet setup mode on device | PAY-01, PAY-02 | Real Stripe SDK UI; needs the matching `pk_test_` key (D-01/D-02) | Add 4242 4242 4242 4242; it appears; set default; remove; sheet is dark on a light-mode phone |
| FlowController short-lead booking, iOS expert receives request | PAY-03 | Cross-device, live backend | Book a paid slot within 6 days; iOS expert sees the request; dismiss with a preselected card -> silent cancel (D-18); forced failure then retry -> no duplicate |
| Long-lead and no-card gate | PAY-04 | Live backend, > 6 day slot | No card -> "Add a Payment Method" -> add -> back -> "Book Now" -> long-lead confirms with the saved card |
| 3DS challenge via PaymentLauncher | PAY-05 | Stripe challenge UI on the connected account | Use 4000 0027 6000 3184 (4000 0025 0000 3155 as a second try); pass -> booking confirms; cancel -> alert and retry |
| Rotation / dark appearance / 200% font / JPY topic | PAY-03..07 | Visual and config-change behavior | Rotate during the sheet and the challenge (no double launch); JPY price has no decimals |

*Logged as open items per the manual-testing-deferred preference (D-03); budget ~10 setup-intents per 15 minutes per user.*

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency < 60s
- [x] `nyquist_compliant` set to true in frontmatter

**Approval:** 2026-10-23 -- automated gate green; device checks deferred (D-03)

---
phase: "16"
slug: "cross-platform-qa"
# status lifecycle: draft (seeded by plan-phase) → validated (set by validate-phase §6)
# audit-milestone §5.5 distinguishes NOT-VALIDATED (draft) from PARTIAL (validated + nyquist_compliant: false) (#2117)
status: validated
nyquist_compliant: true
wave_0_complete: true
created: "2026-10-30"
---

# Phase 16 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4.13.2 + Robolectric 4.17 + Mockito 5.24.0 + Compose ui-test + MockWebServer3 + kotlinx-coroutines-test |
| **Config file** | `app/build.gradle.kts` (`unitTests.isIncludeAndroidResources = true`); no `androidTest` source set (deferred) |
| **Quick run command** | `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDevDebugUnitTest --tests '*<FixTest>' ktlintCheck` |
| **Full suite command** | `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDevDebugUnitTest testProdDebugUnitTest ktlintCheck assembleDevDebug`, then separately `lintDevDebug lintProdDebug` and `assembleProdRelease` with `-Dorg.gradle.jvmargs=-Xmx4g -Pkotlin.daemon.jvmargs=-Xmx3g`, plus `bash scripts/check-design-rules.sh && bash scripts/check-design-rules-selftest.sh` |
| **Estimated runtime** | quick ~60 s; full gate ~10 min |

---

## Sampling Rate

- **After every task commit:** Run the quick command with the touched test classes plus `ktlintCheck` (doc-only tasks: their grep check)
- **After every plan wave:** Run `testDevDebugUnitTest ktlintCheck assembleDevDebug`, then `bash scripts/check-design-rules.sh`
- **Before `/gsd-verify-work`:** Full gate green before each upload build; verify-work also blocks on the user's matrix confirmation (D-09)
- **Max feedback latency:** 600 seconds

---

## Per-Task Verification Map

Phase 16 is a manual QA phase: the 8 plans produce runbook, ledger and bug-log records, and two fix batches (16-05, 16-07) ship Android code. Doc-producing tasks are sampled by their plan `<automated>` grep blocks; code fixes by a regression test per bug (TDD: failing before, green after).

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 16-01-T1..T3 | 01 | 1 | QA-01, QA-02, QA-03, D-06 | T-16-01..03 | Runbook spine, every tier-1 checklist row ID, bug-log D-11 columns | doc grep | 16-01-PLAN `<automated>` blocks (print SPINE-OK, MATRIX-OK, TIER1-OK) | ✅ | ✅ green |
| 16-02-T1 | 02 | 1 | REL-03 | T-16-04, T-16-05 | No keystore tracked; AAB signed with the pinned upload cert | tool check | `git ls-files \| grep -cE '\.jks$\|keystore\.properties'` = 0; `jarsigner -verify` + `keytool -printcert` on the vc 5 archive = pin d836be66… | commands only | ✅ green |
| 16-02-T2/T3, 16-03..08 H2 | 02-08 | 1-7 | QA-01, REL-05 | T-16-07, T-16-13 | Device runs the ledger's Play build | device check | H2: `adb -t T shell dumpsys package … \| grep versionCode`; `pm list packages -i` | manual-assisted | ✅ green (ledger vc 1-5, installer com.android.vending) |
| 16-04-T3, 16-06-T3 | 04, 06 | 3, 5 | QA-01 | T-16-15 | Every failed runbook row has a bug-log row | doc grep | `grep -oE '\[!\] fail \(BUG-[0-9]+' 16-MATRIX-RUNBOOK.md` → each id in `16-BUG-LOG.md` (4/4) | ✅ | ✅ green |
| 16-05-T1/T2 | 05 | 4 | QA-01 (BUG-05) | — | Stripe post-completion reset not treated as dismissal | unit | `--tests '*StripeResultMappingTest' --tests '*MakeABookingViewModelPaidTest'` | ✅ | ✅ green |
| 16-05-T2 | 05 | 4 | QA-01 (BUG-06) | — | Confirm requested before FlowController bound still runs | unit (Robolectric) | `--tests '*PaidFlowStripeHostRegressionTest'` | ✅ | ✅ green |
| 16-05-T2 | 05 | 4 | QA-01 (BUG-07) | — | Stripe Link never shown | unit | `--tests '*ClickMePaymentSheetStyleTest'` | ✅ | ✅ green |
| 16-05-T2 | 05 | 4 | QA-01 (BUG-08) | — | Real cause of a sheet failure logged | unit | `--tests '*StripeFailureLogTest'` | ✅ | ✅ green (not exercised on device) |
| 16-05-T2 | 05 | 4 | QA-01 (BUG-09) | — | Decoding remote audio counts as peer back | unit | `--tests '*AgoraEventMappingTest' --tests '*AgoraManagerCreateEngineTest'` | ✅ | ✅ green |
| 16-05-T2 | 05 | 4 | QA-01 (BUG-10) | — | End Call pinned below scrolling content | Compose UI (Robolectric) | `--tests '*MeetingCallScreenEndButtonRegressionTest' --tests '*MeetingCallScreenTest'` | ✅ | ✅ green |
| 16-05-T2 | 05 | 4 | QA-01 (BUG-11) | — | Splash glow not clipped square | Compose UI | `--tests '*SplashGlowRegressionTest'` | ✅ | ✅ green |
| 16-05-T2 | 05 | 4 | QA-01 (BUG-12) | — | Tab labels whole at large font | Compose UI | `--tests '*HomeTabScaffoldLabelRegressionTest'` | ✅ | ✅ green |
| (16.5 fix) | — | — | QA-01 (BUG-01) | — | Splash wordmark/tagline sizes match iOS | Compose UI | `--tests '*SplashScreenTest' --tests '*ThemeTest'` | ✅ | ✅ green (system-splash drawable part manual, see below) |
| 16-07-T1 | 07 | 6 | QA-01 (BUG-31) | — | Crop circle shows the photo | Robolectric native-graphics pixel | `--tests '*PhotoCropCircleRegressionTest'` | ✅ | ✅ green |
| 16-07-T2 | 07 | 6 | QA-01 (BUG-30) | — | Next / Publish clear the nav bar | Compose UI | `--tests '*SignupBottomBarInsetRegressionTest'` | ✅ | ✅ green |
| 16-07-T2 | 07 | 6 | QA-01 (BUG-29) | — | Booking ID on Payment History and expert rows | Compose UI | `--tests '*BookingIdOnListRowsRegressionTest'` | ✅ | ✅ green |
| 16-08-T1..T3 | 08 | 7 | QA-01, QA-02, QA-03 | T-16-23, T-16-24 | Close-out recorded, money settled, dated sign-off | doc grep | 16-08-PLAN `<automated>` blocks (print CLOSEOUT-RECORDED, MONEY-SETTLED, SIGNOFF-RECORDED) | ✅ | ✅ green |

Regression run 2026-11-05 on HEAD: 16 classes, 144 tests, 0 failures (`./gradlew :app:testDevDebugUnitTest` with the class filters above).

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [x] `.planning/phases/16-cross-platform-qa/16-MATRIX-RUNBOOK.md` — covers QA-02, QA-03, D-06
- [x] `.planning/phases/16-cross-platform-qa/16-BUG-LOG.md` — covers QA-01 (D-11 columns)
- [x] `.planning/phases/16-cross-platform-qa/16-BUILD-LEDGER.md` — versionCode and build proof
- [x] `~/ClickMe-release/` directory for archived artifacts and logs (outside the repo)

Framework install: none. Existing test infrastructure covers regression tests for fixes.

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Matrix passes with Android client + iOS expert (live card, 3DS, killed-app push, background call and rejoin, payout onboarding link) | QA-02 | Real hardware on both platforms, real money, user-confirmed checkpoint (D-09, D-13) | `16-MATRIX-RUNBOOK.md` direction 1 |
| Same matrix with iOS client + Android expert | QA-03 | Same | `16-MATRIX-RUNBOOK.md` direction 2 |
| Tier-1 rows of 15-DEVICE-CHECKLIST (font 200%, TalkBack, process death, IME) | QA-01 / D-06 | Device settings + assistive tech | Runbook Samsung-only setting passes |
| Internal-track build installs from Play | REL-03 / REL-05 | Play Console upload is manual | `pm list packages -i` shows installer `com.android.vending` |
| System splash shows the full logo (BUG-01 drawable/theme part, 2466fc09) | QA-01 | Resource-only change rendered by the OS splash API; not observable in Robolectric | Cold start on the Samsung; user accepted on vc 3, 2026-11-02 |
| Live-money settlement (holds voided, captures settled) | QA-02 / QA-03 / D-04 | Stripe live dashboard on a real connected account | Runbook Section 7 E7.1-E7.4; Paid bookings table |

Manual-only results: signed off by the user 2026-11-05 (option-confirm, runbook Section 8), with the accepted gaps listed there (TalkBack rows, 3DS challenge, payouts onboarding, tier 2-4).

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency < 600s
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** validated 2026-11-05

## Validation Audit 2026-11-06

| Metric | Count |
|---|---|
| Gaps found | 0 |
| Resolved | 0 |
| Escalated | 0 |

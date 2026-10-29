---
phase: "14"
slug: "payouts-account-settings"
# status lifecycle: draft (seeded by plan-phase) → validated (set by validate-phase §6)
# audit-milestone §5.5 distinguishes NOT-VALIDATED (draft) from PARTIAL (validated + nyquist_compliant: false) (#2117)
status: draft
nyquist_compliant: false
wave_0_complete: false
created: "2026-10-28"
---

# Phase 14 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4.13.2 + Robolectric 4.17 + Compose ui-test (junit4), kotlinx-coroutines-test, MockWebServer (mockwebserver3) |
| **Config file** | Gradle module `app`; tests in `app/src/test/java/com/uptrendinvestments/clickmeapp/` (no androidTest) |
| **Quick run command** | `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDevDebugUnitTest --tests '*SomeTest' ktlintCheck` |
| **Full suite command** | `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDevDebugUnitTest testProdDebugUnitTest ktlintCheck -Dorg.gradle.jvmargs=-Xmx4g -Pkotlin.daemon.jvmargs=-Xmx3g` |
| **Estimated runtime** | filtered run ~60-120 s (Gradle configuration dominates); full suite several minutes |

---

## Sampling Rate

- **After every task commit:** Run the quick command filtered to the task's test classes (each task's `<automated>` command)
- **After every plan wave:** Run the full suite command
- **Before `/gsd-verify-work`:** Full suite, `lintDevDebug lintProdDebug` and `assembleProdRelease` must be green (14-16)
- **Max feedback latency:** one filtered Gradle run per task

---

## Per-Task Verification Map

Task IDs bound at planning (2026-10-29). Commands are `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDevDebugUnitTest <filters> ktlintCheck` unless stated.

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 14-01 T1-T3 | 14-01 | 1 | PROF-02..05, PAYOUT-01, PAYOUT-03, PAYOUT-04 | T-14-01..T-14-03 | DTO toString redacts password, token, TIN, dob; DELETE carries its body; strict tax body | unit (MockWebServer) | `--tests '*AccountApiTest' --tests '*ProfileHelpApiTest' --tests '*PayoutsTaxApiTest' --tests '*ExpertFinancialApiTest'` plus `:app:assembleDevDebug` | ❌ W0 | ⬜ pending |
| 14-02 T1-T3 | 14-02 | 1 | PAYOUT-02, PROF-03, PROF-04 | T-14-04..T-14-07 | Only two exact clickme URIs act; no query or extras read; single browsable component | unit (Robolectric intent + PackageManager) | `--tests '*ConnectReturnIntentTest' --tests '*ConnectReturnManifestTest' --tests '*CallDialogsTest'` | ❌ W0 / ✅ extend | ⬜ pending |
| 14-03 T1-T3 | 14-03 | 1 | PROF-01 | T-14-08, T-14-09 | No dead rows; self-view hides Book Session | Compose (Robolectric) + unit | `--tests '*ClientProfileHome*' --tests '*ExpertProfileHomeScreenTest' --tests '*ExpertProfileScreenTest'` | ❌ W0 / ✅ extend | ⬜ pending |
| 14-04 T1-T2 | 14-04 | 2 | PROF-03 | T-14-10..T-14-12 | Passwords memory only, wiped on success; guard before send | unit + Compose | `--tests '*AccountSecurityViewModelTest' --tests '*AccountSecurityScreenTest'` | ❌ W0 | ⬜ pending |
| 14-05 T1-T3 | 14-05 | 2 | PROF-04 | T-14-13..T-14-17 | One DELETE per confirmation; token never stored; Farewell survives forced logout | unit + Compose | `--tests '*DeleteAccountViewModelTest' --tests '*DeleteAccountScreenTest' --tests '*AccountDeletionFarewellTest'` | ❌ W0 | ⬜ pending |
| 14-06 T1-T2 | 14-06 | 2 | PROF-05 | T-14-18, T-14-19 | Support reachable in every state; no crash without a mail app | unit + Compose | `--tests '*HelpViewModelTest' --tests '*HelpScreenTest'` | ❌ W0 | ⬜ pending |
| 14-07 T1-T3 | 14-07 | 2 | PROF-02 | T-14-20..T-14-22 | Saved only after a 200; no email key; pending edit flushed on Back | unit (virtual time) + Compose | `--tests '*ProfileAutoSaveTest' --tests '*ClientProfileSettingsViewModelTest' --tests '*ClientProfileSettingsScreenTest'` | ❌ W0 | ⬜ pending |
| 14-08 T1-T3 | 14-08 | 2 | PAYOUT-01, PAYOUT-02 | T-14-23..T-14-25 | https-only Account Link; URL one-shot; no false "eligible" | unit + Compose | `--tests '*PayoutsLogicTest' --tests '*PayoutsViewModelTest' --tests '*PayoutsScreenTest'` | ❌ W0 | ⬜ pending |
| 14-09 T1-T3 | 14-09 | 2 | PAYOUT-03 | T-14-26, T-14-27 | Minor units only on transactions; null-net breakdown regression | unit + Compose | `--tests '*EarningsTransactionRowTest' --tests '*EarningsUnitsRegressionTest' --tests '*TransactionsViewModelTest' --tests '*TransactionsScreenTest'` | ❌ W0 | ⬜ pending |
| 14-10 T1-T2 | 14-10 | 3 | PAYOUT-02, PAYOUT-03, PAYOUT-04 | T-14-28..T-14-30 | Return only re-fetches; refresh re-mints only for its own link | unit + Compose | `--tests '*PayoutsViewModelTest' --tests '*PayoutsScreenTest' --tests '*PayoutsLogicTest'` | ✅ extend | ⬜ pending |
| 14-11 T1-T2 | 14-11 | 2 | PAYOUT-04 | T-14-31..T-14-33 | TIN never stored or pre-filled; saved:false is a prompt | unit + Compose | `--tests '*ExpertTaxProfileViewModelTest' --tests '*ExpertTaxProfileScreenTest'` | ❌ W0 | ⬜ pending |
| 14-12 T1-T3 | 14-12 | 3 | PROF-02 | T-14-34, T-14-35 | Upload only after a confirmed crop; picker URI read once | unit + Compose | `--tests '*ProfileAvatarControllerTest' --tests '*ClientProfileSettingsViewModelTest' --tests '*ClientProfileSettingsScreenTest'` | ❌ W0 / ✅ extend | ⬜ pending |
| 14-13 T1-T3 | 14-13 | 4 | PROF-02 | T-14-36, T-14-37 | Meeting link http(s) only, never blank | unit + Compose | `--tests '*ExpertProfileSettingsViewModelTest' --tests '*ExpertProfileSettingsScreenTest'` | ❌ W0 | ⬜ pending |
| 14-14 T1-T2 | 14-14 | 5 | PROF-01..05, PAYOUT-01, PAYOUT-04 | T-14-38, T-14-39 | Debug UI only when the gate is on; no booking from Expert mode | unit + Compose + lint | `--tests '*SettingsEntriesTest' --tests '*HomeShellsTest' --tests '*ExpertNavigationTest'` plus `:app:lintDevDebug` | ❌ W0 / ✅ extend | ⬜ pending |
| 14-15 T1-T2 | 14-15 | 5 | PAYOUT-01, PAYOUT-02, PROF-03 | T-14-40..T-14-42 | No switch to Expert for a client-only account from a push | unit + Compose | `--tests '*ExpertDashboardScreenTest' --tests '*PushRouterTest' --tests '*PushDestinationResolverTest' --tests '*NotificationDestinationTest' --tests '*NotificationsViewModelTest'` | ✅ extend | ⬜ pending |
| 14-16 T1 | 14-16 | 6 | all | T-14-43 | No Phase 14 logging; R8 keeps the keys and adapters | full gate | `testDevDebugUnitTest testProdDebugUnitTest ktlintCheck assembleDevDebug assembleProdDebug :spikes:stripe:assembleDebug`, then `lintDevDebug lintProdDebug`, then `assembleProdRelease` (each with `-Dorg.gradle.jvmargs=-Xmx4g -Pkotlin.daemon.jvmargs=-Xmx3g`), design rules | n/a | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `app/src/test/.../testing/FakeClickMeApi.kt` — nine new endpoint handlers (14-01)
- [ ] `app/src/test/.../testing/SettingsFixtures.kt` — payload builders for the Phase 14 endpoints (14-01)
- [ ] Wire tests `AccountApiTest`, `ProfileHelpApiTest`, `PayoutsTaxApiTest` (14-01), including the DELETE-with-body request check (RESEARCH A3)
- Framework install: none (JUnit, Robolectric, Compose ui-test and MockWebServer are already on the test classpath).

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Custom Tab return through `clickme://connect/return` and `/refresh` (Chrome follows the backend 302) | PAYOUT-02 | Needs a real browser, device and a sandbox Connect account (RESEARCH A1) | STATE "[Phase 14] DEFERRED (human)" item (a); local sandbox runbook 07-LOCAL-STRIPE-TESTING.md |
| Delete a throwaway account end to end, Farewell through the real session teardown | PROF-04 | Irreversible on a real backend account | STATE item (d), uat account only |
| Avatar pick and crop on the Samsung and API 26-32 | PROF-02 | System photo picker UI | STATE item (c) |
| mailto opens a mail app | PROF-05 | Needs an installed mail client | STATE item (d) |
| 200% font scale and TalkBack on all Phase 14 screens | PROF-01..05, PAYOUT-01..04 | Visual and assistive-tech checks (UI Considerations backstops) | STATE item (f); Phase 15 POL-04 |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 14s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** {pending / approved YYYY-MM-DD}

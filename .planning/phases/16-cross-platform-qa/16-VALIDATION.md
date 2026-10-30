---
phase: "16"
slug: "cross-platform-qa"
# status lifecycle: draft (seeded by plan-phase) → validated (set by validate-phase §6)
# audit-milestone §5.5 distinguishes NOT-VALIDATED (draft) from PARTIAL (validated + nyquist_compliant: false) (#2117)
status: draft
nyquist_compliant: false
wave_0_complete: false
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

Filled in by the planner/executor per plan. Seed rows from research:

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 16-xx | TBD | 1 | QA-01 | — | Every failed runbook row has a bug-log row with D-07 severity | doc grep | `grep -c '^| BUG-' 16-BUG-LOG.md` | ❌ W0 | ⬜ pending |
| 16-xx | TBD | 1 | QA-02, QA-03, D-06 | — | Every tier-1 checklist row ID appears in the runbook | doc grep | row-ID coverage loop (RESEARCH § Code Examples) | ❌ W0 | ⬜ pending |
| 16-xx | TBD | 1 | REL-03 | — | No keystore or keystore.properties tracked; AAB signature verifies | tool check | `git ls-files \| grep -cE '\.jks$\|keystore\.properties'` prints 0; `jarsigner -verify` | commands only | ⬜ pending |
| 16-xx | TBD | — | QA-01 | — | Device runs the ledger's build | device check | `adb -t T shell dumpsys package com.uptrendinvestments.clickmeapp \| grep -E "versionCode\|lastUpdateTime"` | manual-assisted | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `.planning/phases/16-cross-platform-qa/16-MATRIX-RUNBOOK.md` — covers QA-02, QA-03, D-06
- [ ] `.planning/phases/16-cross-platform-qa/16-BUG-LOG.md` — covers QA-01 (D-11 columns)
- [ ] `.planning/phases/16-cross-platform-qa/16-BUILD-LEDGER.md` — versionCode and build proof
- [ ] `~/ClickMe-release/` directory for archived artifacts and logs (outside the repo)

Framework install: none. Existing test infrastructure covers regression tests for fixes.

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Matrix passes with Android client + iOS expert (live card, 3DS, killed-app push, background call and rejoin, payout onboarding link) | QA-02 | Real hardware on both platforms, real money, user-confirmed checkpoint (D-09, D-13) | `16-MATRIX-RUNBOOK.md` direction 1 |
| Same matrix with iOS client + Android expert | QA-03 | Same | `16-MATRIX-RUNBOOK.md` direction 2 |
| Tier-1 rows of 15-DEVICE-CHECKLIST (font 200%, TalkBack, process death, IME) | QA-01 / D-06 | Device settings + assistive tech | Runbook Samsung-only setting passes |
| Internal-track build installs from Play | REL-03 / REL-05 | Play Console upload is manual | `pm list packages -i` shows installer `com.android.vending` |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 600s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending

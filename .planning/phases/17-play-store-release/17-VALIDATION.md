---
phase: "17"
slug: "play-store-release"
# status lifecycle: draft (seeded by plan-phase) → validated (set by validate-phase §6)
# audit-milestone §5.5 distinguishes NOT-VALIDATED (draft) from PARTIAL (validated + nyquist_compliant: false) (#2117)
status: draft
nyquist_compliant: false
wave_0_complete: false
created: "2026-11-05"
---

# Phase 17 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit4 + Robolectric + Compose ui-test (`createAndroidComposeRule`), MockWebServer fakes, `runTest` |
| **Config file** | `app/build.gradle.kts` (`testOptions.unitTests.isIncludeAndroidResources = true`); ktlint via `ktlintCheck`; `scripts/check-design-rules.sh` |
| **Quick run command** | `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew -Pkotlin.daemon.jvmargs=-Xmx3g :app:testDevDebugUnitTest --tests '<pattern>' ktlintCheck` |
| **Full suite command** | `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew -Pkotlin.daemon.jvmargs=-Xmx3g testDevDebugUnitTest testProdDebugUnitTest ktlintCheck assembleDevDebug` then `./gradlew -Dorg.gradle.jvmargs=-Xmx4g -Pkotlin.daemon.jvmargs=-Xmx3g :app:lintDevDebug :app:lintProdDebug` then `bash scripts/check-design-rules.sh && bash scripts/check-design-rules-selftest.sh` |
| **Estimated runtime** | ~600 seconds (full); ~120 seconds (quick) |

---

## Sampling Rate

- **After every task commit:** Run the quick run command with the touched test classes
- **After every plan wave:** Run the full suite command
- **Before `/gsd-verify-work`:** Full suite must be green on the exact build commit
- **Max feedback latency:** 120 seconds

---

## Per-Task Verification Map

Seeded from 17-RESEARCH.md § Validation Architecture; task IDs are filled once plans exist.

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| TBD | TBD | TBD | POL-02 (note) | — | N/A | unit | `--tests '*BookingNoteViewModelTest' --tests '*BookingNoteScreenTest'` | ❌ W0 | ⬜ pending |
| TBD | TBD | TBD | POL-02 (feedback) | — | `toString` stays redacted | unit | `--tests '*FeedbackViewModelTest'` | ❌ W0 | ⬜ pending |
| TBD | TBD | TBD | POL-02 (search) | — | N/A | unit | `--tests '*SearchExpertViewModelTest'` | ❌ W0 | ⬜ pending |
| TBD | TBD | TBD | BUG-22 | — | N/A | Robolectric UI | `--tests '*MeetingCallScreen*'` | ❌ W0 | ⬜ pending |
| TBD | TBD | TBD | BUG-34 | — | N/A | Robolectric UI | `--tests '*MeetingCallScreen*'` | ❌ W0 | ⬜ pending |
| TBD | TBD | TBD | BUG-10 regression | — | N/A | Robolectric UI | `--tests '*MeetingCallScreenEndButtonRegressionTest'` | ✅ | ⬜ pending |
| TBD | TBD | TBD | REL-07 build | — | Signed with upload key; cert pin present | script | ledger B3–B4 commands | ✅ | ⬜ pending |
| TBD | TBD | TBD | Store assets | — | N/A | script | `sips -g pixelWidth -g pixelHeight -g hasAlpha <file>` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] Restore tests for `BookingNoteViewModel`, `FeedbackViewModel`, `SearchExpertViewModel` (pattern: `WriteReviewViewModelTest`)
- [ ] Reachability tests for BUG-22 and BUG-34 (fail first)
- [ ] `17-RELEASE-CHECKLIST.md` with "Check:" lines for every human step
- [ ] Asset spec validator commands (`sips`) in the checklist

*No new framework install needed.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Each Console declaration saved/complete (data safety, FGS, deletion URL, content rating, app access, target audience, listing) | REL-06 / REL-04 | Play Console has no API for App content | "Check:" line per item in `17-RELEASE-CHECKLIST.md` |
| Deletion page live | REL-06 | User publishes on uptrendinvestments.net | `curl -sI <url>` returns 200; page names "ClickMe" and `support@clickme.app` |
| Closed test 12+ testers × 14 days, production access granted | REL-07 | Google-side process | Dashboard state logged in checklist |
| Fresh Play install: client signs up, books free topic, joins call with iOS expert (closed track, then public listing) | REL-07 | Two real devices + production backend | Runbook rows in checklist; result logged |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 120s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending

---
phase: 16-cross-platform-qa
verified: 2026-11-05T23:00:00Z
status: passed
score: 6/6 must-haves verified
covered_files:
  - .planning/phases/16-cross-platform-qa/16-01-PLAN.md
  - .planning/phases/16-cross-platform-qa/16-01-SUMMARY.md
  - .planning/phases/16-cross-platform-qa/16-02-PLAN.md
  - .planning/phases/16-cross-platform-qa/16-02-SUMMARY.md
  - .planning/phases/16-cross-platform-qa/16-03-PLAN.md
  - .planning/phases/16-cross-platform-qa/16-03-SUMMARY.md
  - .planning/phases/16-cross-platform-qa/16-04-PLAN.md
  - .planning/phases/16-cross-platform-qa/16-04-SUMMARY.md
  - .planning/phases/16-cross-platform-qa/16-05-PLAN.md
  - .planning/phases/16-cross-platform-qa/16-05-SUMMARY.md
  - .planning/phases/16-cross-platform-qa/16-06-PLAN.md
  - .planning/phases/16-cross-platform-qa/16-06-SUMMARY.md
  - .planning/phases/16-cross-platform-qa/16-07-PLAN.md
  - .planning/phases/16-cross-platform-qa/16-07-SUMMARY.md
  - .planning/phases/16-cross-platform-qa/16-08-PLAN.md
  - .planning/phases/16-cross-platform-qa/16-08-SUMMARY.md
  - .planning/phases/16-cross-platform-qa/16-BUG-LOG.md
  - .planning/phases/16-cross-platform-qa/16-BUILD-LEDGER.md
  - .planning/phases/16-cross-platform-qa/16-MATRIX-RUNBOOK.md
  - .planning/phases/16-cross-platform-qa/16-REVIEW.md
  - app/build.gradle.kts
  - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/AgoraEventMapping.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/AgoraManager.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/navigation/HomeTabScaffold.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/MeetingCallScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/PaidFlowStripeHost.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/payments/ClientTransactionRow.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/subviews/ExpertPastCard.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/subviews/ExpertUpcomingCard.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/overview/SignupOverviewScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/profilephoto/PhotoCropScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/review/SignupReviewScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/splash/SplashScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/stripe/ClickMePaymentSheetStyle.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/stripe/StripeFailureLog.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/stripe/StripeResultMapping.kt
covered_digest: "v3:sha256:3eedb3ad5f10a5077336cf2e2e041f6387afe33e43958d5dc5e816345160a0c2"
behavior_unverified: 0
overrides_applied: 6
overrides:
  - must_have: "Stripe test cards including 3DS pass in both directions (live 3DS challenge)"
    reason: "No live 3DS challenge was passed on Android or iOS. The user's cards showed no setup challenge, so the P0.9 plan (changed by the user 2026-11-02) accepted not running D1.21 and D2.11; D1.22 (declining card) also not run. Confirm-time Stripe paths that need no challenge passed (D1.18-D1.20, D1.23, D2.09-D2.12). Phase 7 SC-4 stays unproven live."
    accepted_by: "user (Leon Chen), runbook Section 8 option-confirm"
    accepted_at: "2026-11-05"
  - must_have: "Payout onboarding is exercised in both directions (D-05: to the onboarding link)"
    reason: "D2.14 and D2.15 not run. The Android expert account was already onboarded, there is no Set up payouts entry, and a new Connect account is forbidden by D-05. D2.16 (forged return intent) passed as weak proof only."
    accepted_by: "user (Leon Chen), runbook Section 8 option-confirm"
    accepted_at: "2026-11-05"
  - must_have: "Every paid-booking capture is refunded (D-04)"
    reason: "Two CA$5.00 captures (C3eR3Z, See2oK) were kept, not refunded. The user owns both accounts and the platform. All 8 holds were voided and 0 matrix PaymentIntents are in requires_capture. The application fee was not refunded."
    accepted_by: "user (Leon Chen), runbook Section 8 option-confirm"
    accepted_at: "2026-11-05"
  - must_have: "Tier-1 TalkBack rows pass on a device (POL-04, Phase 15 SC-4)"
    reason: "All 23 TalkBack rows plus FS.Y12 (Accessibility Scanner) were skipped by the user. Accessibility semantics are proven by Robolectric tests only. 15-UAT tests 1, 3 and 4 stay unconfirmed on a device."
    accepted_by: "user (Leon Chen), runbook Section 8 option-confirm"
    accepted_at: "2026-11-05"
  - must_have: "A wider group of real testers on a mix of devices (D-10, REL-05)"
    reason: "The user is the only internal tester. Carries Phase 13 D-22. The emulator covered Android 17 only and EM.1 was not run."
    accepted_by: "user (Leon Chen), runbook Section 8 option-confirm"
    accepted_at: "2026-11-05"
  - must_have: "Open iOS-owned blocking bug BUG-25 (iOS client cannot answer an expert reschedule) does not block Android"
    reason: "Reported to iOS as critical. User decision: Android ships on its own. Also accepted as non-passing rows: D1.05 (BUG-13, backend, Read label not live), SW.1 (BUG-24, ios, push token row left after logout), FS.C3 (BUG-34, android, call controls unreachable at font 200%, fix-if-cheap, post-release), plus never-run rows listed in Section 8."
    accepted_by: "user (Leon Chen), runbook Section 8 option-confirm"
    accepted_at: "2026-11-05"
---

# Phase 16: Cross-Platform QA Verification Report

**Phase Goal:** The Android app interoperates with the iOS app in both directions on every money and session path.
**Verified:** 2026-11-05
**Status:** passed (with six user-accepted deviations, listed in `overrides:` above; none are hidden passes)
**Re-verification:** No, initial verification

## Basis of this verdict

Phase 16 is a device-QA phase. Almost all of the evidence is user-reported device results recorded in `16-MATRIX-RUNBOOK.md`. Claude's checks cannot re-run a live-money or two-phone matrix. What I could independently check in the repo: the plan and requirement coverage, the bug log's internal consistency, the fix commits (they exist and are on `main`), the shipped `versionCode`, the build ledger, and the sign-off line. Per D-09 the requirement closes on the user's word, and the user confirmed on 2026-11-05 (option-confirm, Section 8). The user's sign-off covers the named gaps. I report them as accepted deviations, not as passes.

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | SC-1 / QA-01: Every bug from internal testing is triaged, and the release-blocking ones are fixed and verified | VERIFIED | `16-BUG-LOG.md` holds BUG-01..BUG-34, each with owner, severity and a terminal status. Only two bugs carry a `blocking (...)` severity. BUG-05 (android, paid booking dropped after the card sheet) is `verified`: fix `f94d6bbf` is a real commit on `main`, retested on vc 4 (paid booking reached Request Sent, Stripe Uncaptured). BUG-25 (ios, iOS client cannot answer a reschedule) is `reported (ios)`; user decision: does not block Android (override 6). Android-owned blocking bugs open: 0. Every android fix commit in the log exists (`git cat-file -t` returns commit for all 12 sampled shas, and `d5fd82d9` and `f94d6bbf`/`f15d472c`/`e928cc8e` are ancestors of `main`). Non-blocking android items BUG-22, BUG-23, BUG-34 end as `post-release`. |
| 2 | SC-2 / QA-02: With an Android client and an iOS expert the matrix passes: cards (incl. 3DS), push from a killed app, background call and rejoin, payout onboarding | VERIFIED (with overrides 1, 2, 3, 6) | D1.* counts: pass 64, fail 1, not run 6 (71 rows). Pass on a signed Play build: paid hold, cancel, retry and confirm (D1.15-D1.20, D1.23; BUG-05/06 retested), push from a killed app routes to the right screen (P0.5, D1.01, D1.02, D1.04, batch-B smoke H6), 5+ minute background call (D1.09, D1.11), hang-up and rejoin (D1.10, retested after BUG-09), paid session end to end (D1.25). Fail: D1.05 (backend, Read label not live; server saved it). Not run: D1.21/D1.22 (no live 3DS challenge, no failing card), D1.12, D1.24 part 2 and D1.RS5 (unreachable live), D1.PH5. Payout onboarding D2.14/D2.15 not run (account already onboarded, D-05). |
| 3 | SC-3 / QA-03: With an iOS client and an Android expert the same matrix passes | VERIFIED (with overrides 1, 2, 6) | SW.* and D2.* counts: pass 35, fail 2, not run 3 (40 rows). Pass: expert accept/decline in the same call (D2.03, D2.05), background and rejoin (D2.06, D2.07), paid session (D2.12, confirm-connection once per app), held paid booking cancelled (D2.10), account deletion with 409 then delete (D2.17-D2.19), throwaway signup through Publish (D2.17, retested vc 5). Fail: SW.1 (BUG-24, ios, push token row left after logout) and D2.RS1 (BUG-25, ios, an iOS client cannot answer an expert reschedule; user rated critical). Not run: D2.11 (3DS), D2.14, D2.15 (payout onboarding). The iOS-client reschedule-response path is a real interop failure, owned by iOS and accepted as non-blocking to the Android release by the user. |
| 4 | Plan truth (D-08, REL-03): A signed prodRelease build is on the Play internal track and the matrix ran on it | VERIFIED | `16-BUILD-LEDGER.md` Uploads table has vc 1-5, all with "Cert pin match: yes", Play status `available`, and a Samsung proof row showing installer `com.android.vending`. `app/build.gradle.kts:76` has `versionCode = 5`, matching the final Samsung build. The upload certificate pin is recorded. Archive files for vc 5 exist at `~/ClickMe-release/` (aab, apk, mapping). Archive loss of vc 1-4 files is documented in the ledger; sha256 values are retained. |
| 5 | Plan truth (D-11, D-12): One bug log, iOS/backend bugs routed to the report list; sources written back | VERIFIED | `16-BUG-LOG.md` follows the D-11 column set. The ios/backend bugs (BUG-13, 14, 15, 16, 17, 24, 25, 26, 27, 28) are `reported (...)` with "Blocks Android release" tags. Section 8's write-back table maps every deferred STATE item, 09-HUMAN-UAT item, 15-UAT test and 15 gap-list row to a runbook result. |
| 6 | Plan truth (D-09): The user confirmed the matrix in both directions and tier-1 before close | VERIFIED | `16-MATRIX-RUNBOOK.md` Section 8 ends with `Confirmed by the user on 2026-11-05: option-confirm ("confirm")`. Counts, the 3DS outcome, the money line, the accepted gaps and the open-rows list are filled in. Regression gate (orchestrator, today, on `main`): `:app:testDevDebugUnitTest` and `:app:testProdDebugUnitTest` 5276 tests each, 0 failures, ktlintCheck green. |

**Score:** 6/6 truths verified (0 behavior-unverified). Truths 2 and 3 are verified only to the extent the user's device-run results and sign-off support them; the six `overrides:` entries record exactly what falls short of the literal requirement text.

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `16-MATRIX-RUNBOOK.md` (759 lines) | Ordered runbook with filled Results and Section 8 sign-off | VERIFIED | Sections 0-8 present; sign-off line present. |
| `16-BUG-LOG.md` | D-11 bug log with triage | VERIFIED | 34 rows, every row has an owner, severity and terminal status. |
| `16-BUILD-LEDGER.md` | Build ledger, policy, uploads vc 1-5, fix batches A and B | VERIFIED | Present with cert pin. |
| `16-REVIEW.md` | Code review of fix commits | VERIFIED | 0 critical, 3 warnings, 5 info. |
| `app/build.gradle.kts` | Hand-bumped `versionCode` | VERIFIED | `versionCode = 5`, matches the last upload. |
| Fix sources (15 files in `covered_files`) | BUG-05..12, 29, 30, 31 fixes | VERIFIED (exists) | No uncommitted changes under `app/`. `isPostCompletionReset` is defined in `StripeResultMapping.kt`, called in `PaidFlowStripeHost.kt:66`, and covered by `StripeResultMappingTest.kt`. |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| Runbook failed rows | Bug log | Result cell `[!] fail (BUG-NN, vc N)` | WIRED | Each failed or open row in Section 8 names a BUG id that exists in the log (D1.05 -> BUG-13, SW.1 -> BUG-24, D2.RS1 -> BUG-25, FS.C3 -> BUG-34). |
| Bug log fix commit | Repo | sha | WIRED | All sampled shas resolve to commits; the fix commits for BUG-05, BUG-09, BUG-31 and the vc 5 bump are on `main`. |
| Ledger vc 5 | `build.gradle.kts` | versionCode | WIRED | 5 = 5. |
| Section 8 write-back | STATE / 15-DEVICE-CHECKLIST / 15-UAT | Write-back table | WIRED per 16-08 summary; not re-diffed here. |

### Data-Flow Trace (Level 4)

Not applicable. The phase delivers verification evidence and a bounded set of bug fixes, not new rendering features.

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| versionCode matches final upload | `grep versionCode app/build.gradle.kts` | `versionCode = 5` | PASS |
| Fix commits exist and are on main | `git cat-file -t`, `git merge-base --is-ancestor` | all commit / ancestor | PASS |
| Debt markers in modified sources | `grep -E "TBD\|FIXME\|XXX"` on the 15 reviewed files | none | PASS |
| Full unit-test and ktlint gate | run by the orchestrator today (not re-run here) | 5276 tests per flavor, 0 failures, ktlint green | PASS (reported) |

### Probe Execution

SKIPPED: the phase declares no `probe-*.sh` scripts.

### Requirements Coverage

| Requirement | Source Plans | Description | Status | Evidence |
|-------------|--------------|-------------|--------|----------|
| QA-01 | 16-01, 02, 04, 05, 06, 07, 08 | Internal-testing bugs triaged, release-blocking ones fixed | SATISFIED | Truth 1. Blocking android bug BUG-05 fixed and device-verified. Blocking iOS bug BUG-25 reported to iOS, user decided Android ships on its own. |
| QA-02 | 16-01, 02, 03, 04, 08 | Android client + iOS expert matrix passes (cards incl. 3DS, killed-app push, background call and rejoin, payout onboarding) | SATISFIED by user acceptance (D-09), with accepted deviations | Truth 2. Not met in the literal text: live 3DS challenge and payout onboarding were not run (overrides 1, 2). D1.05 fail (backend). |
| QA-03 | 16-01, 02, 03, 06, 08 | iOS client + Android expert matrix passes | SATISFIED by user acceptance (D-09), with accepted deviations | Truth 3. D2.RS1 and SW.1 failed (iOS-owned); D2.11, D2.14, D2.15 not run. |

All three requirement IDs (QA-01, QA-02, QA-03) appear in PLAN frontmatter and in `REQUIREMENTS.md` (lines 196-198, traceability rows 349-351, currently `Pending`). No orphaned requirements are mapped to Phase 16 beyond these. The orchestrator should flip the three checkboxes and the traceability rows to Complete when it commits.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `ClientTransactionRow.kt`, `ExpertPastCard.kt` | 79-90, 100-101 | Booking ID (BUG-29) added visually but missing from the explicit TalkBack description (16-REVIEW WR-01) | Warning | Accessibility regression on two row types. Deferred to the next fix batch. TalkBack is itself an accepted unconfirmed gap. |
| `SignupOverviewScreen.kt` | 168-172 | `heightIn(min = 56.dp)` ordered before the inset and padding, so it is inert (WR-02) | Warning | Button is a few dp smaller than intended; no functional effect. Deferred. |
| `MeetingCallScreen.kt` / `MeetingCallScreenTest.kt` | 107-170 / 234-257 | No test proves call controls can be reached above the pinned End Call (WR-03, IN-01) | Warning | Explains BUG-22 and BUG-34 (controls unreachable at font 200%). User decided non-blocking; post-release. |
| (all reviewed sources) | | TBD/FIXME/XXX | None | None found. |

No blocker anti-patterns. Code review: 0 critical.

### Human Verification Required

None added by this verification. The user already ran or explicitly accepted as gaps every item that would otherwise appear here (TalkBack, live 3DS, payout onboarding, wider tester group, large-window and tier 2-4 rows). They are recorded as accepted deviations in `overrides:` and in the runbook's open-rows list, not as new human items.

### Findings Summary

- **BLOCKER:** none.
- **WARNING (accepted by the user, carried forward, not new human items):**
  1. BUG-25 is a real cross-platform failure on a session path: an iOS client cannot answer an Android expert's reschedule proposal. It is iOS-owned, reported as critical, and the user decided it does not block the Android release. Android's own half (an Android client answering an expert reschedule) was user-reported working on vc 5.
  2. BUG-34/BUG-22: at font 200% the pre-join and last-5-minutes call screens hide Mute, Speaker, Minimize and Chat behind End Call. The call itself works. Fix-if-cheap, left post-release by user decision.
  3. Review warnings WR-01..WR-03 (above) are open and deferred to the next batch.
  4. The "including 3DS" and "payout onboarding" parts of the literal QA-02/QA-03 text were not demonstrated live. Phase 7 SC-4 (3DS) therefore has no live proof anywhere in the project. The user accepted this.
  5. Weak-proof passes carried by the runbook (D1.23, D1.25, D2.04, D2.08, D2.16, D2.RS2 and others, listed in Section 8) rest on user report only.
- **Not verifiable by Claude:** the device results themselves. They are taken from the runbook and the user's sign-off, as D-09 provides.

### Gaps Summary

No gaps. Phase 16's goal is achieved to the standard the user set at sign-off (D-09), with the deviations above recorded rather than hidden. Phase 17 may start. Suggested follow-up input for the next fix batch: BUG-22, BUG-34, BUG-23, the review warnings, the BUG-28 Android half once the backend exposes `decline_reason_text`, and the iOS and backend report list (BUG-13, 14, 15, 16, 17, 24, 25, 26, 27, 28).

---

_Verified: 2026-11-05_
_Verifier: Claude (gsd-verifier)_

---
phase: 11-expert-requests-bookings
verified: 2026-10-27T05:00:00Z
status: passed
score: 4/4 must-haves verified
covered_files:
  - .planning/phases/11-expert-requests-bookings/11-01-PLAN.md
  - .planning/phases/11-expert-requests-bookings/11-01-SUMMARY.md
  - .planning/phases/11-expert-requests-bookings/11-02-PLAN.md
  - .planning/phases/11-expert-requests-bookings/11-02-SUMMARY.md
  - .planning/phases/11-expert-requests-bookings/11-03-PLAN.md
  - .planning/phases/11-expert-requests-bookings/11-03-SUMMARY.md
  - .planning/phases/11-expert-requests-bookings/11-04-PLAN.md
  - .planning/phases/11-expert-requests-bookings/11-04-SUMMARY.md
  - .planning/phases/11-expert-requests-bookings/11-05-PLAN.md
  - .planning/phases/11-expert-requests-bookings/11-05-SUMMARY.md
  - .planning/phases/11-expert-requests-bookings/11-06-PLAN.md
  - .planning/phases/11-expert-requests-bookings/11-06-SUMMARY.md
  - .planning/phases/11-expert-requests-bookings/11-07-PLAN.md
  - .planning/phases/11-expert-requests-bookings/11-07-SUMMARY.md
  - .planning/phases/11-expert-requests-bookings/11-08-PLAN.md
  - .planning/phases/11-expert-requests-bookings/11-08-SUMMARY.md
  - .planning/phases/11-expert-requests-bookings/11-09-PLAN.md
  - .planning/phases/11-expert-requests-bookings/11-09-SUMMARY.md
  - .planning/phases/11-expert-requests-bookings/11-10-PLAN.md
  - .planning/phases/11-expert-requests-bookings/11-10-SUMMARY.md
  - .planning/phases/11-expert-requests-bookings/11-11-PLAN.md
  - .planning/phases/11-expert-requests-bookings/11-11-SUMMARY.md
  - .planning/phases/11-expert-requests-bookings/11-12-PLAN.md
  - .planning/phases/11-expert-requests-bookings/11-12-SUMMARY.md
  - .planning/phases/11-expert-requests-bookings/11-13-PLAN.md
  - .planning/phases/11-expert-requests-bookings/11-13-SUMMARY.md
  - .planning/phases/11-expert-requests-bookings/11-14-PLAN.md
  - .planning/phases/11-expert-requests-bookings/11-14-SUMMARY.md
  - .planning/phases/11-expert-requests-bookings/11-15-PLAN.md
  - .planning/phases/11-expert-requests-bookings/11-15-SUMMARY.md
  - .planning/phases/11-expert-requests-bookings/11-16-PLAN.md
  - .planning/phases/11-expert-requests-bookings/11-16-SUMMARY.md
  - .planning/phases/11-expert-requests-bookings/11-17-PLAN.md
  - .planning/phases/11-expert-requests-bookings/11-17-SUMMARY.md
  - .planning/phases/11-expert-requests-bookings/11-18-PLAN.md
  - .planning/phases/11-expert-requests-bookings/11-18-SUMMARY.md
  - app/build.gradle.kts
  - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/AgoraEngineConfig.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/AgoraManager.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/cancellation/ExpertCancellationViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/confirmed/BookingConfirmedScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/confirmed/CalendarIntent.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/ExpertDashboardScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/ExpertDashboardViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/home/HomeExpertScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/incomingrequests/IncomingRequestsViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/RequestDecisionViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/ExpertBookingsViewModel.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/managers/AgoraEngineConfigTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/managers/AgoraManagerCreateEngineTest.kt
  - gradle/libs.versions.toml
covered_digest: "v3:sha256:f9edc91e0ec677e802eaecd83ecb370a4bdb03f77ffc473bcf2d41a597affbd1"
behavior_unverified: 0
overrides_applied: 0
re_verification:
  previous_status: human_needed
  previous_score: 3/4
  gaps_closed:
    - "SC-3 two-sided Android expert and iOS client call: confirmed on device (11-UAT test 7, 2026-10-27) after the server-side channel fix 11-18 (G-11-6). Truth 3 moves from PRESENT_BEHAVIOR_UNVERIFIED to VERIFIED."
    - "G-11-1, G-11-5, G-11-6 resolved by 11-16, 11-17, 11-18; 11-UAT.md is now status: complete."
  gaps_remaining: []
  regressions: []
advisory:
  - finding: "11-REVIEW incremental IN-02: the INFO remote-presence log lines in AgoraManager ship in release builds (redacted, a G-11-5 debug aid). Gate on BuildConfig.DEBUG or remove now that G-11-5/G-11-6 are closed."
    category: other
    reason: "Redacted and non-blocking; cleanup only."
    evidence_status: "none provided"
  - finding: "11-REVIEW incremental IN-01/IN-03 and 11-16 IN-02, plus initial-review WR-01..WR-08 (notably WR-08: backend discards the optional accept message) remain open in 11-REVIEW-DISPOSITION.md."
    category: other
    reason: "Advisory review findings; none undermines a must-have."
    evidence_status: "none provided"
coincidental_reliance_items:
  - truth: "SC-3: expert joins and both sides meet in the same Agora call"
    reason: undeclared-precondition
    harden: "Cross-platform channel agreement depends on the backend returning/signing the lowercase booking.id. Android and iOS never agree on the channel by themselves (iOS sends the UUID uppercase, Android lowercase). Nothing on the Android side asserts it. The invariant is declared only in the backend (call.controller.js line 111 `generateToken(booking.id, ...)`, agora.js `toLowerCase`) and its tests (48 pass). Keep the backend gate in place, and consider an Android-side contract test or log that channel_name is lowercase."
---

# Phase 11: Expert Requests & Bookings Verification Report

**Phase Goal:** An Android expert can see their workload, accept requests, manage bookings and join sessions, so the expert side works end to end.
**Verified:** 2026-10-27T05:00:00Z
**Status:** passed
**Re-verification:** Yes. The previous report (human_needed, 3/4) was waiting on the two-sided device call for SC-3. That evidence now exists. Everything else was re-checked against HEAD (`94f1c5c`).

## What changed since the last report

- **No Android code changed.** `git diff --stat 4c3e713..HEAD -- app gradle` is empty, and the last commit touching `app/` is `2026-10-26 20:49:51 -0400`. The previous report's truths 1, 2, 4 and the Agora code evidence therefore still apply unchanged.
- **11-18 (G-11-6) is a backend-only change.** I checked the backend repo directly rather than the SUMMARY:
  - `/Users/LeonChen/Documents/ClickMe2026 Backend` has `93d8b15`, `a2c4fee`, `92ac0e4`, `4c694e7`. `git diff --stat f686d27..4c694e7 -- src` touches only `src/controllers/call.controller.js` and `src/services/agora.js`.
  - `call.controller.js` uses the URL param (`req.params.id`) only for the lookup (lines 50-58); the token and echoes come from `booking.id` (line 111 `generateToken(booking.id, userId, sessionEnd)`, lines 100, 116, 181, 192, 289 echo `booking.id`). `agora.js` line 87 `const channelName = String(bookingId).toLowerCase()`.
  - `npm test -- --silent tests/call.test.js` run now: **48 passed, 48 total**.
  - Production `https://clickme-backend-production.up.railway.app/health` returns `{"status":"success","commit":"4c694e7"}`, equal to the backend head. The fix is deployed.
- **The device evidence the prior report waited for exists.** `11-UAT.md` is `status: complete`. Test 7 (two-sided Android expert and iOS client, after 11-18, build installed 21:39:38, later than the last app commit 20:49:51) is `result: pass`, logcat `conn state=3 reason=1`, `remote joined elapsed=148`, `remote audio state=2 reason=6`. G-11-1, G-11-5 and G-11-6 are each `status: resolved` with a `resolved_by` plan that has a SUMMARY. The "3 issues" in the UAT summary are the historical failed attempts (tests 1, 5, 6); each is closed by a gap row and by test 7.
- **Security.** `11-SECURITY.md`: `status: verified`, `threats_open: 0`. It audited `b2042e8` plus backend `4c694e7`, which includes every Phase 11 code change (HEAD has no later `app/` change).

## Goal Achievement

### Observable Truths

| # | Truth (ROADMAP success criterion) | Status | Evidence |
|---|-----------------------------------|--------|----------|
| 1 | Dashboard shows pending request count, upcoming sessions, weekly earnings text and a payout onboarding card | VERIFIED | `ExpertDashboardViewModel` and `ExpertDashboardScreen` exist and are unchanged since the last verification; both are in the covered fingerprint. Targeted `*Expert*` unit tests pass (run below). UAT test 3 passed on device: pending count, reschedule banner, Today's Sessions with Join, weekly earnings, payout card with Stripe Custom Tab, 200% font and TalkBack. |
| 2 | Expert sees an iOS client's request in Incoming Requests, accepts with an optional message or declines with a reason code, sees Confirmed, adds to calendar | VERIFIED | `IncomingRequestsViewModel`, `RequestDecisionViewModel`, `BookingConfirmedScreen` and `CalendarIntent.kt` (ACTION_INSERT) are present, unchanged, and registered through `HomeExpertScreen`. UAT test 3 passed (Add to Calendar editor opens with correct title, times, description). Caveat as before: backend discards the optional accept message (11-REVIEW WR-08, a backend behavior, open). |
| 3 | Expert Bookings (Upcoming/Past) shows Join inside the join window and the expert can join the call | VERIFIED | Join UI, gating and call start unchanged. The three failure layers are each fixed and each tied to evidence. G-11-1: engine NPE fixed by 11-16; device logcat then shows `conn state=3 reason=1` (UAT test 5 evidence). G-11-5: `buildRtcEngineConfig` sets `mChannelProfile = CHANNEL_PROFILE_COMMUNICATION` (read in `AgoraEngineConfig.kt` line 36), JVM-tested (`AgoraEngineConfigTest`, `AgoraManagerCreateEngineTest`). G-11-6: server now signs and returns the lowercase `booking.id` (backend code and 48 tests re-checked, deployed `4c694e7`). Behavioral proof: UAT test 7, a live two-sided Android expert plus iOS client call on device, logcat `remote joined elapsed=148` and `remote audio state=2`. `AgoraManager.kt` line 90-91 logs and emits `EngineEvent.RemoteJoined`, which `CallCenter.kt` line 315 maps to `remote = ConnState.Ready`, so the UI leaves "waiting for the other person". This was directly observed behavior, not presence alone. See Human Verification note for the part supported only by user confirmation. |
| 4 | Expert can accept or decline a client's reschedule request, sees a clear message on `409 SLOT_UNAVAILABLE`, and can cancel | VERIFIED | `ExpertBookingSummaryViewModel` and `ExpertCancellationViewModel` present, unchanged. UAT test 2 passed on device: accept/decline of an iOS-proposed reschedule, "That time is no longer available" on back-to-back bookings, free and held-paid cancellation to the Past tab with the snackbar. |

**Score:** 4/4 truths verified (0 present, behavior-unverified).

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `managers/AgoraEngineConfig.kt` | COMMUNICATION engine profile, `buildChannelMediaOptions` | VERIFIED | Read at HEAD: `mContext`, `mAppId`, `mEventHandler`, `mChannelProfile = COMMUNICATION` set by plain assignment (no scope function); join options COMMUNICATION, broadcaster, mic published, autoSubscribe. |
| `managers/AgoraManager.kt` | join on options builder, redacted presence logs, `RemoteJoined` emit | VERIFIED | Lines 90-91 and 146-148 confirmed. |
| `AgoraEngineConfigTest.kt`, `AgoraManagerCreateEngineTest.kt` | profile, presence, setup-failure cleanup, destroy-throws | VERIFIED | Pass in the run below. |
| `screens/expertmode/*` | Dashboard, Incoming Requests, Decision, Confirmed, Upcoming/Past, Summary, Cancellation | VERIFIED | All directories present; fingerprinted files unchanged. |
| Backend `call.controller.js`, `agora.js`, `tests/call.test.js` | canonical lowercase channel | VERIFIED | Cross-repo, re-checked above; 48/48 pass; deployed. |
| `11-VALIDATION.md` | G-11-5/G-11-6 rows and approval line | VERIFIED | Added by 11-18 (11-18-SUMMARY self-check). Not re-opened this run. |

### Key Link Verification

| From | To | Via | Status |
|------|----|-----|--------|
| Join (Dashboard, Bookings, Summary) | `CallStarter.startCall` to `CallEngine.join` | single path | WIRED |
| `AgoraManager.join` | `buildChannelMediaOptions()` / `rtc.joinChannel` | lines 146-148 | WIRED |
| `AgoraManager.createEngine` | `buildRtcEngineConfig` (COMMUNICATION) | factory receives config; test-proven | WIRED |
| handler `onUserJoined` | `CallCenter` `remote = Ready` | `EngineEvent.RemoteJoined` (AgoraManager:91, CallCenter:315) | WIRED (device-proven, test 7) |
| Android join request | Backend `join` | token and channel from `booking.id`, lowercased | WIRED (backend tests + device) |

### Data-Flow Trace (Level 4)

Phase 11 screens render data from repository calls verified in the earlier verification; the diff since then is empty. UAT tests 2 and 3 confirm real data on device (pending count, earnings, reschedule banner, today's sessions). Status: FLOWING.

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Agora, call, expert unit tests | `./gradlew --offline :app:testDevDebugUnitTest --tests '*AgoraManagerCreateEngineTest' --tests '*AgoraEngineConfigTest' --tests '*CallCenter*' --tests '*Expert*'` (JBR JAVA_HOME) | exit 0, no failures | PASS |
| Backend canonical channel | `npm test -- --silent tests/call.test.js` in the Backend repo | 48 passed, 48 total | PASS |
| Backend deployed | `curl https://clickme-backend-production.up.railway.app/health` | commit `4c694e7` | PASS |
| Full regression gate | `./gradlew --offline :app:testDevDebugUnitTest` | Caller reported 2961 tests, 0 failures/errors/skipped on HEAD (no `app/` change since). | NOT RE-RUN HERE (caller-reported; targeted subset re-run PASS) |
| Native Agora join, remote presence | device and iOS peer | UAT test 7 pass with logcat evidence | PASS (human-observed) |

### Probe Execution

No probes declared by the phase. Skipped.

### Requirements Coverage

Every requirement ID in the PLAN frontmatter (EXP-05 to EXP-09, declared across plans 11-01 to 11-18) appears in REQUIREMENTS.md lines 123-127 (all `[x]`) and in its traceability table lines 314-318 (`Phase 11 | Complete`). No other REQUIREMENTS.md ID maps to Phase 11, so nothing is orphaned.

| Requirement | Source plans | Status | Evidence |
|-------------|--------------|--------|----------|
| EXP-05 | 11-01, 02, 03, 10, 14, 15 | SATISFIED | Truth 1, UAT test 3 |
| EXP-06 | 11-01, 02, 03, 05, 06, 07, 12, 15 | SATISFIED | Truth 2. Optional accept message discarded by the backend (WR-08, open, not a client defect). |
| EXP-07 | 11-01, 02, 07, 15 | SATISFIED | `CalendarIntent.kt` ACTION_INSERT; UAT test 3 |
| EXP-08 | 11-01 to 04, 08, 10, 11, 15, 16, 17, 18 | SATISFIED | Truth 3; UAT test 7 two-sided call. The previous caveat (ahead of evidence) is closed. |
| EXP-09 | 11-01, 02, 03, 08, 09, 13, 15 | SATISFIED | Truth 4, UAT test 2 |

### Prohibitions (must_haves.prohibitions)

| Prohibition | Tier | Result |
|-------------|------|--------|
| 11-17: MUST NOT log uid, RTC token, channel name or exception messages from AgoraManager or helpers | test | Resolved with enforcement evidence: `assertNoSentinelsLogged` across `AgoraManagerCreateEngineTest` (passes). Not flagged. |
| 11-15: MUST NOT log client messages, reasons, names or the Stripe URL; MUST NOT expose deferred controls | test | UAT test 4 passed (human review). The code is unchanged since. Not flagged. |
| 11-18: server MUST NOT sign or echo URL-derived channel text | test | Enforced by backend tests G-11-6 (a)-(h) (48/48 pass, re-run here). Not flagged. |

### Anti-Patterns Found

| File | Pattern | Severity | Impact |
|------|---------|----------|--------|
| Phase 11 sources and changed tests | `TBD` / `FIXME` / `XXX` | None | Debt-marker gate clear (checked in the prior verification; no `app/` change since). |
| `AgoraManager.kt` | INFO presence logs in release | Info (advisory IN-02) | Redacted, non-blocking. |

### Human Verification Required

None open for Phase 11. UAT is complete.

Notes for the record, none of which gate this phase:

- Test 7's logcat evidence covers connection, remote join and remote audio decoding (`remote audio state=2`). Two-way audio, mute, speaker and leave/rejoin rest on the user's "confirmed pass" and are not separately logged. I accept the user's observation as the human evidence for SC-3.
- **09-HUMAN-UAT items 1, 2, 5 are only partly covered by test 7, and I do not consider them cleared.**
  - Item 1 (join against a live iOS expert, mic permission dialogs, external link) was about an Android client joining an iOS expert; test 7 is the reverse direction and did not exercise the mic dialogs or external link.
  - Item 2 (5+ minutes backgrounded, screen off, notification checks) has no evidence; the call in test 7 was short.
  - Item 5 (peer hang-up shows "waiting" without ending, rejoin, amber state at 5:00) has no explicit evidence.
  - `09-HUMAN-UAT.md` is still `status: partial` with 8 items pending. They belong to Phase 9 (voice call) and do not gate Phase 11. Run or schedule them there. Test 7 shows the underlying cross-platform call now works, so they are no longer blocked.

### Gaps Summary

No gaps. All four roadmap success criteria and all five requirements are implemented, wired, test-covered and, for the two device-dependent flows (SC-3 call, SC-4 reschedule/cancel), confirmed on a Samsung device against the production backend and an iOS peer. The only prior blocker, the cross-client call, was a three-layer problem (engine NPE, channel profile, channel-name case split). The first two fixes were in Android; the third was in the backend and is deployed.

**Coincidental-reliance note (advisory, no effect on score or status).** SC-3 holds only because the backend canonicalizes the channel to lowercase. The Android and iOS clients do not agree by themselves. If the backend change were reverted, the bug would silently return, and no Android test would catch it. This is a cross-repo precondition declared in backend code and tests but not asserted on the Android side.

**Open non-blocking items.** 11-REVIEW incremental IN-01, IN-02, IN-03 and 11-16 IN-02; initial-review WR-01 to WR-08 and IN-01 to IN-06 (11-REVIEW-DISPOSITION.md). IN-02 (release-build presence logging) is worth a quick cleanup. WR-08 is a backend gap (optional accept message discarded).

---

_Verified: 2026-10-27T05:00:00Z_
_Verifier: Claude (gsd-verifier)_

---
phase: 16-cross-platform-qa
plan: 02
subsystem: release
tags: [release, signing, play-console, internal-track, r8, ledger, tracer]
requires:
  - phase: 13-push-notifications-internal-testing
    provides: release checklist, Firebase google-services.json, backend D-04 deploy
provides:
  - Signed prodRelease versionCode 1 (0.1.0) AAB and APK, verified against the pinned upload certificate and archived in /Users/LeonChen/ClickMe-release/
  - 16-BUILD-LEDGER.md with the versionCode policy, the reusable build/check/archive/upload/prove and fix-batch procedures, and Uploads row 1 marked available
  - R8 cold start of the signed APK on the Samsung SM_A075F (ok, 783 ms, 0 crash lines)
  - versionCode 1 on the Play internal testing track, installed from Google Play on the Samsung and logged in as account B
affects: [16-03, 16-04, 16-05, 16-06, 16-07, 16-08, 17]
tech-stack:
  added: []
  patterns: [hand-bumped versionCode literal with one ledger row per upload, pinned upload-cert SHA-256 checked on every AAB]
key-files:
  created:
    - .planning/phases/16-cross-platform-qa/16-BUILD-LEDGER.md
  modified:
    - .planning/STATE.md
key-decisions:
  - "Gate B2 was reused from the orchestrator run at 2a532a0b: every later commit touches only .planning/ (verified by git diff --name-only), so the gate was not re-run"
  - "No R8 keep rule was needed; app/src/main/keepRules/rules.keep is unchanged"
  - "Realtime connection proof is left to runbook rows R1 and D1 because the planned logcat check is not implementable (see Deviations)"
patterns-established:
  - "Play app-signing cert fingerprints recorded in the ledger Notes for Firebase registration"
requirements-completed: [QA-01, QA-02, QA-03]
actuals:
  tokens: 9000
  tasks: 3
  commits: 3
plan_head_before: d31aab188972264a57babb3ec737b196327e2f4f
plan_head_after: 9c7010083481b45054c279f597ddb4365c192561
duration: about 1 day elapsed (Play review wait), roughly 40 minutes of executor work
completed: 2026-10-30
status: complete
---

# Phase 16 Plan 02: Signed Release and Internal Track Summary

**Signed prodRelease versionCode 1 (0.1.0) built, verified against the pinned upload certificate, cold-started under R8 on the Samsung, uploaded to Play internal testing and installed from Google Play as account B.**

## Accomplishments

- Built the signed AAB and APK from the main checkout; jarsigner, apksigner, the AAB and APK signer SHA-256, aapt2 badging (versionCode 1, versionName 0.1.0) and google_app_id all checked; no .jks or keystore.properties is tracked. Archived to /Users/LeonChen/ClickMe-release/ with sha256 sums and the R8 mapping file.
- Wrote 16-BUILD-LEDGER.md: versionCode policy, pinned upload certificate, procedures B1-B8 and F1-F8, Uploads row 1.
- R8 cold start of the signed APK on SM_A075F: Status ok, TotalTime 783 ms, 0 crash lines, Login screen visible. No keep rule needed.
- User uploaded the bundle to Play internal testing; versionCode 1 is available. The Samsung runs it from Google Play (installer com.android.vending, versionCode 1, lastUpdateTime 2026-10-30 17:57:21, crash buffer 0), logged in as B. The Play console upload-key certificate matches the pin (user confirmed).
- STATE.md: Phase 13 release steps (1)-(6) and (8) ticked with notes, (7), (9), (10) left open with their notes; Phase 2 deferrals for 02-15 and 02-16 closed by this plan, 02-09 marked replaced by Phase 16 rows D1.15-D1.23; D-22 line extended with the Phase 16 D-10 accepted gap.

## Task Commits

1. **Task 1: Signed versionCode 1 build, verified, archived, ledgered** - `7fd2ea44` (docs)
2. **Task 2: R8 cold start of the signed APK on the Samsung** - `0f72b253` (docs)
3. **Task 3: Internal track upload, Play install, log in as B** - `9c701008` (docs)

**Plan metadata:** the commit `docs(16-02): complete signed release and internal track plan` that adds this file.

## Files Created/Modified

- `.planning/phases/16-cross-platform-qa/16-BUILD-LEDGER.md` - build ledger, procedures, Uploads row 1 (available), Notes
- `.planning/STATE.md` - Phase 13 release checklist ticks, Phase 2 deferral closures, D-22 line

## Decisions Made

- Reused gate B2 from the orchestrator run at 2a532a0b (docs-only commits since).
- Play app signing fingerprints (SHA-256 979e9011c606c65faee98f0895bc3e4e71e57c616ed050936f5c01c2ecc91064, SHA-1 8db73866795076c1d571fb0ffdebd0521cdb0d6a) recorded in the ledger Notes; registering them in Firebase is the user's optional follow-up and is not done.

## Deviations from Plan

### Plan-text deviations (no code changed)

**1. [Plan text] "Log in" versus "Login"**
- **Found during:** Task 2
- **Issue:** The plan's UI-dump check looks for "Log in"; the button on the Login screen is labelled "Login".
- **Fix:** Verified the Login screen by its elements (email, password, Forgot Password?, Sign Up, Login button) and recorded the label in the ledger Notes. The automated R8-OK grep for 'Log in' would not match as written.

**2. [Plan text] Realtime log check cannot pass**
- **Found during:** Task 3 verification
- **Issue:** The plan's `logcat -s Realtime` must show `state=Connected`. No code in app/src/main logs `state=`; the Realtime tag only logs warnings and dropped events (RealtimeService.kt).
- **Fix:** Not forced. H1, H2 and H4 (installer, versionCode, crash count 0) were run; realtime is exercised functionally by runbook rows R1 and D1 (chat and push). Recorded in the ledger Notes.

**3. [Gate reuse] B2 not re-run**
- Gate B2 for versionCode 1 was run by the orchestrator at 2a532a0b (all BUILD SUCCESSFUL). Later commits touch only .planning/, so it was reused. Recorded in the ledger Notes.

**4. [Observation] Play-generated split minSdk 32**
- dumpsys shows minSdk=32 for this device's Play-generated split. This is bundletool device targeting, not a config change (the app stays minSdk 26).

**5. [Acceptance count] `- [ ] (5)` count**
- The plan baseline says the grep for `- [ ] (5)` prints 0 after the update. It prints 2 because two unrelated Phase 9/11 entries (lines 263 and 270) have their own step (5). The Phase 13 (5) is ticked and PLAY-1-OK printed.

**Total deviations:** 5 documentation-level, 0 code changes. **Impact:** none on the build or the release.

## Calendar risk (needs the user)

The Play dashboard shows the closed-testing requirement for this organization account (answer: yes). Production access needs a closed test with 12 testers for 14 days before the Dec 11-13 2026 release. This conflicts with the accepted D-10 gap that the user is the only internal tester. The 14-day clock must be started with 12 testers well before the release date, otherwise the date slips. Recorded as calendar-critical in STATE.md step (8).

## Issues Encountered

None beyond the deviations above.

## Known Stubs

None (docs and release plumbing only).

## Threat Flags

None. No source files changed; the ledger holds only public certificate fingerprints and file hashes, and keystore.properties was never opened.

## Next Phase Readiness

- Plan 16-03 onward can run the matrix on the Play-installed versionCode 1; the next upload needs versionCode 2 via the ledger fix-batch procedure.
- Open for the user: closed-testing tester recruitment (calendar-critical), optional Firebase registration of the Play app-signing SHA-1/SHA-256, Phase 17 Payments policy decision.

## Self-Check: PASSED

- Commits 7fd2ea44, 0f72b253, 9c701008 are ancestors of HEAD.
- 16-BUILD-LEDGER.md exists; the Uploads row 1 is `available`; Task 3 verify printed PLAY-1-OK.

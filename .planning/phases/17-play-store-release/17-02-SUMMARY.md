---
phase: 17-play-store-release
plan: 02
subsystem: ui
tags: [bug-22, bug-34, call-screen, compose-layout, robolectric, tracer]
status: complete

requires:
  - phase: 16-cross-platform-qa
    provides: BUG-22 and BUG-34 rows in 16-BUG-LOG.md
provides:
  - "Call hero glow drawn behind the avatar without reserving layout height"
  - "MeetingCallScreenControlsReachableTest (7 tests) at the BUG-22 and BUG-34 geometries"
  - "CALL_SCROLL_CONTENT_TAG on the scrolling call column"
affects: [17-09 device re-test (S1.05, S1.07), 17-RELEASE-CHECKLIST]

actuals:
  tokens: 5500
  tasks: 3
  commits: 5
plan_head_before: 25942db1b3ae502aa5036bd56faeed737fa75bec
plan_head_after: beff2935

tech-stack:
  added: []
  patterns:
    - "Decorative glow drawn with drawBehind on the hero container, outside its bounds, so it adds no measured height"

key-files:
  created:
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/call/calling/MeetingCallScreenControlsReachableTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/subviews/MeetingCallAvatar.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/MeetingCallScreen.kt
    - .planning/phases/16-cross-platform-qa/16-BUG-LOG.md

key-decisions:
  - "Fix is step 1 of RESEARCH Pattern 2 only (glow out of layout); steps 2a, 2b and 2c were not needed to turn the JVM tests green and were not applied"
  - "BUG-34 JVM geometry uses a 100 px navigation bar (about 47 dp, read off FS.C3-200pct.png), not the 48 px used for BUG-22"

requirements-completed: [REL-07]
---

# Phase 17 Plan 02: BUG-22 and BUG-34 call-screen controls Summary

**The 360 dp hero glow no longer reserves layout height (drawn with drawBehind), so Minimize, Chat, Mute and Speaker sit above the pinned End Call with the banner and chip at font 1.0 and at 200% font in the Waiting state.**

## Accomplishments

- BUG-22 (font 1.0, banner and chip, 360x800 dp, 48 px nav bar): controls now above End Call with no scrolling.
- BUG-34 (font 2.0, 339x753 dp at 340 dpi, Waiting state, 100 px nav bar): controls fit above End Call at rest; `performScrollTo` and a touch swipe work on the screen alone and inside `CallOverlayHost`.
- External variant (End Session) and Connected pair hero keep their controls reachable at font 2.0 with the banner.
- Existing suites stay green: whole `screens.call.calling` package (including `MeetingCallScreenEndButtonRegressionTest`, `MeetingCallScreenTest`, `CallOverlayHostTest`, `CallOverlayHostInsetsTest`), `screens.call.*`, `A11yAuditTest`, `ktlintCheck`, `scripts/check-design-rules.sh`.

## Task commits

| Task | Commit | Note |
| ---- | ------ | ---- |
| 1 RED | ed58aee5 | failing BUG-22 test |
| 1 GREEN | 84e35b85 | glow via drawBehind, CALL_SCROLL_CONTENT_TAG |
| 2 | 6261f3ea | BUG-34 tests (the code fix is 84e35b85) |
| 3 | 5357287c | external and connected-variant guard tests |
| 3 | beff2935 | 16-BUG-LOG rows: fixed (pending re-test) |

## RED messages

- BUG-22: `Assert failed: The component with TestTag = 'call_control_minimize' is not displayed!` (before the glow change).
- BUG-34 fit-at-rest test, on the old hero: `call_control_minimize bottom is 643.2941.dp (top 558.58826.dp) but End Call starts at 625.8823.dp at font 2.0`.

## BUG-34 cause and fix step

Fix step: **1 (glow out of layout)**, the same change as BUG-22. The glow box added about 176 dp of dead height above the info block, pushing the controls to 558-643 dp while End Call starts at 626 dp. After the change they end at about 618 dp (8 dp of margin on Robolectric text metrics).

**Not reproduced on the JVM: "swipe up does not scroll".** On the old layout, with the BUG-34 geometry, the touch swipe scrolls both in `MeetingCallScreen` alone and inside `CallOverlayHost`; the overlay's `pointerInput` (consumes in the Main pass, after the children) did not block it, so step 2b was not applied. The swipe tests therefore pass before and after, and the "does not scroll" cause remains unproven. The fit-at-rest test is the reproducible RED. Device re-test row S1.05 must confirm the swipe on the Samsung (SM-A075F); on-device text may wrap more than Robolectric's, so the 8 dp margin is thin. If the controls still overflow there, the next steps are 2a (size the hero from the scroll viewport) and 2c (pin the controls with End Call).

## RESEARCH A9 (flagged assumption): CONFIRMED

Removing the glow from layout alone turned the BUG-22 test green. Task 2's viewport sizing fallback was not needed.

## Deviations from Plan

**1. [Rule 1 - test realism] BUG-34 tests use a 100 px navigation bar**
- **Found during:** Task 2. The plan's common setup said 48 px, but at 340 dpi that is 22.6 dp; the screenshot shows a 3-button bar of about 100 px (47 dp). With 48 px the old layout already passed.
- **Fix:** `navBar` is a var; BUG-34 tests set `BUG_34_NAV_BAR_PX = 100`, BUG-22 keeps 48.
- **Files modified:** MeetingCallScreenControlsReachableTest.kt

**2. [Plan wording] Swipe tests tolerate content that already fits**
The swipe helper requires the content to move only when some control is still below End Call. Without this the tests would fail after a successful fix, since nothing needs scrolling.

**3. [Plan wording] Extra test and commit shape**
Added `controlsFitAboveEndCallAtRestAtTwoHundredPercentInTheWaitingState` (the reproducible BUG-34 RED). The `fix(17-02): BUG-34` commit carries tests only; the code fix is in the BUG-22 commit. BUG-22 was committed as a separate RED test commit then a fix commit (TDD), not one combined commit.

## Known Stubs

None.

## Threat Flags

None. `CallOverlayHost.kt` was not changed (T-17-05 not triggered).

## Notes

- The BUG-34 device re-test (swipe, glow look around the avatar on the phone) is for smoke run 1 rows S1.05 and S1.07 (plan 17-09); no device was used here.
- 16-BUG-LOG.md Status for both rows is `fixed (pending re-test)`; BUG-22 fix sha 84e35b85, BUG-34 6261f3ea (code fix 84e35b85).

## Self-Check: PASSED

- MeetingCallScreenControlsReachableTest.kt, MeetingCallAvatar.kt and MeetingCallScreen.kt exist; commits ed58aee5, 84e35b85, 6261f3ea, 5357287c, beff2935 are on the branch.
- Final verify run: `screens.call.*`, `A11yAuditTest`, `ktlintCheck` green; design rules OK; the bug-log grep prints 2.

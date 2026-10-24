---
phase: 09-voice-call
plan: 03
subsystem: call
tags: [join-window, clock, intents, security, pure-logic]
requires: []
provides:
  - isWithinJoinWindow / isStartingNow / JoinClock (managers/JoinWindow.kt)
  - JoinCallButton shared composable
  - ExternalMeetingProvider.detect
  - Context.openExternalMeeting / rememberExternalMeetingLauncher
affects: [09-09, join sites, external call variant]
tech-stack:
  added: []
  patterns: [stateIn WhileSubscribed app-scoped clock, scheme allow-list + REQUIRE_NON_BROWSER intent]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/JoinWindow.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/components/call/JoinCallButton.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/ExternalMeetingProvider.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/components/web/ExternalLinkLauncher.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/managers/JoinWindowTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/managers/JoinClockTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/components/call/JoinCallButtonTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/call/calling/ExternalMeetingProviderTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/components/web/ExternalLinkLauncherTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/components/web/CustomTabLauncher.kt
key-decisions:
  - "Client join rule is a UX gate only; server can_join ignored, backend /join remains the authority"
  - "External links are app-first (REQUIRE_NON_BROWSER on API 30+), Custom Tab fallback for http(s) only; no <queries> needed"
metrics:
  tasks: 2
  files: 10
  completed: 2026-10-24
---

# Phase 9 Plan 03: Join Window, JoinClock and External Meeting Launcher Summary

Shared join-entry utilities: the iOS +-3600 s join rule with a 30 s app-scoped clock, a shared Join Call button, the iOS-parity meeting provider detector, and an allow-listed app-first external link launcher with Custom Tab fallback.

## Tasks

| Task | Name | Commits |
|------|------|---------|
| 1 | Join window rule, JoinClock, JoinCallButton | 4038a59 (RED tests), 9752212 (feat) |
| 2 | ExternalMeetingProvider + external link launcher | d211bbe (RED tests), 5f0fb9b (feat) |

## Verification

- `:app:testDevDebugUnitTest` for JoinWindowTest, JoinClockTest, JoinCallButtonTest, ExternalMeetingProviderTest (10), ExternalLinkLauncherTest (7) plus the existing CustomTabLauncherTest (5): all pass; `ktlintCheck` passes; `scripts/check-design-rules.sh` prints OK.
- Acceptance greps: no non-comment `canJoin` in JoinWindow.kt, `JOIN_CLOCK_TICK_MS = 30_000L` present once, `FLAG_ACTIVITY_REQUIRE_NON_BROWSER` present, no non-comment `parseUri`, `findActivity` is `internal`.

## Deviations from Plan

None to the code. Execution notes:

- The worktree started at an older commit than the expected base `770937d`; per the startup check it was reset to that base before any work.
- Parallel agents repeatedly ran `./gradlew --stop`, killing a few Gradle invocations ("stop command received"); the run was simply repeated. This is an environment artifact, not a code issue.
- The RED commits are compile-failure REDs (the tests reference types that did not exist yet), not runtime assertion failures.

## Known Stubs

None. The three Join sites still use their disabled placeholder; wiring them to `JoinCallButton` / `JoinClock` is owned by later plans.

## Threat Flags

None. The new launch surface (T-09-07, T-09-08) is covered by the plan's threat model and mitigated by the scheme allow-list and tested rejections.

## Self-Check: PASSED

All ten files exist; commits 4038a59, 9752212, d211bbe, 5f0fb9b exist on the branch.

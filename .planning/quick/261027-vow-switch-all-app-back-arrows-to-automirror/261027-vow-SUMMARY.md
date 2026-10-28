---
phase: quick-261027-vow
plan: 01
subsystem: ui
tags: [compose, icons, material3, iconography]
requires:
  - phase: quick-261027-v9i
    provides: Outlined icons in chat/safety directories, with the thread back arrow as the one Filled exception
provides:
  - All 5 back arrows use Icons.AutoMirrored.Outlined.ArrowBack
affects: [phase-15-polish]
tech-stack:
  added: []
  patterns: ["Back arrows are AutoMirrored Outlined app-wide"]
key-files:
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/AuthTopAppBar.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/ExpertProfileScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/forgetpassword/ForgotPasswordScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/debug/DesignSystemGalleryScreen.kt
decisions:
  - "Supersedes the v9i decision 'Thread back arrow stays AutoMirrored.Filled.ArrowBack'. Phase 12 UI-SPEC line 40 now holds with no exception."
  - "03-UI-SPEC.md line 30 names the Filled back arrow. It is a completed-phase record and was not edited; the app-wide convention is now AutoMirrored Outlined for back arrows."
metrics:
  duration: ~5min
  completed: 2026-10-27
status: complete
actuals:
  tokens: 400
  tasks: 1
  commits: 1
plan_head_before: ae3394fab91487af536b30082600ded58d3922d8
plan_head_after: fea65c4
---

# Quick Task 261027-vow: AutoMirrored Outlined back arrows Summary

All 5 back arrows (shared AuthTopAppBar behind 29 call sites, chat thread, Expert Profile, Forgot Password "Back to Login", Design System Gallery) now draw `Icons.AutoMirrored.Outlined.ArrowBack`. The change is 5 import swaps and 5 usage swaps, with no change to contentDescription, onClick, enabled, size or tint.

Commit: fea65c4 `style(quick-261027-vow): AutoMirrored Outlined back arrows app-wide`

## Verification

- Zero Filled ArrowBack references remain in app/src. 5 Outlined imports and 5 usages in app/src/main.
- Filled usages in screens/chat, components/chat and components/safety: 0 (v9i exception closed).
- App-wide non-comment Filled usage lines dropped by exactly 5, and Filled import lines by exactly 5 (measured 29 to 24 on `git grep` over app/src incl. tests; the plan's 27 to 22 used a slightly different grep scope, the delta of 5 is what matters). Expert Profile MoreVert intact.
- Diff against ae3394f: exactly 5 files, every changed line contains ArrowBack.
- `./gradlew :app:testDevDebugUnitTest ktlintCheck :app:assembleDevDebug`: BUILD SUCCESSFUL.
- `scripts/check-design-rules.sh`: Design rules: OK.

## Deviations from Plan

None - plan executed exactly as written.

## Open Items (non-blocking, deferred manual testing)

- On-device glance on the Samsung test device: Login/Signup top bar, chat thread, Expert Profile and Forgot Password show the outlined arrow, and under an RTL locale the arrow still points toward the start edge. Fold into the Phase 15 polish pass.
- Observation only: Expert Profile's top-bar overflow is still Filled MoreVert next to the now-Outlined back arrow. Candidate for the Phase 15 icon sweep.

## Known Stubs

None.

## Self-Check: PASSED

All 5 files modified and committed (fea65c4 is an ancestor of HEAD).

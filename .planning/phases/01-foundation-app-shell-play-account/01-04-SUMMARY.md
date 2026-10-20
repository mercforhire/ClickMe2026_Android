---
phase: 01-foundation-app-shell-play-account
plan: 04
subsystem: ui
tags: [splash, launcher-icon, theme-xml, app-route, hilt-viewmodel]

requires:
  - phase: 01-01
    provides: core-splashscreen dependency, Hilt, manifest baseline, robolectric config
provides:
  - Transparent ClickMe mark drawable (nodpi) copied from iOS SplashLogo
  - Adaptive + themed (monochrome) launcher icon on solid #131313
  - Theme.ClickMe (values + values-v27) and Theme.ClickMe.Splash (SplashScreen API)
  - AppRoute enum and AppViewModel with destinationResolved splash-hold flag
affects: [01-08 MainActivity/installSplashScreen, phase 2 auto-login, phase 4 mode switch]

tech-stack:
  added: []
  patterns:
    - "Only XML color is @color/surface (#FF131313)"
    - "windowLightNavigationBar lives only in values-v27 (lint-safe on minSdk 26)"
    - "AppViewModel constructed directly in plain JUnit tests (no Hilt)"

key-files:
  created:
    - app/src/main/res/drawable-nodpi/clickme_mark.png
    - app/src/main/res/drawable/splash_logo.xml
    - app/src/main/res/drawable/ic_launcher_monochrome.xml
    - app/src/main/res/values-v27/themes.xml
    - app/src/main/java/com/uptrendinvestments/clickmeapp/AppRoute.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/AppViewModel.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/AppViewModelTest.kt
  modified:
    - app/src/main/res/drawable/ic_launcher_foreground.xml
    - app/src/main/res/mipmap-anydpi/ic_launcher.xml
    - app/src/main/res/mipmap-anydpi/ic_launcher_round.xml
    - app/src/main/res/values/colors.xml
    - app/src/main/res/values/themes.xml
    - app/src/main/AndroidManifest.xml
  deleted:
    - app/src/main/res/drawable/ic_launcher_background.xml
    - ten mipmap-*dpi/ic_launcher(_round).webp template files

key-decisions:
  - "Adaptive foreground and monochrome both inset the transparent mark by 17dp (D-11 revised)"
  - "AppViewModel resolves ClientHome synchronously in init; no delay (iOS 3s splash not ported)"

patterns-established:
  - "Splash hold: MainActivity (plan 01-08) keeps the OS splash until AppViewModel.destinationResolved is true"

requirements-completed: [FND-11]

duration: 15min
completed: 2026-10-20
---

# Phase 1 Plan 04: Splash, Launcher Icon and Root Route Summary

**Android 12 SplashScreen theme and adaptive/themed launcher icon built from the iOS ClickMe mark on #131313, plus AppRoute/AppViewModel that resolve the stub ClientHome route synchronously for the splash hold.**

## Accomplishments
- Launcher icon is an adaptive icon (solid #131313 background, foreground and monochrome layers from the transparent mark); template robot icons, grid background and palette colors removed.
- `Theme.ClickMe.Splash` (parent `Theme.SplashScreen`) shows the mark on #131313 and hands off to `Theme.ClickMe` whose window background is #131313; `windowLightNavigationBar` only in `values-v27`, so lint passes on minSdk 26.
- Manifest: application theme `Theme.ClickMe`, predictive back enabled, activity on the splash theme, redundant activity label removed; still exactly one exported component.
- `AppViewModel` exposes `route` (ClientHome) and `destinationResolved` (true) right after construction, with `debugSwitchScaffold()` toggling Client/Expert home. 3 unit tests pass.

## Task Commits
1. Task 1: mark asset, launcher icon, splash drawable - `7a6681c` (feat)
2. Task 2 (TDD): RED `d7a8ad2` (test), GREEN `f0bdfbb` (feat)

## Verification
- `assembleDevDebug assembleProdDebug lintDevDebug lintProdDebug ktlintCheck` and `AppViewModelTest` (3/3) pass; `scripts/check-design-rules.sh` OK.
- Acceptance greps verified (one `android:label`, one exported, no `delay(`, `windowLightNavigationBar` only in v27).
- Visual splash/icon checks are deferred to plan 01-09 on devices.

## Deviations from Plan
None in code. Process: worktree base started at dc2172d, behind the expected base fbae728; reset to fbae728 per the startup check before any work. The plan's automated verify command points at the main checkout path; it was run from the worktree instead.

## Known Stubs
`AppViewModel.resolveInitialRoute` returns the stub route ClientHome by design (Phase 2 FND-07 replaces it with session-based auto-login and must bound the splash hold).

## Threat Flags
None. T-01-13 mitigated (single exported component), T-01-14 mitigated (synchronous resolve, no delay).

## Self-Check: PASSED
- Files present: clickme_mark.png, splash_logo.xml, ic_launcher_monochrome.xml, values-v27/themes.xml, AppRoute.kt, AppViewModel.kt, AppViewModelTest.kt.
- Commits present: 7a6681c, d7a8ad2, f0bdfbb.

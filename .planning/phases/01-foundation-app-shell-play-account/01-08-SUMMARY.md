---
phase: 01-foundation-app-shell-play-account
plan: 08
subsystem: ui
tags: [compose, app-shell, splash, navigation3, hilt]

requires:
  - phase: 01-04
    provides: AppRoute, AppViewModel (route, destinationResolved, debugSwitchScaffold), splash theme
  - phase: 01-05
    provides: HomeTabScaffold, Navigator, rememberNavigationState, client/expert tab definitions
  - phase: 01-06
    provides: PlaceholderTabContent, GalleryControlsSection
  - phase: 01-07
    provides: TopBarAvatarButton, GalleryMediaSection
provides:
  - DesignSystemGalleryScreen + DesignSystemGalleryKey (hosts theme, controls and media gallery sections)
  - HomeClientScreen/HomeClientShell and HomeExpertScreen/HomeExpertShell (Hilt wrapper + stateless 5-tab shell)
  - HomeClientViewModel / HomeExpertViewModel (avatarUrl flow, null in Phase 1)
  - ClickMeApp AppRoute switch
  - MainActivity with installSplashScreen before super.onCreate and keep-on-screen bound to destinationResolved
affects: [01-09 device verification, phase 2 auto-login, phase 4 mode switch and avatar action]

tech-stack:
  added: []
  patterns:
    - "Shell = Hilt wrapper (BuildConfig.DEBUG, hiltViewModel) over a stateless shell that Robolectric can render"
    - "AppRoute is a plain when in ClickMeApp, not a nav destination (D-01)"
    - "Debug gallery is pushed on the Profile tab stack via Navigator.navigate(DesignSystemGalleryKey)"

key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/ClickMeApp.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/debug/DesignSystemGalleryScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/home/HomeClientScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/home/HomeClientViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/home/HomeExpertScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/home/HomeExpertViewModel.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/HomeShellsTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/MainActivity.kt

key-decisions:
  - "Tab title and icon in each shell come from the TabDefinition (clientTabs / expertTabs) via a private ClientTabRoot / ExpertTabRoot helper, so labels are not repeated"
  - "Login and SignupSetup routes render the same empty surface Box as Splash (unreachable in Phase 1)"

patterns-established:
  - "Tab-root onAvatarClick is an empty lambda in Phase 1 (UI-SPEC); Phase 4 wires the mode switch"

requirements-completed: [FND-10, FND-11, FND-12]

duration: 25min
completed: 2026-10-20
---

# Phase 1 Plan 08: App Shell Assembly Summary

**Cold launch now goes through the SplashScreen API into ClickMeApp's AppRoute switch, rendering a 5-tab client or expert shell on HomeTabScaffold with a debug-gated scaffold switch and a Design system gallery that hosts every shared component.**

## Accomplishments
- DesignSystemGalleryScreen (TopAppBar with back arrow, scrolling column of GalleryThemeSection, GalleryControlsSection, GalleryMediaSection) with its `DesignSystemGalleryKey` nav key. The two previously unhosted sections from plans 01-06 and 01-07 are now on screen.
- HomeClientShell / HomeExpertShell: five tabs each, `Nothing here yet.` empty state per tab, 48 dp avatar button on Explore and Dashboard, Profile footer with debug-only scaffold switch and gallery buttons. Hilt wrappers pass `showDebugAffordances = BuildConfig.DEBUG`.
- ClickMeApp: plain `when (route)`; ClientHome and ExpertHome host the shells; Splash, Login and SignupSetup show an empty surface.
- MainActivity: `@AndroidEntryPoint`, `installSplashScreen()` before `super.onCreate`, `setKeepOnScreenCondition { !destinationResolved }`, dark edge-to-edge, `ClickMeTheme { ClickMeApp(appViewModel) }`. No delay, Handler, exit animation or intent extras.
- HomeShellsTest: 9 Robolectric tests, all passing (tabs and selection, avatar button, debug switch callback, gallery sections, per-tab stack retention across a tab switch, re-tap-to-root, Back node, debug gate off for both shells, expert tabs).
- Full suite green: `ktlintCheck lintDevDebug lintProdDebug testDevDebugUnitTest testProdDebugUnitTest assembleDevDebug assembleProdDebug` exit 0; `scripts/check-design-rules.sh` OK. All acceptance greps verified.

## Task Commits
1. Task 1: gallery screen and client/expert shells - `b4c6b05` (feat)
2. Task 2: tests `3dab77e` (test), ClickMeApp + MainActivity `50fd731` (feat)

## Deviations from Plan
None in code.

Process notes:
- Worktree started at dc2172d, an ancestor of the expected base b60459d; reset to b60459d per the startup check before any work.
- TDD ordering for Task 2: the shells already existed from Task 1, so HomeShellsTest passed on first run and there was no failing RED commit. The test commit is still separate from the ClickMeApp/MainActivity implementation commit.
- The plan's verify commands point at the main checkout path; they were run from the worktree instead.

## Known Stubs
- `HomeClientViewModel.avatarUrl` / `HomeExpertViewModel.avatarUrl` are always null (no profile in Phase 1); the /me profile feeds them once a session exists (Phase 2+).
- `onAvatarClick = {}` in both Hilt wrappers, as specified by UI-SPEC; Phase 4 wires the mode switch.
- Tab roots show the placeholder empty state by design; real screens arrive in later phases.
- `AppRoute.Login` / `SignupSetup` render an empty surface until Phases 3 and 4.

## Threat Flags
None. T-01-23 mitigated (`BuildConfig.DEBUG` gate in both wrappers, test asserts buttons absent when false), T-01-24 mitigated (route only set by AppViewModel, no intent extras read), T-01-25 mitigated (no delay/Handler in MainActivity).

## Notes for Later Plans
- Plan 01-09 must verify on device: splash handoff, real stack and scroll retention, icon, and 200% font scale.
- Phase 2 must bound the splash hold when route resolution becomes asynchronous.

## Self-Check: PASSED
- Files present: ClickMeApp.kt, MainActivity.kt, DesignSystemGalleryScreen.kt, HomeClientScreen.kt, HomeClientViewModel.kt, HomeExpertScreen.kt, HomeExpertViewModel.kt, HomeShellsTest.kt.
- Commits present: b4c6b05, 3dab77e, 50fd731.

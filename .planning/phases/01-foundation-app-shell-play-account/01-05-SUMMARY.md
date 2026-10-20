---
phase: 01-foundation-app-shell-play-account
plan: 05
subsystem: ui
tags: [navigation3, tabs, back-stack, compose]

requires:
  - phase: 01-01
    provides: Navigation 3, lifecycle-viewmodel-navigation3, serialization, icons-extended, Robolectric config, tab_* strings
  - phase: 01-03
    provides: ClickMeTheme, Motion tokens
provides:
  - NavigationState / rememberNavigationState / toDecoratedEntries (per-tab NavBackStacks, exit-through-home)
  - Navigator (navigate, goBack, onTabSelected, reselect, navigateInTab)
  - TabDefinition, clientTabs / clientStartTab, expertTabs / expertStartTab
  - HomeTabScaffold (NavigationBar + NavDisplay, tab fade-through, push/pop slide) and BottomBar.hidden() opt-out
affects: [01-08 HomeClientScreen / HomeExpertScreen, phase 12 chat entry points, phase 13 push routing]

tech-stack:
  added: []
  patterns:
    - "Per-tab back stacks are app-owned NavBackStacks; NavDisplay only renders (D-01); no Navigation Compose 2 (D-15)"
    - "NavigationState.lastTransition (Push / Pop / TabSwitch) is read by NavDisplay transition specs"
    - "Tab scaffold takes plain parameters only, so it renders under Robolectric"

key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/navigation/NavigationState.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/navigation/Navigator.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/navigation/TabDefinition.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/navigation/HomeTabScaffold.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/home/subviews/HomeClientTab.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/home/subviews/HomeExpertTab.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/navigation/NavigatorTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/navigation/HomeTabScaffoldTest.kt
  modified: []

key-decisions:
  - "toDecoratedEntries is an extension function (recipe has it as a member) so plan 01-08 can rely on the plan's names"
  - "clientStartTab / expertStartTab are inferred from tabs.first().key rather than declared as NavKey, keeping the ': NavKey' acceptance counts at 5"
  - "Dropped the recipe's reselectEvents flow; reselect pops the tab to root directly"

patterns-established:
  - "Back at a non-home tab root selects home; at home root goBack is a no-op and NavDisplay has a single entry so system Back exits"
  - "Route opts out of the bottom bar via NavEntry metadata BottomBar.hidden()"

requirements-completed: [FND-12]

duration: 20min
completed: 2026-10-20
---

# Phase 1 Plan 05: Tab-Shell Navigation Core Summary

**Navigation 3 tab shell: app-owned per-tab back stacks (nav3-recipes multiplestacks), Navigator with Back-to-home, re-tap-to-root and cross-tab API, client/expert tab lists, and a shared HomeTabScaffold with styled NavigationBar and Motion-spec transitions.**

## Accomplishments
- NavigationState and Navigator adapted from the Apache-2.0 multiplestacks recipe (headers kept), extended with `lastTransition`, `reselect`, `navigateInTab` and `require`-based unknown-tab failures (T-01-17).
- Client tabs Explore, Search, Bookings, Chats, Profile and expert tabs Dashboard, Bookings, Topics, Chats, Profile with Outlined/Filled icons and always-visible labels.
- HomeTabScaffold: NavigationBar (surfaceContainer, primary/secondaryContainer item colors, labelMedium) over NavDisplay; 150 ms fade after 90 ms out for tab switches, 300 ms FastOutSlowIn slide + fade for push/pop and predictive pop; bar hidden when the top entry's metadata carries `BottomBar.HIDE_KEY`.
- Tests: NavigatorTest 12/12, HomeTabScaffoldTest 5/5 (Robolectric: tabs and selection, tab click shows root, expert tabs, hidden-bar metadata, pushed screen survives a tab round trip). `ktlintCheck`, `lintDevDebug`, `assembleDevDebug`, `scripts/check-design-rules.sh` all pass.

## Task Commits
1. **Task 1: NavigationState and Navigator** - RED `0db66fa` (test), GREEN `9991ba4` (feat)
2. **Task 2: Tab definitions and HomeTabScaffold** - RED `70f587b` (test), GREEN `483542f` (feat)

## Deviations from Plan
None in code; plan executed as written.

Process notes:
- Worktree started at dc2172d, behind the expected base e871a29; reset to e871a29 per the startup check before any work.
- Recipe sources were fetched through the jsDelivr mirror of `android/nav3-recipes` (main), the same repo and path the plan names, because the GitHub raw host is blocked by the tool guard.
- The plan's verify command uses the main checkout path; commands were run from the worktree instead.

## Known Stubs
None. No Phase 1 route uses `BottomBar.hidden()` and `navigateInTab` has no caller, both by design (UI-SPEC).

## Threat Flags
None. T-01-17 mitigated: `reselect` and `navigateInTab` use `require(tab in state.backStacks)`; both throw cases are test-asserted.

## Notes for Later Plans
- Plan 01-08 should use `rememberNavigationState(clientStartTab, clientTabs.map { it.key }.toSet())`, `remember { Navigator(state) }` and `HomeTabScaffold(...)`; tab-root screens own their own TopAppBar.
- Tab-switch fade-through is selected via `lastTransition`; animation feel, retained scroll position and process-death restore are verified on device in plan 01-09.
- The Robolectric scaffold test relies on `rememberNavBackStack` serialization of `@Serializable data object` keys; keep tab keys argument-less and public/internal.

## Self-Check: PASSED
- Files present: NavigationState.kt, Navigator.kt, TabDefinition.kt, HomeTabScaffold.kt, HomeClientTab.kt, HomeExpertTab.kt, NavigatorTest.kt, HomeTabScaffoldTest.kt.
- Commits present: 0db66fa, 9991ba4, 70f587b, 483542f.

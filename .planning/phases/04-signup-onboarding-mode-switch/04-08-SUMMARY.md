---
phase: 04-signup-onboarding-mode-switch
plan: 08
subsystem: ui
tags: [compose, horizontal-pager, radio-group, onboarding, mode-switch]
requires:
  - phase: 04-02
    provides: "bundled mode_client / mode_expert / onboarding_* drawables"
  - phase: 04-03
    provides: "AuthTopAppBar"
provides:
  - "screens.settings.modeswitching.ModeSwitchKey, ModeSwitchScreen"
  - "screens.settings.modeswitching.subviews.ModeSwitchCard"
  - "screens.onboarding.OnboardingContent, OnboardingPage, OnboardingPages.all, ONBOARDING_PAGER_TAG"
  - "strings_mode_switch.xml and strings_onboarding.xml"
affects: [04-18]
tech-stack:
  added: []
  patterns: ["stateless screens with callbacks (no ViewModel) for trivial iOS view models", "BackHandler enabled only past page 1"]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/modeswitching/ModeSwitchScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/modeswitching/subviews/ModeSwitchCard.kt
    - app/src/main/res/values/strings_mode_switch.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/settings/modeswitching/ModeSwitchScreenTest.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/onboarding/OnboardingPages.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/onboarding/OnboardingScreen.kt
    - app/src/main/res/values/strings_onboarding.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/onboarding/OnboardingScreenTest.kt
  modified: []
key-decisions:
  - "Both screens are stateless (no ViewModel); the mode and finish callbacks come from the shell (plan 04-18)"
  - "Onboarding pages scroll vertically inside the pager so 200% font scale and short screens stay reachable"
requirements-completed: [ONB-01, ONB-02]
duration: ~35 min
completed: 2026-10-21
---

# Phase 4 Plan 08: Mode Switch and Onboarding Carousel Summary

Stateless Mode Switcher (two radio-group cards with bundled images and an Active badge) and a four-page HorizontalPager onboarding carousel with Skip, Next/Get Started and page-aware system Back, both ready for plan 04-18 to wire.

## Tasks

| Task | Name | Commit |
| ---- | ---- | ------ |
| 1 | Mode Switch page with radio-group cards (D-18, D-19) | 9fc4f69 |
| 2 | Onboarding carousel (D-21) | 9706f85 |

## What was built

- ModeSwitchScreen: Scaffold with AuthTopAppBar, `selectableGroup` column (scrollable). Tapping the inactive card calls `onSwitchMode(mode)` then `onBack()`; tapping the active card calls `onBack()` only. `ModeSwitchKey` lives in the screen file.
- ModeSwitchCard: `selectable(role = RadioButton)` with the shared `pressScale`, animated fill and border (secondaryContainer + 2 dp primary when active), 88 dp bundled image (decorative), Active badge, merged label "Title. Subtitle." plus " Active.".
- OnboardingContent: HorizontalPager, animated dots (merged "Page N of M"), Next / Get Started `PressScaleButton`, Skip on every page, `BackHandler(enabled = currentPage > 0)`, glow background and a scrim gradient over the illustration. No separate window is used.
- Tests: ModeSwitchScreenTest (6), OnboardingScreenTest (8), all passing under Robolectric.

## Verification

- `testDevDebugUnitTest` for both test classes: 14 tests, 0 failures.
- `ktlintCheck`, `lintDevDebug` exit 0; `scripts/check-design-rules.sh` prints "Design rules: OK".

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Accessibility] Vertical scroll on each onboarding page**
- **Found during:** Task 2
- **Issue:** At 200% font scale or on short screens the 260 dp illustration plus text can overflow the pager area while the footer is fixed.
- **Fix:** Each page body is wrapped in `verticalScroll`. Tests use `@Config(qualifiers = "w400dp-h900dp")` so `assertIsDisplayed` is meaningful.
- **Files modified:** OnboardingScreen.kt, OnboardingScreenTest.kt

**2. [Plan acceptance nit] `grep -c 'HorizontalPager'` prints 2, not 1**
- The count includes the import line plus the single call site. There is one HorizontalPager usage; kept the import rather than fully qualifying the call.

Added two small a11y strings (`mode_switch_card_a11y`, `mode_switch_card_a11y_active`) to strings_mode_switch.xml for the merged card label, since all copy must live in strings XML.

## Known Stubs

None.

## Deferred / Manual checks (not blocking, per project policy)

- On-device: swipe feel, press-scale on the cards, TalkBack reading of the radio cards and dots, predictive back on page 2 to 4, behavior at 200% font scale.

## Threat Flags

None. Both screens render static bundled content; the mode choice is only reported to the caller (T-04-22, T-04-23 accepted).

## Self-Check: PASSED

All eight files exist; commits 9fc4f69 and 9706f85 present.

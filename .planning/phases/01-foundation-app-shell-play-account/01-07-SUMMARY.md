---
phase: 01-foundation-app-shell-play-account
plan: 07
subsystem: ui
tags: [compose, components, custom-tabs, coil]

requires:
  - phase: 01-01
    provides: browser, Coil 3, Robolectric, AppLinks, shared strings
  - phase: 01-03
    provides: ClickMeTheme, ClickMePalette, extras, Spacing
provides:
  - openCustomTab / rememberCustomTabLauncher / isAllowedCustomTabUri (http(s) allowlist)
  - Avatar with Coil 3 image over a Person fallback; TopBarAvatarButton (48 dp target)
  - GreenGlowBlobLayer (gradient-only, decorative)
  - GalleryMediaSection for the debug design-system gallery
affects: [01-08 Explore/Dashboard top bars, 01-09 gallery wiring, phase 9 join URLs, phase 14 Stripe onboarding]

tech-stack:
  added: []
  patterns:
    - "Non-composable code reads colors from ClickMePalette (Custom Tab bars)"
    - "Avatar fallback is always composed; the Coil image is drawn over it so loading and error show the silhouette"

key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/components/web/CustomTabLauncher.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/components/avatar/Avatar.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/components/avatar/TopBarAvatarButton.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/components/backgrounds/GreenGlowBlobLayer.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/debug/GalleryMediaSection.kt
    - app/src/main/res/values/strings_gallery_media.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/components/web/CustomTabLauncherTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/components/avatar/AvatarTest.kt
  modified: []

key-decisions:
  - "rememberCustomTabLauncher takes an onFailure parameter (UI-SPEC listed none) so callers can show the failure Snackbar"
  - "GalleryMediaSection is a plain Column; the host gallery screen supplies scrolling"

patterns-established:
  - "Decorative layers use clearAndSetSemantics { } and draw via drawBehind"

requirements-completed: [FND-10]

duration: 20min
completed: 2026-10-20
---

# Phase 1 Plan 07: Media Components Summary

**Custom Tab launcher that admits only http(s) links (dark #131313 bars, ACTION_VIEW fallback, onFailure), Coil 3 Avatar with Person fallback, 48 dp TopBarAvatarButton, gradient-only GreenGlowBlobLayer, and a debug gallery media section.**

## Accomplishments
- `isAllowedCustomTabUri` rejects javascript:, file:, content:, intent:, mailto: and host-less URLs before any `startActivity` (T-01-20 mitigated, proven by Robolectric tests).
- Custom Tab -> ACTION_VIEW (BROWSABLE) -> `onFailure` chain; NEW_TASK added automatically for non-Activity contexts.
- `Avatar` (32/40/56/96 dp) exposes a contentDescription only when given one; `TopBarAvatarButton` is labelled "Your profile" and uses `IconButton`, never `clickable`.
- `GreenGlowBlobLayer` draws two radial blobs (0.18 center alpha, 190 dp) with no blur effect and no semantics.
- Tests: CustomTabLauncherTest 5/5, AvatarTest 8/8. `ktlintCheck`, `lintDevDebug`, `assembleDevDebug` and `scripts/check-design-rules.sh` pass.

## Task Commits
1. Task 1 (TDD): RED `403ed4f` (test), GREEN `1a3497e` (feat)
2. Task 2 (TDD): RED `4ede3c2` (test), GREEN `343e508` (feat)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Doc comment mentioned the forbidden blur modifier**
- **Found during:** Task 2 acceptance greps
- **Issue:** The KDoc on GreenGlowBlobLayer contained the word "blur", failing the "grep -c blur prints 0" criterion.
- **Fix:** Reworded the comment.
- **Commit:** 343e508

**2. [Process] Worktree base correction**
- The worktree started at dc2172d and was reset to the expected base e871a29 per the startup check. Gradle was run with `--offline` from the worktree rather than the main checkout path in the plan's verify command. `ktlintFormat` was used once to fix chain-method-continuation, import order and blank-line style findings.

## Known Stubs
None. The gallery section intentionally uses no network images.

## Threat Flags
None beyond the plan's register (T-01-20 mitigated; T-01-21 and T-01-22 accepted).

## Notes for Later Plans
- Human check in plan 01-09: Custom Tab appearance (#131313 toolbar, Back returns to gallery), blocked-link copy, glow softness, avatar sizes.
- `GalleryMediaSection` must be added to the gallery screen by the plan that assembles it (01-09 / 01-08); it is not wired into any host here.

## Self-Check: PASSED
- Files present: all eight created files listed above.
- Commits present: 403ed4f, 1a3497e, 4ede3c2, 343e508.

---
phase: 01-foundation-app-shell-play-account
plan: 06
subsystem: ui
tags: [compose, components, accessibility, material3]

requires:
  - phase: 01-03
    provides: ClickMeTheme, MaterialTheme.extras, Spacing, Motion tokens
  - phase: 01-01
    provides: string resources (save/a11y/empty/error copy), Robolectric config
provides:
  - PressScaleButton, Modifier.pressScale, PressScaleButtonDefaults (filled, tonal, destructive)
  - AsyncSaveButton and SaveState (Idle, Saving, Saved)
  - ValidatedField (pill field, error semantics, secure mode)
  - EmptyState, EmptyStateAction, ErrorState
  - PlaceholderTabContent (tab-root app bar plus stub empty state, actions and footer slots)
  - GalleryControlsSection for the debug design-system gallery
affects: [01-08 shells and tabs, 01-09 gallery and device checks, Phase 2 auth screens, Phase 15 error states]

tech-stack:
  added: []
  patterns:
    - "Press feedback: Modifier.pressScale shares one MutableInteractionSource with the M3 Button so ripple, semantics and 48 dp target stay"
    - "Text field with custom border thickness: BasicTextField + OutlinedTextFieldDefaults.DecorationBox + Container"
    - "Exit-animation text kept via SideEffect-updated last non-null error"

key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/components/buttonstyles/PressScale.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/components/buttons/AsyncSaveButton.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/components/ValidatedField.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/components/EmptyState.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/components/PlaceholderTabContent.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/debug/GalleryControlsSection.kt
    - app/src/main/res/values/strings_gallery_controls.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/components/ControlsTest.kt
  modified: []

key-decisions:
  - "ValidatedField built on BasicTextField + DecorationBox because the value-based OutlinedTextField has no container slot for the 2 dp error border"
  - "pressScale placed before the caller modifier so the AsyncSaveButton glow shadow scales with the button"
  - "Rest border uses outline (not iOS outlineVariant) for 3:1 contrast, per UI-SPEC"

patterns-established:
  - "Stateless, Hilt-free components with a dark private @Preview wrapped in ClickMeTheme + Surface"
  - "ControlsTest: Robolectric + androidx.compose.ui.test.junit4.v2.createComposeRule, semantics asserted via SemanticsMatcher"

requirements-completed: [FND-10]

duration: 40min
completed: 2026-10-20
---

# Phase 1 Plan 06: Interactive Shared Components Summary

**PressScaleButton, AsyncSaveButton, ValidatedField, EmptyState/ErrorState and PlaceholderTabContent ported from iOS Views/Components under the UI-SPEC contract, with a debug gallery controls section and 14 Robolectric semantics tests.**

## Accomplishments
- `PressScaleButton` scales to 0.96 over 120 ms EaseInOut while pressed, keeps M3 ripple and a 48 dp minimum height; filled, tonal and destructive color sets.
- `AsyncSaveButton` shows Idle title, 24 dp spinner (Saving) or check + "Saved!" (Saved) at constant size via `Crossfade`; clicks forwarded only when enabled and Idle (T-01-19); Saving/Saved keep the primary look and glow, expose `stateDescription` "Saving"/"Saved", Saved is a polite live region.
- `ValidatedField` is a pill field with 1 dp outline at rest, 2 dp primary focused, 2 dp destructive on error; error message animates in/out and is announced through `error()` semantics; secure mode uses `PasswordVisualTransformation`, Password keyboard, no autocorrect or capitalization, `ContentType.Password` autofill, and a Show/Hide password toggle (T-01-18, value never logged).
- `EmptyState` requires heading and body (no defaults); `ErrorState` is the shared "Something went wrong" / "Retry connection" pattern; `PlaceholderTabContent` is the only user of `empty_state_placeholder_body`.
- `GalleryControlsSection` shows every control in every state, including an interactive Idle to Saving to Saved demo.

## Task Commits
1. Task 1: RED `882ec5c` (test), GREEN `375e680` (feat) - PressScale, AsyncSaveButton, EmptyState/ErrorState, PlaceholderTabContent
2. Task 2: RED `102b7b3` (test), GREEN `e1d5bde` (feat) - ValidatedField, gallery controls section, gallery strings

## Verification
- `ControlsTest`: 14/14 pass. `assembleDevDebug`, `ktlintCheck`, `lintDevDebug` and `scripts/check-design-rules.sh` green.
- Acceptance greps verified (SaveState enum, stateDescription, PRESSED_SCALE, collectIsPressedAsState, no heading/body defaults, empty_state_placeholder_body only in PlaceholderTabContent, no BuildConfig/hiltViewModel/ClickMePalette in components, ContentType import, `error(` semantics call, no Log/println, 25 non-translatable gallery strings).
- Visual checks (border thickness, focus color, press feel, 200% font scale) are deferred to plan 01-09 on a device.

## Deviations from Plan
None in code; plan executed as written. Notes:
- **Process:** worktree started at dc2172d, behind the expected base e871a29; reset to e871a29 per the startup check before any work.
- ktlint (max line length 120) required extracting `toggleIcon` in ValidatedField; test-file formatting was normalized by `ktlintFormat`.
- `error(error)` inside the semantics block resolves to `androidx.compose.ui.semantics.error` (explicit import wins over `kotlin.error`; confirmed by the passing error-semantics test).

## Known Stubs
`PlaceholderTabContent` renders the "Nothing here yet." stub by design (UI-SPEC Phase 1 placeholder); real tab content replaces it in later phases. `ErrorState` has no Phase 1 caller by design (Phase 15 consumer).

## Threat Flags
None. T-01-18 and T-01-19 mitigated as planned.

## Self-Check: PASSED
- Files present: all eight created files listed above.
- Commits present: 882ec5c, 375e680, 102b7b3, e1d5bde.

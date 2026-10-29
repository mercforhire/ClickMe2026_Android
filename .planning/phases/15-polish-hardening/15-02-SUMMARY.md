---
phase: 15-polish-hardening
plan: 02
subsystem: ui
tags: [orientation, large-screen, insets, cutout, test-helpers, tracer]
status: complete

requires: []
provides:
  - "MainActivity portrait lock (manifest, iOS parity)"
  - "Modifier.contentMaxWidth() / ContentMaxWidth = 600.dp"
  - "Horizontal display-cutout padding on the CallOverlayHost navigation root and pill"
  - "InsetsTestSupport Robolectric helpers (imeInsets, navigationBarInsets, cutoutInsets, combinedInsets, dispatchInsets)"
affects: [15-03, 15-04, 15-05, 15-06, 15-07, 15-08, 15-09, 15-10, 15-11, 15-12, 15-13, 15-14, 15-15, 15-16, 15-17]

tech-stack:
  added: []
  patterns:
    - "Per-screen content cap via Modifier.contentMaxWidth(); app bars and tab bar stay full width"
    - "Inset tests: createAndroidComposeRule<ComponentActivity>() + captured LocalView + dispatchInsets, w360dp-h800dp so dp == px"

key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/ui/theme/ContentWidth.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/ManifestOrientationTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/ui/theme/ContentMaxWidthTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/testing/InsetsTestSupport.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/testing/InsetsTestSupportTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/call/calling/CallOverlayHostInsetsTest.kt
  modified:
    - app/src/main/AndroidManifest.xml
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/CallOverlayHost.kt

key-decisions:
  - "tools:ignore=LockedOrientationActivity kept on MainActivity: lint reported the warning (RESEARCH A7 confirmed), and it is gone after the attribute"
  - "RESEARCH idiom for contentMaxWidth (fillMaxWidth/wrapContentWidth/widthIn/fillMaxWidth) passed unchanged in Box, Column and LazyColumn-item parents (A5 confirmed)"
  - "dispatchInsets is a single ComposeContentTestRule extension that dispatches to view.rootView and view, so callers need no rule.activity"

patterns-established:
  - "contentMaxWidth is applied per screen by the plan that owns the screen; there is no host-level clamp"

requirements-completed: [POL-03]

duration: ~35 min (dominated by cold Gradle builds)
completed: 2026-10-29
actuals:
  tokens: 14000
  tasks: 2
  commits: 2
plan_head_before: 283f0109be4ba4e6fb5984231ad407ad3bc15e88
plan_head_after: d5b13ea4e6d068580e4dfe28077eb1ad0a6168ec
---

# Phase 15 Plan 02: Orientation, large-screen cap and inset foundations Summary

Portrait lock on MainActivity, a 600 dp `contentMaxWidth()` modifier, horizontal cutout padding on the call overlay root and pill, and Robolectric inset helpers that later plans use for IME and bar assertions.

## Tasks

| Task | Name | Commit |
| ---- | ---- | ------ |
| 1 (tracer) | Portrait lock + 600 dp content cap | a7c91e03 |
| 2 | Inset test helpers + cutout padding on CallOverlayHost | d5b13ea4 |

## Verification

- ManifestOrientationTest 3/3, ContentMaxWidthTest 3/3, MainActivityIntentTest green (tracer gate re-run passed before Task 2).
- InsetsTestSupportTest 3/3, CallOverlayHostInsetsTest 2/2, CallOverlayHostTest 12/12, MeetingCallScreenTest 12/12.
- `:app:lintDevDebug` BUILD SUCCESSFUL; `LockedOrientationActivity` count 0 after the `tools:ignore`.
- `bash scripts/check-design-rules.sh` prints "Design rules: OK".
- Acceptance greps: `screenOrientation="portrait"` 1, `configChanges` 0, `requestedOrientation` in main 0, `WindowInsets.displayCutout` 2 in CallOverlayHost.kt.
- ktlint: every file this plan touched is clean (no entries in the ktlint reports).
- TDD RED seen: ManifestOrientationTest.mainActivityIsPortrait failed before the manifest change; CallOverlayHostInsetsTest.navigationContentClearsLeftCutout failed before the CallOverlayHost change. ContentMaxWidthTest and the pill test passed on first run (the modifier file was written before its test run, and the pill sits at BottomEnd so a left cutout alone is trivially cleared). The pill test therefore also dispatches a 40 px right cutout and asserts `right <= 320`, which fails without the pill's cutout padding.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] `DpRect.width` not resolvable in ContentMaxWidthTest**
- **Issue:** `bounds.width` needs an extra import; compile error.
- **Fix:** use `bounds.right - bounds.left`.
- **Commit:** a7c91e03

**2. [Rule 3 - Blocking] `setContent` cannot be called twice in one test**
- **Issue:** `capsInColumnAndLazyItem` first called `setContent` twice.
- **Fix:** one `setContent` that switches Column and LazyColumn on a `mutableStateOf` flag.
- **Commit:** a7c91e03

**3. [Plan shape] `dispatchInsets` signature**
- The helper dispatches to `view.rootView` and `view` instead of `rule.activity.window.decorView`, so it fits the plan's `ComposeContentTestRule.dispatchInsets(view, insets)` signature. Behaviour matches RESEARCH Code Example 3.

**4. [Strengthened test] `pillClearsLeftCutout`** also asserts the right side with a right cutout (see Verification).

## Deferred Issues (out of scope, pre-existing)

- `./gradlew ktlintCheck` fails on files this plan did not touch: `AccountSecurityViewModel.kt:208` (main), `ClientProfileSettingsViewModelTest.kt:98` and `DeleteAccountViewModelTest.kt:35` (test). The plan's `ktlintCheck` verify command therefore exits non-zero from those violations alone; the test tasks and all files of this plan pass. Whoever owns those files (the settings plans) should fix them.

## Open Items (device checks, deferred by the user)

- Real rotate/resize behaviour on sw600dp+ at targetSdk 37 (the system ignores the portrait lock there; Robolectric `sdk=35` cannot simulate it): device-checklist row for 15-17, verified in Robolectric at `w900dp` only.
- No resizable AVD exists locally (only `Medium_Phone_API_37.0`): create one for the D-02 resize check, device-checklist row.
- Real cutout phone: no text or control under the status bar, side cutout or navigation bar (UI Consideration "Cutouts and system bars", backstop verification).

## Known Stubs

None.

## Threat Flags

None. T-15-03 (no orientation on ConnectReturnActivity) and T-15-04 (no configChanges on MainActivity) are covered by `connectReturnActivityHasNoOrientation` and `mainActivityHandlesNoConfigChanges`.

## Self-Check: PASSED

All six created files exist; commits a7c91e03 and d5b13ea4 are ancestors of HEAD.

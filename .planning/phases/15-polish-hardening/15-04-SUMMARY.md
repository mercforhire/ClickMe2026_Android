---
phase: 15-polish-hardening
plan: 04
subsystem: accessibility
tags: [accessibility, talkback, semantics, contrast, test-helpers, tracer]
status: complete
requires: []
provides:
  - "ValidatedField keeps its label as an accessible name once filled"
  - "AsyncSaveButton busyDescription parameter"
  - "A11yAudit JVM semantics helper (names, 48 dp targets, traversal order, heading)"
  - "PaletteContrastTest pinning the UI-SPEC contrast table"
affects: [15-05, 15-06, 15-07, 15-08, 15-17]
tech-stack:
  added: []
  patterns:
    - "JVM semantics audit helper as extension functions on SemanticsNodeInteractionsProvider"
key-files:
  created:
    - app/src/test/java/com/uptrendinvestments/clickmeapp/testing/A11yAudit.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/testing/A11yAuditTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/ui/theme/PaletteContrastTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/components/ValidatedFieldA11yTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/components/buttons/AsyncSaveButtonTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/components/ValidatedField.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/components/buttons/AsyncSaveButton.kt
key-decisions:
  - "D-14 decision: jvm-audit-plus-gap (orchestrator-applied default, user may revisit)"
  - "ValidatedField keeps the name via contentDescription = placeholder only when value is non-empty"
  - "assertTouchTargets uses layout size (boundsInRoot / layoutInfo), not touchBoundsInRoot, because Compose pads touch bounds to 48 dp for every pointer-input node"
requirements-completed: [POL-04]
plan_head_before: 283f0109be4ba4e6fb5984231ad407ad3bc15e88
plan_head_after: cb09dd8d7791625979c7b52226fcb0bcef958920
commits: 2
actuals:
  tokens: 14000
  tasks: 3
  commits: 2
completed: 2026-10-29
---

# Phase 15 Plan 04: Accessible inputs, A11yAudit and contrast table Summary

Filled text fields keep their label as an accessible name, busy submit buttons take an overridable state description, and every Phase 15 flow plan now has a JVM `A11yAudit` helper plus a pinned `PaletteContrastTest`.

## What was built

- **ValidatedField** (`components/ValidatedField.kt`): when `value` is non-empty the field's semantics set `contentDescription = placeholder`. Empty fields keep the placeholder `Text` as the name. The value stays only in `EditableText`; a password value never reaches Text or ContentDescription. KDoc notes the spoken result is a device-checklist row (A6).
- **AsyncSaveButton**: new `busyDescription: String = stringResource(R.string.a11y_state_saving)` used as the Saving `stateDescription`. Existing callers unchanged.
- **A11yAudit.kt**: `assertActionablesNamed`, `assertTouchTargets(min = 48.dp)`, `traversalLabels`, `assertTraversalOrder`, `assertIsHeading`.
- **PaletteContrastTest**: 9 tests on the UI-SPEC contrast rules, WCAG 2.x relative luminance; the two banned pairs (onSurfaceMuted on surfaceContainerHighest and surfaceBright) assert `< 4.5`.

## Tests

- ValidatedFieldA11yTest 4/4, AsyncSaveButtonTest 3/3, A11yAuditTest 5/5, PaletteContrastTest 9/9.
- LoginScreenTest, SignupBasicInfoScreenTest, AccountSecurityScreenTest, ForgotPasswordScreenTest stay green.
- ktlint is clean for all files in this plan.

## D-14 decision

D-14 decision: jvm-audit-plus-gap (orchestrator-applied default, user may revisit).

Compose accessibility checks are a no-op under Robolectric (RESEARCH Pitfall 1), so `A11yAudit` plus `PaletteContrastTest` are the JVM gate and the checks that cannot run on the JVM are device-checklist rows. No `ui-test-junit4-accessibility` dependency and no `enableAccessibilityChecks()` was added.

**Phase 16 gap:** add an `androidTest` source set with `ui-test-junit4-accessibility` and call `enableAccessibilityChecks()` on the four SC-4 flows, run on the emulator (D-16 tier 1). 15-17 copies this into the checklist's "Gap list for Phase 16" and the STATE.md Phase 16 hand-off.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] assertTouchTargets cannot use touchBoundsInRoot**
- **Found during:** Task 3
- **Issue:** `touchBoundsInRoot` is padded to 48 dp for every pointer-input node, so a 40 dp raw clickable passed. Using `boundsInRoot` alone failed a Material `IconButton`, whose 48 dp comes from `minimumInteractiveComponentSize` outside the clickable's semantics bounds.
- **Fix:** effective size is `max(boundsInRoot, layoutInfo width/height)`; this fails the 40 dp Box and passes `IconButton`. Documented in the KDoc.
- **Files modified:** `testing/A11yAudit.kt`
- **Commit:** cb09dd8d

The D-14 checkpoint (Task 2) was answered by the orchestrator with the plan default; no stop.

## Deferred Issues

Out-of-scope, pre-existing ktlint violations in files this plan did not touch (`./gradlew :app:ktlintCheck` fails on them): `AccountSecurityViewModel.kt`, `ClientProfileSettingsViewModelTest.kt`, `DeleteAccountViewModelTest.kt`. Running the plan's literal verify command with `ktlintCheck` therefore exits non-zero on the base commit until those are fixed; my files report no violations.

## Open items (device checklist, nothing blocks on a person)

- TalkBack speech for a field carrying both a name and a value (A6): device-checklist row for 15-17. Semantics tests prove exposure only (Pitfall 9).
- Every SC-4 flow keeps an open TalkBack row.
- Accessibility Scanner on the Samsung.

## Known Stubs

None.

## Threat Flags

None. T-15-07 mitigated by `passwordValueNeverNamed`; T-15-08 by the open TalkBack rows above.

## Commits

- 966aedc1 feat(15-04): keep field name when filled, busyDescription on AsyncSaveButton
- cb09dd8d test(15-04): A11yAudit semantics helper and palette contrast table

## Self-Check: PASSED

All created files exist and both commits are ancestors of HEAD.

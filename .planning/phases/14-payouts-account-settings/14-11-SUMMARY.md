---
phase: 14-payouts-account-settings
plan: 11
subsystem: expert-payouts
tags: [tax-profile, tin, form, date-picker, privacy, compose]

requires:
  - phase: 14-payouts-account-settings
    provides: "14-01 tax profile endpoints, DTOs, FakeClickMeApi handlers and fixture; 14-02 SettingsOrigin"
provides:
  - "Tax & Payout Info screen (ExpertTaxProfileScreen / ExpertTaxProfileContent) with load, edit, save and the iOS TIN handling"
  - "TaxProfileKey(origin) nav key (14-14 registers it; 14-03 and 14-10 link to it)"
  - "TaxProfileRepository (stateless GET/POST wrapper)"
affects: [14-14, 14-03, 14-10]

tech-stack:
  added: []
  patterns:
    - "Write-only secret: memory-only form with a redacting toString, no saved-state handle, TIN cleared after saved: true"
    - "Server 'not yet' answers (saved: false, tin_required) are a banner plus focus, never an error dialog"
    - "One-shot UI events as boolean state flags consumed by the screen (savedEvent, focusTinEvent)"

key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/taxprofile/TaxProfileKey.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/taxprofile/TaxProfileRepository.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/taxprofile/ExpertTaxProfileViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/taxprofile/ExpertTaxProfileScreen.kt
    - app/src/main/res/values/strings_tax_profile.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/taxprofile/ExpertTaxProfileViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/taxprofile/ExpertTaxProfileScreenTest.kt
  modified: []

key-decisions:
  - "The TIN field uses a custom, unrecognised autofill content type instead of ValidatedField's default Password hint, so password managers are not offered the TIN, without touching the shared ValidatedField"
  - "A saved: false answer without tin_required opens the save-error dialog (never read as success); only tin_required is a banner"
  - "Country input is uppercased and capped at two letters (UI-SPEC); the DOB picker also blocks future days"
  - "A stored date of birth outside 1900..current year starts the picker empty (the Material picker throws on an out-of-range initial date)"

patterns-established:
  - "Disabled BasicTextField nodes have no SetText action: tests match EditableText plus the tag ancestor to assert the locked state"

requirements-completed: [PAYOUT-04]

duration: ~70min
completed: 2026-10-29
status: complete
commits: 4
plan_head_before: 4b40aff415a7e5278fcf17fe862865fb5524c4e7
plan_head_after: c2834c0001be5619b1cecac998cdcebda08e102b
actuals:
  tokens: 16400
  tasks: 2
  commits: 4
---

# Phase 14 Plan 11: Tax & Payout Info Summary

**Tax & Payout Info form that prefills the masked server profile, saves with the iOS TIN rules (write-only, cleared after saving, `tin_required` as a prompt), validates inline from 422s, and enters the date of birth with a Material date picker.**

## Accomplishments

- Load: `GET expert/tax-profile` prefills legal name, DOB, street, apt, city, region (falling back to province), postal code, country (CA when empty) and the financial account; the TIN is always empty. First load shows the skeleton (three text lines, then four 56dp blocks per section); a failed load shows "Couldn't load tax profile" with the server message (else the network line) and Retry.
- Save: enabled only when the seven required fields are non-blank (iOS `canSave`). Sends the strict body (trimmed values, blank line 2, financial account and TIN omitted, country uppercased). `saved: true` shows the "Tax profile saved." snackbar, clears the TIN and shows "TIN on file" with the field relabelled "Replace TIN (optional)".
- `saved: false, tin_required: true` is a banner (server message else the fallback), keeps every field, focuses the TIN field and leaves `tinOnFile` false.
- 422 `errors[]` map to the TIN, date of birth, legal name, postal code (last path segment) and the other form fields inline; unknown fields and other failures open "Couldn't save tax profile" ("Got it"); a network failure uses "Check your connection and try again."
- While saving: fields disabled, spinner on the button, repeat taps send nothing (state guard set before the coroutine launches), top-bar Back disabled and system Back swallowed.
- Date of birth: read-only `PickerField` opening a Material `DatePickerDialog` (1900 to the current year, no future days), shown as `MMM d, yyyy`, sent as `yyyy-MM-dd`.
- Privacy (T-14-31): memory-only form, redacting `toString`, no saved-state handle, no logging, TIN masked.

## Task Commits

TDD RED/GREEN per task:

1. Task 1 (tracer): RED `d071d20`, GREEN `69d80f9`
2. Task 2: RED `29bb189`, GREEN `c2834c0`

## Verification

- `ExpertTaxProfileViewModelTest` 19 tests and `ExpertTaxProfileScreenTest` 13 tests pass (32 total); `ktlintCheck`, `:app:assembleDevDebug` and `scripts/check-design-rules.sh` pass.
- Acceptance greps: `DEFAULT_TAX_COUNTRY = "CA"` 1, `SavedStateHandle` 0, `DatePickerDialog` 2, `tinRequired` 1.
- Tracer gate: the Task 1 slice (load, edit, save, TIN cleared and shown as on file) was exercised end to end by the real-view-model screen test `formAndSave` before Task 2 expanded it.

## TDD Gate Compliance

RED commits `d071d20` and `29bb189` precede their GREEN commits. RED used compiling stubs so the target tests failed on assertions (8 of 9 in Task 1 and 11 of 32 in Task 2); `theTinNeverAppearsInThePrintedForm` already passed in Task 1 because the redacting `toString` was part of the stub data class by design. The `check tdd-red-evidence` classifier was not run (`workflow.tdd_mode` is not enabled in this worktree and the JUnit report format is not among its adapters); the semantic assessment was done by reading the failing assertions in the test report.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing critical functionality] TIN autofill hint**
- **Found during:** Task 1
- **Issue:** `ValidatedField(isSecure = true)` defaults the autofill content type to `ContentType.Password`, which would offer the TIN to password managers; UI-SPEC and the plan say the TIN has no autofill hint.
- **Fix:** Pass a custom, unrecognised `ContentType("taxIdentificationNumber")` through the existing `contentType` parameter. The shared `ValidatedField` is untouched (parallel wave, not in this plan's files).
- **Files modified:** ExpertTaxProfileScreen.kt
- **Commit:** 69d80f9

**2. [Rule 1 - Bug] Date picker crash on an out-of-range stored date**
- **Found during:** Task 2
- **Issue:** `rememberDatePickerState` throws when `initialSelectedDateMillis` falls outside `yearRange`; a stored date of birth before 1900 or a future year would crash the dialog.
- **Fix:** An out-of-range or unparsable stored date starts the picker with no selection.
- **Commit:** c2834c0

### Scope notes (not failures)

- The loading skeleton and the failed-load `EmptyState` landed in Task 1 (the screen needs them to be complete); Task 2 added their tests, the banner, TIN focus, back lock and the date picker. Task 1's `grep` baselines (no `tinRequired`, no `DatePickerDialog`) still held.
- Added beyond the plan, all inside this plan's files: a `saved: false` answer without `tin_required` opens the save dialog instead of being ignored (T-14-33 intent); the country field is capped at two uppercase letters; the picker blocks future days; 422 field mapping also covers street, apt, city, region, country and financial account.

## Known Stubs

None.

## Threat Flags

None. T-14-31 (TIN retention): redacting `toString` and `theTinNeverAppearsInThePrintedForm`, no saved-state handle (grep gate 0), TIN cleared after `saved: true` (`saveSendsStrictBodyAndClearsTin`, `formAndSave`). T-14-32: request type carries schema keys only (`saveSendsStrictBodyAndClearsTin`). T-14-33: `tinOnFile` set only on `saved: true` (`tinRequiredIsAPrompt`).

## Open Items

- Device checks (deferred per project policy): the masked TIN field and the date picker on the Samsung device, and that no password manager offers to save the TIN.
- RESEARCH A6 "Tax gating on payouts" stays open (built ungated, backend answer pending in `14-BACKEND-REQUEST.md`).
- 14-14 must register `TaxProfileKey` in both shells' `detailEntries` and call `ExpertTaxProfileScreen(onBack = ...)`.

## Self-Check: PASSED

- All seven created files exist on disk.
- Commits d071d20, 69d80f9, 29bb189 and c2834c0 are ancestors of HEAD.

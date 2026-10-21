---
phase: 04-signup-onboarding-mode-switch
plan: 12
subsystem: signup
tags: [compose, form, picker, languages, countries, viewmodel]
requires:
  - phase: 04-07
    provides: SignupSetupRepository.saveBasicInfo, SignupDraft, SignupTestHarness
  - phase: 04-04
    provides: PickerField, SelectableChip, PickerSheet, PickerRow, PickerLoading
  - phase: 04-02
    provides: Countries
  - phase: 04-01
    provides: ClickMeApi.getLanguages, LanguageItem
provides:
  - SignupBasicInfoViewModel (form, country sheet, language sheet, save state)
  - SignupBasicInfoScreen and stateless SignupBasicInfoContent
  - CountryPickerSheet and LanguageSelectionSheet (plus stateless Content bodies)
  - strings_signup_basic_info.xml
affects: [04-19]
tech-stack:
  added: []
  patterns:
    - "Form seeded from the shared draft until first edit, mirrored to SavedStateHandle"
    - "Sheet state (query, working selection, catalog) lives in the ViewModel UI state"
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/basicinfo/SignupBasicInfoViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/basicinfo/SignupBasicInfoScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/basicinfo/subviews/CountryPickerSheet.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/basicinfo/languageselection/LanguageSelectionSheet.kt
    - app/src/main/res/values/strings_signup_basic_info.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/signup/basicinfo/SignupBasicInfoViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/signup/basicinfo/SignupBasicInfoScreenTest.kt
  modified: []
key-decisions:
  - "Reopening the language sheet after a failed load retries automatically; a loaded catalog is never fetched again"
  - "Province/State uses ImeAction.Done (last text field, UI-SPEC IME rule); First Name and City use Next"
  - "onSavedConsumed resets saveState to Idle so a returning screen shows an active Save button"
requirements-completed: [SIGN-02]
duration: long (first Gradle build in the worktree took about 15 min)
completed: 2026-10-21
---

# Phase 4 Plan 12: Personal Details Summary

**Personal Details step: First Name, City, optional Province/State, searchable 50-country picker and searchable multi-select language picker (GET /meta/languages) with English pre-selected, tap-order languages, always-enabled Save through SignupSetupRepository with a 600 ms Saved hold and inline 422 mapping.**

## Accomplishments
- `SignupBasicInfoViewModel` seeds from the draft (English only when the draft has no languages), re-seeds on a late hydrate until the first edit, and mirrors the form to `SavedStateHandle` (rotation and process death).
- Language sheet keeps a working selection in tap order (D-14), loads the catalog once per ViewModel, supports retry, Cancel discards and Done applies. No primary-language helper (D-24).
- Save calls `repository.saveBasicInfo` (which sends only filled fields and normalizes the country, D-04/D-05), holds Saved for `Motion.SAVED_CONFIRMATION_HOLD_MS`, then raises `saved`. A 422 maps `first_name`, `location.city`/`city`, `location.province_state`, `location.country_code`, `languages*` to inline errors; anything else shows a "Couldn't save" dialog.
- Screen follows UI-SPEC screen 3 with the Save button pinned above the keyboard; fields and chips are inert while saving.

## Task Commits
1. Task 1: SignupBasicInfoViewModel and tests - `bf7ca03`
2. Task 2: Screen, country and language sheets, strings, screen tests - `b9c6e78`
3. ktlint formatting of the two test files - `f3b6f72`

## Verification
- `:app:testDevDebugUnitTest --tests '*SignupBasicInfo*'`: 32 tests (18 ViewModel, 14 screen), 0 failures.
- `ktlintCheck` and `lintDevDebug` pass; `scripts/check-design-rules.sh` prints "Design rules: OK".
- Acceptance greps: single `repository.saveBasicInfo(`, `api.getLanguages()`, `LanguageItem("en", "English")`, `"location.city"`, `"location.country_code"` and `Motion.SAVED_CONFIRMATION_HOLD_MS` lines in the ViewModel; no "primary language" string in `strings_signup_basic_info.xml`.

## Deviations from Plan
- Tests and implementation were written together and committed once per task rather than as separate RED/GREEN commits (same as plans 04-04 and 04-07).
- Screen tests call `performScrollTo()` before clicking the country field, Add language row and chip, because the Robolectric viewport is smaller than the form.
- Task 1 tests initially failed to compile only because the screen test file existed before the screen did; resolved by finishing Task 2 before the first full run. No behavioral change.

None otherwise: no auto-fix rules triggered.

## Known Stubs
None.

## Threat Flags
None. T-04-32 (trim and typed DTO, 422 shown inline) and T-04-34 (only catalog ids selectable) are implemented as planned; T-04-33 accepted.

## Manual checks (deferred by project policy)
- On device: open the country and language sheets with the keyboard up and confirm search stays above the IME.
- On device: rotate the form mid-edit and confirm typed values and chips remain.
- On device: kill the process with the form open and confirm values restore.
- TalkBack: "Remove English" is read for the chip; Country reads "Country, Canada".

## Self-Check: PASSED
All seven created files exist; commits `bf7ca03`, `b9c6e78` and `f3b6f72` exist; ktlintCheck, lintDevDebug and design rules pass.

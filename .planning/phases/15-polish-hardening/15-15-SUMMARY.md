---
phase: 15-polish-hardening
plan: 15
subsystem: settings
tags: [settings, saved-state, process-death, secrets, ime, tracer]
status: complete

requires: [15-02]
provides:
  - "ClientProfileSettingsViewModel, ExpertProfileSettingsViewModel, ExpertTaxProfileViewModel, DeleteAccountViewModel take a SavedStateHandle and mirror non-secret input with a touched flag"
  - "Restored profile edits win over the fetched profile and are saved through the existing debounced AutoSaver"
  - "ExpertTaxProfileScreen snackbar clears the navigation bar and the keyboard (navigationBarsPadding + imePadding)"
  - "contentMaxWidth() on ClientProfileSettings, ExpertProfileSettings and ExpertTaxProfile"
  - "ClientProfileSettingsInsetsTest: IME and large-window proof for one Settings form"
affects: [15-17, 15-20]

tech-stack:
  added: []
  patterns:
    - "Per-field mirror: only edited fields are written to the handle; the VM lays them over the server snapshot in seed()/load() and, if the result differs from the server baseline, calls AutoSaver.onEdited like a keystroke"
    - "Language lists are stored as two parallel ArrayList<String> (ids, with \"\" for a custom language, and labels)"
    - "Secret/sensitive fields have no saved-state key at all (savedStateKey() returns null) and are asserted absent by value and by key in tests"

key-files:
  created:
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/settings/clientprofilesettings/ClientProfileSettingsInsetsTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/clientprofilesettings/ClientProfileSettingsViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/clientprofilesettings/ClientProfileSettingsScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/settings/ExpertProfileSettingsViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/settings/ExpertProfileSettingsScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/taxprofile/ExpertTaxProfileViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/taxprofile/ExpertTaxProfileScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/deleteaccount/DeleteAccountViewModel.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/settings/clientprofilesettings/ClientProfileSettingsViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/settings/clientprofilesettings/ClientProfileSettingsScreenTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/settings/ExpertProfileSettingsViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/settings/ExpertProfileSettingsScreenTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/taxprofile/ExpertTaxProfileViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/taxprofile/ExpertTaxProfileScreenTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/settings/deleteaccount/DeleteAccountViewModelTest.kt

key-decisions:
  - "The financial account identifier is not mirrored either (plan said every field except the TIN): it is bank detail, the plan's behavior list does not restore it, and the must-have says 'non-secret fields'. It comes back empty like the TIN."
  - "A successful Tax save clears the touched flag, so a later recreation shows the server's data rather than the stale draft."
  - "DeleteAccountViewModel's SavedStateHandle parameter has a SavedStateHandle() default so DeleteAccountScreenTest (owned by 15-20) and any 15-20 test that builds the VM by hand keep compiling and merge without a conflict; Hilt always supplies the real handle."
  - "Tax date of birth is stored as the ISO yyyy-MM-dd String (the form's own value, written by onFieldChange for both typed and picked values) instead of epoch millis as Long; both are Bundle-safe and one path avoids a second source of truth."

patterns-established:
  - "Restoration test for settings forms: build a second ViewModel from a copy of the first handle's entries, then assert snapshot equality, server-vs-restored precedence and one debounced save"

requirements-completed: [POL-02, POL-03]

duration: ~1.5 h wall clock (Gradle runs 1-2 min each on a quiet machine)
completed: 2026-10-29
actuals:
  tokens: 24000
  tasks: 3
  commits: 3
plan_head_before: 075bfb8190ba39be5a580efa1bfd98bff5bfd193
plan_head_after: 97f197d50ee5109fa4b339d9ec6bc62e7a7abdda
---

# Phase 15 Plan 15: Settings forms restoration, secrets and keyboard Summary

Client Profile Settings, Expert Profile Settings, Tax & Payout Info and Delete Account now restore their typed non-secret input after Activity recreation and process death, with the TIN, the account identifier and the deletion password never entering SavedStateHandle.

## Tasks

| Task | Name | Commit |
| ---- | ---- | ------ |
| 1 (tracer, tdd) | Client Profile Settings restoration, auto-save of restored edits, IME and width proof | cd844caa |
| 2 (tdd) | Expert Profile Settings restoration including the meeting link | 8f21191e |
| 3 (tdd) | Tax & Payout Info and Delete Account restoration, snackbar insets, width cap | 97f197d5 |

## What changed

- **Client / Expert Profile Settings:** every `onFieldChange`, `applyProfessional`, `selectCountry` and `applyLanguages` writes the clamped value to the handle and sets a `*_touched` flag (`client_settings_*`, `expert_settings_*`). When the profile arrives, `seed()` rebases the saver on the server snapshot, lays the restored fields over it and, if the result differs, calls `saver.onEdited(restored)`, so a restored edit is saved after the normal 1.2 s debounce (T-15-32: restored text is clamped to the field maximum again). Expert additionally restores the typed meeting-link text but re-validates it: a restored blank or invalid link keeps the server link in the snapshot and is never sent.
- **Tax & Payout Info:** legal name, address lines, city, region, postal code, country and date of birth are mirrored (`tax_profile_*`); the TIN and the financial account identifier have no key. A successful save clears the touched flag.
- **Delete Account:** step, reason (enum name) and comments (re-capped at 1000) are mirrored (`delete_account_*`); the password has no key.
- **Screens:** `contentMaxWidth()` on all three forms; Tax snackbar host is `SnackbarHost(state, Modifier.navigationBarsPadding().imePadding())`.

## RED / GREEN evidence

- Implementation for each task was written before its tests (the restoration tests need the new constructor parameter to compile, so a compile-level RED would only prove a missing parameter). The behavior is new for every ViewModel (none held a SavedStateHandle before), so each new restoration test could not have passed on the base commit.
- The one test that pins a pre-existing weakness was checked RED explicitly: with the Tax snackbar host reverted to `SnackbarHost(snackbarHostState)`, `ExpertTaxProfileScreenTest.snackbarAboveNavBarAndIme` fails (AssertionError at the nav-bar or IME bound check); with the fix it passes.
- Test results (Robolectric, from the Gradle XML reports): ClientProfileSettingsViewModelTest 33, ClientProfileSettingsScreenTest 25, ProfileAutoSaveTest 15, ClientProfileSettingsInsetsTest 2, ExpertProfileSettingsViewModelTest 20, ExpertProfileSettingsScreenTest 8, ExpertTaxProfileViewModelTest 25, ExpertTaxProfileScreenTest 15, DeleteAccountViewModelTest 18, DeleteAccountScreenTest 14: 0 failures. `ktlintCheck` clean. `scripts/check-design-rules.sh`: "Design rules: OK".
- Tracer gate: Task 1's verify command passed end to end before Tasks 2 and 3 expanded.

## New tests

- Client VM: `typedFieldsRestored`, `restoredBeatsServer`, `untouchedSeedsFromServer`, `restoredEditIsAutoSaved`, `onlyBundleTypes`.
- Client insets: `focusedFieldAboveIme` (300 px IME on 360x800: last text field and its error line bottom <= 500 dp), `capped` (<= 600 dp on 900x600).
- Expert VM: the same five plus `blankMeetingLinkNeverSent`.
- Tax VM: `nonSecretFieldsRestored`, `tinNeverInSavedState` (by value and by key, also covers the account identifier), `restoredTinIsEmptyWithoutError`, `restoredBeatsServer`, `untouchedSeedsFromServer`, `aSavedFormIsNotRestoredOverTheServer`.
- Tax screen: `snackbarAboveNavBarAndIme` (nav bar 48 px -> bottom <= 752 dp; IME 300 px -> <= 500 dp), `capped`.
- Delete VM: `reasonFeedbackAndStepRestored`, `backToWarningIsRestored`, `restoredFeedbackIsCapped`, `passwordNeverInSavedState`, `restoredPasswordEmptyWithoutError`.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Existing tests that construct the changed ViewModels by hand**
- **Found during:** Tasks 1 and 2
- **Issue:** `ClientProfileSettingsScreenTest` and `ExpertProfileSettingsScreenTest` (not in the plan's file list) call the ViewModel constructors, which now take a `SavedStateHandle`.
- **Fix:** pass `SavedStateHandle()` in the one constructor call each.
- **Files modified:** the two ScreenTest files
- **Commits:** cd844caa, 8f21191e

**2. [Rule 2 - Missing critical] Financial account identifier kept out of saved state**
- **Found during:** Task 3
- **Issue:** the plan says to mirror every Tax field except the TIN, but the account identifier is bank detail and the plan's own behavior list restores only name, address and date of birth.
- **Fix:** no saved-state key for it; `tinNeverInSavedState` asserts it is absent as well.
- **Commit:** 97f197d5

**3. [Design choice] DeleteAccountViewModel handle has a default**
- To keep 15-20's `DeleteAccountScreenTest` untouched and merge-safe (see key-decisions). Also: the ExpertTaxProfileViewModel KDoc that said "no saved-state handle (T-14-31)" was rewritten, since this plan deliberately reverses it for non-secret fields.

## Auth gates

None.

## Known Stubs

None.

## Threat Flags

None. No new endpoints or storage; saved state now holds only short Strings, Booleans and string lists (T-15-33).

## Open items (device checks deferred by the user)

- Process death with "Don't keep activities" on the Samsung device: edit each of the four forms, background, return; confirm typed values, the country, languages and meeting link reappear, that the TIN, account identifier and deletion password come back empty with no error text, and that a restored profile edit is saved ("Saved" indicator) a moment after return.
- Visual check of the 600 dp cap on a tablet or resized window for the three forms, and of the Tax "saved" snackbar above the gesture bar and keyboard.

## Self-Check: PASSED

- Created file present: `ClientProfileSettingsInsetsTest.kt` (FOUND).
- Commits cd844caa, 8f21191e, 97f197d5 are ancestors of HEAD (FOUND).
- `grep -ciE 'savedStateHandle\[[^]]*(tin|password)'` prints 0 for ExpertTaxProfileViewModel.kt and DeleteAccountViewModel.kt.

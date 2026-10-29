---
phase: 14-payouts-account-settings
plan: 07
subsystem: ui
tags: [profile-settings, auto-save, debounce, compose, patch, tdd]

requires:
  - phase: 14-payouts-account-settings
    provides: "14-01 network contract (updateUserProfile, UpdateUserProfileRequest groups, FakeClickMeApi handlers, SettingsFixtures)"
provides:
  - "AutoSaver engine (debounce, serialized saves, baseline-after-200, flush on the app scope) reusable by the expert settings screen"
  - "ProfileSnapshot and buildProfilePatch (changed-groups-only PATCH, languages included, no email)"
  - "ClientProfileSettingsScreen / ClientProfileSettingsContent / ClientProfileSettingsViewModel / ClientProfileSettingsKey"
  - "Shared field composables: AutoSaveStatusIcon, ProfileTextField, ProfileReadOnlyField, ProfileBioField, ProfileProfessionalCard, ProfessionalDetailsSheet"
affects: [14-12 avatar/languages/country, 14-13 expert settings, 14-14 navigation wiring]

actuals:
  tokens: 23000
  tasks: 3
  commits: 6

plan_head_before: 4b40aff415a7e5278fcf17fe862865fb5524c4e7
plan_head_after: 945004212c809e1788d84f1b7d66e65c7a546363

tech-stack:
  added: []
  patterns:
    - "Debounced auto-save: one cancellable debounce job that launches the save as a separate job, a Mutex serializing saves, baseline moved only after success"
    - "Flush on leave via an injected @ApplicationScope scope, with ViewModel.onCleared as a backstop"
    - "Stateless sheet body (ProfessionalDetailsBody) split from ModalBottomSheet wrapper for window-free tests"

key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/clientprofilesettings/ProfileAutoSave.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/clientprofilesettings/ProfileSnapshot.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/clientprofilesettings/ProfileSettingsRepository.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/clientprofilesettings/ClientProfileSettingsKey.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/clientprofilesettings/ClientProfileSettingsViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/clientprofilesettings/ClientProfileSettingsScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/clientprofilesettings/subviews/ProfileFields.kt
    - app/src/main/res/values/strings_profile_settings.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/settings/clientprofilesettings/ProfileAutoSaveTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/settings/clientprofilesettings/ClientProfileSettingsViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/settings/clientprofilesettings/ClientProfileSettingsScreenTest.kt
  modified: []

key-decisions:
  - "Saves launch as their own job from the debounce job, so a later edit cancelling the debounce never cancels a PATCH in flight"
  - "A snapshot that differs only by whitespace persists nothing (groups compared on trimmed values) and the baseline still moves"
  - "A 422 maps to fields only when errors[] names one of this screen's fields (suffix after the last dot); otherwise the dialog opens"
  - "Save-failure fallback copy for an Http error without a server message is login_error_network (\"Check your connection and try again.\")"
  - "ProfileTextField takes a leading icon because the shared ValidatedField requires one; ValidatedField was not changed (parallel executors)"

patterns-established:
  - "AutoSaver<S>: onEdited / flush / retry / rebase over any snapshot type; the expert screen (14-13) reuses it unchanged"

requirements-completed: [PROF-02]

coverage:
  - id: D1
    description: "An edit on Profile Settings is saved by a PATCH 1.2 s after the last change; a burst gives one PATCH carrying the final values; no Save button and no discard dialog"
    requirement: PROF-02
    verification:
      - kind: unit
        ref: "ProfileAutoSaveTest#debouncesBurstIntoOneSave"
        status: pass
      - kind: unit
        ref: "ClientProfileSettingsViewModelTest#burstOfEditsSendsOnePatchWithTheFinalValue"
        status: pass
      - kind: automated_ui
        ref: "ClientProfileSettingsScreenTest#fieldsInOrder"
        status: pass
    human_judgment: false
  - id: D2
    description: "The PATCH carries only the changed groups; an equal snapshot sends nothing; the baseline moves only after a 200"
    requirement: PROF-02
    verification:
      - kind: unit
        ref: "ProfileAutoSaveTest#buildPatchIncludesOnlyChangedGroups"
        status: pass
      - kind: unit
        ref: "ProfileAutoSaveTest#failureKeepsBaselineAndRetrySavesLatest"
        status: pass
    human_judgment: false
  - id: D3
    description: "App-bar save indicator: idle empty slot, Saving spinner, Saved check, Error glyph that retries on tap"
    requirement: PROF-02
    verification:
      - kind: automated_ui
        ref: "ClientProfileSettingsScreenTest#statusIconStates"
        status: pass
    human_judgment: true
    rationale: "The 150 ms cross-fade, the 2 s fade back to idle on screen and TalkBack announcements are not asserted by a test"
  - id: D4
    description: "A failed save opens the Couldn't save changes dialog (server message else the connection copy) or shows the 422 message under its field"
    requirement: PROF-02
    verification:
      - kind: unit
        ref: "ClientProfileSettingsViewModelTest#failureOpensDialogAndErrorIcon"
        status: pass
      - kind: unit
        ref: "ClientProfileSettingsViewModelTest#validationErrorMapsToField"
        status: pass
      - kind: automated_ui
        ref: "ClientProfileSettingsScreenTest#saveFailureDialogShowsTitleMessageAndGotIt"
        status: pass
    human_judgment: false
  - id: D5
    description: "Back with a pending edit saves it at once on the application scope, then pops; the pop is never blocked"
    requirement: PROF-02
    verification:
      - kind: automated_ui
        ref: "ClientProfileSettingsScreenTest#backFlushesPendingEditThenPops"
        status: pass
      - kind: unit
        ref: "ProfileAutoSaveTest#flushSavesPendingInFlushScope"
        status: pass
    human_judgment: false
  - id: D6
    description: "Email is read-only, blank optional fields are valid, limits (names 100, bio 500 with counter from 400, job title and company 200) clamp input"
    requirement: PROF-02
    verification:
      - kind: unit
        ref: "ClientProfileSettingsViewModelTest#limitsClampInput"
        status: pass
      - kind: automated_ui
        ref: "ClientProfileSettingsScreenTest#bioCounterShowsAt400"
        status: pass
    human_judgment: false
  - id: D7
    description: "Professional card opens Edit Professional Details; Update details applies and auto-saves, Discard changes applies nothing"
    requirement: PROF-02
    verification:
      - kind: unit
        ref: "ClientProfileSettingsViewModelTest#updateDetailsAppliesAndSaves"
        status: pass
      - kind: automated_ui
        ref: "ClientProfileSettingsScreenTest#sheetDiscardAppliesNothing"
        status: pass
    human_judgment: true
    rationale: "The real ModalBottomSheet window (drag, scrim, IME) is only rendered by the Content-level test; on-device behavior is unverified"
  - id: D8
    description: "Successful saves refresh UserManager.profile so the hub card and top-bar avatar show the new values"
    requirement: PROF-02
    verification:
      - kind: unit
        ref: "ClientProfileSettingsViewModelTest#firstNameEditPatchesBasicGroupOnly"
        status: pass
    human_judgment: false

duration: ~95 min (about 20 min of it was a cold Gradle build)
completed: 2026-10-29
status: complete
---

# Phase 14 Plan 07: Client Profile Settings with Auto-Save Summary

**Client Profile Settings screen with iOS-style auto-save: a 1.2 s debounced PATCH of only the changed groups, a four-state app-bar indicator, an error dialog with retry, and a flush on Back that runs on the application scope so the last edit is never lost.**

## Accomplishments

- `AutoSaver<S>` engine: one debounce timer, saves launched as separate jobs and serialized by a `Mutex`, baseline moves only after a successful `persist`, equal snapshots are skipped, `flush()` saves on the injected flush scope (the app scope) so leaving the screen cannot cancel it.
- `buildProfilePatch` sends only groups that changed (basic, professional, location, languages) with trimmed values; an empty string clears a field; blank country becomes null; languages split into catalogue ids and custom labels; no email key.
- `ClientProfileSettingsViewModel` seeds from the cached profile (one refresh when the cache is empty), clamps every field to its server maximum, maps a 422 whose `errors[]` names a field (prefixed or bare) to that field, otherwise opens the dialog; `flushPendingSave()` and `onCleared()` both flush.
- Screen: read-only email, first name, last name, bio (120 dp, counter from 400, tertiary at 500), Professional Details card, city, state/province; no Save button, no discard dialog; the app-bar `AutoSaveStatusIcon` is a fixed 48 dp slot with Idle, Saving, Saved ("Changes saved", polite live region) and Error ("Couldn't save changes. Tap to retry.").
- Professional Details card (one `Role.Button`, dash for empty) and `ProfessionalDetailsSheet` (`ModalBottomSheet`, working copy, "Update details" / "Discard changes").
- Two iOS bugs not copied: the pending debounce is flushed on leave, and languages are part of the patch builder.

## Task Commits

TDD RED then GREEN per task:

1. Task 1 (tracer): RED `fdfd6a8`, GREEN `fcd9c49`
2. Task 2: RED `5f89245`, GREEN `3dd41a7`
3. Task 3: RED `37ec410`, GREEN `9450042`

Tests (all green): `ProfileAutoSaveTest` 14, `ClientProfileSettingsViewModelTest` 15, `ClientProfileSettingsScreenTest` 14 = 43. `ktlintCheck`, `:app:assembleDevDebug` and `scripts/check-design-rules.sh` ("Design rules: OK") pass.

## TDD Gate Compliance

Each task has a `test(14-07)` commit before its `feat(14-07)` commit. RED evidence: Task 1 used stubs that throw `NotImplementedError`, so the run compiled and all 17 target tests executed and failed (semantic assessment: they failed because the behavior did not exist, not because of a compile, import or fixture fault). Task 2 RED failed exactly on `fieldsInOrder` (City missing) and the two 422 field-mapping tests; tests that already passed (limits, dialog, retry) were kept as regression coverage. Task 3 RED failed on 7 targets (card, sheet, apply, clamp). `discardChangesLeavesValues` passed at RED because the stub was a no-op, which is the correct end state.

## Tracer gate

Task 1 was the tracer. Its `<verify>` command (the two target test classes plus `ktlintCheck`) was re-run after the commit with all tests passing, so `Tracer verified end-to-end - expanding` was logged before Tasks 2 and 3. The mixed Compose-in-runTest test (`backFlushesPendingEditThenPops`) confirmed the Screen glue (flush then pop) end to end.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Sheet body renamed `ProfessionalDetailsBody`**
- **Found during:** Task 3 acceptance check
- **Issue:** `grep -c 'fun ProfessionalDetailsSheet'` must equal 1, but the stateless body `ProfessionalDetailsSheetContent` also matched.
- **Fix:** renamed the body composable to `ProfessionalDetailsBody`.
- **Commit:** `9450042`

### Plan-shaped adjustments (not rule-driven)

- **No suggested-role chips (D-12):** the signup flow has no job-title editor or suggested-roles list (`SignupTagsScreen` suggestions are expertise tags), so the sheet shows only the two fields, as the plan allowed.
- **Capitalization:** the UI-SPEC asks for `KeyboardCapitalization.Words` on names. The shared `ValidatedField` forces `capitalization = None` internally, so names do not auto-capitalize. Left unchanged to avoid editing a shared component during parallel execution; a one-line follow-up in `ValidatedField` (honor a caller-supplied capitalization) would fix it for this screen and signup.
- **Dialog wired in Task 1:** the save-failure `AuthErrorDialog` and `retrySave`/`dismissDialog` landed with the tracer (the saver's `onFailure` needs a sink); Task 2 added the 422 field mapping, City and State/Province.
- **Leading icons on profile fields:** `ValidatedField` requires a leading icon, so `ProfileTextField` takes one (Person, LocationOn, Map, Work, Business).
- **Files at iOS-mirrored folder:** all files live in `screens/settings/clientprofilesettings/` as the plan's context directs.

**Total deviations:** 1 auto-fixed (Rule 3). **Impact:** none on behavior.

## Known Stubs

None. The Task 1 and Task 3 RED stubs were replaced by their GREEN commits.

## Open items for later plans

- `ClientProfileSettingsKey` is created but not yet registered in the nav graph or opened from the Profile hub; that wiring belongs to the navigation plan (14-14). `ClientProfileSettingsScreen(viewModel, onBack)` takes a Hilt `ClientProfileSettingsViewModel`.
- If the profile cache is empty and the one refresh fails, the screen keeps its spinner (no retry affordance); the hub only opens Settings with a cached profile, so this is a corner case.
- 14-12 adds avatar, country and languages (the patch builder, snapshot fields and saver already carry `countryCode` and `languages`); 14-13 reuses `AutoSaver`, `ProfileFields.kt` and the repository.
- Manual on-device checks deferred (project policy): the sheet over the keyboard, TalkBack announcement of the status icon, 200% font scale stacking of the professional card.

## Threat Flags

None. T-14-20 (no email key in the request type, none sent), T-14-21 (Saved only after a 200, Error icon persists, `failureKeepsBaselineAndRetrySavesLatest`) and T-14-22 (1.2 s debounce, serialized saves, equal-snapshot skip) are covered by tests.

## Self-Check: PASSED

- All eleven created files exist on disk.
- Commits `fdfd6a8`, `fcd9c49`, `5f89245`, `3dd41a7`, `37ec410`, `9450042` are ancestors of HEAD (`git rev-list --count 4b40aff..HEAD` = 6 before this SUMMARY commit).
- Acceptance greps: `AUTO_SAVE_DEBOUNCE_MS = 1_200L` 1, `fun buildProfilePatch` 1, `@ApplicationScope` 1, `fun retrySave` 1, `AuthErrorDialog(` 1, `fun ProfessionalDetailsSheet` 1, `profile_settings_discard_details` 1.

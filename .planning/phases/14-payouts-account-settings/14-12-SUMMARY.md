---
phase: 14-payouts-account-settings
plan: 12
subsystem: ui
tags: [profile-settings, avatar, photo-picker, crop, languages, country, auto-save, tdd]

requires:
  - phase: 14-payouts-account-settings
    provides: "14-07 AutoSaver, ProfileSnapshot/buildProfilePatch, ClientProfileSettings screen and ViewModel; 14-02 SettingsOrigin"
provides:
  - "ProfileAvatarController and AvatarUiState (pick, crop, upload on the application scope), shared with the expert settings screen (14-13)"
  - "ProfilePhotoCropKey(origin: SettingsOrigin) for 14-14 to register with PhotoCropScreen"
  - "ProfileSettingsRepository.uploadAvatar(jpeg) and languages()"
  - "ProfileAvatarSection, ProfileLanguagesField, ProfileCountryField composables"
  - "ClientProfileSettingsScreen(viewModel, onBack, onOpenCrop) with avatar, country and spoken languages, all auto-saved"
affects: [14-13 expert settings, 14-14 navigation wiring, 14-16 device checks]

actuals:
  tokens: 31000
  tasks: 3
  commits: 6

plan_head_before: b65fd67281052f0dcf51cc3ce74a098e71090717
plan_head_after: c685a044e526bf4d98df5c8eb0f6901496afebe5

tech-stack:
  added: []
  patterns:
    - "Avatar upload runs on the injected application scope, the picker URI is read once inside PhotoProcessor.importPicked and never stored"
    - "Return-from-crop is detected on every ON_RESUME (LifecycleEventEffect) and gated by an awaitingCrop flag, so a crop upload happens exactly once"
    - "Catalogue-backed sheet state reuses LanguageSheetState/LanguageCatalog from Phase 4 with a per-ViewModel once-only fetch"

key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/clientprofilesettings/ProfileAvatarController.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/clientprofilesettings/ProfilePhotoCropKey.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/clientprofilesettings/subviews/ProfileAvatarSection.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/clientprofilesettings/subviews/ProfileLanguagesField.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/clientprofilesettings/subviews/ProfileCountryField.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/settings/clientprofilesettings/ProfileAvatarControllerTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/clientprofilesettings/ProfileSettingsRepository.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/clientprofilesettings/ClientProfileSettingsViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/clientprofilesettings/ClientProfileSettingsScreen.kt
    - app/src/main/res/values/strings_profile_settings.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/settings/clientprofilesettings/ClientProfileSettingsViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/settings/clientprofilesettings/ClientProfileSettingsScreenTest.kt

key-decisions:
  - "AvatarUiState carries two fields beyond the plan's list: errorTitleRes (import failure reuses the Phase 4 'Couldn't open photo' title; null means 'Couldn't upload avatar') and preview (the cropped bitmap shown while uploading)"
  - "A legacy alpha-2 country is normalized to alpha-3 when the ViewModel seeds the snapshot (not in ProfileSnapshot.from), so the baseline and every later location PATCH carry the alpha-3 form"
  - "The upload scrim is a sibling of the clickable avatar, not a child, so its test tag and description survive the clickable's descendant merge"
  - "Upload failure copy comes from toSaveDialogError (server message, else 'Check your connection and try again.')"
  - "ClientProfileSettingsContent takes plain callbacks for every new editor rather than a bundled actions object, matching the 14-07 style"

patterns-established:
  - "ProfileAvatarController is unscoped (one per ViewModel) but uploads on @ApplicationScope; expert settings can inject it unchanged"

requirements-completed: [PROF-02]

coverage:
  - id: D1
    description: "Tapping the avatar or Change photo launches the system photo picker (images only); a picked image is imported, the crop opens, and only a confirmed crop is uploaded to POST user/profile/avatar, then the profile is refreshed"
    requirement: PROF-02
    verification:
      - kind: unit
        ref: "ProfileAvatarControllerTest#pickCropUploadRefresh"
        status: pass
      - kind: automated_ui
        ref: "ClientProfileSettingsScreenTest#avatarSectionAndChangePhoto"
        status: pass
    human_judgment: true
    rationale: "The real PickVisualMedia launcher, the Nav3 crop push/return and the API 26-32 document-picker fallback are not exercised by Robolectric; they are device items for 14-16"
  - id: D2
    description: "Cancelling the crop uploads nothing and deletes the working files; a failed upload shows 'Couldn't upload avatar' with the server message (else the connection copy) and keeps the previous avatar; a scrim and spinner cover the avatar while uploading"
    requirement: PROF-02
    verification:
      - kind: unit
        ref: "ProfileAvatarControllerTest#cancelledCropUploadsNothing"
        status: pass
      - kind: unit
        ref: "ProfileAvatarControllerTest#uploadFailureShowsDialogKeepsAvatar"
        status: pass
      - kind: automated_ui
        ref: "ClientProfileSettingsScreenTest#uploadingShowsAProgressNodeOverTheAvatar"
        status: pass
      - kind: automated_ui
        ref: "ClientProfileSettingsScreenTest#avatarErrorShowsTheUploadTitleAndMessage"
        status: pass
    human_judgment: false
  - id: D3
    description: "An upload continues on the application scope when the user leaves the screen"
    requirement: PROF-02
    verification:
      - kind: unit
        ref: "ProfileAvatarControllerTest#uploadSurvivesLeaving"
        status: pass
    human_judgment: false
  - id: D4
    description: "Spoken Languages shows chips and Edit languages opens the Phase 4 sheet; Done saves language_ids for catalogue entries and keeps id-less custom languages in custom_languages; zero shows a hint and many wrap"
    requirement: PROF-02
    verification:
      - kind: unit
        ref: "ClientProfileSettingsViewModelTest#doneSavesIdsAndKeepsCustoms"
        status: pass
      - kind: unit
        ref: "ClientProfileSettingsViewModelTest#languagesSheetLoadsCatalogOnOpen"
        status: pass
      - kind: automated_ui
        ref: "ClientProfileSettingsScreenTest#languageChipsAndEmptyHint"
        status: pass
      - kind: automated_ui
        ref: "ClientProfileSettingsScreenTest#manyLanguagesWrapInsteadOfClipping"
        status: pass
    human_judgment: true
    rationale: "The ModalBottomSheet window (drag, scrim, IME) of the shared language sheet is not rendered by these tests; the sheet title is the Phase 4 title (UI-SPEC exception E2)"
  - id: D5
    description: "Country opens the Phase 4 country sheet; the choice is saved as an alpha-3 location.country_code via Countries.normalize and shown by name; Select shows when unset"
    requirement: PROF-02
    verification:
      - kind: unit
        ref: "ClientProfileSettingsViewModelTest#countrySelectionSavesAlpha3"
        status: pass
      - kind: automated_ui
        ref: "ClientProfileSettingsScreenTest#unsetCountryShowsSelect"
        status: pass
      - kind: automated_ui
        ref: "ClientProfileSettingsScreenTest#fieldOrderComplete"
        status: pass
    human_judgment: false
  - id: D6
    description: "Every editor goes through the same AutoSaver as the text fields (debounce, status icon, flush on Back)"
    requirement: PROF-02
    verification:
      - kind: unit
        ref: "ClientProfileSettingsViewModelTest#dismissingTheLanguageSheetSendsNothing"
        status: pass
      - kind: unit
        ref: "ClientProfileSettingsViewModelTest#legacyAlpha2CountryIsSentAsAlpha3WithOtherLocationEdits"
        status: pass
    human_judgment: false

duration: ~75 min
completed: 2026-10-29
status: complete
---

# Phase 14 Plan 12: Avatar, Languages and Country Editors Summary

**Client Profile Settings now changes the avatar through the system photo picker and the Phase 4 crop step (uploaded on the application scope), and edits spoken languages and the country in the Phase 4 sheets, all through the 14-07 auto-saver, with languages actually saved (the iOS port skips them).**

## Accomplishments

- `ProfileAvatarController` (shared with the expert screen): `onPicked` imports through `PhotoProcessor` (URI read once, never stored) and raises `openCrop`; `onCropOpened` arms `awaitingCrop`; `onReturnedFromCrop` (called on every resume) uploads a stored crop on the app scope, clears the working files, refreshes the profile best-effort, and on cancel just clears the files. Failures set an error with its own title and leave the old avatar.
- `ProfileAvatarSection`: centered 96dp avatar showing the cropped preview while uploading, 40% scrim and 32dp spinner, underlined 48dp "Change photo" button; avatar and button both pick.
- Screen: `PickVisualMedia(ImageOnly)` launcher, `LaunchedEffect(openCrop)` pushing the crop through `onOpenCrop`, `LifecycleEventEffect(ON_RESUME)`, upload/open-error `AuthErrorDialog`.
- `ProfilePhotoCropKey(origin)` created for 14-14; `ProfileSettingsRepository.uploadAvatar` (multipart `image`/`avatar.jpg`/`image/jpeg`) and `languages()`.
- Languages: catalogue fetched once per ViewModel, working selection in tap order, Done rebuilds `snapshot.languages` as selection + unchanged custom entries and auto-saves ids plus customs; `ProfileLanguagesField` shows wrapping `ExpertTagChip`s or the "Add the languages you speak" hint.
- Country: seeded and saved as alpha-3 via `Countries.normalize`, `ProfileCountryField` shows the name or "Select", reusing `CountryPickerSheet` and the same query filtering as signup.

## Task Commits

TDD RED then GREEN per task:

1. Task 1 (tracer, avatar): RED `84c843c`, GREEN `88e2c35`
2. Task 2 (languages): RED `ec53569`, GREEN `3eea0ac`
3. Task 3 (country): RED `74ee0d0`, GREEN `c685a04`

Final test run (all green): `ProfileAvatarControllerTest` 10, `ClientProfileSettingsViewModelTest` 26, `ClientProfileSettingsScreenTest` 24 (the full `*ClientProfileSettings*` and `*Countries*`/`*ProfileAutoSaveTest` filters), plus `*SignupProfilePhoto*` and `*SignupBasicInfo*` regression runs; `ktlintCheck`, `:app:assembleDevDebug` and `scripts/check-design-rules.sh` ("Design rules: OK") pass.

## TDD Gate Compliance

Each task has a `test(14-12)` commit before its `feat(14-12)` commit. RED evidence was taken from a compiling run with throwing stubs (`TODO()`) or empty composables: Task 1 failed 14 tests (all 10 controller tests plus 4 screen tests that need the avatar nodes), Task 2 failed exactly the 3 new screen tests and 7 new ViewModel tests, Task 3 failed 7 tests (all new country tests). Failures were caused by missing behavior, not compile or fixture faults. The first Task 1 RED run hit a test-compile error (two Content parameters missing in one existing test); it was fixed before the RED commit.

## Tracer gate

Task 1 was the tracer. Its `<verify>` command (controller, `*ClientProfileSettings*`, `*SignupProfilePhoto*` tests plus `ktlintCheck`) passed after the commit, so "Tracer verified end-to-end - expanding" was logged before Tasks 2 and 3.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Upload scrim not findable inside the clickable avatar**
- **Found during:** Task 1 GREEN run
- **Issue:** the scrim was a child of the clickable avatar Box, so its test tag and "Uploading photo" description were merged away and the progress node could not be located (also meant TalkBack would not read it separately).
- **Fix:** scrim is now a sibling stacked over the clickable avatar inside a common 96dp Box.
- **Files modified:** `subviews/ProfileAvatarSection.kt`
- **Commit:** `88e2c35`

### Plan-shaped adjustments (not rule-driven)

- **Extra `AvatarUiState` fields:** `errorTitleRes` (import failure must show the Phase 4 "Couldn't open photo" title, the plan's own behavior list requires it) and `preview` (the plan says the avatar shows the cropped preview while uploading).
- **Country normalization location:** done in the ViewModel seed as the plan directs, so `ProfileSnapshot.from` keeps its 14-07 contract.
- **Test-only formatting:** `ktlintFormat` was run on the module; it only touched this plan's files.

**Total deviations:** 1 auto-fixed (Rule 1). **Impact:** none on behavior.

## Known Stubs

None. The RED-stage `TODO()` bodies and empty composables were replaced in the GREEN commits.

## Open items for later plans

- `ClientProfileSettingsScreen` now requires `onOpenCrop`; 14-14 must pass a lambda that pushes `ProfilePhotoCropKey(SettingsOrigin.Profile)` and register that key with `PhotoCropScreen(onDone = back, onCancel = back)`. Until then `ClientProfileSettingsKey` is still not registered in the nav graph.
- The return-from-crop upload relies on the settings entry receiving ON_RESUME again after the crop route pops (either a recomposed entry or a lifecycle pause/resume). This could not be observed under Robolectric and is a device item for 14-16, together with: the API 26-32 document-picker fallback look, the language and country sheets over the keyboard, and TalkBack on the avatar ("Profile photo. Double tap to change.").
- The language sheet title is the Phase 4 title from `signup_basic_info_language_sheet_title` (UI-SPEC exception E2); the country field's TalkBack reading is "Select, {country}" because `PickerField` composes "{placeholder}, {value}".
- Names still do not auto-capitalize (the 14-07 `ValidatedField` follow-up is unchanged).

## Threat Flags

None. T-14-34 is covered: only a stored crop is uploaded (`cancelledCropUploadsNothing`, `returnWithoutOpeningTheCropDoesNothing`, `aPickStoresNothingUntilTheCropIsConfirmed`), cancel and failure clear working files, and the picker URI is never stored by the controller. T-14-35: language ids come only from the catalogue or the profile, countries go through `Countries.normalize`, a 422 maps to the existing dialog or field errors.

## Self-Check: PASSED

- All six created files exist on disk (`ProfileAvatarController.kt`, `ProfilePhotoCropKey.kt`, `ProfileAvatarSection.kt`, `ProfileLanguagesField.kt`, `ProfileCountryField.kt`, `ProfileAvatarControllerTest.kt`).
- Commits `84c843c`, `88e2c35`, `ec53569`, `3eea0ac`, `74ee0d0`, `c685a04` are ancestors of HEAD (`git rev-list --count b65fd67..HEAD` = 6 before this SUMMARY commit).
- Acceptance greps: `PickVisualMedia` in the screen 3, `croppedJpeg()` in the controller 1, `data class ProfilePhotoCropKey` 1, `LanguageSelectionSheet(` 1, `fun applyLanguages` 1, `Countries.normalize` 2, `CountryPickerSheet(` 1.

---
phase: 14-payouts-account-settings
plan: 13
subsystem: ui
tags: [expert-settings, auto-save, meeting-link, expertise, avatar, tdd]

requires:
  - phase: 14-payouts-account-settings
    provides: "14-07 AutoSaver/buildProfilePatch/ProfileFields, 14-12 ProfileAvatarController and editors, 14-01 updateExpertMeetingUrl, 14-02 SettingsOrigin"
provides:
  - "ExpertProfileSettingsKey(origin) for 14-14 to register in the expert shell"
  - "ExpertProfileSettingsScreen(viewModel, origin, onBack, onOpenCrop: (SettingsOrigin) -> Unit) and stateless ExpertProfileSettingsContent"
  - "ExpertProfileSettingsViewModel with ExpertSettingsSnapshot / ExpertSettingsLoad and the meeting-link guard"
  - "ExpertProfileSettingsRepository (load, updateMeetingUrl)"
affects: [14-14 navigation wiring, 14-15 Dashboard tool link, 14-16 device checks and backend question on clearing the link]

actuals:
  tokens: 17300
  tasks: 3
  commits: 6

plan_head_before: f894ed8ed729013b2f1b1860dce1bb5e42f31967
plan_head_after: 3a83cf30cd4e95fbbdabb304c657e98d22a4eafe

tech-stack:
  added: []
  patterns:
    - "One AutoSaver over a composite snapshot (shared profile + meeting link) with an ordered two-call persist"
    - "Typed-versus-saved split: the field shows what the user typed, the saver snapshot only ever holds a valid link"
    - "Sheet slot parameter on a stateless Content so tests can substitute a fake for a Hilt-backed sheet"

key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/settings/ExpertProfileSettingsKey.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/settings/ExpertProfileSettingsRepository.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/settings/ExpertProfileSettingsViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/settings/ExpertProfileSettingsScreen.kt
    - app/src/main/res/values/strings_expert_settings.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/settings/ExpertProfileSettingsViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/settings/ExpertProfileSettingsScreenTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/clientprofilesettings/ClientProfileSettingsViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/clientprofilesettings/subviews/ProfileFields.kt

key-decisions:
  - "A typed link that is blank or fails isAllowedMeetingUrl falls back to the LAST SAVED link in the saver snapshot (not the last accepted one), so a link broken after being valid is never saved"
  - "The profile PATCH goes first, then the link PATCH, in one save; the baseline moves only after both, so a failure on the second call retries both (the profile patch is idempotent)"
  - "The user-profile refresh runs only when a profile PATCH was sent; a link-only save does not change GET user/profile"
  - "Load error body is the server message, else the connection copy (toSaveDialogError with a network fallback), not the generic toSectionMessage copy, as UI-SPEC requires"
  - "A 422 naming meeting_url shows the server message under the link field"

patterns-established:
  - "ExpertSettingsActions: callbacks with no-op defaults so a Content test names only the callbacks it uses"

requirements-completed: [PROF-02]

coverage:
  - id: D1
    description: "Expert Settings loads GET expert/profile (skeleton first) and pre-fills avatar, read-only email, names, bio, job title, company, meeting link, location, languages and expertise chips"
    requirement: PROF-02
    verification:
      - kind: unit
        ref: "ExpertProfileSettingsViewModelTest#loadsAndSeedsFromExpertProfile"
        status: pass
      - kind: automated_ui
        ref: "ExpertProfileSettingsScreenTest#loadingShowsTheSkeleton"
        status: pass
      - kind: automated_ui
        ref: "ExpertProfileSettingsScreenTest#fieldOrder"
        status: pass
    human_judgment: false
  - id: D2
    description: "A valid http(s) meeting link saves 1.2 s later to PATCH expert/profile only; shared fields go to PATCH user/profile; both changed sends the profile patch then the link in one save"
    requirement: PROF-02
    verification:
      - kind: unit
        ref: "ExpertProfileSettingsViewModelTest#meetingLinkSavesToExpertProfileOnly"
        status: pass
      - kind: unit
        ref: "ExpertProfileSettingsViewModelTest#sharedFieldsGoToUserProfile"
        status: pass
      - kind: unit
        ref: "ExpertProfileSettingsViewModelTest#bothChangedSendsBoth"
        status: pass
    human_judgment: false
  - id: D3
    description: "A blank or non-http(s) link is never sent; an invalid one shows 'Enter a link that starts with https://' inline (T-14-36, T-14-37)"
    requirement: PROF-02
    verification:
      - kind: unit
        ref: "ExpertProfileSettingsViewModelTest#invalidOrBlankLinkNotSent"
        status: pass
      - kind: unit
        ref: "ExpertProfileSettingsViewModelTest#aLinkBrokenAfterBeingValidIsNotSaved"
        status: pass
      - kind: automated_ui
        ref: "ExpertProfileSettingsScreenTest#invalidLinkShowsTheInlineMessage"
        status: pass
    human_judgment: false
  - id: D4
    description: "A failed save (including a failure on the second call) opens the error dialog and Retry re-sends both calls; a failed load shows 'Couldn't load profile' with the server message else the connection copy, and Retry loads again"
    requirement: PROF-02
    verification:
      - kind: unit
        ref: "ExpertProfileSettingsViewModelTest#secondCallFailureRetriesBoth"
        status: pass
      - kind: unit
        ref: "ExpertProfileSettingsViewModelTest#failedLoadThenRetry"
        status: pass
      - kind: unit
        ref: "ExpertProfileSettingsViewModelTest#failedLoadWithoutAServerMessageUsesTheConnectionCopy"
        status: pass
      - kind: automated_ui
        ref: "ExpertProfileSettingsScreenTest#failedLoadShowsMessageAndRetry"
        status: pass
    human_judgment: false
  - id: D5
    description: "Professional details, country (saved as alpha-3) and spoken languages (ids plus kept custom languages) auto-save through the same saver as the client screen"
    requirement: PROF-02
    verification:
      - kind: unit
        ref: "ExpertProfileSettingsViewModelTest#professionalDetailsApplyAndSave"
        status: pass
      - kind: unit
        ref: "ExpertProfileSettingsViewModelTest#countrySelectionSavesAlpha3"
        status: pass
      - kind: unit
        ref: "ExpertProfileSettingsViewModelTest#languagesDoneSavesIdsAndKeepsCustoms"
        status: pass
    human_judgment: true
    rationale: "The real ModalBottomSheet windows (drag, scrim, IME) for the three sheets are not rendered by these tests; on-device behavior is a 14-16 device item"
  - id: D6
    description: "The avatar flow pushes the photo crop with this screen's own SettingsOrigin"
    requirement: PROF-02
    verification:
      - kind: automated_ui
        ref: "ExpertProfileSettingsScreenTest#avatarUsesOwnOrigin"
        status: pass
    human_judgment: true
    rationale: "The real PickVisualMedia launcher, Nav3 crop push/return and the resume-triggered upload are not exercised under Robolectric; device item for 14-16"
  - id: D7
    description: "The Expertise section shows the tags as wrapping chips; Edit expertise opens the Phase 10 sheet (fresh ViewModel per opening) and a save updates the chips without any auto-save call; no reset-onboarding affordance"
    requirement: PROF-02
    verification:
      - kind: unit
        ref: "ExpertProfileSettingsViewModelTest#expertiseSaveDoesNotAutoSave"
        status: pass
      - kind: automated_ui
        ref: "ExpertProfileSettingsScreenTest#expertiseChipsFromProfile"
        status: pass
      - kind: automated_ui
        ref: "ExpertProfileSettingsScreenTest#editExpertiseOpensSheetSlot"
        status: pass
    human_judgment: true
    rationale: "The default slot (hiltViewModel keyed per opening, real ExpertiseEditorSheet) is not composed by the tests, which substitute a fake; it follows the TopicEditorScreen pattern and is covered by assembleDevDebug only"

duration: ~70 min
completed: 2026-10-29
status: complete
---

# Phase 14 Plan 13: Expert Settings Summary

**Expert Settings loads the expert profile and auto-saves it like the client screen, adds the Skype / Zoom meeting link (only ever sent when it is a valid http(s) or meeting-app URL) and an Expertise section that reuses the Phase 10 sheet.**

## Accomplishments

- `ExpertProfileSettingsViewModel`: `init` loads `GET expert/profile` (Loading, Ready, `Failed(message)`), seeds an `ExpertSettingsSnapshot(profile: ProfileSnapshot, meetingUrl)` (country normalized to alpha-3, languages mapped to `ProfileLanguage`, link trimmed) and rebases one `AutoSaver`. `persist` sends `buildProfilePatch` through `ProfileSettingsRepository.updateProfile` when non-null, then `updateMeetingUrl` only when the link changed, is non-blank and passes `isAllowedMeetingUrl`.
- The link field shows what the user typed; the saver snapshot only takes a typed value that is valid, otherwise it keeps the last saved link. A non-blank invalid value shows "Enter a link that starts with https://" and nothing is sent; a blank value is simply never sent.
- Screen: avatar, read-only email, names, bio, professional card, meeting link (`KeyboardType.Uri`, `autoCorrectEnabled = false`, placeholder "https://"), Expertise, city, state/province, country, spoken languages, in the iOS order. Skeleton while loading (96 dp circle plus six 56 dp blocks in one "Loading" group), `EmptyState` with Retry on a failed load, the app-bar status icon, error dialog and flush on Back as on the client screen.
- Avatar, professional, languages and country functions mirror the client view model over the shared snapshot and the same `ProfileAvatarController`; the crop route is pushed with `onOpenCrop(origin)`.
- Expertise: wrapping `ExpertTagChip`s, outlined 56 dp "Edit expertise" button with the `Psychology` icon, and an `expertiseSheet` slot defaulting to `ExpertiseEditorSheet` with `hiltViewModel(key = "expertise_editor_$session")` (a `rememberSaveable` UUID, new for every opening because the slot is only composed while open, as `TopicEditorScreen` does). A save updates the chips only.

## Task Commits

TDD RED then GREEN per task:

1. Task 1 (tracer): RED `0dbc9842`, GREEN `8c0f8faf`
2. Task 2: RED `52a47ae6`, GREEN `448ce790`
3. Task 3: RED `5ba6fce6`, GREEN `3a83cf30`

Final run (all green): `ExpertProfileSettingsViewModelTest` 14 and `ExpertProfileSettingsScreenTest` 8 (22 total), together with `*ClientProfileSettings*`, `*ProfileAvatarControllerTest`, `*ExpertiseEditor*`, `*TopicEditor*`; `ktlintCheck`, `:app:assembleDevDebug` and `scripts/check-design-rules.sh` ("Design rules: OK") pass.

## TDD Gate Compliance

Each task has a `test(14-13)` commit before its `feat(14-13)` commit. RED evidence came from compiling runs against no-op stubs: Task 1 failed all 3 tests (state never left Loading, no link request, nothing rendered); Task 2 failed 7 (field order, skeleton, Retry, avatar origin, professional/country/languages saves); Task 3 failed 5 (chips, sheet slot, field order with Expertise, tags seed, no-auto-save). Failures were caused by missing behavior, not compile or fixture faults. Tests that already passed at RED (for example `sharedFieldsGoToUserProfile`, `invalidOrBlankLinkNotSent`, which exercise Task 1 logic) are kept as regression coverage for the Task 2 behavior list.

## Tracer gate

Task 1 was the tracer. Its `<verify>` command (the two target test classes plus `ktlintCheck`) passed after the GREEN code, so "Tracer verified end-to-end - expanding" was logged before Tasks 2 and 3.

## Deviations from Plan

### Auto-fixed Issues

None needed beyond test-layout fixes (the fields below the avatar are off-screen in Robolectric, so the tests use `performScrollTo`).

### Plan-shaped adjustments (not rule-driven)

- **Visibility changes in 14-07 code (no behavior change):** `fieldForServerName`, `fieldMessages`, `ProfileSnapshot.with` and `HTTP_UNPROCESSABLE` in `ClientProfileSettingsViewModel.kt` went from `private` to `internal` so the expert view model reuses the 422 field mapping and field copy. `ProfileTextField` gained an optional `placeholder` parameter (defaults to the label) for the "https://" placeholder. Both are additive and the client screen tests stay green.
- **No shared helper extracted:** the plan allowed one if duplication exceeded a few functions. The sheet, country, language and avatar functions are duplicated (about 120 lines) rather than refactoring the 14-07/14-12 view model mid-wave; a future cleanup could extract a `ProfileSheetsController`.
- **Link fallback uses the last saved link** (`savedMeetingUrl`), per the plan text, rather than the last accepted one; `aLinkBrokenAfterBeingValidIsNotSaved` pins this.
- **Profile refresh only after a profile PATCH** (the plan said "afterwards"); a link-only save does not change `GET user/profile`.
- **Load error copy:** the body is the server message else "Check your connection and try again." via `toSaveDialogError`, because `toSectionMessage` would show the generic "unknown" copy for a message-less HTTP error, contradicting UI-SPEC.
- **Extra string `expert_settings_retry` ("Retry")** because the shared `error_state_retry` reads "Retry connection".
- **Extra state:** `meetingUrlError` is a `FieldError` (so a 422 server message and the local message share one slot) and `selectedCountryCode` is a computed state property.

**Total deviations:** 0 rule-driven, plan-shaped adjustments listed above. **Impact:** none on behavior.

## Known Stubs

None. The RED-stage no-op stubs were replaced in the GREEN commits.

## Open items for later plans

- 14-14 must register `ExpertProfileSettingsKey` and the expert-origin `ProfilePhotoCropKey`, passing `ExpertProfileSettingsScreen(viewModel, origin, onBack, onOpenCrop = { origin -> push ProfilePhotoCropKey(origin) })`; 14-15 links the Dashboard tool to the key.
- Device checks for 14-16: the photo picker and crop return upload on this screen, the three sheets over the keyboard, the real `ExpertiseEditorSheet` opened from this screen (fresh view model per opening), the Uri keyboard, TalkBack on the status icon.
- Backend question carried to 14-16 (UI-SPEC "Meeting link cannot be cleared", RESEARCH A7): a blank link is never sent, so an expert cannot clear it.
- The 14-07 follow-up (names do not auto-capitalize because `ValidatedField` forces no capitalization) applies to this screen too.

## Threat Flags

None. T-14-36 (`isAllowedMeetingUrl` before any save, invalid values never enter the saver snapshot) and T-14-37 (blank never sent) are covered by `invalidOrBlankLinkNotSent`, `aLinkBrokenAfterBeingValidIsNotSaved` and `meetingLinkSavesToExpertProfileOnly`. No new endpoints: `GET expert/profile`, `PATCH user/profile` and `PATCH expert/profile` already existed from 14-01.

## Self-Check: PASSED

- All seven created files exist on disk (key, repository, view model, screen, strings, two tests).
- Commits `0dbc9842`, `8c0f8faf`, `52a47ae6`, `448ce790`, `5ba6fce6`, `3a83cf30` are ancestors of HEAD (`git rev-list --count f894ed8e..HEAD` = 6 before this SUMMARY commit).
- Acceptance greps: `isAllowedMeetingUrl(` in the view model 2, `data class ExpertProfileSettingsKey` 1, `expert_settings_meeting_invalid` in the view model 1, `PickVisualMedia` in the screen 3, `ExpertiseEditorSheet(` in the screen 1.

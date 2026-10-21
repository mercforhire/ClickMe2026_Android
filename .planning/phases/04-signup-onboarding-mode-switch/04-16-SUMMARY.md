---
phase: 04-signup-onboarding-mode-switch
plan: 16
subsystem: signup-profile-photo
tags: [compose, photo-picker, multipart, avatar, robolectric]
requires: [04-03, 04-05, 04-07, 04-09]
provides:
  - SignupProfilePhotoViewModel, SignupProfilePhotoUiState
  - SignupProfilePhotoScreen (Hilt wrapper) and stateless SignupProfilePhotoContent
  - strings_signup_profile_photo.xml
affects: [04-19]
tech-stack:
  added: []
  patterns:
    - PickVisualMedia(ImageOnly) launcher feeding a ViewModel that imports into the app-private working copy
    - onShown() re-reads the PhotoProcessor on every entry so the crop result appears on return
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/profilephoto/SignupProfilePhotoViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/profilephoto/SignupProfilePhotoScreen.kt
    - app/src/main/res/values/strings_signup_profile_photo.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/signup/profilephoto/SignupProfilePhotoViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/signup/profilephoto/SignupProfilePhotoScreenTest.kt
  modified: []
decisions:
  - "Remove with both a saved avatar and a local unsaved pick clears only the local pick (no dialog); a second Remove then asks to delete the saved avatar"
  - "An in-flight DELETE is tracked by a private flag rather than saveState, so the Save button does not show a spinner during remove; Save and a second confirm are ignored while it runs"
  - "Save uploads whenever the processor has cropped JPEG bytes (not only when the preview decoded)"
metrics:
  completed: 2026-10-21
  tasks: 2
  files: 5
requirements-completed: [SIGN-02]
---

# Phase 4 Plan 16: Profile Photo Step Summary

Profile Photo signup step: system photo picker (no permission) feeds the 04-05 pipeline, opens the 04-09 crop route, shows the cropped preview, and Save uploads the JPEG as multipart `image`, holds Saved for 600 ms and returns; Remove handles both unsaved picks (local clear) and saved avatars (confirm then DELETE).

## Commits
- b614063: feat(04-16): SignupProfilePhotoViewModel (13 tests)
- 788b430: feat(04-16): SignupProfilePhotoScreen with photo picker (6 tests)

## Verification
`:app:testDevDebugUnitTest --tests '*SignupProfilePhoto*' ktlintCheck lintDevDebug` exits 0; `scripts/check-design-rules.sh` prints "Design rules: OK". ViewModel 13/13, Screen 6/6. No SavedStateHandle in the ViewModel, no AndroidManifest change, `size = 160.dp` appears once.

## Deviations from Plan
- Screen tests use `@Config(qualifiers = "w400dp-h900dp")` (as OnboardingScreenTest does); the default Robolectric viewport clipped the buttons below the scrolling header so `assertIsDisplayed`/clicks failed (test fix, not a code change).
- `TextButtonDefaults` does not exist in this Material3 version; `ButtonDefaults.textButtonColors` is used for the destructive Remove Photo button.
- Tests and implementation per task were committed together (no separate RED commits).
- Back is disabled (and system Back swallowed) while Saving/Saved, matching SetNewPasswordScreen, so the Saved hold is not cancelled.

## Known Stubs
None. `onOpenCrop` is a parameter; plan 04-19 wires it to the crop route key and registers this screen.

## Threat Flags
None. T-04-41 to T-04-44 mitigated as planned (picker URI only passed to `importPicked`; single in-flight upload via the Saving guard; repository builds the `image/jpeg` part; `clearWorkingFiles` after upload and after clearing an unsaved pick).

## Deferred / Manual (per project policy, non-blocking)
- On device: pick a large camera photo, crop, confirm the preview and Save upload; Choose Another Photo; Adjust Crop reopens on the original; Remove saved avatar (confirm) and Remove an unsaved pick (no dialog, no server call); upload failure on airplane mode keeps the crop for retry.
- The system photo picker and nav wiring to `PhotoCropKey` are only exercisable once plan 04-19 registers the routes.

## Self-Check: PASSED
All 5 files exist; commits b614063 and 788b430 present.

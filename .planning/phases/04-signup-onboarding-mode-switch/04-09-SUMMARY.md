---
phase: 04-signup-onboarding-mode-switch
plan: 09
subsystem: signup-profile-photo
tags: [compose, gestures, canvas, crop, accessibility, robolectric]
requires: [04-03, 04-05]
provides:
  - PhotoCropViewModel, PhotoCropUiState
  - PhotoCropScreen (Hilt wrapper) and stateless PhotoCropContent, CROP_CANVAS_TAG
  - strings_signup_photo_crop.xml (incl. signup_photo_open_error_title/body, reused by 04-16)
affects: [04-16, 04-19]
tech-stack:
  added: []
  patterns:
    - Normalised saved state (zoom relative to min scale, offset relative to guide radius) so rotation keeps the framing
    - awaitEachGesture pinch/pan host with an end callback; animate offset/scale only when not dragging
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/profilephoto/PhotoCropViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/profilephoto/PhotoCropScreen.kt
    - app/src/main/res/values/strings_signup_photo_crop.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/signup/profilephoto/PhotoCropViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/signup/profilephoto/PhotoCropScreenTest.kt
  modified: []
decisions:
  - "Scale is animated together with offset (same 200 ms spec, snap while dragging) so double-tap and zoom steps do not jump"
  - "Load-failure dialog dismiss routes to onCancel inside PhotoCropContent (not only the wrapper) so the stateless content is directly testable"
metrics:
  completed: 2026-10-21
  tasks: 2
  files: 5
requirements-completed: [SIGN-02]
---

# Phase 4 Plan 09: Photo Crop Screen Summary

Full-screen crop route: a pinch/pan/double-tap canvas under a fixed circular guide (60% scrim cutout plus 2 dp ring), 200 ms spring-back, six TalkBack custom actions, and Use Photo / Cancel, backed by a saved-state-aware `PhotoCropViewModel` built on the tested `CropMath` and `PhotoProcessor` from 04-05.

## Commits
- 837b835: feat(04-09): PhotoCropViewModel with saved-state transform and crop (10 tests)
- 808c851: feat(04-09): PhotoCropScreen with guide canvas, gestures and a11y actions (7 tests)

## Verification
`:app:testDevDebugUnitTest --tests '*PhotoCrop*' ktlintCheck lintDevDebug` exits 0; `scripts/check-design-rules.sh` prints "Design rules: OK". PhotoCropViewModelTest 10/10, PhotoCropScreenTest 7/7.

## Notes
- The ViewModel handles either ordering of "image loaded" and "guide measured" (the transform initialises once both are known) and rebuilds the transform from saved state on every guide resize, which also covers rotation with the ViewModel retained.
- Robolectric tests that create `ImageBitmap` need `@GraphicsMode(GraphicsMode.Mode.NATIVE)` (as in PhotoProcessorTest and AvatarTest).
- Compose `CustomAccessibilityAction` lambdas must return Boolean; each of the six returns true after calling the step callback.
- The ViewModel never references a picker URI; it only reads the working copy through `PhotoProcessor`.

## Deviations from Plan
None of substance. Two interpretation notes:
- Acceptance greps `awaitEachGesture` and `detectTapGestures` print 2 each (import line plus the single use); there is exactly one use of each.
- Dialog dismiss selection (`loadFailed` ? onCancel : onDismissDialog) lives in `PhotoCropContent` rather than the Hilt wrapper; behaviour is identical and the stateless content is testable.

## Known Stubs
None.

## Threat Flags
None. T-04-24 mitigated (bounded working copy; load failure shows "Couldn't open photo" instead of crashing); T-04-25 accepted (only zoom factor and normalised offsets are saved).

## Deferred / Manual (per project policy, non-blocking)
- Device check of crop feel on the Samsung test device: pinch about the finger, drag, release spring-back, double-tap toggle, rotation mid-crop keeps framing, TalkBack custom actions, large camera photo.
- Plan 04-19 must register the crop route key; plan 04-16 pushes it and reuses `signup_photo_open_error_*` strings.

## Self-Check: PASSED
All 5 files exist; commits 837b835 and 808c851 present in git log.

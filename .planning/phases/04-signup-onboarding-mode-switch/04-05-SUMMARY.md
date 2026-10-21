---
phase: 04-signup-onboarding-mode-switch
plan: 05
subsystem: signup-profile-photo
tags: [crop, bitmap, exif, jpeg, pure-math, robolectric, hilt]
requires: []
provides:
  - CropMath, CropRect, CropTransform (pure crop geometry)
  - PhotoProcessor interface, PhotoImportException, PhotoMath
  - AndroidPhotoProcessor (Hilt-bound via di.PhotoModule)
  - testing.FakePhotoProcessor
affects: [04-07, 04-09, 04-16]
tech-stack:
  added: []
  patterns:
    - Pure geometry object testable on the JVM
    - App-private working copy in cacheDir/signup; picker URI read once
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/profilephoto/CropMath.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/profilephoto/PhotoProcessor.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/profilephoto/AndroidPhotoProcessor.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/di/PhotoModule.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/signup/profilephoto/CropMathTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/signup/profilephoto/PhotoProcessorTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/testing/FakePhotoProcessor.kt
  modified: []
decisions:
  - "Crop rect clamping shifts and caps the square inside the bitmap instead of clipping, so it stays square"
  - "Platform android.media.ExifInterface used; no new dependency"
metrics:
  completed: 2026-10-21
  tasks: 2
  files: 7
requirements-completed: [SIGN-02]
---

# Phase 4 Plan 05: Crop Math and Photo Pipeline Summary

Pure, tested crop geometry (`CropMath`) and an on-device photo pipeline (`AndroidPhotoProcessor`: bounded decode, EXIF rotate, app-private working copy, 1024 px q85 square JPEG crop, cleanup) with a Hilt binding and a scriptable fake, with no new dependency.

## Commits
- 8552424: feat(04-05): CropMath pure crop geometry with tests (14 tests)
- Task 2 commit: feat(04-05): PhotoProcessor pipeline, Hilt binding and fake (11 tests)

## Verification
`:app:testDevDebugUnitTest --tests '*PhotoProcessorTest' --tests '*CropMathTest' ktlintCheck lintDevDebug` exits 0. CropMathTest 14/14, PhotoProcessorTest 11/11. No `androidx.exifinterface` in Gradle files.

## Notes
- The EXIF rotation test works under Robolectric (a 300 x 200 source with orientation 6 comes back 200 x 300), so it is included in addition to the `PhotoMath.rotationDegrees` tests.
- The plan's example crop `CropRect(500, 0, 1365)` is on the downsampled working copy, so the crop test imports a 4000 x 3000 source (sampled to 2000 x 1500) so the rect is valid.
- TDD: tests and implementation were committed together per task (no separate RED commit); tests were written first and pass.

## Deviations from Plan
**[Rule 1 - Bug] Crop rect clamping.** The initial clamp clipped each edge independently, which could turn an out-of-bounds rect into a non-square region drawn into a square output (distortion). Replaced with cap-side-then-shift so the source rect is always square. Included in the Task 2 commit.

## Known Stubs
None.

## Threat Flags
None. T-04-10 to T-04-13 mitigated as planned: bounds-first decode with a capped sample size and exceptions mapped to `PhotoImportException`, re-encode drops EXIF/GPS, picker URI never stored, working files in `cacheDir/signup` with `clearWorkingFiles()`.

## Deferred / Manual
Device check of crop feel and real-photo EXIF handling is deferred per project policy (no human checkpoint in this plan; belongs to plans 04-09 / 04-16).

## Self-Check: PASSED
All 7 files exist; both task commits present in git log.

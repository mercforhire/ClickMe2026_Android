---
phase: 04-signup-onboarding-mode-switch
fixed_at: 2026-10-21T19:50:21Z
review_path: .planning/phases/04-signup-onboarding-mode-switch/04-REVIEW.md
iteration: 1
findings_in_scope: 11
fixed: 10
skipped: 1
status: partial
---

# Phase 4: Code Review Fix Report

**Fixed at:** 2026-10-21T19:50:21Z
**Source review:** .planning/phases/04-signup-onboarding-mode-switch/04-REVIEW.md
**Iteration:** 1

**Summary:**
- Findings in scope: 11 (CR-01..CR-03, WR-01..WR-08; the Info items are out of scope)
- Fixed: 10
- Skipped: 1 (CR-01. Its decision, D-27, conflicts with the backend role model and needs the user to decide again. See below.)

**Verification:** `./gradlew testDevDebugUnitTest ktlintCheck lintDevDebug assembleDevDebug` passes, and so does `bash scripts/check-design-rules.sh`. There are 861 unit tests (baseline 809, plus 52 new) with 0 failures.

**Process note:** The orchestrator said to work on the main checkout, so no isolated worktree or recovery sentinel was used. Every commit staged explicit paths only. The user's `.idea/` changes and the untracked `.planning/config.json` / `.idea/appInsightsSettings.xml` were not touched.

## Fixed Issues

### CR-02: The D-06 publish-blocked marker survives the fix, so Publish stays disabled

**Files modified:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/SignupSetupRepository.kt`, `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/SignupDraft.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/signup/SignupSetupRepositoryTest.kt`
**Commit:** 89a6648
**Applied fix:** When a step saves successfully, only that step's block is lifted. This covers Basic Info → PersonalDetails, Timezone, Tags → Expertise, avatar upload → ProfilePhoto, and email verify → VerifyEmail. I did not clear the whole set, so a step the server still rejects stays marked until it is fixed or re-hydrated. Tests cover the upload-lifts-photo-only case, each step lifting its own block until Publish is enabled again, and a failed save keeping the block.

### CR-03: Photo working files leak across sessions and accounts

**Files modified:** `app/src/main/java/com/uptrendinvestments/clickmeapp/managers/SessionDataCleaner.kt` (new), `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/profilephoto/PhotoWorkingFilesCleaner.kt` (new), `app/src/main/java/com/uptrendinvestments/clickmeapp/di/PhotoModule.kt`, `app/src/main/java/com/uptrendinvestments/clickmeapp/managers/UserManager.kt`, `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/SignupSetupRepository.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/managers/UserManagerTest.kt`
**Commit:** 696499d
**Applied fix:**
- Added a small `SessionDataCleaner` interface, injected into `UserManager` as a Hilt `@IntoSet` set. The set defaults to empty, so existing test constructors are unchanged.
- `UserManager` runs every cleaner, best effort, at three points:
  - inside the non-cancellable `logout()`, so it also covers the restore-time rejection path;
  - before `login()`;
  - before `signup()`.
- `PhotoWorkingFilesCleaner` registers `photoProcessor.clearWorkingFiles()`. Cleanup therefore no longer depends on the lazily created `SignupSetupRepository` existing in the process.

The new files were required by the fix. Tests cover:
- logout clearing after the tokens are gone;
- a restore-time rejection clearing;
- login and signup clearing before tokens are stored;
- a real `PhotoWorkingFilesCleaner` deleting the files with no repository in the process;
- a failing cleaner not breaking logout or login.

### WR-01: Mode writes are not session-guarded

**Files modified:** `app/src/main/java/com/uptrendinvestments/clickmeapp/managers/UserManager.kt`, `app/src/main/java/com/uptrendinvestments/clickmeapp/AppViewModel.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/managers/UserManagerTest.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/AppViewModelTest.kt`
**Commit:** f4511f0
**Applied fix:**
- A private `setModeForSession(mode, generation)` writes the mode only if two things hold: the session generation captured when the operation started is still current, and tokens still exist. A `Mutex` orders it against logout's generation bump, so a write that wins the lock finishes before `sessionStore.clear()`.
- `requestModeSwitch`, `resolvePostAuthDestination` and `completeSetup` use it. The restore path captures its generation at the very start of `restoreInternal`. When the session ended, they return `LaunchDestination.Login`.
- `AppViewModel.switchMode` and `onSetupPublished` apply their result only while `isLoggedIn` is true.

Tests cover a switch lookup finishing after logout (complete and incomplete), a late restore after logout, a resolve with no session, `completeSetup` after logout, and publish after a rejection.
**Status:** fixed: requires human verification (concurrency and state handling)

### WR-02: A stale hydrate can overwrite a newer step save

**Files modified:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/SignupSetupRepository.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/signup/SignupSetupRepositoryTest.kt`
**Commit:** a3a7a03
**Applied fix:**
- Every successful step write goes through `applyWrite`. Under the repository lock, it checks the generation, bumps a `writes` counter and updates the draft.
- `runHydrate` records `writes` before each fetch. If the counter moved, it drops that snapshot and fetches again, because the newer snapshot includes the save. After 3 attempts it keeps the draft and reports `Failed`, so the hub offers a retry.
- The generation check, the merge and the hydration-state write now run together under the lock. This also closes the first race in IN-08.

Tests cover a timezone save and an avatar upload landing mid-hydrate, and a save racing every attempt.
**Status:** fixed: requires human verification (concurrency and state handling)

### WR-03: The pre-selected English overwrites the server's languages

**Files modified:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/SignupSetupRepository.kt`, `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/basicinfo/SignupBasicInfoViewModel.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/signup/SignupSetupRepositoryTest.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/signup/basicinfo/SignupBasicInfoViewModelTest.kt`
**Commit:** 8037c60
**Applied fix:**
- Basic Info tracks language edits (`basic_languages_touched`, kept in saved state) separately from text edits. The languages keep re-seeding from the draft until the user changes them.
- English is pre-selected only when hydration is `Loaded` and the server has no languages. While hydration is Idle, Loading or Failed, the draft is shown as is.
- Save sends `languages` only if the user changed them, or if the English default was shown. `saveBasicInfo` now takes `languages: List<LanguageItem>?`, where null omits the field.

Existing tests that saved an empty form with Idle hydration now hydrate an empty profile first, so their intent is unchanged. New tests cover:
- a failed hydrate;
- a text edit before a late hydrate;
- untouched hydrated languages;
- changed languages in tap order;
- language edits surviving a hydrate;
- text-only saved state.

**Status:** fixed: requires human verification (behaviour change: on a fresh signup, English now appears only after the hub's hydrate succeeds)

### WR-04: Users can never clear the optional province/state once it is saved

**Files modified:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/SignupSetupRepository.kt`, `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/basicinfo/SignupBasicInfoViewModel.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/signup/SignupSetupRepositoryTest.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/signup/basicinfo/SignupBasicInfoViewModelTest.kt`
**Commits:** a724a2b, plus ktlint formatting in 2718a96
**Applied fix:**
- `saveBasicInfo` takes `clearCity` / `clearProvince`. When the user emptied the field and the draft still has a value, it sends `""` (the server schema accepts it) and updates the draft.
- The ViewModel sets these flags only for fields the user actually typed in (`basic_city_edited` / `basic_province_edited` in saved state). Otherwise a blank field that was never seeded, such as one left empty when a late hydrate arrived, would wipe the server value.
- I applied the fix to city as well, because the review title says "or any text field". First name is left out: the server requires `min(1)`, and it is a required field anyway.

**Status:** fixed: requires human verification (logic)

### WR-05: System Back during Signup Initial submit can cancel the signup after the account exists

**Files modified:** `app/src/main/java/com/uptrendinvestments/clickmeapp/managers/UserManager.kt`, `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/initial/SignupInitialScreen.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/managers/UserManagerTest.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/signup/initial/SignupInitialScreenTest.kt`
**Commit:** 69b6951
**Applied fix:**
- Added `BackHandler(enabled = state.isSubmitting) {}` to `SignupInitialContent`.
- `login()` and `signup()` now share a non-cancellable `startSession()` that saves the tokens and sets `isLoggedIn` and the related flags once the server has answered.

Tests cover system Back while idle and while submitting, and signup and login whose caller is cancelled after the response.

### WR-06: Basic Info and Timezone allow Back mid-save

**Files modified:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/basicinfo/SignupBasicInfoScreen.kt`, `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/timezone/SignupTimezoneScreen.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/signup/basicinfo/SignupBasicInfoScreenTest.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/signup/timezone/SignupTimezoneScreenTest.kt`
**Commits:** 05957c7, plus ktlint import ordering in 2718a96
**Applied fix:** Both steps now mirror Tags with `BackHandler(enabled = !editable) {}` and `AuthTopAppBar(backEnabled = editable)`, where `editable` means `saveState == Idle`. Tests cover system Back while idle, while saving and during the Saved hold, and the disabled arrow.

### WR-07: Custom (id-less) languages are silently deleted by Save and Publish

**Files modified:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/SignupDraft.kt`, `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/SignupSetupRepository.kt`, `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/basicinfo/SignupBasicInfoViewModel.kt`, `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/review/SignupReviewViewModel.kt`, plus tests in `SignupSetupRepositoryTest.kt`, `SignupDraftTest.kt`, `SignupReviewViewModelTest.kt` and `SignupBasicInfoViewModelTest.kt`
**Commits:** 23e2a64, plus ktlint formatting in 2718a96
**Applied fix:**
- `SignupDraft.customLanguages` keeps the labels of id-less server languages from hydrate.
- Custom languages:
  - count toward Personal Details;
  - show on Review;
  - suppress the English default.
- Publish omits `languages` while custom languages exist. This is safe because any Basic Info save that sends languages has already replaced the server list, and it clears `customLanguages` in the draft.
- With WR-03, an untouched Basic Info save no longer sends languages either.

**Remaining limitation:** If the user does edit languages in Basic Info, custom entries are still replaced. `PATCH /expert/profile/setup` accepts catalogue ids only. No warning is shown.
**Status:** fixed: requires human verification (logic)

### WR-08: FakePhotoProcessor diverges from the real processor

**Files modified:** `app/src/test/java/com/uptrendinvestments/clickmeapp/testing/FakePhotoProcessor.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/testing/FakePhotoProcessorTest.kt` (new), `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/signup/profilephoto/SignupProfilePhotoViewModelTest.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/signup/profilephoto/PhotoCropViewModelTest.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/managers/UserManagerTest.kt`
**Commit:** 3cc0126
**Applied fix:** The fake now models the two working files (`hasSourceFile`, `storedCrop`) the way `AndroidPhotoProcessor` does:
- import deletes the crop;
- crop needs a source and throws `PhotoImportException` otherwise;
- `croppedPreview()` and `croppedJpeg()` are non-null exactly when a crop is stored. The preview falls back to a lazily created 1x1 bitmap.
- `clearWorkingFiles()` deletes both files and no longer resets the scripted `importError`/`cropError`.

Test configuration (`sourceBitmap`, `sourcePixelSize`, `previewBitmap`, `cropBytes`) is kept separate, with `givenSource()`/`givenCrop()` setup helpers. I added a contract test for the fake and three photo-step tests that depend on the corrected behaviour: a new pick drops the old preview, a removed pick stays gone, and a saved crop is not offered again.

## Skipped Issues

### CR-01: Switching a client-only user to Expert traps them in the setup hub permanently

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/managers/UserManager.kt:255-257, 326-332` (plus `SignupOverviewScreen.kt`, `SignupSetupNavHost.kt`, `AppViewModel.kt`)
**Reason:** I could not apply D-27 as written. It relies on a role distinction the backend does not make.
- D-27 forces setup only for "pure-expert accounts: the expert role without the client role", and shows the hub exit whenever the user holds the client role.
- The backend never creates an expert without the client role:
  - `backend-reference/src/controllers/auth.controller.js:562` assigns `const roles = role === 'expert' ? ['client', 'expert'] : ['client'];`. The Android signup always sends `role: "expert"`, so every signup account has `['client','expert']`.
  - `backend-reference/src/middleware/ensureExpert.js` (step b/c) converts a client-only account to exactly the same `['client','expert']`, and sets the scalar `role` to `'expert'` in both cases.
- So a converted client and a fresh expert signup cannot be told apart by roles. Under D-27 as written, no real account is "pure expert". The forced resume into the hub at launch and login (SIGN-05, D-10), and the D-11 foreground re-check, would never fire again. The new exit would also appear on every unfinished signup's hub.
- Our test fixtures use `roles = ["expert"]`. That is why this conflict does not show up in the unit tests.

I made no changes for CR-01, so the trap described in the review still exists.

**Options for the user (pick one, then re-run the fixer):**
1. **The exit for everyone, with resume kept.** Every unfinished setup hub gets a "Not now" / close action that goes to client mode through an `AppViewModel` method. Launch and login still open the hub while setup is incomplete, and the user can leave every time. This needs no role test. It changes UX for real expert signups, who also get "Not now".
2. **A different signal for "converted client".** For example, a persisted local flag "setup entered by a mode switch". It is set in `requestModeSwitch` and lost on logout or on another device. Another option is a backend field such as a signup-origin or converted-at marker on `/me` or `/expert/profile`. Then force setup only when that flag is absent.
3. **D-27 exactly as written.** Accept that, with this backend, no account is forced into setup at launch or login. SIGN-05's "an unfinished signup resumes into the hub at splash or login" then no longer holds.

**Original issue:** `requestModeSwitch(Expert)` converts a client-only account (D-23) and routes to a hub that has no exit. Every later launch and login then routes back to the hub, so the user can never use client mode again until they publish an expert profile.

---

_Fixed: 2026-10-21T19:50:21Z_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 1_

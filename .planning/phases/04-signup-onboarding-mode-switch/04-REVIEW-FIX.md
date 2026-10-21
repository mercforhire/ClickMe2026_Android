---
phase: 04-signup-onboarding-mode-switch
fixed_at: 2026-10-21T20:03:22Z
review_path: .planning/phases/04-signup-onboarding-mode-switch/04-REVIEW.md
iteration: 1
findings_in_scope: 11
fixed: 11
skipped: 0
status: all_fixed
---

# Phase 4: Code Review Fix Report

**Fixed at:** 2026-10-21T20:03:22Z
**Source review:** .planning/phases/04-signup-onboarding-mode-switch/04-REVIEW.md
**Iteration:** 1

**Summary:**
- Findings in scope: 11 (CR-01..CR-03, WR-01..WR-08; the Info items are out of scope)
- Fixed: 11
- Skipped: 0. CR-01 was first skipped because the original D-27 keyed on a role split the backend does not make. It was fixed after the user revised D-27 to "exit + remember" (ff814e9).

**Verification:** `./gradlew testDevDebugUnitTest ktlintCheck lintDevDebug assembleDevDebug` passes, and so does `bash scripts/check-design-rules.sh`. There are 876 unit tests (baseline 809, plus 67 new) with 0 failures.

**Process note:** The orchestrator said to work on the main checkout, so no isolated worktree or recovery sentinel was used. Every commit staged explicit paths only. The user's `.idea/` changes and the untracked `.planning/config.json` / `.idea/appInsightsSettings.xml` were not touched.

## Fixed Issues

### CR-01: Switching a client-only user to Expert traps them in the setup hub permanently

**Files modified:** `app/src/main/java/com/uptrendinvestments/clickmeapp/managers/SessionStore.kt`, `app/src/main/java/com/uptrendinvestments/clickmeapp/managers/UserManager.kt`, `app/src/main/java/com/uptrendinvestments/clickmeapp/AppViewModel.kt`, `app/src/main/java/com/uptrendinvestments/clickmeapp/ClickMeApp.kt`, `app/src/main/java/com/uptrendinvestments/clickmeapp/navigation/SignupSetupNavHost.kt`, `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/overview/SignupOverviewScreen.kt`, `app/src/main/res/values/strings_signup_overview.xml`, `app/src/test/java/com/uptrendinvestments/clickmeapp/managers/SessionStoreTest.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/managers/UserManagerTest.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/AppViewModelTest.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/signup/overview/SignupOverviewScreenTest.kt`
**Commit:** 3d3a8af
**Applied fix (revised D-27, "exit + remember"):**
- **UI choice:** "Not now" is a `TextButton` action in the hub's `TopAppBar`, before the "More options" overflow. The string is `signup_overview_not_now` in `strings_signup_overview.xml`. I chose a low-emphasis app-bar text action over a second bottom button for two reasons:
  - "Next" stays the hub's single primary CTA (UI-SPEC color rule 1).
  - The hub stays a stack root with no navigation icon (UI-SPEC navigation table).
- **Exit path:** `SignupOverviewScreen(onNotNow)` → `SignupSetupNavHost(onNotNow)` → `AppViewModel.onSetupDeferred()`. That calls `UserManager.deferSetup()`, which:
  - sets mode Client through the session-guarded `setModeForSession` (WR-01);
  - persists `SessionStore.setSetupDeferred(true)` and mirrors it in memory;
  - clears `setupCheckPending`.

  The route then becomes ClientHome, but only while the user is still logged in.
- **While the flag is set:**
  - `resolvePostAuthDestination` (launch restore and login) skips the setup lookup entirely, with no `GET /expert/profile`;
  - `recheckSetupIfPending` (the D-11 foreground re-check) clears the pending flag with no request.

  The flag is loaded with the other persisted state in `loadPersistedState` (restore and the cap path).
- **What clears the flag:**
  - `requestModeSwitch(Expert)`, which still opens setup when setup is incomplete (D-17);
  - `completeSetup()` (publish);
  - `logout()`, whose `sessionStore.clear()` wipes the key and resets the in-memory value. The next login therefore resumes into setup (SIGN-05).

**Tests:** these use the backend's `['client','expert']` roles.
- `AppViewModelTest`:
  - "Not now" routes to ClientHome and persists the flag;
  - relaunch while deferred goes to ClientHome with no setup lookup;
  - the foreground re-check is skipped while deferred, after a cached-path relaunch;
  - switching to Expert reopens setup, clears the flag, and setup is forced again on relaunch;
  - logout then login resumes into SignupSetup;
  - "Not now" after the session ended stays on Login.
- `UserManagerTest`: the same cases at manager level, plus publish clearing the flag and `deferSetup` writing nothing after logout.
- `SessionStoreTest`: the flag round-trips and is removed by `clear()`.
- `SignupOverviewScreenTest`: the "Not now" action calls the exit.

**Status:** fixed: requires human verification (routing and state logic; a device check of the app-bar action's fit at 200% font scale is also worth doing)

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

---

_Fixed: 2026-10-21T20:03:22Z_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 1_

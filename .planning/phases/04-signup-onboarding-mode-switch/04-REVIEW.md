---
phase: 04-signup-onboarding-mode-switch
reviewed: 2026-10-21T17:03:03Z
depth: standard
files_reviewed: 88
files_reviewed_list:
  - app/src/main/java/com/uptrendinvestments/clickmeapp/AppViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/ClickMeApp.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/components/PickerField.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/components/PickerSheet.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/components/SelectableChip.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/components/avatar/Avatar.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/constants/Countries.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/constants/Timezones.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/di/ClockModule.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/di/PhotoModule.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/MonotonicClock.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/UserManager.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/requests/SignupRequests.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/ApiErrorEnvelope.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/AvatarUploadData.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/EmailStatusData.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/ExpertProfileData.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/MetaPayloads.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/SetupExpertProfileData.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/SignupData.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/navigation/AuthKeys.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/navigation/AuthNavHost.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/navigation/AuthNavigator.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/navigation/SignupSetupKeys.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/navigation/SignupSetupNavHost.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/navigation/SignupSetupNavigator.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/network/ApiErrorParser.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/network/ApiException.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/network/ApiRequestUrls.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/network/ClickMeApi.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/home/HomeClientScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/home/HomeClientViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/home/HomeExpertScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/AuthErrorDialog.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/AuthTopAppBar.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/AuthValidators.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/onboarding/OnboardingPages.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/onboarding/OnboardingScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/modeswitching/ModeSwitchScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/modeswitching/subviews/ModeSwitchCard.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/SetupStep.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/SignupDraft.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/SignupSetupHostViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/SignupSetupRepository.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/basicinfo/SignupBasicInfoScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/basicinfo/SignupBasicInfoViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/basicinfo/languageselection/LanguageSelectionSheet.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/basicinfo/subviews/CountryPickerSheet.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/initial/SignupInitialScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/initial/SignupInitialViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/overview/SignupOverviewScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/overview/SignupOverviewViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/profilephoto/AndroidPhotoProcessor.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/profilephoto/CropMath.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/profilephoto/PhotoCropScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/profilephoto/PhotoCropViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/profilephoto/PhotoProcessor.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/profilephoto/SignupProfilePhotoScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/profilephoto/SignupProfilePhotoViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/review/SignupReviewScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/review/SignupReviewViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/tags/SignupTagsScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/tags/SignupTagsViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/timezone/SignupTimezoneScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/timezone/SignupTimezoneViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/verifyemail/SignupVerifyEmailScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/verifyemail/SignupVerifyEmailViewModel.kt
  - app/src/main/res/values/strings.xml
  - app/src/main/res/values/strings_auth.xml
  - app/src/main/res/values/strings_auth_screens.xml
  - app/src/main/res/values/strings_mode_switch.xml
  - app/src/main/res/values/strings_onboarding.xml
  - app/src/main/res/values/strings_pickers.xml
  - app/src/main/res/values/strings_session.xml
  - app/src/main/res/values/strings_signup_basic_info.xml
  - app/src/main/res/values/strings_signup_common.xml
  - app/src/main/res/values/strings_signup_initial.xml
  - app/src/main/res/values/strings_signup_overview.xml
  - app/src/main/res/values/strings_signup_photo_crop.xml
  - app/src/main/res/values/strings_signup_profile_photo.xml
  - app/src/main/res/values/strings_signup_review.xml
  - app/src/main/res/values/strings_signup_tags.xml
  - app/src/main/res/values/strings_signup_timezone.xml
  - app/src/main/res/values/strings_signup_verify_email.xml
  - app/src/test/java/com/uptrendinvestments/clickmeapp/testing/FakeClickMeApi.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/testing/FakePhotoProcessor.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/testing/SignupTestHarness.kt
findings:
  critical: 3
  warning: 8
  info: 9
  total: 20
status: issues_found
---

# Phase 4: Code Review Report

**Reviewed:** 2026-10-21T17:03:03Z
**Depth:** standard
**Files Reviewed:** 88 (68 Kotlin main sources, 17 string files, 3 test fakes; the other 43 changed test files were only checked for test-count vs assertion-count, and none had fewer assertions than tests)
**Status:** issues_found

## Summary

I reviewed the Phase 4 signup, setup hub, photo pipeline, onboarding carousel and mode switch against 04-CONTEXT.md (D-01..D-26) and the backend `onboarding.controller.js` / `onboarding.schema.js` contracts.

The networking DTOs, the redacted `toString()`s, the password handling (it never reaches SavedStateHandle or a nav key), the crop math and the D-25 `listFieldFor` rule all hold up. The problems are in state lifecycles that cross screens and sessions:

1. A client-only user who taps "Expert" is converted to expert by the server (D-23), then gets stuck in the setup hub on every launch, with no way back to client mode.
2. The D-06 "publish blocked" marker is never cleared when the user fixes the step, so Publish stays disabled.
3. Photo working files on disk outlive the session when the repository singleton was never created in that process, so one account's cropped photo can be uploaded as another account's avatar.

Several races also let a step's result, or a stale hydrate, write over newer data. These are the logout vs mode-switch race, Back during a save, and hydrate overwriting a save.

## Critical Issues

### CR-01: Switching a client-only user to Expert traps them in the setup hub permanently

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/managers/UserManager.kt:255-257, 326-332`; `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/overview/SignupOverviewScreen.kt:141-145`; `app/src/main/java/com/uptrendinvestments/clickmeapp/navigation/SignupSetupNavHost.kt:46-51`
**Issue:** `requestModeSwitch(Expert)` calls `GET /expert/profile`. For a client-only account this runs `ensureExpert` (D-23), which permanently adds the expert role, and then returns `setup_completed: false`. AppViewModel routes to `AppRoute.SignupSetup`.

The hub has `onBack = null`, and its only action is Log out. System Back at the hub root exits the app. There is no "back to client mode" path. On the next launch or login, `resolvePostAuthDestination` sees `UserRole.Expert in roles && isExpertProfileIncomplete()` and routes to `SignupSetup` again. Logging out clears the stored mode, so logging back in does not help either.

The result: a client who taps the Expert card once, even by accident, can never use client mode again until they finish and publish an expert profile. D-17 says "switching to client is always allowed". That promise is broken from the moment the switch lands in the hub.
**Fix:** Give the hub an exit when the user arrived by a mode switch, or whenever they also hold the client role. Then stop forcing setup at launch for those users:
```kotlin
// UserManager.resolvePostAuthDestination
val isExpert = UserRole.Expert in roles
val isClient = UserRole.Client in roles
// Only a pure expert signup is forced into setup; a converted client keeps client mode.
if (isExpert && !isClient && isExpertProfileIncomplete()) return LaunchDestination.SignupSetup

// AppViewModel
fun onSetupExited() { viewModelScope.launch { userManager.setMode(AppMode.Client); _route.value = AppRoute.ClientHome } }
// SignupSetupNavHost(onPublished, onExit = appViewModel::onSetupExited) -> hub TopAppBar close / "Not now" action
```

### CR-02: The D-06 publish-blocked marker survives the fix, so Publish stays disabled

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/SignupDraft.kt:28`; `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/SignupSetupRepository.kt:160, 229-237, 242-249, 303-306`; `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/overview/SignupOverviewViewModel.kt:81-95`
**Issue:** When publish returns 422 `missing_fields`, `publish()` stores the steps in `publishBlockedSteps`. `isComplete()` returns false for any step in that set. Only `runHydrate` with a successful profile fetch clears the set (line 160).

The step saves (`patch`, `uploadAvatar`, `removeAvatar`, `verifyCode`) never touch it. When the hub is shown again, `onShown()` calls only `refreshEmailStatus()` while hydration is `Loaded`, never `hydrate()`. So the flow D-06 is built for goes wrong: dialog, OK, back to the hub, user opens "Add a profile picture", uploads, Save. The photo row stays unchecked, Review still lists it under "Still needed", and Publish stays disabled. The only ways out are an undiscoverable pull-to-refresh on the hub or a process restart.

No test covers clearing after a save. `SignupSetupRepositoryTest` only checks that hydrate clears the set.
**Fix:** Clear the step's marker when its save succeeds. Simplest is to drop the whole set on any successful write, since the server is re-checked on publish anyway:
```kotlin
private suspend fun patch(request: SetupExpertProfileRequest, apply: (SignupDraft) -> SignupDraft) {
    if (!request.hasAnyField()) return
    val startGeneration = generation
    apiCall { api.setupExpertProfile(request) }
    if (startGeneration == generation) _draft.update { apply(it).copy(publishBlockedSteps = emptySet()) }
}
// same in uploadAvatar(): it.copy(avatarUrl = url, publishBlockedSteps = it.publishBlockedSteps - SetupStep.ProfilePhoto)
```

### CR-03: Photo working files leak across sessions and accounts (T-04-18 mitigation incomplete)

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/SignupSetupRepository.kt:102-110, 314-324`; `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/profilephoto/AndroidPhotoProcessor.kt:42-44`; `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/profilephoto/SignupProfilePhotoViewModel.kt:74-82, 116-126`; `app/src/main/java/com/uptrendinvestments/clickmeapp/managers/UserManager.kt:163-180`
**Issue:** The cropped photo lives on disk in `cacheDir/signup/photo_crop.jpg` and survives process death. Only `SignupSetupRepository.reset()` deletes it. That runs from an `isLoggedIn` collector created in the repository's `init`, but the repository is a lazily created Hilt singleton. In any process where nothing injected it before the logout, no collector exists and the files stay.

A concrete path:
1. User A picks and crops a photo on the Photo step without saving.
2. The process is killed in the background.
3. On relaunch the expert-profile check fails (offline), so D-11 routes to home. Or the refresh token has expired, so restore logs out inside `afterMeFailure`.
4. A logs out from client home. The repository was never created in this process, so `reset()` never runs.
5. User B signs up on the same device and opens Profile Photo. `onShown()` reads `croppedPreview()` and shows A's face as B's unsaved photo, and Save uploads A's JPEG to B's account.

**Fix:** Tie the file cleanup to the session itself, not to a lazily created observer. For example, call `photoProcessor.clearWorkingFiles()` from `UserManager.logout()` and at the start of `signup()`/`login()`. Or eagerly create `SignupSetupRepository` from `Application.onCreate`. Defense in depth: name the files by user id and ignore files that belong to a different user.
```kotlin
// UserManager.logout(), inside the NonCancellable block
runCatching { photoWorkingFiles.clear() }   // inject PhotoProcessor (or a small cleanup interface)
```

## Warnings

### WR-01: Mode writes are not session-guarded; a switch or late restore can persist a mode into the next session

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/managers/UserManager.kt:255-263, 320-333, 398-401`; `app/src/main/java/com/uptrendinvestments/clickmeapp/AppViewModel.kt:53-60, 119-134`
**Issue:** `switchJob` is cancelled only when the main-thread `isLoggedIn` collector runs. `logout()` runs on `io` and flips the flag from there, so the switch can resume first on Main and call `setMode(Expert)`. It can resume because the GET finished, or because it failed with 401 after the tokens were cleared, which "counts as complete". `sessionStore.setMode` may then land after `sessionStore.clear()`.

The cap path has the same problem: a late `restore()` → `resolvePostAuthDestination` → `setMode` runs after a logout that happened in between. The next login reads `sessionStore.storedMode()` and inherits the previous user's mode. For a client-only next user that means ExpertHome, and any expert call there converts the account (D-23) without consent.

`onSetupPublished()` also sets `ClientHome` with no `isLoggedIn` check. If a logout or rejection lands during `completeSetup()`, that shows a protected route with no session.
**Fix:** Capture `sessionGeneration` before the lookup in `requestModeSwitch`/`resolvePostAuthDestination`. Skip `setMode` (and return `Login`) if it changed. In AppViewModel, check `userManager.isLoggedIn.value` before applying `switchMode` and `onSetupPublished` results.

### WR-02: A stale hydrate can overwrite a newer step save

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/SignupSetupRepository.kt:147-168, 329-345`
**Issue:** `runHydrate` merges the server snapshot into the current draft, preferring server values (non-blank name/city/country/timezone, the whole language and tag lists, and `avatarUrl` unconditionally). Saves do not cancel or fence an in-flight hydrate, and `generation` changes only on `reset()`.

Here is how it fails. After process death, the host starts hydrating. The user is restored onto a step, saves before the GET returns, and the GET then returns the pre-save snapshot. The draft goes back to the old values. For example, a freshly uploaded `avatarUrl` becomes null, or saved tags are replaced. Publish then sends the reverted payload.
**Fix:** Bump a write counter on every successful save and drop the hydrate merge if it changed after the hydrate started (or cancel `hydrateInFlight` on save):
```kotlin
@Volatile private var writes = 0
// patch/uploadAvatar/removeAvatar/verifyCode success: writes++
// runHydrate: val startWrites = writes ... if (startGeneration != generation || startWrites != writes) return false
```

### WR-03: The pre-selected English overwrites the server's languages when the form was not seeded from the server

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/basicinfo/SignupBasicInfoViewModel.kt:114-136, 252-274`; `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/SignupSetupRepository.kt:204`
**Issue:** `seedFrom` fills `languages = draft.languages.ifEmpty { listOf(ENGLISH) }`, and the collector stops re-seeding after the first edit of any field.

There are two ways the user can save `["en"]` over a server list they never touched:
- Hydrate failed. The draft languages are empty, and `listFieldFor(["en"], [])` returns `["en"]`.
- The user typed a first name before a late hydrate landed. `touched` stays true, so languages are not re-seeded and the form still shows English. Save sends `["en"]`, a non-empty selection, which replaces the server list.

The server does a full replace, so the user's real languages are deleted. This defeats the D-25 intent ("omit when untouched").
**Fix:** Track languages separately (for example `languagesTouched`). Send `languages` only when the user changed them, or when the draft came from a successful hydrate and was empty. Don't apply the English default when hydration is `Failed` or `Loading`.

### WR-04: Users can never clear the optional province/state (or any text field) once it is saved

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/SignupSetupRepository.kt:194-213`
**Issue:** `provinceValue = provinceState.trim().ifBlank { null }` omits the field, so the server keeps the old value. The draft also keeps it (`provinceValue ?: draft.provinceState`). The field shows empty, Save reports "Saved", and when the user comes back the step is re-seeded with the old province. Review then shows the old province, and it is published. D-03 makes the province optional, so clearing it is a valid edit.
**Fix:** Send an empty string when the user cleared a value that the draft had: `provinceState = if (provinceValue == null && current.provinceState.isNotBlank()) "" else provinceValue`. The server's `z.string().optional()` accepts `""`. Update the draft to match.

### WR-05: System Back during Signup Initial submit can cancel the signup after the account exists

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/initial/SignupInitialScreen.kt:122-127`; `app/src/main/java/com/uptrendinvestments/clickmeapp/managers/UserManager.kt:140-153`
**Issue:** Only the top-bar arrow is disabled while `isSubmitting`. There is no `BackHandler`, unlike Review, Photo, Tags and Verify. A system or predictive Back pops `SignUpKey`, clears the ViewModel and cancels `userManager.signup()`. Two outcomes are possible:
- The cancellation hits during the network call. The account is created but no tokens are stored, and a retry gets 409 "email taken".
- The cancellation hits at the `withContext(io)` return. Tokens are saved, but `_isLoggedIn` is never set, so the user sits on Login with a stored session that only shows up at the next launch.

**Fix:** Add `BackHandler(enabled = state.isSubmitting) {}` in `SignupInitialContent`. Also make the post-response bookkeeping in `signup()`/`login()` non-cancellable (`withContext(NonCancellable + io) { tokens.save(...) }`, then set the flags).

### WR-06: Basic Info and Timezone allow Back mid-save, so the server and the draft diverge

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/basicinfo/SignupBasicInfoScreen.kt:144`; `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/timezone/SignupTimezoneScreen.kt:104`
**Issue:** These two steps have no `BackHandler` or `backEnabled` guard while `saveState != Idle`. The other steps guard on purpose. Back pops the entry and cancels `viewModelScope` after the PATCH reached the server, but before `patch()` applies the draft update. The hub then shows the step incomplete even though the server has the data, and the step re-seeds from the stale draft.
**Fix:** Mirror the Tags screen: `BackHandler(enabled = !editable) {}` and `AuthTopAppBar(..., backEnabled = editable)`.

### WR-07: Custom (id-less) languages are silently deleted by Save and Publish

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/SignupSetupRepository.kt:296, 340-342`
**Issue:** `mergeProfile` drops languages whose `id` is null (`mapNotNull`). The DTO documents these as custom free-text languages. Publish always sends `languages = current.languages.map { it.id }`, and the server does a full delete-then-insert on `user_languages`, so any custom language saved by another client is deleted on publish. A Basic Info save that sends languages deletes it too. A user whose only language is custom also sees Personal Details as incomplete.
**Fix:** Keep the id-less entries in the draft (for example a `customLanguages` field). Omit `languages` from the publish payload when the selection is unchanged from the hydrated server list (apply D-25 on publish as well), or block with a clear message.

### WR-08: FakePhotoProcessor diverges from the real processor, so photo tests can pass on broken flows

**File:** `app/src/test/java/com/uptrendinvestments/clickmeapp/testing/FakePhotoProcessor.kt:28-62`
**Issue:**
- `cropToJpeg` sets `cropped` but not `preview`. With the real processor, a stored crop always yields a `croppedPreview()`, and `SignupProfilePhotoViewModel` relies on that for `localPreview` and `hasUnsavedPhoto`. Tests driving crop then requestRemove or save exercise a state that can't happen in production.
- `importPicked` nulls `cropped` but leaves `preview`. In production, a new pick deletes the old crop preview.
- `clearWorkingFiles` also resets the scripted `importError`/`cropError`, so a test that clears and then expects the scripted failure silently gets success.

**Fix:** Model the processor's file state (`hasCrop`), derive `croppedPreview()` from it, and keep test configuration separate from simulated file state.

## Info

### IN-01: PII and the OTP are written to SavedStateHandle, contrary to the T-04-49 note

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/basicinfo/SignupBasicInfoViewModel.kt:153-163`; `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/verifyemail/SignupVerifyEmailViewModel.kt:169, 237`; `app/src/main/java/com/uptrendinvestments/clickmeapp/navigation/SignupSetupKeys.kt:6-10`
**Issue:** The keys' KDoc says PII stays in the repository. But first name, city, province and the email are stored in the saved-instance Bundle, and so is the 6-digit email code (Phase 3's SetNewPassword does the same). The password is correctly kept out; I checked `SignupInitialViewModel`. This is a deliberate trade-off for surviving process death, but the threat note is inaccurate.
**Fix:** Either update T-04-49 / the KDoc to say saved state holds this data, or drop the OTP from saved state (it is short-lived and easy to retype).

### IN-02: Picker URIs are embedded in exception messages

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/profilephoto/AndroidPhotoProcessor.kt:57, 83, 149`
**Issue:** `PhotoImportException("Not a readable image: $uri")` puts content URIs into messages that a future crash reporter would capture.
**Fix:** Use static messages.

### IN-03: EXIF TRANSPOSE/TRANSVERSE (5/7) and flips are ignored

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/profilephoto/PhotoProcessor.kt:68-74`
**Issue:** Orientations 5 and 7 imply a 90/270 rotation plus a mirror. Images with those values end up sideways.
**Fix:** Handle all eight orientations with `Matrix.postScale(-1f, 1f)` plus a rotation.

### IN-04: Uncaught IOExceptions in the photo step, and partial source writes

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/profilephoto/SignupProfilePhotoViewModel.kt:122-125, 155-157`; `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/profilephoto/AndroidPhotoProcessor.kt:85-90, 139`
**Issue:** `croppedJpeg()` (`readBytes`) and `clearWorkingFiles()` can throw IOException. `save()` and `requestRemove()` catch only `ApiException`, so a disk error crashes the app. `importPicked` deletes the old crop and truncates `photo_src.jpg` before `compress` finishes, so a failed write leaves a corrupt source. On that path `prepared` is also never recycled.
**Fix:** Catch `IOException` in the ViewModel. Write to a temp file and rename it. Recycle in `finally`.

### IN-05: A fresh draft counts Timezone as complete before anything is saved, and the comment cites the wrong decision

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/SignupSetupRepository.kt:358-359`
**Issue:** `freshDraft()` seeds `timezone = Timezones.detected().id`, so a brand-new account shows 1/5 with Timezone checked while the server has no timezone. Publish sends it, so this works, but the progress is optimistic. The comment cites D-09 (email verification); the relevant decision is D-13.
**Fix:** Fix the reference, or count Timezone complete only after a save or hydrate.

### IN-06: Timezone uses a hard-coded 600 ms hold

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/timezone/SignupTimezoneViewModel.kt:39, 130`
**Issue:** `SAVED_HOLD_MS = 600L` duplicates `Motion.SAVED_CONFIRMATION_HOLD_MS`, which the other steps use.
**Fix:** Use the shared constant.

### IN-07: The avatar tap pushes ModeSwitchKey without a duplicate guard, and the switch gives no in-flight feedback

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/home/HomeClientScreen.kt:121`; `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/home/HomeExpertScreen.kt:85`; `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/modeswitching/ModeSwitchScreen.kt:58-61`
**Issue:** `navigator.navigate(ModeSwitchKey)` always pushes, so a double tap stacks two pages. Picking Expert pops the page right away while `GET /expert/profile` runs, and nothing on screen shows the switch is pending.
**Fix:** Guard with a top-of-stack check (as `SignupSetupNavigator.pushUnlessOnTop` does), and show a progress state until `switchMode` resolves.

### IN-08: Narrow races in the repository's generation guards

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/SignupSetupRepository.kt:156-166, 273-275`
**Issue:**
- `runHydrate` checks `generation` once and then writes the draft and `Loaded` without re-checking, so a `reset()` between those lines is overwritten.
- `recordCodeSent` has no generation guard, so a send that completes after logout marks "code sent" for the next session.

**Fix:** Do the check and the write together under `lock`. Pass the generation into `recordCodeSent`.

### IN-09: A server country outside the curated 50 shows as a raw code and is never marked selected

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/constants/Countries.kt:96-100`; `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/basicinfo/subviews/CountryPickerSheet.kt:60-66`
**Issue:** `displayName("NGA")` returns "NGA", and the picker has no selected row for it.
**Fix:** Fall back to `Locale` display names for any valid alpha-3, or add the hydrated country to the list, as the Timezone step does for zones.

---

_Reviewed: 2026-10-21T17:03:03Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_

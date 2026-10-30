---
phase: 15-polish-hardening
plan: 06
subsystem: signup
tags: [signup, saved-state, process-death, secrets, large-screen, tracer]
status: complete

requires: [15-02]
provides:
  - "SignupVerifyEmailViewModel holds the verification code in memory only; only code-sent and cooldown-end are mirrored"
  - "SignupSetupRestorationTest: step and typed-text restoration through the real SignupSetupNavDisplay and entry decorators"
  - "contentMaxWidth() on SignupInitial, BasicInfo, Timezone, VerifyEmail, ProfilePhoto, Tags, Review, Overview and Onboarding"
affects: [15-17]

tech-stack:
  added: []
  patterns:
    - "Restoration test: StateRestorationTester + fresh ViewModelStoreOwner + fresh repository to emulate process death over the real Nav3 decorators, screens built with explicit viewModel { ... createSavedStateHandle() } factories"
    - "Lazy lists are capped by contentMaxWidth() on the LazyColumn itself (Tags)"

key-files:
  created:
    - app/src/test/java/com/uptrendinvestments/clickmeapp/navigation/SignupSetupRestorationTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/verifyemail/SignupVerifyEmailViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/initial/SignupInitialScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/basicinfo/SignupBasicInfoScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/timezone/SignupTimezoneScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/verifyemail/SignupVerifyEmailScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/profilephoto/SignupProfilePhotoScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/tags/SignupTagsScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/review/SignupReviewScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/overview/SignupOverviewScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/onboarding/OnboardingScreen.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/signup/verifyemail/SignupVerifyEmailViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/signup/initial/SignupInitialViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/signup/basicinfo/SignupBasicInfoScreenTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/signup/review/SignupReviewScreenTest.kt

key-decisions:
  - "Removed the OTP mirror (verify_code) as CONTEXT D-09 says, overriding the Phase 4 choice to keep it; a user recreated mid-code loses typed digits by design. STATE.md wording is updated by 15-17."
  - "Tags: the LazyColumn itself is capped with contentMaxWidth() rather than each item; touch-scrolling outside the 600 dp column does not scroll the list on a large window (acceptable trade-off, no per-item churn)."
  - "Short-window test uses h360dp (stricter than the plan's h600dp) and checks the pinned Save button plus a scrolled-to last row, because Save is pinned outside the scroll area (there is no Continue button on Basic Info)."

patterns-established:
  - "Secrets (code, password) are asserted absent from SavedStateHandle by value and by key in the ViewModel tests"

requirements-completed: [POL-01, POL-02, POL-03]

duration: ~2 h wall clock (first Gradle run alone took 39 min under shared-daemon contention and OOMed the Kotlin daemon; later runs 1-6 min)
completed: 2026-10-29
actuals:
  tokens: 21000
  tasks: 3
  commits: 4
plan_head_before: e808da6b9c3dd62717fc017104c562ebca5b1350
plan_head_after: ceffbe731dee522fad2a2446334856c1fe879ee3
---

# Phase 15 Plan 06: Signup restoration, secrets and large-screen cap Summary

Signup now restores its step and typed text after Activity recreation and process death, the email verification code never enters saved state, and nine signup/onboarding screens cap their content at 600 dp on a large window.

## Tasks

| Task | Name | Commits |
| ---- | ---- | ------- |
| 1 (tracer, tdd) | Restoration through the real nav display; code and password out of saved state | cbf3abdc (RED), ac66eff2 (GREEN) |
| 2 (tdd) | Initial, Basic Info, Timezone, Verify Email, Profile Photo capped | 30e5acfe |
| 3 (tdd) | Tags, Review, Overview, Onboarding capped; Review in-flight/failure contract proven | ceffbe73 |

## RED / GREEN evidence

- Task 1 RED (cbf3abdc): `SignupVerifyEmailViewModelTest.codeNeverInSavedState` and `cooldownStillRestores_withAnEmptyCodeAndNoError_afterProcessDeath` failed on the planned assertions (code present in the handle / restored as "987654"); `SignupSetupRestorationTest.stepAndTextRestored` failed at the post-restore "Enter 6-digit code" placeholder assertion because the restored field still held "123456". Semantic assessment: all three ran, compiled and failed for the intended reason (the OTP mirror), not for setup faults. A first run hit a fixture fault (TemporaryFolder used in a field initializer); that was fixed before the RED run counted. Pinning tests `passwordNeverInSavedState` and `restoredTextBeatsHydration` were already green (the behaviour existed; they lock it in), as the plan expected.
- Task 1 GREEN (ac66eff2): `KEY_CODE` and both writes removed; all targeted tests, `SignupVerifyEmailScreenTest`, `SignupBasicInfoViewModelTest` and `ktlintCheck` pass.
- Task 2/3 caps: the main edits were written before the tests, then the cap was shown RED by reverting only the screen file: `SignupBasicInfoScreenTest.formCappedOnLargeWindow` and `SignupReviewScreenTest.reviewCappedOnLargeWindow` each failed alone against the uncapped screen and pass with it. The other new tests (short window, in-flight, failure) pin existing behaviour and passed from the start.
- Tracer gate: the verify command for Task 1 passed end to end before Tasks 2 and 3 expanded.

## Verification

- `SignupVerifyEmailViewModelTest`, `SignupVerifyEmailScreenTest`, `SignupSetupRestorationTest` (2/2), `SignupInitialViewModelTest`, `SignupBasicInfoViewModelTest` plus `ktlintCheck`: BUILD SUCCESSFUL.
- `SignupInitialScreenTest`, `SignupBasicInfoScreenTest` (+2), `SignupTimezoneScreenTest`, `SignupVerifyEmailScreenTest`, `SignupProfilePhotoScreenTest` plus `ktlintCheck`: BUILD SUCCESSFUL.
- `SignupTagsScreenTest`, `SignupReviewScreenTest` (+3), `SignupOverviewScreenTest`, `OnboardingScreenTest` plus `ktlintCheck`: BUILD SUCCESSFUL.
- `bash scripts/check-design-rules.sh`: "Design rules: OK".
- Acceptance greps: `"verify_code"` 0, `savedStateHandle[KEY_CODE]` 0, `KEY_COOLDOWN_END` 3 (kept), `contentMaxWidth()` present in the 5 + 4 screen files, `PhotoCropScreen.kt` 0.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] SignupSetupNavHost replaced by the Hilt-free nav display in the restoration test**
- **Found during:** Task 1
- **Issue:** `SignupSetupNavHost` needs `hiltViewModel()`; a plain Robolectric compose test has no Hilt graph.
- **Fix:** The test drives the production `SignupSetupNavDisplay`, `rememberNavigationState`, `SignupSetupNavigator` and the real `SignupBasicInfoScreen` / `SignupVerifyEmailScreen`, passing explicit `viewModel { X(h.repository, ..., createSavedStateHandle()) }` factories (the approach of `EntrySavedStateHandleTest`). The Hilt wiring and the host's `ensureHydrated()` call are replaced by an explicit `repository.hydrate()` in `restoredTextBeatsHydration`.
- **Files modified:** `SignupSetupRestorationTest.kt`
- **Commit:** cbf3abdc

**2. [Rule 3 - Blocking] `SignupInitialViewModel.EMAIL_KEY` is private**
- **Found during:** Task 1 RED compile
- **Fix:** The test declares its own `EMAIL_KEY = "signup_email"` constant and asserts it is the only key in the handle.
- **Commit:** cbf3abdc

**3. [Plan adaptation] Short-window test**
- The plan named a Continue button; Basic Info has a pinned Save button outside the scroll area, so `performScrollTo()` on it would fail. The test scrolls to the last in-scroll row ("Add language...") and clicks the pinned Save, on `w900dp-h360dp`. No behaviour change.

**4. [Process] Kotlin daemon OOM**
- The first Gradle invocation (39 min, shared daemons) failed with `OutOfMemoryError` in the Kotlin compiler. Re-running with `-Pkotlin.daemon.jvmargs=-Xmx4g -Dorg.gradle.jvmargs=-Xmx3g` worked. No code change.

Review needed no production change for the in-flight/failure contract: `AsyncSaveButton` already ignores taps unless Idle, announces the busy state, and the ViewModel guards `publish()`; tests now pin this.

## Open Items (deferred device checks)

- Real-device check that rotation and "Don't keep activities" leave Verify Email with an empty code field and no focus ring, and Personal Details with typed text intact (the Robolectric test emulates the registry and ViewModel-store loss, not a real process kill).
- Visual check of the 600 dp cap on a tablet or resized window: app bars full width, Onboarding imagery and glow full width, Tags list scrolling.
- The "no focus ring on the empty code field after restore" part of the UI Consideration is not asserted by an automated test (focus is not part of the Robolectric assertions here).

## Known Stubs

None.

## Threat Flags

None. T-15-11, T-15-12 and T-15-13 are mitigated and tested (`codeNeverInSavedState`, `passwordNeverInSavedState`, `restoredTextBeatsHydration`).

## Self-Check: PASSED

- Created/modified files present: SignupSetupRestorationTest.kt FOUND, SignupVerifyEmailViewModel.kt FOUND.
- Commits cbf3abdc, ac66eff2, 30e5acfe, ceffbe73 are ancestors of HEAD.

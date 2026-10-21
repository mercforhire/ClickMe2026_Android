---
phase: 04-signup-onboarding-mode-switch
verified: 2026-10-21T21:00:00Z
status: human_needed
score: 4/4 roadmap success criteria verified (automated); 7/7 requirements satisfied in code
overrides_applied: 0
re_verification: false
gaps: []
deferred: []
human_verification:
  - test: "Full signup on the Samsung device: create an account, fill all five hub steps in a non-default order, Review, Publish"
    expected: "Lands on client home with the onboarding carousel; no username or phone field appears anywhere"
    why_human: "End-to-end flow against the real backend and device UI"
  - test: "Verify Email with a real emailed code"
    expected: "Nothing is emailed when the step opens. A code arrives only after tapping Send. Resend shows a 30 s countdown and re-enables at zero."
    why_human: "Real email delivery and visual countdown; the logic is covered by unit tests with a fake clock"
  - test: "Profile Photo crop feel (pick, pinch-zoom, pan under the circular guide, Use, Save)"
    expected: "Gestures feel right, the output is square, and the upload succeeds"
    why_human: "Touch gesture quality and image output"
  - test: "Kill the app mid-setup and relaunch; also log out and log in during an unfinished setup"
    expected: "Both resume into the hub with completed steps checked"
    why_human: "Real process death and a real session"
  - test: "Tap 'Not now' on the hub, relaunch (client home, no forced setup), log out, log in"
    expected: "After the re-login the app resumes into setup. Check the app-bar 'Not now' action at 200% font scale."
    why_human: "Device persistence and font-scale layout (the routing logic is unit-tested)"
  - test: "Switch client to expert to client from the top-bar avatar, then force-stop and relaunch"
    expected: "The last chosen mode is restored. The carousel does not reappear after being dismissed once. It reappears after logout and login."
    why_human: "Real relaunch persistence and the visual carousel"
---

# Phase 4: Signup, Onboarding & Mode Switch Verification Report

**Phase Goal:** A new user can create an account, finish expert setup, and move between client and expert modes.
**Verified:** 2026-10-21
**Status:** human_needed (all automated checks pass; remaining items need a device and are logged per project policy)
**Re-verification:** No, initial verification

## Goal Achievement

### Observable Truths (ROADMAP success criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | A new user creates an account on Initial (no username or phone), completes Basic Info + language, Timezone, Photo, Verify Email, Tags in any order, then reviews and publishes | VERIFIED | `SignupInitialViewModel.submit` calls `userManager.signup(email, password)` and the request carries only email, password and role=expert. A grep for username or phone under `screens/signup` found nothing. `SignupSetupNavHost` registers the hub plus Personal Details, Timezone, Photo, Crop, Verify Email, Tags and Review, and each step is opened independently from the hub. `SignupSetupRepository` saves through `PATCH expert/profile/setup` (`patch`, `saveBasicInfo`, `saveTimezone`, `saveTags`) and `uploadAvatar` posts multipart `image`. `publish()` sends the full payload with `setupCompleted = true` and a normalized country. `SignupDraft.canPublish` gates Publish on the server's required fields (D-03). The Phase 3 placeholders are deleted and nothing references them. |
| 2 | The verification code is sent only on tap; resend is disabled for 30 s | VERIFIED | The only caller of `repository.sendVerificationCode()` (`POST auth/email/resend`) is `SignupVerifyEmailViewModel.requestCode`, reached from `send()` and `resend()` (button taps). `requestCode` returns early while `cooldownActive()`. `COOLDOWN_MS = 30_000`, tracked on `MonotonicClock`. The Screen disables the button while `resendSecondsRemaining > 0`. 429 `RATE_LIMITED` counts as sent (D-08). Tests: `opening_sendsNothing_andTakesTheEmailFromTheDraft`, `resend_isIgnoredWhileCountingDown_andAtZeroCallsClearsCodeAndRestarts`, `reEntry_within30s_keepsCodeFieldAndCountdown_andResendStaysDisabled`. |
| 3 | A user who leaves signup halfway is resumed into setup at next login or launch, with completed steps kept (including the D-27 "Not now" interaction) | VERIFIED (see note) | Resume: `UserManager.restoreInternal` and `LoginViewModel` (line 129) both go through `resolvePostAuthDestination`, which returns `SignupSetup` when an expert's `GET /expert/profile` has `setupCompleted=false`. `SignupSetupNavHost` runs `ensureHydrated()`, and `runHydrate` merges `GET /expert/profile` plus `GET /auth/email/status` into the draft, so completed steps are kept (server-side PATCHes). The D-27 deferral is per login: `deferSetup` sets `setupDeferred` and persists `SessionStore.setSetupDeferred(true)`. `logout()` sets `setupDeferred = false` and `sessionStore.clear()` wipes the key, so the next login resumes. `requestModeSwitch(Expert)` and `completeSetup` also clear it. `recheckSetupIfPending` honors it. Tests: `AppViewModelTest` (`notNowRoutesToClientHomeAndRemembersTheDeferral`, `relaunchWhileDeferredGoesToClientHome`, `foregroundRecheckIsSkippedWhileDeferred`, `switchingToExpertReopensSetupAndClearsTheDeferral`, `logoutThenLoginResumesSetupAgain`, `notNowAfterTheSessionEndedStaysOnLogin`), the `UserManagerTest` mirror set including `logoutClearsTheDeferralSoTheNextLoginResumesSetup`, and `SessionStoreTest.setupDeferredRoundTripsAndClearRemovesIt`. |
| 4 | After publishing the user can log out and in again; the carousel shows only on the first launch into client home; the user can switch modes and the mode persists across relaunch | VERIFIED | Publish: `Review.publish` leads to `AppViewModel.onSetupPublished`, then `completeSetup` (mode Client, persisted) and route `ClientHome`. Carousel: `HomeClientScreen` shows `OnboardingContent` while `!hasSeenOnboarding`. Finish calls `markOnboardingShown`, which persists to `SessionStore`. Logout resets it (`_hasSeenOnboarding=false` plus `sessionStore.clear()`). Mode switch: the top-bar avatar on both homes navigates to `ModeSwitchKey` and `ModeSwitchScreen`, whose callback goes to `AppViewModel.switchMode`, then `requestModeSwitch`. This persists the mode through `setModeForSession`, with an incomplete-setup gate (D-17). Restore reads `sessionStore.storedMode()`. Tests: `requestModeSwitch*`, `restoreHonorsStoredExpertModeForClientOnlyUser`, `HomeClientViewModelTest`, `OnboardingScreenTest`, `ModeSwitchScreenTest`. |

**Score:** 4/4 truths verified by code and automated tests.

**Note on criterion 3:** "Not now" (D-27) is a user-confirmed, documented deviation. A user who explicitly taps it is not forced back into setup on relaunch within that login. The next login resumes setup, because logout clears the flag. A user who simply leaves (kills the app or loses the session) is always resumed. The roadmap intent is met and the interaction is implemented and tested.

### Required Artifacts

| Artifact | Status | Details |
|----------|--------|---------|
| `screens/signup/initial/*` | VERIFIED | Wired into `AuthNavHost` (`SignUpKey`); success leads to `onLoggedIn(SignupSetup)` |
| `screens/signup/{overview,basicinfo,timezone,profilephoto,verifyemail,tags,review}/*` | VERIFIED | All registered in `SignupSetupNavHost`; the VMs use `SignupSetupRepository` |
| `SignupSetupRepository`, `SignupDraft` | VERIFIED | Real API calls; hydrate merge with a write counter (WR-02) |
| `navigation/SignupSetupNavHost` | VERIFIED | Rendered by `ClickMeApp` for `AppRoute.SignupSetup` with `onPublished` and `onNotNow` |
| `screens/onboarding/*` | VERIFIED | Four pages, bundled PNG drawables present |
| `screens/settings/modeswitching/*` | VERIFIED | `mode_client.png` and `mode_expert.png` present; used by both homes |
| `UserManager`, `SessionStore`, `AppViewModel` | VERIFIED | Setup gate, deferral, session-guarded mode writes, and a mode collector that follows `currentMode` |
| Network layer (`ApiRequestUrls`, `ClickMeApi`) | VERIFIED | `auth/signup`, `auth/email/{resend,verify,status}`, `expert/profile/setup`, `meta/*`, `user/profile/avatar` |

### Key Link Verification

| From | To | Status |
|------|----|--------|
| `SignupInitialViewModel` to `UserManager.signup` to `POST auth/signup`, then `AuthNavHost.onLoggedIn` to `AppViewModel.onLoggedIn` | WIRED |
| Hub "Not now" to `AppViewModel.onSetupDeferred` to `UserManager.deferSetup` to `SessionStore.setSetupDeferred` | WIRED |
| Review publish to `AppViewModel.onSetupPublished` to `completeSetup` | WIRED |
| Home avatar to `ModeSwitchScreen` to `AppViewModel.switchMode` to `requestModeSwitch` | WIRED |
| `LoginViewModel` and `restore` to `resolvePostAuthDestination` to `SignupSetup` | WIRED |
| `isLoggedIn=false` to `SignupSetupRepository.reset` and `SessionDataCleaner` (photo files) | WIRED |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Full dev unit suite, forced rerun | `./gradlew cleanTestDevDebugUnitTest testDevDebugUnitTest` | 876 tests, 0 failures, 0 errors, 0 skipped | PASS |

Probe execution: no `probe-*.sh` declared by this phase. Skipped.

### Requirements Coverage

All 7 phase IDs appear in PLAN frontmatter (04-01 through 04-20). No orphaned IDs.

| Requirement | Source Plans | Status | Evidence |
|-------------|--------------|--------|----------|
| SIGN-01 | 04-01, 04-03, 04-06, 04-10, 04-20 | SATISFIED | Email and password only; 409 and 422 mapping; tests in `SignupInitialViewModelTest` |
| SIGN-02 | 04-01 to 04-05, 04-07, 04-09, 04-11 to 04-16, 04-19, 04-20 | SATISFIED | Five steps, PATCH saves, any order from the hub |
| SIGN-03 | 04-01, 04-07, 04-15, 04-20 | SATISFIED | Send on tap only; 30 s cooldown |
| SIGN-04 | 04-01, 04-07, 04-17, 04-19, 04-20 | SATISFIED | Review plus Publish with gating and a 422 `missing_fields` map |
| SIGN-05 | 04-01, 04-06, 04-07, 04-11, 04-19, 04-20 | SATISFIED | Resume via `resolvePostAuthDestination`; hydrate; D-27 logout clears the deferral |
| ONB-01 | 04-02, 04-06, 04-08, 04-18, 04-20 | SATISFIED | Persistent flag; logout clears it |
| ONB-02 | 04-02, 04-06, 04-08, 04-18, 04-20 | SATISFIED | Mode switch screen; persisted mode; D-17 gate |

### Anti-Patterns Found

None blocking. A scan of `screens/signup`, `screens/onboarding`, `screens/settings`, `managers`, `navigation` and `AppViewModel.kt` found no TBD, FIXME, XXX, TODO, HACK or PLACEHOLDER markers. The old Sign Up and Setup placeholders no longer exist.

### Findings and Warnings (non-blocking)

- **WARNING (documentation):** `.planning/REQUIREMENTS.md` still shows SIGN-01..05, ONB-01 and ONB-02 as unchecked and "Pending" (lines 47-58 and 273-279). Update them at phase completion.
- **INFO:** WR-03 changed behavior: on a fresh signup, English appears pre-selected only after the hub's hydrate succeeds. This is logged in the review-fix report as needing a human check, and is included in the device flow above.
- **INFO:** WR-07 limitation: editing languages in Basic Info replaces server-side custom languages, because the PATCH accepts catalogue ids only. This is documented and accepted.
- **INFO:** The CR-01 fix adds the "Not now" action in the hub's `TopAppBar`. Its fit at 200% font scale is a device check (see human_verification).

### Human Verification Required

See the `human_verification` frontmatter: six device checks, logged per the project's manual-testing-deferred policy. They are not gaps, because the automated tests cover the logic.

### Gaps Summary

No gaps. All four roadmap success criteria are backed by implemented, wired code and by a freshly re-run unit suite (876 tests, 0 failures). The D-27 interaction with criterion 3 is implemented (the flag is persisted per login and cleared by logout, a mode switch to Expert and publish) and covered at the `SessionStore`, `UserManager` and `AppViewModel` levels. Status is `human_needed` only because device-only checks remain.

---

_Verified: 2026-10-21_
_Verifier: Claude (gsd-verifier)_

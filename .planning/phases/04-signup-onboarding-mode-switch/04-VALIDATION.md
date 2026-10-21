---
phase: 4
slug: signup-onboarding-mode-switch
status: complete
nyquist_compliant: true
wave_0_complete: true
created: 2026-10-20
---

# Phase 4 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution. Source: `04-RESEARCH.md` § Validation Architecture.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4.13.2 + Robolectric 4.17 + kotlinx-coroutines-test 1.11.0 + mockwebserver3 5.5.0 + Compose UI test (`createComposeRule` under `RobolectricTestRunner`) |
| **Config file** | `app/src/test/resources/robolectric.properties`; `unitTests.isIncludeAndroidResources = true` in `app/build.gradle.kts` |
| **Quick run command** | `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDevDebugUnitTest --tests '<pattern>'` |
| **Full suite command** | `./gradlew ktlintCheck lintDevDebug lintProdDebug testDevDebugUnitTest testProdDebugUnitTest assembleDevDebug assembleProdDebug assembleProdRelease && bash scripts/check-design-rules.sh` |
| **Estimated runtime** | ~30 seconds quick (pure JVM), several minutes full |

Baseline: 421 tests per flavor, green at the end of Phase 3.

---

## Sampling Rate

- **After every task commit:** Run the targeted `--tests` pattern for the files touched
- **After every plan wave:** Run `./gradlew testDevDebugUnitTest ktlintCheck && bash scripts/check-design-rules.sh`
- **Before `/gsd-verify-work`:** Full suite must be green on both flavors, including `assembleProdRelease` (R8)
- **Max feedback latency:** 60 seconds

---

## Per-Task Verification Map

Task-level map (final, plan 04-20) followed by the requirement-level map (from RESEARCH).

### Task map

All commands are `./gradlew :app:testDevDebugUnitTest --tests <pattern>` with the JDK prefix from Test Infrastructure. Every test class was confirmed to exist under `app/src/test`. The whole suite ran green on both flavors in 04-20 (809 tests per flavor).

| Task ID | Plan | Wave | Requirement | Automated Command (test patterns) | File Exists | Status |
|---------|------|------|-------------|-----------------------------------|-------------|--------|
| 04-01-T1 | 01 | 1 | SIGN-01, SIGN-02, SIGN-03, SIGN-04, SIGN-05 | `*ResponseDecodingTest` `*ApiErrorParserTest` `*UserManagerTest` `*AppViewModelTest` | yes | green |
| 04-01-T2 | 01 | 1 | SIGN-01, SIGN-02, SIGN-03, SIGN-04, SIGN-05 | `*ApiRequestUrlsTest` `*ClickMeApiTest` + ktlintCheck | yes | green |
| 04-02-T1 | 02 | 1 | SIGN-02, ONB-01, ONB-02 | `*CountriesTest` `*TimezonesTest` | yes | green |
| 04-02-T2 | 02 | 1 | SIGN-02, ONB-01, ONB-02 | `*BundledAssetsTest` + assembleDevDebug + PNG size check | yes | green |
| 04-03-T1 | 03 | 1 | SIGN-01, SIGN-02 | `*AuthValidatorsTest` | yes | green |
| 04-03-T2 | 03 | 1 | SIGN-01, SIGN-02 | `*AuthComponentsTest` `*AvatarTest` `*AuthStaticScreensTest` `*LoginScreenTest` `*ForgotPasswordScreenTest` `*SetNewPasswordScreenTest` + ktlintCheck, lintDevDebug, check-design-rules | yes | green |
| 04-04-T1 | 04 | 1 | SIGN-02 | `*PickerComponentsTest` | yes | green |
| 04-04-T2 | 04 | 1 | SIGN-02 | `*PickerComponentsTest` + ktlintCheck, lintDevDebug, check-design-rules | yes | green |
| 04-05-T1 | 05 | 1 | SIGN-02 | `*CropMathTest` | yes | green |
| 04-05-T2 | 05 | 1 | SIGN-02 | `*PhotoProcessorTest` `*CropMathTest` + ktlintCheck, lintDevDebug | yes | green |
| 04-06-T1 | 06 | 2 | SIGN-01, SIGN-05, ONB-01, ONB-02 | `*UserManagerTest` `*LoginViewModelTest` `*RealtimeConnectionManagerTest` | yes | green |
| 04-06-T2 | 06 | 2 | SIGN-01, SIGN-05, ONB-01, ONB-02 | `*AppViewModelTest` `*UserManagerTest` + ktlintCheck | yes | green |
| 04-07-T1 | 07 | 2 | SIGN-02, SIGN-03, SIGN-04, SIGN-05 | `*SignupDraftTest` | yes | green |
| 04-07-T2 | 07 | 2 | SIGN-02, SIGN-03, SIGN-04, SIGN-05 | `*SignupSetupRepositoryTest` `*SignupDraftTest` + ktlintCheck | yes | green |
| 04-08-T1 | 08 | 2 | ONB-01, ONB-02 | `*ModeSwitchScreenTest` | yes | green |
| 04-08-T2 | 08 | 2 | ONB-01, ONB-02 | `*OnboardingScreenTest` `*ModeSwitchScreenTest` + ktlintCheck, lintDevDebug, check-design-rules | yes | green |
| 04-09-T1 | 09 | 2 | SIGN-02 | `*PhotoCropViewModelTest` | yes | green |
| 04-09-T2 | 09 | 2 | SIGN-02 | `*PhotoCrop*` + ktlintCheck, lintDevDebug, check-design-rules | yes | green |
| 04-10-T1 | 10 | 3 | SIGN-01 | `*SignupInitialViewModelTest` | yes | green |
| 04-10-T2 | 10 | 3 | SIGN-01 | `*SignupInitial*` `*AuthStaticScreensTest` `*AuthNavDisplayTest` `*AuthNavigatorTest` + ktlintCheck, lintDevDebug, check-design-rules | yes | green |
| 04-11-T1 | 11 | 3 | SIGN-02, SIGN-05 | `*SignupOverviewViewModelTest` | yes | green |
| 04-11-T2 | 11 | 3 | SIGN-02, SIGN-05 | `*SignupOverview*` + ktlintCheck, lintDevDebug, check-design-rules | yes | green |
| 04-12-T1 | 12 | 3 | SIGN-02 | `*SignupBasicInfoViewModelTest` | yes | green |
| 04-12-T2 | 12 | 3 | SIGN-02 | `*SignupBasicInfo*` + ktlintCheck, lintDevDebug, check-design-rules | yes | green |
| 04-13-T1 | 13 | 3 | SIGN-02 | `*SignupTimezoneViewModelTest` | yes | green |
| 04-13-T2 | 13 | 3 | SIGN-02 | `*SignupTimezone*` + ktlintCheck, lintDevDebug, check-design-rules | yes | green |
| 04-14-T1 | 14 | 3 | SIGN-02 | `*SignupTagsViewModelTest` | yes | green |
| 04-14-T2 | 14 | 3 | SIGN-02 | `*SignupTags*` + ktlintCheck, lintDevDebug, check-design-rules | yes | green |
| 04-15-T1 | 15 | 3 | SIGN-03, SIGN-02 | `*SignupVerifyEmailViewModelTest` | yes | green |
| 04-15-T2 | 15 | 3 | SIGN-03, SIGN-02 | `*SignupVerifyEmail*` + ktlintCheck, lintDevDebug, check-design-rules | yes | green |
| 04-16-T1 | 16 | 3 | SIGN-02 | `*SignupProfilePhotoViewModelTest` | yes | green |
| 04-16-T2 | 16 | 3 | SIGN-02 | `*SignupProfilePhoto*` + ktlintCheck, lintDevDebug, check-design-rules | yes | green |
| 04-17-T1 | 17 | 3 | SIGN-04 | `*SignupReviewViewModelTest` | yes | green |
| 04-17-T2 | 17 | 3 | SIGN-04 | `*SignupReview*` + ktlintCheck, lintDevDebug, check-design-rules | yes | green |
| 04-18-T1 | 18 | 3 | ONB-01, ONB-02 | `*HomeShellsTest` `*HomeClientViewModelTest` | yes | green |
| 04-18-T2 | 18 | 3 | ONB-01, ONB-02 | `*AppViewModelTest` `*HomeShellsTest` `*RootScreensTest` + ktlintCheck, lintDevDebug, assembleDevDebug, check-design-rules | yes | green |
| 04-19-T1 | 19 | 4 | SIGN-02, SIGN-04, SIGN-05 | `*SignupSetupNavigatorTest` | yes | green |
| 04-19-T2 | 19 | 4 | SIGN-02, SIGN-04, SIGN-05 | `*SignupSetupNav*` `*RootScreensTest` `*AppViewModelTest` + ktlintCheck, lintDevDebug, assembleDevDebug, check-design-rules | yes | green |

### Requirement-level map

| Requirement | Behavior | Test Type | Automated Command | File Exists | Status |
|-------------|----------|-----------|-------------------|-------------|--------|
| SIGN-01 | Password and email rules; silent until the first submit, then live | unit | `--tests '*AuthValidatorsTest' --tests '*SignupInitialViewModelTest'` | extend / ❌ W0 | ✅ green |
| SIGN-01 | 409 maps to the email field; 422 `errors[]` maps per field; any other error shows a dialog; success stores tokens and routes to `SignupSetup` | unit | `--tests '*SignupInitialViewModelTest' --tests '*UserManagerTest'` | ❌ W0 / extend | ✅ green |
| SIGN-01 | `/auth/signup` body is exactly `{email,password,role}`, with no bearer | MockWebServer | `--tests '*ClickMeApiTest'` | extend | ✅ green |
| SIGN-01 | Initial screen renders its fields; back is disabled while submitting | Robolectric Compose | `--tests '*SignupInitialScreenTest'` | ❌ W0 | ✅ green |
| SIGN-02 | Partial PATCH sends only the set keys (exact bodies, `{"expertise_tags":[]}`); empty-array rule (D-25, 3 cases via `listFieldFor` and the step saves) | MockWebServer + unit | `--tests '*ClickMeApiTest' --tests '*SignupSetupRepositoryTest'` (04-01, 04-07) | extend / ❌ W0 | ✅ green |
| SIGN-02 | `ExpertProfileData` decodes the full GET shape and old fixtures | unit | `--tests '*ResponseDecodingTest'` | extend | ✅ green |
| SIGN-02 | Repository hydrate and merge; draft cleared on logout | unit | `--tests '*SignupSetupRepositoryTest'` | ❌ W0 | ✅ green |
| SIGN-02 | Hub completion rules; Next always enabled | unit + Compose | `--tests '*SignupOverviewViewModelTest' --tests '*SignupOverviewScreenTest'` | ❌ W0 | ✅ green |
| SIGN-02 | Step VMs: save only filled fields, 600 ms Saved hold, 422 inline, tap order preserved | unit | `--tests '*SignupBasicInfoViewModelTest' --tests '*SignupTimezoneViewModelTest' --tests '*SignupTagsViewModelTest'` | ❌ W0 | ✅ green |
| SIGN-02 | Countries (50, alpha-3) and Timezones (10 + device zone) tables | unit | `--tests '*CountriesTest' --tests '*TimezonesTest'` | ❌ W0 | ✅ green |
| SIGN-02 | Crop math: min and max scale, clamp, centered `srcRect`, zoom about the centroid | unit (pure) | `--tests '*CropMathTest'` | ❌ W0 | ✅ green |
| SIGN-02 | Photo processing: EXIF rotation, downsampling, ~1024 square JPEG q85, cleanup | Robolectric | `--tests '*PhotoProcessorTest'` | ❌ W0 | ✅ green |
| SIGN-02 | Photo VM: pick, crop, upload multipart; failure keeps the crop; remove calls `DELETE` | unit | `--tests '*SignupProfilePhotoViewModelTest'` | ❌ W0 | ✅ green |
| SIGN-02 | Crop screen custom semantics actions; Cancel and Use | Robolectric Compose | `--tests '*PhotoCropScreenTest'` | ❌ W0 | ✅ green |
| SIGN-03 | No send on open; Send calls resend once; 30 s countdown in virtual time; leaving and re-entering the step keeps the cooldown (end time held in the singleton repository, reset on logout); the end time is restored after process death | unit | `--tests '*SignupVerifyEmailViewModelTest' --tests '*SignupSetupRepositoryTest'` (04-15, 04-07) | ❌ W0 | ✅ green |
| SIGN-03 | 429 `RATE_LIMITED` on send; 422 `INVALID_CODE` inline; 429 `TOO_MANY_REQUESTS` on verify inline; already-verified variant | unit | same | ❌ W0 | ✅ green |
| SIGN-04 | Publish gated on the D-03 items; "Still needed" list; province optional | unit + Compose | `--tests '*SignupReviewViewModelTest' --tests '*SignupReviewScreenTest'` | ❌ W0 | ✅ green |
| SIGN-04 | Publish payload; 422 top-level `missing_fields` mapped to step names; success sets mode Client and routes to ClientHome | unit + MockWebServer | same + `--tests '*ApiErrorParserTest' --tests '*SignupSetupRepositoryTest' --tests '*UserManagerTest' --tests '*AppViewModelTest'` | extend / ❌ W0 | ✅ green |
| SIGN-05 | Post-auth destination; launch resume; the foreground re-check routes `Home` to `SignupSetup` after a failed setup lookup, a failed `/me` launch, or the 5 s splash cap; client-only accounts make no `GET /expert/profile` | unit | `--tests '*UserManagerTest' --tests '*AppViewModelTest'` (04-06) | extend | ✅ green |
| SIGN-05 | Logout from the hub goes to Login and clears the draft | unit | `--tests '*AppViewModelTest' --tests '*SignupSetupRepositoryTest'` | extend / ❌ W0 | ✅ green |
| ONB-01 | Carousel shows once; Skip and Get Started call onFinish; Back on page 1 does not; the client-home gate shows it while the flag is false; marking it shown persists to `SessionStore` (survives relaunch); no flash on the cap path | unit + Compose | `--tests '*OnboardingScreenTest' --tests '*HomeShellsTest' --tests '*HomeClientViewModelTest' --tests '*UserManagerTest' --tests '*AppViewModelTest'` (04-08, 04-18, 04-06) | ❌ W0 / extend | ✅ green |
| ONB-02 | `switchMode` gate (D-17, D-23); `currentMode` collector ignores Login and Splash | unit | `--tests '*AppViewModelTest'` | extend | ✅ green |
| ONB-02 | Mode switch screen semantics; a mode switched to Expert is restored as `ExpertHome` by a fresh `UserManager` + `AppViewModel` over the same `SessionStore` (relaunch); the avatar opens the page | Compose + unit | `--tests '*ModeSwitchScreenTest' --tests '*AppViewModelTest' --tests '*HomeShellsTest'` (04-08, 04-06, 04-18) | ❌ W0 / extend | ✅ green |
| All | URL constants, bearer allow-list, `FakeClickMeApi` handlers | unit | `--tests '*ApiRequestUrlsTest'` | extend | ✅ green |
| All | Design rules and string hygiene | script | `bash scripts/check-design-rules.sh` + `ktlintCheck lintDevDebug lintProdDebug` | existing | ✅ green |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [x] `FakeClickMeApi` handlers and counters for the 9 new endpoints, added in the same change as the interface
- [x] An injectable clock seam for the verify countdown (reads `testScheduler.currentTime`)
- [x] `FakePhotoProcessor` plus an in-memory `SignupSetupRepository` fake, or a repository built over `FakeClickMeApi`
- [x] JSON string fixtures: full `GET /expert/profile` shape, signup response, publish 422 `missing_fields`, verify 429
- [x] Programmatic bitmaps for the photo tests (Robolectric native graphics); no binary fixtures
- [x] Every new test class in the map above

---

## Manual-Only Verifications

Deferred per project policy: log these as open items and do not block waves.

| Behavior | Requirement | Why Manual | Test Instructions | Status |
|----------|-------------|------------|-------------------|--------|
| Full signup with a real emailed code | SIGN-01..04 | Needs a real inbox and the backend | Samsung SM-A075F: sign up, complete every step, publish | deferred (non-blocking) |
| Crop gesture feel and spring-back | SIGN-02 | Tactile and visual | Pinch, pan and release on the crop screen | deferred (non-blocking) |
| Photo picker fallback on API 26–32 | SIGN-02 | No AVD available | Pick a photo on an API ≤32 device | deferred (non-blocking) |
| Carousel visuals and Back | ONB-01 | Visual | First client-home entry after login | deferred (non-blocking) |
| Mode persists across relaunch | ONB-02 | Process kill on a device | Switch mode, force-stop, relaunch | deferred (non-blocking) |
| Keyboard with `ModalBottomSheet` | SIGN-02 | IME behavior | Search in the language and tag pickers | deferred (non-blocking) |
| 200% font scale on new screens | All | Visual | System font size at max | deferred (non-blocking) |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency < 60s
- [x] Nyquist compliance flag set in frontmatter

**Approval:** approved 2026-10-21 (automated gate green; manual rows deferred, non-blocking)

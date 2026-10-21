---
phase: 4
slug: signup-onboarding-mode-switch
status: draft
nyquist_compliant: false
wave_0_complete: false
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

Filled in by the planner and executor as task IDs are assigned. Requirement-level map (from RESEARCH):

| Requirement | Behavior | Test Type | Automated Command | File Exists | Status |
|-------------|----------|-----------|-------------------|-------------|--------|
| SIGN-01 | Password and email rules; silent until the first submit, then live | unit | `--tests '*AuthValidatorsTest' --tests '*SignupInitialViewModelTest'` | extend / ❌ W0 | ⬜ pending |
| SIGN-01 | 409 maps to the email field; 422 `errors[]` maps per field; any other error shows a dialog; success stores tokens and routes to `SignupSetup` | unit | `--tests '*SignupInitialViewModelTest' --tests '*UserManagerTest'` | ❌ W0 / extend | ⬜ pending |
| SIGN-01 | `/auth/signup` body is exactly `{email,password,role}`, with no bearer | MockWebServer | `--tests '*ClickMeApiTest'` | extend | ⬜ pending |
| SIGN-01 | Initial screen renders its fields; back is disabled while submitting | Robolectric Compose | `--tests '*SignupInitialScreenTest'` | ❌ W0 | ⬜ pending |
| SIGN-02 | Partial PATCH sends only the set keys; empty-array rule (D-25, 3 cases) | MockWebServer + unit | `--tests '*SetupExpertProfileRequestTest' --tests '*ClickMeApiTest'` | ❌ W0 | ⬜ pending |
| SIGN-02 | `ExpertProfileData` decodes the full GET shape and old fixtures | unit | `--tests '*ResponseDecodingTest'` | extend | ⬜ pending |
| SIGN-02 | Repository hydrate and merge; draft cleared on logout | unit | `--tests '*SignupSetupRepositoryTest'` | ❌ W0 | ⬜ pending |
| SIGN-02 | Hub completion rules; Next always enabled | unit + Compose | `--tests '*SignupOverviewViewModelTest' --tests '*SignupOverviewScreenTest'` | ❌ W0 | ⬜ pending |
| SIGN-02 | Step VMs: save only filled fields, 600 ms Saved hold, 422 inline, tap order preserved | unit | `--tests '*SignupBasicInfoViewModelTest' --tests '*SignupTimezoneViewModelTest' --tests '*SignupTagsViewModelTest'` | ❌ W0 | ⬜ pending |
| SIGN-02 | Countries (50, alpha-3) and Timezones (10 + device zone) tables | unit | `--tests '*CountriesTest' --tests '*TimezonesTest'` | ❌ W0 | ⬜ pending |
| SIGN-02 | Crop math: min and max scale, clamp, centered `srcRect`, zoom about the centroid | unit (pure) | `--tests '*CropMathTest'` | ❌ W0 | ⬜ pending |
| SIGN-02 | Photo processing: EXIF rotation, downsampling, ~1024 square JPEG q85, cleanup | Robolectric | `--tests '*PhotoProcessorTest'` | ❌ W0 | ⬜ pending |
| SIGN-02 | Photo VM: pick, crop, upload multipart; failure keeps the crop; remove calls `DELETE` | unit | `--tests '*SignupProfilePhotoViewModelTest'` | ❌ W0 | ⬜ pending |
| SIGN-02 | Crop screen custom semantics actions; Cancel and Use | Robolectric Compose | `--tests '*PhotoCropScreenTest'` | ❌ W0 | ⬜ pending |
| SIGN-03 | No send on open; Send calls resend once; 30 s countdown in virtual time; the end time is restored | unit | `--tests '*SignupVerifyEmailViewModelTest'` | ❌ W0 | ⬜ pending |
| SIGN-03 | 429 `RATE_LIMITED` on send; 422 `INVALID_CODE` inline; 429 `TOO_MANY_REQUESTS` on verify inline; already-verified variant | unit | same | ❌ W0 | ⬜ pending |
| SIGN-04 | Publish gated on the D-03 items; "Still needed" list; province optional | unit + Compose | `--tests '*SignupReviewViewModelTest' --tests '*SignupReviewScreenTest'` | ❌ W0 | ⬜ pending |
| SIGN-04 | Publish payload; 422 top-level `missing_fields` mapped to step names; success sets mode Client | unit + MockWebServer | same + `--tests '*ApiErrorParserTest'` | extend / ❌ W0 | ⬜ pending |
| SIGN-05 | Post-auth destination; foreground re-check routes `Home` to `SignupSetup`; launch resume | unit | `--tests '*UserManagerTest' --tests '*AppViewModelTest'` | extend | ⬜ pending |
| SIGN-05 | Logout from the hub goes to Login and clears the draft | unit | `--tests '*AppViewModelTest' --tests '*SignupSetupRepositoryTest'` | extend / ❌ W0 | ⬜ pending |
| ONB-01 | Carousel shows once; Skip, Next and Get Started set the flag; Back on page 1 does not; the flag survives relaunch; no flash on the cap path | unit + Compose | `--tests '*OnboardingScreenTest' --tests '*OnboardingViewModelTest' --tests '*UserManagerTest' --tests '*AppViewModelTest'` | ❌ W0 / extend | ⬜ pending |
| ONB-02 | `switchMode` gate (D-17, D-23); `currentMode` collector ignores Login and Splash | unit | `--tests '*AppViewModelTest'` | extend | ⬜ pending |
| ONB-02 | Mode switch screen semantics; mode persists across a new `SessionStore`; the avatar opens the page | Compose + unit | `--tests '*ModeSwitchScreenTest' --tests '*SessionStoreTest' --tests '*HomeShellsTest'` | ❌ W0 / extend | ⬜ pending |
| All | URL constants, bearer allow-list, `FakeClickMeApi` handlers | unit | `--tests '*ApiRequestUrlsTest'` | extend | ⬜ pending |
| All | Design rules and string hygiene | script | `bash scripts/check-design-rules.sh` + `ktlintCheck lintDevDebug lintProdDebug` | existing | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `FakeClickMeApi` handlers and counters for the 9 new endpoints, added in the same change as the interface
- [ ] An injectable clock seam for the verify countdown (reads `testScheduler.currentTime`)
- [ ] `FakePhotoProcessor` plus an in-memory `SignupSetupRepository` fake, or a repository built over `FakeClickMeApi`
- [ ] JSON string fixtures: full `GET /expert/profile` shape, signup response, publish 422 `missing_fields`, verify 429
- [ ] Programmatic bitmaps for the photo tests (Robolectric native graphics); no binary fixtures
- [ ] Every new test class in the map above

---

## Manual-Only Verifications

Deferred per project policy: log these as open items and do not block waves.

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Full signup with a real emailed code | SIGN-01..04 | Needs a real inbox and the backend | Samsung SM-A075F: sign up, complete every step, publish |
| Crop gesture feel and spring-back | SIGN-02 | Tactile and visual | Pinch, pan and release on the crop screen |
| Photo picker fallback on API 26–32 | SIGN-02 | No AVD available | Pick a photo on an API ≤32 device |
| Carousel visuals and Back | ONB-01 | Visual | First client-home entry after login |
| Mode persists across relaunch | ONB-02 | Process kill on a device | Switch mode, force-stop, relaunch |
| Keyboard with `ModalBottomSheet` | SIGN-02 | IME behavior | Search in the language and tag pickers |
| 200% font scale on new screens | All | Visual | System font size at max |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 60s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending

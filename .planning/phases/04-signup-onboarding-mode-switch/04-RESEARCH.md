# Phase 4: Signup, Onboarding & Mode Switch - Research

**Researched:** 2026-10-20
**Domain:** Android (Jetpack Compose, Navigation 3, Hilt, Retrofit/Moshi) port of the iOS expert-signup flow, onboarding carousel and local mode switch. It runs against the existing ClickMe REST backend.
**Confidence:** MEDIUM-HIGH. Backend contracts and the existing Android code were read directly. The Compose and crop mechanics are standard, but nothing has been compiled or run here.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

Carried forward (not re-opened):
- **Field and error rules:** field validation timing, the always-enabled submit button with an in-flight spinner, and the server-error split between inline and dialog all follow Phase 3 D-01 to D-04 (Login).
- **Resume routing:** routing after auth, including resuming setup, goes through `UserManager.resolvePostAuthDestination`, which already returns `SignupSetup` (Phase 2 D-05).
- **Logout:** clears tokens, profile, `currentMode` and the onboarding flag (Phase 2 D-09).
- **Navigation:** Navigation 3 with back stacks owned by the app. The root `AppRoute` is app state (Phase 1 D-01).
- **Styling:** Material 3 adaptation; pixel parity with iOS is out of scope.

Signup Initial (SIGN-01)
- **D-01: Expert-only signup, matching iOS.**
  - `POST /auth/signup {email, password, role: "expert"}`. No role picker. The backend's `role: "client"` path is unused.
  - The response returns tokens, which are stored the way `login` stores them. The user is logged in immediately and the realtime manager picks up the session.
  - The back stack then becomes the hub, so the user can't go back and resubmit Initial.
- **D-02: The signup password follows the server rule.**
  - At least 8 characters, with at least one letter and at least one digit. Checked client-side and shown inline (iOS doesn't check the digit; this is a deliberate deviation).
  - The `PasswordStrengthMeter` shows once the field isn't empty, as on Set New Password.
  - Email uses the shared `AuthValidators` regex and messages.
  - A 409 shows on the email field, and a 422 maps each error to its field.

Publish rules (SIGN-02, SIGN-04)
- **D-03: Required to publish (follows the server's `onboarding.controller.js`, not iOS):** first name; city; country; timezone; at least 1 language; at least 1 expertise tag; **profile photo (`avatar_url`)** (iOS doesn't require it, but the server does). **Province/state is optional.** iOS requires it, but the server doesn't. **Verified email is NOT required to publish.** Verify Email is still one of the 5 hub steps and counts toward progress.
- **D-04: Steps save whatever is filled in (iOS parity).** Each step's Save sends only the filled fields through `PATCH /expert/profile/setup`, then returns to the hub. A step counts as complete only when its required fields are present. No per-step blocking validation.
- **D-05: Only Publish is gated.** The hub's "Next" is always enabled and opens Review (iOS parity). Review marks missing items. Publish stays disabled until every required item in D-03 is done, with a hint listing what's left. Publish re-sends the full payload with `setup_completed: true` and normalizes `country_code`, which iOS forgets to do.
- **D-06: If Publish still returns 422 `missing_fields`, name the steps.** Map each field to its hub step name (for example, "Add a profile picture" or "Add expertise") in the error dialog, and mark those steps incomplete on the hub.

Email verification and resume (SIGN-03, SIGN-05)
- **D-07: The code is sent only on tap.** "Send code" calls `POST /auth/email/resend`, never automatically. The 6-digit code field appears after a successful send. The button is disabled for a 30-second countdown. Verify calls `POST /auth/email/verify {code}`. A 422 `INVALID_CODE` shows inline.
- **D-08: A 429 `RATE_LIMITED` on Send counts as "already sent".** Show the code field and restart the 30-second countdown. No error dialog.
- **D-09: Track the real verified state (an iOS bug fixed).** Seed it from `email_verified` in the signup response. Refresh it with `GET /auth/email/status` when the hub opens or setup resumes. A verified user always sees the step checked.
- **D-10: Resume always opens the Overview hub, matching iOS.** Pre-fill it from `GET /expert/profile` (the full shape: name, location, timezone, languages, tags, avatar) plus the email status. Completed steps show as checked. `ExpertProfileData` must be expanded beyond `setup_completed`.
- **D-11: If the setup check fails, fall through and check again (iOS parity, in line with Phase 2 D-01).** If `GET /expert/profile` fails during the splash or login check (network or 5xx), route to home as iOS does. Re-check setup status on the next app foreground, and route to the hub if it's incomplete.

Step details (SIGN-02)
- **D-12: Profile Photo has a real crop UI, a deliberate improvement over iOS** (whose zoom slider does nothing). Flow: `PickVisualMedia` -> a full-screen crop screen (Cancel / Use) -> back to the Photo step with the cropped preview -> Save uploads. The user can re-crop or replace from the preview. The crop is **custom Compose code with no new dependency**: pinch-zoom and pan the image under a fixed **circular guide**. Output is a **square** JPEG at about 1024 px and 0.85 quality. Upload is multipart field `image` to `POST /user/profile/avatar`, reading `data.avatar_url`. Remove calls `DELETE /user/profile/avatar`.
- **D-13: Timezone uses the iOS curated list of 10 zones plus auto-detect.** If the device zone isn't in the list, add it at the top, so the default selection is always visible. Saves `{timezone}`.
- **D-14: Primary tag and language follow tap order (an iOS bug fixed).** Send `languages` and `expertise_tags` in the order the user selected them, so the first pick becomes primary on the server (iOS sends them alphabetically). A small helper line explains that the first one is primary. No explicit primary-picker UI.
- **D-15: Tags need at least 1, with no maximum and no custom tags.** The catalog comes from `GET /meta/expertise-tags`, with search. The iOS `CustomTagSection` is unused there and stays out of scope.
- **D-16: Basic Info, ported from iOS:** first name, city, province/state (optional), country, and languages with English pre-selected. The country picker is the **iOS static 50-country list (alpha-3)**, searchable. Languages come from `GET /meta/languages`, with search and multi-select.

Mode switch and onboarding (ONB-01, ONB-02)
- **D-17: Switching to expert routes by setup status (a deviation from iOS, which has no gate).** If expert setup is incomplete, switching to expert opens the setup hub instead of expert home. Switching to client is always allowed. Mode is local only, with no server call. It persists through `SessionStore`/`UserManager.setMode`. `AppViewModel` must react to `currentMode` changes. Today there's only `debugSwitchScaffold`.
- **D-18: The mode switch is a full-screen page with a `TopAppBar` back arrow** (the user chose this over a bottom sheet). Two cards: Client ("Find and book experts") and Expert ("Manage bookings and profile"). The current mode is marked. Tapping a card switches mode and closes the page.
- **D-19: The card illustrations are bundled drawables.** iOS loads them from remote URLs (`ModeSwitchView.swift` `imageURL`). Export those two images once into `res/drawable`, so nothing flashes on load and the screen works offline.
- **D-20: The entry point this phase is the top-bar avatar** on client and expert home, which is a no-op today. The profile-hub card comes with Phase 15.
- **D-21: The onboarding carousel shows once per login (iOS parity, ONB-01).** It's a full-screen 4-page carousel on entering client home while `hasSeenOnboarding` is false. It ports the iOS pages: "Welcome to ClickMe", "Discover Top Experts", "Book & Schedule Instantly", "Seamless Communication". Skip appears on every page, with Next, and "Get Started" on the last page. Swiping moves between pages. Skip and Get Started both mark it shown. Logout clears the flag (already done in Phase 2).
- **D-22: After publishing, set mode to client and open client home, where the carousel appears (iOS parity).**

### Claude's Discretion
- Hub layout details: rows with check states, a done/5 progress bar, a tips card and a bottom "Next" bar, adapted to Material 3.
- The Review screen layout: cards with Edit buttons, plus a Timezone card. iOS omits timezone; adding it is fine.
- How the signup screens fit into navigation: Initial in the auth back stack; the hub and its steps in a back stack under `AppRoute.SignupSetup`. The accumulator/shared state can be a setup-scoped ViewModel or repository.
- The in-memory signup accumulator design (port `SignupAccumulator.swift`).
- How the foreground re-check (D-11) hooks in. Reuse `ProcessForeground` from Phase 3.
- Crop math, gesture limits (minimum zoom fills the circle), and the downscale implementation.
- Carousel illustration assets: copy them from the iOS asset catalog.

### Deferred Ideas (OUT OF SCOPE)
- **Client-role signup** (`role: "client"`): the backend supports it, but it's new capability beyond iOS.
- **Custom expertise tags:** the iOS `CustomTagSection` exists but is unused.
- **Explicit primary tag and language picker:** tap order covers this for now (D-14).
- **Change-email from Verify Email:** iOS only goes back.
- **Mode-switch card on the profile hubs:** Phase 15, profile and settings.
- **A maximum tag count:** not enforced by the server.
- Also out of scope (CONTEXT domain): a notification-permission prompt (Phase 13); the profile-hub mode-switch card (Phase 15).
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| SIGN-01 | Create an account on the signup Initial step, no username or phone | `POST /auth/signup` contract (body `{email,password,role}`, 200 body with tokens and `email_verified`, 409 `EMAIL_TAKEN`, 422 `errors[]`). `signup()` on `UserManager` mirrors `login()`. Public path is already allow-listed. |
| SIGN-02 | Hub steps in any order, each saving through `PATCH /expert/profile/setup` | Verified PATCH schema (strict, all optional, alpha-3, IANA), partial-save semantics, array-replace semantics, `GET /expert/profile` shape for hydration, avatar upload/delete endpoints, meta endpoints. |
| SIGN-03 | Code sent only on tap, 30-second resend cooldown | `/auth/email/resend` (fixed 30s limiter, 429 `RATE_LIMITED`), `/auth/email/verify` (5 per 15 min limiter, 429 `TOO_MANY_REQUESTS`), `/auth/email/status`. Deploy status verified live. |
| SIGN-04 | Review and publish | Server publish gate in `onboarding.controller.js`, `missing_fields` 422 shape (top-level, not in `errors[]`), required-field list. |
| SIGN-05 | Unfinished signup resumes at login or splash, completed steps kept | `resolvePostAuthDestination` already routes to `SignupSetup`; hydration from `GET /expert/profile` + email status; the D-11 foreground re-check hook. |
| ONB-01 | Carousel on first launch into client home, not again until logout | `hasSeenOnboarding` already persisted and cleared on logout; overlay on `AppRoute.ClientHome`; asset export plan. |
| ONB-02 | Switch client/expert on the mode switch screen, local only, persisted | `UserManager.setMode` / `SessionStore`; `AppViewModel` must follow `currentMode`; setup gating (D-17). |
</phase_requirements>

## Summary

Phase 4 is almost entirely client-side work against backend contracts that are already live in production. The production `/health` commit (`f686d27`) equals the local backend checkout HEAD, and both the "send code on request" change (`261017-gx9`) and the "username removed" change (`261017-g2d`) are earlier commits. The "NOT YET DEPLOYED" banners in `backend-reference/docs/ios/*.md` are therefore stale. [VERIFIED: `curl https://clickme-backend-production.up.railway.app/health` and `git log` in `backend-reference`]

The existing Android scaffolding covers most of what the phase touches. `UserManager.resolvePostAuthDestination` already returns `SignupSetup`. `AppRoute.SignupSetup`, `AuthNavHost`, `SessionStore` (mode and onboarding flag), `ValidatedField`, `PasswordStrengthMeter`, `AsyncSaveButton` and `ProcessForeground` are all in place. The new work is:
1. A network and DTO layer: 10 endpoints plus request and response models.
2. A shared `SignupSetupRepository` (the accumulator).
3. About 9 new screens.
4. A custom crop composable.
5. Wiring `AppViewModel` to modes and foreground re-checks.
6. Asset export.

The research found several places where CONTEXT, UI-SPEC and the real backend disagree or leave a gap. The planner must handle these:
- **Language order is not persisted by the server.** `user_languages` has no primary or order column. Only tags get `is_primary` (first element of the array). The UI-SPEC helper line "The first language you pick is your primary language." is therefore inaccurate.
- **`PATCH` with `[]` clears the list.** The server deletes then re-inserts. D-04 ("send only filled fields") needs a rule for "the user emptied tags or languages".
- **Verify has its own 429.** The verify endpoint's limiter returns `429 TOO_MANY_REQUESTS` (5 attempts per 15 min per user). It is not covered by D-07/D-08.
- **`missing_fields` is top-level.** The publish 422 body carries it outside `errors[]`. Today's `ApiErrorEnvelope` and `ApiException.Http` have no field for it.
- **Three small shared-component changes beyond the UI-SPEC list:**
  - `AuthErrorDialog` has a hard-coded title.
  - `AuthTopAppBar` has a fixed container color and a mandatory back arrow.
  - `ExpertProfileData(setupCompleted = ...)` is used by name in about 10 existing tests, so every new field must have a default.

**Primary recommendation:** Build bottom-up in this order:
1. Network layer, DTOs and fakes.
2. `UserManager` and `AppViewModel` changes.
3. Shared components.
4. A `@Singleton SignupSetupRepository` (draft state, hydrate, per-step saves, publish).
5. Screens, with the crop math as pure, unit-tested functions.

Use no new dependencies. The one new binary is the six image assets, which need a non-WebP fallback because no WebP encoder is installed.

## Project Constraints (from CLAUDE.md)

- Treat `ios-reference/` Swift code as the source of truth for behavior and business logic. Deviate only for Android platform conventions.
- Stack is fixed: Jetpack Compose + Material 3, MVVM with Compose state, Kotlin coroutines (no RxJava), Hilt, Retrofit + Moshi, EncryptedSharedPreferences for JWT, Firebase Messaging, Agora, Stripe. minSdk 26, targetSdk 37, compileSdk 37.
- Platform swaps apply: `.sheet` becomes `ModalBottomSheet`; back chevron becomes `TopAppBar` with back arrow; `.alert` becomes Material `AlertDialog`; iOS switch becomes Material `Switch`; `PhotosPicker` becomes `rememberLauncherForActivityResult(PickVisualMedia)`; `.task` becomes `LaunchedEffect(Unit)`.
- Mirror the iOS folder tree. Example: `screens/signup/initial/SignupInitialScreen.kt` + `SignupInitialViewModel.kt` under `app/src/main/java/com/uptrendinvestments/clickmeapp/`.
- Backend: same REST contract as iOS. `backend-reference/docs/ios/` holds the authoritative specs.
- Worktree executors have no `ios-reference` or `backend-reference` symlinks and no `local.properties`. Use the absolute paths `/Users/LeonChen/Documents/ClickMe2026/` and `/Users/LeonChen/Documents/ClickMe2026 Backend/` (MEMORY: build-env-gotchas).
- Gradle needs JDK 25: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"` (MEMORY: build-env-gotchas).
- Manual and device checks are deferred. Log human checkpoints as open items and do not block a wave (MEMORY: manual-testing-deferred).
- Project CI rules (`scripts/check-design-rules.sh`): no `Color(0x...)` literals outside `ui/theme/Color.kt`, no `*Brand` types, no Stripe secret keys. UI-SPEC adds: no `fontSize`, strings only in per-feature `strings_*.xml`.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Signup, token issue, email code issue and verify, rate limits | API / Backend | Client | Server owns credentials, codes (SHA-256 hashed), and limits. The client only mirrors the password rule for UX. |
| Publish gate (`setup_completed`) | API / Backend | Client (UX gate) | The server recomputes `missing_fields` on every PATCH. The client disables Publish for UX but must handle the 422. |
| Setup draft state (accumulator) | Client (app-scoped repository) | API (hydrate source) | In-memory draft seeded from `GET /expert/profile`. The server is the source of truth for what is saved. |
| Photo pick, crop, JPEG re-encode | Client | API (upload and storage) | Pure on-device work, then multipart upload. |
| Mode (client/expert) | Client (`SessionStore`) | API (setup-status lookup) | Local only by decision. The only server touch is the setup-status read for the D-17 gate. |
| Onboarding-seen flag | Client (`SessionStore`) | — | Local only. Logout clears it. |
| Root routing (`AppRoute`) | Client (`AppViewModel`) | `UserManager` | App state, not a nav destination (Phase 1 D-01). |
| Country, timezone, tag and language catalogs | Client constants (countries, timezones) / API (tags, languages) | — | iOS parity: static 50 countries and 10 zones. Tags (about 560 rows) and languages come from `/meta/*`. |

## Standard Stack

No new libraries are required. [VERIFIED: `gradle/libs.versions.toml` and `app/build.gradle.kts` read in this session]

### Core (already in the project)
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Compose BOM | 2026.09.00 | UI (`ModalBottomSheet`, `HorizontalPager`, `PullToRefreshBox`, pointer input) | Locked stack |
| Navigation 3 (`navigation3-runtime/ui`) | 1.2.0 | Per-flow back stacks (`NavDisplay`, `NavKey`) | Phase 1 decision; `AuthNavHost` is the model |
| `lifecycle-viewmodel-navigation3` | 2.11.0 | Entry-scoped ViewModels | Used by Phase 3 |
| Hilt + `hilt-lifecycle-viewmodel-compose` | 2.60.1 / 1.4.0 | DI, `hiltViewModel()` | Locked stack |
| Retrofit + Moshi codegen | 3.0.0 / 1.15.2 | REST and multipart | Locked stack; every DTO needs `@JsonClass(generateAdapter = true)` and an explicit `@Json(name=)` on multi-word fields (R8-safe, reflection-free) |
| OkHttp (`MultipartBody`) | 5.5.0 | Avatar upload part | Already a dependency |
| Coil 3 | 3.6.3 | Remote avatar display (already used by `Avatar`) | Already used |
| androidx.activity-compose | 1.13.0 | `PickVisualMedia` (needs 1.7.0+) | [CITED: developer.android.com/training/data-storage/shared/photo-picker] |
| DataStore Preferences | 1.2.1 | Mode and onboarding flag (already in `SessionStore`) | Already used |
| JUnit 4.13.2, Robolectric 4.17, coroutines-test 1.11.0, mockwebserver3 5.5.0, Compose UI test | — | Test stack | Phase 2 and 3 pattern |

### Supporting (platform, no dependency)
| API | Purpose | When to Use |
|-----|---------|-------------|
| `android.graphics.BitmapFactory` (`inSampleSize`) + `android.media.ExifInterface(InputStream)` (API 24+) | Downsampled decode with EXIF rotation, no new dependency | Photo working copy. minSdk 26 > 24, so the platform class is enough; no `androidx.exifinterface` needed. [CITED: developer.android.com/reference/android/media/ExifInterface, constructor added in API 24; the page was truncated so treat as MEDIUM] |
| `java.util.Locale` (`Locale.Builder().setRegion(r)`, `getISO3Country()`, `getDisplayCountry`) | Alpha-2 to alpha-3 and country names from the 50 alpha-2 codes | Static `Countries` list. No hand-written alpha-3 table needed. [ASSUMED: JDK behavior from training; planner should unit-test all 50 codes against iOS `alpha2ToAlpha3`] |
| `java.time.ZoneId` / `ZoneRules` | Device zone id and current offset for the `(GMT±hh:mm) {City}` label | Timezone step. API 26 has `java.time`. |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Hand-rolled crop (locked, D-12) | uCrop or android-image-cropper | Rejected by D-12 (no new dependency). Not researched further. |
| Platform `ExifInterface` | `androidx.exifinterface` | Only needed for minSdk below 24. Not needed here. |
| `@Singleton` repository for draft state | Activity-scoped ViewModel | Activity-scoped VM outlives logout within the same Activity and would leak the previous user's draft. See Pitfall 5. |

**Installation:** none. **Version verification:** versions above are read from the repo's own `libs.versions.toml`, not from a registry, because no package is being added.

## Package Legitimacy Audit

No external packages are installed in this phase. slopcheck was not run because there is nothing to check. Every library listed is already in `gradle/libs.versions.toml`.

**Packages removed due to slopcheck [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none

## Architecture Patterns

### System Architecture Diagram

```
                                  +---------------------------------------------+
  AppViewModel (route, mode)      |  ClickMeApp: AnimatedContent(route) fade    |
   |  route: Splash/Login/        |  (fade-through 90 out / 150 in)             |
   |  SignupSetup/ClientHome/     +--+---------+---------------+--------------+--+
   |  ExpertHome                     |         |               |              |
   |                                 v         v               v              v
   |                           AuthNavHost  SignupSetupNavHost ClientHome   ExpertHome
   |                           (Login root)  (Hub root)         (+ carousel  (shell)
   |                              |   |         |                overlay)      |
   |                      SignUpKey --> SignupInitialScreen          |            |
   |                              | POST /auth/signup                |            |
   |                              v                                  |            |
   |             UserManager.signup(): save tokens, isLoggedIn=true   |            |
   |             -> onLoggedIn(LaunchDestination.SignupSetup)        |            |
   |                              |  (whole auth stack replaced)     |            |
   |                              v                                  |            |
   |                     Hub (5 rows) --- Next ---> Review ---Publish--+            |
   |      +---------+-----------+---------+----------+      PATCH {full, setup_completed:true}
   |      v         v           v         v          v                |            |
   |  Personal  Timezone    Photo -> Crop  Verify   Tags              |            |
   |      \         |           |           |        /                 |            |
   |       +--------+-----+-----+-----------+-------+                 |            |
   |                      v                                           |            |
   |        SignupSetupRepository (@Singleton, draft StateFlow)       |            |
   |          hydrate(): GET /expert/profile + GET /auth/email/status |            |
   |          saveX(): PATCH /expert/profile/setup (filled fields)    |            |
   |          photo: POST|DELETE /user/profile/avatar                 |            |
   |          email: POST /auth/email/resend|verify                   |            |
   |          reset() on logout / publish                             |            |
   |                                                                  |            |
   |  Publish success: userManager.setMode(Client) + refreshProfile() |            |
   |                   -> onLoggedIn(Home(Client)) -> carousel -------+            |
   |                                                                               |
   |  Mode switch page (pushed on Explore/Dashboard stack, bottom bar hidden) <----+
   |     card tap -> AppViewModel.switchMode(mode):                                 |
   |        Client            -> setMode, route ClientHome                          |
   |        Expert & setup ok -> setMode, route ExpertHome                          |
   |        Expert & setup NOT ok -> route SignupSetup (mode not persisted)         |
   |  MainActivity STARTED -> AppViewModel.onAppForegrounded():                     |
   |        refreshIfStale() + recheck pending expert setup (D-11)                  |
```

### Recommended Project Structure
```
app/src/main/java/com/uptrendinvestments/clickmeapp/
├── constants/            Countries.kt (50 alpha-2 -> Locale-derived), Timezones.kt (10 zones + device)
├── model/requests/       SignupRequest, SetupExpertProfileRequest(+Location), VerifyEmailCodeRequest
├── model/responses/      SignupData, ExpertProfileData (expanded), SetupExpertProfileData,
│                         EmailStatusData, AvatarUploadData, LanguagesPayload, ExpertiseTagsPayload
├── network/              ApiRequestUrls (+consts), ClickMeApi (+10 endpoints), ApiErrorEnvelope/ApiException (+missingFields)
├── managers/             UserManager (+signup, +setup recheck, +setup status), SignupSetupRepository (new)
├── navigation/           AuthKeys (SignUpKey stays), SignupSetupKeys, SignupSetupNavHost, ModeSwitch key
├── components/           PickerField, PickerSheet, SelectableChip, Avatar (+160dp)
├── screens/signup/
│   ├── initial/          SignupInitialScreen + ViewModel
│   ├── overview/         SignupOverviewScreen + ViewModel (hub)
│   ├── basicinfo/        SignupBasicInfoScreen + ViewModel (+ language/country sheets)
│   ├── timezone/         SignupTimezoneScreen + ViewModel
│   ├── profilephoto/     SignupProfilePhotoScreen + VM, PhotoCropScreen, CropMath.kt, PhotoProcessor.kt
│   ├── verifyemail/      SignupVerifyEmailScreen + ViewModel
│   ├── tags/             SignupTagsScreen + ViewModel
│   └── review/           SignupReviewScreen + ViewModel
├── screens/onboarding/   OnboardingScreen + ViewModel (4 pages)
├── screens/settings/modeswitching/   ModeSwitchScreen + ViewModel
└── res/drawable-nodpi/   mode_client/expert, onboarding_* images
```
This mirrors the iOS tree per CLAUDE.md. New string files: `strings_signup_*.xml`, `strings_onboarding.xml`, `strings_mode_switch.xml`.

### Pattern 1: Stateless content composable + thin Hilt screen (established)
**What:** Each screen is `XScreen(viewModel = hiltViewModel())` that collects state and calls `XContent(state, callbacks)`. `XContent` is Hilt-free so Robolectric `createComposeRule` tests can drive it. [VERIFIED: `LoginScreen.kt` / `LoginScreenTest.kt`]
**When to use:** Every new screen.

### Pattern 2: Entry-scoped ViewModel with a `start(...)` call, not assisted injection (established)
**What:** NavKey carries only non-secret scalars. The VM gets them via `start(x)` from a `LaunchedEffect`. [VERIFIED: `ForgotPasswordViewModel.start`, `AuthNavHost` `entry<ForgotPasswordKey>`] Never place a password, code or token in a key or `SavedStateHandle`.

### Pattern 3: One nav host per root route
**What:** `SignupSetupNavHost` copies `AuthNavHost`: `rememberNavigationState(HubKey, setOf(HubKey))`, a `Navigator`, `entryProvider<NavKey> { entry<...> }`, and `NavDisplay` with the shared `slideTransition`. Keys are `@Serializable data object/class : NavKey`. At the hub (stack root) `NavDisplay` has one entry, so system Back exits the app. This is the UI-SPEC behavior. [VERIFIED: `AuthNavHost.kt`, `Navigator.kt`, `NavTransitions.kt`]

Keys: `HubKey`, `PersonalDetailsKey`, `TimezoneKey`, `ProfilePhotoKey`, `PhotoCropKey`, `VerifyEmailKey`, `TagsKey`, `ReviewKey`. Review's Edit buttons push the same step keys. Step Save pops one entry, which lands on Review or the hub. No extra bookkeeping is needed.

### Pattern 4: `SignupSetupRepository` (the accumulator), `@Singleton`
**What:** An app-scoped holder. Its state is `StateFlow<SignupDraft>` with: `email`, `emailVerified`, `firstName`, `city`, `provinceState`, `countryCode` (alpha-3), `languages: List<LanguageItem>`, `timezone`, `avatarUrl`, `expertiseTags: List<TagItem>`, plus a hydration status. Its methods:
- `hydrate()`: `async` both `GET /expert/profile` and `GET /auth/email/status`, then merge.
- `saveBasicInfo(...)`, `saveTimezone(...)`, `saveTags(...)`, `uploadAvatar(bytes)`, `removeAvatar()`: one PATCH or upload each. The draft is updated only on success.
- `sendCode()`, `verifyCode(code)`.
- `publish()`: builds the full payload with `setup_completed = true`.
- `reset()`: called when `userManager.isLoggedIn` becomes false and after a successful publish.

**Why a singleton, not a ViewModel:** step VMs are entry-scoped (Hilt per `NavEntry`), so they cannot share state. An Activity-scoped VM would survive logout. See Pitfall 5.

**Draft semantics (recommended):** the draft holds *last saved or known server state*. Each step screen keeps its own local form state and writes to the draft only after a successful Save. This makes `hydrate()` safe to call whenever the hub opens (no unsaved local edits to clobber), and it matches D-04 ("steps save only on Save", UI-SPEC "unsaved edits are discarded without a prompt").

**Never store the password in the draft.** iOS stores `password` on `SignupAccumulator`; do not port that field. It is not needed after `signup()` returns.

### Pattern 5: Root route follows mode and gates the expert switch
**What:**
- `AppViewModel` gets `fun switchMode(target: AppMode)`.
  - Client: `userManager.setMode(Client)`, route `ClientHome`.
  - Expert: ask `UserManager` whether setup is incomplete. If yes, route `SignupSetup` and do not persist Expert. If no, or the lookup failed (iOS "failed counts as complete"), `setMode(Expert)` and route `ExpertHome`.
- Also add a collector on `userManager.currentMode` that moves the route between `ClientHome` and `ExpertHome` only while the current route is one of those two. It must be guarded, because `resolvePostAuthDestination` and `logout` call `setMode` or reset the mode while the route is `Login` or `Splash`. This mirrors iOS `onChange(of: currentMode)`. [VERIFIED: `ClickMe2026App.swift`] The collector is the safety net for later callers (the Phase 15 profile-hub card, D-22).
- Keep the `AppViewModel(userManager)` constructor unchanged. `AppViewModelTest`'s `Harness` constructs it positionally.
- Persist `Expert` only when the user actually reaches expert home. Otherwise a stored `Expert` plus an incomplete profile makes the next relaunch go to `SignupSetup` through `resolvePostAuthDestination` anyway, so the stored value would be misleading.

### Pattern 6: D-11 foreground re-check
**What:** `resolvePostAuthDestination` currently swallows a failed `GET /expert/profile` and returns `false` (complete). Change `isExpertProfileIncomplete()` to also record `setupCheckPending = true` when it failed for an expert, and clear it on success or logout. Add `UserManager.recheckSetupIfPending(): LaunchDestination?` that returns `SignupSetup` when the retry finds `setup_completed == false`. Hook it into the existing `AppViewModel.onAppForegrounded()`, which `MainActivity` already calls on every `STARTED` via `repeatOnLifecycle`. When it returns `SignupSetup` while the route is `ClientHome` or `ExpertHome`, set the route to `SignupSetup`.

**Recommendation on CONTEXT's "reuse `ProcessForeground`":** `onAppForegrounded` is already wired to the same lifecycle signal and already drives `refreshIfStale()`. `ProcessForeground` is the realtime manager's seam. Using `onAppForegrounded` is simpler and fully covered by the existing `AppViewModelTest` pattern. This is within Claude's Discretion. [VERIFIED: `MainActivity.kt`, `AppViewModel.kt`]

### Pattern 7: Crop as pure math plus a thin composable
**What:** Keep all geometry in a pure object (`CropMath`) so it is JVM-unit-testable. The composable only feeds pointer events into it.

```kotlin
// Canvas coordinates; C = canvas center; R = guide radius (px); img = bitmap size (px)
fun minScale(imgW: Int, imgH: Int, r: Float) = 2f * r / minOf(imgW, imgH)       // covers the guide
fun maxScale(minScale: Float) = minScale * 5f                                   // UI-SPEC: 5x
// Zoom about a centroid, then pan. s = image px -> canvas px.
fun applyGesture(s: Float, off: Offset, centroidFromC: Offset, zoom: Float, pan: Offset, min: Float, max: Float):
    Pair<Float, Offset> {
    val s2 = (s * zoom).coerceIn(min, max); val z = s2 / s
    return s2 to (centroidFromC - (centroidFromC - off) * z + pan)              // keeps content under the centroid
}
fun clampOffset(off: Offset, s: Float, imgW: Int, imgH: Int, r: Float) =        // guide stays inside the image
    Offset(off.x.coerceIn(-maxOf(0f, imgW * s / 2 - r), maxOf(0f, imgW * s / 2 - r)),
           off.y.coerceIn(-maxOf(0f, imgH * s / 2 - r), maxOf(0f, imgH * s / 2 - r)))
// Source rect in image pixels for the square inside the guide:
fun srcRect(s: Float, off: Offset, imgW: Int, imgH: Int, r: Float): RectF {
    val size = 2f * r / s
    val left = imgW / 2f + (-r - off.x) / s
    val top  = imgH / 2f + (-r - off.y) / s
    return RectF(left, top, left + size, top + size)
}
```
[ASSUMED: derivation from standard affine geometry in this session, not copied from a library. Unit tests must pin it: min scale covers the guide, clamp keeps the guide inside the image, `srcRect` at scale `min` and zero offset is the centered square of side `min(imgW, imgH)`.]

Gesture host: use `awaitEachGesture { awaitFirstDown(); do { val e = awaitPointerEvent(); zoom/pan/centroid via calculateZoom/calculatePan/calculateCentroid; update raw (unclamped) state } while (e.changes.any { it.pressed }); animate to clamp (200ms FastOutSlowIn) }`. `detectTransformGestures` has no end-of-gesture callback, so the UI-SPEC "spring back on release" needs this form. Add a second `pointerInput` with `detectTapGestures(onDoubleTap)` toggling min and 2.5x of min. Semantics: `customActions` for Zoom in/out and Move left/right/up/down, each step 10% of the guide.

### Pattern 8: Photo working copy in app-private cache
**What:** On pick, immediately decode (downsampled, EXIF-rotated) and write a working JPEG to `cacheDir/signup/photo_src.jpg`. The crop screen and the "Adjust Crop" button read this file, not the picker URI. `PhotoCropKey` carries no URI. Delete the working and cropped files on Save success, Remove, logout and `reset()`.

**Why:**
- Picker URIs are only guaranteed until the app stops or the device restarts. [CITED: developer.android.com/training/data-storage/shared/photo-picker]
- After process death the nav stack is restored, so a URI-based crop screen would fail.
- Raw camera originals can be 12 MP or more.

Decode with `BitmapFactory.Options.inJustDecodeBounds` to compute `inSampleSize` for a longest side of at most 2048, read `ExifInterface(InputStream).getAttributeInt(TAG_ORIENTATION, ...)`, apply a rotate `Matrix`, and write the copy. The final crop draws `srcRect` of the working bitmap onto a 1024x1024 `Bitmap` via `Canvas.drawBitmap(src, srcRect, dstRect, paint)` with `Paint(FILTER_BITMAP_FLAG)`, then `compress(JPEG, 85)`. Re-encoding drops EXIF including GPS (privacy benefit).

Put this in a `PhotoProcessor` class behind an interface (`@IoDispatcher`, `@ApplicationContext`) so the ViewModel is testable with a fake.

### Anti-Patterns to Avoid
- **Eagerly seeding English into the hub-level draft.** See Pitfall 6. Pre-select English inside the Personal Details form only.
- **Letting `hydrate()` overwrite a user's unsaved edits.** Solved by the draft semantics above.
- **Keeping the draft in an Activity-scoped ViewModel.** It leaks across logout.
- **Hard-coding a dialog title in `AuthErrorDialog`** when seven different titles are needed.
- **Sending `null` or empty values for "unfilled".** Rely on Moshi omitting nulls, and read the array rule in Pitfall 2.
- **Navigating on `isLoggedIn` flips inside step screens.** Only `AppViewModel` changes the route.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Alpha-2 to alpha-3 and country names | A 50-row hand table | `Locale.Builder().setRegion(code).build()` with `isO3Country` and `getDisplayCountry` over the 50 alpha-2 codes from iOS `alpha2ToAlpha3` | Removes transcription error risk. Unit test all 50 against the iOS table. |
| Multipart upload body | Manual boundary building | Retrofit `@Multipart` + `MultipartBody.Part.createFormData("image", "avatar.jpg", bytes.toRequestBody("image/jpeg".toMediaType()))` | multer's `fileFilter` keys on the part's Content-Type, so a missing or wrong type yields `req.file` undefined and a 422. |
| Error-body parsing | New parsers | Existing `ApiErrorParser` / `ApiException.Http`, extended with `missingFields` | Already lenient to HTML and unknown codes. |
| Password strength | A new meter | Existing `PasswordStrength` + `PasswordStrengthMeter` | Reuse (D-02). |
| Email regex, 6-digit sanitizer | New validators | `AuthValidators.EMAIL_REGEX`, `sanitizeCode`, `resetCode` | Shared messages (D-02). |
| Save button states | A new button | `AsyncSaveButton` + `SaveState` | Existing 600 ms hold pattern from Phase 3. |
| Pager | A custom pager | `HorizontalPager` + `PagerState` | Standard. |
| Pull-to-refresh | Custom gesture | M3 `PullToRefreshBox` (may need `@OptIn(ExperimentalMaterial3Api::class)`) | [ASSUMED: the page fetched did not confirm its stability in this BOM. An unneeded opt-in is harmless.] |
| Avatar display | A new image composable | `Avatar` (Coil) plus a local `Bitmap` overload or a `File` model | Coil's `AsyncImage` accepts a `File` or `Bitmap` data object. |
| Country and tag search | A search engine | `filter { it.contains(query, ignoreCase = true) }` | iOS does the same. About 560 tags is trivial for a `LazyColumn`. |

**Key insight:** the only genuinely custom code in this phase is the crop geometry and the `awaitEachGesture` host. Everything else is composition of existing parts, so risk concentrates in (a) contract edge cases and (b) the crop.

## Backend Contracts (verified against `backend-reference` source)

### Auth and email
| Call | Request | Success | Errors the client must handle |
|------|---------|---------|-------------------------------|
| `POST /auth/signup` (public) | `{email, password, role:"expert"}`. A stray `username` key is stripped. | 200 `{status, data:{token, refresh_token, expires_at, expires_in, user:{id, first_name:"", last_name:"", roles:["client","expert"], avatar_url:null}, email_verified:false}}` | 409 `EMAIL_TAKEN` message "An account with that email already exists."; 422 `VALIDATION_ERROR` with `errors[{field,message}]` (messages: "Invalid email format.", "Password must be at least 8 characters.", "Password must contain at least one letter.", "Password must contain at least one digit.", "Role must be..."). The server lowercases and trims the email. |
| `POST /auth/email/resend` (authed, no body) | — | 200 `{status, message:"Verification email sent."}`. Also returned, with no email sent, if already verified. | 429 `RATE_LIMITED` (fixed 30 s window per user); 401 token codes. |
| `POST /auth/email/verify` (authed) | `{code:"123456"}` | 200 `{status:"success"}` (also if already verified) | 422 `INVALID_CODE` "Invalid or expired code. Please request a new one." (wrong, expired, used, malformed, or 5 or more attempts on that code). **429 `TOO_MANY_REQUESTS` "Too many attempts. Please try again in 15 minutes." (5 per 15 min per user)** |
| `GET /auth/email/status` (authed) | — | 200 `{status, data:{verified:bool}}` | — |

[VERIFIED: `backend-reference/src/controllers/auth.controller.js`, `src/middleware/rateLimiter.js`, `src/routes/auth.js`, `src/schemas/auth.schema.js`]

Notes:
- The signup response `user` has no `email`, and the top-level `email_verified` is always `false` at signup. Keep the email the user typed (trimmed). On resume, take it from `GET /expert/profile` -> `personal_info.email`.
- The signup refresh token (minted via magic-link `verifyOtp` on an account immediately re-set to unconfirmed) was live-tested and honored by `/auth/refresh` (backend STATE row `260802-mb1`, "T-mb1-05 RESOLVED FAVORABLY"). The refresh-after-signup path is safe. [CITED: `backend-reference/.planning/STATE.md` row 260802-mb1]
- Define a separate `SignupData` DTO (not `LoginData`): same fields plus `@Json(name="email_verified") val emailVerified: Boolean = false`.

### Expert profile setup
`PATCH /expert/profile/setup` [VERIFIED: `onboarding.schema.js`, `onboarding.controller.js`, `routes/onboarding.js`]:
- Body schema is `.strict()`: unknown keys are rejected with 422. Allowed keys: `profile_image_url` (URL), `first_name` (1..100), `location{city, province_state, country_code}` (alpha-3, `^[A-Z]{3}$`), `timezone` (IANA, validated by luxon), `languages: string[]`, `expertise_tags: string[]`, `setup_completed: bool`. Do **not** send `profile_image_url`. The avatar upload endpoint already persists `profiles.avatar_url`.
- Only keys present are written. `languages` and `expertise_tags`, when present, are **deleted then re-inserted (full replace)**. `[]` clears them. Tag order is stored as `is_primary = (index == 0)`. **Language rows have no primary or order column**; array order is not persisted.
- `ensureExpert` runs first. A client-only account is converted to expert on its first touch of any `/expert/*` route (including `GET /expert/profile`). This is permanent and is by design (backend EXP-01).
- Response 200 `{status, data:{profile_id, completion_percentage, missing_fields:[...], next_recommended_step, setup_completed}}`.
- Publish 422 (when `setup_completed:true` and anything is missing): `{status:"error", code:"VALIDATION_ERROR", message:"Profile is incomplete; ...", missing_fields:[...]}`. **`missing_fields` is a top-level key, not `errors[]`.** Other fields in the same request remain persisted. Values: `profile_image_url`, `first_name`, `location` (city **or** country_code missing), `timezone`, `languages`, `expertise_tags`.
- `setup_completed:false` is ignored. The flag is one-way.
- Server publish requirements (matches D-03): avatar, first name, city and country_code, timezone, 1 or more languages, 1 or more tags. State and province are optional.

`GET /expert/profile` [VERIFIED: `expert.controller.js`]: `data:{profile_id, avatar_url, personal_info{first_name,last_name,email,bio}, professional_details{job_title,company,education,meeting_url}, location_details{city,state_province,country_code,timezone}, languages:[{id (nullable),label}], expertise_tags:[{id,label,category_id,is_primary}], setup_completed}`.
- `expertise_tags` come back primary-first, then alphabetical. Only the first tag's position survives a round trip.
- `languages` come back in unspecified DB order.
- `state_province` (GET) versus `province_state` (PATCH): different key names.

### Media and meta
- `POST /user/profile/avatar` [VERIFIED: `userProfile.js`, `userProfile.controller.js`]: multipart field `image`, jpeg, png or webp, max 5 MB, `uploadLimiter` (20 per 15 min, 429 `TOO_MANY_REQUESTS`). It persists `profiles.avatar_url` and returns `data:{avatar_url}`.
- `DELETE /user/profile/avatar` [VERIFIED]: idempotent 200 `{status:"success"}`, no body data.
- `GET /meta/languages` [VERIFIED]: `data:{languages:[{id,label}]}` (ids like `"en"`, ordered by label).
- `GET /meta/expertise-tags` [VERIFIED]: `data:{tags:[{id (uuid string),label,category_id}]}` (up to 1000; about 560 rows). Optional `q` and `category_id` query params exist. Filter client-side, as iOS does.

### Error shape support needed
`ApiErrorEnvelope` and `ApiException.Http` need `missingFields: List<String>`. The envelope already ignores unknown keys, so today `missing_fields` is silently dropped. Add `@Json(name = "missing_fields") val missingFields: List<String>? = null` (default null) and a `missingFields: List<String> = emptyList()` last parameter on `Http`, so existing constructor calls still compile. `ErrorCode` already has `EmailTaken`, `InvalidCode`, `RateLimited`, `TooManyRequests`, `ValidationError`.

## Runtime State Inventory

Not a rename or migration phase. Omitted. (There is, however, new persisted state: the only new persisted values are the existing `SessionStore` keys `current_mode` and `has_seen_onboarding`. Nothing new is stored on disk beyond the temporary photo working files in `cacheDir`, which must be deleted on Save, Remove, logout and publish.)

## Common Pitfalls

### Pitfall 1: Language order is not persisted, so the "first language is primary" copy is wrong
**What goes wrong:** D-14 and the UI-SPEC promise that the first language picked becomes primary. `user_languages` has `(id, user_id, language_id, custom_label)` and no order or primary column, and the controller inserts rows without one. `GET` returns them in unspecified order. [VERIFIED: `supabase/migrations/20260603060000_create_junctions.sql`, `onboarding.controller.js`]
**Why it happens:** The iOS comment "First element is treated as the primary language by the server" is not backed by the backend code.
**How to avoid:** Keep sending languages in tap order (harmless), but surface this to the user. Recommended: drop the language helper line "The first language you pick is your primary language." and keep the tag one ("The first tag is your primary tag." is true). Do not rely on language order after hydration.
**Warning signs:** After a resume, the language chip order differs from what the user picked.

### Pitfall 2: `[]` is destructive, and "send only filled fields" has a hole
**What goes wrong:** If the user deselects all tags (or languages) and Save omits the key, the server keeps the old rows. The hub then says "incomplete" locally while the server still has tags. If Save sends `[]`, the server clears them. iOS avoids this by refusing to save an empty tag selection.
**How to avoid (recommended rule):**
- Send the array when non-empty.
- Send `[]` only when the user removed everything from a list that was **non-empty in the draft** (i.e. last known server state). This keeps server and client aligned.
- Otherwise omit the key.
- Add a ViewModel unit test for the three cases (non-empty, emptied from non-empty, empty from empty).
Flag this to the user as a refinement of D-04. UI-SPEC says "Save with zero tags is allowed; the step just stays incomplete".

### Pitfall 3: The verify endpoint has an unspecified 429, and wrong codes burn the quota
**What goes wrong:** `verifyEmailLimiter` allows 5 requests per 15 minutes per user. Typing wrong codes locks the user out, returning 429 `TOO_MANY_REQUESTS` with a server message, which is not an `INVALID_CODE`. The RPC also invalidates a code after 5 failed attempts.
**How to avoid:** On Verify, map 422 `INVALID_CODE` to the inline error and 429 `TOO_MANY_REQUESTS` to an inline error using the server message (fallback "Too many attempts. Please try again in 15 minutes."). Do not auto-retry. Do not show the generic dialog for a 429. Add the strings and a test. This is a gap in CONTEXT and UI-SPEC. Raise it as a planner-added decision.

### Pitfall 4: The publish 422 carries `missing_fields` at the top level
**What goes wrong:** Parsing only `errors[]` (as the Login 422 does) yields no field list, and D-06's step-naming dialog has nothing to show.
**How to avoid:** Add `missingFields` to the envelope and `ApiException.Http` as above. Map values to hub step names per UI-SPEC (`profile_image_url` -> "Add profile picture"; `first_name`, `location`, `languages` -> "Personal details"; `timezone` -> "Confirm timezone"; `expertise_tags` -> "Add expertise"; collapse duplicates). Mark those hub rows incomplete by recording the failed keys in the draft until the next hydrate. Add a `ResponseDecodingTest` fixture using the exact body above.

### Pitfall 5: Draft leakage and stale draft after logout or process death
**What goes wrong:**
(a) An Activity-scoped ViewModel keeps the previous user's email, name and photo after logout, then shows it to the next signup.
(b) Nav 3 restores the `SignupSetup` back stack after process death (`rememberNavBackStack` saves it), possibly on Review or a step, but a singleton repository is empty at that point.
**How to avoid:**
- Use the `@Singleton` repository and clear it on `isLoggedIn == false` and after publish. Collect `userManager.isLoggedIn` in the repository's init on `@ApplicationScope`. The dependency is one-way (repository -> UserManager).
- Make `SignupSetupNavHost` call `repository.ensureHydrated()` on first composition (idempotent per process and per session). Step screens read the draft and tolerate the brief empty state. Review shows "Required" until hydrate completes. Consider showing the hub's 4 dp indeterminate bar while hydrating.
- Persisting the draft to disk is **not** recommended. Unsaved edits are explicitly discarded by the UI-SPEC, and PII on disk adds risk.

### Pitfall 6: English "pre-selected" can mask a missing server language
**What goes wrong:** iOS seeds `languages = [English]` into the accumulator. With Android's hub completion rule ("Personal details = first name, city, country, 1 or more languages"), a resumed user with first name, city and country saved but languages never saved would show Personal Details as complete while the server has zero languages, and Publish would fail with `languages` missing.
**How to avoid:** Keep the draft's `languages` empty by default. Pre-select English **inside the Personal Details form's initial local state** when the draft list is empty. It is then saved on Save like any other chip. D-16 is satisfied (English is pre-selected), and the hub, Review and Publish never rely on an unsaved default. The device timezone default is the same idea but is fine: Publish re-sends the full payload including timezone, so the server always receives it.

### Pitfall 7: Provisioning side effect of the setup-status check (D-17)
**What goes wrong:** `GET /expert/profile` goes through `ensureExpert`. For a client-only account, calling it (for the D-17 gate) converts the account to expert on the server permanently (creates `expert_profiles`, updates `app_metadata` and `profiles.roles`). [VERIFIED: `routes/expert.js`, `middleware/ensureExpert.js`]
**Why this is probably fine:** The Mode Switch to Expert is exactly the "become an expert" path, and iOS reaches expert endpoints on expert home anyway. But it is a real side effect of what looks like a read.
**How to avoid / note:** Do not call the check for Client switches. Surface this in the plan as an accepted consequence. Also note that the JWT's roles stay stale until the next refresh (backend T-19-05). `resolvePostAuthDestination` honors the stored mode first, so a relaunch does not misroute.

### Pitfall 8: Existing tests and fakes break unless the DTO and interface changes are staged
**What goes wrong:**
- `ExpertProfileData(setupCompleted = x)` is constructed by name in `AppViewModelTest`, `UserManagerTest`, `ResponseDecodingTest`, and others. Required new fields break compilation.
- `FakeClickMeApi` implements `ClickMeApi`. Every new interface method must be added to the fake in the same change.
- `HomeClientShell` / `HomeExpertShell` are constructed with named parameters in `HomeShellsTest`.
**How to avoid:** Give every new DTO field a default. Add each endpoint to `ClickMeApi`, `ApiRequestUrls`, `FakeClickMeApi` (handler plus call counter) and a URL-constant test in one plan. Add new shell parameters with defaults.

### Pitfall 9: Countdown correctness (rotation, process death, tests)
**What goes wrong:** Storing the remaining seconds drifts or resets. A `delay`-loop ticker under `runTest` works only if time comes from the test scheduler.
**How to avoid:** Persist the **end time** (a monotonic timestamp from an injected `nowMillis: () -> Long`, default `SystemClock.elapsedRealtime()`) in `SavedStateHandle`. Derive remaining seconds from a 1 s ticker flow. In tests, inject `{ testScheduler.currentTime }` and `advanceTimeBy(30_000)`. The server window is fixed 30 s from the request, so a client countdown started at response time never ends early. On a 429, restart the full 30 s (D-08). On resend, clear the typed code, since a new code invalidates the old one. Announce "You can resend the code" once at zero.

### Pitfall 10: `Avatar`, `AuthErrorDialog` and `AuthTopAppBar` do not yet support what the UI-SPEC needs
**What goes wrong:**
- `Avatar`'s KDoc lists allowed sizes 32/40/56/96 (160 is a documented addition in UI-SPEC).
- `AuthErrorDialog` always uses the title "Something went wrong" (`auth_dialog_title`). The UI-SPEC needs "Couldn't create account", "Couldn't save", "Couldn't upload photo", "Couldn't send code", "Couldn't publish profile".
- `AuthTopAppBar` has a fixed `surface` container and always shows a back arrow. Initial needs a transparent container, and the hub needs no navigation icon plus a `MoreVert` action.
**How to avoid:** Plan these as small backward-compatible changes:
- `AuthErrorDialog(message, onDismiss, title: String = default)`.
- `AuthTopAppBar(..., containerColor = surface, actions = {})` with a hub variant (or a nullable `onBack`).
- `Avatar`: allow a local `Bitmap` or `File` model (Coil accepts both).
Existing `AuthStaticScreensTest` and Login tests must stay green.

### Pitfall 11: Carousel flag race and Back semantics
**What goes wrong:**
(a) `hasSeenOnboarding` defaults to `false` until `loadPersistedState` runs inside `restore()`. If the route resolved first (splash cap, `offlineDestination()`), the carousel could flash for a returning user.
(b) A `Dialog` or `Popup` overlay would swallow system Back, so page 1 could not leave the app.
**How to avoid:**
(a) `loadPersistedState()` is the first statement of `restoreInternal()` and the route is set only after `restore` returns or the 5 s cap fires. The cap path is the only exposure. Reduce it by having `offlineDestination()` also `await` the persisted flag (or call `loadPersistedState()` if not loaded yet). Add a test for the cap path.
(b) Render the carousel as a normal composable (not `Dialog`) and swap it with client home using `AnimatedContent` / `Crossfade`. This also keeps TalkBack from reading the shell underneath. Use `BackHandler(enabled = currentPage > 0)` to go back one page.

### Pitfall 12: `PickVisualMedia` URI lifetime and process death
**What goes wrong:** The picker URI's grant lasts until the app stops or the device restarts. A restored crop screen can no longer read it. [CITED: developer.android.com/training/data-storage/shared/photo-picker]
**How to avoid:** Copy to the cache working file at pick time (Pattern 8). Do not store the URI in a nav key or saved state.

### Pitfall 13: `ModalBottomSheet` plus search field and the keyboard
**What goes wrong:** The search field sits at the top, but a tall list plus the IME can leave the footer or rows hidden.
**How to avoid:** Use `rememberModalBottomSheetState(skipPartiallyExpanded = true)` and `Modifier.imePadding()` on the sheet content. Check at 200% font scale. [ASSUMED: standard technique from training; verify visually in the deferred manual pass.]

### Pitfall 14: Timezone id validity
**What goes wrong:** `ZoneId.systemDefault().id` can be a legacy or unusual id, and the server validates with luxon `IANAZone.isValidZone`. A rejection shows as an inline 422 on Save.
**How to avoid:** Use the device id if it is in `ZoneId.getAvailableZoneIds()`. Otherwise fall back to the nearest bundled zone by current offset, then UTC (this is the iOS `Timezones.detected()` logic). D-13 says "add the device zone at the top if not in the list". Both can hold: add it only when it is a valid java.time zone id. [ASSUMED: ICU/Intl accepts the same ids as java.time; legacy aliases like `Asia/Calcutta` should be valid.]

## Code Examples

### Retrofit endpoints to add (mirror of Phase 2/3 style)
```kotlin
// Source: pattern from existing ClickMeApi.kt; paths verified against backend routes
@POST(ApiRequestUrls.SIGNUP) suspend fun signup(@Body body: SignupRequest): SuccessDataResponse<SignupData>
@POST(ApiRequestUrls.EMAIL_RESEND) suspend fun resendVerificationEmail(): SuccessMessageResponse
@POST(ApiRequestUrls.EMAIL_VERIFY) suspend fun verifyEmailCode(@Body body: VerifyEmailCodeRequest): SuccessMessageResponse
@GET(ApiRequestUrls.EMAIL_STATUS) suspend fun getEmailStatus(): SuccessDataResponse<EmailStatusData>
@PATCH(ApiRequestUrls.EXPERT_PROFILE_SETUP) suspend fun setupExpertProfile(@Body body: SetupExpertProfileRequest): SuccessDataResponse<SetupExpertProfileData>
@GET(ApiRequestUrls.META_LANGUAGES) suspend fun getLanguages(): SuccessDataResponse<LanguagesPayload>
@GET(ApiRequestUrls.META_EXPERTISE_TAGS) suspend fun getExpertiseTags(): SuccessDataResponse<ExpertiseTagsPayload>
@Multipart @POST(ApiRequestUrls.USER_AVATAR) suspend fun uploadAvatar(@Part image: MultipartBody.Part): SuccessDataResponse<AvatarUploadData>
@DELETE(ApiRequestUrls.USER_AVATAR) suspend fun deleteAvatar(): SuccessMessageResponse
// existing: getExpertProfile() now returns the full ExpertProfileData
```
Constants: `auth/signup` (already in `publicPaths`), `auth/email/resend`, `auth/email/verify`, `auth/email/status`, `expert/profile/setup`, `meta/languages`, `meta/expertise-tags`, `user/profile/avatar`. Add a path-constant assertion and a `needAuthToken` assertion for each (`ApiRequestUrlsTest` already has the email paths in its "authed" list).

### Request DTOs (null-omitting partial PATCH)
```kotlin
@JsonClass(generateAdapter = true)
data class SetupExpertProfileRequest(
    @Json(name = "first_name") val firstName: String? = null,
    val location: SetupLocation? = null,
    val timezone: String? = null,
    val languages: List<String>? = null,
    @Json(name = "expertise_tags") val expertiseTags: List<String>? = null,
    @Json(name = "setup_completed") val setupCompleted: Boolean? = null,
)
@JsonClass(generateAdapter = true)
data class SetupLocation(
    val city: String? = null,
    @Json(name = "province_state") val provinceState: String? = null,
    @Json(name = "country_code") val countryCode: String? = null,
)
```
`MoshiConverterFactory.create(moshi)` does not enable `serializeNulls`, so null fields are omitted. Pin this with a `MockWebServer` test asserting the recorded request body has only the keys that were set. The backend's `.strict()` schema makes a stray key a 422. [VERIFIED: `NetworkFactory.retrofit` builds the converter without `.serializeNulls()`; ASSUMED: Moshi's default omits nulls, from training; the MockWebServer test settles it.]

### Multipart avatar part
```kotlin
// Source: OkHttp 5 API; field name and MIME verified against multer config in routes/userProfile.js
val part = MultipartBody.Part.createFormData(
    name = "image",
    filename = "avatar.jpg",
    body = jpegBytes.toRequestBody("image/jpeg".toMediaType()),
)
```

### PickVisualMedia launcher (Compose)
```kotlin
// Source: https://developer.android.com/training/data-storage/shared/photo-picker
val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
    if (uri != null) viewModel.onPicked(uri)   // VM copies to cache working file on IO
}
pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
```
No storage permission and no manifest change is needed.

### Mode-aware route (sketch)
```kotlin
// AppViewModel
fun switchMode(target: AppMode) {
    viewModelScope.launch {
        when (userManager.requestModeSwitch(target)) {          // new UserManager method
            is LaunchDestination.SignupSetup -> _route.value = AppRoute.SignupSetup
            is LaunchDestination.Home -> _route.value = if (target == AppMode.Expert) AppRoute.ExpertHome else AppRoute.ClientHome
            LaunchDestination.Login -> Unit
        }
    }
}
```
`requestModeSwitch` returns `Home(target)` after `setMode` for Client, and for Expert returns `SignupSetup` (without persisting) when the setup lookup says incomplete, otherwise `setMode(Expert)` and `Home(Expert)`.

### Publish payload
```kotlin
SetupExpertProfileRequest(
    firstName = d.firstName.trim(),
    location = SetupLocation(d.city.trim(), d.provinceState.trim().ifEmpty { null }, Countries.normalize(d.countryCode)),
    timezone = d.timezone,
    languages = d.languages.map { it.id },        // tap order
    expertiseTags = d.expertiseTags.map { it.id }, // tap order; first is primary
    setupCompleted = true,
)
```
`Countries.normalize` ports iOS `Countries.normalize`: trim, uppercase, alpha-3 passthrough if in the list or a valid `^[A-Z]{3}$`, alpha-2 mapped, else null.

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Storage permissions for image pick | `PickVisualMedia` system photo picker (no permission) | Android 13 and AndroidX backport | No manifest change; fallback is `ACTION_OPEN_DOCUMENT` on devices without the picker. [CITED: developer.android.com/training/data-storage/shared/photo-picker] |
| `androidx.exifinterface` for old APIs | Platform `ExifInterface(InputStream)` from API 24 | API 24 | minSdk 26 needs no extra dependency. |
| iOS: verification code emailed at signup | Backend: no code at signup; request via `/auth/email/resend` | 2026-10-17 (`261017-gx9`) | Android never auto-sends; deployed. [VERIFIED: prod `/health` commit equals local HEAD, which contains the commit] |
| iOS: username at signup/login | Removed | 2026-10-17 (`261017-g2d`) | Email only; deployed. |

**Deprecated/outdated:**
- The "DEPLOY STATUS: NOT YET DEPLOYED" banners in `EMAIL-VERIFICATION-ON-REQUEST.md`, `SIGNUP-USERNAME-OPTIONAL.md`, `PHONE-NUMBER-REMOVED.md`. They are stale. Production reports `f686d27` (2026-10-18 HEAD).
- iOS `Countries`/`Timezones` Foundation lookups: reimplement from lists, not from `Foundation`.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | `Locale.Builder().setRegion(code).build().isO3Country` gives the same alpha-3 for all 50 iOS codes (e.g. `TW`->`TWN`, `HK`->`HKG`) | Don't Hand-Roll | A wrong code makes the server 422 on `country_code`. Mitigation: unit test all 50 against the iOS `alpha2ToAlpha3` map in the same task. |
| A2 | Moshi omits null properties by default with the project's converter, so the partial PATCH sends only set keys | Code Examples | The strict schema would 422 on a `null`-valued key. The MockWebServer body-assertion test settles it before any screen depends on it. |
| A3 | `PullToRefreshBox` may still need `@OptIn(ExperimentalMaterial3Api::class)` in BOM 2026.09.00 | Don't Hand-Roll | Compile warning or error only. Add the opt-in. |
| A4 | Android timezone ids from `ZoneId.systemDefault()` are accepted by luxon `IANAZone.isValidZone` | Pitfall 14 | Rare 422 on timezone Save. Fallback to nearest bundled zone. |
| A5 | The crop transform math in Pattern 7 is correct as written | Pattern 7 | Visually wrong crops. The unit tests on `CropMath` are the safeguard; the device crop feel check is deferred. |
| A6 | `ModalBottomSheet` + search + IME needs `imePadding` and `skipPartiallyExpanded = true` | Pitfall 13 | Hidden footer on small screens; checked in the deferred manual pass. |
| A7 | A Coil `AsyncImage` can take a `File` or `Bitmap` as `model` for the local crop preview | Don't Hand-Roll | Use `Image(bitmap.asImageBitmap())` instead. Trivial. |
| A8 | Recommendation to drop the languages "first is primary" helper line and to use the Pitfall 2 empty-array rule are product decisions the user has not confirmed | Pitfalls 1 and 2 | The user may prefer to keep the UI-SPEC copy. They need to confirm, since D-14 and D-04 are locked. |
| A9 | A client-only account becoming expert via the D-17 setup-status read is acceptable product behavior | Pitfall 7 | If not, gate on roles first (a client-only account would route to the hub directly without calling the endpoint). |

## Open Questions

1. **Language "primary" copy (D-14 versus server reality).**
   - What we know: tags honor order (`is_primary` = index 0). Languages persist no order. [VERIFIED: backend source]
   - What's unclear: whether to keep the approved helper line anyway.
   - Recommendation: drop the language line, keep the tag line, and note the deviation in the plan. Ask the user to confirm (A8).

2. **Empty-array saves (D-04 versus the destructive `[]`).**
   - What we know: the server treats `[]` as clear and omission as untouched.
   - What's unclear: whether the user wants "emptied list clears the server list".
   - Recommendation: the rule in Pitfall 2 (send `[]` only when emptying a previously non-empty list), with a test.

3. **Verify-email 429 UX.**
   - What we know: 5 attempts per 15 min per user; wrong codes count.
   - What's unclear: the copy and whether the Verify button should lock.
   - Recommendation: inline server message under the code field; no extra lock UI; add the string. Treat as planner-added.

4. **Shared-component changes beyond UI-SPEC (`AuthErrorDialog` title, `AuthTopAppBar` container and actions, `Avatar` model).**
   - Recommendation: include as an early small plan, backward compatible, with the existing component tests kept green.

5. **Image asset format.** UI-SPEC specifies WebP, but no `cwebp`, ImageMagick or Pillow is installed here. Only `sips` (JPEG/PNG output) is available.
   - Recommendation: use JPEG via `sips` for the six images (all source images have no alpha channel), or use PNG if the user insists on lossless. WebP can be produced in Android Studio (Convert to WebP) as a manual step. Planner decision.

6. **Does the hub need `PullToRefreshBox` plus a Snackbar plus a progress bar** (UI-SPEC)? It is specified and straightforward. It requires a `SnackbarHost` in the hub `Scaffold`. No question, but it adds a hub ViewModel state machine (idle, loading, failed).

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| JDK 25 (Android Studio JBR) | Gradle build and tests | yes | `/Applications/Android Studio.app/Contents/jbr/Contents/Home` | System `java` is 18 and will not work. Always set `JAVA_HOME` as above. |
| Android SDK + emulator image | Builds; Robolectric needs none | yes | `~/Library/Android/sdk` | — |
| `adb` / attached device | Deferred manual checks | no (not on PATH; device is Samsung SM-A075F, API 36) | — | Manual checks are deferred by project policy. No API 26 AVD exists. |
| Backend (production Railway) | Contract verification, deferred device runs | yes | commit `f686d27` live | — |
| `sips` | Resize/convert image assets | yes (`/usr/bin/sips`) | macOS | — |
| `cwebp`, ImageMagick, Pillow | WebP export | no | — | JPEG via `sips`, or Android Studio "Convert to WebP" manually |
| `ctx7` CLI / Context7 MCP | Doc lookup | no | — | Used developer.android.com via WebFetch. Some pages truncated (flagged above). |
| slopcheck | Package audit | no | — | Not needed (no new packages) |
| The two remote mode-switch images | D-19 export | yes today (HTTP 200, 512x512 PNG, no alpha, about 265 KB each) | — | These are Stitch-hosted `lh3.googleusercontent.com` URLs that may expire. **Export them early in the phase.** |
| iOS carousel PNGs | D-21 assets | yes | `Assets.xcassets/{welcome,discover,booking,communication}.imageset/*.png`, 1448x1086, 1.5-1.7 MB each, no alpha | Downscale to at most 1080 px wide with `sips -Z 1080 -s format jpeg -s formatOptions 85` |

**Missing dependencies with no fallback:** none.
**Missing dependencies with fallback:** WebP encoder (use JPEG via `sips`); `adb` (manual checks deferred).

The mode-switch image URLs are the only time-sensitive item. If a fetch fails later, the fallback is to ask the user for the images, or to design a simple vector placeholder, which would be a D-19 deviation.

## Validation Architecture

(`workflow.nyquist_validation` is absent from `.planning/config.json`, so this section is required.)

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 4.13.2 + Robolectric 4.17 + kotlinx-coroutines-test 1.11.0 + mockwebserver3 5.5.0 + Compose UI test (`createComposeRule` under `RobolectricTestRunner`) |
| Config file | `app/src/test/resources/robolectric.properties`; `unitTests.isIncludeAndroidResources = true` in `app/build.gradle.kts` |
| Quick run command | `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDevDebugUnitTest --tests '<pattern>'` |
| Full suite command | `./gradlew ktlintCheck lintDevDebug lintProdDebug testDevDebugUnitTest testProdDebugUnitTest assembleDevDebug assembleProdDebug assembleProdRelease` plus `bash scripts/check-design-rules.sh` |

Existing baseline: 421 tests per flavor, green at end of Phase 3. [CITED: `.planning/STATE.md`]

### Phase Requirements to Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| SIGN-01 | Password rule (8+, letter, digit) and messages; email rules; silent until first submit then live | unit | `--tests '*AuthValidatorsTest'` (extend) `--tests '*SignupInitialViewModelTest'` | extend / Wave 0 |
| SIGN-01 | 409 -> email field; 422 `errors[]` -> per-field; other -> dialog titled "Couldn't create account"; success stores tokens, sets logged-in and yields `SignupSetup` | unit (fake API) | `--tests '*SignupInitialViewModelTest' --tests '*UserManagerTest'` | Wave 0 / extend |
| SIGN-01 | Initial renders fields, hint, strength meter after typing, Continue; back disabled while submitting | Robolectric Compose | `--tests '*SignupInitialScreenTest'` | Wave 0 |
| SIGN-01 | `/auth/signup` request body is exactly `{email,password,role}` and sends no bearer | MockWebServer | `--tests '*ClickMeApiTest'` (extend) | extend |
| SIGN-02 | Partial PATCH serializes only set keys (`location` nested partial); `languages`/`expertise_tags` empty-array rule (3 cases) | MockWebServer + unit | `--tests '*SetupExpertProfileRequestTest' --tests '*ClickMeApiTest'` | Wave 0 |
| SIGN-02 | `ExpertProfileData` decodes the full GET shape (nullable language id, `state_province`, `is_primary`) and old `setup_completed`-only fixtures | unit | `--tests '*ResponseDecodingTest'` (extend) | extend |
| SIGN-02 | Hydrate merges server state; server null timezone keeps device default; draft empty by default for languages; draft cleared on logout | unit | `--tests '*SignupSetupRepositoryTest'` | Wave 0 |
| SIGN-02 | Hub completion rules (5 rows, done/5, verified from real state) and Next always enabled | unit + Compose | `--tests '*SignupOverviewViewModelTest' --tests '*SignupOverviewScreenTest'` | Wave 0 |
| SIGN-02 | Personal Details, Timezone, Tags: save only filled fields, Saved hold 600 ms then pop (virtual time), 422 inline, others dialog titled "Couldn't save" | unit | `--tests '*SignupBasicInfoViewModelTest' --tests '*SignupTimezoneViewModelTest' --tests '*SignupTagsViewModelTest'` | Wave 0 |
| SIGN-02 | Tags/languages preserve tap order in the request; primary is first | unit | same VM tests | Wave 0 |
| SIGN-02 | Countries: 50 entries, alpha-3 for all match iOS table, normalize cases; Timezones: 10 ids, device zone added at top, offset fallback | unit | `--tests '*CountriesTest' --tests '*TimezonesTest'` | Wave 0 |
| SIGN-02 | Crop math: min scale covers the guide, max is 5x, clamp keeps guide inside image, `srcRect` at min scale is the centered square, zoom about centroid keeps the point fixed | unit (pure) | `--tests '*CropMathTest'` | Wave 0 |
| SIGN-02 | Photo processing: rotates by EXIF, downsamples with `inSampleSize`, output is square about 1024 JPEG decodable at q85, working files cleaned on remove, save and logout | Robolectric | `--tests '*PhotoProcessorTest'` | Wave 0 |
| SIGN-02 | Photo VM: pick -> crop -> use -> upload multipart `image` with `image/jpeg`, failure keeps the local crop, remove-confirm calls `DELETE`, removing an unsaved pick makes no call | unit | `--tests '*SignupProfilePhotoViewModelTest'` | Wave 0 |
| SIGN-02 | Crop screen semantics custom actions (Zoom/Move) change state; Cancel/Use buttons | Robolectric Compose | `--tests '*PhotoCropScreenTest'` | Wave 0 |
| SIGN-03 | No network call on screen open (zero `resend` calls); Send calls resend once; code field appears on success | unit | `--tests '*SignupVerifyEmailViewModelTest'` | Wave 0 |
| SIGN-03 | 30 s countdown with virtual time; button disabled during; "Resend code" at 0; end time (not remaining) is saved and restored | unit | same | Wave 0 |
| SIGN-03 | 429 `RATE_LIMITED` on send shows code field and restarts countdown, no dialog; 422 `INVALID_CODE` inline; 429 `TOO_MANY_REQUESTS` on verify inline; non-6-digit client error; already verified shows Done variant | unit | same | Wave 0 |
| SIGN-04 | Publish disabled until all D-03 items present; "Still needed" lists hub step names; province optional; verified email not required | unit + Compose | `--tests '*SignupReviewViewModelTest' --tests '*SignupReviewScreenTest'` | Wave 0 |
| SIGN-04 | Publish payload: full fields, `setup_completed:true`, normalized `country_code`, tap order; 422 `missing_fields` decoded from top-level and mapped to step names with duplicates collapsed; success sets mode Client and refreshes the profile | unit + MockWebServer | same + `--tests '*ApiErrorParserTest'` (extend) | extend / Wave 0 |
| SIGN-05 | `resolvePostAuthDestination` returns `SignupSetup` for incomplete expert (existing); failed lookup records pending; foreground re-check routes `Home` -> `SignupSetup` when incomplete and clears the flag; no re-check for clients | unit | `--tests '*UserManagerTest' --tests '*AppViewModelTest'` (extend) | extend |
| SIGN-05 | Launch with incomplete expert lands on `SignupSetup`; logout from the hub goes to Login and clears the draft | unit | `--tests '*AppViewModelTest' --tests '*SignupSetupRepositoryTest'` | extend / Wave 0 |
| ONB-01 | Carousel shows only on `ClientHome` while the flag is false; Skip on every page, Next advances, Get Started marks the flag; Back on page 1 does not mark it; flag survives relaunch; logout clears | unit + Compose | `--tests '*OnboardingScreenTest' --tests '*OnboardingViewModelTest' --tests '*UserManagerTest'` (extend) | Wave 0 / extend |
| ONB-01 | Cap-path launch does not flash the carousel for a returning user | unit | `--tests '*AppViewModelTest'` (extend) | extend |
| ONB-02 | `switchMode`: client always allowed; expert with complete setup goes to `ExpertHome` and persists; expert with incomplete setup goes to `SignupSetup` and does not persist; lookup failure treated as complete; `currentMode` collector ignores Login/Splash routes | unit | `--tests '*AppViewModelTest'` (extend) | extend |
| ONB-02 | Mode switch screen: radio-group semantics, "Active" on the current card, tap switches and pops, active-card tap just pops; mode persists across a new `SessionStore` instance | Compose + unit | `--tests '*ModeSwitchScreenTest' --tests '*SessionStoreTest'` | Wave 0 / existing |
| ONB-02 | Avatar button on Explore and Dashboard opens the page and hides the bottom bar | Robolectric Compose | `--tests '*HomeShellsTest'` (extend) | extend |
| All | URL constants, bearer allow-list, `FakeClickMeApi` handlers for every new endpoint | unit | `--tests '*ApiRequestUrlsTest'` (extend) | extend |
| All | Design rules and string hygiene | script | `bash scripts/check-design-rules.sh` plus `ktlintCheck lintDevDebug lintProdDebug` | existing |

### Sampling Rate
- **Per task commit:** the targeted `--tests` pattern for the files touched (under 30 s for pure-JVM tests, a little longer for Robolectric).
- **Per wave merge:** `./gradlew testDevDebugUnitTest ktlintCheck` plus `bash scripts/check-design-rules.sh`.
- **Phase gate:** the full suite command above green on both flavors, including `assembleProdRelease` (R8), before `/gsd-verify-work`.

### Wave 0 Gaps
- [ ] `FakeClickMeApi` handlers and counters for the 9 new endpoints (same change as the interface).
- [ ] An injectable clock seam for the verify countdown (test helper that reads `testScheduler.currentTime`).
- [ ] `FakePhotoProcessor` and an in-memory `SignupSetupRepository` fake (or a repository built over `FakeClickMeApi`) for screen and VM tests.
- [ ] Test fixtures as JSON strings for `GET /expert/profile` full shape, signup response, publish 422 `missing_fields`, and verify 429. Copy them from the backend contracts above.
- [ ] Bitmaps for photo tests are created programmatically (Robolectric native graphics). No binary fixtures needed.
- [ ] All new test classes listed in the table (none exist yet).

### Manual-only (deferred per project policy)
Log these as open items in STATE.md and the plan SUMMARY. Do not block waves.
- Full signup on the Samsung SM-A075F with a real emailed code.
- Crop gesture feel and the spring-back animation.
- Photo-picker fallback on API 26 to 32 (no AVD available).
- The carousel visuals and Back behavior.
- Mode persistence across relaunch.
- Keyboard and `ModalBottomSheet` interaction.
- 200% font scale on every new screen.
- TalkBack pass (still Phase 15, POL-04).

## Security Domain

`security_enforcement` is enabled (absent from config, so enabled). ASVS L1, block on high.

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | yes | Client mirrors the server password rule (8+, letter, digit) for UX only; the server is authoritative. Signup tokens go through the same `tokens.save` (EncryptedSharedPreferences) path as login. Password is **never** stored in the draft, `SavedStateHandle`, nav keys or logs (Phase 3 A8 precedent). Autofill `contentType = NewPassword`. |
| V3 Session Management | yes | Signup session established identically to login. Logout resets the repository draft, deletes cached photo files, and clears tokens, mode and the onboarding flag. Refresh-after-signup was live-verified server-side. |
| V4 Access Control | partial | Server-side only (`ensureExpert`, JWT roles from the token). The client never sends a role other than the literal `"expert"` and no user id. The D-17 gate is UX, not a security boundary. |
| V5 Input Validation | yes | Client: email regex and trim, password rule, 6-digit code sanitizer, first name length 1..100, country alpha-3 normalization, IANA zone from a known list. The server's `.strict()` zod schemas are the real validation. Do not send fields outside the schema. |
| V6 Cryptography | no | None added. Verification codes are hashed server-side. Do not hand-roll anything client-side. |
| V7 Error Handling and Logging | yes | Show server `message` or a generic string. Never log request bodies (the debug interceptor is BASIC, already safe), emails, names, codes or tokens. Redact `SignupRequest.password` from any `toString()` use (override or avoid logging the DTO). |
| V8 Data Protection | yes | PII (email, name, location, photo) lives only in memory and the app-private cache, with cleanup on logout and publish. No backup (`allowBackup=false`, backup rules exclude everything). The re-encoded JPEG strips EXIF/GPS from the user's photo before upload. |
| V12 Files and Resources | yes | Only `content://` URIs from the system picker are opened. A decode failure maps to the "Couldn't open photo" dialog. Decode with a bounded `inSampleSize` to avoid OOM from oversized images. Upload is capped by server at 5 MB; the 1024 px q85 JPEG is far below that. |
| V13 API and Web Service | yes | Respect the server rate limits: no auto-retry on 429, a visible cooldown, and a single in-flight request per action (`AsyncSaveButton` ignores taps while Saving). |

### Known Threat Patterns for this stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Mass assignment (extra JSON keys) | Tampering | Typed Moshi DTOs only; server `.strict()` rejects extras. Never serialize a map. |
| Credential leakage via saved state, nav keys or logs | Information disclosure | Password only in ViewModel memory; never in `SavedStateHandle` or keys; no body logging. |
| Cross-user data leakage after logout | Information disclosure | `@Singleton` draft cleared on `isLoggedIn == false`; cache photo files deleted; no Activity-scoped VM for setup state. |
| Verify-code brute force | Spoofing / Elevation | Server limits (5 per 15 min and 5 attempts per code). The client adds no retry loop and surfaces the 429 clearly. |
| Email-send abuse via Resend | Denial of service | Server 30 s limit; client disables the button for 30 s and starts no automatic send. |
| Account enumeration via signup 409 | Information disclosure | Accepted by the backend contract (same as iOS). Do not add extra oracle channels. |
| Malicious or oversized image | Denial of service | Bounded decode (`inSampleSize`), re-encode, size cap server-side. |
| Picker URI reuse or confused deputy | Elevation | Copy to app-private cache at pick time. Do not persist or forward the URI. |
| Unprotected exported component abuse | Spoofing | No new activities, services or providers; the manifest is unchanged. |
| PII in UI snapshots / recents | Information disclosure | Not enforced at L1 for this phase; noted for Phase 15 (`FLAG_SECURE` is out of scope). |

## Sources

### Primary (HIGH confidence)
- `/Users/LeonChen/Documents/ClickMe_Android/backend-reference/src/controllers/onboarding.controller.js`, `src/schemas/onboarding.schema.js`, `src/routes/onboarding.js`: PATCH contract, publish gate, `missing_fields` shape.
- `backend-reference/src/controllers/auth.controller.js`, `src/routes/auth.js`, `src/middleware/rateLimiter.js`, `src/schemas/auth.schema.js`: signup, resend, verify, status, limiter codes.
- `backend-reference/src/controllers/expert.controller.js`, `userProfile.controller.js`, `meta.controller.js`, `routes/expert.js`, `routes/userProfile.js`, `media.js`, `services/profileExpertiseTags.js`, `middleware/ensureExpert.js`, `supabase/migrations/20260603060000_create_junctions.sql`: GET shape, avatar endpoints, meta shapes, language persistence, convert-on-demand.
- `backend-reference/docs/ios/EMAIL-VERIFICATION-ON-REQUEST.md`, `SIGNUP-USERNAME-OPTIONAL.md`, `PHONE-NUMBER-REMOVED.md`: specs (deploy banners stale).
- Live `GET https://clickme-backend-production.up.railway.app/health` -> `{"status":"success","commit":"f686d27"}`, equal to local backend HEAD.
- iOS: `SignupAccumulator.swift`, `SignupInitialViewModel.swift`, `SignupOverviewViewModel.swift`, `SignupReviewViewModel.swift`, `SignupVerifyEmailViewModel.swift`, `SignupTimezoneViewModel.swift`, `SignupProfilePhotoViewModel.swift`, `SignupBasicInfoViewModel.swift`, `SignupTagsViewModel.swift`, `Timezones.swift`, `Countries.swift`, `ModeSwitchView.swift`, `ClickMe2026App.swift`, `HomeClientView.swift` (carousel trigger), `Model/Requests/SetupExpertProfileRequest.swift`, `Model/Responses/ExpertProfileData.swift`.
- Android code read: `AppRoute`, `AppViewModel`, `ClickMeApp`, `MainActivity`, `UserManager`, `SessionStore`, `LaunchDestination`, `ClickMeApi`, `ApiRequestUrls`, `ApiException`, `ApiErrorParser`, `ApiErrorEnvelope`, `ErrorCode`, `NetworkFactory`, `MoshiProvider`, `DebugNetwork`, `AuthNavHost`, `AuthKeys`, `AuthNavigator`, `Navigator`, `NavigationState`, `HomeTabScaffold`, `LoginViewModel`, `AuthValidators`, `FieldError`, `AuthTopAppBar`, `AuthErrorDialog`, `Avatar`, `TopBarAvatarButton`, `AsyncSaveButton`, `ProcessForeground`, `FakeClickMeApi`, `AppViewModelTest`, `libs.versions.toml`, `build.gradle.kts`, `check-design-rules.sh`, `AndroidManifest.xml`.
- developer.android.com/training/data-storage/shared/photo-picker: `PickVisualMedia`, availability, fallback, URI lifetime.

### Secondary (MEDIUM confidence)
- developer.android.com/reference/android/media/ExifInterface: `ExifInterface(InputStream)` at API 24 (page truncated; confirmed from training and the partial fetch).
- `backend-reference/.planning/STATE.md` row `260802-mb1`: refresh token from an unconfirmed signup session is honored.

### Tertiary (LOW confidence, flagged in Assumptions Log)
- `Locale.getISO3Country` mapping for all 50 codes (A1), Moshi null omission (A2), `PullToRefreshBox` stability (A3), IANA id acceptance (A4), crop math (A5), sheet IME handling (A6), Coil local-model support (A7).

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH. No new libraries; versions read from the repo.
- Backend contracts: HIGH. Read from source, and cross-checked against production via `/health` and git history.
- Architecture: MEDIUM-HIGH. It extends proven Phase 3 patterns (nav host, entry-scoped VMs, stateless content). The singleton repository and the `AppViewModel` mode wiring are new but small.
- Crop and photo pipeline: MEDIUM. The math is derived here and must be pinned by the unit tests. Device feel is unverified.
- Pitfalls: HIGH for the contract-derived ones (1 to 4, 7, 8). MEDIUM for the Compose-behavior ones (13, 14).

**Research date:** 2026-10-20
**Valid until:** 2026-11-19 for backend contracts (stable); re-check `/health` and the iOS repo if the backend deploys before execution. The mode-switch image URLs may expire at any time, so export them first.

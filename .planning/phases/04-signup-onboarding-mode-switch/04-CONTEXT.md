# Phase 4: Signup, Onboarding & Mode Switch - Context

**Gathered:** 2026-10-21
**Status:** Ready for planning

<domain>
## Phase Boundary

A new user creates an expert account with email and password only (no username or phone). They complete the Overview hub's five steps in any order: Personal details with languages, Timezone, Profile Photo, Verify Email and Tags. Each step saves through `PATCH /expert/profile/setup`. They then review and publish. An unfinished signup resumes into the hub at splash or login.

After publishing, the user lands on client home, where the onboarding carousel shows. They can switch between client and expert mode on the mode switch screen, and the mode persists across relaunches.

The signup Initial screen replaces the Phase 3 Sign Up placeholder in the auth back stack. The setup hub replaces the `AppRoute.SignupSetup` placeholder.

Out of scope:
- A client signup path
- Custom tags
- Change-email
- The profile-hub mode-switch card (Phase 15)
- A notification-permission prompt (Phase 13)

</domain>

<decisions>
## Implementation Decisions

Carried forward (not re-opened):
- **Field and error rules:** field validation timing, the always-enabled submit button with an in-flight spinner, and the server-error split between inline and dialog all follow Phase 3 D-01 to D-04 (Login).
- **Resume routing:** routing after auth, including resuming setup, goes through `UserManager.resolvePostAuthDestination`, which already returns `SignupSetup` (Phase 2 D-05).
- **Logout:** clears tokens, profile, `currentMode` and the onboarding flag (Phase 2 D-09).
- **Navigation:** Navigation 3 with back stacks owned by the app. The root `AppRoute` is app state (Phase 1 D-01).
- **Styling:** Material 3 adaptation; pixel parity with iOS is out of scope.

### Signup Initial (SIGN-01)
- **D-01: Expert-only signup, matching iOS.**
  - `POST /auth/signup {email, password, role: "expert"}`. No role picker. The backend's `role: "client"` path is unused.
  - The response returns tokens, which are stored the way `login` stores them. The user is logged in immediately and the realtime manager picks up the session.
  - The back stack then becomes the hub, so the user can't go back and resubmit Initial.
- **D-02: The signup password follows the server rule.**
  - At least 8 characters, with at least one letter and at least one digit. Checked client-side and shown inline (iOS doesn't check the digit; this is a deliberate deviation).
  - The `PasswordStrengthMeter` shows once the field isn't empty, as on Set New Password.
  - Email uses the shared `AuthValidators` regex and messages.
  - A 409 shows on the email field, and a 422 maps each error to its field.

### Publish rules (SIGN-02, SIGN-04)
- **D-03: Required to publish (follows the server's `onboarding.controller.js`, not iOS):**
  - first name
  - city
  - country
  - timezone
  - at least 1 language
  - at least 1 expertise tag
  - **profile photo (`avatar_url`)**: iOS doesn't require it, but the server does.
  - **Province/state is optional.** iOS requires it, but the server doesn't.
  - **Verified email is NOT required to publish.** Verify Email is still one of the 5 hub steps and counts toward progress.
- **D-04: Steps save whatever is filled in (iOS parity).**
  - Each step's Save sends only the filled fields through `PATCH /expert/profile/setup`, then returns to the hub.
  - A step counts as complete only when its required fields are present.
  - No per-step blocking validation.
- **D-05: Only Publish is gated.**
  - The hub's "Next" is always enabled and opens Review (iOS parity).
  - Review marks missing items. Publish stays disabled until every required item in D-03 is done, with a hint listing what's left.
  - Publish re-sends the full payload with `setup_completed: true` and normalizes `country_code`, which iOS forgets to do.
- **D-06: If Publish still returns 422 `missing_fields`, name the steps.** Map each field to its hub step name (for example, "Add a profile picture" or "Add expertise") in the error dialog, and mark those steps incomplete on the hub.

### Email verification & resume (SIGN-03, SIGN-05)
- **D-07: The code is sent only on tap.**
  - "Send code" calls `POST /auth/email/resend`, never automatically. The 6-digit code field appears after a successful send.
  - The button is disabled for a 30-second countdown.
  - Verify calls `POST /auth/email/verify {code}`. A 422 `INVALID_CODE` shows inline.
- **D-08: A 429 `RATE_LIMITED` on Send counts as "already sent".** Show the code field and restart the 30-second countdown. No error dialog.
- **D-09: Track the real verified state (an iOS bug fixed).**
  - Seed it from `email_verified` in the signup response.
  - Refresh it with `GET /auth/email/status` when the hub opens or setup resumes.
  - A verified user always sees the step checked.
- **D-10: Resume always opens the Overview hub, matching iOS.**
  - Pre-fill it from `GET /expert/profile` (the full shape: name, location, timezone, languages, tags, avatar) plus the email status. Completed steps show as checked.
  - `ExpertProfileData` must be expanded beyond `setup_completed`.
- **D-11: If the setup check fails, fall through and check again (iOS parity, in line with Phase 2 D-01).**
  - If `GET /expert/profile` fails during the splash or login check (network or 5xx), route to home as iOS does.
  - Re-check setup status on the next app foreground, and route to the hub if it's incomplete.

### Step details (SIGN-02)
- **D-12: Profile Photo has a real crop UI, a deliberate improvement over iOS** (whose zoom slider does nothing).
  - Flow: `PickVisualMedia` → a full-screen crop screen (Cancel / Use) → back to the Photo step with the cropped preview → Save uploads. The user can re-crop or replace from the preview.
  - The crop is **custom Compose code with no new dependency**: pinch-zoom and pan the image under a fixed **circular guide**.
  - Output is a **square** JPEG at about 1024 px and 0.85 quality.
  - Upload is multipart field `image` to `POST /user/profile/avatar`, reading `data.avatar_url`. Remove calls `DELETE /user/profile/avatar`.
- **D-13: Timezone uses the iOS curated list of 10 zones plus auto-detect.** If the device zone isn't in the list, add it at the top, so the default selection is always visible. Saves `{timezone}`.
- **D-14: Primary tag and language follow tap order (an iOS bug fixed).**
  - Send `languages` and `expertise_tags` in the order the user selected them, so the first pick becomes primary on the server (iOS sends them alphabetically).
  - A small helper line explains that the first one is primary. No explicit primary-picker UI.
- **D-15: Tags need at least 1, with no maximum and no custom tags.**
  - The catalog comes from `GET /meta/expertise-tags`, with search.
  - The iOS `CustomTagSection` is unused there and stays out of scope.
- **D-16: Basic Info, ported from iOS:**
  - First name, city, province/state (optional), country, and languages with English pre-selected.
  - The country picker is the **iOS static 50-country list (alpha-3)**, searchable.
  - Languages come from `GET /meta/languages`, with search and multi-select.

### Mode switch & onboarding (ONB-01, ONB-02)
- **D-17: Switching to expert routes by setup status (a deviation from iOS, which has no gate).**
  - If expert setup is incomplete, switching to expert opens the setup hub instead of expert home.
  - Switching to client is always allowed.
  - Mode is local only, with no server call. It persists through `SessionStore`/`UserManager.setMode`.
  - `AppViewModel` must react to `currentMode` changes. Today there's only `debugSwitchScaffold`.
- **D-18: The mode switch is a full-screen page with a `TopAppBar` back arrow** (the user chose this over a bottom sheet).
  - Two cards: Client ("Find and book experts") and Expert ("Manage bookings and profile"). The current mode is marked.
  - Tapping a card switches mode and closes the page.
- **D-19: The card illustrations are bundled drawables.** iOS loads them from remote URLs (`ModeSwitchView.swift` `imageURL`). Export those two images once into `res/drawable`, so nothing flashes on load and the screen works offline.
- **D-20: The entry point this phase is the top-bar avatar** on client and expert home, which is a no-op today. The profile-hub card comes with Phase 15.
- **D-21: The onboarding carousel shows once per login (iOS parity, ONB-01).**
  - It's a full-screen 4-page carousel on entering client home while `hasSeenOnboarding` is false. It ports the iOS pages: "Welcome to ClickMe", "Discover Top Experts", "Book & Schedule Instantly", "Seamless Communication".
  - Skip appears on every page, with Next, and "Get Started" on the last page. Swiping moves between pages.
  - Skip and Get Started both mark it shown. Logout clears the flag (already done in Phase 2).
- **D-22: After publishing, set mode to client and open client home, where the carousel appears (iOS parity).**

### Claude's Discretion
- Hub layout details: rows with check states, a done/5 progress bar, a tips card and a bottom "Next" bar, adapted to Material 3.
- The Review screen layout: cards with Edit buttons, plus a Timezone card. iOS omits timezone; adding it is fine.
- How the signup screens fit into navigation: Initial in the auth back stack; the hub and its steps in a back stack under `AppRoute.SignupSetup`. The accumulator/shared state can be a setup-scoped ViewModel or repository.
- The in-memory signup accumulator design (port `SignupAccumulator.swift`).
- How the foreground re-check (D-11) hooks in. Reuse `ProcessForeground` from Phase 3.
- Crop math, gesture limits (minimum zoom fills the circle), and the downscale implementation.
- Carousel illustration assets: copy them from the iOS asset catalog.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.** iOS paths are under `ios-reference/`, which in worktrees is `/Users/LeonChen/Documents/ClickMe2026/`. Backend paths are under `backend-reference/`, which in worktrees is `/Users/LeonChen/Documents/ClickMe2026 Backend/`.

### Signup flow (SIGN-01..05)
- `ios-reference/ClickMe2026/Views/Screens/Signup Flow/Initial/`: Initial view and VM, signup call, 409/422 handling
- `ios-reference/ClickMe2026/Views/Screens/Signup Flow/Overview/`: hub rows, progress, Next
- `ios-reference/ClickMe2026/Views/Screens/Signup Flow/Basic Info/`: fields, language picker
- `ios-reference/ClickMe2026/Views/Screens/Signup Flow/Timezone/`: the curated zone list and auto-detect (the zones are defined in `SignupTimezoneViewModel.swift`)
- `ios-reference/ClickMe2026/Views/Screens/Signup Flow/Profile Photo/`: picker, upload, remove
- `ios-reference/ClickMe2026/Views/Screens/Signup Flow/Verify Email/`: send on tap, cooldown, verify
- `ios-reference/ClickMe2026/Views/Screens/Signup Flow/Setup Tags/`: tag catalog and search
- `ios-reference/ClickMe2026/Views/Screens/Signup Flow/Signup Review/`: review cards and publish
- `ios-reference/ClickMe2026/Views/Screens/Signup Flow/SignupAccumulator.swift`: shared signup state and publish payload
- `ios-reference/ClickMe2026/Views/Screens/Login Flow/Login/LoginView.swift`: signup orchestration and the resume check after login
- `ios-reference/ClickMe2026/Views/Screens/Splash/SplashViewModel.swift`: the `setupIncomplete` check at launch
- `ios-reference/Constants/Countries.swift`: the 50-country list (alpha-3)
- `ios-reference/ClickMe2026/Network/ClickMeAPI+Auth.swift`: signup, email resend, verify and status
- `ios-reference/ClickMe2026/Network/ClickMeAPI+ExpertProfile.swift`: `PATCH /expert/profile/setup`, `GET /expert/profile`
- `ios-reference/ClickMe2026/Network/ClickMeAPI+Media.swift` and `ClickMeAPI+UserProfile.swift`: avatar upload and delete
- `ios-reference/ClickMe2026/Network/ClickMeAPI+Meta.swift`: `GET /meta/languages`, `GET /meta/expertise-tags`

### Backend contracts
- `backend-reference/docs/ios/SIGNUP-USERNAME-OPTIONAL.md`: no username at signup
- `backend-reference/docs/ios/PHONE-NUMBER-REMOVED.md`: no phone field
- `backend-reference/docs/ios/EMAIL-VERIFICATION-ON-REQUEST.md`: send on request, 30s `RATE_LIMITED`, 6-digit code with a 15-minute lifetime, polling `GET /auth/email/status`. It has a deploy-status note: production emailed a code at signup until that change shipped.
- `backend-reference/src/controllers/onboarding.controller.js`: publish-required fields and `missing_fields` (source for D-03 and D-06)
- `backend-reference/src/schemas/auth.schema.js`: the signup schema and password rule (letter + digit)

### Onboarding & mode switch (ONB-01, ONB-02)
- `ios-reference/ClickMe2026/Views/Screens/Onboarding/`: `OnboardingView`, `OnboardingViewModel` (4 pages), `Subviews/` (brand, illustration card, dots, Skip, Next)
- `ios-reference/ClickMe2026/Views/Screens/Settings/Mode Switching/`: `ModeSwitchView` (card copy and the remote `imageURL`s to export, per D-19), `ModeSwitchViewModel`, `Subviews/ModeSwitchCard.swift`

### Prior phase decisions
- `.planning/phases/03-login-password-recovery-realtime/03-CONTEXT.md`: field, validation and error rules (D-01..D-04); the Sign Up placeholder this phase replaces (D-05)
- `.planning/phases/02-networking-session-sdk-spikes/02-CONTEXT.md`: offline launch (D-01), the `setupIncomplete` port (D-05), logout clearing the flags (D-09)
- `.planning/REQUIREMENTS.md`: SIGN-01..05, ONB-01, ONB-02

</canonical_refs>

<code_context>
## Existing Code Insights

Base: `app/src/main/java/com/uptrendinvestments/clickmeapp/`

### Reusable Assets
- **Phase 1–3 components:** `components/ValidatedField.kt` (with trailing badge and contentType), `PasswordStrength`/`PasswordStrengthMeter`, `AsyncSaveButton`, `Avatar`, `TopBarAvatarButton`, `EmptyState`.
- **Auth helpers:** `screens/login/AuthValidators.kt` (`EMAIL_REGEX`, `sanitizeCode` for the 6-digit code), `FieldError`, `AuthErrorDialog`, `AuthTopAppBar` (with `backEnabled`).
- **Session state:** `managers/UserManager.kt` has `login`, `logout`, `setMode`, `markOnboardingShown`, `hasSeenOnboarding`, and `resolvePostAuthDestination` (which already checks setup). `SessionStore` persists the mode and the onboarding flag. `tokens.save(TokenPair)` is the pattern for storing signup tokens.
- **Libraries:** Coil 3 for images. Retrofit + OkHttp support multipart. `/auth/signup` is already in the public, no-auth paths.
- **`realtime/ProcessForeground.kt`:** foreground signal for the setup re-check in D-11.

### Established Patterns
- **Screen structure:** each screen is a `ViewModel` + `Screen` pair, with the stateless content split out for Robolectric tests. Strings go in a per-feature `strings_*.xml`.
- **Navigation:** the auth back stack is in `navigation/AuthNavHost.kt`, `AuthKeys.kt` and `AuthNavigator.kt`. `SignUpPlaceholderScreen` is wired in, ready to replace with `SignupInitialScreen`.
- **Networking:** `network/ApiRequestUrls.kt` + `ClickMeApi.kt` + `testing/FakeClickMeApi.kt`. Every new endpoint gets a fake handler and a URL test.
- **Error mapping:** the Phase 2 `Error+UserMessage` port handles 422 `errors[]`, which drives the inline field errors.

### Integration Points
- `AppRoute.SignupSetup` → `screens/signup/SignupSetupPlaceholder.kt` (replace with the hub's back stack).
- `screens/signup/SignUpPlaceholderScreen.kt` plus its entries in `AuthNavHost`/`AuthKeys` (replace with Initial).
- `AppViewModel` needs to follow `currentMode` for D-17 and D-22.
- The no-op avatar clicks in `screens/clientmode/home/HomeClientScreen.kt` and `screens/expertmode/home/HomeExpertScreen.kt` (D-20).
- Client home entry shows the onboarding carousel when `hasSeenOnboarding` is false.
- **Missing APIs:**
  - `signup`
  - `resendVerificationEmail`
  - `verifyEmailCode`
  - `checkEmailStatus`
  - `setupExpertProfile` (PATCH)
  - `getExpertProfile`, with the full DTO
  - `getLanguages`
  - `getExpertiseTags`
  - `uploadAvatar` (multipart `image`)
  - `deleteAvatar`
- **Missing DTOs and constants:** the matching request and response DTOs, plus `Countries` and `Timezones`.

</code_context>

<specifics>
## Specific Ideas

- **iOS bugs fixed on purpose:** don't port these:
  - publishing without a photo always fails (D-03)
  - the verified state is lost on resume (D-09)
  - the primary tag and language are accidental (D-14)
  - the zoom slider does nothing (D-12)
  - `country_code` isn't normalized on publish (D-05)
- **Deliberate deviations from iOS:**
  - province is optional
  - the signup password needs a digit
  - switching to expert is gated by setup status
  - Publish is disabled until the required items are done
  - the mode switch card images are bundled
- **Device testing:** manual and device checks (the full signup on the Samsung, real email codes, crop feel, carousel, mode persistence across relaunch) are deferred human checkpoints, following the project's manual-testing-deferred preference.

</specifics>

<deferred>
## Deferred Ideas

- **Client-role signup** (`role: "client"`): the backend supports it, but it's new capability beyond iOS.
- **Custom expertise tags:** the iOS `CustomTagSection` exists but is unused.
- **Explicit primary tag and language picker:** tap order covers this for now (D-14).
- **Change-email from Verify Email:** iOS only goes back.
- **Mode-switch card on the profile hubs:** Phase 15, profile and settings.
- **A maximum tag count:** not enforced by the server.

</deferred>

---

*Phase: 04-signup-onboarding-mode-switch*
*Context gathered: 2026-10-21*

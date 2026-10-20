---
phase: 01-foundation-app-shell-play-account
verified: 2026-10-20T14:30:00Z
status: human_needed
score: 4/4 must-haves verified (automated); device-only visual checks outstanding
overrides_applied: 0
re_verification:
  previous_status: none
gaps: []
deferred: []
human_verification:
  - test: "Cold-launch the devDebug build on an API 26 emulator and on an API 31+ device/emulator"
    expected: "Splash shows the ClickMe mark on #131313 with no white frame and no fixed delay, mark not cropped or oversized, then lands on the Explore tab. On API 26, Nunito 600 is visibly heavier than 400 and system bar icons are light."
    why_human: "Visual, and needs a device. D-09 excludes instrumented tests from CI. No API 26 AVD was available to the executor. Only an API 37 headless emulator was used."
  - test: "Open the Profile tab, tap the debug gallery button, scroll halfway, switch to Explore and back"
    expected: "The gallery and its scroll position are still there. Re-tapping Profile pops to root. Back on Bookings selects Explore. Back on Explore exits. Tab switches cross-fade; gallery push and pop slide."
    why_human: "Stack retention is covered by JVM and Robolectric tests, but scroll-position retention and system Back and predictive-back feel on a real build are not."
  - test: "Profile tab, tap the debug switch to expert scaffold, visit Dashboard, Bookings, Topics, Chats and Profile, then switch back"
    expected: "The expert shell renders all five tabs, and switching back restores the client shell."
    why_human: "Visual check on a real build. Robolectric covers the structure."
  - test: "Set the font scale to 200% and walk the tabs and the gallery"
    expected: "No clipping or overlap."
    why_human: "Visual."
  - test: "Check the home screen launcher icon with themed icons on and off (API 33+), on the dev flavor"
    expected: "Label is 'ClickMe Dev'. The mark sits inside the adaptive mask. The themed icon is legible. See IN-04 in 01-REVIEW.md (the monochrome layer reuses the glow PNG)."
    why_human: "Visual."
  - test: "Open the debug design-system gallery and use every component: press feel, AsyncSaveButton states, field focus and error borders, password eye toggle, Custom Tab open and Back, blocked-link message"
    expected: "All six shared components behave as described in 01-UI-SPEC.md."
    why_human: "Touch feel and an external browser (Custom Tab) cannot be checked programmatically."
---

# Phase 1: Foundation, App Shell & Play Account Verification Report

**Phase Goal:** The themed app shell launches on a device, and the outside wait for the Play Console organization account has started.
**Verified:** 2026-10-20
**Status:** human_needed (all automated checks pass; no gaps; device-only visual checks remain)
**Re-verification:** No, initial verification

## Goal Achievement

### Observable Truths (ROADMAP success criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | The app launches through the Android 12+ splash into an empty dark tab layout. Client (Explore, Search, Bookings, Chats, Profile) and expert (Dashboard, Bookings, Topics, Chats, Profile) scaffolds render. Each tab keeps its own back stack. | VERIFIED (visual device checks pending) | `MainActivity` calls `installSplashScreen()` and `setKeepOnScreenCondition { !destinationResolved }` with no fixed delay. The manifest applies `Theme.ClickMe.Splash` (parent `Theme.SplashScreen`, surface background, `splash_logo`, `postSplashScreenTheme`). `clientTabs` and `expertTabs` define the exact tab sets in the exact order. `HomeClientShell` and `HomeExpertShell` render them through `HomeTabScaffold`. The expert shell is reachable only through the debug switch (`BuildConfig.DEBUG`), which is acceptable for Phase 1 because the real mode switch is ONB-02 (Phase 4). `NavigationState` holds one `NavBackStack` per top-level route, and `Navigator` is tested for retention (`stackIsRetainedAcrossTabSwitches`, `switchingTabDoesNotTouchOtherStack`, `galleryStaysOnProfileStackAcrossTabSwitch`). I viewed `phase1-launch2.png`: dark #131313 surface, "Explore" top bar, "Nothing here yet." empty state, 5-tab bar with Explore selected. This confirms the emulator smoke claim. |
| 2 | Dev and prod flavors both build and target the production backend URL. The Stripe publishable key and Agora App ID come from build config. Lint and CI run and pass on every push. | VERIFIED | `app/build.gradle.kts` defines the `dev` and `prod` flavors with `BASE_URL = https://clickme-backend-production.up.railway.app/api/v1`, per-flavor `STRIPE_PUBLISHABLE_KEY` (pk_test / pk_live) and `AGORA_APP_ID`. `AppConfig` reads them. I re-ran `ktlintCheck lintDevDebug lintProdDebug testDevDebugUnitTest testProdDebugUnitTest assembleDevDebug assembleProdDebug` and it passed. Test results are 71 tests, 0 failures, 0 skipped per flavor. Both APKs exist. `scripts/check-design-rules.sh` prints "Design rules: OK". `.github/workflows/ci.yml` triggers on `push` and `pull_request` and runs the same commands. The public GitHub API confirms run 39426784314 (head_sha `d1217fb`) is `completed` / `success`. The only commits after `d1217fb` are docs. They are not pushed (local `main` is ahead of `origin/main` by 3). |
| 3 | The shared components (press-scale button, async save button, validated text field, avatar, glow background, Custom Tab launcher) render in the Material 3 theme with the bundled rounded font. | VERIFIED (device feel pending) | All six exist and are substantive: `PressScale.kt` (128 lines), `AsyncSaveButton.kt` (138), `ValidatedField.kt` (259), `Avatar.kt` (93), `GreenGlowBlobLayer.kt` (69), `CustomTabLauncher.kt` (86, with an http/https allowlist). The debug gallery uses all six (`GalleryControlsSection`, `GalleryMediaSection`). `ClickMeTheme` wraps `MaterialTheme` with the dark scheme and `ClickMeTypography`. `Type.kt` sets `NunitoFamily` from the bundled `res/font/nunito.ttf` (variable font, weights 400 and 600). The screenshot shows Nunito-style rounded text. Robolectric tests cover them (`ControlsTest`, `AvatarTest`, `CustomTabLauncherTest`, `ThemeTest`). |
| 4 | The corporation's D-U-N-S is confirmed or requested, the Play Console organization account is created and submitted, and the wait is logged in STATE.md. | VERIFIED (user-attested) | STATE.md Blockers line: "[Phase 1]: OUTSIDE WAIT (RESOLVED). Play Console organization account submitted for Google verification on 2026-10-20 and shown as verified on 2026-10-20. D-U-N-S: confirmed, details match." 01-02-SUMMARY records the same, with no D-U-N-S number or personal data in the repo. The Play Console state is external and can only be attested by the user. The user reported it, and the roadmap note says the same. |

**Score:** 4/4 truths verified.

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `app/build.gradle.kts` | Flavors and build config fields; Hilt, Retrofit, Moshi, OkHttp, coroutines, Coil, Navigation | VERIFIED | All dependencies are declared. `minSdk` 26, `targetSdk` 37. |
| `config/AppConfig.kt` | Typed `BuildConfig` access | VERIFIED | Used by the per-flavor tests. Not consumed by app code yet (Phase 2 networking), which is expected. |
| `.github/workflows/ci.yml` and `scripts/check-design-rules.sh` | CI on every push | VERIFIED | Both exist. CI is green. |
| `AppRoute.kt`, `AppViewModel.kt`, `ClickMeApp.kt`, `MainActivity.kt` | Splash hold and route switch | VERIFIED | The route is a stub that resolves to `ClientHome`. This is a locked decision, and the hook is documented for Phase 2. |
| `navigation/*` (NavigationState, Navigator, TabDefinition, HomeTabScaffold) | Per-tab stacks | VERIFIED | Wired from both home shells. |
| `ui/theme/*` | Dark M3 scheme (surface #131313, primary #44f697, container #00d97e) and Nunito | VERIFIED | Palette values match FND-09. `darkColorScheme` is used. No light scheme. |
| `components/*` (6 shared components) | See truth 3 | VERIFIED | Wired through the gallery. They have no production-screen consumers yet, which is expected for Phase 1. |
| `res/font/nunito.ttf`, launcher icon and splash drawables | Bundled font, icon, splash | VERIFIED | Files exist. Lint passes on both flavors. |

### Key Link Verification

| From | To | Via | Status |
|------|----|-----|--------|
| `MainActivity` | `AppViewModel.destinationResolved` | `setKeepOnScreenCondition` | WIRED |
| `MainActivity` | `ClickMeApp` | `setContent { ClickMeTheme { ClickMeApp(vm) } }` | WIRED |
| `ClickMeApp` | `HomeClientScreen` / `HomeExpertScreen` | `when (route)` | WIRED |
| Home shells | `HomeTabScaffold` | `rememberNavigationState` + `Navigator` + `entryProvider` | WIRED |
| `HomeTabScaffold` bottom bar | `Navigator.onTabSelected` | `NavigationBarItem.onClick` | WIRED |
| `NavDisplay` | `Navigator.goBack` | `onBack` | WIRED |
| Gallery | the six shared components | direct calls | WIRED |

### Data-Flow Trace (Level 4)

Phase 1 screens are static placeholders by design ("Nothing here yet."). `avatarUrl` is null until /me exists (Phase 2 and later). This is documented, intentional and not a stub defect. `AppViewModel` resolves the route synchronously, as the plan states.

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Full local suite | `./gradlew ktlintCheck lintDevDebug lintProdDebug testDevDebugUnitTest testProdDebugUnitTest assembleDevDebug assembleProdDebug` | BUILD SUCCESSFUL | PASS |
| Unit tests | test-results XML | 71 / 71 passing per flavor | PASS |
| Design rules | `bash scripts/check-design-rules.sh` | "Design rules: OK" | PASS |
| CI on `d1217fb` | GitHub REST API | `completed` / `success` | PASS |
| Emulator launch | `phase1-launch2.png` | Dark shell with 5 tabs | PASS |

Step 7c: no probes declared (no `probe-*.sh` in this phase), so none were run.

### Requirements Coverage

| Requirement | Source Plan(s) | Description | Status | Evidence |
|-------------|----------------|-------------|--------|----------|
| FND-01 | 01-01 | Builds with the stack; dev and prod point at the production backend; Stripe key and Agora ID from build config | SATISFIED | Flavors, `AppConfig` and per-flavor tests. See WR-02 below. The trailing slash is a Phase 2 concern, not a Phase 1 failure. |
| FND-02 | 01-01, 01-09 | Lint and baseline CI on every push | SATISFIED | `ci.yml` plus a green run. WR-03 says the hex-color gate is bypassable. It does not defeat the requirement, but it weakens one CI check. |
| FND-09 | 01-03 | Dark M3 scheme and bundled rounded font | SATISFIED | `Color.kt`, `Theme.kt`, `Type.kt`, `ThemeTest`. |
| FND-10 | 01-06, 01-07, 01-08 | Six shared components | SATISFIED | Components exist, are tested and appear in the gallery. WR-04 (`ValidatedField` overrides keyboard options) is a latent issue for later phases. |
| FND-11 | 01-04, 01-08 | Android 12+ SplashScreen API (no fixed delay) and `AppRoute` | SATISFIED | `MainActivity` and `AppRoute` enum with Splash, Login, SignupSetup, ClientHome, ExpertHome. Auto-login resolution is Phase 2 and Phase 3. |
| FND-12 | 01-05, 01-08 | 5-tab client and expert scaffolds, each with a saved back stack | SATISFIED | Tab definitions, `NavigationState` and `NavigatorTest`. WR-01 and WR-05 affect transition animation and API validation only, not stack retention. |
| REL-01 | 01-02 | D-U-N-S confirmed or requested | SATISFIED (user-attested) | STATE.md and 01-02-SUMMARY. |
| REL-02 | 01-02 | Play Console org account created and submitted | SATISFIED (user-attested) | STATE.md says submitted and verified on 2026-10-20. |

All 8 requirement IDs in the phase are accounted for in plan frontmatter and in REQUIREMENTS.md. There are no orphaned requirements: the Phase 1 rows in the traceability table are exactly FND-01, FND-02, FND-09, FND-10, FND-11, FND-12, REL-01 and REL-02.

### Anti-Patterns Found

Scan of `app/src`, `scripts`, `.github` and `build.gradle.kts`: no TBD, FIXME or XXX markers. No hard-coded empty production data beyond the documented Phase 1 stubs. No keystore, `google-services.json` or secret keys are tracked.

Code review warnings (01-REVIEW.md: 0 critical, 5 warnings, 9 info). I checked each against the must-haves:

| ID | Severity here | Undermines a must-have? |
|----|---------------|--------------------------|
| WR-02: `BASE_URL` has no trailing slash | Warning (forward-looking) | **No for Phase 1.** FND-01 specifies the URL without a trailing slash, and nothing in Phase 1 calls `Retrofit.Builder().baseUrl(...)` (grep found no use). This will throw `IllegalArgumentException` in Phase 2 when Retrofit is wired, and the flavor tests lock the current value. **Action:** Phase 2 planning must either normalize in `AppConfig` or end the constant with `/`, update both flavor tests, and use relative Retrofit paths. |
| WR-01 predictive-back transition uses stale state; WR-05 `navigateInTab` and `reselect` set the wrong transition and accept invalid routes | Warning | No. Per-tab stack retention and Back rules are unaffected. The visible effect is the wrong animation in some sequences. WR-05 matters for Phases 12 and 13, which depend on `navigateInTab`. |
| WR-03: design-rule script is bypassable and the secret scan skips files | Warning | No. CI runs and passes. A gate weakness. |
| WR-04: `ValidatedField` overrides keyboard options | Warning | No. Latent until Phase 3 and later. |

None of these is a BLOCKER. They should be triaged before Phases 2, 3 and 12.

### Human Verification Required

See the `human_verification` list in the frontmatter (six items). They were deferred by plan 01-09 and 01-VALIDATION.md to end-of-phase UAT. They need a device or emulator and visual judgement (API 26 splash and Nunito weight, on-device scroll retention and Back behaviour, expert shell, 200% font scale, themed icon, component feel and Custom Tab). The JVM, Robolectric and API 37 emulator evidence covers the structure and behavior, but not these.

### Gaps Summary

No gaps. Every ROADMAP success criterion has supporting code, and I re-ran the build, lint, 142 unit tests and the design-rule script myself. CI is independently confirmed green. Success criterion 4 rests on the user's attestation of the Play Console state, which is the only evidence possible for an external system. Status is `human_needed` solely because the device-only visual checks remain; nothing found blocks the phase goal. Tracking files (STATE.md frontmatter showing 0 of 9 plans and REQUIREMENTS.md traceability all "Pending") are stale and should be updated by the orchestrator at phase close. Three local docs commits are also unpushed.

---

_Verified: 2026-10-20_
_Verifier: Claude (gsd-verifier)_

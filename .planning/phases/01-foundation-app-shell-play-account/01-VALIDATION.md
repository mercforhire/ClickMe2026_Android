---
phase: 1
slug: foundation-app-shell-play-account
status: executed
nyquist_compliant: true
wave_0_complete: true
created: 2026-10-19
---

# Phase 1 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4.13.2 + Robolectric 4.17 + Compose `ui-test-junit4` (JVM only, per D-09) |
| **Config file** | none — Wave 0 adds `testOptions { unitTests.isIncludeAndroidResources = true }` and test deps to `app/build.gradle.kts` |
| **Quick run command** | `./gradlew ktlintCheck :app:testDevDebugUnitTest` |
| **Full suite command** | `./gradlew ktlintCheck lintDevDebug lintProdDebug testDevDebugUnitTest testProdDebugUnitTest assembleDevDebug assembleProdDebug && bash scripts/check-design-rules.sh` |
| **Estimated runtime** | ~60 seconds warm (quick), ~4 minutes (full) |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew ktlintCheck :app:testDevDebugUnitTest`
- **After every plan wave:** Run the full suite command
- **Before `/gsd-verify-work`:** Full suite green locally AND on GitHub Actions, plus the manual checklist
- **Max feedback latency:** 60 seconds

---

## Per-Task Verification Map

Requirement → test mapping from RESEARCH.md. Statuses recorded by plan 01-09 (2026-10-20) from the full local suite run plus `assembleProdRelease`, and the GitHub Actions run on `d1217fb`.

| Requirement | Behavior | Test Type | Automated Command | File Exists | Status |
|-------------|----------|-----------|-------------------|-------------|--------|
| FND-01 | Both flavors build; `BASE_URL` is prod; dev key `pk_test_`, prod `pk_live_`; Agora ID non-empty | unit (src/testDev, src/testProd) + build | `./gradlew assembleDevDebug assembleProdDebug testDevDebugUnitTest testProdDebugUnitTest` | ✅ | ✅ green |
| FND-02 | ktlint + lint + design-rule script pass; CI workflow exists | script + gradle | `./gradlew ktlintCheck lintDevDebug lintProdDebug && bash scripts/check-design-rules.sh` | ✅ | ✅ green (local; GitHub Actions run 39426784314 on `d1217fb` concluded `success`) |
| FND-09 | Color scheme matches UI-SPEC hex; type sizes in {12,16,20,28}sp; Nunito 400/600 | unit | `--tests "*ThemeTest"` | ✅ | ✅ green (automated); Nunito on API 26 pending human check |
| FND-10 | AsyncSaveButton states, ValidatedField error semantics, Avatar fallback, EmptyState | Robolectric Compose | `--tests "*ComponentsTest"` (implemented as `*components.ControlsTest` and avatar/media tests) | ✅ | ✅ green (automated); on-device feel pending human check |
| FND-10 | Custom Tab launcher rejects non-http(s), calls onFailure when unresolved | Robolectric unit | `--tests "*CustomTabLauncherTest"` | ✅ | ✅ green |
| FND-11 | Stub resolver yields ClientHome; destinationResolved true | unit (coroutines-test) | `--tests "*AppViewModelTest"` | ✅ | ✅ green; emulator cold start OK (see below); white-flash check pending human |
| FND-12 | Per-tab stacks retained; Back at non-home root → home; re-tap pops to root | JVM unit | `--tests "*NavigatorTest"` | ✅ | ✅ green; on-device scroll retention pending human check |
| FND-12 | Client/expert tab labels; selected state exposed | Robolectric Compose | `--tests "*HomeTabScaffoldTest"` / `*HomeShellsTest` | ✅ | ✅ green |
| REL-01/02 | Play Console steps logged | human-action gate | `grep -n "Play Console" .planning/STATE.md` | n/a | ✅ green (01-02: org account verified 2026-10-20) |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

### Per-Plan Task Map (from the 9 PLAN.md files)

| Plan | Wave | Task | Requirement | Automated command |
|------|------|------|-------------|-------------------|
| 01-01 | 1 | T1 toolchain, flavors, ktlint | FND-01 | `./gradlew assembleDevDebug assembleProdDebug ktlintCheck` |
| 01-01 | 1 | T2 identity, Hilt app, AppConfig | FND-01 | `./gradlew testDevDebugUnitTest testProdDebugUnitTest assembleDevDebug ktlintCheck` |
| 01-01 | 1 | T3 design-rules script, CI workflow | FND-02 | `bash scripts/check-design-rules.sh` (+ negative fixtures) `&& ./gradlew lintDevDebug lintProdDebug` |
| 01-02 | 1 | T1-T2 Play Console (human-action) | REL-01, REL-02 | user reply (checkpoint) |
| 01-02 | 1 | T3 record wait | REL-01, REL-02 | `grep -n 'OUTSIDE WAIT' .planning/STATE.md` |
| 01-03 | 2 | T1 tokens + ClickMeTheme | FND-09 | `--tests "*ui.theme.ThemeTest"` |
| 01-03 | 2 | T2 Nunito + typography + gallery theme section | FND-09 | `--tests "*ui.theme.ThemeTest"` + lintDevDebug |
| 01-04 | 2 | T1 icon + splash artwork | FND-11 | `./gradlew assembleDevDebug lintDevDebug` |
| 01-04 | 2 | T2 themes, manifest, AppRoute/AppViewModel | FND-11 | `--tests "*AppViewModelTest"` + lint both flavors |
| 01-05 | 3 | T1 NavigationState + Navigator | FND-12 | `--tests "*navigation.NavigatorTest"` |
| 01-05 | 3 | T2 tab definitions + HomeTabScaffold | FND-12 | `--tests "com.uptrendinvestments.clickmeapp.navigation.*"` |
| 01-06 | 3 | T1 buttons, EmptyState, PlaceholderTabContent | FND-10 | `--tests "*components.ControlsTest"` |
| 01-06 | 3 | T2 ValidatedField + gallery controls | FND-10 | `--tests "*components.ControlsTest"` + lintDevDebug |
| 01-07 | 3 | T1 Custom Tab launcher | FND-10 | `--tests "*components.web.CustomTabLauncherTest"` |
| 01-07 | 3 | T2 avatar, glow, gallery media | FND-10 | `--tests "com.uptrendinvestments.clickmeapp.components.*"` |
| 01-08 | 4 | T1 gallery screen + home shells | FND-10, FND-12 | `./gradlew assembleDevDebug assembleProdDebug ktlintCheck lintDevDebug` |
| 01-08 | 4 | T2 ClickMeApp + MainActivity + HomeShellsTest | FND-11, FND-12 | full suite command |
| 01-09 | 5 | T1 full suite, release build, push | FND-02 | full suite + `assembleProdRelease` + `git push origin main` |
| 01-09 | 5 | T2 emulator smoke + sign-off | FND-11, FND-12 | `adb shell am start -W ...` + VALIDATION.md grep |

`--tests` rows run as `./gradlew :app:testDevDebugUnitTest --tests "<pattern>"`. No three consecutive code tasks lack an automated command.

---

## Wave 0 Requirements

- [x] `.editorconfig` (ktlint Composable naming exemption)
- [x] `scripts/check-design-rules.sh`
- [x] `.github/workflows/ci.yml`
- [x] Test deps + `testOptions` in `app/build.gradle.kts`; remove/fix template `ExampleUnitTest`/`ExampleInstrumentedTest`
- [x] Test files under `app/src/test/java/com/uptrendinvestments/clickmeapp/…` (ThemeTest, ControlsTest, CustomTabLauncherTest, AppViewModelTest, NavigatorTest, HomeTabScaffoldTest, HomeShellsTest, per-flavor BuildConfig tests)

---

## Automated Evidence from Plan 01-09 (2026-10-20)

| Check | Result |
|-------|--------|
| Full local suite (`ktlintCheck lintDevDebug lintProdDebug testDevDebugUnitTest testProdDebugUnitTest assembleDevDebug assembleProdDebug`) | BUILD SUCCESSFUL |
| `assembleProdRelease` (R8, unsigned) | BUILD SUCCESSFUL |
| `bash scripts/check-design-rules.sh` | `Design rules: OK` |
| Secret hygiene (`.jks`, `.keystore`, `keystore.properties`, `google-services.json` tracked) | 0 files; no uncommitted changes in code paths |
| Push to origin | `main` pushed, `dc2172d..d1217fb`; local HEAD equals `origin/main` |
| GitHub Actions `CI` run 39426784314 on `d1217fb` | status `completed`, conclusion `success` (read via the public REST API) |
| Emulator smoke (API 37, `devDebug`, cold start) | `am start -W`: `Status: ok`, `LaunchState: COLD`, `TotalTime: 3288 ms` (headless emulator just booted, so not a performance figure) |
| Crash buffer for the package | 0 lines |
| First-screen screenshot (viewed) | Dark `#131313` surface, "Explore" top app bar, avatar button top right, "Explore / Nothing here yet." empty state, bottom nav with Explore selected in green and Search, Bookings, Chats, Profile |
| Emulator note | First screenshot showed a "System UI isn't responding" dialog. It came from the headless emulator's system process (logcat showed ANRs in `com.google.android.gms`, none in the app). Dismissed with Wait, and the second screenshot was clean. Not an app defect. |

---

## Manual-Only Verifications

All pending the user's end-of-phase review. Full step lists are in the 01-09 plan, Task 2 human checks.

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Splash: no white flash, no fixed delay, correct mark size on API 26 and 31+ | FND-11 | Needs device; D-09 excludes instrumented CI | Cold-launch on API 26 emulator and API 35+ emulator; observe splash → tab layout |
| Per-tab stack + scroll retained on real build | FND-12 | Needs device | Push debug destination, switch tab, return; verify stack and scroll intact |
| Tab rules: re-tap pops to root, Back at non-home root selects Explore, Back on Explore exits | FND-12, D-15 | System Back behavior | See 01-09 human checks |
| Expert scaffold on a real build | FND-12 | Visual | Profile → "Switch to expert scaffold", visit each tab, switch back |
| Nunito renders at 400/600 on API 26; 200% font scale layout | FND-09 | Visual | Set font scale 200%, inspect tabs and component gallery |
| Launcher icon (adaptive + themed) renders from SplashLogo mark | D-11 | Visual | Check home screen icon with themed icons on and off (API 33+) |
| Shared components on device (press feel, focus borders, Custom Tab) | FND-10 | Device and browser | Exercise the gallery |
| CI green on GitHub | FND-02 | Requires push to remote | Result read via API: success. User may re-confirm at https://github.com/mercforhire/ClickMe2026_Android/actions |
| Play Console org account submitted, wait logged | REL-01, REL-02 | Human, external | Follow plan 01-02 checklist; confirm STATE.md entry |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency < 60s
- [x] Nyquist compliance flag set in frontmatter

**Approval:** automated checks signed off 2026-10-20; REL-01/02 done 2026-10-20 (plan 01-02); manual device checks remain open.

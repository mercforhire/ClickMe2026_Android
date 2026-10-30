---
phase: "15"
slug: "polish-hardening"
# status lifecycle: draft (seeded by plan-phase) → validated (set by validate-phase §6)
# audit-milestone §5.5 distinguishes NOT-VALIDATED (draft) from PARTIAL (validated + nyquist_compliant: false) (#2117)
status: validated
nyquist_compliant: true
wave_0_complete: true
created: "2026-10-29"
---

# Phase 15 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4.13.2 + Robolectric 4.17 (`sdk=35`) + Compose `ui-test-junit4` (BOM) + coroutines-test |
| **Config file** | `app/src/test/resources/robolectric.properties`; `unitTests.isIncludeAndroidResources = true` in `app/build.gradle.kts` |
| **Quick run command** | `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDevDebugUnitTest --tests '*SomeTest' ktlintCheck` |
| **Full suite command** | `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDevDebugUnitTest ktlintCheck :app:assembleDevDebug && bash scripts/check-design-rules.sh` |
| **Estimated runtime** | ~15–60 s per touched test class; full suite several minutes |

---

## Sampling Rate

- **After every task commit:** Run the touched test class(es) via the quick run command
- **After every plan wave:** Run the full suite command
- **Before `/gsd-verify-work`:** Full suite green on both flavors (`testDevDebugUnitTest`, `testProdDebugUnitTest`), lint, `assembleProdRelease`, design rules
- **Max feedback latency:** ~60 seconds per task

---

## Per-Task Verification Map

To be filled from PLAN.md tasks. Requirement → test mapping (from 15-RESEARCH.md § Validation Architecture):

| Plan / Task | Requirement | Behavior | Test Type | Automated Command | File Exists | Status |
|-------------|-------------|----------|-----------|-------------------|-------------|--------|
| 15-01 T2 (and per-screen plans 15-05 to 15-20) | POL-01 | Loading / empty / error+Retry per screen; `ErrorState` semantics | Robolectric Compose | `--tests '*ControlsTest'` + per-screen `*ScreenTest` | ✅ | ✅ green |
| 15-01 T1; 15-11 T1-T2; 15-12 T1-T3; 15-13 T1-T3; 15-16 T1 | POL-01 | Refresh-failed snackbar has Retry; one at a time | Robolectric Compose | `--tests '*RefreshFailedSnackbarTest'` + per-screen tests | ✅ | ✅ green |
| 15-05 T2; 15-06 T1 | POL-02 | OTP/secret keys absent from SavedStateHandle | JVM unit | `--tests '*SignupVerifyEmailViewModelTest' --tests '*SetNewPasswordViewModelTest'` | ✅ | ✅ green |
| 15-15 T1-T3; 15-03 T1; 15-14 T1 | POL-02 | SavedStateHandle round-trip for Settings, Tax, Delete forms (and the sheets) | JVM unit | `--tests '*ClientProfileSettingsViewModelTest'` etc. | ✅ | ✅ green |
| 15-10 T2; 15-11 T3 | POL-02 | Chat composer restored per thread | JVM unit | `--tests '*ChatThreadViewModelTest'` | ✅ | ✅ green |
| 15-08 T2; 15-09 T1 | POL-02 | Call layer never auto-joins after restore | Robolectric Compose | `--tests '*CallOverlayHostTest'` | ✅ | ✅ green |
| 15-02 T2; 15-03 T1; 15-05 T1; 15-07 T1; 15-10 T2; 15-13 T3; 15-14 T3; 15-19 T1 | POL-03 | IME inset keeps key controls above keyboard | Robolectric Compose + `InsetsTestSupport` | `--tests '*InsetsTest'` | ✅ | ✅ green |
| 15-02 T1 (and each screen plan 15-05 to 15-20) | POL-03 | Cutout + `contentMaxWidth()` on large window | Robolectric Compose (`w900dp`) | `--tests '*ContentMaxWidthTest'` + per-screen `capped` tests | ✅ | ✅ green |
| 15-03 T2 | POL-03 | Insets rule + R7 pass | Script | `bash scripts/check-design-rules.sh` and `check-design-rules-selftest.sh` | ✅ | ✅ green |
| 15-02 T1 | POL-03 | Manifest orientation declaration | JVM unit | `--tests '*ManifestOrientationTest'` | ✅ | ✅ green |
| 15-04 T1, T3; 15-05 T1; 15-07 T1-T2; 15-08 T1-T2; 15-10 T1 | POL-04 | Four flows expose names/roles/states/headings/order | Robolectric Compose + `A11yAudit` | `--tests '*TalkBackFlowTest'` (Login, Booking, Call, Chat) | ✅ | ✅ green |
| 15-04 T3 | POL-04 | Palette contrast ratios | JVM unit | `--tests '*PaletteContrastTest'` | ✅ | ✅ green |
| 15-17 T2 | POL-01..POL-04 | Whole-phase gate: both flavors, ktlint, three debug assembles, lint, R8 release | Gradle | see "Phase gate (15-17)" below | ✅ | ✅ green |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [x] `app/src/test/.../testing/A11yAudit.kt` — semantics audit helper (POL-04) (15-04 T3)
- [x] `app/src/test/.../testing/InsetsTestSupport.kt` — IME and cutout dispatch helpers (POL-03) (15-02 T2)
- [x] `RefreshFailedSnackbarTest` (15-01 T1), `ContentMaxWidthTest` and `ManifestOrientationTest` (15-02 T1), `PaletteContrastTest` (15-04 T3)
- [x] `scripts/check-design-rules.sh` insets rule + failing fixture (15-03 T2, `check-design-rules-selftest.sh`)
- [x] Framework install: none (no new dependency)

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Real TalkBack reading of Login, Booking, Call, Chat | POL-04 | Compose ATF checks no-op under Robolectric; no androidTest set | `15-DEVICE-CHECKLIST.md` |
| Font scale 200%, large window | POL-03/04 | Device rendering | `15-DEVICE-CHECKLIST.md` |
| "Don't keep activities" + rotation restore; R8 release restore | POL-02 | Process death + minified build on device | `15-DEVICE-CHECKLIST.md` |

Device checklist is logged as open items and does not block (D-14, manual-testing-deferred).

---

## D-03 reading of SC-2

With phones locked to portrait, "rotating the device" means any Activity recreation (dark or font-scale change, window resize, "Don't keep activities", process death), and release and debug builds behave the same (D-03).

Record this reading in VERIFICATION.md (CONTEXT D-03). SC-2 is therefore proven by the `StateRestorationTester` / `SavedStateHandle` tests, the manifest and `ContentMaxWidthTest` guards, and the open device rows Y1-Y7 and Y11 of `15-DEVICE-CHECKLIST.md`; no debug-only rotation exists.

## Phase gate (15-17)

Run 2026-10-29 on the phase worktree (JDK from Android Studio JBR, `-Dorg.gradle.jvmargs=-Xmx4g -Pkotlin.daemon.jvmargs=-Xmx3g`):

| Step | Result |
|------|--------|
| `testDevDebugUnitTest` | 4316 tests, 0 failures, 0 skipped |
| `testProdDebugUnitTest` | 4316 tests, 0 failures, 0 skipped |
| `ktlintCheck`, `assembleDevDebug`, `assembleProdDebug`, `:spikes:stripe:assembleDebug` | BUILD SUCCESSFUL (one invocation with the tests, 6m 35s) |
| `check-design-rules.sh` (app/src), `check-design-rules.sh spikes`, `check-design-rules-selftest.sh` | "Design rules: OK" twice, "Design rules self-test: OK" |
| `lintDevDebug lintProdDebug` | First run found 1 error (`LocalContextGetResourceValueCall` in `ChatThreadScreen.kt`, `PeerArrivalAnnouncer`); fixed in commit de321fb5 by reading the string through `LocalResources.current`; rerun BUILD SUCCESSFUL, 0 errors |
| Chat tests and `ktlintCheck` after the lint fix | BUILD SUCCESSFUL |
| `assembleProdRelease` (R8) | BUILD SUCCESSFUL (3m 52s), unsigned APK about 129 MB |

Sweeps over `app/src/main` (counts): action-less refresh snackbars 0 (baseline 12); `"verify_code"`/`"reset_code"` literals 0 (baseline 2); `ConnectivityManager`/`NetworkCallback` files 0; `requestedOrientation` 0; `android:screenOrientation` in the manifest 1 (MainActivity); `// insets-ok:` opt-outs with no reason 0 (none used in `app/src`); informative `onSurfaceMuted` text on `surfaceContainerHighest` or `surfaceBright` 0 (UI-SPEC A9: `BookingRequestSentDetailsCard` uses Highest for an icon tile and the status pill, and its muted tint is on a decorative icon).

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency < 60s
- [x] `nyquist_compliant` set to true in frontmatter

**Approval:** approved 2026-10-29 on the automated gate (plan 15-17). The three manual-only rows above stay open in `15-DEVICE-CHECKLIST.md` and do not gate phase close (D-14).

---
phase: "15"
slug: "polish-hardening"
# status lifecycle: draft (seeded by plan-phase) → validated (set by validate-phase §6)
# audit-milestone §5.5 distinguishes NOT-VALIDATED (draft) from PARTIAL (validated + nyquist_compliant: false) (#2117)
status: draft
nyquist_compliant: false
wave_0_complete: false
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

| Requirement | Behavior | Test Type | Automated Command | File Exists | Status |
|-------------|----------|-----------|-------------------|-------------|--------|
| POL-01 | Loading / empty / error+Retry per screen; `ErrorState` semantics | Robolectric Compose | `--tests '*ControlsTest'` + per-screen `*ScreenTest` | partial | ⬜ pending |
| POL-01 | Refresh-failed snackbar has Retry; one at a time | Robolectric Compose | `--tests '*RefreshFailedSnackbarTest'` | ❌ W0 | ⬜ pending |
| POL-02 | OTP/secret keys absent from SavedStateHandle | JVM unit | `--tests '*SignupVerifyEmailViewModelTest' --tests '*SetNewPasswordViewModelTest'` | exists (update) | ⬜ pending |
| POL-02 | SavedStateHandle round-trip for Settings, Tax, Delete forms | JVM unit | `--tests '*ClientProfileSettingsViewModelTest'` etc. | exists (new cases) | ⬜ pending |
| POL-02 | Chat composer restored per thread | JVM unit | `--tests '*ChatThreadViewModelTest'` | exists (new cases) | ⬜ pending |
| POL-02 | Call layer never auto-joins after restore | Robolectric Compose | `--tests '*CallOverlayHostTest'` | exists (new cases) | ⬜ pending |
| POL-03 | IME inset keeps key controls above keyboard | Robolectric Compose + `InsetsTestSupport` | `--tests '*InsetsTest'` | ❌ W0 | ⬜ pending |
| POL-03 | Cutout + `contentMaxWidth()` on large window | Robolectric Compose (`w900dp`) | `--tests '*ContentMaxWidthTest'` | ❌ W0 | ⬜ pending |
| POL-03 | Insets rule + R7 pass | Script | `bash scripts/check-design-rules.sh` | extend | ⬜ pending |
| POL-03 | Manifest orientation declaration | JVM unit | `--tests '*ManifestOrientationTest'` | ❌ W0 | ⬜ pending |
| POL-04 | Four flows expose names/roles/states/headings/order | Robolectric Compose + `A11yAudit` | `--tests '*TalkBackFlowTest'` | ❌ W0 | ⬜ pending |
| POL-04 | Palette contrast ratios | JVM unit | `--tests '*PaletteContrastTest'` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `app/src/test/.../testing/A11yAudit.kt` — semantics audit helper (POL-04)
- [ ] `app/src/test/.../testing/InsetsTestSupport.kt` — IME and cutout dispatch helpers (POL-03)
- [ ] `RefreshFailedSnackbarTest`, `ContentMaxWidthTest`, `PaletteContrastTest`, `ManifestOrientationTest`
- [ ] `scripts/check-design-rules.sh` insets rule + failing fixture
- [ ] Framework install: none (no new dependency)

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Real TalkBack reading of Login, Booking, Call, Chat | POL-04 | Compose ATF checks no-op under Robolectric; no androidTest set | `15-DEVICE-CHECKLIST.md` |
| Font scale 200%, large window | POL-03/04 | Device rendering | `15-DEVICE-CHECKLIST.md` |
| "Don't keep activities" + rotation restore; R8 release restore | POL-02 | Process death + minified build on device | `15-DEVICE-CHECKLIST.md` |

Device checklist is logged as open items and does not block (D-14, manual-testing-deferred).

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 60s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending

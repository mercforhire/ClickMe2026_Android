---
phase: 07-payment-methods-paid-booking
plan: 01
subsystem: payments
tags: [stripe, dependency, r8, di, appearance]
requires: []
provides:
  - "Stripe paymentsheet 23.24.0 in :app (dev debug and R8 prod release build)"
  - "PaymentConfiguration.init at app start"
  - "StripePaymentMethodReader seam bound in Hilt (StripeModule)"
  - "clickMeSheetConfiguration, clickMePaymentSheetAppearance, rememberClickMePaymentSheetAppearance"
affects: [07-09, 07-13, 13-data-safety, 17-release]
tech-stack:
  added: ["com.stripe:paymentsheet 23.24.0 (already in the version catalog; first use in :app)"]
  patterns: ["SDK behind an injectable interface so ViewModels never import Stripe", "equal light/dark Colors to pin a dark palette"]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/stripe/StripePaymentMethodReader.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/stripe/ClickMePaymentSheetStyle.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/di/StripeModule.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/testing/FakeStripePaymentMethodReader.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/stripe/ClickMePaymentSheetStyleTest.kt
  modified:
    - app/build.gradle.kts
    - app/src/main/java/com/uptrendinvestments/clickmeapp/ClickMeApplication.kt
key-decisions:
  - "rules.keep left untouched: R8 prodRelease passed with no missing_rules.txt, so no -dontwarn lines were added"
  - "Stripe version not bumped (23.24.0 stays)"
requirements-completed: [PAY-02, PAY-03, PAY-05]
duration: ~35min
completed: 2026-10-22
---

# Phase 7 Plan 01: Stripe SDK seams Summary

Stripe paymentsheet 23.24.0 is now compiled into `:app` (R8 release passes with no extra keep rules), initialized once at app start, with an injectable SetupIntent `pm_` reader and one shared dark-palette PaymentSheet configuration for later plans.

## Tasks

| Task | Name | Commit |
| ---- | ---- | ------ |
| 1 | Stripe dependency, app-start init and R8 release build | c895ba5 |
| 2 | pm_ reader seam, shared sheet configuration and dark appearance | 207fc2b |

## Verification

- `./gradlew testDevDebugUnitTest ktlintCheck assembleDevDebug assembleProdRelease` exits 0 (full suite, not only the filter).
- `ClickMePaymentSheetStyleTest`: 6 tests, 0 failures (configuration, label, light==dark, theme colors, shapes/typography/button, fake reader).
- `bash scripts/check-design-rules.sh` prints "Design rules: OK".
- All grep acceptance criteria hold (dependency line 1, `stripe = "23.24.0"` unchanged, init line, stale comment gone, dev key unchanged, no `stripeAccountId` in reader code, no `UserInterfaceStyle`).

## Data-safety inputs (T-07-04; Phase 13/17)

Merged-manifest `uses-permission` delta, prodRelease, before vs after adding Stripe:

| Permission | Before | After |
| ---------- | ------ | ----- |
| android.permission.INTERNET | yes | yes |
| com.uptrendinvestments.clickmeapp.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION | yes | yes |
| android.permission.ACCESS_NETWORK_STATE | no | added |
| android.permission.NFC | no | added |
| android.permission.VIBRATE | no | added |

prodRelease APK (unsigned, R8 on): 8,787,672 bytes (about 8.4 MiB) before, 19,202,108 bytes (about 18.3 MiB) after; +10.4 MB (about 2.2x). Worth knowing for Phase 17 size budgeting; the NFC permission is the one to justify on the data safety form (card scanning/NFC support in the Stripe UI).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] `PaymentSheet.Typography.default.copy(...)` does not exist**
- **Found during:** Task 2 (API check against the 23.24.0 sources jar)
- **Issue:** `Typography` is a Poko class, not a data class, so there is no `copy`.
- **Fix:** `PaymentSheet.Typography.Builder().fontResId(R.font.nunito).build()`; same result (default scale, Nunito).
- **Commit:** 207fc2b

**2. [Rule 3 - Blocking] Stripe config/appearance members are module-internal**
- **Issue:** The planned test could not read `merchantDisplayName`, `allowsDelayedPaymentMethods`, `colorsLight`/`colorsDark`, etc. from `:app` tests.
- **Fix:** The test compares through Poko `equals` against an independently built expected object and reads the mangled internal getters reflectively (`member(...)` helper) for the light==dark and configuration-field assertions.
- **Commit:** 207fc2b

Other API confirmations (RESEARCH A5/A8/A9): `PrimaryButton`, `PrimaryButtonColors` (Color overload), `PrimaryButtonShape(cornerRadiusDp)` and `Shapes(cornerRadiusDp, borderStrokeWidthDp)` names match 23.24.0 with no renames. `retrieveSetupIntentSynchronous(clientSecret)` returns a non-null `SetupIntent` (use `.paymentMethodId`), throws `StripeException` subclasses; no signature change needed.

TDD note: RED and GREEN landed in one commit for Task 2 because the RED state was a compile failure (types did not exist yet), which cannot be committed without breaking the build.

## Known Stubs

None.

## Threat Flags

None beyond the plan's threat model. T-07-02 and T-07-03 are met in code (secret not logged, no `stripeAccountId`); T-07-04 data is recorded above.

## Self-Check: PASSED

- Files exist: all 7 created/modified source files and this SUMMARY.
- Commits exist: c895ba5, 207fc2b.
- STATE.md and ROADMAP.md untouched.

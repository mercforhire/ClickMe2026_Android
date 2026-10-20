---
phase: 02-networking-session-sdk-spikes
plan: 04
subsystem: payments-spike
tags: [spike, stripe, paymentsheet, payment-launcher, gradle-modules, ci]

requires:
  - phase: 02-networking-session-sdk-spikes
    provides: Version catalog entries stripe-paymentsheet and agora-voice-sdk (plan 02-01)
provides:
  - ":spikes:stripe and :spikes:agora throwaway application modules (no :app dependency)"
  - "Stripe spike code for SetupIntent card save and 3DS on the connected account (Path A)"
  - "Agora spike skeleton (login form, AgoraSpikeApi with login and joinCall) for plan 02-08"
  - "CI assembles both spikes; design-rules and secret scan cover spikes/"
affects: [02-08, 02-09, 07-payments, 09-voice]

tech-stack:
  added: []
  patterns:
    - "Optional ~/.clickme/spike-inputs.properties read via providers.fileContents into BuildConfig String fields (empty when absent)"
    - "rememberPaymentLauncher driven from LaunchedEffect(challenge) because stripeAccountId is a remember key"

key-files:
  created:
    - spikes/.gitignore
    - spikes/stripe/build.gradle.kts
    - spikes/stripe/src/main/AndroidManifest.xml
    - spikes/stripe/src/main/java/com/uptrendinvestments/clickmeapp/spike/stripe/StripeSpikeApi.kt
    - spikes/stripe/src/main/java/com/uptrendinvestments/clickmeapp/spike/stripe/StripeSpikeViewModel.kt
    - spikes/stripe/src/main/java/com/uptrendinvestments/clickmeapp/spike/stripe/MainActivity.kt
    - spikes/agora/build.gradle.kts
    - spikes/agora/src/main/AndroidManifest.xml
    - spikes/agora/src/main/java/com/uptrendinvestments/clickmeapp/spike/agora/AgoraSpikeApi.kt
    - spikes/agora/src/main/java/com/uptrendinvestments/clickmeapp/spike/agora/MainActivity.kt
  modified:
    - settings.gradle.kts
    - .github/workflows/ci.yml
    - scripts/check-design-rules.sh

key-decisions:
  - "PaymentSheet.Builder(callback) inside remember { } followed by .build() compiles against Stripe 23.24.0, so the rememberPaymentSheet fallback (A8) was not needed"
  - "Payment-intent and confirm success statuses are logged as the contract values (200 and 201) since the untyped Map return does not expose the HTTP code"

patterns-established:
  - "Spike evidence log prefixes under logcat tag StripeSpike: login:, setup:, methods:, payment-intent:, confirm:, 3ds:, confirm-retry:"

requirements-completed: []
requirements-enabled: [FND-13, FND-14]  # code only; device proof arrives in plan 02-09, so not marked complete

duration: n/a
completed: 2026-10-20
---

# Phase 2 Plan 04: Stripe and Agora Spike Modules Summary

**Two independent throwaway spike apps with Stripe SetupIntent save plus PaymentLauncher 3DS on the connected account, CI-built and design-rules-scanned.**

## Performance

- **Tasks:** 2/2
- **Files created:** 10, modified: 3

## Accomplishments

- `:spikes:stripe` and `:spikes:agora` are separate application modules (own applicationIds, shared version catalog, no `:app` dependency), each with its own bare Retrofit `/auth/login` and in-memory token (D-10, D-11).
- Spike inputs are read from `~/.clickme/spike-inputs.properties` through `providers.fileContents`; the file is optional and absent values become empty BuildConfig strings, so builds work on CI and before the user creates the file.
- Stripe spike implements the full D-12 sequence: SetupIntent via PaymentSheet, card list preselecting the `3184` test card, payment-intent with a fresh `booking_attempt_id` per tap, server-side confirm with exactly six keys, `402 PAYMENT_REQUIRES_ACTION` mapped to an `Sca` state handled by `PaymentLauncher.handleNextActionForPaymentIntent` on `detail.stripe_account`, then the identical confirm body is retried. `stripe_account` is cleared in `onPaymentResult` and never persisted. The app never confirms the PaymentIntent itself (Path A).
- CI appends both spike `assembleDebug` tasks and a `check-design-rules.sh spikes` step; the secret scan path list is now `app spikes gradle ./*.kts`.

## Task Commits

1. **Task 1: Spike infrastructure and both module skeletons** - `f8ea299` (feat)
2. **Task 2: Stripe spike SetupIntent save and 3DS flow** - `2aa4472` (feat)

## Verification

- `./gradlew ktlintCheck :spikes:stripe:assembleDebug :spikes:agora:assembleDebug assembleDevDebug` exits 0.
- `bash scripts/check-design-rules.sh` and `bash scripts/check-design-rules.sh spikes` both print "Design rules: OK".
- All Task 1 and Task 2 acceptance greps pass (no pk_live, exactly one pk_test line, no HttpLoggingInterceptor, no client-side confirm APIs, no SharedPreferences/DataStore, `3184` and `handleNextActionForPaymentIntent` present).
- Not run on a device: device evidence is plan 02-09 and is blocked on the deferred inputs file.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Gradle script needs explicit imports for java.util / java.io**
- **Found during:** Task 1
- **Issue:** In a build script `java` resolves to the Gradle `java` extension, so `java.util.Properties()` and `java.io.StringReader` failed to compile.
- **Fix:** Added `import java.io.StringReader` and `import java.util.Properties` at the top of both spike build files.
- **Files modified:** spikes/stripe/build.gradle.kts, spikes/agora/build.gradle.kts
- **Commit:** f8ea299

**2. [Rule 3 - Blocking] Spike build outputs would be untracked**
- **Found during:** Task 1
- **Issue:** Neither the root nor a module `.gitignore` covered `spikes/*/build`.
- **Fix:** Added `spikes/.gitignore` with `*/build/`.
- **Files modified:** spikes/.gitignore
- **Commit:** f8ea299

**3. [Rule 1 - cosmetic] ktlintFormat rewrote a null check awkwardly**
- **Fix:** Replaced the inline `if (token != null)` expression in both login handlers with a `hasToken` local before commit.

No auth gates occurred.

## Known Stubs

None. The Agora `MainActivity` is intentionally login-only; plan 02-08 adds the call flow (documented in the plan).

## Threat Flags

None. No surface beyond the plan's threat model. Mitigations applied: only the dev `pk_test` key is present (T-02-09), no logging interceptor and no token, client_secret, ephemeral_key or stripe_account values in logs (T-02-10), no client-side PaymentIntent confirmation (T-02-11), no persistence of stripe_account (T-02-12), no `:app` dependency and debug builds only (T-02-13).

## Notes for Plan 02-09

- `~/.clickme/spike-inputs.properties` does not exist yet; the Stripe spike's booking fields are editable on screen, so a run is possible by typing the values, but the file should be created before rebuilding for convenience.
- `book()` logs `payment-intent: 200` and `confirm: 201` as literal contract values; a different 2xx would still be mislabeled in the log text.

## Self-Check: PASSED

- All 10 created files exist; commits f8ea299 and 2aa4472 present in git log.

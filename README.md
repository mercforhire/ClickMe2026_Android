# ClickMe for Android

ClickMe is a marketplace where clients book paid or free voice or video sessions with experts. This repository is the
native Android app (Kotlin, Jetpack Compose), a port of the live iOS app (SwiftUI). Both apps share the same REST and
Socket.IO backend, which is private and not part of this repository. The iOS app lives in a separate repository. The
Android app is published on Google Play (versionName 1.0.0, application ID `com.uptrendinvestments.clickmeapp`).

Users switch between a client mode and an expert mode inside the same app.

## Features

### Client mode

- Explore experts by category, and search
- Expert profiles with topics and reviews, and a favorites list
- Booking flow: pick a topic, a date and a time slot. Paid bookings go through Stripe PaymentSheet, free bookings skip
  payment
- Booking summary and request-sent screens
- My Bookings, and an upcoming booking screen with a Join button that follows the join window
- Cancellation, reschedule, writing reviews and private booking notes
- Payment history

### Expert mode

- Dashboard and availability
- Topics the expert offers
- Incoming requests with accept or decline
- Upcoming and confirmed sessions, with reschedule and cancel
- Client profiles and private booking notes
- Stripe Connect payout onboarding in a Custom Tab, with a deep-link return into the app
- Earnings transactions and a tax profile

### Shared

- Multi-step signup with profile photo, tags, timezone and email verification
- Login, forgot password and password reset, and switching between client and expert mode
- Real-time chat with new messages, read receipts, typing indicators and online status, plus report and block
- In-app voice calls over the Agora voice SDK, kept alive by a microphone foreground service with an ongoing
  notification
- Video sessions use an external meeting link (Skype or Zoom) that the app opens
- FCM push notifications that open the matching screen, a notification center and notification preferences
- Saved payment methods, account security, help, feedback and account deletion

## Tech stack

| Area | Library | Version |
|------|---------|---------|
| Language | Kotlin, Java 11 bytecode | 2.4.21 |
| Build | Android Gradle Plugin, Gradle, KSP | 9.4.1, 9.6.0, 2.3.12 |
| SDK levels | Android API levels | `minSdk 26`, `targetSdk 37`, `compileSdk 37` |
| UI | Jetpack Compose BOM, Material 3, Material icons | 2026.09.00 |
| Navigation | Navigation 3, Lifecycle | 1.2.0, 2.11.0 |
| Dependency injection | Hilt | 2.60.1 |
| Networking | Retrofit, OkHttp, Moshi with KSP codegen | 3.0.0, 5.5.0, 1.15.2 |
| Realtime | Socket.IO client | 2.1.2 |
| Payments | Stripe Android PaymentSheet | 23.24.0 |
| Calls | Agora voice SDK | 4.6.4 |
| Push | Firebase BoM, Firebase Cloud Messaging | 35.1.0 |
| Images | Coil | 3.6.3 |
| Storage | DataStore Preferences, EncryptedSharedPreferences via security-crypto | 1.2.1, 1.1.0 |
| Code style | ktlint Gradle plugin | 14.2.0 |
| Static analysis | Android Lint with abortOnError | Gradle plugin bundled |
| Tests | JUnit, Robolectric, Mockito | 4.13.2, 4.17, 5.24.0 |
| Test support | kotlinx-coroutines-test, OkHttp MockWebServer, Compose UI test | 1.11.0, 5.5.0, BOM |

## Architecture

ClickMe is a single-activity Compose app (`MainActivity.kt`, with `ClickMeApplication.kt` as the Hilt application). It
follows MVVM: each screen is a composable plus a Hilt view model that exposes its UI state as a StateFlow. Per-feature
repositories sit on top of the Retrofit API in `network/ClickMeApi.kt`, Hilt modules live in `di/`, and Navigation 3
back stacks are kept per tab in `navigation/HomeTabScaffold.kt`. Kotlin coroutines and Flow are used throughout.

The package layout mirrors the iOS app's folder tree, so a feature can be compared file for file across the two apps.

### Package layout

The package root is `app/src/main/java/com/uptrendinvestments/clickmeapp/`. Screen folders hold a Screen, a ViewModel
and a subviews folder.

| Package | Contents |
|---------|----------|
| `screens/` | Feature screens: client mode, expert mode, and the shared flows (signup, login, chat, call, settings) |
| `components/` | Reusable composables: buttons, avatars, badges, validated fields, skeletons |
| `managers/` | App-wide state and rules: session, tokens, join window, call and push managers |
| `network/` | Retrofit API, OkHttp setup, token authenticator and refresher |
| `realtime/` | Socket.IO connection, event decoding and connection lifecycle |
| `model/` | Request and response models and the enums that mirror server values |
| `services/` | Android services: the call foreground service and FCM push handling |
| `stripe/` | Stripe PaymentSheet styling, result mapping and failure logging |
| `navigation/` | Navigation 3 hosts, keys and navigators for auth, signup and the home tabs |
| `di/` | Hilt modules |
| `ui/theme/` | Colors, typography and the Material theme |
| `util/` | Formatting and small helpers, including currency formatting |
| `config/` | App configuration and links |
| `constants/` | Static lists such as countries, timezones and category icons |

Repo-level folders:

- `app/src/test/`: JVM unit tests
- `scripts/`: CI scripts, including the design-rule checks
- `spikes/stripe/`: a standalone Stripe PaymentSheet spike module
- `.github/workflows/ci.yml`: the CI workflow

## Engineering highlights

- Join window: `managers/JoinWindow.kt` opens Join 3600 seconds before the start. The first-join window lasts at
  least 3600 seconds after the start, and a party who already joined may reconnect up to 3600 seconds after the
  scheduled end. Only confirmed, in-progress and completed bookings enable Join, and a clock re-evaluates every 30
  seconds. The rules mirror the backend and the iOS app.
- Currency formatting: `util/Money.kt` takes amounts as integer minor units plus an ISO 4217 code. Decimals come from
  `java.util.Currency` (none for JPY, three for KWD), separators follow the device locale, and the USD and CAD symbols
  are pinned to US$ and CA$ so the two currencies never render alike.
- Token refresh: `network/TokenAuthenticator.kt` is the single OkHttp Authenticator. It is single-flight behind a
  coroutine Mutex and retries once. `network/TokenRefresher.kt` performs the refresh, and the rotated token pair is
  persisted before the retry because the backend revokes the session when a refresh token is reused. Network errors,
  429 and 5xx keep the session. `managers/EncryptedPrefsTokenStore.kt` keeps tokens in EncryptedSharedPreferences and
  recovers from a corrupt file or a lost Keystore key.
- Realtime lifecycle: `realtime/RealtimeConnectionManager.kt` keeps the socket up while the user is logged in and the
  app is in the foreground, keeps it up in the background during an active call, and reconnects after every token
  refresh.
- Calls: `services/call/CallService.kt` is a foreground service of type microphone, so a voice call survives leaving
  the app, with an ongoing notification.
- Design-rule checks in CI: `scripts/check-design-rules.sh` fails the build on color literals outside
  `ui/theme/Color.kt`, per-screen Brand types, Stripe secret-key patterns, and several window-inset and keyboard
  mistakes. Its self-test is `scripts/check-design-rules-selftest.sh`.
- Build flavors: dev and prod product flavors in `app/build.gradle.kts`, each with its own API base URL, Stripe
  publishable key and Agora app ID BuildConfig fields. The dev flavor can point at a local backend through an optional
  local override.
- Optional Firebase config: the Google Services plugin is applied only when the Firebase config file exists, so fresh
  clones and CI build without it.

## Getting started

### Requirements

- Android Studio with Android SDK 37
- A JDK that runs AGP 9.4 (CI uses Temurin JDK 25)

### Build and run

Clone the repository, open it in Android Studio, and build from the command line or the IDE:

```shell
./gradlew assembleDevDebug
./gradlew installDevDebug
```

The dev flavor targets the development configuration and the prod flavor the production one. The dev flavor ships a
Stripe test-mode key, so paid bookings work only against a backend in Stripe test mode. Setting `clickme.devBaseUrl` in
`local.properties` points the dev flavor at a local backend.

Push notifications need your own `google-services.json` in the `app` folder, which is gitignored. The build still works
without it, but the app then has no push.

### Release builds

```shell
./gradlew bundleProdRelease
```

Release signing reads a local `keystore.properties` at the repo root. It is gitignored and never committed. Without it
the release build is unsigned.

## Testing and code quality

```shell
./gradlew testDevDebugUnitTest
./gradlew ktlintCheck
./gradlew lintDevDebug
bash scripts/check-design-rules.sh
bash scripts/check-design-rules-selftest.sh
```

The project has 400 unit test files. They are JVM tests, with Robolectric for Android and Compose code and MockWebServer
for the API layer. Most are in `app/src/test/`, with small flavor-specific sets in `app/src/testDev/` and
`app/src/testProd/`.

## Continuous integration

`.github/workflows/ci.yml` runs on every push and pull request on Ubuntu with Temurin JDK 25. It runs the design rules
for the app and spike sources, then ktlint, lint for both flavors, unit tests for both flavors, and debug builds of both
flavors and the Stripe spike module. On failure it uploads the reports. CI has no signing material and builds unsigned
debug variants only.

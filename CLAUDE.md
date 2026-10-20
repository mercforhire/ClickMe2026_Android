# ClickMe Android — port from iOS

## iOS reference
`ios-reference/` is a symlink to the iOS codebase. Treat the Swift
code as the source of truth for feature behavior and business logic.
Deviate only for Android platform conventions (see "Platform swaps"
below).

## Architecture
- Jetpack Compose + Material 3
- MVVM with Compose State
- Kotlin coroutines (not RxJava)
- Hilt for DI
- Retrofit + Moshi for networking
- EncryptedSharedPreferences for JWT storage
- Firebase Messaging for push
- Agora Android SDK for voice
- Stripe Android SDK with PaymentSheet
- minSdk 26, targetSdk 37, compileSdk 37

## Platform swaps (iOS → Android)
- `.sheet` → `ModalBottomSheet`
- iOS back chevron → `TopAppBar` with back arrow
- `.alert` → Material `AlertDialog`
- iOS switch → Material `Switch`
- `PhotosPicker` → `rememberLauncherForActivityResult(PickVisualMedia)`
- `.toolbar` nav title → `TopAppBar` title slot
- `.task` on appear → `LaunchedEffect(Unit)`

## File organization
Mirror the iOS folder tree. Example:
`ClickMe2026/Views/Screens/Signup Flow/Initial/SignupInitialView.swift`
→ `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/initial/SignupInitialScreen.kt`
+ `SignupInitialViewModel.kt`

## Backend
Same REST contract as iOS. `backend-reference/` is a symlink to the
backend repo; see `backend-reference/docs/ios/` for the
authoritative specs (user reporting, chat unblock, earnings
breakdown, signup username removal, etc.)

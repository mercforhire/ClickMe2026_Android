# Decisions (from ADRs)

Source ADRs: 1
- source: /Users/LeonChen/Documents/ClickMe_Android/CLAUDE.md
  title: ClickMe Android — port from iOS
  status: LOCKED (locked: true, precedence: 0, type set by ingest manifest)

Each decision below is LOCKED. No other ingested source can override it. Lower-precedence sources (the PRD) can only add to it.

---

## DEC-ios-source-of-truth: iOS code is the source of truth for behavior
- source: /Users/LeonChen/Documents/ClickMe_Android/CLAUDE.md (section "iOS reference")
- status: LOCKED
- scope: feature behavior, business logic
- decision: `ios-reference/` (symlink to the iOS codebase, ../ClickMe2026) is the source of truth for feature behavior and business logic. Deviate only for Android platform conventions (see DEC-platform-swaps).
- application note (synthesizer): the PRD lists iOS defects that Android should not copy. Each one is backed by a backend spec this ADR names as authoritative (DEC-backend-contract), by the iOS code's own stated intent, or is a wiring defect in an iOS feature that otherwise exists. These are treated as consistent with this decision. See INGEST-CONFLICTS.md, INFO "iOS-defect fixes are consistent with the locked ADR".

## DEC-ui-compose-m3: UI toolkit
- source: /Users/LeonChen/Documents/ClickMe_Android/CLAUDE.md (section "Architecture")
- status: LOCKED
- scope: UI
- decision: Jetpack Compose + Material 3.

## DEC-mvvm-compose-state: Presentation architecture
- source: /Users/LeonChen/Documents/ClickMe_Android/CLAUDE.md (section "Architecture")
- status: LOCKED
- scope: presentation layer
- decision: MVVM with Compose State.

## DEC-coroutines: Concurrency
- source: /Users/LeonChen/Documents/ClickMe_Android/CLAUDE.md (section "Architecture")
- status: LOCKED
- scope: async/concurrency
- decision: Kotlin coroutines. Not RxJava.

## DEC-hilt: Dependency injection
- source: /Users/LeonChen/Documents/ClickMe_Android/CLAUDE.md (section "Architecture")
- status: LOCKED
- scope: DI
- decision: Hilt.

## DEC-retrofit-moshi: Networking
- source: /Users/LeonChen/Documents/ClickMe_Android/CLAUDE.md (section "Architecture")
- status: LOCKED
- scope: networking, JSON
- decision: Retrofit + Moshi.

## DEC-jwt-encrypted-sharedprefs: Token storage
- source: /Users/LeonChen/Documents/ClickMe_Android/CLAUDE.md (section "Architecture")
- status: LOCKED
- scope: JWT storage
- decision: EncryptedSharedPreferences for JWT storage.
- note: Google has deprecated the androidx.security-crypto library that provides EncryptedSharedPreferences. The decision stays locked. See INGEST-CONFLICTS.md, INFO "Locked token-storage library is deprecated upstream".

## DEC-fcm: Push
- source: /Users/LeonChen/Documents/ClickMe_Android/CLAUDE.md (section "Architecture")
- status: LOCKED
- scope: push notifications
- decision: Firebase Messaging.

## DEC-agora: Voice
- source: /Users/LeonChen/Documents/ClickMe_Android/CLAUDE.md (section "Architecture")
- status: LOCKED
- scope: voice calls
- decision: Agora Android SDK.

## DEC-stripe-paymentsheet: Payments
- source: /Users/LeonChen/Documents/ClickMe_Android/CLAUDE.md (section "Architecture")
- status: LOCKED
- scope: payments
- decision: Stripe Android SDK with PaymentSheet.

## DEC-sdk-levels: Android SDK levels
- source: /Users/LeonChen/Documents/ClickMe_Android/CLAUDE.md (section "Architecture")
- status: LOCKED
- scope: build configuration
- decision: minSdk 26, targetSdk 37, compileSdk 37.

## DEC-platform-swaps: iOS to Android UI convention swaps
- source: /Users/LeonChen/Documents/ClickMe_Android/CLAUDE.md (section "Platform swaps (iOS → Android)")
- status: LOCKED
- scope: UI conventions
- decision:
  - `.sheet` → `ModalBottomSheet`
  - iOS back chevron → `TopAppBar` with back arrow
  - `.alert` → Material `AlertDialog`
  - iOS switch → Material `Switch`
  - `PhotosPicker` → `rememberLauncherForActivityResult(PickVisualMedia)`
  - `.toolbar` nav title → `TopAppBar` title slot
  - `.task` on appear → `LaunchedEffect(Unit)`

## DEC-file-organization: Mirror the iOS folder tree
- source: /Users/LeonChen/Documents/ClickMe_Android/CLAUDE.md (section "File organization")
- status: LOCKED
- scope: source layout
- decision: Mirror the iOS folder tree. Example:
  `ClickMe2026/Views/Screens/Signup Flow/Initial/SignupInitialView.swift`
  → `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/initial/SignupInitialScreen.kt` + `SignupInitialViewModel.kt`
- note: amended 2026-10-19 by the user. The package is `com.uptrendinvestments.clickmeapp` for both `applicationId` and `namespace`/source folders, matching the iOS bundle ID. Already applied in the repo and CLAUDE.md.

## DEC-application-id: Package name
- source: user decision at ingest (2026-10-19)
- status: LOCKED
- decision: `applicationId` and `namespace` are `com.uptrendinvestments.clickmeapp` (same as the iOS bundle ID). It can't change after the first Play upload.

## DEC-backend-contract: Same REST contract as iOS
- source: /Users/LeonChen/Documents/ClickMe_Android/CLAUDE.md (section "Backend")
- status: LOCKED
- scope: backend REST contract
- decision: Same REST contract as iOS. `backend-reference/` (symlink to ../ClickMe2026 Backend) is the backend repo. `backend-reference/docs/ios/` holds the authoritative specs (user reporting, chat unblock, earnings breakdown, signup username removal, etc.).
- verified present in backend-reference/docs/ios/ at ingest: CALL-END-SEMANTICS.md, CHAT-UNBLOCK.md, CLIENT-TRANSACTIONS.md, EMAIL-VERIFICATION-ON-REQUEST.md, EXPERT-EARNINGS-BREAKDOWN.md, EXPERT-EARNINGS-TRANSACTIONS.md, PHONE-NUMBER-REMOVED.md, PRICING-LIMITS.md, RESCHEDULE-PAYMENT-CONVERSION.md, SESSION-BUFFER.md, SIGNUP-USERNAME-OPTIONAL.md, STRIPE-INTEGRATION.md, USER-REPORTING.md (plus SocketSmokeTest.swift)

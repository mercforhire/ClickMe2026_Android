---
phase: 13-push-notifications-internal-testing
reviewed: 2026-10-28T00:00:00Z
depth: standard
files_reviewed: 82
files_reviewed_list:
  - app/build.gradle.kts
  - app/src/main/AndroidManifest.xml
  - app/src/main/java/com/uptrendinvestments/clickmeapp/ClickMeApp.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/ClickMeApplication.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/MainActivity.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/components/NotificationBellButton.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/di/PushModule.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/di/PushRoutingModule.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/di/Qualifiers.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/di/ReviewModule.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/FcmPushTokenRegistrar.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/PushPermissionGate.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/PushPreferences.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/PushRegistrationManager.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/PushTokenRegistrar.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/PushTokenSource.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/UserManager.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/requests/PushRequests.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/requests/ReviewRequests.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/NotificationPayloads.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/ReviewPayloads.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/navigation/Navigator.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/network/ApiRequestUrls.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/network/ClickMeApi.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/bookingsummary/BookingSummaryScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/bookingsummary/BookingSummaryUiState.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/bookingsummary/BookingSummaryViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/bookingsummary/subviews/BookingSummaryFeedback.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/ExploreScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/home/ClientBookingManagementEntries.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/home/HomeClientScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/MyBookingsScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/MyBookingsUiState.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/MyBookingsViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/subviews/PastBookingCard.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/reviews/ReviewLaunchRequest.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/reviews/ReviewsRepository.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/writereview/WriteReviewEntries.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/writereview/WriteReviewKey.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/writereview/WriteReviewRoute.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/writereview/WriteReviewScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/writereview/WriteReviewUiState.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/writereview/WriteReviewViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/writereview/subviews/ReviewStarInput.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/writereview/subviews/ReviewTextField.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/writereview/subviews/WriteReviewFooter.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/writereview/subviews/WriteReviewHeader.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/ExpertDashboardRoute.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/ExpertDashboardScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/home/HomeExpertScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/AuthErrorDialog.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/notifications/NotificationBodyParser.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/notifications/NotificationRows.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/notifications/NotificationsEntries.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/notifications/NotificationsGrouping.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/notifications/NotificationsKey.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/notifications/NotificationsRepository.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/notifications/NotificationsRoute.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/notifications/NotificationsScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/notifications/NotificationsUiState.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/notifications/NotificationsViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/notifications/subviews/NotificationRow.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/notifications/subviews/NotificationsListParts.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/push/PushDestination.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/push/PushDestinationResolver.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/push/PushEvent.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/push/PushLaunchRequest.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/push/PushPermissionRequester.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/push/PushRouteLookups.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/push/PushRoutingHost.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/push/PushRoutingViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/push/ShellRoute.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/push/ShellRouteViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/services/push/ClickMeMessagingService.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/services/push/NotificationChannels.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/services/push/PushMessageHandler.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/services/push/PushNotificationFactory.kt
  - app/src/main/res/drawable/ic_stat_clickme.xml
  - app/src/main/res/values/colors.xml
  - app/src/main/res/values/strings_booking_summary.xml
  - app/src/main/res/values/strings_my_bookings.xml
  - app/src/main/res/values/strings_notifications.xml
  - app/src/main/res/values/strings_push.xml
  - app/src/main/res/values/strings_write_review.xml
  - build.gradle.kts
  - gradle/libs.versions.toml
  - "backend: src/services/push.js (ClickMe2026 Backend, 6daf5c0..92b4827)"
findings:
  critical: 0
  warning: 4
  info: 6
  total: 10
status: issues_found
---

# Phase 13: Code Review Report

**Reviewed:** 2026-10-28
**Depth:** standard
**Files Reviewed:** 82 (81 Android files plus the backend `push.js` change)
**Status:** issues_found

## Summary

Reviewed the Phase 13 push, notifications list, push routing and reviews code, plus the backend `push.js` channel and
category change. Partially changed files were reviewed on their diff against `1c9c187`.

The overall quality is high:
- The exported `MainActivity` only reads whitelisted String extras, and every id is UUID-validated in `PushEvent`.
- `PendingIntent` is `FLAG_IMMUTABLE` and explicit, and only routing keys are copied into it.
- Titles, bodies, comments and tokens are kept out of every `toString`, and no logging was found in the push paths.
- The backend channel map matches `NotificationChannels`, and the `.strict()` register schema matches `RegisterDeviceTokenRequest`.
- Back-stack insertion in `Navigator.openRouted` is consistent with its stated dedupe invariants.

No crash-level or data-exposure defect was proven. The findings are four robustness and privacy gaps that should be fixed before wider testing, and six minor items.

Not verified here: whether tapping a system-displayed (background) notification always delivers the extras to `MainActivity` on a warm start. Android may not call `onNewIntent` for a MAIN/LAUNCHER intent when the task already exists. This needs the on-device check listed in the deferred items.

## Structural Findings (fallow)

None provided for this review.

## Narrative Findings (AI reviewer)

## Warnings

### WR-01: Token registrar holds its mutex across unbounded FCM Task awaits, so logout can hang

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/managers/FcmPushTokenRegistrar.kt:46-85` (with `PushTokenSource.kt:37-58`, `UserManager.kt:200-206`)
**Issue:**
- `register()` holds `lock` while it awaits `tokenSource.currentToken()`, which is `FirebaseMessaging.getToken().await()` with no timeout.
- `unregister()` takes the same `lock`. Its `withTimeoutOrNull(3s)` covers only the `DELETE`. `tokenSource.deleteToken().await()` runs inside the lock and outside that timeout.
- `UserManager.logout()` runs `unregister()` inside `NonCancellable` and flips `_isLoggedIn` only after it returns.
- If the FCM Task does not complete (offline, Play services slow, first-run FID registration pending), the foreground registration keeps the lock and a later logout blocks behind it.
- The same applies if `deleteToken()` itself hangs. The user taps Log out and nothing happens. A rejected or suspended session goes through the same `logout()`, so it also stays on the logged-in UI until the Task resolves.
- The class KDoc promises "logout never hangs offline", but only the `DELETE` is bounded.

I could not confirm that `getToken()` hangs offline, only that nothing here bounds it.

**Fix:**
- Fetch the token outside the lock and bound it. Hold the lock only around the session guard and the API call.
- Bound `deleteToken()` as well.

```kotlin
override suspend fun register(token: String?) {
    withContext(io) {
        val value = token ?: withTimeoutOrNull(TOKEN_TIMEOUT_MS) { tokenSource.currentToken() }
        if (value.isNullOrBlank()) return@withContext
        lock.withLock {
            if (!tokens.hasSession()) return@withLock
            val access = tokens.accessToken
            if (access != null && access == unregisteredAccessToken) return@withLock
            try { apiCall { api.registerDeviceToken(/* ... */) } } catch (_: ApiException) {}
        }
    }
}

override suspend fun unregister() {
    withContext(io) {
        lock.withLock {
            unregisteredAccessToken = tokens.accessToken
            withTimeoutOrNull(UNREGISTER_TIMEOUT_MS) {
                try { apiCall { api.deleteDeviceToken(preferences.deviceId()) } } catch (_: ApiException) {}
                tokenSource.deleteToken()
            }
        }
    }
}
```

### WR-02: Lock-screen privacy is applied to foreground pushes only; system-displayed pushes show full content

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/services/push/PushNotificationFactory.kt:99-100`, `services/push/NotificationChannels.kt:29-69`
**Issue:**
- `PushNotificationFactory.build` sets `VISIBILITY_PRIVATE` and a generic public version ("You have a new notification"). That applies only to notifications this app posts, which means pushes that arrive while the app is in the foreground.
- A push that arrives while the app is backgrounded is a backend `notification` message. FCM displays it directly and never calls `PushNotificationFactory`.
- Those notifications use the channel's default lock-screen visibility, so the full title and body (chat text, amounts, names) can appear on the lock screen. The same event is therefore hidden or exposed depending on app state.
- The channels never set a lock-screen visibility, so nothing makes the background path private.

**Fix:**
- Create the channels with the framework `NotificationChannel` (minSdk 26, so it is always available) and call `setLockscreenVisibility(Notification.VISIBILITY_PRIVATE)`. Android then shows "Contents hidden" for system-displayed pushes.
- Alternatively, or additionally, set `android.notification.visibility: 'private'` and a `publicVersion` equivalent in the backend payload.
- Existing channels keep their original settings, so this needs to land before the channels ship. Channel ids are permanent.

### WR-03: A release build without `google-services.json` succeeds silently and ships without push

**File:** `app/build.gradle.kts:12-18`
**Issue:**
- When `google-services.json` is missing, the build only calls `logger.warn(...)`. `assembleProdRelease` and `bundleProdRelease` still produce a signed artifact where `FirebaseApp.getApps()` is empty.
- That makes every `PushTokenSource` call a quiet no-op, so token registration and notification delivery stop with no visible error.
- The warning prints on every Gradle configuration, including debug builds. A release script that is not watching the log will not notice it.
- The only guard is the 13-14 release checklist.

**Fix:** Fail the build for release variants.

```kotlin
val hasGoogleServices = file("google-services.json").exists()
if (hasGoogleServices) apply(plugin = "com.google.gms.google-services")
gradle.taskGraph.whenReady {
    if (!hasGoogleServices && allTasks.any { it.name.contains("Release", ignoreCase = true) && (it.name.startsWith("bundle") || it.name.startsWith("assemble")) }) {
        throw GradleException("google-services.json is required for release builds (no push without it)")
    }
}
```

### WR-04: Submitting a review can be cancelled by a push-driven stack change, and the app then loses the result

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/writereview/WriteReviewViewModel.kt:122-137`, `navigation/Navigator.kt` `openRouted` (dedupe branch)
**Issue:**
- The submit runs in `viewModelScope`, so it dies with the Write Review nav entry. The UI blocks Back while `submitting`, but `Navigator.openRouted` and the push router do not check that.
- A push tap or a Notifications row tap can cut or pop the stack, which discards the entry and its ViewModel mid-request.
- The `POST /bookings/{id}/reviews` may already have been processed server side. `launchRequest.onSubmitted` then never runs, so `reviewedBookingIds` is not updated and no thanks or reload is triggered.
- The "Leave a review" button stays, and a second attempt hits the "already reviewed" path.

**Fix:**
- Run the submit and its hand-off in a scope that survives the entry, using an `@ApplicationScope` launch wrapped in `NonCancellable`.
- At minimum, make `submit()` call `launchRequest.onSubmitted(...)` from a non-cancellable block so a success is always recorded.

```kotlin
viewModelScope.launch {
    val ok = withContext(NonCancellable) {
        try { repository.submit(...); launchRequest.onSubmitted(key.bookingId, key.opener); true }
        catch (e: ApiException) { /* set submitError */ false }
    }
    if (ok) _uiState.update { it.copy(submitting = false, submitted = true) }
}
```

## Info

### IN-01: Logout after a rejected or suspended session sends an unauthenticated token DELETE

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/managers/FcmPushTokenRegistrar.kt:71-85`, `managers/UserManager.kt:131-134,204`
**Issue:**
- `SessionEvent.Rejected` and `SessionEvent.Suspended` reach `logout()` after the transport has already cleared the tokens (`TokenAuthenticator.rejectSession`).
- `unregister()` then issues `DELETE notifications/tokens/{id}` with no bearer, which costs a 401 round trip for nothing. Its KDoc says the call happens before the tokens are cleared, which is not true on this path.
- The backend row for the previous user stays bound to this device. It is neutralised only because `deleteToken()` invalidates the FCM token.

**Fix:** Skip the `DELETE` when `!tokens.hasSession()` and rely on `deleteToken()`. Correct the KDoc, and consider a server-side stale-token sweep.

### IN-02: `ClickMeMessagingService.onNewToken` registers on a fire-and-forget app-scope coroutine

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/services/push/ClickMeMessagingService.kt:41-43`
**Issue:**
- The process FCM starts for a rotation can be frozen or killed right after `onNewToken` returns, before the HTTP call finishes.
- The rotated token is then only registered at the next foreground transition, because `PushRegistrationManager` re-registers on every foreground. Pushes can be lost in between.

**Fix:** Acceptable for an MVP. If tightened later, enqueue a unique `WorkManager` job with a network constraint, or document the gap in the KDoc.

### IN-03: A push-opened Booking Summary compares booking ids case-sensitively

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/bookingsummary/BookingSummaryViewModel.kt:~90`
**Issue:**
- The ViewModel's `bookingId` comes from `key.bookingId`, which for a push is lowercased by `PushEvent`.
- The hand-off `ReviewSubmitted.bookingId` comes from `details.bookingId` (the server's casing). The check `pending?.bookingId == bookingId` fails if the server ever returns a differently cased id.
- The reload and thanks snackbar would then be silently skipped, and the slot would not be consumed. `PushRouteLookups` already assumes ids may differ in case.

**Fix:** Use `pending?.bookingId.equals(bookingId, ignoreCase = true)`. Do the same for the `reviewedBookingIds` membership checks, or normalise to lowercase when storing.

### IN-04: `_sessionStarts.value++` is not atomic

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/managers/UserManager.kt:188`
**Issue:** The read-modify-write on a `MutableStateFlow` can lose a count under concurrent writes. Login and signup are serial today, so this is latent.
**Fix:** `_sessionStarts.update { it + 1 }`.

### IN-05: Unused parameters and a silent no-op default on new UI entry points

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/notifications/NotificationsRoute.kt:12-19`, `screens/clientmode/explore/ExploreScreen.kt` (`onOpenNotifications: () -> Unit = {}`), `screens/expertmode/dashboard/ExpertDashboardScreen.kt` (same)
**Issue:**
- `NotificationsRoute` suppresses `UNUSED_PARAMETER` for `key` and `mode`.
- `ExploreContent` and `ExpertDashboardContent` default `onOpenNotifications` to `{}`, so a caller that forgets to wire it gets a bell that does nothing and no compile error.

**Fix:** Drop the unused parameters or document why they stay. Make the callback required on the `*Content` composables, and pass `{}` explicitly in previews and tests.

### IN-06: Booking Summary briefly shows "Leave a review" for an already-reviewed booking

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/bookingsummary/BookingSummaryViewModel.kt` (`startLoad` and `loadReviewContext`), `BookingSummaryUiState.kt` (`showLeaveReview`)
**Issue:**
- The state becomes `Loaded(review = Unknown)` before the review-context call returns.
- A completed booking therefore renders the "Leave a review" button, then swaps it for the Feedback block when the context arrives. The UI-SPEC accepts `Unknown` as "show the button", so this is a visual flicker, not a functional bug.
- It also costs a second round trip before the final layout is known.

**Fix:** Defer emitting `Loaded` for completed bookings until the context resolves, or hide the button while the first context load is in flight. This is optional polish.

---

_Reviewed: 2026-10-28_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_

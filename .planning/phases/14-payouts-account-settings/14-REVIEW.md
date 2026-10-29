---
phase: 14-payouts-account-settings
reviewed: 2026-10-29T00:00:00Z
depth: standard
review_mode: split (3 parallel reviewers: A settings, B expert mode, C wiring/strings; merged by orchestrator)
files_reviewed: 135
files_reviewed_list:
  - app/src/main/java/com/uptrendinvestments/clickmeapp/ClickMeApp.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/enums/ErrorCode.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/requests/AccountRequests.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/requests/ProfileRequests.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/DeletionRequestData.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/FaqData.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/SettingsEntries.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/accountsecurity/AccountSecurityKey.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/accountsecurity/AccountSecurityRepository.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/accountsecurity/AccountSecurityScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/accountsecurity/AccountSecurityViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/accountsecurity/subviews/AccountSecurityCards.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/clientprofilesettings/ClientProfileSettingsKey.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/clientprofilesettings/ClientProfileSettingsScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/clientprofilesettings/ClientProfileSettingsViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/clientprofilesettings/ProfileAutoSave.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/clientprofilesettings/ProfileAvatarController.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/clientprofilesettings/ProfilePhotoCropKey.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/clientprofilesettings/ProfileSettingsRepository.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/clientprofilesettings/ProfileSnapshot.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/clientprofilesettings/subviews/ProfileAvatarSection.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/clientprofilesettings/subviews/ProfileCountryField.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/clientprofilesettings/subviews/ProfileFields.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/clientprofilesettings/subviews/ProfileLanguagesField.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/deleteaccount/AccountDeletionState.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/deleteaccount/DeleteAccountFarewell.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/deleteaccount/DeleteAccountKey.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/deleteaccount/DeleteAccountReason.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/deleteaccount/DeleteAccountRepository.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/deleteaccount/DeleteAccountScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/deleteaccount/DeleteAccountViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/deleteaccount/subviews/DeleteAccountComponents.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/help/HelpKey.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/help/HelpRepository.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/help/HelpScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/help/HelpViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/help/subviews/HelpComponents.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/network/ApiErrorParserTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/settings/SettingsEntriesTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/settings/accountsecurity/AccountSecurityScreenTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/settings/accountsecurity/AccountSecurityViewModelTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/settings/clientprofilesettings/ClientProfileSettingsScreenTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/settings/clientprofilesettings/ClientProfileSettingsViewModelTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/settings/clientprofilesettings/ProfileAutoSaveTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/settings/clientprofilesettings/ProfileAvatarControllerTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/settings/deleteaccount/AccountDeletionFarewellTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/settings/deleteaccount/DeleteAccountScreenTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/settings/deleteaccount/DeleteAccountViewModelTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/settings/help/HelpScreenTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/settings/help/HelpViewModelTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/testing/FakeClickMeApi.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/testing/SettingsFixtures.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/requests/TaxProfileRequests.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/EarningsTransactionsData.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/ExpertFinancialData.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/ExpertProfileData.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/TaxProfileData.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/ExpertLaunchRequest.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/ExpertDashboardRoute.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/ExpertDashboardScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/ExpertFinancialRepository.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/subviews/ExpertToolsGrid.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/subviews/FinancialSummarySection.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/home/HomeExpertScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/payouts/ConnectReturnActivity.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/payouts/EarningsTransactionRow.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/payouts/PayoutsKey.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/payouts/PayoutsLogic.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/payouts/PayoutsScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/payouts/PayoutsViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/payouts/TransactionsKey.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/payouts/TransactionsScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/payouts/TransactionsViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/payouts/subviews/PayoutCards.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/profile/ExpertProfileHomeScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/profile/ExpertProfileHomeViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/settings/ExpertProfileSettingsKey.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/settings/ExpertProfileSettingsRepository.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/settings/ExpertProfileSettingsScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/settings/ExpertProfileSettingsViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/taxprofile/ExpertTaxProfileScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/taxprofile/ExpertTaxProfileViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/taxprofile/TaxProfileKey.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/taxprofile/TaxProfileRepository.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/ExpertDashboardScreenTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/payouts/ConnectReturnIntentTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/payouts/ConnectReturnManifestTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/payouts/EarningsTransactionRowTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/payouts/EarningsUnitsRegressionTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/payouts/PayoutsLogicTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/payouts/PayoutsScreenTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/payouts/PayoutsViewModelTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/payouts/TransactionsScreenTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/payouts/TransactionsViewModelTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/profile/ExpertProfileHomeScreenTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/settings/ExpertProfileSettingsScreenTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/settings/ExpertProfileSettingsViewModelTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/taxprofile/ExpertTaxProfileScreenTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/taxprofile/ExpertTaxProfileViewModelTest.kt
  - app/src/main/AndroidManifest.xml
  - app/src/main/java/com/uptrendinvestments/clickmeapp/components/SectionHeader.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/CallDialog.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/navigation/SettingsOrigin.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/network/ApiRequestUrls.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/network/ClickMeApi.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/CallDialogs.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/ExpertProfileKey.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/ExpertProfileScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/subviews/ExpertProfileActionButtons.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/home/HomeClientScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/profile/ClientProfileHomeScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/profile/ClientProfileHomeViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/profile/subviews/ProfileHubComponents.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/notifications/NotificationRows.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/push/PushDestination.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/push/PushDestinationResolver.kt
  - app/src/main/res/values/strings_account_security.xml
  - app/src/main/res/values/strings_call.xml
  - app/src/main/res/values/strings_delete_account.xml
  - app/src/main/res/values/strings_expert_dashboard.xml
  - app/src/main/res/values/strings_expert_settings.xml
  - app/src/main/res/values/strings_help.xml
  - app/src/main/res/values/strings_payouts.xml
  - app/src/main/res/values/strings_payouts_activity.xml
  - app/src/main/res/values/strings_profile_hub.xml
  - app/src/main/res/values/strings_profile_settings.xml
  - app/src/main/res/values/strings_tax_profile.xml
  - app/src/test/java/com/uptrendinvestments/clickmeapp/network/AccountApiTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/network/PayoutsTaxApiTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/network/ProfileHelpApiTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/HomeShellsTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/call/calling/CallDialogsTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/ExpertProfileScreenTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/profile/ClientProfileHomeScreenTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/profile/ClientProfileHomeViewModelTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/notifications/NotificationDestinationTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/notifications/NotificationsViewModelTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/push/PushDestinationResolverTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/push/PushRouterTest.kt
findings:
  critical: 0
  warning: 12
  info: 13
  total: 25
status: issues_found
---

# Phase 14 Code Review (merged)

Scope: 139 files (phase range from b2e60e4), split into three areas because the scope exceeded the 50-file single-reviewer guidance. 135 files reviewed (4 overlapped/were non-source). Finding IDs carry the part letter.

Orchestrator verification: WR-B01 confirmed in code (`openedOnboarding` is only ever set true, PayoutsViewModel.kt:189; `reopenOnboarding()` has no SetupRequired gate, :177; LinkExpired routes to it, :139). WR-B02 confirmed (DashboardFinancials.kt:58-64 accepts any https host). Part C's statement that a hostile refresh "cannot open a Stripe tab" holds only before this screen first opens onboarding — WR-B01 supersedes it.

---

## Part A


# Phase 14 (Part A: settings screens): Code Review Report

**Reviewed:** 2026-10-29
**Depth:** standard
**Files Reviewed:** 52 (main sources read in full; tests/fakes read selectively to confirm coverage)

## Summary

The settings area is generally careful. Password, deletion token and feedback never reach `toString`, saved state or logs. No `HttpLoggingInterceptor` exists in the app. The `AutoSaver` serializes saves with a mutex, survives `ViewModel` teardown through the flush scope, and moves its baseline only after a successful persist. The Farewell is hosted above the route switch and swallows both back and touches. The deletion double-submit guard (`isDeleting`) holds.

Nothing reaches BLOCKER level. The findings below are mostly robustness gaps: an irreversible request pair running in a cancellable scope, a spinner that can never resolve, uncaught non-`ApiException` failures in application-scope coroutines, a keyboard-capitalization regression versus iOS, and several deviations from the iOS source of truth in change-password validation and error mapping.

## Warnings

### WR-A01: Irreversible delete-account request pair runs in `viewModelScope` and can be cancelled between `DELETE` success and `markDeleted()`

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/deleteaccount/DeleteAccountViewModel.kt:115-126`
**Issue:** `confirmDeletion()` launches both calls in `viewModelScope`. The class doc and `AccountDeletionState` already acknowledge that deleting revokes every session and that "any later authenticated request can force a logout and swap the route to Login". A concurrent authenticated request (chat poll, `/me` refresh, push registration) that gets a 401 `INVALID_TOKEN` just after the server revokes sessions, but before this coroutine resumes with the DELETE response, triggers `Rejected` -> logout -> route switch. That disposes the Delete Account nav entry, clears its ViewModel store and cancels `viewModelScope`. The result is either the in-flight `DELETE` is cancelled client-side, or `deletionState.markDeleted()` is never reached. The account is gone server-side, but the user lands on Login with no Farewell. The very race the root-hosted Farewell was built to survive is not covered, because the trigger (`markDeleted`) lives in the cancellable scope. `AutoSaver` and the avatar controller already use `@ApplicationScope` for exactly this "must outlive the screen" reason.
**Fix:** Run the pair on the application scope (or at minimum make the success tail non-cancellable):
```kotlin
class DeleteAccountViewModel @Inject constructor(
    ...,
    @ApplicationScope private val appScope: CoroutineScope,
) {
    ...
    appScope.launch { /* same try/catch; on success deletionState.markDeleted() */ }
}
```
`onFailure` only touches `_uiState`, which is safe on a cleared ViewModel. Alternatively wrap `repository.deleteAccount(...)` plus `markDeleted()` in `withContext(NonCancellable) { }`.

### WR-A02: Profile Settings shows an infinite spinner with no error or retry when the first profile load fails

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/clientprofilesettings/ClientProfileSettingsViewModel.kt:131-138` (rendered at `ClientProfileSettingsScreen.kt:155-162`)
**Issue:** When `userManager.profile.value` is null, the ViewModel calls `refreshProfile()`, swallows the `ApiException`, and then suspends on `userManager.profile.filterNotNull().first()`. On a network failure or 5xx the profile stays null, so `first()` never returns and the screen shows a spinner forever. The only exit is Back. There is no error message, no retry and no timeout, and the only a11y text is "loading". The comment calls this intentional, but it is a dead-end state, not a loading state.
**Fix:** Expose a `loadFailed` flag in `ClientProfileSettingsUiState`, set it in the `catch`, and render an `EmptyState` with a Retry action that re-runs the refresh. Keep the `filterNotNull().first()` seed for the success path.

### WR-A03: Avatar upload on the application scope only catches `ApiException`; any I/O or decode failure crashes the process

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/clientprofilesettings/ProfileAvatarController.kt:94, 101-123`
**Issue:** `uploadStoredCrop()` runs via `appScope.launch`. The scope is `CoroutineScope(SupervisorJob() + Dispatchers.Default)` with no `CoroutineExceptionHandler` (`di/SessionModule.kt:44`), so an uncaught throwable reaches the thread's default handler and kills the app. `photoProcessor.croppedJpeg()` (`cropFile.readBytes()`, outside any `try`, line 102) can throw `IOException`. `croppedPreview()` (line 109, inside the `try` that only catches `ApiException`) can throw `IOException` or `OutOfMemoryError`. `clearWorkingFiles()` can also throw. `onPicked` already handles generic `Exception` for the import half, so the upload half is inconsistent. The `finally` resets state but does not stop the propagation.
**Fix:**
```kotlin
} catch (e: ApiException) {
    ...
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) { // IOException from the working files
    photoProcessor.clearWorkingFiles()
    _state.update { it.copy(error = FieldError.Res(R.string.signup_photo_open_error_body),
                            errorTitleRes = R.string.signup_photo_open_error_title) }
}
```
Move the `croppedJpeg()` call inside the `try` too.

### WR-A04: `ProfileTextField`'s `KeyboardCapitalization.Words` is silently overridden, so names and city do not auto-capitalize (iOS regression)

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/clientprofilesettings/subviews/ProfileFields.kt:168` (cause: `app/src/main/java/com/uptrendinvestments/clickmeapp/components/ValidatedField.kt:129-137`)
**Issue:** `ProfileTextField` defaults `keyboardOptions` to `KeyboardOptions(capitalization = KeyboardCapitalization.Words)`. `ValidatedField` always does `keyboardOptions.copy(capitalization = KeyboardCapitalization.None, ...)` on both the secure and non-secure branches, so the passed value is discarded. iOS `ProfileLabeledField` uses `.autocapitalization(.words)` for these fields (`ProfileLabeledField.swift:30`). First name, last name, city, state/province, job title and company all lose Title Case. The unused option also misleads readers.
**Fix:** Make `ValidatedField` honor a caller-supplied capitalization (for example `capitalization = if (keyboardOptions.capitalization != KeyboardCapitalization.Unspecified) keyboardOptions.capitalization else None`, with an explicit `Unspecified` default), or add a `capitalization` parameter. Remove the dead default if the override is intended.

### WR-A05: Change-password validation reports only the first failing field, diverging from iOS

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/accountsecurity/AccountSecurityViewModel.kt:61-75, 117`
**Issue:** `validateChangePassword` is a single `when` chain that returns the first failure. If current, new and confirm are all empty, only "Please enter your current password" shows, and the user needs three more taps to see the rest. iOS `validate()` fills `currentPasswordError`, `newPasswordError` and `confirmPasswordError` independently in one pass (current-empty is independent of the new chain, confirm is independent of both). The KDoc claims "iOS order", but the observable behavior differs.
**Fix:** Return a `Map<PasswordField, Int>` built with three independent checks (current empty; new empty / short / no digit / same; confirm empty / mismatch) and set all of them in `submit()`.

### WR-A06: 422 field-error mapping drops messages for unmatched fields and is lossy versus iOS

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/accountsecurity/AccountSecurityViewModel.kt:159, 183-198`
**Issue:** `mapFieldErrors` uses `else -> continue`, so a 422 entry for any field other than `current_password` or `new_password` is discarded. If at least one entry matches, the dialog is never shown, so the unmatched message is lost. If none match, the user gets the generic "failed" text instead of the server's message. iOS puts an unmatched field's message into `apiError` (alert), and falls back to the standard message when the envelope does not decode. Also, a 422 whose body is not a field envelope but has a `message` ignores `e.userMessage`.
**Fix:** In the `else` branch collect the message into a dialog (`showFailure(FieldError.Text(fieldError.message))`), and in the no-match fallback use `e.userMessage?.let(FieldError::Text) ?: FieldError.Res(account_security_failed_body)`.

### WR-A07: Edits flushed on leave that then fail are lost silently

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/clientprofilesettings/ClientProfileSettingsViewModel.kt:292-299, 318-325` (with `ProfileAutoSave.kt:80-84`)
**Issue:** `flushPendingSave()` / `onCleared()` send the pending edit on `appScope` and then the screen is popped. If that PATCH fails (offline, 422, 5xx), `onFailure` updates the `_uiState` of a ViewModel that no one observes any more. No toast, dialog or retry surfaces, and the user believes the edits were saved. For a quick edit-then-back, the failure is silent data loss. The AutoSaver doc advertises the flush as "neither lost nor cancelled", but a failed flush is lost.
**Fix:** Route flush failures to a user-visible channel that survives the screen, for example an app-level snackbar or one-shot event on a singleton, or keep the user on the screen by awaiting the flush when the call fails. At minimum, document the limitation.

## Info

### IN-A01: Delete Account button shows a bare spinner with no accessible label while deleting

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/deleteaccount/subviews/DeleteAccountComponents.kt:332-338`
**Issue:** While `isDeleting`, the button's text is replaced by a `CircularProgressIndicator` with no `contentDescription` or `stateDescription`. The button is also `enabled = canConfirm` (false while deleting), so TalkBack announces an unlabelled, disabled control. `AutoSaveStatusIcon` and the avatar upload already set a state description for their spinners.
**Fix:** Add `Modifier.semantics { contentDescription = deletingText }` (for example "Deleting account") to the spinner, or to the button.

### IN-A02: Top-bar back arrow and system Back behave differently on the Reason step

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/deleteaccount/DeleteAccountScreen.kt:82, 89-93`
**Issue:** System Back on the Reason form returns to Warning (`onBackToWarning`), but the app-bar arrow always calls `onBack` and leaves the whole flow. iOS pops on every step. The two Android affordances disagree, and a user who taps the arrow after typing loses the form. Pick one behavior (the arrow should probably do the same as system Back, or Back should pop like iOS).
**Fix:** Wire the arrow to `if (state.step == DeleteStep.Reason) onBackToWarning() else onBack()`.

### IN-A03: Text clamping with `String.take` can split a surrogate pair

**File:** `ClientProfileSettingsViewModel.kt:161, 271-272`; `ProfileFields.kt:180, 275`; `DeleteAccountViewModel.kt:95`
**Issue:** `value.take(max)` cuts at a UTF-16 code unit. If the cut lands inside an emoji (for example in the bio or feedback comments), a lone high surrogate remains. It will be sent to the server and may be rejected or garbled. The counters also count UTF-16 units, not characters the server counts.
**Fix:** Cut on a code point boundary: `if (value.length > max) value.substring(0, if (Character.isHighSurrogate(value[max - 1])) max - 1 else max)`.

### IN-A04: "Saved" check flashes even when `buildProfilePatch` returns null and nothing was sent

**File:** `ClientProfileSettingsViewModel.kt:301-306` with `ProfileAutoSave.kt:109-110`
**Issue:** If an edit differs from the baseline only by whitespace (trailing space), `persist` returns early without a request, but `AutoSaver.save()` still sets `saved = new` and `Status.Saved`. The indicator announces "Saved" (a polite live region) for a no-op. The tests (`whitespaceOnlyChangeGivesNoPatch`) only cover the pure builder.
**Fix:** Have `persist` return a Boolean ("sent"), and skip the `Saved` transition (return to `Idle`) when it is false.


_Reviewed: 2026-10-29_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_

---

## Part B


# Phase 14 (Part B): Code Review Report

**Reviewed:** 2026-10-29
**Depth:** standard
**Files Reviewed:** 44
**Status:** issues_found

## Summary

Reviewed expert-mode Payouts (state machine, Custom Tab launch, onboarding-URL check, `ConnectReturnActivity` trampoline), the earnings ledger (paging, dedupe, minor units), Tax and Payout Info (TIN and PII handling, DOB picker, 422 mapping), Expert Settings, and the Dashboard, hub and home wiring. Main sources were read in full. Tests were skimmed to confirm coverage. The iOS `PayoutsViewModel` and the backend STRIPE-INTEGRATION doc were used as the reference.

No Critical issues. PII/TIN handling is good: `toString()` is redacted on every model, the form is not saved-state, the secure field uses `KeyboardType.Password` with autocorrect off, and there is no HTTP body logging. Money handling is correct (minor units through `Money.format`, no division). The deep-link handler only reads scheme, host and exact path and never trusts the query.

The main concerns are a spoofable re-mint path on the exported return activity, a URL check that is not a real allow-list, and a paging race in the ledger. The exact string "Tax info incomplete" and similar server text are not logged anywhere.

## Warnings

### WR-B01: A spoofed `clickme://connect/refresh` can force a Stripe link re-mint and Custom Tab at any time after the first setup

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/payouts/PayoutsViewModel.kt:97,144-149,174-175`
**Issue:** `ConnectReturnActivity` is `exported` and `BROWSABLE`, so any app or web page can fire `clickme://connect/refresh`. The VM guards the re-mint only with `openedOnboarding`, a flag that is set once (line 190) and never cleared. It is not cleared after `ConnectReturn.Done`, after the account becomes `Onboarded`, or when the screen is left. `reopenOnboarding()` also does not check that the current content is `SetupRequired`. While a `PayoutsViewModel` is alive (the Payouts screen is on the stack), a hostile link can therefore:
- call `callGuard.attempt`, hit `POST expert/connect/refresh-link` and open a fresh Stripe Account Link in a browser, for an expert whose payouts are already fully set up;
- invalidate an onboarding link the user is in the middle of using, since links are single-use;
- bring the app to the foreground, because the trampoline always relaunches `MainActivity`.

The impact is bounded: the URL is server-minted and https-checked, and the user can only open their own account link. It is still an unauthenticated, repeatable trigger of a state-changing call and a browser launch, which the class doc ("only a go re-check signal") says should not exist.
**Fix:**
```kotlin
private fun onConnectReturn(value: ConnectReturn) {
    val reload = if (_uiState.value.load is PayoutsLoad.Loaded) LoadMode.Quiet else LoadMode.Loud
    when (value) {
        ConnectReturn.Done -> { openedOnboarding = false; startLoad(reload) }
        ConnectReturn.LinkExpired ->
            if (openedOnboarding && _uiState.value.content is PayoutsContent.SetupRequired) {
                openedOnboarding = false   // one re-mint per opened link
                reopenOnboarding()
            } else startLoad(reload)
    }
}
```
Also reset `openedOnboarding` when `resolvePayoutOnboarding` yields `Onboarded`. Add a ViewModel test: Done then a stray LinkExpired mints nothing, and a second LinkExpired after one re-mint mints nothing.

### WR-B02: `isAllowedOnboardingUrl` accepts any https host, so it is not an allow-list

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/DashboardFinancials.kt:58-63`
**Issue:** The check only requires scheme `https` and a non-blank host. A compromised or misconfigured backend, a MITM at a corporate TLS proxy, or a poisoned response could hand back `https://evil.example/...`, and the app would open it as a "Stripe onboarding" page. Users are primed to enter identity and bank data on this flow, so this is the highest-value phishing surface in the app. `java.net.URI` is also used to validate, but `Uri.parse` is used to launch (`PayoutsScreen.kt:~125`, `ExpertDashboardScreen.kt:~180`). The two parsers can disagree on edge-case authorities (userinfo `@`, backslashes), so the validated host may not be the launched host.
**Fix:** Pin the host to Stripe, and launch the exact string that was validated:
```kotlin
private val StripeHostSuffixes = listOf("stripe.com")   // connect.stripe.com, etc.
fun isAllowedOnboardingUrl(url: String): Boolean = try {
    val uri = URI(url)
    val host = uri.host?.lowercase(Locale.ROOT)
    uri.scheme.equals("https", true) && uri.userInfo == null && host != null &&
        StripeHostSuffixes.any { host == it || host.endsWith(".$it") }
} catch (_: URISyntaxException) { false }
```
If the backend can return a non-Stripe host in test environments, make the suffix list a `BuildConfig` field.

### WR-B03: `retryPage()` during a pull-to-refresh can skip a ledger page

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/payouts/TransactionsViewModel.kt:107-111,121-143,145-158`
**Issue:** `onRowVisible` checks `!state.isRefreshing`, but `retryPage()` checks only `isPaging` and `pageError`. With a page-error footer showing, the user can pull to refresh (`loadFirstPage` cancels `pageJob`, then starts page 1) and tap Retry on the footer while that refresh is still in flight. `loadNextPage()` then starts a new `pageJob` for the stale `nextPage` (for example 3) and the refresh job is not cancelled. If the refresh finishes first, it sets `rows = page 1` and `nextPage = 2`. The late page-3 response then appends page 3 to page 1 and sets `nextPage = 4`, so page 2 is silently skipped. If the order is reversed, the page-3 `nextPage = 4` is overwritten and the result is fine. The outcome depends on network timing, which makes it hard to reproduce.
**Fix:** Block the retry during a refresh, and make a page result self-validating:
```kotlin
fun retryPage() {
    val state = _uiState.value
    if (state.isPaging || state.isRefreshing || state.pageError == null) return
    loadNextPage()
}
```
A generation counter incremented in `loadFirstPage` and checked after the `transactions()` await in `loadNextPage` would remove the whole class of stale-write bugs. Add a test: refresh in flight, then `retryPage` is a no-op.

### WR-B04: One rejected meeting link makes every later auto-save report failure

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/settings/ExpertProfileSettingsViewModel.kt:228-241,401-415`
**Issue:** When the server answers 422 for the link, `onSaveFailed` shows the inline error, but the `AutoSaver` baseline is not advanced and the snapshot keeps the rejected `meetingUrl` (client-side validation passed, so `onMeetingUrlChange` stored it). Every later edit to an unrelated field (bio, city) then calls `persist(old, new)`. That re-patches the profile successfully, then re-sends the same rejected link and fails again, so the status icon shows a save failure even though the edit was saved. The error is not tied to the field the user is editing, and the Retry icon re-sends the same bad link forever. The user cannot clear the state without editing the link text.
**Fix:** After a 422 naming `meeting_url`, rebase the saver's link component to `savedMeetingUrl` so the rejected value is not retried (for example `saver.rebase(snapshot.copy(meetingUrl = savedMeetingUrl))` while keeping `meetingUrlText` and the inline error). Alternatively, send the link only when it changed since the last attempt.

## Info

### IN-B01: Clearing the meeting link looks like it worked but does nothing

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/settings/ExpertProfileSettingsViewModel.kt:182-190`
**Issue:** A blank value falls back to `savedMeetingUrl` in the snapshot and shows no error. The field reads empty while the server still holds the old link, which reappears on the next load. This matches iOS (the key is omitted when blank), so it is not a regression, but it is a silent no-op.
**Fix:** Either show a neutral helper such as "Leave blank to keep your current link", or confirm with product that clearing should be supported and send an explicit clear.

### IN-B02: Side effect inside a `MutableStateFlow.update` lambda

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/payouts/PayoutsViewModel.kt:188-196`
**Issue:** `openedOnboarding = true` is set inside the `_uiState.update { ... }` lambda. `update` is a CAS loop and may re-run the lambda, so side effects belong outside it. It is harmless today because it is idempotent, and it will matter if WR-B01's flag logic becomes stateful.
**Fix:** Compute `val allowed = isAllowedOnboardingUrl(url)` first, set the flag outside, then `update`.

### IN-B03: First load shows "Ready" or "On hold" when the payout summary call failed

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/payouts/PayoutsLogic.kt:47-53` and `PayoutsViewModel.kt:230-232`
**Issue:** `selectPayoutsContent(Onboarded, summary = null, ...)` returns `ReadyNoEarnings` or `OnHold` when the status call succeeded but the summary hit a non-400 error. On a first (Loud) load, `applyResult` never flags this as a failure (the `lostSummary` guard only applies to non-Loud modes with existing content), so a real balance can be hidden behind "You're ready to receive payouts". This is iOS behavior (`PayoutsViewModel.swift`, forceLoad), so it is parity, but the KDoc claims balances are never replaced by "ready".
**Fix:** Treat `lostSummary` as a failure in Loud mode too (show Retry), or document the iOS parity explicitly.

### IN-B04: "See all" derivation differs between Payouts and the Activity list

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/payouts/PayoutsViewModel.kt:243-250` vs `TransactionsViewModel.kt:150-151`
**Issue:** `PayoutsViewModel.hasMore` uses `pagination?.hasMoreOrDerived == true`, so a missing pagination block gives false. `TransactionsViewModel.hasMorePages` treats a full page as "maybe more". The Payouts screen compensates with `rows.size > PREVIEW_ROW_COUNT`, so there is no visible bug today, but the two rules can diverge.
**Fix:** Share one `EarningsTransactionsData.hasMore()` helper.


_Reviewed: 2026-10-29_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_

---

## Part C


# Phase 14 (Part C): Code Review Report

**Reviewed:** 2026-10-29
**Depth:** standard
**Files Reviewed:** 39 (plus ConnectReturnActivity.kt, PayoutsViewModel.kt, Navigator.openRouted, TokenAuthenticator, AccountSecurity/DeleteAccount view models read for cross-reference)
**Status:** issues_found

## Summary

Part C (cross-cutting wiring) is in good shape. I tried to break each area and could not find a blocker.

- **Manifest / exported activity.** `ConnectReturnActivity` is exported with a custom-scheme VIEW filter. It parses only scheme, host and exact path, and it never reads the query or fragment. It carries no data into app state and only posts "go re-check" slots. `PayoutsViewModel.init` drains a stale `connectReturn` slot on creation, and `LinkExpired` re-mints a link only if that screen opened one. So a hostile `clickme://connect/refresh` cannot open a Stripe tab. The risk is limited to a forced foreground and Dashboard reload.
- **Retrofit contract.** Every new endpoint matches the iOS `APIRequestURLs` and the backend docs on path and HTTP verb:
  - `PATCH user/password`
  - `POST user/account/delete-request`
  - `DELETE user/account`, with a body via `@HTTP(hasBody = true)`
  - `PATCH user/profile` and `PATCH expert/profile`
  - `GET help/faqs`
  - `GET expert/earnings/transactions?page&limit`
  - `GET` and `POST expert/tax-profile`

  `TokenAuthenticator` handles the code-less 401 from the delete endpoints correctly. It refreshes once, then does not clear the session unless the retry carries a token-coded 401.
- **Push routing.** Payouts and KYC go through `allowSwitch`. A client-only account, or a call active in Client mode, gets the current mode's notifications list. Account Security never switches modes. All three match iOS `HomeExpertView`/`PushRouter`. Resolver tests cover the cases.
- **Debug UI.** The debug affordances are gated by `BuildConfig.DEBUG`, passed in from the Hilt entry point. The gallery key is still registered in release builds but cannot be navigated to.
- **Strings.** All 11 string files were checked.
  - Every resource is referenced from main code (scripted check).
  - Format arguments and plural usages match their call sites.
  - Copy matches iOS where it exists.
  - No hard-coded user-facing literals remain outside `@Preview`.

The findings below are minor.

## Warnings

### WR-C01: A Payouts push opened while Payouts is already open from the Profile hub stacks a second Payouts instance

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/push/PushDestinationResolver.kt:112-121` (with `navigation/Navigator.kt:177-210`)

**Issue:**
- The hub opens `PayoutsKey(SettingsOrigin.Profile)` (`HomeExpertScreen.kt:284`).
- A payout or KYC push always resolves to `PayoutsKey(SettingsOrigin.Dashboard)` on the Dashboard stack.
- `openRouted` de-duplicates by key equality. The two keys differ by design, so a tap on a payout push does not find the already-open screen. It switches to the Dashboard tab and pushes a second Payouts.
- Both `PayoutsViewModel`s stay alive. Each runs `ON_RESUME` reloads and each subscribes to `launchRequest.connectReturn`.
- A Stripe return then goes to whichever instance consumes the slot first. The hidden one can call `reopenOnboarding()` and open a Custom Tab if it was the one that opened onboarding.
- Impact is limited to a duplicated screen, extra requests and Back-stack surprise. It is reachable only when a push arrives while Payouts is open from the Profile tab.

**Fix:** When the shell is already in Expert mode, choose the origin from where the user is. The resolver already receives `PushRoutingContext`, which does not carry the tab. Either add the selected tab to the context, or have the expert shell's `openRouted` consumer skip the push when a `PayoutsKey` of either origin is already in the stack:

```kotlin
// illustrative: in the expert shell route consumer, before openRouted (selectTabContaining is a new helper)
val alreadyOpen = state.backStacks.values.any { stack -> stack.any { it is PayoutsKey } }
if (alreadyOpen && route.keys.last() is PayoutsKey) { navigator.selectTabContaining<PayoutsKey>(); return }
```

## Info

### IN-C01: Leftover string `profile_log_out` is now unused

**File:** `app/src/main/res/values/strings_session.xml:11`

**Issue:** The throwaway Profile placeholder was the only user of `profile_log_out`. The new hub uses `profile_hub_log_out`. `grep` finds only the definition, so lint will report `UnusedResources`.

**Fix:** Delete `profile_log_out` from `strings_session.xml`.

### IN-C02: Self-view still shows two action-button skeleton pills, then they collapse

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/subviews/ExpertProfileActionButtons.kt:92-100` (used from `ExpertProfileScreen.kt` `DetailsSkeleton`)

**Issue:**
- The skeleton always draws two 56 dp pills (Book Session and Message).
- On the self-view profile both buttons are hidden once the details load. `showBookSession` is false and `showMessage` is false because `chatEntry.isSelf` is true.
- The loaded layout therefore has no buttons, and everything below jumps up about 128 dp when the crossfade finishes.

**Fix:** Pass the same `showBookSession` and `showMessage` flags into the skeleton, or skip it when both are false.

```kotlin
ExpertProfileActionButtonsSkeleton(pills = listOf(showBookSession, showMessage).count { it })
```

### IN-C03: Hub scaffold sets `scrolledContainerColor` without a scroll behavior, so it never takes effect

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/profile/subviews/ProfileHubComponents.kt:99-111`

**Issue:**
- The `TopAppBar` is created without a `scrollBehavior`, and the body scrolls in a separate `Column`.
- `scrolledContainerColor` is therefore dead configuration. The bar stays `surface` while the content scrolls beneath it.
- This is cosmetic. Other tab roots use the same pattern.

**Fix:** Drop `scrolledContainerColor`, or wire `TopAppBarDefaults.pinnedScrollBehavior()` plus `nestedScroll` as `ExpertProfileContent` does.

### IN-C04: A few Phase 14 strings read poorly under TalkBack

**File:** `app/src/main/res/values/strings_expert_dashboard.xml:36` (`dashboard_earnings_a11y`), `app/src/main/res/values/strings_payouts_activity.xml:23-26` (`payouts_activity_none`, `payouts_activity_row_a11y`), `app/src/main/res/values/strings_payouts.xml:28` (`payouts_none`)

**Issue:**
- `dashboard_earnings_a11y` joins its two values with a middle dot ("·"). `payouts_activity_row_a11y` and `payouts_balance_a11y` use commas.
- The "—" placeholders are dropped into `payouts_activity_row_a11y` for missing text, so TalkBack may announce "em dash" or skip them.
- The placeholders are meant for display only.

**Fix:**
- Use a comma or period in `dashboard_earnings_a11y`.
- Substitute a spoken fallback such as "unavailable" in the a11y string, while keeping "—" for the visible `row_line`.

### IN-C05: Plural item uses non-positional `%d`

**File:** `app/src/main/res/values/strings_payouts.xml:25-26` (`payouts_hold_body`)

**Issue:**
- This is the only Phase 14 plural that uses `%d`. Its siblings in `strings_expert_dashboard.xml` use `%1$d`.
- It works today, because the call site passes one argument.
- A positional specifier is safer if a locale reorders the text. Lint also flags the non-positional form once more than one specifier is present.

**Fix:** Use `%1$d` in both items.


_Reviewed: 2026-10-29_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_

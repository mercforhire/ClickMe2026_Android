---
phase: 14-payouts-account-settings
fixed_at: 2026-10-29T00:00:00Z
review_path: .planning/phases/14-payouts-account-settings/14-REVIEW.md
iteration: 1
findings_in_scope: 12
fixed: 11
skipped: 1
status: partial
---

# Phase 14: Code Review Fix Report

**Fixed at:** 2026-10-29
**Source review:** .planning/phases/14-payouts-account-settings/14-REVIEW.md
**Iteration:** 1

**Summary:**
- Findings in scope: 12 (Warnings WR-A01..A07, WR-B01..B04, WR-C01)
- Fixed: 11
- Skipped: 1 (WR-A07, design decision; limitation documented in code)

**Verification:** every fix was compiled and its focused unit tests run in an isolated git worktree
(`./gradlew :app:testDevDebugUnitTest --tests ...`). After the last commit the whole `:app:testDevDebugUnitTest` suite
ran green in that worktree. The worktree was created with `workflow.use_worktrees` unset (default true), had a copied
`local.properties` and `google-services.json` and an absolute `ios-reference` link. Re-run the suite in the main
checkout if you want numbers reproducible from there.

## Fixed Issues

### WR-A01: Irreversible delete-account request pair ran in `viewModelScope`

**Files modified:** `screens/settings/deleteaccount/DeleteAccountViewModel.kt`, `DeleteAccountViewModelTest.kt`, `DeleteAccountScreenTest.kt`
**Commit:** 490aa1dd
**Applied fix:** `DeleteAccountViewModel` takes an `@ApplicationScope CoroutineScope` and runs the `delete-request` +
`DELETE` pair (and `markDeleted()`) on it, so a logout triggered by a 401 elsewhere cannot cancel the pair or skip the
Farewell. `onFailure` only touches `_uiState`, which is safe on a cleared ViewModel. Tests build the ViewModel with a
scope on the test dispatcher.

### WR-A02: Profile Settings infinite spinner on first-load failure

**Files modified:** `ClientProfileSettingsViewModel.kt`, `ClientProfileSettingsScreen.kt`, `ClientProfileSettingsViewModelTest.kt`, `ClientProfileSettingsScreenTest.kt`
**Commit:** e311b082
**Applied fix:** New `loadFailed` UI state, set when the refresh fails and nothing is cached. The screen shows the
shared `ErrorState` (Retry) and `retryLoad()` re-runs the refresh. The `filterNotNull().first()` wait was replaced by
seeding from the existing `userManager.profile` collector (idempotent `seed`), so a profile that arrives later still
seeds. Added a ViewModel test (fail, then retry seeds) and a screen test (Retry tap).

### WR-A03: Avatar upload on the application scope only caught `ApiException`

**Files modified:** `ProfileAvatarController.kt`, `FakePhotoProcessor.kt`, `ProfileAvatarControllerTest.kt`
**Commit:** 8dd33a47
**Applied fix:** `uploadStoredCrop()` now wraps `croppedJpeg()` as well, rethrows `CancellationException` and maps any
other `Exception` (working-file I/O) to the existing "couldn't open photo" dialog. `clearWorkingFiles()` in the catch
paths is itself guarded. Added `previewError` to the fake and a regression test.

### WR-A04: `KeyboardCapitalization.Words` silently overridden in `ValidatedField`

**Files modified:** `components/ValidatedField.kt`
**Commit:** 05c95e77
**Applied fix:** `ValidatedField` honors a caller-supplied capitalization; `Unspecified` (the default) and secure
fields still resolve to `None`. This also restores Words capitalization on the signup Basic Info name fields, which hit
the same override. No unit test: capitalization is not exposed in Compose semantics under Robolectric.

### WR-A05: Change-password validation reported only the first failing field

**Files modified:** `AccountSecurityViewModel.kt`, `AccountSecurityViewModelTest.kt`
**Commit:** aea02d86 (shared with WR-A06, same two functions)
**Applied fix:** `validateChangePassword` returns a `Map<PasswordField, Int>` built from three independent checks, like
iOS `validate()`, and `submit()` sets all of them. Tests were adjusted (new empty/short/no-digit now also flag a
mismatching confirm, as on iOS) and an all-empty test added.

### WR-A06: 422 field-error mapping dropped unmatched fields

**Files modified:** `AccountSecurityViewModel.kt`, `AccountSecurityViewModelTest.kt`
**Commit:** aea02d86
**Applied fix:** Entries for fields other than `current_password` / `new_password` now open the failure dialog with the
last such message (iOS `apiError`), alongside any matched inline errors. When the body is not a field envelope, the
dialog uses `e.userMessage` before the generic text. Tests added for the unknown-field, mixed and message-only cases.

### WR-B01: Spoofed `clickme://connect/refresh` could force a re-mint and Custom Tab

**Files modified:** `PayoutsViewModel.kt`, `PayoutsViewModelTest.kt`
**Commit:** 843405b1
**Applied fix:** `LinkExpired` re-mints only when `openedOnboarding` is set and the content is still `SetupRequired`,
and the flag is cleared on the re-mint, on `Done` and once the account resolves to `Onboarded`. Three new ViewModel
tests: stray `LinkExpired` after `Done`, no outstanding link after a re-mint, and an onboarded expert.
Note: requires human verification (state-machine logic).

### WR-B02: `isAllowedOnboardingUrl` accepted any https host

**Files modified:** `screens/expertmode/dashboard/DashboardFinancials.kt`, `DashboardFinancialsTest.kt`
**Commit:** b4852c65
**Applied fix:** The check now requires https, no user-info and a host of `stripe.com` or `*.stripe.com`. The screens
already launch the exact validated string. Added tests for look-alike hosts, user-info and backslash tricks. No
`BuildConfig` override was added: Stripe Connect links use `connect.stripe.com` in test mode too. Note: requires human
verification if the backend can return another host in any environment.

### WR-B03: `retryPage()` during a pull-to-refresh could skip a ledger page

**Files modified:** `TransactionsViewModel.kt`, `TransactionsViewModelTest.kt`
**Commit:** ab0fcabc
**Applied fix:** `retryPage()` is ignored while `isRefreshing`. Added a test with a gated refresh. The optional
generation counter was not added: with the guard, `loadFirstPage` already cancels any page job.

### WR-B04: One rejected meeting link made every later auto-save report failure

**Files modified:** `ExpertProfileSettingsViewModel.kt`, `ExpertProfileSettingsViewModelTest.kt`
**Commit:** 61e82f52
**Applied fix:** `persist` now compares the link with what the server has (`savedMeetingUrl`) and remembers a link the
server answered 422 for (`rejectedMeetingUrl`). A 422 naming `meeting_url` puts the message under the link field and
the save completes (the other fields did save), so no failure icon and no re-send of the rejected link; editing the
link text clears the memory. Other link failures still fail the save and keep Retry. Test covers reject, unrelated
edit, then a new link. Note: requires human verification (save-state logic).

### WR-C01: A Payouts push stacked a second Payouts when one was open from the Profile hub

**Files modified:** `navigation/Navigator.kt`, `screens/expertmode/home/HomeExpertScreen.kt`, `NavigatorOpenRoutedTest.kt`, `HomeShellsTest.kt`
**Commit:** 6ff69d19
**Applied fix:** New `Navigator.openExisting(matches)` selects the tab that already holds a matching key and cuts back
to it, pushing nothing. The expert shell calls it for a `PayoutsKey` destination before `openRouted`, so a payout push
reuses the hub's instance (any origin). Navigator and shell tests added. Note: requires human verification (navigation
behaviour, including Back from the reused screen).

## Skipped Issues

### WR-A07: Edits flushed on leave that then fail are lost silently

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/clientprofilesettings/ClientProfileSettingsViewModel.kt:292-299, 318-325` (with `ProfileAutoSave.kt:80-84`)
**Reason:** Surfacing a failed flush needs an app-level message channel (snackbar or one-shot event host above the
route switch) that does not exist; building one is a cross-cutting design decision. Awaiting the flush would block the
non-blocking back behaviour the screen documents. Per the review's "at minimum, document the limitation", the
limitation is now stated in the `AutoSaver.flush()` KDoc (commit 5fb60bc1, doc-only, no behaviour change). The
finding remains open and also applies to Expert Settings, which shares `AutoSaver`.
**Original issue:** If the flush-on-leave PATCH fails (offline, 422, 5xx), `onFailure` updates the `_uiState` of a
ViewModel nobody observes, so the user believes the edits were saved.

---

_Fixed: 2026-10-29_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 1_

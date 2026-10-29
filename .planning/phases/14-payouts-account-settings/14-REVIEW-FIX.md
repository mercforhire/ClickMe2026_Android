---
phase: 14-payouts-account-settings
fixed_at: 2026-10-29T00:00:00Z
review_path: .planning/phases/14-payouts-account-settings/14-REVIEW.md
iteration: 2
findings_in_scope: 25
fixed: 23
skipped: 2
status: partial
---

# Phase 14: Code Review Fix Report

**Fixed at:** 2026-10-29
**Source review:** .planning/phases/14-payouts-account-settings/14-REVIEW.md
**Iteration:** 2 (covers both passes: iteration 1 fixed the Warnings, iteration 2 the Info findings)

**Summary:**
- Findings in scope: 25 (12 Warnings WR-A01..A07, WR-B01..B04, WR-C01; 13 Info IN-A01..A04, IN-B01..B04, IN-C01..C05)
- Fixed: 23 (11 in pass 1, 12 in pass 2)
- Skipped: 2 (WR-A07, IN-B01)

**Verification:**
- Pass 1: every fix compiled and its focused tests ran in an isolated git worktree; the full
  `:app:testDevDebugUnitTest` suite ran green there after the last commit.
- Pass 2: worked directly in the main checkout on `main` (no worktree). Each fix was verified with its focused tests
  (`./gradlew :app:testDevDebugUnitTest --tests ...`), then the whole `:app:testDevDebugUnitTest` suite ran green in the
  main checkout after the last commit (320 result files, 0 failures or errors). These numbers are reproducible from the
  main checkout.

## Fixed Issues

### Pass 1 (Warnings, commits 490aa1dd..6ff69d19)

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

### Pass 2 (Info findings, commits a0305885..3a91205b)

Paths below are relative to `app/src/main/java/com/uptrendinvestments/clickmeapp/` unless prefixed with `res/` or `test/`.

### IN-A01: Delete Account button spinner had no accessible label

**Files modified:** `screens/settings/deleteaccount/subviews/DeleteAccountComponents.kt`, `res/values/strings_delete_account.xml`, `test/.../deleteaccount/DeleteAccountScreenTest.kt`
**Commit:** a0305885
**Applied fix:** The in-button spinner now carries a content description from the new string `delete_account_deleting`
("Deleting account"). Test asserts the description exists while `isDeleting`.

### IN-A02: Top-bar back arrow and system Back differed on the Reason step

**Files modified:** `screens/settings/deleteaccount/DeleteAccountScreen.kt`, `test/.../deleteaccount/DeleteAccountScreenTest.kt`
**Commit:** 42eb51db
**Applied fix:** On the Reason step the app-bar arrow now calls `onBackToWarning`, the same as system Back; on the
Warning step it still leaves the flow. "Keep my account" still leaves the flow (iOS `dismiss()`). Tests cover the
arrow on both steps. Note: requires human verification (UX choice). iOS pops the whole flow from every step and
Android's system Back already stepped back to the Warning before this change, so the arrow now follows the existing
Android behavior rather than iOS.

### IN-A03: `String.take` could split a surrogate pair

**Files modified:** `util/TextClamp.kt` (new), `screens/settings/clientprofilesettings/ClientProfileSettingsViewModel.kt`, `.../subviews/ProfileFields.kt`, `screens/settings/deleteaccount/DeleteAccountViewModel.kt`, `screens/expertmode/settings/ExpertProfileSettingsViewModel.kt`, `test/.../util/TextClampTest.kt` (new)
**Commit:** 03ca97b9
**Applied fix:** New `String.clampLength(max)` cuts on a code point boundary (drops a trailing lone high surrogate).
Used for every text-field clamp the finding cites (profile fields, bio, job title and company, delete-feedback
comments) plus the same clamps in Expert Settings, which share the field limits. The tax-profile country code and list
`take`s are unrelated and unchanged. The counters still count UTF-16 units, as before.

### IN-A04: "Saved" flashed even when nothing was sent

**Files modified:** `screens/settings/clientprofilesettings/ProfileAutoSave.kt`, `.../ClientProfileSettingsViewModel.kt`, `test/.../ProfileAutoSaveTest.kt`, `test/.../ClientProfileSettingsViewModelTest.kt`
**Commit:** 5197af45
**Applied fix:** `AutoSaver` gets an optional `sendsNothing(old, new)` predicate (default: never). When it is true
the snapshot becomes the baseline silently, with no Saving/Saved transition. The profile ViewModel passes
`buildProfilePatch(old, new) == null`. This keeps `persist`'s signature unchanged, so Expert Settings and the other
callers are untouched. Tests: saver level and a whitespace-only edit at ViewModel level.

### IN-B02: Side effect inside a `MutableStateFlow.update` lambda

**Files modified:** `screens/expertmode/payouts/PayoutsViewModel.kt`
**Commit:** a9e70c4b
**Applied fix:** `isAllowedOnboardingUrl(url)` is evaluated first, `openedOnboarding` is set outside the lambda, then
`update` only copies state. Behavior is unchanged; existing WR-B01 tests cover the flag.

### IN-B03: First load showed "Ready" or "On hold" when the summary call failed

**Files modified:** `screens/expertmode/payouts/PayoutsViewModel.kt` (KDoc only)
**Commit:** 58a3298c
**Applied fix:** Documented, not changed. The review itself calls this iOS parity, so treating `lostSummary` as a
failure in Loud mode would deviate from the iOS source of truth. The class KDoc now states that the first load has
nothing to keep and shows "ready" or "on hold" until the next reload. I could not locate the iOS `PayoutsViewModel`
under `ios-reference/` to re-check it, so the parity claim rests on the reviewer's reading. Note: requires human
verification.

### IN-B04: "See all" derivation differed between Payouts and Activity

**Files modified:** `model/responses/EarningsTransactionsData.kt`, `screens/expertmode/payouts/PayoutsViewModel.kt`, `screens/expertmode/payouts/TransactionsViewModel.kt`, `test/.../model/responses/EarningsTransactionsDataTest.kt` (new)
**Commit:** e8c58526
**Applied fix:** One shared `EarningsTransactionsData.hasMoreRows(pageSize)` (server flag, else page counts, else a
full page means maybe more) is used by both view models. The only behavior change: Payouts now treats a missing
pagination block with a full first page as "has more", as Activity already did. Named `hasMoreRows` to avoid clashing
with `PaginationMeta.hasMore` and the view models' own `hasMore`.

### IN-C01: Unused string `profile_log_out`

**Files modified:** `res/values/strings_session.xml`
**Commit:** 66cb59f6
**Applied fix:** Deleted the string and its stale "Profile tab placeholder" comment. `grep` confirmed no references.

### IN-C02: Self-view skeleton drew two action-button pills, then collapsed

**Files modified:** `screens/clientmode/expertprofile/subviews/ExpertProfileActionButtons.kt`, `screens/clientmode/expertprofile/ExpertProfileScreen.kt`, `test/.../ExpertProfileScreenTest.kt`
**Commit:** 0571ef6b
**Applied fix:** `ExpertProfileActionButtonsSkeleton` takes `pills` (default 2, so existing callers and the existing
test are unchanged); `DetailsSkeleton` passes the count of `showBookSession` and `showMessage` that are true. Zero
pills draws nothing. Test added for 1 and 0 pills.

### IN-C03: Dead `scrolledContainerColor` on the hub app bar

**Files modified:** `screens/clientmode/profile/subviews/ProfileHubComponents.kt`
**Commit:** aff7847b
**Applied fix:** Removed the unused `scrolledContainerColor`; no scroll behavior was added (cosmetic, matches the
other tab roots).

### IN-C04: Phase 14 strings read poorly under TalkBack

**Files modified:** `res/values/strings_expert_dashboard.xml`, `res/values/strings_payouts_activity.xml`, `screens/expertmode/payouts/EarningsTransactionRow.kt`, `test/.../EarningsTransactionRowTest.kt`, `test/.../ExpertDashboardScreenTest.kt`
**Commit:** 3a91205b
**Applied fix:** `dashboard_earnings_a11y` now joins with a comma instead of the middle dot. The earnings row's
spoken description substitutes "unavailable" (new `payouts_activity_none_a11y`) for a missing topic or client, while
the visible line keeps "—". `payouts_none` (the Payout method row) was left alone: it is a plain two-column text row
that is not merged into a combined description, so the finding's concern does not apply. Tests updated.

### IN-C05: Plural used non-positional `%d`

**Files modified:** `res/values/strings_payouts.xml`
**Commit:** eeb7336f
**Applied fix:** Both items of `payouts_hold_body` now use `%1$d`.

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

### IN-B01: Clearing the meeting link looks like it worked but does nothing

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/settings/ExpertProfileSettingsViewModel.kt:182-190`
**Reason:** Needs a product decision, and the current behavior is iOS parity. The review itself says it is not a
regression (iOS omits the key when the link is blank). Both of its options are product choices: new helper copy
("Leave blank to keep your current link") or supporting an explicit clear, which would change the request contract
versus iOS. Left unchanged.
**Original issue:** A blank value falls back to `savedMeetingUrl` in the snapshot and shows no error, so the field
reads empty while the server still holds the old link, which reappears on the next load.

---

_Fixed: 2026-10-29_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 2_

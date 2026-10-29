# Phase 15: Polish & Hardening - Pattern Map

**Mapped:** 2026-10-29
**Files analyzed:** 17 (groups)
**Analogs found:** 15 / 17

Paths below are relative to `app/src/main/java/com/uptrendinvestments/clickmeapp/` (main) or `app/src/test/java/com/uptrendinvestments/clickmeapp/` (test). All were verified with `git ls-files`.

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match |
|---|---|---|---|---|
| main `components/EmptyState.kt` (ErrorState params, heading semantics) | component | request-response | itself (lines 77-90) | exact |
| main `components/RefreshFailedSnackbar.kt` (NEW) | utility | event-driven | `screens/signup/overview/SignupOverviewScreen.kt:131-141` | exact |
| 12 screens' refresh-failed `showSnackbar` (Help, Conversations, ChatThread, Explore, MyBookings, IncomingRequests, ExpertDashboard, Availability, ExpertBookings, Transactions, Payouts, Notifications) | screen | event-driven | `SignupOverviewScreen.kt` + `ConversationsScreen.kt:153-158` | exact |
| main `ui/theme/ContentWidth.kt` (NEW) | utility | transform | research Code Example 6 | no analog |
| `AndroidManifest.xml` | config | n/a | research Code Example 5 | no analog |
| `SignupVerifyEmailViewModel.kt`, `SetNewPasswordViewModel.kt` (remove OTP mirror) | viewmodel | request-response | `SignupBasicInfoViewModel.kt` (touched-flag mirror) | role-match |
| `ClientProfileSettingsViewModel`, `ExpertProfileSettingsViewModel`, `ExpertTaxProfileViewModel`, `DeleteAccountViewModel` (add SavedStateHandle) | viewmodel | CRUD | `screens/signup/basicinfo/SignupBasicInfoViewModel.kt` | exact |
| `components/RescheduleResponseCard.kt` DeclineDialog -> sheet (R7) | component | request-response | `screens/expertmode/requestdecision/subviews/DeclineRequestSheet.kt` | exact |
| `components/ValidatedField.kt` (accessible name) | component | request-response | itself (semantics imports at lines 47-60) | exact |
| `components/AsyncSaveButton.kt` (busyDescription) | component | request-response | itself (line ~60) | exact |
| `LoginScreen.kt` (heading, single sign-up node) | screen | request-response | `EmptyState` heading semantics | partial |
| Cutout handling at MeetingCallScreen / roots | screen | transform | `MeetingCallScreen.kt:84` `safeDrawing.only(Horizontal)` | role-match |
| Zero-inset Scaffold snackbar (Availability, ExpertTaxProfile) | screen | event-driven | `WriteReviewFooter.kt:62-63` (`navigationBarsPadding`) | partial |
| `scripts/check-design-rules.sh` (insets rule) | config | batch | itself (grep + `fail=1` blocks) | exact |
| test `testing/A11yAudit.kt`, `InsetsTestSupport.kt` (NEW) | test util | transform | research Code Examples 2-3 | no analog |
| New per-screen state compliance tests | test | request-response | test `screens/expertmode/dashboard/ExpertDashboardScreenTest.kt:275-287`, `components/ControlsTest.kt` | exact |
| New SavedStateHandle/restoration tests | test | event-driven | test `screens/clientmode/makeabooking/EntrySavedStateHandleTest.kt` | exact |
| `15-DEVICE-CHECKLIST.md` | doc | n/a | Phase 13 checklist plan (per D-21) | not examined |

## Pattern Assignments

### `components/EmptyState.kt` ErrorState (component)
**Current** (lines 77-90):
```kotlin
@Composable
fun ErrorState(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EmptyState(
        icon = Icons.Outlined.CloudOff,
        heading = stringResource(R.string.error_state_heading),
        body = stringResource(R.string.error_state_body),
        modifier = modifier,
        action = EmptyStateAction(stringResource(R.string.error_state_retry), onRetry),
    )
}
```
Add `heading/body/retryLabel` params defaulting to these strings, add live region, and heading() on the EmptyState heading (RESEARCH Code Example 1). Existing `ControlsTest` must stay green.

### `components/RefreshFailedSnackbar.kt` (utility, event-driven)
**Analog:** `screens/signup/overview/SignupOverviewScreen.kt:131-141` (full retry pattern):
```kotlin
LaunchedEffect(state.refreshFailed) {
    if (state.refreshFailed) {
        val result = snackbarHostState.showSnackbar(
            message = failedMessage, actionLabel = retryLabel, duration = SnackbarDuration.Indefinite)
        if (result == SnackbarResult.ActionPerformed) onRefresh()
        onRefreshErrorShown()
    }
}
```
Use `SnackbarDuration.Long` in the helper; dismiss `currentSnackbarData` first.

### The 12 call sites (screens)
**Analog (the no-Retry form to replace):** `screens/chat/conversations/ConversationsScreen.kt:153-158`:
```kotlin
LaunchedEffect(state.refreshFailed) {
    if (state.refreshFailed) {
        scope.launch { snackbarHostState.showSnackbar(refreshFailedMessage, duration = SnackbarDuration.Long) }
        onRefreshFailureConsumed()
    }
}
```
Keep the `scope.launch` then consume ordering (Pitfall 5). Pass the existing `onRetry`/refresh lambda (ConversationsScreen has `onRetry` at line 124). Quiet reloads stay silent.

### SavedStateHandle mirrors (settings/tax/delete ViewModels)
**Analog:** `screens/signup/basicinfo/SignupBasicInfoViewModel.kt`
```kotlin
private const val KEY_FIRST_NAME = "basic_first_name"
private const val KEY_TOUCHED = "basic_touched"
private const val KEY_LANGUAGE_IDS = "basic_language_ids"   // stored as ArrayList<String>
...
private val savedStateHandle: SavedStateHandle,              // constructor, line 113
private var touched = savedStateHandle.get<Boolean>(KEY_TOUCHED) == true   // line 121
firstName = savedStateHandle.get<String>(KEY_FIRST_NAME).orEmpty(),        // line 170
val ids = savedStateHandle.get<ArrayList<String>>(KEY_LANGUAGE_IDS).orEmpty() // line 179
```
Write on each edit; seed server data only when `touched` is false. Never mirror TIN or deletion password. Passwords/OTP: remove `KEY_CODE` usages (`SignupVerifyEmailViewModel.kt:75,169,237,289`; `SetNewPasswordViewModel.kt:58,74,207`), keep `KEY_CODE_SENT` and cooldown keys.

### `components/RescheduleResponseCard.kt` DeclineDialog (R7)
**Current:** `AlertDialog(` ~line 445 with `OutlinedTextField` (`reason`, `onReasonChange`, `RESCHEDULE_DECLINE_REASON_TAG`, counter text `reschedule_decline_reason_counter`).
**Analog:** `screens/expertmode/requestdecision/subviews/DeclineRequestSheet.kt` uses `ModalBottomSheet` (line 108) with `rememberModalBottomSheetState` (111) and `.imePadding()` (154). Keep the same string resources, test tag, and `rememberSaveable` state (`showDecline`, `declineText` at lines 101-103).

### `ValidatedField.kt`
Imports `semantics.error` and `semantics` already (lines 47-48). Fix: keep a label name when filled (RESEARCH Gap table POL-04). Verify on the device (Pitfall 9).

### `scripts/check-design-rules.sh`
**Pattern to copy** (lines 9-14):
```bash
hits=$(grep -rnE --include='*.kt' '<regex>' "$SRC_DIR" | grep -v "<exclusion>" || true)
if [ -n "$hits" ]; then
    echo "<message>:"
    echo "$hits"
    fail=1
fi
```
Add the insets rule (a)(b)(c) and the AlertDialog+TextField rule in this form, with `// insets-ok:` opt-out comments on the 4 `HomeTabScaffold` delegates.

### Tests
**State compliance:** `ExpertDashboardScreenTest.kt:275-287`:
```kotlin
val f = fixture(bookingsFail = true)
setContent(f)
rule.onNodeWithText("Couldn't load dashboard").assertExists()
f.api.expertBookingsHandler = { ExpertBookingsPayload() }
rule.onNodeWithText("Retry").performClick()
rule.onNodeWithText("Hello, Ada").assertExists()
```
**Restoration:** `EntrySavedStateHandleTest.kt`: `@RunWith(RobolectricTestRunner::class)`, `createComposeRule()`, real `HomeClientShell` with entry decorators. ViewModel-level: new VM with `SavedStateHandle(mapOf(...))` (RESEARCH Code Example 4).
**Insets/a11y helpers:** no repo analog; use RESEARCH Code Examples 2-3 (`@Config(qualifiers = "w360dp-h800dp")`).

## Shared Patterns
- **Scaffold insets:** `padding(innerPadding).consumeWindowInsets(innerPadding)` then a single `imePadding()` (`AccountSecurityScreen.kt:85-86`, `SignupBasicInfoScreen.kt:160-161`); pinned footers use `navigationBarsPadding()` then `imePadding()` (`WriteReviewFooter.kt:62-63`).
- **Error mapping:** `ApiException.toSectionMessage()` in `screens/clientmode/makeabooking/BookingErrors.kt:67`.
- **Loading:** `SkeletonGroup` / `SkeletonBlock` (do not rework compliant screens, D-08).
- **Strings:** new per-screen strings go in that screen's own `strings_*.xml`; shared ones in Wave 0 only.
- **Design gates:** no `Color(0x` outside `ui/theme/Color.kt`, no `fontSize` outside the theme, no hard-coded `Text("...")`.

## No Analog Found
| File | Role | Reason |
|---|---|---|
| `ui/theme/ContentWidth.kt` | utility | No max-width modifier exists; use RESEARCH Code Example 6 |
| `AndroidManifest.xml` portrait lock | config | No orientation attribute exists; RESEARCH Code Example 5 (MainActivity only, not ConnectReturnActivity) |
| `testing/A11yAudit.kt`, `InsetsTestSupport.kt` | test util | No WindowInsets or a11y tests exist; RESEARCH Code Examples 2-3 |
| `RefreshFailedSnackbar.kt` extraction | utility | Only inline versions exist (see above) |

## Metadata
**Analog search scope:** components/, screens/signup, screens/chat, screens/expertmode, scripts/, test tree
**Pattern extraction date:** 2026-10-29
**Note:** The Phase 13 checklist file and the other 11 snackbar call sites were not individually opened; line numbers for them come from RESEARCH.md.

---
phase: 08-client-bookings-cancellation
reviewed: 2026-10-24T01:31:52Z
depth: standard
files_reviewed: 86
files_reviewed_list:
  - app/src/main/java/com/uptrendinvestments/clickmeapp/components/BookingStatusBadge.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/components/RescheduleResponseCard.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/components/buttonstyles/DestructiveOutlinedButton.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/requests/BookingActionRequests.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/BookingActionResponses.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/ClientBookingDetail.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/ClientBookingsPayload.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/network/ApiRequestUrls.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/network/ClickMeApi.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/bookingsummary/BookingSummaryKey.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/bookingsummary/BookingSummaryScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/bookingsummary/BookingSummaryUiState.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/bookingsummary/BookingSummaryViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/bookingsummary/subviews/BookingSummaryCard.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/cancellation/CancelBookingKey.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/cancellation/CancellationReason.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/cancellation/ClientCancellationScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/cancellation/ClientCancellationUiState.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/cancellation/ClientCancellationViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/cancellation/RefundPreview.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/cancellation/subviews/CancellationConfirmStep.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/cancellation/subviews/CancellationReasonStep.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/ExploreScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/subviews/ExploreTodaysSessionsSection.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/home/ClientBookingFlowEntries.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/home/ClientBookingManagementEntries.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/home/HomeClientScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/BookingFormatting.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/BookingBuckets.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/BookingStatusDisplay.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/BookingsLaunchRequest.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/ClientBookingsRepository.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/MyBookingsScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/MyBookingsUiState.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/MyBookingsViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/RescheduleDisclosure.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/RescheduleErrors.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/RescheduleResponse.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/RescheduleUiState.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/subviews/MyBookingsStates.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/subviews/PastBookingCard.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/subviews/UpcomingBookingCard.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingKey.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingUiState.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/subviews/UpcomingBookingCards.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/subviews/UpcomingBookingJoinCard.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/subviews/UpcomingBookingStatusRow.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/util/Money.kt
  - app/src/main/res/values/strings_booking_summary.xml
  - app/src/main/res/values/strings_cancel_booking.xml
  - app/src/main/res/values/strings_my_bookings.xml
  - app/src/main/res/values/strings_reschedule_response.xml
  - app/src/main/res/values/strings_upcoming_booking.xml
  - app/src/test/java/com/uptrendinvestments/clickmeapp/components/BookingStatusBadgeTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/components/RescheduleResponseCardTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/navigation/BookingFlowNavigationTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/network/ApiRequestUrlsTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/network/ClickMeApiTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/network/ResponseDecodingTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/HomeShellsTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/bookingsummary/BookingSummaryScreenTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/bookingsummary/BookingSummaryViewModelTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/cancellation/CancellationReasonTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/cancellation/ClientCancellationScreenTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/cancellation/ClientCancellationViewModelTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/cancellation/RefundPreviewTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/ExploreScreenTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/ExploreSectionsTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/BookingFormattingTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/BookingBucketsTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/BookingManagementFixtures.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/BookingStatusDisplayTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/BookingsLaunchRequestTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/ClientBookingsRepositoryTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/MyBookingsScreenTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/MyBookingsViewModelTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/RescheduleDisclosureTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/RescheduleErrorsTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/RescheduleResponseTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingScreenTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingViewModelTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/testing/FakeClickMeApi.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/testing/FakeRealtimeConnection.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/util/MoneyTest.kt
findings:
  critical: 0
  warning: 6
  info: 6
  total: 12
status: issues_found
---

# Phase 8: Code Review Report

**Reviewed:** 2026-10-24T01:31:52Z
**Depth:** standard
**Files Reviewed:** 86 (55 production and resource files read in full; 31 test files checked for false-green patterns)
**Status:** issues_found

## Summary

The review covered the Phase 8 client booking-management code: My Bookings, Upcoming Booking Details, Cancellation, Booking Summary, the shared reschedule card and controller, the new endpoints and models, `Money.formatMajor` and the client-shell wiring. Backend behavior was checked against `backend-reference/src/controllers/clientBooking.controller.js`, `utils/refund.js` and `services/events.js`. Locked decisions D-01..D-16 were treated as intended. That includes page 1 only, the singleton launch hand-off, Back not being blocked during reschedule responses, and the dormant captured-refund branches.

The cancel flow is solid. It is single-flight, Back is blocked while the request runs, the idempotent 200 is a success, and step, reason and comment survive process death. The refund preview matches `refund.js` (strictly more than 24h). The bucket and rejoin logic matches the backend status sets.

No blockers were found. The main problems are in the reschedule-response path:
- The promise that the booking reloads before success is shown can be broken by the server's own `booking.update` WebSocket event.
- The server's idempotent-200 replies are always reported as success.

Two navigation and data-freshness gaps came with the new entry points (Book Again, and Today's Sessions on Explore). `formatMajor` does not invert the backend's currency divisor for every currency.

Test files: no false-green patterns were found. The fake API's unstubbed handlers throw `notStubbed()`. The single-flight test gates on a `CompletableDeferred` and checks the request list. The tests did not cover the race in WR-01 or the navigation case in WR-03.

## Narrative Findings (AI reviewer)

## Warnings

### WR-01: The reload before the success notice can be cancelled, so a stale card becomes actionable again after a successful accept or decline

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/RescheduleResponse.kt:127-188`, `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingViewModel.kt:63-69,77-82,114-129`, `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/bookingsummary/BookingSummaryViewModel.kt:56-62,101-116`

**Issue:** The controller's KDoc promises: "After any response the booking is reloaded first and only then is the dialog or notice set. `submitting` stays set until the reload finished."

The reload is `{ startLoad(quiet = true).join() }`. `startLoad` begins with `loadJob?.cancel()`, so any later trigger cancels the reload job. Those triggers are:
- the realtime `booking.update` collector;
- `refreshQuietly()` on `ON_RESUME`, or on re-composition.

`Job.join()` returns normally when the joined job is cancelled. `succeed()` then sets the notice, and the `finally` block resets `submitting` to `None`. The newer load is still running at that point.

The backend sends a `booking.update` to both parties from inside the accept and decline handlers: `dispatchBookingEvent` in `services/events.js:428-439` runs before `res.json`. If that event reaches the socket after the HTTP response, which is a normal network ordering, this race happens.

During that window, and permanently if the quiet reload fails, which is swallowed (`if (!quiet)`), the screen looks like this:
- the "Reschedule accepted" snackbar is showing;
- the old `pending_reschedule` card is still there with Accept and Decline enabled;
- Cancel is enabled.

A Decline tap then gets the backend's idempotent 200 (`declineReschedule` returns 200 for any non-pending status) and shows "Reschedule declined" on a booking that was just accepted. See WR-02.

**Fix:** Mark the proposal resolved locally until a load confirms it, and make the reload wait for the latest load rather than the one it started.

```kotlin
// RescheduleUiState
data class RescheduleUiState(
    val submitting: RescheduleSubmitting = RescheduleSubmitting.None,
    val resolved: Boolean = false,          // hide or disable the card after a 2xx until the next load lands
    ...
)

// ViewModels: wait for whichever load is current
reload = {
    startLoad(quiet = true)
    while (true) {
        val job = loadJob ?: break
        job.join()
        if (job === loadJob) break         // no newer load superseded it
    }
}
```

In `succeed()`, set `resolved = true`. Clear it in `startLoad` once a load lands whose status is not `pending_reschedule`. Gate `ProposalActions` and the Cancel button on `!resolved`.

### WR-02: Idempotent-200 replies always show "accepted" or "declined", even when the proposal was resolved some other way

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/RescheduleResponse.kt:66-103`, `app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/BookingActionResponses.kt:22-26`

**Issue:** `RescheduleResponder` ignores `RescheduleResponseData.status`. The backend returns 200 in these cases:
- `declineReschedule`: whenever `status !== 'pending_reschedule'`, whatever the current status is, including `cancelled`, `completed`, or `confirmed` after an accept. The reply carries `data.status` set to that current status (controller lines 680-686 and 732-738).
- `acceptReschedule`: whenever the booking is `confirmed`. That also happens when the expert withdrew the proposal, or the client declined it on another device (lines 461-467).

So the screen shows "Reschedule declined" for a booking the expert has cancelled, and "Reschedule accepted" while the reloaded card shows the original time. The UI tells the client something about their session that is not true.

**Fix:** After the reload, decide the notice from the reloaded detail, not from the 2xx alone. The controller already reloads first.

```kotlin
// Accept: success only if the booking now sits at the proposed start.
val accepted = reloaded.status == "confirmed" && reloaded.startTime == proposal.proposedStart
// Decline: success only if it returned to an active status at the original start.
val declined = reloaded.status in setOf("confirmed", STATUS_PENDING_APPROVAL) && reloaded.startTime == proposal.currentStart
```

If either check fails, show `RescheduleDialog.Error(FieldError.Res(R.string.reschedule_error_not_pending))` instead of the success notice. To support this, pass the proposal snapshot into `accept()` and `decline()`, and have `reload` return the detail.

### WR-03: Book Again, then Request Sent, then "Back to Home" leaves Request Sent on top of the Bookings stack

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/home/ClientBookingManagementEntries.kt:30,76-83`, `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/home/ClientBookingFlowEntries.kt:43-57`

**Issue:** Book Again (D-13) pushes `BookingKey` onto the Bookings stack. Make a Booking then calls `replaceTop(RequestSentKey)`, so the Bookings stack becomes `[ClientBookingsRoot, BookingSummaryKey, RequestSentKey]`.

Request Sent's "Back to Home" is `navigator::showExplore`, which is `resetAndSelect(ExploreRoot, setOf(ExploreRoot))`. That resets only Explore. The next time the user taps the Bookings tab, they land on the stale Request Sent confirmation. Its bottom bar is hidden, and Back from it goes to the old Summary.

The KDoc on `clientBookingFlowEntries` says "it lands on Explore even if a future entry point starts the flow from another tab". Phase 8 adds that entry point, and the tab the flow started from is left dirty.

**Fix:** Reset the tab the flow ran in as well, or always reset both booking-capable stacks.

```kotlin
fun Navigator.showExplore() = resetAndSelect(ExploreRoot, setOf(ExploreRoot, ClientBookingsRoot))
```

Alternatively, add a `showExploreFrom(currentTab)` that resets `state.topLevelRoute` too. Add a `HomeShellsTest` case for Summary, then Book Again, then Request Sent, then Back to Home, then the Bookings tab.

### WR-04: Explore's Today's Sessions is never refreshed after a cancel, accept or decline started from it

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/home/HomeClientScreen.kt:89-95`, `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/home/ClientBookingManagementEntries.kt:27`, and the Phase 5 `explore/ExploreViewModel.kt:169` (`loadTodaysSessions` runs only on home load and refresh)

**Issue:** Phase 8 makes Today's Sessions a way into Upcoming Booking Details, and from there into Cancel and the reschedule responses. `showMyBookings` resets the Explore stack to its root but keeps the root entry and its ViewModel, as intended. That ViewModel has no realtime `booking.update` collection and no reload when the screen is shown again.

So after a cancel, the cancelled session stays in Today's Sessions until a manual pull-to-refresh. Tapping it opens a details screen showing a cancelled booking with the Join card. After an accepted reschedule, the strip shows the old time. My Bookings got all three D-10 triggers; this second entry point got none.

**Fix:** Have `ExploreViewModel` collect `realtime.events.filterIsInstance<RealtimeEvent.BookingUpdate>()` and call `loadTodaysSessions()`. The backend emits `booking.update` for cancel, accept and decline. Alternatively, reload Today's Sessions from a `LaunchedEffect(Unit)` and `ON_RESUME` hook in `ExploreScreen`, the same way My Bookings does it.

### WR-05: `Money.formatMajor` does not invert the backend's divisor for 3-decimal currencies, or for zero-decimal currencies outside the backend's 7-code set

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/util/Money.kt:34-43`

**Issue:** The backend computes `refund_amount = minor / divisor`. The divisor is 1 for its hard-coded set (JPY, KRW, VND, CLP, ISK, XOF, XAF) and 100 for every other currency (`minorUnitDigitsFor`, controller lines 165-170).

`formatMajor` multiplies by `10^Currency.defaultFractionDigits` instead:
- 3-decimal currencies (KWD, BHD, JOD, OMR, TND): the backend divides by 100 and Android multiplies by 1000, so the amount shows 10 times too large. 5000 minor becomes 50.0, then 50000 minor, shown as "KWD 50.000".
- Java zero-decimal currencies outside the backend set (UGX, PYG, RWF, KMF, GNF, BIF, DJF, VUV, XPF): the backend divides by 100 and Android multiplies by 1, so the amount shows 100 times too small.

The value lands in the "Booking cancelled. {amount} will be refunded" notice, which the user reads as a statement about their money. `MoneyTest.backendZeroDecimalSet_hasZeroFractionDigitsOnThisJvm` only checks one direction (backend set ⊆ Java zero-digit). The refund branch is dormant on today's backend (RESEARCH Finding 2), so this is a WARNING rather than a BLOCKER.

**Fix:** Invert the backend's divisor exactly, then format through `format()`. Keep the backend list in one constant and test it in both directions.

```kotlin
private val BACKEND_ZERO_DECIMAL = setOf("JPY", "KRW", "VND", "CLP", "ISK", "XOF", "XAF")

fun formatMajor(major: Double, currencyCode: String, locale: Locale = Locale.getDefault()): String {
    val code = currencyCode.uppercase(Locale.ROOT)
    val backendDigits = if (code in BACKEND_ZERO_DECIMAL) 0 else 2
    val minorFromBackend = BigDecimal.valueOf(major).movePointRight(backendDigits).setScale(0, RoundingMode.HALF_UP)
    // Re-express in this JVM's minor units before calling format().
    val javaDigits = fractionDigits(currencyOrNull(code))
    val minor = minorFromBackend.movePointLeft(backendDigits).movePointRight(javaDigits)
        .setScale(0, RoundingMode.HALF_UP).toLong()
    return format(minor, code, locale)
}
```

Also file a backend-owner note: `minorUnitDigitsFor` disagrees with Stripe's zero-decimal and 3-decimal currency lists.

### WR-06: The Accept dialog stays open when the proposal changes underneath it, and it never shows the time being accepted

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/components/RescheduleResponseCard.kt:94-131,331-385`

**Issue:** `showAccept` is `rememberSaveable { mutableStateOf(false) }` with no key. The detail screens reload under the dialog on realtime `booking.update`, on resume, and after returning from Payment Methods. The accept call carries no proposal identifier (backend: `acceptReschedule` accepts whatever `proposed_start` is current).

If the expert withdraws and re-proposes a different time while the dialog is open, the client's Accept commits them to a time the dialog never showed. The body only says "The session will move to the new time the expert proposed." The card behind the scrim changes, but the dialog does not.

**Fix:** Key the dialog state on the proposal so a changed proposal closes it, and put the proposed slot in the dialog body.

```kotlin
var showAccept by rememberSaveable(proposal.proposedStart) { mutableStateOf(false) }
var showDecline by rememberSaveable(proposal.proposedStart) { mutableStateOf(false) }
// AcceptDialog body: prepend formatProposalSlot(proposal.proposedStart, zone, locale, timeFormat)
```

## Info

### IN-01: A user's pull-to-refresh failure is silently dropped when a quiet reload supersedes it

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/MyBookingsViewModel.kt:147-175`

**Issue:** A realtime event or `onScreenShown` that arrives during a Pull calls `startLoad(Quiet)`, which cancels the Pull job. If the quiet load then fails, it clears `isRefreshing` without setting `refreshFailed`. The spinner disappears and the user gets no "Couldn't refresh" snackbar.

**Fix:** Keep the strongest pending mode: if the cancelled job was a Pull, run the replacement as a Pull.

### IN-02: The meeting link is copied to the clipboard without the sensitive flag

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/subviews/UpcomingBookingJoinCard.kt:166`

**Issue:** Zoom and Skype links often carry an embedded passcode (`?pwd=`). On API 33+ the clip shows in the system clipboard preview, and keyboards can sync it.

**Fix:** Mark the clip as sensitive:
`ClipData.newPlainText(label, joinLink).apply { description.extras = PersistableBundle().apply { putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true) } }`.

### IN-03: The character limits are hard-coded in the counter strings

**File:** `app/src/main/res/values/strings_cancel_booking.xml:15`, `app/src/main/res/values/strings_reschedule_response.xml:23`

**Issue:** `%1$d/450` and `%1$d/500` repeat `CANCEL_COMMENT_MAX_CHARS` and `DECLINE_REASON_MAX_CHARS`. Changing a constant would leave the counter wrong.

**Fix:** Use `%1$d/%2$d` and pass the constant.

### IN-04: Duplicated load and notice logic, and a misused motion constant

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingScreen.kt:153-173,270-300`, `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/bookingsummary/BookingSummaryScreen.kt:131-151,233-264`, `UpcomingBookingViewModel.kt:93-129`, `BookingSummaryViewModel.kt:80-116`

**Issue:** Several pieces are copied between the two detail screens and ViewModels:
- the `startLoad`, `refreshQuietly` and `onScreenHidden` state machine;
- the `lastProposal` and `AnimatedVisibility` card block;
- the snackbar notice effect;
- the dialog `popOnDismiss` handling.

Any fix for WR-01 or WR-06 has to be made twice. The card animation also uses `Motion.STRENGTH_METER_VISIBILITY_MS`, a password-meter constant.

**Fix:** Extract a `QuietReloader` helper and a `RescheduleSection` composable, and add a named `Motion` constant for the card animation.

### IN-05: The rejoin rule is evaluated only at load time

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/BookingBuckets.kt:26`, `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/MyBookingsViewModel.kt:181`

**Issue:** `clock.instant()` is read once per load. A `completed` session stays in Upcoming, with the Join placeholder, after its `endTime` has passed, until the next reload trigger.

**Fix:** This is acceptable for MVP. When Phase 9 makes Join live, re-derive the lists on a timer, or at the earliest rejoinable `endTime`.

### IN-06: The expert timezone note takes its abbreviation from the device-zone date, and shows the backend's `UTC` fallback as if it were real

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingScreen.kt:309`, `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/subviews/UpcomingBookingCards.kt:197-224`

**Issue:**
- `details.start.atZone(zone).toLocalDate()` uses the viewer's zone. Near a DST change the expert-zone abbreviation can be wrong; the instant itself should be used.
- `getDetail` falls back to `'UTC'` when the expert has no availability schedule. The screen then shows "Expert timezone: UTC (UTC)" as if it were the expert's real zone.

**Fix:** Pass `details.start` and use `zone.rules.isDaylightSavings(instant)` when building the abbreviation. Optionally hide the note for `UTC` / `Etc/UTC` until the backend can tell "unknown" apart from a real UTC zone.

---

_Reviewed: 2026-10-24T01:31:52Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_

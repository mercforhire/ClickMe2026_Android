---
phase: 11-expert-requests-bookings
reviewed: 2026-10-26T00:00:00Z
depth: standard
files_reviewed: 131
files_reviewed_list:
  - app/src/main/java/com/uptrendinvestments/clickmeapp/components/ExpertRescheduleBadge.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/components/RescheduleResponseCard.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/CallDialog.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/requests/ExpertBookingRequests.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/BookingRequestsPayload.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/EarningsBreakdownDto.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/ExpertBookingsPayload.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/ExpertFinancialData.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/network/ApiRequestUrls.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/network/ClickMeApi.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/CallDialogs.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/RescheduleErrors.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/RescheduleResponse.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/ExpertDisplay.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/ExpertLaunchRequest.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/ExpertNavigation.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/availability/AvailabilityKey.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryEntries.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryKey.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryUiState.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertRescheduleErrors.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertRescheduleResponder.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/subviews/ExpertMessageCard.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/subviews/ExpertSummaryCard.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/cancellation/ExpertCancelEntries.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/cancellation/ExpertCancelErrors.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/cancellation/ExpertCancelKey.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/cancellation/ExpertCancelReason.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/cancellation/ExpertCancellationScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/cancellation/ExpertCancellationUiState.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/cancellation/ExpertCancellationViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/cancellation/ExpertRefundPreview.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/cancellation/subviews/ExpertCancelConfirmStep.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/cancellation/subviews/ExpertCancelReasonStep.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/confirmed/BookingConfirmedEntries.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/confirmed/BookingConfirmedKey.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/confirmed/BookingConfirmedScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/confirmed/BookingConfirmedViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/confirmed/CalendarIntent.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/confirmed/subviews/ConfirmedDetailsCard.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/DashboardFinancials.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/DashboardLogic.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/ExpertDashboardRoute.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/ExpertDashboardScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/ExpertDashboardUiState.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/ExpertDashboardViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/ExpertFinancialRepository.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/subviews/DashboardHeader.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/subviews/DashboardSkeleton.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/subviews/ExpertToolsGrid.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/subviews/FinancialSummarySection.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/subviews/TodaysSessionsSection.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/home/HomeExpertScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/incomingrequests/BookingRequestsRepository.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/incomingrequests/IncomingRequestsEntries.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/incomingrequests/IncomingRequestsKey.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/incomingrequests/IncomingRequestsLogic.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/incomingrequests/IncomingRequestsScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/incomingrequests/IncomingRequestsUiState.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/incomingrequests/IncomingRequestsViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/incomingrequests/subviews/IncomingRequestCard.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/incomingrequests/subviews/IncomingRequestsSkeleton.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/DeclineReason.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/RequestDecisionEntries.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/RequestDecisionErrors.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/RequestDecisionKey.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/RequestDecisionScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/RequestDecisionUiState.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/RequestDecisionViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/RequestEarnings.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/subviews/AcceptRequestSheet.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/subviews/DeclineRequestSheet.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/subviews/RequestActionFooter.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/subviews/RequestDecisionBlocks.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/subviews/RequestEarningsSection.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/ExpertBookingBuckets.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/ExpertBookingsRepository.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/ExpertBookingsRoute.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/ExpertBookingsScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/ExpertBookingsUiState.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/ExpertBookingsViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/subviews/ExpertBookingsSkeleton.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/subviews/ExpertPastCard.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/subviews/ExpertUpcomingCard.kt
  - app/src/main/res/values/strings_booking_confirmed.xml
  - app/src/main/res/values/strings_call.xml
  - app/src/main/res/values/strings_expert_booking_summary.xml
  - app/src/main/res/values/strings_expert_bookings.xml
  - app/src/main/res/values/strings_expert_cancel.xml
  - app/src/main/res/values/strings_expert_dashboard.xml
  - app/src/main/res/values/strings_expert_shared.xml
  - app/src/main/res/values/strings_incoming_requests.xml
  - app/src/main/res/values/strings_request_decision.xml
  - app/src/main/res/values/strings_reschedule_response.xml
  - app/src/test/java/com/uptrendinvestments/clickmeapp/components/RescheduleResponseCardTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/navigation/ExpertNavigationTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/network/ApiRequestUrlsTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/network/BookingRequestsApiTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/network/ExpertBookingsApiTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/network/ExpertFinancialApiTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/HomeShellsTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/call/calling/CallDialogsTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/RescheduleResponseTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/ExpertLaunchRequestTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/ExpertSharedUiTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryScreenTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryViewModelTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertRescheduleErrorsTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/cancellation/ExpertCancellationScreenTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/cancellation/ExpertCancellationViewModelTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/cancellation/ExpertRefundPreviewTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/confirmed/BookingConfirmedScreenTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/confirmed/CalendarIntentTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/DashboardFinancialsTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/DashboardLogicTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/ExpertDashboardScreenTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/ExpertDashboardViewModelTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/incomingrequests/IncomingRequestsLogicTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/incomingrequests/IncomingRequestsScreenTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/incomingrequests/IncomingRequestsViewModelTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/DeclineReasonTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/RequestDecisionScreenTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/RequestDecisionViewModelTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/RequestEarningsSectionTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/ExpertBookingBucketsTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/ExpertBookingsScreenTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/ExpertBookingsViewModelTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/testing/ExpertBookingFixtures.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/testing/FakeClickMeApi.kt
findings:
  critical: 0
  warning: 8
  info: 6
  total: 14
status: issues_found
---

# Phase 11: Code Review Report

**Reviewed:** 2026-10-26
**Depth:** standard
**Files Reviewed:** 131 (all `app/src/main` files read in full; test files only skimmed for obvious reliability problems)

## Summary

The expert request, booking, cancellation, reschedule and dashboard code is carefully built. I cross-checked the wire contracts against `backend-reference` (accept, decline, cancel and reschedule schemas, the feed and detail shapes, `buildEarningsBreakdown`). The checks covered:

- **Money units:** weekly and payout amounts are minor units and everything else is major. `Money.formatMajor` and `Money.format` are used on the matching sides, with no stray `/100`.
- **Request bodies:** the strict-schema bodies are correct. `AcceptBookingRequestBody(null)` serialises to `{}`, and the decline body always carries both keys.
- **Logging:** no logging calls exist in the reviewed main sources. The PII and token-logging concern is clean.
- **Custom Tab URL:** the Stripe link is checked for an https scheme with a host, it is not cached, and it is cleared from state before it is opened.
- **Navigation:** `replaceTop`, `resetAndSelect` and the duplicate-key `origin` trick behave as documented.

I found no BLOCKER-level defects. The warnings below are real behavioural or spec deviations that should be fixed:

- A stale single-use Stripe link can open unprompted.
- A lost-race cancel is reported as a success without being verified.
- Estimated earnings are shown without the required qualifier.
- A transient failure on a quiet dashboard reload wipes the financial figures.
- The "soft" financial fetches gate the whole dashboard.
- The decision screen never evaluates expiry itself.
- The reschedule banner count disagrees with the Bookings list.
- An accept message is offered to the expert but discarded by the backend.

## Warnings

### WR-01: A single-use Stripe onboarding link can be opened unprompted long after it was minted

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/ExpertDashboardScreen.kt:165-169` (state set in `ExpertDashboardViewModel.kt:185-208`)

**Issue:**

- `startPayoutSetup()` stores the URL in `ExpertDashboardUiState.onboardingUrl` and relies on the composed Dashboard to consume it in a `LaunchedEffect`.
- Nav3 only composes the top entry. If the expert taps "Complete payout setup", then navigates away (Incoming Requests, a tab switch, backgrounding) before the mint finishes, the URL sits in the ViewModel.
- When the Dashboard is composed again, possibly many minutes later, the effect fires and opens a Custom Tab with a link that has expired. The link lives about 5 minutes. The user sees an unexpected browser tab showing a Stripe error.
- While `onboardingUrl != null`, `startPayoutSetup()` also treats the ViewModel as busy, so a tap is ignored. The effect fires on re-entry anyway.
- The effect also runs regardless of the lifecycle state, so it can launch a browser while the activity is paused.

**Fix:** Drop the URL when the screen is hidden, or gate the launch by lifecycle and age. For example:

```kotlin
// ViewModel
fun onScreenHidden() {
    loadIsCurrent = false
    _uiState.update { it.copy(onboardingUrl = null) }   // a minted link must not outlive the screen it was minted for
}
```

or use `LifecycleResumeEffect` and ignore a URL older than about 60 seconds. If the URL is dropped, also clear `isMintingLink` handling so the card is tappable again.

### WR-02: A lost-race `ALREADY_CANCELLED` is reported to the expert as a successful cancel without verification

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/cancellation/ExpertCancellationViewModel.kt:161-168` (rule documented in `ExpertCancelErrors.kt:12-16`)

**Issue:**

- In the backend (`expertBooking.controller.js:1087-1097`), `ALREADY_CANCELLED` is thrown whenever the atomic `UPDATE ... WHERE status IN (cancellable)` matches no row.
- That happens when the status changed between the read and the update to anything non-cancellable, for example `in_progress`, `completed`, `missed`, or a cancel by the client. It is not guaranteed to mean "cancelled".
- The ViewModel always treats this as success. It posts the Past-tab hand-off with a "Booking cancelled" notice and resets the stacks, even if the session is actually live.

**Fix:** Do not assume success. Treat it like `INVALID_STATUS` (the "booking changed" dialog and return to a refreshed Bookings list) or reload the detail and branch on `status.code == "cancelled"`. At minimum, post `BookingsLaunch(BookingsTab.Upcoming)` with no "cancelled" notice and let the refreshed lists show the truth.

### WR-03: Estimated (`expert_default`) earnings are shown as exact on the request and booking cards

**Files:**
- `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/subviews/ExpertUpcomingCard.kt:73-78`
- `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/incomingrequests/IncomingRequestsLogic.kt:74-88`

**Issue:**

- `backend-reference/docs/ios/EXPERT-EARNINGS-BREAKDOWN.md` section 3 (line 144) requires the expert card's earnings label to branch on `fee_source`: `booking` is plain, `expert_default` carries an approximate qualifier, `unavailable` shows nothing or "pending".
- iOS does this in `UpcomingBookingsViewModel.formatEarnings` by prefixing "≈".
- `ExpertBookingItem.netEarningsText` and `requestCardEarnings` ignore `feeSource` and format `net_amount` as final. An estimate based on the expert's default rate is presented as a confirmed payout.
- The Request Decision screen handles this correctly (`Receipt.estimated`). The cards are inconsistent with that screen.

**Fix:** Pass the fee source through and prefix the formatted amount for estimates:

```kotlin
val text = Money.formatMajor(net, currency, locale)
return if (breakdown.feeSource == FEE_SOURCE_EXPERT_DEFAULT) "≈ $text" else text
```

Do the same in `requestCardEarnings`, for example by adding an `estimated` flag to `RequestCardEarnings.Net`.

### WR-04: A transient soft-fetch failure on a quiet reload wipes the financial figures and the payout card

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/ExpertDashboardViewModel.kt:263-277, 286-305`

**Issue:**

- The class contract says a failed quiet reload "keeps the previous data".
- That holds for the hard half only. Every quiet reload (resume, realtime `booking.update`, Back to Home) re-runs `fetchFinancials()`.
- `softOrNull` turns any failure into `null`, and `applyLoaded` unconditionally assigns `weekly = financials.weekly`, `payoutSummary = financials.summary` and `onboarding = resolvePayoutOnboarding(...)`.
- A single flaky Stripe-backed call (`/payouts/summary` and `/connect/status` call out to Stripe) makes a screen showing "$120.00 / Available $40.00" flip to "—", and the payout-onboarding card appears or disappears, until the next successful reload.

**Fix:** In quiet and pull modes, keep the previous value when the new one is `null`:

```kotlin
weekly = financials.weekly ?: if (keepPrevious) state.weekly else null,
payoutSummary = financials.summary ?: if (keepPrevious) state.payoutSummary else null,
onboarding = resolved.takeUnless { it == PayoutOnboarding.Unknown && keepPrevious } ?: state.onboarding,
```

Only replace with `null` or `Unknown` on a Loud first load.

### WR-05: "Soft" financial fetches are serialized after the hard fetches and gate the Loaded state

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/ExpertDashboardViewModel.kt:238-243`

**Issue:**

- `startLoad` runs `fetch()` (bookings and requests), then `fetchFinancials()`, and only then updates `load = Loaded`.
- The financial calls are declared non-critical (they can never fail the load), yet they delay the first paint by a full extra network round trip. A slow `/payouts/summary` or `/connect/status` keeps the expert on the skeleton, and keeps the pull-to-refresh spinner up, for the whole timeout.
- A cancel (a newer `startLoad`) during the financial phase also discards the already-fetched hard data.

**Fix:** Start the three soft fetches in parallel with the hard fetches in one `coroutineScope`. Alternatively, publish the Loaded state as soon as the hard half returns and apply the financial results in a second `_uiState.update`.

### WR-06: Request Decision never evaluates expiry itself, so a stale list row stays acceptable

**Files:**
- `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/RequestDecisionKey.kt:21`
- `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/RequestDecisionViewModel.kt:48-49, 152`
- `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/incomingrequests/IncomingRequestsLogic.kt:94-101`

**Issue:**

- `isExpired` and `hoursLeft` are computed once at list-load time.
- `expiresAtMillis` is written into the key but never read anywhere in `app/src/main` (grep confirms), so it is dead data.
- If the expert leaves the list open past `expires_at`, the stale row is tapped. The decision screen opens as pending with live Accept and Decline buttons, and the sheet inputs are accepted. The server finally answers 409 `ALREADY_PROCESSED`, and the generic "Couldn't update request" dialog appears. This is wasted effort and a confusing message.
- The "Expires in Nh" label on the list is also frozen at load.

**Fix:**

- In `RequestDecisionViewModel`, seed `isExpired` as `key.isExpired || (key.expiresAtMillis != null && !Instant.ofEpochMilli(key.expiresAtMillis).isAfter(clock.instant()))`, which needs an injected `Clock`.
- Re-check it in `beginSubmitting`.
- Consider recomputing the list rows against the shared join clock tick.
- Otherwise remove the unused `expiresAtMillis` field.

### WR-07: The Dashboard "N Reschedule Requests" banner can disagree with the Bookings list and is silently capped at 50

**Files:**
- `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/DashboardLogic.kt:44-51`
- `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/ExpertBookingBuckets.kt:53`
- `ExpertDashboardViewModel.kt:258`

**Issue:**

- The banner count is `clientRescheduleCount()` over the raw first page of the `upcoming` bucket (limit 50, ascending by start).
- The banner click lands on the Bookings Upcoming tab, whose list is filtered with `statusCode in JOINABLE && endTime.isAfter(now)`.
- A `pending_reschedule` row whose session end has already passed (the server `upcoming` bucket is status-based and not time-bound) is counted by the banner but absent from the list the banner opens.
- Because the feed is ascending by start and capped at one 50-row page, stale past rows can also push real future reschedules off the page. The count is then too low.

**Fix:** Apply the same `endTime.isAfter(now)` predicate in `clientRescheduleCount`. Pass `now` in so it shares the Bookings rule. Consider documenting or logging the 50-row cap as a known undercount.

### WR-08: The Accept sheet collects a message that the backend discards

**Files:**
- `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/subviews/AcceptRequestSheet.kt:122-130`
- `app/src/main/java/com/uptrendinvestments/clickmeapp/model/requests/ExpertBookingRequests.kt:7-10`

**Issue:**

- The model KDoc admits the server "currently discards" `message`, but the UI still presents "Add a message (optional)" with a 1000-character counter and a placeholder like "Looking forward to our session!".
- The expert is led to believe the client will read it, and nothing indicates otherwise. This is user-visible false feedback, only partly mitigated by a plan-level follow-up.

**Fix:** Hide or disable the message field behind a constant such as `ACCEPT_MESSAGE_SUPPORTED = false` until the backend persists it. Alternatively, keep the field and add a visible caveat. At minimum, track the backend work with an owner.

## Info

### IN-01: Client free-text is copied into the nav back stack and into a calendar event

**Files:**
- `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/confirmed/BookingConfirmedKey.kt:14-23`
- `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/confirmed/CalendarIntent.kt:36`

**Issue:**

- `BookingConfirmedKey` serialises `clientMessage` (up to 1000 chars), the client name and the avatar URL into the saved nav back stack, which is parcelled across process death.
- `calendarEventSpec` then puts the client's private message into the calendar event description. Calendar providers often sync to a cloud account or share the event.
- Both are deliberate (D-06) but worth a second look as a privacy decision.

**Fix:** Consider dropping `clientMessage` from the key (the details are already in the ViewModel store) and omitting the message from the calendar description. A short "Message from client" is not needed in a calendar entry.

### IN-02: `isAllowedOnboardingUrl` does not pin the Stripe host

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/DashboardFinancials.kt:58-64`

**Issue:** Any https URL with a host passes. The URL comes from the authenticated backend, so the risk is low. Account Links are always on a fixed set of Stripe domains.

**Fix:** Allow only `connect.stripe.com` (and `*.stripe.com` if needed). That is a one-line defence-in-depth against a compromised or misconfigured backend.

### IN-03: `ExpertBookingSummaryViewModel.loadedDetail` is dead state

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryViewModel.kt:74-75, 180`

**Issue:** It is public and written on every load but never read in `app/src/main`. The comment says it exists for the reschedule card, which now builds from `ExpertSummaryDetails.proposal`.

**Fix:** Remove it, or make it `private` and keep it only if a test needs it.

### IN-04: Minor Compose hygiene

**Files:**
- `.../dashboard/ExpertDashboardScreen.kt:140`
- `.../bookingsummary/ExpertBookingSummaryScreen.kt:244-245`

**Issue:**

- `DateFormat.getTimeFormat(LocalContext.current)` is called inline, so it creates a new `DateFormat` on every recomposition, including each 30 s join-clock tick, and defeats skipping for the session cards. Other screens use `rememberDeviceTimeFormat()`.
- `LoadedBody` writes `lastProposal` during composition (`if (...) lastProposal = details.proposal`). Prefer a `SideEffect` or `derivedStateOf`-style holder to avoid backwards writes.

**Fix:** Use `rememberDeviceTimeFormat()` in the Dashboard, and move the `lastProposal` update into `LaunchedEffect(details.proposal)`.

### IN-05: Blank-but-non-null topics render as an empty line

**Files:**
- `.../incomingrequests/subviews/IncomingRequestCard.kt:91`
- `.../upcomingbookings/subviews/ExpertUpcomingCard.kt:140`
- `.../upcomingbookings/subviews/ExpertPastCard.kt:76`

**Issue:** These use `item.topic ?: fallback`, whereas the Dashboard and Summary use `takeIf { it.isNotBlank() }`. A topic of `""` shows a blank line. In `ExpertPastCard` the `lastIndexOf(topic)` span logic also degenerates for an empty topic.

**Fix:** `item.topic?.takeIf { it.isNotBlank() } ?: fallback`. Better, normalize once in the DTO mapping.

### IN-06: `launchCalendarInsert` only handles `ActivityNotFoundException`

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/confirmed/CalendarIntent.kt:62-72`

**Issue:** Some calendar apps and OEM providers throw `SecurityException` on `ACTION_INSERT` launch. This would crash the screen after the "Add to Calendar" tap.

**Fix:** `catch (_: ActivityNotFoundException) { onNoHandler() } catch (_: SecurityException) { onNoHandler() }`.

---

_Reviewed: 2026-10-26_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_

# Phase 8: Client Bookings & Cancellation - Context

**Gathered:** 2026-10-23
**Status:** Ready for planning

<domain>
## Phase Boundary

Port the iOS client booking-management screens:

1. **My Bookings** (BOOK-05). This replaces the `ClientBookingsRoot` placeholder.
   - Upcoming and Past tabs, fed by `GET /client/bookings?type=upcoming|past&page=1&limit=20`.
   - **Rejoin rule:** a `completed` booking whose `endTime` hasn't passed moves into Upcoming.
   - **Past labels:** Completed (`completed`), Cancelled (`cancelled`, `declined`) and Missed (`missed`, `expired`). Unknown statuses are dropped.
   - **Sorting:** Upcoming oldest first, Past newest first.
2. **Upcoming Booking Details** (BOOK-06).
   - ID row, expert card, topic and fee, date and time, expert timezone note, and a payment pill.
   - The "Waiting for expert" banner on `pending_approval`.
   - The join card, which stays inert until Phase 9.
   - The client's booking message, and Cancel.
   - There is **no Reschedule action**.
   - It gains the reschedule-response card (D-01).
3. **Cancellation** (BOOK-07): reason picker, optional comment, refund preview and confirm, using `POST /client/bookings/:id/cancel`.
4. **Booking Summary** (BOOK-08): a minimal summary, opened from Past items. Also used when a push opens a booking, once Phase 13 wires that.
5. **Reschedule response** (BOOK-09): accept or decline an expert-proposed reschedule.
   - `POST /client/bookings/:id/accept-reschedule` (`{}` or `{stripe_payment_method_id}`).
   - `POST /client/bookings/:id/decline-reschedule` (`{reason?}`).
   - The booking leaves `pending_reschedule`.
6. **Phase 6/7 hand-off:** the Today's Sessions card tap on Explore opens Upcoming Booking Details.

Out of scope:
- Starting a reschedule (DEF-1).
- Join and the join window (Phase 9; Join stays a disabled placeholder).
- Message Expert and chat (Phase 12).
- Leave a Review and the review Feedback block (Phase 13).
- Push deep links into these screens (Phase 13).
- The private note screen (deferred, D-16).
- Paging past page 1 (D-11).

</domain>

<decisions>
## Implementation Decisions

### Reschedule response (BOOK-09)
- **D-01: The response UI goes on Upcoming Booking Details.** An amber "Expert proposed a new time" card sits at the top of Upcoming Booking Details. It shows the current time, the proposed time, the expert's note, and Accept / Decline.
  - This is where the My Bookings card's "Expert proposed a new time · Tap to review" badge lands.
  - Booking Summary shows the same card, as one shared component, when it is opened for a `pending_reschedule` booking.
  - This deviates from iOS, where Accept/Decline exists only on Booking Summary and is unreachable from My Bookings. Log it as an iOS follow-up.
- **D-02: `PAYMENT_METHOD_REQUIRED` on accept mirrors iOS.**
  - Load saved cards and retry accept with the default card's `pm_` (or the first card if none is default).
  - With no saved card, offer "Add a Payment Method", which opens the Phase 7 Payment Methods screen. After returning, the client taps Accept again.
  - Device testing is blocked until a test-mode Stripe backend exists (see STATE "[Phase 7] Stripe key and mode").
- **D-03: Waiting on the expert.** If the proposal was not made by the expert (the client side proposed it, e.g. from iOS), the card is read-only with "Awaiting expert's response" and no buttons.
- **D-04: Decline has an optional reason.** The confirm dialog has an optional text field (500 characters max) and the buttons Decline / Keep. The reason is sent as `reason` and saved by the backend as `decline_reason_text`.
- **Claude's discretion:**
  - Copy and layout of the accept confirm dialog, including payment wording when the payment needs converting, as on iOS.
  - Handling of 403, 409 `INVALID_STATUS`, 422 `VALIDATION_ERROR` (the proposed time has passed) and `EXPERT_UNAVAILABLE`. Follow the Phase 6/7 error-copy patterns.
  - Refresh the booking after any response.

### Cancellation (BOOK-07)
- **D-05: The comment is merged into `reason`.** Keep the "Additional Comments (Optional)" field.
  - Send `reason` as `"<reason label>: <comment>"`, or just the label when there is no comment. Cap the whole string at 500 characters to match the backend's strict schema.
  - The reason labels are the iOS ones, sent as labels: "Change of plans", "Found another expert", "Schedule conflict", "Personal emergency", "Other".
  - This settles the Phase 8 open item from STATE and REQUIREMENTS. iOS drops the comment, so log an iOS follow-up.
  - Backend note: `reason` is validated but not saved today.
- **D-06: The refund preview applies the backend's 24-hour rule** (it mirrors `backend-reference/src/utils/refund.js`).

  | Booking | Preview line |
  |---|---|
  | Paid, more than 24h before start | "Full refund: {amount}" |
  | Paid, within 24h | "No refund — cancelling within 24 hours of the session" |
  | Free topic | no refund line |
  | Payment only held (not captured) | "The hold on your card will be released" (not "refunded") |

  The amount uses the Phase 7 Money formatter (minor units, JPY with no decimals). The server result stays authoritative (D-07). This deviates from iOS, which always promises the full price; log an iOS follow-up.
- **D-07: After a successful cancel, show the server's result.**
  - Show a brief confirmation using the response's `refund_amount` (major units) and `currency`: "Booking cancelled. {amount} will be refunded" when the amount is above 0, otherwise "Booking cancelled".
  - Then return to My Bookings on the Past tab, refreshed.
  - "Already cancelled" (an idempotent 200) is treated as success.
  - Error copy for 404 / 409 `INVALID_STATUS` / 500 is Claude's discretion, following existing patterns.
- **D-08: The cancel screen is full screen with two steps.** It is pushed, with a TopAppBar.
  - Step 1: reasons and comment, then Continue.
  - Step 2: a "Confirm Cancellation?" card with the booking summary and refund line, then Keep Booking / Confirm Cancellation.
  - System Back on step 2 returns to step 1. This matches the iOS flow.
- Cancel is allowed for `confirmed`, `pending_approval` and `pending_reschedule`, as the backend allows.

### My Bookings list (BOOK-05)
- **D-09: Page 1 only.** Load 20 items per tab, with no paging, matching iOS. This is a known limit: older bookings, and rejoinable completed sessions beyond the first 20 Past items, are not shown. Log it in STATE as a later improvement.
- **D-10: Three refresh triggers:**
  - pull-to-refresh;
  - a quiet reload when the screen or tab is shown again;
  - a quiet reload on a realtime `booking.update` from the Phase 3 socket. Keep raw status strings: Android already handles `rescheduled` and unknown statuses safely.
- **D-11: Empty states.**
  - Upcoming keeps iOS's "No Bookings Yet!" with an "Explore Experts" CTA that opens the Explore tab.
  - Past gets a simple "No past bookings yet" state with no CTA. iOS shows nothing here.
  - Loading uses skeleton cards. An error shows "Couldn't load bookings" with Retry.
- **D-12: Card actions.**
  - Upcoming cards show only a disabled Join placeholder. It follows the Today's Sessions D-02 pattern, and Phase 9 wires it up.
  - Reschedule is hidden for good (DEF-1). Message is hidden until Phase 12. Private Note is hidden (D-16).
  - Tapping an Upcoming card opens Upcoming Booking Details. Tapping a Past card opens Booking Summary.

### Booking Summary and the detail screen (BOOK-06, BOOK-08)
- **D-13: Booking Summary shows details plus Book Again.**
  - Expert card, topic, date and time, price paid, a status badge, and the reschedule card (D-01) when it applies.
  - Book Again opens Make a Booking for that expert (the Phase 6 `BookingKey`).
  - Message Expert is hidden until Phase 12. Leave a Review and the Feedback block are hidden until Phase 13.
- **D-14: Two fixes to Upcoming Booking Details.**
  - The status pill shows the real status ("Pending approval" / "Confirmed" / "New time proposed" / "In progress") instead of iOS's hardcoded "Confirmed".
  - The fee uses the Phase 7 Money formatter instead of iOS's whole-number `amount / 100`.
  - Log both as iOS follow-ups.
- **D-15: The payment pill shows for paid bookings only.** It reads "Card hold placed" (held), "Paid" (captured) or "Refunded", with wording consistent with the Phase 7 lifecycle card. Free topics hide it.
- **D-16: The private note is deferred.** The `GET/POST /client/bookings/:id/note` screen and its buttons are not built in Phase 8.

### Claude's Discretion
- Visual layout, spacing and copy beyond what is pinned above. Follow the iOS layouts as stand-ins (the designs are not accessible), using Material 3, Nunito and the dark theme.
- The times format follows the Phase 6 helpers: the device timezone plus an "Expert timezone: X" note.
- Navigation structure: new route keys inside the client Bookings tab back stack, and Payment Methods reused from Phase 7.
- Response models: extend `ClientBookingItem` / `ClientBookingDetail` with `reschedule`, `price` / payment status, `private_note_preview` (ignored) and any other fields the backend returns that these screens need.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### iOS source of truth (`ios-reference/` = `/Users/LeonChen/Documents/ClickMe2026/`)
- `ios-reference/ClickMe2026/Views/Screens/Client Mode/My Bookings/`: `MyBookingsView.swift`, `MyBookingsViewModel.swift` (the rejoin rule `isRejoinableCompleted` / `mapPastIfApplicable`, sorting, quiet reloads), plus the card subviews (`UpcomingBookingCard.swift`, `PastBookingCard.swift`, `MyBookingsSegmentPicker.swift`).
- `ios-reference/ClickMe2026/Views/Screens/Client Mode/Upcoming Booking Details/`: `UpcomingBookingView.swift`, `UpcomingBookingViewModel.swift`, `Subviews/UpcomingBookingStatusRow.swift`.
- `ios-reference/ClickMe2026/Views/Screens/Client Mode/Cancellation/`: `ClientCancellationView.swift`, `ClientCancellationViewModel.swift` (`defaultReasons`), `Subviews/CancellationBookingSummary.swift`.
- `ios-reference/ClickMe2026/Views/Screens/Client Mode/Booking Summary/`: `BookingSummaryView.swift` (reschedule card around lines 169 and 256-380), `BookingSummaryViewModel.swift` (accept/decline and the `PAYMENT_METHOD_REQUIRED` retry around lines 247-290), and its subviews.
- `ios-reference/ClickMe2026/Model/UIModels/UpcomingBooking.swift`, `PastBooking.swift`, `PastBookingStatus.swift`, `Model/Enums/BookingStatus.swift`, `Model/Responses/ClientBookingDetail.swift`, `Model/Responses/Children/ClientBookingItem.swift`.
- `ios-reference/ClickMe2026/Network/ClickMeAPI+Booking.swift`: the client booking endpoints.

### Backend contract (`backend-reference/` = `/Users/LeonChen/Documents/ClickMe2026 Backend/`)
- `backend-reference/src/controllers/clientBooking.controller.js`: the list buckets (around lines 129-132), detail, cancel, accept-reschedule and decline-reschedule.
- `backend-reference/src/schemas/booking.schema.js`: the strict cancel schema (`reason` only, max 500, around line 119) and the reschedule bodies.
- `backend-reference/src/utils/refund.js`: the 24h full-or-nothing refund rule (around line 36) that D-06 mirrors.
- `backend-reference/docs/ios/`: authoritative specs per CLAUDE.md. Check for anything on cancellation, refunds or reschedule.

### Project
- `.planning/REQUIREMENTS.md`: BOOK-05..BOOK-09.
- `.planning/ROADMAP.md`: the Phase 8 success criteria and planning notes.
- `.planning/STATE.md`: the Phase 8 hand-offs (Today's Sessions tap, the `ClientBookingsRepository` / `ClientBookingDetail` base, "View My Bookings" selecting `ClientBookingsRoot`), the cancellation-comment open item (resolved by D-05), and the iOS follow-up lists.
- `.planning/phases/07-payment-methods-paid-booking/07-CONTEXT.md`: Money formatting, Payment Methods reuse, paid-flow error-copy patterns.
- `.planning/phases/06-booking-flow-free-bookings/` (CONTEXT/SUMMARY): `BookingKey`, booking formatting helpers, Today's Sessions D-01/D-02.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `screens/clientmode/mybookings/ClientBookingsRepository.kt`: `list(type, page, limit=20)` and `detail(id)` already exist. Add cancel, accept-reschedule and decline-reschedule.
- `model/responses/ClientBookingsPayload.kt` (`bookings` and `pagination`; the item has id, expert, topic, times, `meetingType`, status) and `ClientBookingDetail.kt`. Both are missing `reschedule`, `price` / payment-status fields and `join_link`. `private_note` is not needed (D-16).
- `network/ApiRequestUrls.kt` / `ClickMeApi.kt`: currently only the list and detail routes. Add the cancel and reschedule-response routes.
- `screens/clientmode/mybookings/TodaysSessions.kt` and the Explore `ExploreTodaysSessionsSection`. This is the card tap to wire (the Phase 8 hand-off), and the source of the disabled Join placeholder pattern (D-02, test tag `todays_session_join`).
- The Phase 7 `PaymentMethodsKey` and `PaymentMethodsRepository` for D-02. The Money formatter for D-06, D-14 and D-15. `PaymentLifecycleCard` wording for D-15.
- The Phase 6 booking formatting helpers (session time, expert timezone note) and `BookingKey` for Book Again.
- The Phase 3 realtime socket (`booking.update` events, raw status strings) for D-10.
- `AuthErrorDialog` / existing error-copy patterns for failure alerts.

### Established Patterns
- MVVM with Compose State, Hilt, coroutines, Retrofit with Moshi using strict-schema request bodies (omit nulls; send `{}` when a body is required).
- Entry-scoped Hilt ViewModels with SavedStateHandle for rotation and process death (Phase 4/6).
- Per-tab back stacks in the client shell (`HomeClientScreen`, `ClientBookingFlowEntries`). Bottom bar hidden on pushed detail screens.
- `FakeClickMeApi` plus Robolectric Compose tests. The gate is `testDevDebugUnitTest ktlintCheck assembleDevDebug` plus `scripts/check-design-rules.sh`.

### Integration Points
- `ClientBookingsRoot` placeholder becomes My Bookings. "View My Bookings" from Request Sent already selects it.
- Explore Today's Sessions card goes to Upcoming Booking Details.
- Upcoming Details → Cancellation → back to My Bookings (Past tab).
- Reschedule card → Payment Methods (Phase 7) → back → Accept again.
- Booking Summary → Book Again → `BookingKey`.

</code_context>

<specifics>
## Specific Ideas

- Fix the iOS reschedule dead end: "Tap to review" must actually lead to Accept/Decline (D-01).
- The refund preview must never promise money the backend won't refund (D-06).
- The user prefers fixing clear iOS bugs on Android and logging them as iOS follow-ups (D-01, D-05, D-06, D-14), while keeping iOS parity where it's harmless (D-09 page 1 only).

</specifics>

<deferred>
## Deferred Ideas

- **Private note screen** (`GET/POST /client/bookings/:id/note`), with its buttons on cards and Summary. Not in BOOK-05..09. Candidate for Phase 15 polish or the backlog (D-16).
- **Paging My Bookings past page 1.** The backend supports `page`/`limit`/`pagination`. Revisit if users accumulate more than 20 bookings per tab (D-09).
- **Backend: save the cancellation `reason`.** Today it is validated but not stored; this is a backend-owner note.
- **Backend: a refund preview endpoint**, so clients don't mirror `refund.js` (D-06). A backend-owner note.
- Message Expert on cards and Summary goes to Phase 12. Leave a Review and the Feedback block go to Phase 13. Join goes to Phase 9. Push deep links to Booking Summary go to Phase 13.

</deferred>

---

*Phase: 08-client-bookings-cancellation*
*Context gathered: 2026-10-23*

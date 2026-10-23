# Phase 8: Client Bookings & Cancellation - Research

**Researched:** 2026-10-23
**Domain:** Android port (Compose, MVVM, Retrofit/Moshi, Nav3 per-tab back stacks) of the iOS client booking-management screens, on the existing REST contract
**Confidence:** HIGH (backend contract read from source; Android patterns read from the repo). Two UI-SPEC/CONTEXT assumptions are contradicted by the backend; see "Critical Findings".

## Summary

Phase 8 adds no new dependencies. It is a contract-and-wiring phase: three new endpoints (cancel, accept-reschedule, decline-reschedule), extensions to two existing response DTOs, one shared repository extension, five screens and a shared reschedule card, plus nav wiring in the client shell. Every piece has a direct analogue in Phase 6 and 7 code (`BookingRequestSentViewModel`, `MakeABookingViewModel` with `SavedStateHandle`, `clientBookingFlowEntries`, `FakeClickMeApi`, `PaidFlowError`).

I read the backend handlers (`clientBooking.controller.js`, `booking.schema.js`, `refund.js`, `reschedule.js`, `longLeadChargeSweep.js`) and found four things the CONTEXT and UI-SPEC did not account for. The first two change what the planner must build.

1. **The rejoin rule needs both server buckets.** The server returns `completed` only in `type=past`. My Bookings must fetch upcoming and past together on every load (as iOS does) to build the Upcoming tab. The UI-SPEC's "the other tab loads when it is first selected" cannot hold.
2. **D-06's refund lines are mostly unreachable on the current backend.** The detail's `payment_status` is a derived value, and every cancellable status (`confirmed`, `pending_approval`, `pending_reschedule`) derives to `held`, never `captured`. Payment is manual-capture and captured only after the session completes, so a pre-session cancel voids the hold. The "Full refund" and "No refund within 24h" lines will never show for real data. The preview should still be built as D-06 says (a pure function that is correct if the backend changes), but tests and device checks must expect the `held` line and "no line".
3. **Error handling is by `code` with specific HTTP statuses.** `PAYMENT_METHOD_REQUIRED` is a 422, `EXPERT_UNAVAILABLE` a 409, and `RESCHEDULE_OUTSIDE_AUTH_WINDOW` and `INVALID_PAYMENT_METHOD` are 422s the UI-SPEC error table does not list. Decline and a repeated accept are idempotent 200s.
4. **The cancel response is `data: {refund_amount (major units, may be a decimal), currency}`**, not a minor-unit integer. The UI-SPEC says so; no Android helper converts it yet.

**Primary recommendation:** Build bottom-up. Wave 0: DTOs, endpoints, repository methods, `FakeClickMeApi` handlers and the pure-logic units (bucketing, refund preview, reason string, error mapping, disclosure rule) with tests. Then the shared components, ViewModels, screens and nav wiring. Add no libraries.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Reschedule response (BOOK-09)**
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

**Cancellation (BOOK-07)**
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

**My Bookings list (BOOK-05)**
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

**Booking Summary and the detail screen (BOOK-06, BOOK-08)**
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

### Deferred Ideas (OUT OF SCOPE)
- **Private note screen** (`GET/POST /client/bookings/:id/note`), with its buttons on cards and Summary. Not in BOOK-05..09. Candidate for Phase 15 polish or the backlog (D-16).
- **Paging My Bookings past page 1.** The backend supports `page`/`limit`/`pagination`. Revisit if users accumulate more than 20 bookings per tab (D-09).
- **Backend: save the cancellation `reason`.** Today it is validated but not stored; this is a backend-owner note.
- **Backend: a refund preview endpoint**, so clients don't mirror `refund.js` (D-06). A backend-owner note.
- Message Expert on cards and Summary goes to Phase 12. Leave a Review and the Feedback block go to Phase 13. Join goes to Phase 9. Push deep links to Booking Summary go to Phase 13.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| BOOK-05 | My Bookings with Upcoming and Past tabs; `completed` with a future end time stays Upcoming; Past labels completed / cancelled / missed | Server buckets and the rejoin rule (Critical Finding 1); bucketing function spec; list DTO; refresh triggers via `RealtimeConnection.events`; sorting |
| BOOK-06 | Upcoming Booking Details with the "Waiting for expert" banner, join card and Cancel; no Reschedule | Detail DTO additions (`join_link`, `can_join`, `payment_status`, `reschedule`, `authorization_expires_at`, `expert_timezone`); Cancel allowed statuses; Explore hand-off wiring |
| BOOK-07 | Cancel with reason picker and refund preview through `cancelClientBooking`; comment decision | Cancel contract (strict `{reason?<=500}`), response shape, derived `payment_status` reality (Critical Finding 2), error table, resolved open item (D-05) |
| BOOK-08 | Minimal Booking Summary | Same detail endpoint; badge table; Book Again via `BookingKey` |
| BOOK-09 | Accept or decline an expert-proposed reschedule | Accept/decline contract, idempotent 200s, `PAYMENT_METHOD_REQUIRED` recovery per the backend doc, error taxonomy, disclosure rule |
</phase_requirements>

## Resolved Open Item: the cancellation comment

**Decision already made by D-05; the backend confirms it is safe.** `cancelSchema` is `z.object({ reason: z.string().max(500).optional() }).strict()` and is used by `POST /client/bookings/:id/cancel`. [VERIFIED: backend-reference/src/schemas/booking.schema.js:116-119, routes/clientBooking.js:33]

- The only accepted key is `reason`. Sending any other key (for example `comment`) returns a 422 from the `.strict()` validator, so merging the comment into `reason` is the only way to send it.
- The controller destructures nothing from the body: `cancel()` never reads `reason`, so it is validated and discarded. Sending the merged string is harmless. [VERIFIED: clientBooking.controller.js:779-963]
- Cap: the whole string must be 500 characters or fewer (Zod `max(500)` counts UTF-16 code units like Kotlin `String.length`). With the 450-character comment cap and the longest label "Found another expert" (20) plus ": " (2), the maximum is 472. Keep the final `take(500)` guard but cut with `capNotes(text, 500)` so a surrogate pair is never split.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Bucketing (rejoin rule), sorting, status labels | Client ViewModel (pure function) | API (supplies raw buckets) | The server cannot express "completed but still rejoinable"; iOS does it client-side |
| Refund preview | Client (pure function on detail) | API (authoritative result on cancel) | No preview endpoint exists (deferred backend note); the server `refund_amount` is the truth |
| Cancel, accept, decline execution | API / Backend | — | Status guards, Stripe void/refund, idempotency live on the server |
| Reschedule card visibility and who may act | Client (UI gate on `initiated_by == expert`) | API (enforces 403) | UI hides buttons; server is the real guard |
| Card recovery for `PAYMENT_METHOD_REQUIRED` | Client ViewModel orchestration | API (`GET /me/payment-methods`, retry with `pm_`) | Server check is DB-only, so the client must drive the recovery |
| Realtime refresh | Client (collect `RealtimeConnection.events`) | API/socket | Phase 3 socket already exists; no screen consumes it yet |
| Time display | Client (device zone, device 12/24h) | — | Phase 6 helpers |
| Navigation after cancel (select Past tab, snackbar) | Client shell | — | Needs one-shot launch args across stacks |

## Standard Stack

No new libraries. Everything is already in `gradle/libs.versions.toml`. [VERIFIED: repo]

### Core (already present)
| Library | Version | Purpose | Notes |
|---------|---------|---------|-------|
| Compose BOM / Material 3 | 2026.09.00 | `PrimaryTabRow`, `PullToRefreshBox`, `SnackbarHost`, `AlertDialog`, `ModalBottomSheet` | `PrimaryTabRow` confirmed in the material3 TabRow sources in the Gradle cache; `PullToRefreshBox` already used in `ExploreScreen` |
| Navigation 3 | 1.2.0 | Per-tab back stacks, `NavKey` keys, `BottomBar.hidden()` metadata | Existing `Navigator` has `navigate`, `replaceTop`, `resetAndSelect`, `navigateInTab` |
| Hilt | 2.60.1 | `@HiltViewModel(assistedFactory = ...)` with a key plus `SavedStateHandle` | Pattern in `MakeABookingViewModel` |
| Retrofit 3.0.0 + Moshi 1.15.2 | — | New endpoints and DTOs (`@JsonClass(generateAdapter = true)`) | `Instant` adapter exists; unknown JSON keys are ignored |
| kotlinx-coroutines-test | 1.11.0 | VM tests with `MainDispatcherRule` | existing |
| JUnit 4.13.2, Robolectric 4.17, Compose ui-test-junit4 | — | Unit and Compose tests | existing |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Two sequential list calls | `async` pair | Parallel is faster; iOS is sequential only for Swift Sendable reasons. Use `coroutineScope { async/async }`; either satisfies the contract |
| A custom tab/segment control | `PrimaryTabRow` | UI-SPEC locks `PrimaryTabRow` |

**Installation:** none. **Package Legitimacy Audit:** not applicable, no external packages are added (slopcheck not run). [VERIFIED: libs.versions.toml has everything needed]

## Backend Contract (authoritative, read from source)

All routes need role `client`, are scoped to `client_id = caller`, and return 404 `NOT_FOUND` for any booking the caller does not own. [VERIFIED: backend-reference/src/routes/clientBooking.js, controller]

### `GET /client/bookings?type=upcoming|past&page=1&limit=20`
- `limit` defaults to 10, max 100. Android passes `limit=20`. 422 `VALIDATION_ERROR` if `type` is missing or unknown.
- **Upcoming bucket** = `pending_approval, confirmed, pending_reschedule, in_progress`. **Past bucket** = `completed, cancelled, declined, missed, expired`. Upcoming is ordered `start_time` ascending, Past descending (server side, per page). [VERIFIED: controller:129-132, 202-218]
- Row shape: `booking_id, expert{id, full_name, avatar_url}, topic (title string or null), start_time, end_time, meeting_type, status, private_note_preview, reschedule`. `reschedule` is `null` unless `status == pending_reschedule`; on lists it is slim: `{proposed_start, initiated_by}` only. [VERIFIED: controller:226-243, 142-150]
- Envelope: `{status, data: {bookings, pagination: {total_count, total_pages, current_page}}}`.

### `GET /client/bookings/:id` (detail)
`data` = `booking_id, status, start_time, end_time, meeting_type, expert_timezone ('UTC' fallback), payment_status, authorization_expires_at, topic{id,title,duration_mins,is_free,price{amount (MINOR units),currency}}, expert{id,full_name,avatar_url,title}, join_link, can_join, client_notes, private_note, reschedule`. `reschedule` here is full: `{proposed_start, proposed_end, proposed_timezone, initiated_by, note, created_at}`. [VERIFIED: controller:1017-1058]

Fields missing from the current Android `ClientBookingDetail`: `authorization_expires_at` (Instant?), `join_link` (String?), `can_join` (Boolean), `reschedule`. (`private_note` is not needed, D-16.) UI-SPEC assumption A6 is resolved: the backend does return the authorization expiry.

`payment_status` is a **derived 4-value display vocabulary**: `none | held | captured | refunded`. [VERIFIED: controller:71-95, docs/ios/CLIENT-TRANSACTIONS.md:103-106]

| Condition | Derived value |
|---|---|
| no `payment_intent_id` (free booking, and also a paid long-lead booking still in `scheduled`, i.e. card saved and not yet charged) | `none` |
| DB payment status `canceled`, `auth_expired`, `failed` | `none` |
| `pending_approval`, `pending_reschedule`, `confirmed`, `in_progress` (with a PI) | `held` |
| `completed` | `captured` |
| `missed` | `refunded` if refund amount > 0, else `captured` |
| `cancelled`, `declined`, `expired` | `refunded` if refund amount > 0, else `none` |

### `POST /client/bookings/:id/cancel`
- Body `{reason?: string<=500}`, strict. [VERIFIED]
- Allowed statuses: `confirmed, pending_approval, pending_reschedule`. Otherwise 409 `INVALID_STATUS`. 404 if not owned.
- A lost concurrent-cancel race returns **200** with `message: "Booking is already cancelled."` and `data: {refund_amount: 0, currency}`. Treat as success (D-07).
- Success: `200 {status, message, data: {refund_amount, currency}}`. `refund_amount` is in **major units** (`minor / 10^digits`), so it can be a decimal such as `12.5`. `currency` is upper case, defaulting to `CAD`. Zero-decimal set on the server: `JPY, KRW, VND, CLP, ISK, XOF, XAF`. [VERIFIED: controller:166-170, 955-962]
- Money behaviour: every paid booking uses `capture_method: manual`, and capture happens only after the session completes (about 15 minutes after the end). A pre-session cancel therefore **voids the hold** and returns `refund_amount: 0`. A refund path (`refund.js`: full amount when start is strictly more than 24h away, else 0) only runs if the PaymentIntent is already `succeeded`. A long-lead booking not yet charged is settled by `cancelScheduledPayment` (nothing to refund). Stripe failures after the DB commit still return 200 (the booking is cancelled; a reconciliation cron fixes the money). [VERIFIED: controller:845-939, docs/ios/STRIPE-INTEGRATION.md:122,188-189, cron/longLeadChargeSweep.js]
- The expert is notified (`BOOKING_CANCELLED` event to `expert_id`), which covers success criterion 3's "iOS expert sees the cancellation". Nothing for Android to do.

### `POST /client/bookings/:id/accept-reschedule`
- Body `{}` or `{stripe_payment_method_id: "pm_..."}` (`startsWith('pm_')`), strict. Send `{}` explicitly (Retrofit would otherwise send no body; same rule as `createPaymentMethodSetupIntent`).
- Flow (in order): 404 not owned; if status is already `confirmed` return **idempotent 200**; any other non-`pending_reschedule` status is 409 `INVALID_STATUS`; if `proposed_by != 'expert'` then 403 `FORBIDDEN`; expert suspended gives 409 `EXPERT_UNAVAILABLE`; original `end_time <= now` gives 422 `VALIDATION_ERROR` ("scheduled time has already passed"); `proposed_start <= now` gives 422 `VALIDATION_ERROR` ("no longer in the future"). Then, only when the DB payment status is `requires_capture`: verify the supplied `pm_` (422 `INVALID_PAYMENT_METHOD` if it is not the caller's), and `assertConversionAllowed` can throw 422 `PAYMENT_METHOD_REQUIRED` or 422 `RESCHEDULE_OUTSIDE_AUTH_WINDOW`. Success: `200 {data: {booking_id, status: 'confirmed'}}`. [VERIFIED: controller:445-655, utils/reschedule.js]
- Free bookings and bookings whose payment is not `requires_capture` never produce `PAYMENT_METHOD_REQUIRED`.
- Booking times move to the proposed slot (duration preserved) and the status becomes `confirmed`.

### `POST /client/bookings/:id/decline-reschedule`
- Body `{reason?: string<=500}`, strict. Idempotent 200 if the booking is no longer `pending_reschedule`. 403 `FORBIDDEN` if `proposed_by != 'expert'`. 409 `INVALID_STATUS` only on a rare lost race. Success restores the previous status (`reschedule_prev_status`, fallback `confirmed`) and clears the proposal; `reason` is stored in `decline_reason_text`. [VERIFIED: controller:664-761]

### Error taxonomy for the reschedule response (map by `code`, not message)
Source: `docs/ios/RESCHEDULE-PAYMENT-CONVERSION.md` §3 and the controller. [VERIFIED]

| HTTP | Code | When | Android handling |
|---|---|---|---|
| 422 | `PAYMENT_METHOD_REQUIRED` | accept: no card on the row/request, or the referenced card cannot be prepared | D-02 recovery. Does NOT mean "no saved cards" (backend doc §3.1). Money-safe: nothing changed |
| 422 | `INVALID_PAYMENT_METHOD` | the supplied `pm_` is not the caller's | Not in UI-SPEC table. Show the server message in "Couldn't update booking"; do not retry |
| 422 | `RESCHEDULE_OUTSIDE_AUTH_WINDOW` | no connected account, direct charging off, proposed start under 24h, or card cannot be prepared | Not in UI-SPEC table. Terminal. Show the server message; reload; suggest cancelling and re-booking is optional copy (Claude's discretion) |
| 422 | `VALIDATION_ERROR` | original or proposed time has passed | UI-SPEC copy "The proposed time has already passed..." then reload |
| 409 | `INVALID_STATUS` | booking is not `pending_reschedule` | "This reschedule is no longer pending." then reload |
| 409 | `EXPERT_UNAVAILABLE` | expert suspended/terminated | UI-SPEC copy then reload |
| 403 | `FORBIDDEN` | proposal was not made by the expert | UI-SPEC copy then reload |
| 404 | `NOT_FOUND` | not owned | pop |

`ErrorCode` already has `PaymentMethodRequired`, `InvalidPaymentMethod`, `RescheduleOutsideAuthWindow`, `ExpertUnavailable`, `InvalidStatus`, `Forbidden`, `ValidationError`, `NotFound`. [VERIFIED: model/enums/ErrorCode.kt]

## Critical Findings (contradict or refine UI-SPEC / CONTEXT)

### Finding 1: The Upcoming tab depends on the Past payload (rejoin rule)
`completed` is a Past-bucket status on the server. The rejoin rule ("a `completed` booking whose `endTime` hasn't passed stays in Upcoming") can only be met by fetching `type=past` as well. iOS fetches both on every load, builds `upcomingRaw = upResponse + pastResponse.filter(isRejoinableCompleted)`, sorts Upcoming by `startTime` ascending, and Past (after dropping rejoinable and unknown statuses) by `startTime` descending. [VERIFIED: MyBookingsViewModel.swift:105-148]

UI-SPEC surface 1 says "the other tab loads when it is first selected" and describes a per-tab first-load skeleton. That cannot be implemented as written. **Prescription:** `MyBookingsViewModel` has one `loadAll(quiet: Boolean)` that fetches both buckets (`list("upcoming")` and `list("past")`, page 1, limit 20) and derives both tab lists from the pair. One combined load state (Loading / Loaded / Failed). The skeleton shows on the first load of the screen, not per tab. If either call fails on a non-quiet load, the whole screen is Failed (iOS behaviour); a quiet load that fails keeps the old lists.

Consequence of D-09 worth recording in STATE: Past is ordered by start descending, so future-dated cancelled bookings (start in the future) sort ahead of a rejoinable completed session. With 20 or more such rows the rejoinable one falls off page 1. This is the accepted D-09 limit.

Edge: unknown statuses in either bucket are dropped. A `completed` row with a future `end_time` appears in Upcoming with a Join placeholder and no reschedule badge. Upcoming cards for `completed` have no banner (it is not `pending_approval`). `Cancel` is not offered for `completed` or `in_progress` on the details screen.

### Finding 2: D-06's refund lines versus what the backend can actually return
D-06 (locked) keys the preview on `payment_status` being `captured` or `held`. From the derivation table above:

- For the three cancellable statuses with a payment intent, the derived value is always **`held`**. `captured` is only derived for `completed`, which cannot be cancelled. So "Full refund: {amount}" and "No refund — cancelling within 24 hours" can never render for real data on the current backend.
- A paid booking in the long-lead `scheduled` state (card saved, charge at T-24h) has no PaymentIntent, so it derives to **`none`**; D-06 and UI-SPEC then render no refund line. Cancelling it is free (nothing was ever charged).
- The `held` line, "The hold on your card will be released", is correct for every real paid cancel (cancelPI voids the hold). The server `refund_amount` will be `0`, so the D-07 snackbar will read "Booking cancelled" for paid bookings in practice.

**Prescription (stays within D-06):** implement `RefundPreview.from(detail, now)` exactly as the D-06 / UI-SPEC table, as a pure function with the strict `> 24h` boundary taken from `refund.js` (exactly 24h gives no refund). Unit test all five rows, including `captured` ones, because they are correct if the backend ever captures earlier. Do not add other claims. Plan the device checks (deferred anyway) around `held`, free and long-lead. Log in the Open Questions that the "Full refund / No refund" copy is dormant, and add it to the iOS and backend follow-up list (iOS promises a full refund, which the backend never issues pre-session).

A second, smaller risk: the preview's `held` wording is only wrong if the server's PI is already `succeeded` while the booking is `confirmed`. Per `quick 261016-r9s` the backend captures only for completed sessions and sole client no-shows, so this does not happen today. [CITED: docs/ios/STRIPE-INTEGRATION.md:122]

### Finding 3: Cancel and accept responses need their own DTOs
- `CancelBookingData(refund_amount: Double, currency: String)` inside `SuccessDataResponse`. The envelope also has `message`; Moshi ignores it. Use `Double` (the value can be integral or decimal; Moshi reads both into `Double`).
- Convert to minor units for `Money.format(Long, String)`: `BigDecimal.valueOf(refund).movePointRight(Currency.getInstance(code).defaultFractionDigits).setScale(0, RoundingMode.HALF_UP).toLong()`. Java's fraction digits for `JPY, KRW, VND, CLP, ISK, XOF, XAF` are 0, matching the server's zero-decimal set, so the two agree. [ASSUMED: JDK/Android ICU fraction digits for XOF/XAF/ISK match; verify in a unit test]. Add this as `Money.formatMajor(major: Double, currency)` next to `Money.format`; do not duplicate the logic in a ViewModel. Fall back to 2 digits for an unknown currency code, as `Money.format` does.
- `AcceptRescheduleData(booking_id, status)` and `DeclineRescheduleData(booking_id, status)`; the VM only needs success/failure and then reloads, so a shared `RescheduleResponseData` is fine.

### Finding 4: Request bodies
Use small `@JsonClass` data classes and let Moshi omit nulls (the established "strict body" convention):
- `CancelBookingRequest(reason: String?)`. The reason is always non-null from the app (a label always exists).
- `AcceptRescheduleRequest(@Json(name = "stripe_payment_method_id") val stripePaymentMethodId: String? = null)`. With the null default it serializes to `{}`; add a test asserting the wire body is exactly `{}` (MockWebServer, in `ClickMeApiTest`).
- `DeclineRescheduleRequest(reason: String? = null)`. Trim; blank becomes null (omitted).
- Add URL constants to `ApiRequestUrls`: `CLIENT_BOOKING_CANCEL = "client/bookings/{id}/cancel"`, `CLIENT_BOOKING_ACCEPT_RESCHEDULE = "client/bookings/{id}/accept-reschedule"`, `CLIENT_BOOKING_DECLINE_RESCHEDULE = "client/bookings/{id}/decline-reschedule"` and a case in `ApiRequestUrlsTest`.

### Finding 5: Realtime has no screen consumer yet
`RealtimeConnection` is a Hilt-bound `@Singleton` (`RealtimeModule`) exposing `events: SharedFlow<RealtimeEvent>` (replay 0, buffer 64, drop-oldest). `RealtimeEvent.BookingUpdate(bookingId, status, ...)` exists and decodes `booking.update`. No ViewModel collects it. [VERIFIED: realtime/RealtimeService.kt, RealtimeEvents.kt:81, grep]
- Inject `RealtimeConnection` into `MyBookingsViewModel` and `UpcomingBookingViewModel` and collect `events.filterIsInstance<RealtimeEvent.BookingUpdate>()` in `viewModelScope`. Upcoming details filters on its own `bookingId` (UI-SPEC A7); My Bookings reloads on any.
- Because events can arrive in bursts (cancel, complete), debounce or coalesce: cancel an in-flight quiet reload before starting another (`reloadJob?.cancel()`), the pattern `ExploreViewModel` already uses.
- `FakeRealtimeConnection` has no way to emit an event (its `_events` is private). **Wave 0 gap:** add `fun emit(event: RealtimeEvent)` using `tryEmit` (same style as `emitFailure`).

### Finding 6: Resume-triggered reloads
Nav3 may not deliver `ON_RESUME` to an entry that was covered, and then re-enters composition when popped (Phase 6 RESEARCH A10; Phase 7 plan 07-12 observed both `LaunchedEffect(Unit)` and `ON_RESUME` firing on return from Payment Methods). `MakeABookingScreen` uses `LifecycleEventEffect(ON_RESUME)` plus `LaunchedEffect(Unit)` with a de-duplicating guard in the ViewModel (`refreshPaymentMethods` ignores a call while a load started since the screen was last hidden is running). Reuse that pattern for My Bookings and Upcoming details: both effects call one idempotent `refreshQuietly()` which coalesces concurrent calls. The first composition must not double-load (the initial `init { load() }` is the first load; the effects only call the quiet reload when state is already Loaded or when a prior load finished).

## Architecture Patterns

### System Architecture Diagram

```
Explore tab                      Bookings tab (ClientBookingsRoot)
 Today's Session card ──tap──┐    MyBookingsScreen
                             │     ├ PrimaryTabRow (Upcoming | Past)       ┌── RealtimeConnection.events
                             │     ├ PullToRefreshBox ◄─── pull / resume / │   (booking.update) ──► quiet reload
                             │     └ list cards                            │
                             │         │ Upcoming tap        │ Past tap     │
                             │         ▼                     ▼              │
                             └─► UpcomingBookingKey(id)   BookingSummaryKey(id)
                                  UpcomingBookingScreen      BookingSummaryScreen
                                  ├ RescheduleResponseCard ◄── shared ──► RescheduleResponseCard
                                  │    Accept ─► accept-reschedule {} ──422 PAYMENT_METHOD_REQUIRED──┐
                                  │      ▲                                                            ▼
                                  │      │  retry with pm_ ◄── GET /me/payment-methods (default or first)
                                  │      │                      none ─► dialog ─► PaymentMethodsKey ─► Back ─► resume reload
                                  │    Decline ─► decline-reschedule {reason?}
                                  ├ Join card (disabled placeholder)
                                  └ Cancel Booking ─► CancelBookingKey(id)
                                                        ClientCancellationScreen
                                                        step 1: reason + comment ──Continue──► step 2: RefundPreview(detail, now)
                                                                                             └─► POST cancel {reason="label: comment"}
                                                                                                    │ 200 (also idempotent "already cancelled")
                                                                                                    ▼
                                           resetAndSelect(ClientBookingsRoot, {Explore, Bookings}) + one-shot {selectPast, snackbar}
                                                                                                    ▼
                                                                                  MyBookings (Past tab, refreshed, snackbar)
 Booking Summary ─ Book Again ─► BookingKey(expert seed) (Phase 6)

All screens ─► ClientBookingsRepository ─► ClickMeApi (Retrofit/Moshi) ─► backend /client/bookings*
```

### Recommended Project Structure (mirrors the iOS tree, per CLAUDE.md)
```
app/src/main/java/com/uptrendinvestments/clickmeapp/
├── model/responses/ClientBookingsPayload.kt     # extend: ClientBookingItem.reschedule (slim), add RescheduleDto
├── model/responses/ClientBookingDetail.kt       # extend: reschedule, authorizationExpiresAt, joinLink, canJoin
├── model/responses/BookingActionResponses.kt    # CancelBookingData, RescheduleResponseData  (new)
├── model/requests/BookingActionRequests.kt      # Cancel/Accept/Decline bodies  (new)
├── network/ApiRequestUrls.kt, ClickMeApi.kt     # 3 endpoints
├── util/Money.kt                                # + formatMajor
├── components/BookingStatusBadge.kt, RescheduleResponseCard.kt
├── screens/clientmode/mybookings/               # repository (+3 methods), MyBookingsScreen/ViewModel/UiState, BookingBuckets.kt, subviews/
├── screens/clientmode/upcomingbooking/          # UpcomingBookingKey/Screen/ViewModel, subviews/
├── screens/clientmode/cancellation/             # CancelBookingKey/Screen/ViewModel, RefundPreview.kt, CancellationReason.kt, subviews/
├── screens/clientmode/bookingsummary/           # BookingSummaryKey/Screen/ViewModel
├── screens/clientmode/home/                     # HomeClientScreen (bookingsRoot slot), new clientBookingManagementEntries(...)
└── res/values/strings_my_bookings.xml, strings_upcoming_booking.xml, strings_reschedule_response.xml, strings_cancel_booking.xml, strings_booking_summary.xml
```
The five string files and the component list come from the UI-SPEC; they are the contract.

### Pattern 1: Pure-logic units, tested in isolation (do these first)
Keep decisions out of composables and out of ViewModels' `viewModelScope` code so JVM tests cover them without Robolectric:
- `BookingBuckets.kt`: `fun buildLists(upcoming, past, now): Pair<List<UpcomingRow>, List<PastRow>>` implementing the rejoin rule, status to label mapping, drop-unknown and the two sort orders. Inputs are the raw `ClientBookingItem`s and an `Instant`.
- `RefundPreview.kt`: `fun refundPreview(detail, now): RefundPreview` (sealed: `Full(minor,currency)`, `NoRefundWithin24h`, `HoldReleased`, `PolicyFallback`, `None`).
- `CancellationReason.kt`: the five labels and `fun cancelReason(label, comment): String` (`"$label: $comment"` when the trimmed comment is non-blank, else the label; `capNotes(.., 500)`).
- `RescheduleErrors.kt`: `fun ApiException.toRescheduleFailure(): RescheduleFailure` and `toCancelFailure()` (same shape as `toBookingFailure()` / `PaidFlowError`).
- `rescheduleDisclosure(detail, now)`: `showsPaymentDisclosure = reschedule.proposedEnd > authorizationExpiresAt` and `chargesSoon = proposedStart < now + 48h`, with `now` from the injected `Clock`.

### Pattern 2: ViewModels
- Entry-scoped `@HiltViewModel(assistedFactory = X.Factory::class)` with `@Assisted key: XKey` plus `SavedStateHandle` (as `MakeABookingViewModel`). `CancelBookingViewModel` persists `step`, `selectedReasonIndex` and `comment` in `SavedStateHandle`; `MyBookingsViewModel` persists the selected tab (UI-SPEC says `rememberSaveable`; either is fine).
- Expose `StateFlow<UiState>` with `collectAsStateWithLifecycle()`; one-shot UI events (snackbar text, navigate) go through a `consume...()` field in state, the existing `completedBookingId` / `consumeCompletedBooking()` idiom.
- In-flight guards: a boolean `isSubmitting` in state; the method returns immediately if it is already true (iOS `isSubmittingReschedule`). Results of accept or decline that complete after the user left are dropped by `viewModelScope` cancellation only if the entry is popped; UI-SPEC says the call finishes in the ViewModel, which is the default.
- All ApiException handling through the pure mappers; `ErrorCode.AccountSuspended` is `Silent` (global dialog owns it, Phase 2 D-08). Server text only ever shown as plain `FieldError.Text` (T-06-13).

### Pattern 3: Shell wiring (Hilt-free, testable)
- Add a `bookingsRoot: @Composable (Navigator) -> Unit` slot to `HomeClientShell` alongside `exploreRoot`, defaulting to the placeholder, and replace `entry<ClientBookingsRoot> { ClientTabRoot(...) }`. Pass `MyBookingsScreen` from `HomeClientScreen`.
- Add `clientBookingManagementEntries(navigator, upcoming, cancel, summary)` modelled on `clientBookingFlowEntries(...)`: slots receive `(key, callbacks)` and `HomeClientScreen` supplies the Hilt ViewModels. Register `UpcomingBookingKey`, `CancelBookingKey`, `BookingSummaryKey` with `metadata = BottomBar.hidden()`. Add the three keys as `@Serializable data class X(val bookingId: String) : NavKey`.
- `ExploreScreen` / `ExploreTodaysSessionsSection` get an `onSessionClick: (bookingId: String) -> Unit`; `HomeClientScreen` passes `{ navigator.navigate(UpcomingBookingKey(it)) }`. `TodaysSessionUi.bookingId` already exists. The existing `ExploreSectionsTest` must be updated for the new parameter and the click target; keep `TODAYS_SESSION_JOIN_TAG` and the disabled state unchanged.
- Navigation callbacks: Upcoming Cancel → `navigator.navigate(CancelBookingKey(id))`; Add a Payment Method → `navigator.navigate(PaymentMethodsKey)`; Past card → `BookingSummaryKey`; Book Again → `navigator.navigate(BookingKey(...))` from the seed in the detail (`expert.id/full_name/title/avatar_url`); hidden when `expert.id` is null.
- **After cancel (D-07).** `navigator.resetAndSelect(ClientBookingsRoot, setOf(ExploreRoot, ClientBookingsRoot))`, which also covers cancel started from the Explore stack. The "select Past and show snackbar" request must outlive the Cancel ViewModel and be readable by a possibly not-yet-created `MyBookingsViewModel`. Use a small `@Singleton` holder (for example `BookingsLaunchRequest` with a `MutableStateFlow<Request?>` and `consume()`), injected into `MyBookingsViewModel` and posted from the shell callback. A nav result cannot be used because both origins must behave the same (UI-SPEC). The holder must be consumed once (consume-on-read), and the VM must read it on both creation and when the tab re-enters.
- Add `ClientBookingsRoot` selection note: `HomeClientShell` shows only the start stack plus the selected tab (`stacksInUse`), so the Bookings entry leaves composition when another tab is selected. Do not rely on the ViewModel surviving a tab switch; the quiet reload on re-show (D-10) and the saved tab selection make this safe either way. [ASSUMED: ViewModel retention across tab switches; the design does not depend on it]

### Anti-Patterns to Avoid
- **Fetching only the active tab's bucket** (breaks the rejoin rule, Finding 1).
- **Optimistic UI for accept/decline/cancel.** UI-SPEC: nothing flips until the server answers and the booking reloads.
- **Treating `PAYMENT_METHOD_REQUIRED` as "no cards".** Always call `GET /me/payment-methods` first (backend doc §3.1).
- **Gating anything on `authorization_expires_at` beyond the disclosure.** The backend doc withdrew that guidance; it is informational only.
- **Using `price.is_free`.** Use the topic-level `topic.is_free` (existing DTO comment). `topic.price.amount` is minor units, unlike `refund_amount`.
- **Hard-coded colours, `fontSize`, `Brand` types.** `scripts/check-design-rules.sh` fails the gate.
- **Parsing server `message` for logic.** Branch on `ErrorCode` / HTTP status only.
- **Showing a raw IANA zone id.** Use `zoneCity`/`zoneAbbreviation` (Phase 6 D-05).

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Currency formatting | A new formatter | `Money.format` (+ one new `formatMajor` for the cancel response) | Fraction digits, locale, JPY (Phase 5/7 rules) |
| Notes length cap | `take(n)` | `capNotes(text, max)` in `BookingFormatting.kt` | Does not split surrogate pairs |
| Times | `SimpleDateFormat` or fixed `h:mm a` | `formatShortDate`, `formatSessionTimeRange`, `rememberDeviceTimeFormat`, `formatSlotTime`, `DateFormat.getBestDateTimePattern` | Device zone and 12/24h (iOS fixed `en_US` is a known bug) |
| Expert timezone note | Raw IANA display | `zoneCity`, `zoneAbbreviation`, `isSameZone` in `BookingTimezoneNote.kt` | Phase 6 rule |
| Pull to refresh, tabs, snackbars, dialogs | Custom gestures/tabs | `PullToRefreshBox`, `PrimaryTabRow`, `SnackbarHost`, `AlertDialog`/`AuthErrorDialog` | UI-SPEC pins them; already in use |
| Error dialogs | New dialog | `AuthErrorDialog` | Pinned |
| Loading/empty/error | Custom views | `EmptyState`, `SkeletonGroup`/`SkeletonBlock` | Pinned |
| Saved cards | Direct API in the VM | `PaymentMethodsRepository.list()` | Existing Singleton |
| Idempotency of double taps | Local locks beyond the `isSubmitting` flag | Server idempotent 200s plus the flag | Backend already handles replays |
| Cross-tab navigation | Manual stack edits | `Navigator.resetAndSelect` / `navigate` | Existing, tested |

**Key insight:** every hard part (money, time, zones, error mapping, nav) already has a tested Phase 5-7 helper. The risk in this phase is wiring and contract details, not new technology.

## Common Pitfalls

### Pitfall 1: Per-tab loading hides the rejoin rule
**What goes wrong:** Upcoming shows no rejoinable completed session, or flickers when Past loads later. **Why:** `completed` is in the Past bucket. **Avoid:** one `loadAll` for both buckets (Finding 1). **Warning sign:** a VM with `loadUpcoming()` and `loadPast()` that run independently.

### Pitfall 2: Dormant refund copy mistaken for working copy
**What goes wrong:** Tests pass on fabricated `captured` fixtures and the team believes "Full refund" shows in the app. **Avoid:** document in the plan that real data yields `held` or no line (Finding 2); include a `held` fixture in the Cancel screen test and a `none` fixture for the long-lead paid case.

### Pitfall 3: Cancel response treated as minor units
**What goes wrong:** JPY 1000 refund shown as "¥10" or CAD 12.5 shown as "$0.13". **Avoid:** `Money.formatMajor` and tests with `0`, `12.5`, `75`, and JPY `1000`.

### Pitfall 4: Strict bodies
**What goes wrong:** 422 on every cancel/accept/decline. **Why:** `.strict()` schemas reject unknown keys, and a null-serialized `reason` is not a string. **Avoid:** null defaults and omitted nulls; assert exact wire JSON in `ClickMeApiTest` (`{}` for accept without a card, `{"stripe_payment_method_id":"pm_x"}`, `{"reason":"Change of plans"}`).

### Pitfall 5: Slim vs full `reschedule`
**What goes wrong:** Decoder crashes or misses data. **Why:** the list `reschedule` has only `proposed_start` and `initiated_by`; the detail has six fields. **Avoid:** one `RescheduleDto` with `proposedStart` required and the rest nullable. Treat any `initiated_by` other than `"expert"` (including null) as not actionable (D-03; the server 403s it anyway).

### Pitfall 6: Accept retry loops
**What goes wrong:** Repeat `PAYMENT_METHOD_REQUIRED` re-triggers recovery forever. **Avoid:** exactly one retry with the default (or first) card; a second `PAYMENT_METHOD_REQUIRED` goes to the "card can't be used" dialog (iOS behaviour). A failure of `GET /me/payment-methods` gives "Couldn't check your saved payment methods." and no retry.

### Pitfall 7: Back during in-flight cancel
**What goes wrong:** User leaves, the cancel completes, and the result navigation fires on a dead screen. **Avoid:** `BackHandler(enabled = isSubmitting)` swallowing the event and `AuthTopAppBar(backEnabled = !isSubmitting)`. Step 2 `BackHandler` returns to step 1 (enabled only on step 2). Order the two handlers so the in-flight one wins.

### Pitfall 8: Double reload on return from Payment Methods
**What goes wrong:** Two overlapping detail loads flicker the reschedule card. **Avoid:** the coalescing guard from Finding 6.

### Pitfall 9: Quiet reload replaced by skeleton
**What goes wrong:** Realtime or resume reload blanks the list. **Avoid:** `loadAll(quiet = true)` never emits Loading and never replaces a Loaded state with Failed (UI-SPEC: failed quiet reload shows nothing; failed pull-to-refresh shows a snackbar).

### Pitfall 10: Same booking key in two stacks
`UpcomingBookingKey(id)` can exist in both the Explore stack and the Bookings stack. The bottom bar is hidden on these routes so the user cannot switch tabs while on them, and the post-cancel `resetAndSelect` clears both stacks, so a duplicate content key in one `NavDisplay` is not reachable today. Add a test (`resetAndSelect` clears both) rather than extra code. Low risk. [ASSUMED: Nav3 behaviour with duplicate content keys; not reachable]

### Pitfall 11: Strictly greater than 24h
`refund.js` returns a refund only when `hoursUntilStart > 24`; exactly 24h is no refund. Use `Duration.between(now, start) > Duration.ofHours(24)` with the injected `Clock`.

### Pitfall 12: Today's Sessions includes `pending_reschedule`
`JOINABLE_BOOKING_STATUSES` contains `pending_reschedule`, so tapping such a card lands on Upcoming details showing the reschedule card above a Join placeholder. This is intended (UI-SPEC says Join shows for `pending_reschedule`). Make sure the details screen still loads its own data and does not rely on the list row.

## Code Examples

### Rejoin rule and bucketing (port of iOS `mapPastIfApplicable` / `isRejoinableCompleted`)
```kotlin
// Source: ios-reference MyBookingsViewModel.swift:105-190 (ported), CONTEXT D-10..D-12
private val PAST_LABELS = mapOf(
    "completed" to PastLabel.Completed, "cancelled" to PastLabel.Cancelled, "declined" to PastLabel.Cancelled,
    "missed" to PastLabel.Missed, "expired" to PastLabel.Missed,
)

fun ClientBookingItem.isRejoinableCompleted(now: Instant) = status == "completed" && endTime.isAfter(now)

fun buildBookingLists(upcoming: List<ClientBookingItem>, past: List<ClientBookingItem>, now: Instant): BookingLists {
    val rejoinable = past.filter { it.isRejoinableCompleted(now) }
    return BookingLists(
        upcoming = (upcoming + rejoinable).sortedBy { it.startTime },            // oldest first
        past = past.sortedByDescending { it.startTime }                          // newest first
            .mapNotNull { item ->
                if (item.isRejoinableCompleted(now)) null
                else PAST_LABELS[item.status]?.let { PastRow(item, it) }          // unknown dropped
            },
    )
}
```

### Cancel reason string (D-05)
```kotlin
// Source: CONTEXT D-05, backend cancelSchema max(500)
fun cancelReason(label: String, comment: String): String {
    val c = comment.trim()
    val text = if (c.isEmpty()) label else "$label: $c"
    return capNotes(text, 500)
}
```

### Refund preview (D-06; `refund.js` boundary)
```kotlin
// Source: backend-reference/src/utils/refund.js:36 (strictly > 24h)
fun refundPreview(d: ClientBookingDetail, now: Instant): RefundPreview {
    val price = d.topic.price
    if (d.topic.isFree) return RefundPreview.None
    return when (d.paymentStatus) {
        "held" -> RefundPreview.HoldReleased
        "captured" -> {
            val amount = price?.amount; val cur = price?.currency
            if (amount == null || cur == null) RefundPreview.PolicyFallback
            else if (Duration.between(now, d.startTime) > Duration.ofHours(24)) RefundPreview.Full(amount, cur)
            else RefundPreview.NoRefundWithin24h
        }
        else -> RefundPreview.None
    }
}
```

### Accept with one-shot card recovery (D-02; iOS `acceptReschedule`)
```kotlin
// Source: BookingSummaryViewModel.swift:247-290 and docs/ios/RESCHEDULE-PAYMENT-CONVERSION.md §3.1
suspend fun accept(id: String): AcceptOutcome {
    try { bookings.acceptReschedule(id, null); return AcceptOutcome.Done }
    catch (e: ApiException.Http) { if (e.code != ErrorCode.PaymentMethodRequired) return AcceptOutcome.Failed(e) }
    val cards = try { paymentMethods.list() } catch (e: ApiException) { return AcceptOutcome.CardsUnavailable }
    val pm = (cards.firstOrNull { it.isDefault } ?: cards.firstOrNull())?.id ?: return AcceptOutcome.NeedsPaymentMethod
    return try { bookings.acceptReschedule(id, pm); AcceptOutcome.Done }
    catch (e: ApiException.Http) {
        if (e.code == ErrorCode.PaymentMethodRequired) AcceptOutcome.CardUnusable else AcceptOutcome.Failed(e)
    }
}
```
(Never log the `pm_` id; it is not a secret but is account data.)

### Retrofit declarations
```kotlin
@POST(ApiRequestUrls.CLIENT_BOOKING_CANCEL)
suspend fun cancelClientBooking(@Path("id") id: String, @Body body: CancelBookingRequest): SuccessDataResponse<CancelBookingData>

@POST(ApiRequestUrls.CLIENT_BOOKING_ACCEPT_RESCHEDULE)
suspend fun acceptClientReschedule(@Path("id") id: String, @Body body: AcceptRescheduleRequest): SuccessDataResponse<RescheduleResponseData>

@POST(ApiRequestUrls.CLIENT_BOOKING_DECLINE_RESCHEDULE)
suspend fun declineClientReschedule(@Path("id") id: String, @Body body: DeclineRescheduleRequest): SuccessDataResponse<RescheduleResponseData>
```
`FakeClickMeApi` must implement the three (it implements the interface) with `var ...Handler` and `...Calls` counters, following the existing style.

## State of the Art

| Old / iOS approach | Android approach | Impact |
|---|---|---|
| Accept/Decline only on Booking Summary (unreachable) | Shared card also on Upcoming details | D-01; iOS follow-up |
| Comment dropped | `reason = "label: comment"` | D-05; iOS follow-up |
| Always promises a full refund | Rule-based preview (mostly the `held` line in practice) | D-06; iOS and backend follow-ups |
| Hardcoded "Confirmed" pill and `amount/100` | Real status, `Money.format` | D-14 |
| `authorization_expires_at` used to gate reschedule slots | Informational only (disclosure) | Backend doc withdrew gating (Phase 27.1) |

**Deprecated/outdated in this phase's inputs:** CONTEXT line 76 and UI-SPEC refund rows describe `captured` paid cancellations, which the backend's manual-capture-after-session model does not produce for cancellable bookings (Finding 2).

## Runtime State Inventory

Not a rename/refactor/migration phase. Omitted.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | JDK/Android fraction digits for `JPY, KRW, VND, CLP, ISK, XOF, XAF` are all 0, matching the backend zero-decimal set | Finding 3 | `Money.formatMajor` would mis-scale a refund snackbar for that currency. Mitigate with the unit test |
| A2 | The `MyBookingsViewModel` may not survive tab switches (Bookings entry leaves composition when another tab is selected) | Pattern 3 | If it does survive, nothing breaks; if it does not, the first-load skeleton re-shows on re-entry unless state is cached. Design uses a quiet reload, so low risk |
| A3 | Nav3 tolerates the same `NavKey` value in two stacks | Pitfall 10 | Not reachable today (bottom bar hidden, post-cancel reset) |
| A4 | I did not read the iOS Upcoming/Cancellation/Booking Summary view files line by line; the layouts, copy and ordering come from CONTEXT, UI-SPEC and the booking view models/APIs I read | Whole phase | Layout detail drift. UI-SPEC is the contract and was approved against the iOS files |
| A5 | `SuccessDataResponse` decodes the idempotent-cancel 200 (it has an extra `message`) | Finding 3 | Moshi ignores unknown keys by default in this project, but confirm in the DTO test |

## Open Questions (RESOLVED)

1. **Dormant refund copy (Finding 2).**
   - Known: for every cancellable status the derived `payment_status` is `held` or `none`; the backend voids holds on a pre-session cancel; `refund_amount` will be 0.
   - Unclear: whether the user wants D-06's "Full refund / No refund within 24h" kept as dormant defensive code (recommended: yes, it is small and tested) or dropped.
   - Recommendation: keep it as specified, test it, and log the iOS note: "iOS promises a full refund; the backend voids the hold instead".
   - RESOLVED: kept as dormant defensive code, built and tested (plan 08-03).
2. **Paid long-lead booking in `scheduled` state** (derived `none`) has no payment pill and no refund line. D-15 and D-06 imply that. Optionally add "You haven't been charged" copy later; not required.
   - RESOLVED: deferred. No extra copy this phase.
3. **`RESCHEDULE_OUTSIDE_AUTH_WINDOW` copy.** Not in the UI-SPEC error table. Recommendation: show the server message in "Couldn't update booking", reload, and stay (Claude's discretion area).
   - RESOLVED: server message in "Couldn't update booking", then reload (plan 08-04).
4. **Where the one-shot "select Past + snackbar" lives.** Recommendation: a `@Singleton` holder. A shell-level `rememberSaveable` also works but loses the value if the shell recomposes before My Bookings reads it. The planner picks; the requirement is that Explore-stack and Bookings-stack cancels behave identically.
   - RESOLVED: `@Singleton` `BookingsLaunchRequest` holder (plan 08-02).
5. **Manual checks deferred** (project preference, Phase 2 rule): all device checks, and anything touching a card, is blocked until a test-mode Stripe backend exists (STATE "[Phase 7] Stripe key and mode"). Reschedule accept/decline needs the iOS expert to propose one.
   - RESOLVED: deferred, logged as open items in STATE (plan 08-15).

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| JDK 25 (Android Studio JBR) | Gradle/AGP 9.4 | yes | `/Applications/Android Studio.app/Contents/jbr/Contents/Home` | none needed (system `java` is 18, do not use) |
| Gradle wrapper | build and tests | yes | repo wrapper | — |
| Samsung SM-A075F (API 36), `-s R8YYC1QFA6H` | Device checks | yes (per memory) | — | Checks are deferred by project preference |
| Test-mode Stripe backend | Card-based device checks (accept recovery, paid cancel) | no | — | Automated tests with `FakeClickMeApi`; device checks stay open (STATE) |
| iOS expert account | Propose reschedule / see cancellation | external | — | Deferred device UAT |
| Network access to backend | none for automated tests | n/a | — | Tests are offline (Fake API, MockWebServer) |

No missing dependency blocks planning or automated execution.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 4.13.2 + Robolectric 4.17 + kotlinx-coroutines-test 1.11.0 + Compose `ui-test-junit4` + okhttp-mockwebserver3 |
| Config file | none; `app/build.gradle.kts` has `testOptions.unitTests.isIncludeAndroidResources = true`. Fakes live in `app/src/test/.../testing/` |
| Quick run command | `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDevDebugUnitTest --tests '<pattern>'` |
| Full suite command | `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDevDebugUnitTest ktlintCheck assembleDevDebug && bash scripts/check-design-rules.sh` (the Phase gate; add `lintDevDebug lintProdDebug testProdDebugUnitTest assembleProdRelease` for the final wave as Phase 7 did) |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| BOOK-05 | Rejoin rule: `completed` with future end moves to Upcoming; past ended `completed` stays Past; status to label map; unknown dropped; Upcoming asc, Past desc | unit (pure) | `--tests '*BookingBucketsTest'` | ❌ Wave 0 |
| BOOK-05 | VM loads both buckets (2 calls) on first load, quiet reload keeps list on failure, failed first load is Failed, pull-to-refresh, `booking.update` event triggers one reload | unit (VM + FakeClickMeApi + FakeRealtimeConnection + MutableClock) | `--tests '*MyBookingsViewModelTest'` | ❌ Wave 0 |
| BOOK-05 | Screen: tabs, skeleton, empty (both), error with Retry, card click targets, disabled Join tag `my_bookings_join` | Compose (Robolectric) | `--tests '*MyBookingsScreenTest'` | ❌ Wave 0 |
| BOOK-05 | Today's Sessions card is one click target to `UpcomingBookingKey`; Join tag and disabled state unchanged | Compose | `--tests '*ExploreSectionsTest'` | ✅ extend |
| BOOK-06 | Detail DTO decode (full `reschedule`, `authorization_expires_at`, `join_link`, `can_join`, payment status) | unit | `--tests '*ResponseDecodingTest'` | ✅ extend |
| BOOK-06 | Pending banner vs join card; Cancel shown only for `confirmed/pending_approval/pending_reschedule`; no Reschedule; real status pill; payment pill only for paid; fee via `Money` or "Free" | Compose | `--tests '*UpcomingBookingScreenTest'` | ❌ Wave 0 |
| BOOK-06 | VM: quiet reload on resume and on matching `booking.update` only | unit | `--tests '*UpcomingBookingViewModelTest'` | ❌ Wave 0 |
| BOOK-07 | `cancelReason` merge, trim, 500 cap, 450-char comment worst case | unit (pure) | `--tests '*CancellationReasonTest'` | ❌ Wave 0 |
| BOOK-07 | `refundPreview`: all D-06 rows, strict `> 24h` boundary (exactly 24h is no refund), held, free, unknown price fallback | unit (pure) | `--tests '*RefundPreviewTest'` | ❌ Wave 0 |
| BOOK-07 | `Money.formatMajor`: `0`, `12.5`, `75`, JPY `1000`, unknown currency | unit | `--tests '*MoneyTest'` | ✅ extend |
| BOOK-07 | VM: steps, SavedStateHandle restore, in-flight lock, success (incl. idempotent 200) posts launch request + snackbar text, 404/409 pop, 500 stays on step 2 | unit | `--tests '*ClientCancellationViewModelTest'` | ❌ Wave 0 |
| BOOK-07 | Screen: 5 reasons with default selection, comment counter 450, step 2 Back returns to step 1, buttons, spinner while in flight | Compose | `--tests '*ClientCancellationScreenTest'` | ❌ Wave 0 |
| BOOK-07 | Wire bodies: `{"reason":"..."}` strict; path and verb | MockWebServer | `--tests '*ClickMeApiTest' --tests '*ApiRequestUrlsTest'` | ✅ extend |
| BOOK-08 | Summary: badge table, price label rules (Price paid / Session fee / Refunded), Book Again seed and hidden when no expert id, no Message/Review/Feedback | Compose | `--tests '*BookingSummaryScreenTest'` | ❌ Wave 0 |
| BOOK-09 | Accept: `{}` body; `PAYMENT_METHOD_REQUIRED` then card list then retry with default else first; none gives payment-method dialog; second `PAYMENT_METHOD_REQUIRED` gives "card can't be used"; cards-load failure gives its dialog; one retry only | unit (VM or delegate) | `--tests '*RescheduleResponseTest'` | ❌ Wave 0 |
| BOOK-09 | Error mapping table: 403, 409 `INVALID_STATUS`, 409 `EXPERT_UNAVAILABLE`, 422 `VALIDATION_ERROR`, 422 `RESCHEDULE_OUTSIDE_AUTH_WINDOW`, 422 `INVALID_PAYMENT_METHOD`, 404, network, suspended is silent | unit (pure) | `--tests '*RescheduleErrorsTest'` | ❌ Wave 0 |
| BOOK-09 | Disclosure rule: `proposed_end > authorization_expires_at`; 48h caveat; null expiry shows no disclosure | unit (pure) | `--tests '*RescheduleDisclosureTest'` | ❌ Wave 0 |
| BOOK-09 | Decline: reason trimmed, blank omitted, 500 cap, body wire JSON | unit + MockWebServer | `--tests '*ClickMeApiTest'` | ✅ extend |
| BOOK-09 | Card: expert-initiated shows Accept/Decline; client-initiated read-only footer; buttons disabled in flight; hidden when not `pending_reschedule` | Compose | `--tests '*RescheduleResponseCardTest'` | ❌ Wave 0 |
| BOOK-06/07/08 | Routes registered with `BottomBar.hidden()`; `resetAndSelect` after cancel clears Explore and Bookings stacks and selects Bookings; `UpcomingBookingKey` reachable from Explore | unit / Robolectric nav | `--tests '*BookingFlowNavigationTest' --tests '*HomeTabScaffoldTest'` | ✅ extend |

### Sampling Rate
- **Per task commit:** the task's targeted `--tests` pattern (under 60 s).
- **Per wave merge:** `./gradlew testDevDebugUnitTest ktlintCheck && bash scripts/check-design-rules.sh`.
- **Phase gate:** full suite green on both flavors plus `assembleProdRelease` (R8) before `/gsd-verify-work`.

### Wave 0 Gaps
- [ ] `FakeClickMeApi`: add `cancelClientBookingHandler`, `acceptClientRescheduleHandler`, `declineClientRescheduleHandler` (+ call counters) and update `clientBookingsHandler` to take `(type)` per call so the two-bucket load can be scripted.
- [ ] `FakeRealtimeConnection`: add `fun emit(event: RealtimeEvent)` (`tryEmit`) so VM tests can drive `booking.update`.
- [ ] Test fixtures for `ClientBookingItem` / `ClientBookingDetail` with the new fields (extend `BookingTestFixtures.kt` style).
- [ ] New test files listed above (13 new files, 5 extended).
- No new framework or dependency install.

## Security Domain

`security_enforcement` is not set to false, so it is included. [VERIFIED: .planning/config.json has no override]

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no (existing session/JWT stack, unchanged) | existing `AuthHeaderInterceptor` / `TokenAuthenticator` |
| V3 Session Management | no | existing |
| V4 Access Control | yes | Server enforces ownership (`client_id` scope, 404 otherwise) and counterparty-only (403). The client only hides controls; never rely on the UI gate |
| V5 Input Validation | yes | Strict Moshi bodies; client caps (450-character comment, 500-character reason and decline reason via `capNotes`); the server's Zod `.strict()` is authoritative |
| V6 Cryptography | no | none added |
| V7 Error handling and logging | yes | Never log reason text, notes, `pm_` ids or the join link; `ApiException` messages carry status and code only (existing T-02-08) |

### Known Threat Patterns for this stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| IDOR on booking ids | Information disclosure / Tampering | Server 404 for non-owned; app never constructs ids from user text |
| Mass-assignment via request body | Tampering | `.strict()` server schemas; fixed Moshi request classes with only allowed keys |
| Injection through free text rendered back (reason, note, expert note) | Tampering | Render as plain `Text` only; never as HTML or in a `WebView`; server text via `FieldError.Text` (T-06-13) |
| Double-submit of cancel/accept | Tampering | `isSubmitting` guard in the VM plus server idempotent 200 handling |
| Sensitive data in logs or clipboard | Information disclosure | No logging of notes/ids; clipboard copy only on explicit user tap of the meeting link, no auto-copy (UI-SPEC A2) |
| Meeting link opening (Phase 9) | Spoofing | Not opened this phase; display and copy only |

## Sources

### Primary (HIGH confidence)
- Backend source (read in full for the relevant parts): `backend-reference/src/controllers/clientBooking.controller.js`, `src/schemas/booking.schema.js`, `src/routes/clientBooking.js`, `src/utils/refund.js`, `src/utils/reschedule.js`, `src/cron/longLeadChargeSweep.js`.
- Backend docs: `docs/ios/RESCHEDULE-PAYMENT-CONVERSION.md` (§1-§4), `docs/ios/STRIPE-INTEGRATION.md` (lines 122, 188-189), `docs/ios/CLIENT-TRANSACTIONS.md` (payment status vocabulary).
- iOS source: `MyBookingsViewModel.swift`, `BookingSummaryViewModel.swift` (accept/decline, disclosure), `ClientBookingDetail.swift`, `RescheduleProposal.swift`, `BookingStatus.swift`, `ClickMeAPI+Booking.swift` (endpoints).
- Android repo (read): `ClientBookingsRepository`, `ClientBookingsPayload`, `ClientBookingDetail`, `ClickMeApi`, `ApiRequestUrls`, `ApiException`, `ErrorCode`, `BookingErrors`, `PaidFlowError`, `HomeClientScreen`, `ClientBookingFlowEntries`, `Navigator`, `NavigationState`, `HomeTabScaffold`, `BookingFormatting`, `TodaysSessions`, `ExploreTodaysSessionsSection`, `Money`, `RealtimeService`, `RealtimeEvents`, `FakeClickMeApi`, `FakeRealtimeConnection`, `MainDispatcherRule`, `libs.versions.toml`, `scripts/check-design-rules.sh`.
- `PrimaryTabRow` and `PullToRefresh` present in the cached material3 1.4.0 sources (`TabRow.kt`, `PullToRefresh.kt`).

### Secondary (MEDIUM confidence)
- Phase 6/7 conventions as recorded in `.planning/STATE.md` and Phase 7 VALIDATION (`07-VALIDATION.md`).

### Tertiary (LOW confidence)
- None used for decisions.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH, no new libraries; all in the repo.
- Architecture: HIGH, direct analogues in Phases 5-7 and the Nav3 shell code was read.
- Backend contract: HIGH, read from source and the backend docs.
- Pitfalls: HIGH for contract-derived ones (1-6, 11); MEDIUM for Nav3 lifecycle ones (8, 10).

**Research date:** 2026-10-23
**Valid until:** about 30 days; re-check the backend controller if `clientBooking.controller.js` or `STRIPE-INTEGRATION.md` changes (especially payment capture timing, which would revive the dormant refund lines).

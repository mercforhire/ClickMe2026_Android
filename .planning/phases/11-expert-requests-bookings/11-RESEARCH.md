# Phase 11: Expert Requests & Bookings - Research

**Researched:** 2026-10-25
**Domain:** Android (Compose + Hilt + Retrofit/Moshi + Navigation 3) port of the iOS expert side; backend contract verification against `backend-reference/`
**Confidence:** HIGH for backend contract and in-repo reuse (read from source this session); MEDIUM for on-device behavior (calendar intent, Custom Tab return), which is deferred to manual checks.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

Carried forward (not re-opened):
- iOS is the source of truth (DEC-ios-source-of-truth). Material 3, dark only, Nunito. Pixel parity is out of scope.
- API pattern: `ApiRequestUrls` constants, `ClickMeApi` methods, and a Hilt `@Singleton` repository using `apiCall {}`. DTOs are Moshi codegen with explicit `@Json(name=…)`.
- Money uses minor units, formatted with `util/Money.kt`. Error copy is the server `message` in a Material `AlertDialog` (Phases 6–10).
- Join follows the iOS client-side window rule (Phase 9 D-02): never for `pending_approval`; for `completed` while `endTime > now`; otherwise ±3600s of start. A live clock re-evaluates it (D-03). Join goes through `CallStarter.startCall` / `CallGuard.startCall`, reusing `JoinCallButton` and `JoinClock`. The call stack is role-agnostic.
- Booking-modifying actions are guarded with `CallGuard.attemptModifyBooking` (Phase 9 D-12/D-13).
- Starting a reschedule is DEF-1, so the Reschedule button is hidden. Message buttons stay hidden until Phase 12. The private note card stays hidden (Phase 8 D-16 precedent).

Decline contract (EXP-06)
- **D-01: The decline picker uses the backend's 4 reason codes, not iOS's 5.** The backend `declineSchema` is `reason_code: z.enum(['CONFLICT','OUT_OF_SCOPE','UNAVAILABLE','OTHER'])`. iOS sends `OUTSIDE_EXPERTISE` / `SCHEDULE_CONFLICT` / `PRICING`, which the backend rejects with 422.
  - Labels: "Schedule conflict" → `CONFLICT`, "Outside my expertise" → `OUT_OF_SCOPE`, "Not available at that time" → `UNAVAILABLE`, "Other reason" → `OTHER`.
  - "Pricing mismatch" is dropped. Log an iOS follow-up, because iOS decline is broken against the backend.
- **D-02: `reason_text` is required by the backend (1–1000 chars), but the field stays optional in the UI.** If the expert leaves it empty, send the selected reason's label as `reason_text`. Cap input at 1000.
  - iOS sends nil when the field is empty, which also fails with 422. Log an iOS follow-up.
- **D-03: After a successful decline**, the sheet closes and the screen pops back to Incoming Requests. A brief "Request declined" snackbar shows, and the list is refreshed so the item is gone. This deviates from iOS, which stays on the screen with a "Declined" footer.

Accept → Confirmed flow (EXP-06, EXP-07)
- **D-04: iOS never shows `BookingConfirmedView`.** It's orphaned; after an accept, iOS only shows "Accepted!" in the footer. EXP-06 requires the Confirmed screen, so Android wires it in:
  - On accept success, the accept sheet closes and **Confirmed replaces Request Decision** in the back stack.
  - Back from Confirmed returns to a refreshed Incoming Requests, never to a stale decision screen.
  - Port the iOS Confirmed layout: check icon, headline, details card, and the buttons "Add to Calendar" / "Back to Home". The staggered entry animation is optional.
- **D-05: "Back to Home" pops to the current tab's root and lands on the Dashboard, refreshed**, so the pending count drops. If the flow didn't start on the Dashboard tab, switch to it.
- **D-06: Calendar intent content.** `Intent.ACTION_INSERT` on `CalendarContract.Events`:
  - Title: `"ClickMe · {topic} with {client name}"`. iOS uses the expert's own name, which is a bug.
  - `EXTRA_EVENT_BEGIN_TIME` / `EXTRA_EVENT_END_TIME` from the request's start and end.
  - Description: the meeting type ("In-App Voice" or "Skype/Zoom"), plus the client's message if present.
  - No location. No reminder: `ACTION_INSERT` can't set one, so the calendar app's default applies.
  - If no app can handle the intent, show a short message instead of crashing.
- **D-07: `409 ALREADY_PROCESSED` on accept or decline** (already handled, expired, or cancelled by the client) shows the server message in an `AlertDialog`. On OK, pop to a refreshed Incoming Requests list. Other errors show the server message and stay on the screen.
- **D-08: The accept sheet's optional message is capped at 1000 chars** (backend `acceptSchema`), with a character counter shown near the limit. It is trimmed, and nil when empty (iOS parity).
- Ported as-is from iOS (no discussion needed):
  - The Incoming Requests list: `GET /expert/booking-requests` with page 1, limit 50. Pending requests are sorted soonest-expiry first and expired ones sink to the bottom. Expired means `status == "expired"` or `expiresAt <= now`, with an amber avatar ring. There's pull-to-refresh and a quiet reload when the screen reappears.
  - Request Decision: `GET /expert/booking-requests/:id`. It shows the client header, the details card, the client message, and the earnings section from `economics.earnings_breakdown` (EXPERT-EARNINGS-BREAKDOWN.md; a null breakdown falls back to the session price plus "Payout amount pending"). An expired request shows the "This request has expired" banner and no footer.
  - Accept and decline are sheets (`ModalBottomSheet`). The footer's Message button is hidden until Phase 12.

Dashboard (EXP-05)
- **D-09: The payout card shows status and can launch onboarding.**
  - `GET /expert/connect/status` is authoritative: onboarding is required when `!charges_enabled || currently_due` is not empty.
  - `GET /expert/payouts/summary` is a soft fallback: a 400 means onboarding is required, but only when the status is unavailable.
  - When onboarding is required, the "Complete payout setup" card mints a link with `POST onboard`, or `refresh-link` when an incomplete account exists. It opens the URL in a Custom Tab (`components/web/CustomTabLauncher.kt`).
  - The tap is guarded by `CallGuard.attempt` with a new `RestrictedAction` for "set up payouts".
  - Show a spinner ("Opening secure setup…") while minting, and ignore repeat taps.
  - Re-fetch the status when the Dashboard resumes. Phase 14 adds the `clickme://connect/return|refresh` deep links and the Payouts screen.
  - When onboarded, show the iOS "Available Payout" and next-payout line from the summary if it loaded. Otherwise show "—".
- **D-10: Weekly earnings:** "{net} this week · {n} sessions" from `GET /expert/earnings/weekly`, formatted with `Money.kt` (minor units; verify in research). It fails soft: a failed fetch shows "—" and never blocks the dashboard.
- **D-11: The Expert Tools grid has all 6 cells (iOS parity):**
  - Pending Requests → Incoming Requests
  - Calendar → switch to the Bookings tab
  - Availability → push `AvailabilityKey(AvailabilityOrigin.Dashboard)` (a new origin value; the Phase 10 hand-off)
  - Topics → switch to the Topics tab
  - Profile Settings and Payouts → a "coming soon" placeholder until Phase 14 replaces them
- **D-12: The "N Reschedule Requests" banner and the "View Calendar" link both switch to the Bookings tab (Upcoming).**
  - Upcoming cards for `pending_reschedule` bookings where `reschedule.initiated_by == client` show a "Client proposed a new time · Tap to review" badge. This mirrors the Phase 8 client card badge.
  - The reschedule count only counts client-initiated proposals (iOS).
- **D-13: Refresh triggers** (same as Phase 8 D-10): a quiet reload when the tab is shown again, a quiet reload on a realtime `booking.update`, and pull-to-refresh.
- Ported as-is from iOS:
  - The hero "Hello, {first name}".
  - The pending-requests banner (count from `pagination.total_count`, falling back to the number of items).
  - Today's Sessions: upcoming bookings (limit 50) whose start is today on the device, with status `confirmed` / `in_progress` / `pending_reschedule`, sorted by start. Each card has a "STARTING NOW" pill within ±1h and a Join button. There's an empty state ("No sessions today").
  - A hard failure (bookings or requests) shows "Couldn't load dashboard" with Retry. The financial fetches are soft.

Expert bookings, Summary & cancel (EXP-08, EXP-09)
- **D-14: Join appears in three places:** the Upcoming list cards, the Dashboard Today's Sessions cards, **and** the expert Booking Summary. iOS has no Join on the Summary, so log an iOS follow-up.
- **D-15: The list follows iOS.**
  - Fetch `GET /expert/bookings?type=upcoming` and `type=past` (page 1, limit 50 each).
  - Upcoming includes `confirmed` / `pending_reschedule` / `in_progress`, plus completed bookings whose `endTime > now` (rejoinable), merged in.
  - Past is labeled completed / cancelled (cancelled or declined) / missed (missed or expired).
  - Tapping any card opens the expert Booking Summary.
  - Same page-1-only limit as Phase 8 D-09. Refresh triggers are as in D-13.
- **D-16: The expert-cancel preview mirrors the backend 24h rule.** The backend uses `computeRefundAmountMinorUnits` for expert cancels too. iOS always says "full refund". The client-facing wording:

  | Booking | Preview line |
  |---|---|
  | Captured, more than 24h before start | "Your client will receive a full refund of {amount}" |
  | Captured, within 24h | "Your client won't be refunded — cancelling within 24 hours of the session" |
  | `pending_approval` / hold only | "The client's card hold will be released" |
  | Free | no refund line |

  - Reuse the Phase 8 `RefundPreview.kt` logic.
  - Log a backend-owner note: a late expert cancel currently penalizes the client.
  - Log an iOS follow-up.
- **D-17: The expert cancel flow reuses the Phase 8 two-step full-screen pattern** (Phase 8 D-08).
  - Step 1: reason plus an optional comment, then Continue. Step 2: confirm, showing the booking summary and the refund line, with Keep Booking / Confirm Cancellation.
  - The expert reason labels are the iOS ones: "Personal Emergency", "Scheduling Conflict", "Technical Issues", "Other".
  - The request is `POST /expert/bookings/:id/cancel` with `reason` = `"<label>: <comment>"` or just the label, capped at 500.
  - Guarded by `attemptModifyBooking`.
  - On success, pop to the expert bookings list, refreshed. "Already cancelled" counts as success.
- **D-18: The reschedule response on the expert Summary** reuses `components/RescheduleResponseCard.kt` in an **expert variant**.
  - Accept: a confirm dialog ("Accept the proposed reschedule?" / "The session will move to the new time your client proposed."), then `POST /expert/bookings/:id/accept-reschedule`. There is no payment step.
  - Decline: a dialog with an optional reason (500 max), with the buttons Decline / "Keep session as-is", then `decline-reschedule`.
  - If the expert proposed the change (from iOS), the card is read-only with "Awaiting client's response".
  - `409 SLOT_UNAVAILABLE` shows "That time is no longer available — the booking stays at its original time." Then the booking is refreshed.
  - Other errors (403, 409 `INVALID_STATUS`, 422) show the server message.
  - Both buttons are guarded by `attemptModifyBooking`. Refresh the detail after any response.
- Ported as-is from iOS (Summary): `GET /expert/bookings/:id` shows the client header, date/time/duration, the meeting-type label and icon, the fee label (the server `consultation_fee.label`, falling back to amount + currency), and the preparation notes card. Cancel shows when `actions.can_cancel`.

### Claude's Discretion
- Visual layout, spacing and copy beyond what is pinned above. Follow the iOS layouts as stand-ins.
- Route key names and package layout, mirroring the iOS tree: `screens/expertmode/dashboard/`, `incomingrequests/`, `requestdecision/`, `confirmed/`, `upcomingbookings/`, `bookingsummary/`, `cancellation/`. Incoming Requests and Decision are pushed on the current tab's stack.
- Whether the expert cancel screen is a parameterized version of the client one or a sibling sharing subviews.
- The exact `RestrictedAction` names and `call_action_*` strings for payouts and the expert reschedule response.
- The "coming soon" placeholder: a shared screen or a snackbar.
- DTOs for the booking requests, expert bookings, weekly earnings, connect status and payout summary endpoints.

### Deferred Ideas (OUT OF SCOPE)
- **Message buttons** on Request Decision and the expert Summary: Phase 12 (`initiateChat`).
- **Payouts screen, Connect deep-link return/refresh, Profile Settings:** Phase 14. The Tools grid shows "coming soon" placeholders until then.
- **Expert private booking note** (`ExpertBookingNoteView`): deferred, as in Phase 8 D-16.
- **Starting a reschedule (expert):** DEF-1.
- **Paging the expert bookings past page 1:** same later improvement as Phase 8 D-09.
- **Backend-owner note:** an expert cancel within 24h gives the client no refund. Policy question for the backend owner.
- **Client profile from a request ("View Profile"):** DEF-3 (Client Profile, expert side).
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| EXP-05 | Expert sees a simplified Dashboard: pending request count, upcoming sessions, weekly earnings text and a payout onboarding card. | Endpoint shapes for `/expert/booking-requests` (no pagination block), `/expert/bookings`, `/expert/earnings/weekly` (minor units, never null), `/expert/connect/status`, `/expert/payouts/summary`, onboard/refresh-link (Section "Backend Contract"); Dashboard VM pattern from `MyBookingsViewModel`; soft-fail rules from iOS `ExpertDashboardViewModel`. |
| EXP-06 | Expert can open Incoming Requests and accept (optional message) or decline (reason code); after accepting they see Confirmed. | List/detail/accept/decline contract, strict schemas, `ALREADY_PROCESSED`, expired-state caveat (detail has no status), Confirmed data source. |
| EXP-07 | Expert can add a confirmed session to the calendar via `Intent.ACTION_INSERT`. | Android Calendar Provider intent docs; no permission needed; no `<queries>` in the manifest so use `try/catch ActivityNotFoundException`. |
| EXP-08 | Expert can view Upcoming and Past bookings; Join appears inside the join window using the client call stack. | `/expert/bookings` shape (status is an object), upcoming feed includes `pending_approval`, rejoin merge needs both buckets, `JoinWindow`/`JoinClock`/`JoinCallButton`/`CallStarter` reuse. |
| EXP-09 | Expert Booking Summary: accept/decline a client reschedule (handle `409 SLOT_UNAVAILABLE`) and cancel the booking. | Detail endpoint is `/expert/bookings/:id/details`; reschedule accept/decline semantics and error codes; cancel semantics (void vs refund), error codes, response units; expert variant of `RescheduleResponseCard`. |
</phase_requirements>

## Summary

Phase 11 is a large but pattern-rich phase: almost every moving part has a Phase 8/9/10 analogue to copy (My Bookings state machine, two-step cancel, shared reschedule controller, `JoinCallButton`/`JoinClock`, `FakeClickMeApi` test harness). No new library is needed. The planner's biggest risk is not code structure but **contract drift**: reading the backend source this session showed that several statements in CONTEXT.md and the UI-SPEC do not match what the server actually does. Those are listed first (Section "Contract Corrections") because each one changes a task. The most consequential: the expert booking detail URL is `GET /expert/bookings/:id/details` (there is no `GET /expert/bookings/:id`); `/expert/booking-requests` has no pagination block and ignores `page`/`limit`; the request-detail response carries no status or expiry, so "expired" must be handed from the list; the expert detail response exposes no `payment_status`, and because the backend now captures only after a session completes, every cancellable paid booking is a card hold, so the D-16 "within 24h the client is not refunded" line would be wrong in practice.

The weekly-earnings question is settled: `/expert/earnings/weekly` returns **integer minor units** (`gross_amount`, `net_amount`, `session_count`), and `net_amount` is never null (the controller coalesces to `0`; zero-session weeks report currency `CAD`). Connect status returns only `{charges_enabled, currently_due}`; onboard and refresh-link return `{url}`. Expert reschedule accept/decline are idempotent (a repeat returns 200), `SLOT_UNAVAILABLE` on accept comes from the database exclusion constraint (30-minute session buffer), and the expert cancel returns `refund_amount` in major units.

**Primary recommendation:** Build the phase as seven vertical slices on one shared data layer (`ExpertBookingsRepository`, `BookingRequestsRepository`, `ExpertEarningsRepository`/`ConnectRepository`), reuse Phase 8/9 components by parameterizing them for the expert viewer, and fix the contract corrections in the first plan (URLs, DTOs, error mapper, refund preview inputs) before any screen work.

## Contract Corrections (read before planning)

Each row is a place where CONTEXT.md or the UI-SPEC disagrees with source that was opened this session. "Plan impact" says what to change.

| # | CONTEXT / UI-SPEC says | Source says | Plan impact |
|---|---|---|---|
| C1 | `GET /expert/bookings/:id` loads the Summary and the cancel screen; `ApiRequestUrls` gets `expert/bookings[/:id[...]]` | Routes are `router.get('/expert/bookings/:id/details', ensureExpert, getDetails);` and `router.get('/expert/bookings', ensureExpert, getFeed);` (`backend-reference/src/routes/expertBooking.js`). No bare `/:id` GET exists. iOS: `case .getExpertBookingDetails: return "/expert/bookings/:id/details"` (`ios-reference/ClickMe2026/Network/APIRequestURLs.swift:258`) [VERIFIED] | Add `EXPERT_BOOKING_DETAILS = "expert/bookings/{id}/details"`. A bare-id GET returns 404. |
| C2 | Pending count from `pagination.total_count`; list is page 1, limit 50 | `list()` returns `res.status(200).json({ status: 'success', data: { requests } });` with no pagination and never reads `page`/`limit` (`bookingRequests.controller.js:133`). It filters `targetStatus = statusFilter === 'expired' ? 'expired' : 'pending_approval'` (`:58`) [VERIFIED] | Count is `requests.size`. Model `pagination` as absent. Do not send `page`/`limit` (harmless, but pointless). Expired-by-time rows still `pending_approval` (until the cron flips them) are the only "expired" rows the default list returns; `expiresAt <= now` is the real test. |
| C3 | Request Decision shows an expired banner from the loaded request | `getDetail` response has `request_id`, `client`, `session{start_time,end_time,topic,meeting_type,client_message}`, `economics{potential_earnings,currency,earnings_breakdown}`. No `status`, no `expires_at` (`bookingRequests.controller.js:176-208`). iOS passes it in: `init(requestId: UUID, isExpired: Bool = false, ...)` (`RequestDecisionViewModel.swift`) [VERIFIED] | `RequestDecisionKey` must carry `isExpired` (and ideally `expiresAtMillis`), set from the tapped list item. A request opened without it (future push routing) is treated as pending and the server answers `409 ALREADY_PROCESSED` if it is not. |
| C4 | Use legacy money fields for the card/earnings | `potential_earnings: data.amount_paid_minor_units / 100` is hard-coded /100 even for JPY (`bookingRequests.controller.js:195-197`); list `earnings` divides by `10^digits`. `earnings_breakdown` amounts are major units already digit-correct (`utils/earnings.js`) [VERIFIED] | Show money only from `earnings_breakdown` (major units, convert with `Money.formatMajor(amount, currency)`). For the "Session price" fallback card use `economics`-independent data: breakdown `gross_amount` when present, else list `earnings` (digit-correct) passed in the key; never `potential_earnings` for zero-decimal currencies. |
| C5 | Fee label is the server `consultation_fee.label` | Server builds ``label: `$${major.toFixed(digits)} / session` `` (`expertBooking.controller.js:138`): the `$` is hard-coded regardless of currency [VERIFIED] | Format the fee client-side: `Money.formatMajor(consultation_fee.amount, consultation_fee.currency)` (optionally + " / session"). Treat `amount == 0.0` as free (the server writes `amount_paid_minor_units \|\| 0`). Keep the label only as a last-resort fallback. |
| C6 | (implicit) booking `status` is a string as on the client | Expert endpoints return `status: statusMeta(b.status)` = `{ label, code, color_accent }` (`expertBooking.controller.js:80-83,257,334`) [VERIFIED] | DTO `ExpertStatusMeta(label, code, @Json color_accent)`; branch on `status.code`. Do not reuse `ClientBookingItem` (string status). |
| C7 | D-16: captured >24h = full refund line; captured <=24h = "won't be refunded"; hold = release; `GET /expert/bookings/:id` exposes `payment_status`/`amount_paid` | The expert detail `select` reads `payment_status` but the **response** has no payment field: only `session.consultation_fee{amount,currency,label}` and `earnings_breakdown` (`expertBooking.controller.js:330-364`). Money lifecycle: "Expert accepts: Booking becomes confirmed. Still only a hold"; capture happens "when the session completes"; "a cancelled ... booking's hold is voided, never captured" (`docs/ios/STRIPE-INTEGRATION.md:122,188`). Cancel voids when the Stripe PI is `requires_capture` regardless of the 24h rule (`expertBooking.controller.js:1138-1152`); long-lead bookings are charged at T-24h as a manual-capture PaymentIntent that ends in `requires_capture` (`cron/longLeadChargeSweep.js:34-38`) [VERIFIED] | Every cancellable paid booking (`confirmed`, `pending_approval`, `pending_reschedule`) is a hold; expert cancel releases it and the response `refund_amount` is `0`. Implement D-16 as a pure function over a `PaymentState {None, Held, Captured}` so the table stays encoded, but feed it `Held` for paid bookings (paid = `consultation_fee.amount > 0`). Result: paid shows "The client's card hold will be released", free shows nothing. The Captured rows are dormant, exactly like Phase 8's `RefundPreview.Full`. The CONTEXT backend-owner note ("a late expert cancel penalizes the client") is not supported by the code for held bookings: drop it or reword it as unverified (see Open Question 1). |
| C8 | "Already cancelled counts as success" | A second cancel on a `cancelled` booking returns `409 INVALID_STATUS` ("Cannot cancel a booking with status 'cancelled'", `expertBooking.controller.js:1077-1080`). `409 ALREADY_CANCELLED` is only the lost-race branch after the pre-check (`:1095-1097`) [VERIFIED] | Treat `ALREADY_CANCELLED` as success (D-17). `INVALID_STATUS` stays the UI-SPEC dialog ("This booking can no longer be cancelled." then pop to the refreshed list). Cancellable set is `['confirmed', 'pending_approval', 'pending_reschedule']` (`:64`), so `in_progress` is not cancellable: trust `actions.can_cancel`. |
| C9 | Reschedule errors: 403, 409 `INVALID_STATUS`, 422 show server message; reuse Phase 8 mapper | Expert accept/decline: already-resolved is an **idempotent 200** (accept: `confirmed` returns 200; any other non-`pending_reschedule` status returns `409 INVALID_STATUS`, `:748-758`; decline: any non-pending status returns 200, `:962-968`); `403 FORBIDDEN` when `proposed_by !== 'client'` (`:761-763`, `:970-972`); `422 VALIDATION_ERROR` when the session time or proposed time has passed (`:776-783`); `409 CLIENT_UNAVAILABLE` when the client is suspended (`:767-771`); `422 RESCHEDULE_OUTSIDE_AUTH_WINDOW` for unconvertible legacy rows (`:818-822`); `409 SLOT_UNAVAILABLE` is not thrown in the handler: the final `UPDATE` of `start_time`/`end_time` trips the DB exclusion constraint and `dbError` maps SQLSTATE `23P01` to `AppError(409, 'SLOT_UNAVAILABLE', ...)` (`utils/errors.js:99-100`); `docs/ios/SESSION-BUFFER.md` confirms accept-reschedule returns it for back-to-back sessions [VERIFIED] | Write a separate `ApiException.toExpertRescheduleFailure()`. The existing `toRescheduleFailure()` is client-worded (`ExpertUnavailable`, and `ValidationError` -> "Ask the expert to propose a new time") and has no `SlotUnavailable` branch, so it cannot be reused as-is. Mapping: `SlotUnavailable` -> UI-SPEC copy + reload; `Forbidden` -> "You can't respond to this reschedule." + reload; `InvalidStatus` -> "This reschedule is no longer pending." + reload; `ValidationError`, `ClientUnavailable`, `RescheduleOutsideAuthWindow` -> server message + reload; `NotFound` -> pop; `AccountSuspended` -> silent. |
| C10 | Accept sends an optional message that reaches the client | `accept()` never reads the body (`bookingRequests.controller.js:231-289`: only `req.user.id` and `req.params.id`); `acceptSchema` only permits `message` up to 1000 (`booking.schema.js:167-169`) [VERIFIED] | Still send it (D-08, harmless, future-proof) but log a backend-owner note: the message is validated and discarded. |
| C11 | Summary shows a meeting-link row (`ExpertMeetingLinkRow`, "Not available yet") | The expert detail response has no link field (`getDetails` data: `booking_id, status, reschedule, authorization_expires_at, client, session{topic_title, consultation_fee, earnings_breakdown, schedule, meeting_type}, preparation_notes, expert_private_note, actions`); iOS `ExpertBookingSummaryView` has no link row either. The Skype/Zoom URL comes only from `POST /bookings/:id/join` -> `external_config.join_url` (`call.controller.js:84-96`) [VERIFIED] | Drop `ExpertMeetingLinkRow` (it would always read "Not available yet"). Join for Skype/Zoom goes through the Phase 9 path (`rememberExternalMeetingLauncher`). |
| C12 | "The other tab loads when first selected" (UI-SPEC surface 5) | Phase 8 loads both buckets on every load because the rejoin rule moves rows from Past into Upcoming (`MyBookingsViewModel.loadAll`, `buildBookingLists`). The upcoming feed also returns `pending_approval` rows (`UPCOMING_STATUSES = ['pending_approval', 'confirmed', 'pending_reschedule', 'in_progress']`, `:58`) [VERIFIED] | Load both buckets together (parallel `async`). Filter Upcoming to `confirmed`/`pending_reschedule`/`in_progress` with `endTime > now`, plus rejoinable `completed` (iOS `isUpcoming`/`isRejoinableCompleted`). Dashboard Today's Sessions already excludes `pending_approval`. |
| C13 | UI-SPEC check badge = "Phase 6 values" (primary 12% fill, 30% border, `Check`) | The existing `BookingRequestSentCheckIcon` is a `surfaceContainer` circle with a 25% `primary` border and an `Outlined.CheckCircle` glyph [VERIFIED: `screens/clientmode/requestsent/subviews/BookingRequestSentCheckIcon.kt`] | Generalize/reuse the existing composable as-is; do not re-skin Phase 6. |

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Accept / decline request, cancel, reschedule accept/decline | API / Backend | Client (UI gates) | Server enforces ownership, state machine, Stripe void/refund. UI gates (`can_cancel`, window) are convenience only. |
| Join window gate | Client (UX gate) | API (`POST /join` final say: `[start-120min, end+120min]` else `403 SESSION_NOT_ACTIVE`) | Phase 9 D-02: client rule is ±3600 s; server is authoritative. |
| Weekly earnings, payout balance, Connect readiness | API / Backend (Stripe live data) | Client (soft-fail display) | Backend aggregates `expert_earnings` and reads live Stripe; client never computes. |
| Net-earnings math | API / Backend | — | `earnings_breakdown` is computed server-side; client only formats. |
| Refund preview line | Client (informational) | API (authoritative result in cancel response) | Server decides void vs refund from live PI state. |
| Today's Sessions filter, sorting, expiry labels | Client | — | Derived from the device clock/zone (iOS parity). |
| Add to calendar | Client (OS intent) | — | `ACTION_INSERT` hands off to the calendar app; no permission. |
| Onboarding URL | API (mints single-use Stripe link) | Client (Custom Tab) | URL is a short-lived secret: open once, never persist or log. |
| Cross-screen hand-offs (reload on arrival, snackbar) | Client (singleton launch-request store) | — | Same pattern as `BookingsLaunchRequest` (Phase 8). |

## Standard Stack

No new dependencies. Everything this phase needs is already in `app/build.gradle.kts` / `gradle/libs.versions.toml` (checked this session).

### Core (existing, reuse)
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Compose BOM | 2026.09.00 (`gradle/libs.versions.toml:6`) | Material 3 (`ModalBottomSheet`, `PullToRefreshBox`, `PrimaryTabRow`, `AlertDialog`) | `PullToRefreshBox` is already used in `MyBookingsScreen.kt`, `ExploreScreen.kt` [VERIFIED: grep]; `ModalBottomSheet` already used in `PaymentInfoSheet.kt` etc. |
| Navigation 3 | 1.2.0 (`libs.versions.toml:7`) | Per-tab back stacks, `entry<Key>(metadata = BottomBar.hidden())`, `Navigator.replaceTop/resetAndSelect/navigate(tab)` | Existing shell. |
| Hilt | 2.60.1 | `@HiltViewModel`, assisted factories for key-scoped VMs | Existing (`ClientCancellationViewModel` pattern). |
| Retrofit + Moshi codegen | existing | Endpoints/DTOs | Existing; `InstantAdapter` handles ISO instants. |
| androidx.browser | existing (`implementation(libs.androidx.browser)`) | Custom Tab for Stripe onboarding | `rememberCustomTabLauncher` already exists. |
| MockWebServer3 / Robolectric / compose-ui-test / coroutines-test | existing | Tests | `app/build.gradle.kts` testImplementation lines. |

### Supporting
| Item | Purpose | When to Use |
|------|---------|-------------|
| `android.provider.CalendarContract` (platform) | `Events.CONTENT_URI`, `EXTRA_EVENT_BEGIN_TIME/END_TIME`, `Events.TITLE/DESCRIPTION` | Add to Calendar. No permission, no library. |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| `ACTION_INSERT` intent | `CalendarContract` insert via ContentResolver | Needs `WRITE_CALENDAR` runtime permission; EXP-07 and D-06 explicitly require the intent. Do not use. |
| `resolveActivity()` pre-check | `try { startActivity } catch (ActivityNotFoundException)` | The manifest has no `<queries>` (grep of `AndroidManifest.xml` shows none), so on Android 11+ `resolveActivity` can return null even when a calendar app exists. Use try/catch. |

**Installation:** none.

**Version verification:** no packages added, so no registry check was needed. Material3 resolved version was not read from a lockfile (the UI-SPEC FLAG stands); `./gradlew :app:dependencies --configuration devDebugRuntimeClasspath | grep material3` settles it if a plan needs an API from a specific version.

## Package Legitimacy Audit

No external packages are installed in this phase. `CalendarContract` and `Intent` are Android platform APIs; `androidx.browser`, Moshi, Retrofit, Hilt, Navigation 3 are already dependencies.

**Packages removed due to [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none

## Backend Contract (verified from source)

All paths are relative to `/api/v1/`. Envelope: `{ "status": "success", "data": ..., "message"?: ... }`; errors: `{ "status": "error", "code"?: ..., "message": ... }`; validation failures are `422` with `errors:[{field,message}]` (`middleware/validate.js`, `utils/errors.js`). All `/expert/*` routes use `ensureExpert`.

### Endpoints

| Method + path | Request | Response `data` | Errors |
|---|---|---|---|
| `GET expert/booking-requests` | optional `sort_by` (`expiration` default, `earnings`, `date`), `status` (`pending` default, `expired`); `page`/`limit` ignored | `{ requests: [ { request_id, client{id, full_name, avatar_url}, topic, earnings (major units, digit-aware), currency, earnings_breakdown, time_slot{start_time,end_time}, expires_at, status } ] }` | 500 |
| `GET expert/booking-requests/{id}` | — | `{ request_id, client{id,name,avatar_url}, session{start_time,end_time,topic,meeting_type,client_message}, economics{potential_earnings (/100, avoid), currency, earnings_breakdown} }` | `404 NOT_FOUND` "Booking request not found." |
| `POST expert/booking-requests/{id}/accept` | strict `{ message?: string<=1000 }`; send `{}` when empty (iOS: body-less POST gets `VALIDATION_ERROR`) | `{ booking_id, status:"confirmed", confirmed_at, meeting_link:null }` + top-level `message` | `404 NOT_FOUND`; `409 ALREADY_PROCESSED` (not `pending_approval` any more: handled, expired, cancelled); `422 VALIDATION_ERROR` |
| `POST expert/booking-requests/{id}/decline` | strict `{ reason_code: CONFLICT\|OUT_OF_SCOPE\|UNAVAILABLE\|OTHER, reason_text: string 1..1000 }` (both required) | `{ request_id, status:"declined", declined_at }` | `404`; `409 ALREADY_PROCESSED`; `422 VALIDATION_ERROR` |
| `GET expert/bookings?type=upcoming\|past&page&limit` | `limit` 1..100 (default 10); `type` required else `422 VALIDATION_ERROR` | `{ view_state, bookings:[ { booking_id, client{id,name,avatar_url}, session{topic,start_time,end_time,meeting_type,consultation_fee,earnings_breakdown}, status{label,code,color_accent}, actions{can_join,can_reschedule,can_message}, private_note_preview, reschedule{proposed_start,initiated_by}\|null } ], pagination{total_count,total_pages,current_page} }` | 422 |
| `GET expert/bookings/{id}/details` | — | `{ booking_id, status{...}, reschedule{proposed_start,proposed_end,proposed_timezone,initiated_by,note,created_at}\|null, authorization_expires_at, client{id,name,avatar_url}, session{topic_title,consultation_fee{amount,currency,label},earnings_breakdown,schedule{start_time,end_time},meeting_type}, preparation_notes{text}, expert_private_note, actions{can_join,can_reschedule,can_cancel,can_message} }` | `404 NOT_FOUND` "Booking not found." |
| `POST expert/bookings/{id}/cancel` | strict `{ reason?: string<=500 }` | `{ refund_amount (MAJOR units), currency }` + message | `404`; `409 INVALID_STATUS`; `409 ALREADY_CANCELLED` (race) |
| `POST expert/bookings/{id}/accept-reschedule` | strict `{ stripe_payment_method_id?: pm_... }`; expert ignores it; send `{}` | `{ booking_id, status:"confirmed" }` (idempotent 200 if already confirmed) | `404`; `403 FORBIDDEN`; `409 INVALID_STATUS`; `409 CLIENT_UNAVAILABLE`; `409 SLOT_UNAVAILABLE`; `422 VALIDATION_ERROR`; `422 RESCHEDULE_OUTSIDE_AUTH_WINDOW` |
| `POST expert/bookings/{id}/decline-reschedule` | strict `{ reason?: string<=500 }` (persisted to `decline_reason_text`) | `{ booking_id, status }` (idempotent 200 if already resolved; restores `reschedule_prev_status`, fallback `confirmed`) | `404`; `403 FORBIDDEN`; `409 INVALID_STATUS` (rare race) |
| `GET expert/earnings/weekly` | optional `week_start=YYYY-MM-DD` (a Monday) | `{ week_start, week_end, gross_amount, net_amount, currency, session_count }`, integers in **minor units** | `422 INVALID_WEEK_START` |
| `GET expert/connect/status` | — | `{ charges_enabled: bool, currently_due: [string] }` only | `404 NOT_FOUND` (no connected account yet) |
| `POST expert/connect/onboard` | none | `{ url }` | `500 CONFIG_MISSING`, `422 INVALID_COUNTRY`, `403 CONNECT_NOT_RATIFIED` |
| `POST expert/connect/refresh-link` | none | `{ url }` | `404 NOT_FOUND`, `500 CONFIG_MISSING` |
| `GET expert/payouts/summary` | — | `{ available_amount, pending_amount, currency, next_payout_date ('YYYY-MM-DD' or null), payout_method:"bank_transfer" }`, minor units | `400 NO_PAYOUT_METHOD`, `400 KYC_INCOMPLETE` |

Sources (all `[VERIFIED]`, opened this session): `backend-reference/src/controllers/bookingRequests.controller.js`, `expertBooking.controller.js`, `earnings.controller.js`, `connect.controller.js`, `payouts.controller.js`, `schemas/booking.schema.js`, `routes/*.js`, `docs/ios/STRIPE-INTEGRATION.md` (§3.1-3.3, §5), `docs/ios/EXPERT-EARNINGS-BREAKDOWN.md`.

### Verbatim values the code must match

- `declineSchema` (`booking.schema.js:158-161`): `reason_code: z.enum(['CONFLICT', 'OUT_OF_SCOPE', 'UNAVAILABLE', 'OTHER']),` and `reason_text: z.string().min(1).max(1000),`
- `acceptSchema` (`booking.schema.js:167-169`): `message: z.string().max(1000).optional(),`
- `cancelSchema` (`booking.schema.js:119-121`): `reason: z.string().max(500).optional(),`; `rescheduleDeclineSchema` (`:196-198`): `reason: z.string().max(500).optional(),`; `rescheduleAcceptSchema` (`:180-182`): `stripe_payment_method_id: z.string().startsWith('pm_').optional(),`
- Status sets (`expertBooking.controller.js:58,61,64`): `UPCOMING_STATUSES = ['pending_approval', 'confirmed', 'pending_reschedule', 'in_progress']`, `PAST_STATUSES = ['completed', 'cancelled', 'declined', 'missed', 'expired']`, `CANCELLABLE_STATUSES = ['confirmed', 'pending_approval', 'pending_reschedule']`
- `reschedule.initiated_by` is the raw `proposed_by` column: `'client'` or `'expert'` (`expertBooking.controller.js:99,110`; set to `'expert'` at `:654`; client side `clientBooking.controller.js:17` documents `proposed_by='client'`).
- `earnings_breakdown.fee_source` is one of `'booking'|'expert_default'|'unavailable'`; `commission_pct` is a percent (15.0 = 15%), can be `0`; `currency` uppercase, defaults `'CAD'` (`utils/earnings.js`).
- Android `ErrorCode` already contains every needed code (`model/enums/ErrorCode.kt:38,40,41,42,54,96,97,122`): `SlotUnavailable("SLOT_UNAVAILABLE")`, `InvalidStatus("INVALID_STATUS")`, `AlreadyCancelled("ALREADY_CANCELLED")`, `AlreadyProcessed("ALREADY_PROCESSED")`, `RescheduleOutsideAuthWindow("RESCHEDULE_OUTSIDE_AUTH_WINDOW")`, `NoPayoutMethod("NO_PAYOUT_METHOD")`, `KycIncomplete("KYC_INCOMPLETE")`, `ClientUnavailable("CLIENT_UNAVAILABLE")`. No enum change needed.

### Money units, resolved

| Field | Unit | Convert / format with |
|---|---|---|
| `/expert/earnings/weekly` `gross_amount`, `net_amount` | integer minor units (`get_weekly_earnings` returns `bigint`; iOS `WeeklyEarningsData` decodes `Int`) | `Money.format(Long, currency)` |
| `net_amount` null? | Never: `net_amount: row.net_amount \|\| 0` (`earnings.controller.js:157`); `currency: (row.currency \|\| 'cad').toUpperCase()` (`:159`) | Zero-session week = `0` + `"CAD"`. D-10's "—" applies to a failed fetch only. |
| weekly `net_amount` accuracy | Known backend gap: built from a legacy env-var rate, so net can equal gross; "do not correct it client-side" (`EXPERT-EARNINGS-BREAKDOWN.md` §6.1) | Display as-is; log as a backend note. |
| `/expert/payouts/summary` amounts | integer minor units (`STRIPE-INTEGRATION.md:296`) | `Money.format` |
| `earnings_breakdown.*` amounts | MAJOR units, digit-correct (`÷10^digits`) | `Money.formatMajor(Double, currency)` (exists, HALF_UP) |
| list `earnings`, `consultation_fee.amount` | MAJOR units, digit-correct | `Money.formatMajor` |
| request detail `potential_earnings` | `÷100` always (wrong for zero-decimal) | do not use for display |
| cancel `refund_amount` | MAJOR units | `Money.formatMajor` |

## Architecture Patterns

### System Architecture Diagram

```
                       +-------------------- Expert shell (HomeExpertShell) --------------------+
                       |  per-tab Nav3 back stacks; detail entries use BottomBar.hidden()         |
                       |                                                                          |
 Dashboard tab         |  DashboardRoot --(banner / Pending tool)--> IncomingRequestsKey          |
 (ExpertDashboardVM)   |     |   ^                                      |  tap card (carries      |
   |  \                |     |   | reload-on-arrival,                   v  isExpired)             |
   |   +-> GET bookings|     |   | snackbar (ExpertLaunchRequest) RequestDecisionKey(id,isExpired)|
   |   +-> GET requests|     |   |                                      | Accept sheet / Decline  |
   |   +-> GET weekly  |     |   +--- Decline ok: goBack() <------------+ sheet (ModalBottomSheet) |
   |   +-> GET connect |     |        Accept ok: replaceTop(BookingConfirmedKey(data from detail))|
   |   +-> GET payouts |     |                         |                                          |
   |   +-> POST onboard|     |                         +--> Add to Calendar (ACTION_INSERT)       |
   |       -> CustomTab|     |                         +--> Back to Home: resetAndSelect(Dashboard)|
   |                   |     +--(Today card / Join)--> JoinCallButton -> CallStarter.startCall      |
   |                   |     +--(Availability tool)--> AvailabilityKey(Origin.Dashboard)            |
   v                   |     +--(Calendar tool / banners)--> navigate(ExpertBookingsRoot) [Upcoming]|
 Realtime booking.update                                                                           |
   (quiet reload) ---> |  ExpertBookingsRoot (ExpertBookingsVM: upcoming+past loaded together)     |
                       |     +-- card tap --> ExpertBookingSummaryKey(id, origin)                   |
                       |            |  GET /expert/bookings/{id}/details                            |
                       |            +-- RescheduleResponseCard(expert variant) -> accept/decline    |
                       |            |      guarded attemptModifyBooking -> POST accept|decline-...  |
                       |            +-- JoinCallButton (D-14)                                       |
                       |            +-- Cancel -> ExpertCancelKey(id): 2-step -> POST cancel        |
                       |                   success: post launch(Past tab, notice) -> Bookings root  |
                       +--------------------------------------------------------------------------+
        Data layer (Hilt @Singleton, apiCall{}): BookingRequestsRepository, ExpertBookingsRepository,
        ExpertEarningsRepository (weekly + payouts + connect)  ->  ClickMeApi (Retrofit) -> backend
```

### Recommended Project Structure
```
app/src/main/java/com/uptrendinvestments/clickmeapp/
├── model/requests/ExpertBookingRequests.kt     # AcceptRequestBody, DeclineRequestBody (reuse CancelBookingRequest, DeclineRescheduleRequest, AcceptRescheduleRequest)
├── model/responses/BookingRequestsPayload.kt   # list + detail + EarningsBreakdownDto
├── model/responses/ExpertBookingsPayload.kt    # feed item, detail, ExpertStatusMeta, actions
├── model/responses/ExpertFinancialData.kt      # WeeklyEarningsData, ConnectStatusData, ConnectLinkData, PayoutSummaryData
├── network/ApiRequestUrls.kt                   # + 12 constants (see below)
├── network/ClickMeApi.kt                       # + 14 methods
├── screens/expertmode/
│   ├── data/ (or per-feature)                  # BookingRequestsRepository, ExpertBookingsRepository, ExpertFinancialRepository
│   ├── ExpertLaunchRequest.kt                  # @Singleton one-shot hand-off (mirror BookingsLaunchRequest)
│   ├── dashboard/ (+subviews/)
│   ├── incomingrequests/ (+subviews/)
│   ├── requestdecision/ (+subviews/)           # keys, VM, sheets, DeclineReason enum
│   ├── confirmed/ (+subviews/)                 # CalendarIntent.kt (pure builder)
│   ├── upcomingbookings/ (+subviews/)          # ExpertBookingBuckets.kt (pure)
│   ├── bookingsummary/ (+subviews/)
│   └── cancellation/                           # ExpertCancelReason, ExpertRefundPreview, VM, screen
└── managers/CallDialog.kt                      # + RestrictedAction.SetUpPayouts, RespondToReschedule (update CallDialogs.kt `when`)
```

New `ApiRequestUrls` constants (existing style, no leading slash, `{id}` placeholders): `EXPERT_BOOKING_REQUESTS = "expert/booking-requests"`, `EXPERT_BOOKING_REQUEST = "expert/booking-requests/{id}"`, `..._ACCEPT`, `..._DECLINE`, `EXPERT_BOOKINGS = "expert/bookings"`, `EXPERT_BOOKING_DETAILS = "expert/bookings/{id}/details"`, `EXPERT_BOOKING_CANCEL`, `EXPERT_BOOKING_ACCEPT_RESCHEDULE`, `EXPERT_BOOKING_DECLINE_RESCHEDULE`, `EXPERT_EARNINGS_WEEKLY`, `EXPERT_PAYOUTS_SUMMARY`, `EXPERT_CONNECT_STATUS`, `EXPERT_CONNECT_ONBOARD`, `EXPERT_CONNECT_REFRESH_LINK`. `ApiRequestUrlsTest` exists: extend it.

### Pattern 1: Reuse the Phase 8 list state machine
**What:** `MyBookingsViewModel` (`LoadMode.Loud/Quiet/Pull`, `loadJob` cancel-on-restart, `onScreenShown/onScreenHidden`, realtime `filterIsInstance<RealtimeEvent.BookingUpdate>`, `SavedStateHandle` tab, `joinNow = joinClock.now`, launch-request consume in `init`) is the template for `ExpertBookingsViewModel` and, with different fetches, `ExpertDashboardViewModel` and `IncomingRequestsViewModel`.
**When to use:** every list/dashboard in this phase.
**Example (rejoin merge, adapted from `buildBookingLists`, iOS `UpcomingBookingsViewModel.isUpcoming`):**
```kotlin
// Source: screens/clientmode/mybookings/BookingBuckets.kt + ios-reference UpcomingBookingsViewModel.runFetch
fun buildExpertBookingLists(
    upcoming: List<ExpertBookingItem>, past: List<ExpertBookingItem>, now: Instant,
): ExpertBookingLists {
    val rejoinable = past.filter { it.status.code == "completed" && it.endTime.isAfter(now) }
    val live = upcoming.filter { it.status.code in LIVE_STATUSES && it.endTime.isAfter(now) } // confirmed, pending_reschedule, in_progress
    return ExpertBookingLists(
        upcoming = (live + rejoinable).sortedBy { it.startTime }.distinctBy { it.bookingId },
        past = past.filterNot { it in rejoinable }.sortedByDescending { it.startTime }
            .mapNotNull { pastStatusDisplay(it.status.code)?.let { d -> ExpertPastRow(it, d) } }.distinctBy { it.item.bookingId },
    )
}
```
`pastStatusDisplay(code)` and `BookingStatusBadge` are reusable as-is (they take raw status strings).

### Pattern 2: Cross-screen one-shot hand-off
**What:** `@Singleton ExpertLaunchRequest` (mirror `BookingsLaunchRequest`: `MutableStateFlow<Launch?>`, `post`, `getAndUpdate{null}` `consume`). Consumers: Incoming Requests (reload + "Request declined" snackbar), Dashboard (reload after Back to Home), ExpertBookings (select Past + "Booking cancelled" snackbar). Needed because the target VM may not exist yet when the producing screen finishes (documented reason in `BookingsLaunchRequest` KDoc).

### Pattern 3: Navigation operations (all exist on `Navigator`)
- Push detail: `navigator.navigate(IncomingRequestsKey)` (non-tab key pushes on the current tab stack; equal-top push ignored).
- Accept -> Confirmed: `navigator.replaceTop(BookingConfirmedKey(...))` (`Navigator.kt:88`; check requires stack size > 1).
- Decline / ALREADY_PROCESSED: post launch, then `goBack()`.
- Back to Home: `navigator.resetAndSelect(DashboardRoot, setOf(DashboardRoot))` (`Navigator.kt:103`) then post a Dashboard reload.
- Calendar / Topics / banners: `navigator.navigate(ExpertBookingsRoot)` / `navigate(TopicsRoot)` (tab keys select the tab). Selecting Bookings -> Upcoming: post a launch with tab = Upcoming.
- After cancel: `resetAndSelect(ExpertBookingsRoot, setOf(ExpertBookingsRoot, DashboardRoot))` + launch(Past, notice).
- Keys that can sit in two tab stacks carry `origin` (as `AvailabilityKey` does) so Nav3 content keys stay unique: `ExpertBookingSummaryKey(bookingId, origin)`, `ExpertCancelKey(bookingId, origin)`. `AvailabilityOrigin` is `enum class AvailabilityOrigin { Topics, Profile }` (`AvailabilityKey.kt:8`): add `Dashboard` and `Bookings`.

### Pattern 4: Tab-root slots on the shell
`HomeExpertShell` is stateless and Hilt-free so it renders under Robolectric; Hilt screens are injected through slots (`topicsRoot`, `detailEntries`). `DashboardRoot` and `ExpertBookingsRoot` are currently `ExpertTabRoot` placeholders inside the shell. Add `dashboardRoot` / `bookingsRoot` slot parameters with the placeholder default so `HomeShellsTest` (builds `HomeExpertShell(...)` directly) keeps compiling, and register the new keys in `HomeExpertScreen`'s `detailEntries`.

### Pattern 5: Expert variant of `RescheduleResponseCard`
The shared card is client-oriented: it is actionable only when `proposal.initiatedByExpert`, shows `reschedule_title_expert/client`, `reschedule_note_*`, "Awaiting expert's response", an accept dialog with payment disclosure paragraphs, and its decline dialog dismiss label is "Keep". `RescheduleProposalUi` carries `initiatedByExpert`, `showsPaymentDisclosure`, `chargesSoon`.
Recommended change: add a viewer parameter (enum `Client|Expert`) or an `actionable: Boolean` + string-set selector to the card, keep `RESCHEDULE_CARD_TAG/ACCEPT/DECLINE/DECLINE_REASON` tags and the existing `RescheduleResponseCardTest` green (it asserts client behavior). Expert strings (new): title "Client proposed a new time" / "You proposed a new time", note label "Client's message" / "Your message", awaiting "Awaiting client's response", accept message "The session will move to the new time your client proposed.", decline keep label "Keep session as-is". Expert has `showsPaymentDisclosure = false`, `chargesSoon = false`. The in-flight state (`RescheduleUiState`, `RescheduleSubmitting`, `RescheduleResponseController`) is reusable verbatim, but `RescheduleResponder` is client-only (calls `ClientBookingsRepository` and retries with a saved card): write an `ExpertRescheduleResponder` (no card retry) and feed the controller through a small interface, or give the controller an `ExpertRescheduleFailure`-aware responder. The expert `RescheduleDialog.Error` copy path (`AuthErrorDialog`, title "Couldn't update booking") is reusable.

### Anti-Patterns to Avoid
- **Calling `GET expert/bookings/{id}`:** does not exist (C1).
- **Treating `status` as a string on expert DTOs:** it is an object (C6).
- **Showing gross as earnings** when `net_amount == null` (`fee_source == "unavailable"`); and treating `0` as missing (zero commission is real, `net == gross`). Branch on `fee_source`, check `== null`, never truthiness.
- **Dividing money by 100 yourself:** use `Money.format` (minor) or `Money.formatMajor` (major).
- **Reusing `toRescheduleFailure()` for the expert** (C9).
- **Optimistic updates** for accept/decline/cancel/reschedule: all are server-authoritative; reload after any response.
- **`resolveActivity` for the calendar intent** (no `<queries>`).
- **Logging** the onboarding URL, reason text, client message, or `Authorization` header.
- **Hand-writing nav result passing** for snackbar/reload flags: use the singleton launch store.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Join button + enablement | New button/clock | `JoinCallButton`, `isWithinJoinWindow`, `isStartingNow`, `JoinClock.now` (30 s), `CallStarter.startCall(CallStartRequest(bookingId, peerId=client.id, peerName, peerAvatarUrl, topic, scheduledStart, scheduledEnd, meetingType))` | Role-agnostic since Phase 9; server participant check covers the expert (`call.controller.js:59`). |
| Today's sessions / time ranges | New filters | `todaysSessions` pattern (`JOINABLE_BOOKING_STATUSES = confirmed,in_progress,pending_reschedule`), `formatSessionTimeRange`, `rememberDeviceTimeFormat` | Already unit-tested; the existing `todaysSessions` is typed to `ClientBookingItem`: generalize with a small interface/lambda, or duplicate the 4-line filter over the expert item. |
| Currency formatting | `NumberFormat` ad hoc | `Money.format` / `Money.formatMajor` | JPY/KWD digits, locale. iOS `currencyLabel` divides by 100 always: do not port that. |
| Status badges | New pills | `BookingStatusBadge`, `pastStatusDisplay`, `detailStatusDisplay`, `summaryStatusDisplay` | Take raw status code strings. |
| Two-step cancel UI | New screens | `CancellationReasonStep` / `CancellationConfirmStep` / `ClientCancellationScreen` shapes; `cancelReason(label, comment)`, `capCancelComment` (450), `CANCEL_REASON_MAX_CHARS` (500) | The merge/cap logic is generic over a label; the client enum `CancellationReason` has client labels and a default selection, so add an `ExpertCancellationReason` enum (4 labels, no default) rather than editing it. |
| Error dialog | New dialog | `AuthErrorDialog(message, onDismiss, title)` | Already used by Phase 8 reschedule dialogs. |
| Guard dialogs | New dialogs | `CallGuard.attempt/attemptModifyBooking` + root overlay | Add enum entries + strings only. `CallDialogs.kt:45-49` maps every `RestrictedAction` in an exhaustive `when`: adding values without a branch breaks the build. |
| Custom Tab | `Intent.ACTION_VIEW` | `rememberCustomTabLauncher { }` (https/http only allow-list, `isAllowedCustomTabUri`) | Already handles missing provider/fallback. |
| Pull to refresh, skeleton, empty/error | Custom | `PullToRefreshBox`, `SkeletonBlock/Group`, `EmptyState`, `SectionError`, `SectionHeader` | Existing. |
| Fake API in tests | New mocks | `FakeClickMeApi` handler properties + call counters, `FakeCallGuard`, `FakeCallStarter`, `FakeRealtimeConnection`, `MutableClock`, `MainDispatcherRule` | Add handlers for the new endpoints. |

**Key insight:** the genuinely new code is the data layer (DTOs/repos), the five expert screens' composables, the expert reschedule mapper, the expert refund/cancel enums, the calendar intent builder, and the Dashboard financial logic. Everything else is parameterizing existing pieces.

## Common Pitfalls

### Pitfall 1: Wrong detail URL
**What goes wrong:** `GET expert/bookings/{id}` returns 404; Summary and Cancel load forever/fail.
**Why:** CONTEXT copied the shorthand from the doc table; the route is `/details`.
**How to avoid:** C1. Add a MockWebServer test asserting the request path ends with `/expert/bookings/abc/details`.
**Warning signs:** `404 NOT_FOUND` on every Summary open in dev.

### Pitfall 2: Expired request detected from the wrong source
**What goes wrong:** the decision screen shows Accept/Decline for an expired request, or never shows the expired banner.
**Why:** the detail response has no status/expiry (C3).
**How to avoid:** carry `isExpired` in the key from the list item (`status == "expired" || expiresAt <= now`). Also hide the footer when expired, but rely on `409 ALREADY_PROCESSED` for the race (accept does not check `expires_at`, only `status = pending_approval`).

### Pitfall 3: Strict backend schemas reject extra or missing keys
**What goes wrong:** `422` from `.strict()` schemas.
**How to avoid:** Moshi omits nulls, so `AcceptRequestBody(message=null)` serializes `{}` (required: a body-less POST fails validation). Decline always sends both keys (fallback `reason_text` = the reason label). Expert accept-reschedule sends `AcceptRescheduleRequest(null)` -> `{}`; decline-reschedule `DeclineRescheduleRequest(reason?)`. Connect onboard/refresh-link have no schema: no body needed. Wire tests (MockWebServer) must assert the exact JSON, as `ExpertApiTest` does.

### Pitfall 4: Join-window drift between client and server
**What goes wrong:** button enabled but `POST /join` returns `403 SESSION_NOT_ACTIVE`, or the reverse.
**Why:** client rule ±3600 s (Phase 9) vs server `[start-120min, end+120min]` (`call.controller.js:16-17`); the server flag `actions.can_join` uses 5 min before start (`expertBooking.controller.js:158-162`).
**How to avoid:** ignore `actions.can_join` (Phase 9 D-02), use `isWithinJoinWindow(status.code, start, end, now)`; the Phase 9 join-error dialog already handles a server refusal.

### Pitfall 5: Expert cancel preview promises a refund policy that does not apply
See C7. Compute `PaymentState` from `consultation_fee.amount`; never print "won't be refunded". The post-cancel notice should come from the response: `refund_amount > 0 && currency != null` -> refund notice, else "Booking cancelled" (same rule as `ClientCancellationViewModel`).

### Pitfall 6: Connect state machine edge cases
**What goes wrong:** onboarding card flickers, shows after completion, or mints the wrong link type.
**Rules (iOS `fetchPayoutSummary` + `hasIncompleteConnectAccount`):** fetch status first. Status 200: `required = !charges_enabled || currently_due.isNotEmpty()`; use `refresh-link` when status loaded and `required`, `onboard` when status is unavailable (404 = no account yet) or the summary 400. Status unavailable: `required = summary returned HTTP 400`; if status also failed without a 400 (network), required is unknown: show no card and "—" (UI-SPEC "Unknown status"). Re-fetch on `ON_RESUME` so returning from the Custom Tab updates the card. The URL is single-use (~5 min): mint on tap, never cache.
**Note:** `charges_enabled` true with a `400` from the summary (balance not initialized) is a real case the iOS comment documents; status stays authoritative.

### Pitfall 7: Quiet reload clobbering in-flight state
Same Phase 8 rule: quiet reloads never set Loading; `loadJob?.cancel()` on restart gives latest-wins on bursts of `booking.update`; a failed quiet reload keeps previous data. Copy `MyBookingsViewModel` exactly rather than re-deriving.

### Pitfall 8: Entry-scoped ViewModels and `replaceTop`
`replaceTop` clears the removed entry's ViewModel store by design (`Navigator.kt` KDoc, "RESEARCH Pitfall 11"). The Accept coroutine runs in the decision VM; it must emit its success event before the screen is replaced, and the Confirmed key must carry everything it shows (no fetch). `Instant` is not `@Serializable` by default in the NavKey: carry epoch millis `Long`s and plain strings (`startMillis`, `endMillis`, `clientName`, `avatarUrl`, `topic`, `meetingType`, `clientMessage`). All of these come from the already-loaded detail (`session.start_time/end_time/topic/meeting_type/client_message`, `client.name`), not from the accept response (which has none of them).

### Pitfall 9: Calendar intent robustness
Times must be UTC epoch millis (`Instant.toEpochMilli()`); do not pass a time zone. Title/description are plain strings from untrusted users (client name, message): fine as extras, never as URIs. Wrap `startActivity` in `try/catch (ActivityNotFoundException)`; also guard `SecurityException`-free (none expected). Add `FLAG_ACTIVITY_NEW_TASK` only if the context is not an Activity (use the same `findActivity()` idea as `CustomTabLauncher`).

### Pitfall 10: `RestrictedAction` additions
Adding `SetUpPayouts`/`RespondToReschedule` requires: enum entries (`CallDialog.kt:10-16`), `CallDialogs.kt:45-49` branches, strings in `strings_call.xml` (existing `call_action_*` at lines 98-102). `FakeCallGuard` records attempts: assert them in VM tests.

### Pitfall 11: Free vs paid detection on expert endpoints
`amount_paid_minor_units` null -> `consultation_fee.amount == 0` and `earnings_breakdown.gross_amount == null` (list: `earnings_breakdown.gross_amount = null`). Free requests: the UI-SPEC says render "Free" and omit breakdown rows; branch on `gross_amount == null || == 0.0` for free and `fee_source == "unavailable"` for degraded paid (gross present, net null).

## Code Examples

### Calendar insert (EXP-07)
```kotlin
// Source: https://developer.android.com/identity/providers/calendar-provider (insert via intent; no WRITE_CALENDAR needed)
fun buildCalendarInsertIntent(title: String, startMillis: Long, endMillis: Long, description: String): Intent =
    Intent(Intent.ACTION_INSERT)
        .setData(CalendarContract.Events.CONTENT_URI)
        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
        .putExtra(CalendarContract.Events.TITLE, title)
        .putExtra(CalendarContract.Events.DESCRIPTION, description)

fun Context.launchCalendarInsert(intent: Intent, onNoHandler: () -> Unit) {
    if (findActivity() == null) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try { startActivity(intent) } catch (_: ActivityNotFoundException) { onNoHandler() }
}
```
Keep the intent builder a pure function (unit-testable under Robolectric by asserting `action`, `data`, extras, with no location extra).

### Decline request body (D-01/D-02)
```kotlin
// Source: backend booking.schema.js:158-161 (both fields required); D-02 fallback
enum class DeclineReason(val wire: String, @StringRes val label: Int) {
    ScheduleConflict("CONFLICT", R.string.decline_reason_conflict),
    OutsideExpertise("OUT_OF_SCOPE", R.string.decline_reason_out_of_scope),
    NotAvailable("UNAVAILABLE", R.string.decline_reason_unavailable),
    Other("OTHER", R.string.decline_reason_other),
}
@JsonClass(generateAdapter = true)
data class DeclineRequestBody(
    @Json(name = "reason_code") val reasonCode: String,
    @Json(name = "reason_text") val reasonText: String,
)
// reasonText = note.trim().ifEmpty { labelOf(reason) }.take(1000)  // capNotes() avoids splitting a surrogate pair
```

### Expert reschedule failure mapper (C9), shape follows `toRescheduleFailure`
```kotlin
// Source: expertBooking.controller.js:748-783,761-763,767-771,818-822; utils/errors.js:99-100
fun ApiException.toExpertRescheduleFailure(): RescheduleFailure =
    when (this) {
        is ApiException.Http -> when {
            code == ErrorCode.AccountSuspended -> RescheduleFailure.Silent
            code == ErrorCode.SlotUnavailable -> Show(Res(R.string.expert_reschedule_error_slot_unavailable), reload = true)
            code == ErrorCode.Forbidden || status == 403 -> Show(Res(R.string.reschedule_error_forbidden), reload = true)
            code == ErrorCode.InvalidStatus -> Show(Res(R.string.reschedule_error_not_pending), reload = true)
            code in setOf(ErrorCode.ValidationError, ErrorCode.ClientUnavailable, ErrorCode.RescheduleOutsideAuthWindow) ->
                Show(serverTextOrUnknown(), reload = true)
            code == ErrorCode.NotFound || status == 404 -> Show(Res(R.string.reschedule_error_not_found), pop = true)
            else -> Show(serverTextOrUnknown())
        }
        is ApiException.Network -> Show(Res(R.string.login_error_network))
        is ApiException.Decoding -> Show(Res(R.string.login_error_unknown))
    }
```
(`Show`, `Res`, `serverTextOrUnknown` are the existing `RescheduleFailure.Show`, `FieldError.Res`, private helper in `RescheduleErrors.kt`; make the helper `internal`.)

### Expert refund preview (D-16 encoded, fed `Held` today)
```kotlin
sealed interface ExpertRefundPreview {
    data class ClientFullRefund(val amountMinor: Long, val currency: String) : ExpertRefundPreview
    data object ClientNoRefundWithin24h : ExpertRefundPreview   // dormant until the API exposes payment_status
    data object HoldReleased : ExpertRefundPreview
    data object PolicyFallback : ExpertRefundPreview
    data object None : ExpertRefundPreview
}
fun expertRefundPreview(paid: Boolean, payment: PaymentState, amountMinor: Long?, currency: String?, start: Instant, now: Instant) = when {
    !paid -> ExpertRefundPreview.None
    payment == PaymentState.Held -> ExpertRefundPreview.HoldReleased   // today's only paid case
    amountMinor == null || currency == null -> ExpertRefundPreview.PolicyFallback
    Duration.between(now, start) > REFUND_FULL_WINDOW -> ExpertRefundPreview.ClientFullRefund(amountMinor, currency)
    else -> ExpertRefundPreview.ClientNoRefundWithin24h
}
```
Reuse `REFUND_FULL_WINDOW` from `RefundPreview.kt`.

### Earnings breakdown rendering rule
```kotlin
// Source: docs/ios/EXPERT-EARNINGS-BREAKDOWN.md §3; utils/earnings.js
when (b?.feeSource) {
    "booking" -> Receipt(net = b.netAmount!!, estimated = false)       // 0 net/fee is real
    "expert_default" -> Receipt(net = b.netAmount!!, estimated = true)  // "≈" + caption
    else -> PricePending(sessionPrice)                                  // null breakdown or "unavailable"; never gross under "earnings"
}
// Guard with `?.let`/== null, not `?: 0.0`. Money.formatMajor(amount, b.currency).
```

## Runtime State Inventory

Not applicable: this is a greenfield feature phase, not a rename/refactor/migration. (`Step 2.5` skipped.)

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Capture on expert accept | Authorize at booking, capture after session completes (~15 min after end) | Backend Phase 27 | Accept moves no money; cancel voids holds (C7). |
| Legacy gross `earnings` | `earnings_breakdown` with `fee_source` branching | Backend 260811-kgj / 261015-luj | Show net; branch on `fee_source`. |
| Hard auth-window gating of reschedule | Conversion to charge-at-T-24h; `authorization_expires_at` informational | Backend Phase 27.1 | Expert accept has no payment step; only the rare `RESCHEDULE_OUTSIDE_AUTH_WINDOW` backstop. |
| Self-service withdraw | Retired (`410 ENDPOINT_RETIRED`); admin transfers | quick 260820-lh4 | Never call `/expert/payouts/withdraw`; no withdraw UI (Payouts is Phase 14). |

**Deprecated/outdated:** `can_withdraw` / `min_withdrawable_amount` removed from payouts summary: do not declare them (declare only fields that exist; Moshi ignores unknown keys).

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | PostgREST returns the weekly RPC `bigint` columns as JSON numbers (so `Long` decoding works). Supported by iOS declaring `Int` fields in production code, but not exercised from Android. | Money units | Decode failure on the weekly card (fails soft to "—"). Cover with a `ResponseDecodingTest`/MockWebServer test using a numeric body; if the server ever sends strings, add a lenient adapter. |
| A2 | The in-flight Stripe PI for a cancellable booking is always `requires_capture`, so expert cancel always voids. Derived from docs + code paths; the code falls back to `succeeded` for non-pending bookings only if `retrievePI` throws (`expertBooking.controller.js:1134-1136`), which would apply the 24h refund rule. | C7 | Rare: a Stripe retrieve outage would make a late expert cancel refund 0. The preview stays "hold released"; the cancel response notice (server-authoritative) is the safety net. |
| A3 | Backend `join` window and the Phase 9 client rule disagreement (±60 vs ±120 min) is acceptable UX; no change needed in Phase 11. | Pitfall 4 | Minor: a few minutes where Join is disabled but the server would allow it. |
| A4 | Rendering Stripe onboarding only over `https` is sufficient (existing `isAllowedCustomTabUri` allows `http` too); host pinning to `connect.stripe.com` was not verified. | Security | Low: URL comes from the authenticated backend. Optionally require `https`. |
| A5 | Custom Tab return does not re-trigger anything except `ON_RESUME` (Phase 14 deep links are deferred), so the card updates on the next resume. | Pitfall 6 | The card may show stale "Complete payout setup" until the Stripe status propagates; acceptable per CONTEXT. |
| A6 | `ExpertBookingsRoot` empty-state "Update Availability" (`AvailabilityOrigin.Bookings`, UI-SPEC A3) needs a new enum value; adding it is harmless. | Pattern 3 | None. |

## Open Questions

1. **D-16 premise vs backend behavior (user confirmation needed).**
   - What we know: the table's "Captured" rows cannot occur for cancellable bookings under the current capture-after-completion model, and the detail response exposes no payment field (C7). The "backend-owner note: a late expert cancel penalizes the client" is not supported by the code for holds.
   - What's unclear: whether the user wants the dormant rows kept (recommended: yes, as a pure function) and whether to still file the backend-owner note.
   - Recommendation: implement per C7 (paid -> hold released; free -> none), keep the 24h logic encoded but unreachable, drop or soften the backend note. Ask the user to confirm during planning; do not block.

2. **Backend `message` on accept is discarded** (C10). Recommendation: send it anyway, log a backend note and keep the UI (D-08).

3. **Expired requests are absent from the default list.** The default query returns only `pending_approval`; `status=expired` rows (cron-flipped) are never fetched, so the "Expired" badge appears only for rows whose `expires_at` has passed but whose cron flip has not run yet. Recommendation: keep iOS behavior; do not add a second `?status=expired` call (not in scope), but still implement the expired UI (it is reachable for a short window and via `isExpired` handoff).

4. **Page size 50 vs `limit`:** `/expert/bookings` honours `limit` up to 100 (fine). `/expert/booking-requests` ignores it (all pending returned). No action.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| JDK (Android Studio JBR) | Gradle build and unit tests | ✓ (per project env note) | JBR in `/Applications/Android Studio.app/Contents/jbr/Contents/Home` | — |
| Gradle wrapper | `./gradlew :app:testDevDebugUnitTest` | ✓ | repo wrapper | — |
| Backend repo (`backend-reference/`) | contract verification | ✓ | read-only symlink | — |
| iOS repo (`ios-reference/`) | behavior reference | ✓ | read-only symlink | — |
| Samsung SM-A075F + iOS second account | manual checks (calendar insert, Custom Tab, two-party Join) | not exercised | — | Deferred per project preference (log as open items) |
| External search/docs MCP providers | research | WebFetch used for the Calendar docs | — | — |

**Missing dependencies with no fallback:** none.
**Missing dependencies with fallback:** device/iOS counterpart: manual checks are logged, not blocking (memory: manual testing deferred).

## Validation Architecture

`workflow.nyquist_validation` is absent from `.planning/config.json` (file only has `workflow._auto_chain_active`), so validation is enabled.

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit4 + Robolectric + Compose UI test (`createComposeRule`) + `kotlinx-coroutines-test` + MockWebServer3 |
| Config file | `app/src/test/resources/robolectric.properties`; deps in `app/build.gradle.kts` |
| Quick run command | `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDevDebugUnitTest --tests "com.uptrendinvestments.clickmeapp.screens.expertmode.*"` |
| Full suite command | `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDevDebugUnitTest` (plus `bash scripts/check-design-rules.sh`, ktlint, lint as in earlier phases) |

Never run `gradlew --stop`.

### Phase Requirements -> Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| EXP-05 | Dashboard VM: counts (requests = list size), client-initiated reschedule count only, today filter + sort, soft-fail of earnings/connect/payout, hard-fail on bookings/requests, resume re-fetch, onboarding required/unknown logic, mint = onboard vs refresh-link, repeat tap ignored, guard attempt `SetUpPayouts` | unit (VM + FakeClickMeApi) | `--tests "*ExpertDashboardViewModelTest"` | ❌ Wave 0 |
| EXP-05 | Weekly/connect/payout wire decode (minor-unit `Long`, `next_payout_date` null, `currently_due`) | unit (MockWebServer) | `--tests "*ExpertFinancialApiTest"` | ❌ Wave 0 |
| EXP-05 | Dashboard screen states: skeleton, error+Retry, banners, empty sessions, tools grid targets, placeholder snackbars | Robolectric compose | `--tests "*ExpertDashboardScreenTest"` | ❌ Wave 0 |
| EXP-06 | Requests VM: sort (soonest expiry first, expired last), expired = status or `expiresAt<=now`, quiet reload, launch-request consume (snackbar once) | unit | `--tests "*IncomingRequestsViewModelTest"` | ❌ Wave 0 |
| EXP-06 | Decision VM: accept sends `{}` vs trimmed message, decline wire body (4 codes, label fallback, 1000 cap), in-flight single flight, `ALREADY_PROCESSED` -> pop+reload event, other errors stay, success events (replace vs pop) | unit | `--tests "*RequestDecisionViewModelTest"` | ❌ Wave 0 |
| EXP-06 | Earnings section branches (`booking`, `expert_default`, `unavailable`/null, zero commission, free) and money conversion (JPY) | unit (pure) + compose | `--tests "*RequestEarningsSectionTest"` | ❌ Wave 0 |
| EXP-06 | Wire JSON for accept/decline (strict schema), list/detail decode incl. missing pagination | unit (MockWebServer) | `--tests "*BookingRequestsApiTest"` | ❌ Wave 0 |
| EXP-07 | Calendar intent builder (action, data URI, begin/end millis, title `ClickMe · {topic} with {client}`, description with/without message, no location) and no-handler path | Robolectric unit | `--tests "*CalendarIntentTest"` | ❌ Wave 0 |
| EXP-07 | Confirmed screen: Back to Home action, buttons enabled after animation, snackbar on no handler | compose | `--tests "*BookingConfirmedScreenTest"` | ❌ Wave 0 |
| EXP-08 | Bucket builder: excludes `pending_approval`, requires `endTime>now`, merges rejoinable completed, Past labels, de-dupe | unit (pure) | `--tests "*ExpertBookingBucketsTest"` | ❌ Wave 0 |
| EXP-08 | Bookings VM: both buckets loaded together, tab restore, quiet reload on `BookingUpdate`, `join()` builds `CallStartRequest` with client as peer | unit | `--tests "*ExpertBookingsViewModelTest"` | ❌ Wave 0 |
| EXP-08 | Join rule reuse (no change) | unit | `--tests "*JoinWindowTest"` | ✅ exists |
| EXP-09 | Summary VM: loads `/details`, reschedule accept/decline guarded by `attemptModifyBooking(RespondToReschedule)`, reload-after, join visible set, Cancel visibility via `can_cancel` | unit | `--tests "*ExpertBookingSummaryViewModelTest"` | ❌ Wave 0 |
| EXP-09 | Expert reschedule mapper (each code in C9, incl. `SLOT_UNAVAILABLE` copy + reload) | unit (pure) | `--tests "*ExpertRescheduleErrorsTest"` | ❌ Wave 0 |
| EXP-09 | Card expert variant (actionable iff `initiated_by == client`, copy, no payment disclosure) without regressing client card | compose | `--tests "*RescheduleResponseCardTest"` | ✅ exists (extend) |
| EXP-09 | Cancel VM: step/reason/comment saved state, `"<label>: <comment>"` cap 500, guard `CancelBooking`, `ALREADY_CANCELLED` = success, `INVALID_STATUS`/404 -> return, refund notice from response, preview via `expertRefundPreview` | unit | `--tests "*ExpertCancellationViewModelTest"`, `"*ExpertRefundPreviewTest"` | ❌ Wave 0 |
| EXP-05/08/09 | URL constants (incl. `/details`) and nav keys/origin routing (Dashboard/Bookings origins, replaceTop, resetAndSelect) | unit | `--tests "*ApiRequestUrlsTest"`, `"*ExpertNavigationTest"` | ✅ / ❌ Wave 0 |
| All | Shell still builds with new tab-root slots | Robolectric | `--tests "*HomeShellsTest"` | ✅ exists (extend) |

### Sampling Rate
- **Per task commit:** the single new/changed test class (`--tests "*<Name>"`), under 30 s each.
- **Per wave merge:** `--tests "com.uptrendinvestments.clickmeapp.screens.expertmode.*" --tests "*network.*" --tests "*components.*"`.
- **Phase gate:** full `:app:testDevDebugUnitTest` (also the `prod` flavor as in earlier phases), ktlint, lint, `scripts/check-design-rules.sh` green before `/gsd-verify-work`.

### Wave 0 Gaps
- [ ] `FakeClickMeApi` handlers + call counters + recorded-request lists for: booking-requests list/detail/accept/decline, expert bookings list/details/cancel/accept-reschedule/decline-reschedule, weekly earnings, connect status/onboard/refresh-link, payout summary (file `app/src/test/.../testing/FakeClickMeApi.kt`; it must implement every new `ClickMeApi` method or the test source set will not compile).
- [ ] `testing/ExpertBookingFixtures.kt` (expert list item, detail, request item/detail, breakdown variants), modelled on `BookingManagementFixtures.kt`.
- [ ] Add fakes' counterparts: none new for guard/starter (`FakeCallGuard`, `FakeCallStarter` exist).
- [ ] Update `CallDialogs`-related exhaustive `when` tests if any enumerate `RestrictedAction` values.
- [ ] Framework install: none.

Manual checks (deferred, log in STATE, project preference): accept/decline against an iOS client, Confirmed + calendar insert on the SM-A075F, expired request, Custom Tab onboarding round trip, Join from the three sites with the iOS client, reschedule accept/decline and `SLOT_UNAVAILABLE` (needs two back-to-back bookings; the 30-min buffer), expert cancel, 200% font scale on the UI-SPEC screen list.

## Security Domain

`security_enforcement` is not set in config (absent = enabled).

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no (no new auth) | existing `TokenAuthenticator` |
| V3 Session Management | no | existing token store |
| V4 Access Control | yes | Server enforces expert ownership (`.eq('expert_id', req.user.id)` everywhere) and counterparty-only reschedule (403 `FORBIDDEN`). UI gates (`can_cancel`, join window, hidden footer on expired) are convenience, never authorization. IDs in nav keys are not secrets. |
| V5 Input Validation | yes | Client caps mirror strict zod schemas (1000/1000/500/450 UTF-16 with `capNotes`); server stays authoritative; no HTML/link rendering of untrusted client text (selectable plain `Text`, as T-08-21). |
| V6 Cryptography | no | none hand-rolled |
| V7/V8 Logging & data protection | yes | Never log the Stripe onboarding URL (single-use secret), reason text, client message, or tokens. `ApiException.message` already carries status+code only. |
| V12/V13 Files & API | yes | Custom Tab only for http(s) URLs with a host (`isAllowedCustomTabUri`); prefer https-only for the Stripe link. Calendar intent passes only plain-text extras and the fixed `Events.CONTENT_URI`. |

### Known Threat Patterns for this stack

| Pattern | STRIPE | Standard Mitigation |
|---------|--------|---------------------|
| Opening a server-supplied URL in a browser (open redirect / scheme abuse) | Tampering | Allow-list scheme (https), no `intent:`/`javascript:`; use existing launcher; do not persist the URL |
| Double-submit of money-affecting actions (accept, decline, cancel, onboard mint) | Tampering / Repudiation | Single-flight flags in the VM; server atomic status guards; idempotent reschedule responses |
| Acting on stale state (accept after client cancel) | Tampering | Treat `409 ALREADY_PROCESSED` / `INVALID_STATUS` as refresh triggers, never as success (except `ALREADY_CANCELLED`) |
| Untrusted text (client message, names) injected into UI/calendar | Tampering / Spoofing | Plain text only, no autolink; calendar extras are strings, not URIs/HTML |
| Sensitive data in logs/crash reports | Information disclosure | No reason/message/URL logging; reuse existing redaction habits |
| Call-guard bypass during live call | Tampering | `attempt` / `attemptModifyBooking` before payout setup, reschedule response, cancel |

## Sources

### Primary (HIGH confidence) - opened this session
- `backend-reference/src/controllers/bookingRequests.controller.js` (list, detail, accept, decline)
- `backend-reference/src/controllers/expertBooking.controller.js` (feed, details, accept/decline reschedule, cancel; `statusMeta`, `buildSessionMoney`, status sets)
- `backend-reference/src/schemas/booking.schema.js`, `routes/{bookingRequests,expertBooking,earnings,connect,payouts}.js`, `middleware/validate.js`, `utils/{errors,earnings,refund}.js`
- `backend-reference/src/controllers/{earnings,connect,payouts,call}.controller.js`; `supabase/migrations/20260710040000_earnings_rpc.sql`
- `backend-reference/docs/ios/{STRIPE-INTEGRATION,EXPERT-EARNINGS-BREAKDOWN,SESSION-BUFFER,RESCHEDULE-PAYMENT-CONVERSION}.md`
- iOS: `Expert Dashboard/ExpertDashboardViewModel.swift`, `Request decision/RequestDecisionViewModel.swift`, `Incoming Requests/IncomingRequestsViewModel.swift`, `Upcoming Bookings/UpcomingBookingsViewModel.swift`, `Booking Summary/ExpertBookingSummaryViewModel.swift`, `Model/Responses/{ExpertBookingDetail,Children/ExpertBookingItem,BookingRequestDetail,Children/BookingRequestItem,WeeklyEarningsData,ConnectStatusData,PayoutSummaryData,ConnectOnboardData,Children/EarningsBreakdown}.swift`, `Network/{ClickMeAPI+Payouts,APIRequestURLs}.swift`
- Android: `HomeExpertScreen.kt`, `HomeExpertTab.kt`, `AvailabilityKey.kt`, `Navigator.kt`, `MyBookingsViewModel.kt`, `BookingBuckets.kt`, `BookingsLaunchRequest.kt`, `ClientBookingsRepository.kt`, `RescheduleResponse.kt`, `RescheduleErrors.kt`, `RescheduleUiState.kt`, `RescheduleDisclosure.kt`, `RescheduleResponseCard.kt`, `BookingStatusDisplay.kt`, `TodaysSessions.kt`, `ClientCancellationViewModel.kt`, `CancellationReason.kt`, `RefundPreview.kt`, `JoinWindow.kt`, `JoinCallButton.kt`, `CallGuard.kt`, `CallDialog.kt`, `CallSession.kt`, `CustomTabLauncher.kt`, `ErrorCode.kt`, `ApiException.kt`, `ApiRequestUrls.kt`, `ClickMeApi.kt`, `Money.kt`, DTOs under `model/responses/`, test harness (`FakeClickMeApi`, `FakeCallGuard`, `FakeCallStarter`, `HomeShellsTest`, `MyBookingsViewModelTest`, `ExpertApiTest`)
- Android Calendar Provider docs (insert via intent): https://developer.android.com/identity/providers/calendar-provider

### Secondary (MEDIUM confidence)
- none

### Tertiary (LOW confidence)
- none (package-legitimacy/registry tooling not needed: no new packages)

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - no new dependencies; all reuse points opened and read.
- Architecture: HIGH - every navigation operation and pattern exists in code; only the expert reschedule responder and card variant are design work.
- Backend contract: HIGH - read from controllers/schemas/routes, cross-checked against iOS models and docs; discrepancies with CONTEXT recorded as C1-C13.
- Pitfalls: HIGH for contract-derived ones, MEDIUM for device behavior (calendar app handling, Custom Tab return) which is deferred.

**Research date:** 2026-10-25
**Valid until:** 2026-11-24 (backend has shipped contract changes in quick tasks on a weekly cadence; re-check `git log backend-reference` on the files above before executing if more than two weeks pass)

## Open Question Resolutions (user, 2026-10-25)

These are locked user decisions. They override the matching CONTEXT/UI-SPEC text.

1. **D-16 refund preview: hold released / no line.** For a paid booking, show "The client's payment hold will be released." For a free booking, show no refund line. Keep the D-16 table as a pure function fed `Held` (paid) or `None` (free). The Captured rows stay dormant, but tests still cover them. Drop the backend-owner note "a late expert cancel penalizes the client".
2. **The accept message is sent anyway.** Keep the optional message field on the Accept sheet, for iOS parity. Send `message` in the accept body. Record a backend follow-up note in the plan: the backend currently discards it.
3. **Expired requests: keep the iOS behavior.** Do not fetch `status=expired`. Build the expired badge and banner UI, and pass `isExpired` from the list item through `RequestDecisionKey`.
4. **Earnings units (from the UI-SPEC probe resolution) are confirmed by research.** `/expert/earnings/weekly` returns integer minor units and `net_amount` is never null. "—" appears only when the fetch fails.

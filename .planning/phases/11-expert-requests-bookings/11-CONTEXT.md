# Phase 11: Expert Requests & Bookings - Context

**Gathered:** 2026-10-25
**Status:** Ready for planning

<domain>
## Phase Boundary

The expert side works end to end on Android:

- **Dashboard** (EXP-05): pending request count, client-initiated reschedule count, today's sessions with Join, weekly earnings, a payout onboarding card, and the Expert Tools grid.
- **Incoming Requests → Request Decision** (EXP-06): accept with an optional message, or decline with a reason code. A successful accept leads to the **Confirmed** screen.
- **Add to calendar** from Confirmed through `Intent.ACTION_INSERT` (EXP-07).
- **Expert bookings** Upcoming/Past (EXP-08), with Join inside the join window, using the same call stack as the client.
- **Expert Booking Summary** (EXP-09): accept or decline a client's reschedule (with `409 SLOT_UNAVAILABLE` handling), Join, and Cancel.

Not in this phase: starting a reschedule (DEF-1), chat/Message buttons (Phase 12), the private booking note (deferred, as in Phase 8 D-16), the Payouts screen, Connect deep-link return/refresh, and Profile Settings (Phase 14).

</domain>

<decisions>
## Implementation Decisions

Carried forward (not re-opened):
- iOS is the source of truth (DEC-ios-source-of-truth). Material 3, dark only, Nunito. Pixel parity is out of scope.
- API pattern: `ApiRequestUrls` constants, `ClickMeApi` methods, and a Hilt `@Singleton` repository using `apiCall {}`. DTOs are Moshi codegen with explicit `@Json(name=…)`.
- Money uses minor units, formatted with `util/Money.kt`. Error copy is the server `message` in a Material `AlertDialog` (Phases 6–10).
- Join follows the iOS client-side window rule (Phase 9 D-02): never for `pending_approval`; for `completed` while `endTime > now`; otherwise ±3600s of start. A live clock re-evaluates it (D-03). Join goes through `CallStarter.startCall` / `CallGuard.startCall`, reusing `JoinCallButton` and `JoinClock`. The call stack is role-agnostic.
- Booking-modifying actions are guarded with `CallGuard.attemptModifyBooking` (Phase 9 D-12/D-13).
- Starting a reschedule is DEF-1, so the Reschedule button is hidden. Message buttons stay hidden until Phase 12. The private note card stays hidden (Phase 8 D-16 precedent).

### Decline contract (EXP-06)
- **D-01: The decline picker uses the backend's 4 reason codes, not iOS's 5.** The backend `declineSchema` is `reason_code: z.enum(['CONFLICT','OUT_OF_SCOPE','UNAVAILABLE','OTHER'])`. iOS sends `OUTSIDE_EXPERTISE` / `SCHEDULE_CONFLICT` / `PRICING`, which the backend rejects with 422.
  - Labels: "Schedule conflict" → `CONFLICT`, "Outside my expertise" → `OUT_OF_SCOPE`, "Not available at that time" → `UNAVAILABLE`, "Other reason" → `OTHER`.
  - "Pricing mismatch" is dropped. Log an iOS follow-up, because iOS decline is broken against the backend.
- **D-02: `reason_text` is required by the backend (1–1000 chars), but the field stays optional in the UI.** If the expert leaves it empty, send the selected reason's label as `reason_text`. Cap input at 1000.
  - iOS sends nil when the field is empty, which also fails with 422. Log an iOS follow-up.
- **D-03: After a successful decline**, the sheet closes and the screen pops back to Incoming Requests. A brief "Request declined" snackbar shows, and the list is refreshed so the item is gone. This deviates from iOS, which stays on the screen with a "Declined" footer.

### Accept → Confirmed flow (EXP-06, EXP-07)
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

### Dashboard (EXP-05)
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

### Expert bookings, Summary & cancel (EXP-08, EXP-09)
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

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Backend contract (authoritative where it differs from iOS)
- `backend-reference/src/schemas/booking.schema.js`: `declineSchema` (4 reason codes, `reason_text` required, 1–1000), `acceptSchema` (`message` ≤1000), `cancelSchema` (`reason` ≤500, used for the expert cancel too), and the reschedule accept/decline schemas.
- `backend-reference/src/controllers/bookingRequests.controller.js`: list, detail, accept (`409 ALREADY_PROCESSED`, response `{booking_id, status, confirmed_at, meeting_link}`), and decline.
- `backend-reference/src/controllers/expertBooking.controller.js`: expert bookings list and detail, cancel (EBOOK-08, the 24h refund rule, void on `pending_approval`), and accept/decline reschedule (`SLOT_UNAVAILABLE`).
- `backend-reference/src/utils/refund.js`: the 24h binary refund formula, shared by client and expert cancel.
- `backend-reference/docs/ios/EXPERT-EARNINGS-BREAKDOWN.md`: `earnings_breakdown` (dollars), `fee_source`, and null `net_amount` handling for the Request Decision earnings section.
- `backend-reference/docs/ios/STRIPE-INTEGRATION.md`: Connect status, onboard and refresh-link, for the dashboard payout card.
- `backend-reference/docs/ios/RESCHEDULE-PAYMENT-CONVERSION.md`: context for reschedule acceptance.

### iOS source of truth
- `ios-reference/ClickMe2026/Views/Screens/Expert Mode/Expert Dashboard/ExpertDashboardView.swift` and `ExpertDashboardViewModel.swift`
- `ios-reference/ClickMe2026/Views/Screens/Expert Mode/Incoming Requests/` (the view, VM, and `Subviews/IncomingRequestCard.swift`)
- `ios-reference/ClickMe2026/Views/Screens/Expert Mode/Request decision/` (`RequestDecisionView.swift`, `RequestDecisionViewModel.swift`, and the `Subviews/` accept/decline sheets, earnings section and footer)
- `ios-reference/ClickMe2026/Views/Screens/Expert Mode/Confirmed/` (`BookingConfirmedView.swift`, and `Subviews/CalendarEventEditorSheet.swift` for the event fields)
- `ios-reference/ClickMe2026/Views/Screens/Expert Mode/Upcoming Bookings/` (the view, the VM, and the session and past cards)
- `ios-reference/ClickMe2026/Views/Screens/Expert Mode/Booking Summary/ExpertBookingSummaryView.swift` and `ExpertBookingSummaryViewModel.swift`
- `ios-reference/ClickMe2026/Views/Screens/Expert Mode/Cancellation/ExpertCancellationView.swift` and `ExpertCancellationViewModel.swift`
- `ios-reference/ClickMe2026/Views/Screens/Expert Mode/Home/HomeExpertView.swift`: expert shell routes.
- `ios-reference/ClickMe2026/Model/Responses/ExpertBookingDetail.swift`, `ExpertBookingSummary.swift`, `Children/ExpertBookingItem.swift`

### Project
- `.planning/REQUIREMENTS.md`: EXP-05 to EXP-09.
- `.planning/ROADMAP.md`: Phase 11 success criteria and the planning notes (the Phase 9 and Phase 10 hand-offs).
- `.planning/phases/08-client-bookings-cancellation/08-CONTEXT.md`: D-01–D-04 (the reschedule card), D-05–D-08 (cancel flow, refund preview), D-09/D-10 (list paging, refresh triggers).
- `.planning/phases/09-voice-call/09-CONTEXT.md`: D-01–D-04 (Join), D-12/D-13 (the guard API).
- `.planning/phases/10-expert-topics-availability/10-CONTEXT.md`: D-01 (the Dashboard Availability entry hand-off).

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `components/call/JoinCallButton.kt`, `managers/JoinWindow.kt` (`JoinClock`), and `managers/CallGuard.kt` / `CallCenter.kt` (`startCall`, `attempt`, `attemptModifyBooking`).
- `managers/CallDialog.kt` (`RestrictedAction`: `SwitchModes`, `LogOut`, `AddPaymentMethod`, `CancelBooking`, `EditAvailability`). Add values for payouts and the expert reschedule response.
- `components/RescheduleResponseCard.kt`: the Phase 8 shared reschedule card. Add an expert variant (no payment step).
- `screens/clientmode/cancellation/` (`ClientCancellationScreen.kt`, `RefundPreview.kt`, `subviews/CancellationReasonStep.kt`, `CancellationConfirmStep.kt`): the two-step cancel pattern and the refund logic.
- `screens/clientmode/mybookings/` (`BookingBuckets.kt`, `BookingStatusDisplay.kt`, `RescheduleDisclosure.kt`, `subviews/UpcomingBookingCard.kt`, `PastBookingCard.kt`): bucket and label logic and card patterns.
- `screens/clientmode/requestsent/`: the confirmation-screen pattern (check icon, headline, details card) for Confirmed.
- `components/BookingStatusBadge.kt`, `PaymentLifecycleCard.kt`, `EmptyState.kt`, `SkeletonBlock.kt`, `SectionError.kt`, `PickerSheet.kt`
- `components/web/CustomTabLauncher.kt` and `ExternalLinkLauncher.kt`: the Connect onboarding URL.
- `util/Money.kt` and `util/PriceDisplay.kt`
- `realtime/RealtimeEvents.kt` (`BOOKING_UPDATE`): quiet-reload trigger.

### Established Patterns
- The expert shell `screens/expertmode/home/HomeExpertScreen.kt`: `DashboardRoot` and `ExpertBookingsRoot` are `ExpertTabRoot` placeholders that this phase replaces. Detail entries are pushed with `BottomBar.hidden()` metadata. Each tab keeps its own Navigation 3 back stack.
- `AvailabilityKey(AvailabilityOrigin.X)`: add `Dashboard`.
- `network/ApiRequestUrls.kt` has no expert booking or booking-request constants yet. Add `expert/booking-requests[/:id[/accept|/decline]]`, `expert/bookings[/:id[/cancel|/accept-reschedule|/decline-reschedule]]`, `expert/earnings/weekly`, `expert/payouts/summary`, and `expert/connect/status|onboard|refresh-link`.

### Integration Points
- The Dashboard and Bookings tab roots in `HomeExpertScreen.kt`. Tab switching for Calendar/Topics/Bookings goes through the shell's navigator.
- The call overlay and guard dialogs are root-level (Phase 9). Expert Join uses the same `CallStartRequest`.
- The Phase 14 Payouts screen and Profile Settings replace the "coming soon" placeholders in the Tools grid.

</code_context>

<specifics>
## Specific Ideas

- Fix the iOS defects instead of copying them. Each gets an iOS follow-up:
  - decline codes and `reason_text` (D-01/D-02)
  - the orphaned Confirmed screen (D-04)
  - the calendar title using the expert's own name (D-06)
  - no Join on the Summary (D-14)
  - "full refund" always promised on an expert cancel (D-16)
- Research items:
  - Confirm `/expert/earnings/weekly` amounts are minor units, and how a null `net_amount` appears there.
  - Confirm the exact Connect endpoints and response shapes (STRIPE-INTEGRATION.md).
  - Confirm whether `GET /expert/bookings/:id` exposes `payment_status` / `amount_paid` for the D-16 refund preview.

</specifics>

<deferred>
## Deferred Ideas

- **Message buttons** on Request Decision and the expert Summary: Phase 12 (`initiateChat`).
- **Payouts screen, Connect deep-link return/refresh, Profile Settings:** Phase 14. The Tools grid shows "coming soon" placeholders until then.
- **Expert private booking note** (`ExpertBookingNoteView`): deferred, as in Phase 8 D-16.
- **Starting a reschedule (expert):** DEF-1.
- **Paging the expert bookings past page 1:** same later improvement as Phase 8 D-09.
- **Backend-owner note:** an expert cancel within 24h gives the client no refund. Policy question for the backend owner.
- **Client profile from a request ("View Profile"):** DEF-3 (Client Profile, expert side).

</deferred>

---

*Phase: 11-expert-requests-bookings*
*Context gathered: 2026-10-25*

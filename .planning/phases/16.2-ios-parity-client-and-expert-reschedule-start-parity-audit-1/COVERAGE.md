# API Coverage — ClickMe backend reschedule propose (`PATCH /client/bookings/:id/reschedule`, `PATCH /expert/bookings/:id/reschedule`), plus `GET /experts/:id/availability`, `GET /me/payment-methods` and the expert `actions.can_reschedule` flag

> Full coverage by default. Opt-outs are explicit, reasoned decisions.
> Contract: `backend-reference/src/schemas/booking.schema.js` (`rescheduleSchema`, `.strict()`, shared by both PATCH routes), `src/controllers/clientBooking.controller.js` and `src/controllers/expertBooking.controller.js` (`reschedule`, list and detail `actions`), `src/controllers/availability.controller.js` (`getClientCalendar`), `src/utils/reschedule.js`, `docs/ios/RESCHEDULE-PAYMENT-CONVERSION.md` sections 1-6, `docs/ios/STRIPE-INTEGRATION.md` section 2.5 and the error table. Plans: 16.2-01 (wire), 16.2-04 (proposers and error mapping), 16.2-06 and 16.2-08 (pickers), 16.2-10 and 16.2-11 (expert flag).

| capability | decision | reason |
|---|---|---|
| client PATCH body `proposed_start` (UTC ISO-8601 instant, seconds precision) | INTEGRATE | |
| client PATCH body `timezone` (device zone id) | INTEGRATE | |
| client PATCH body `note` (trimmed, max 500, omitted when blank) | INTEGRATE | |
| client PATCH body `stripe_payment_method_id` (one retry with a saved card) | INTEGRATE | |
| expert PATCH body `proposed_start`, `timezone`, `note` | INTEGRATE | |
| expert PATCH body `stripe_payment_method_id` | OPT-OUT | explicitly out of scope: experts are never charged or gated on a card (RESCHEDULE-PAYMENT-CONVERSION.md section 4 and 6, CONTEXT locked); the expert request type has no card field |
| PATCH response `data.booking_id`, `data.status` | INTEGRATE | |
| PATCH response top-level `message` | OPT-OUT | not needed: the origin shows the fixed "Reschedule request sent" snackbar (D-09, D-10) |
| availability query `month` (displayed month and both neighbours) | INTEGRATE | |
| availability query `exclude_booking_id` (the booking being moved) | INTEGRATE | |
| availability query `topic_id` | OPT-OUT | not needed: iOS parity, neither Make a Booking nor the pickers send it (RESEARCH Pitfall 1 and Open Question 5 record the 30-minute grid consequence) |
| availability response slots (`start_utc`, `end_utc`, `available`, `held`) | INTEGRATE | |
| availability response `expert_timezone` | INTEGRATE | |
| `GET /me/payment-methods` for the PAYMENT_METHOD_REQUIRED recovery | INTEGRATE | |
| SetupIntent or card entry inside the picker | OPT-OUT | explicitly out of scope: section 3.1 and D-06 route a missing card to the existing Payment Methods screen |
| expert list row and detail `actions.can_reschedule` | INTEGRATE | |
| expert `actions.can_message` and other undeclared action flags | OPT-OUT | not needed: messaging is already gated elsewhere (Phase 12); unknown keys are ignored |
| client booking `can_reschedule` flag | OPT-OUT | not available: the backend exposes no such flag on client endpoints; D-04's local rules are the gate |
| propose error `RESCHEDULE_OUTSIDE_AUTH_WINDOW` (fixed D-13 copy) | INTEGRATE | |
| propose error `RESCHEDULE_TOO_FAR_AHEAD` (server text, clears the slot) | INTEGRATE | |
| propose error `PAYMENT_METHOD_REQUIRED` (client recovery) | INTEGRATE | |
| propose errors `INVALID_PAYMENT_METHOD`, `VALIDATION_ERROR` | INTEGRATE | |
| propose errors `INVALID_STATUS`, `NOT_FOUND` (leave the picker) | INTEGRATE | |
| propose errors `EXPERT_UNAVAILABLE`, `SLOT_UNAVAILABLE` (clear slot, reload) | INTEGRATE | |
| propose error `ACCOUNT_SUSPENDED` and network failures | INTEGRATE | |
| accept and decline reschedule (`/accept-reschedule`, `/decline-reschedule`) | OPT-OUT | explicitly out of scope: already integrated in Phases 8 and 11 (CONTEXT phase boundary) |

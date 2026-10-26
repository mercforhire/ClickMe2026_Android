# API Coverage — ClickMe backend, expert requests / bookings / earnings / Connect (Phase 11)

> Full coverage by default. Opt-outs are explicit, reasoned decisions.
> Surface enumerated from `backend-reference/src/routes/{bookingRequests,expertBooking,earnings,connect,payouts}.js` (verified 2026-10-25, 11-RESEARCH.md "Backend Contract"), plus the Stripe-hosted onboarding link and the Android platform intents this phase uses.

| capability | decision | reason |
|---|---|---|
| GET expert/booking-requests (list) | INTEGRATE | |
| GET expert/booking-requests?status=expired | OPT-OUT | explicitly out of scope: user decision (RESEARCH Open Question Resolution 3) keeps iOS behavior and does not fetch expired requests |
| GET expert/booking-requests?sort_by=earnings or date | OPT-OUT | not needed: iOS sorts client-side by expiry; the default server sort is unused |
| GET expert/booking-requests/:id (detail) | INTEGRATE | |
| POST expert/booking-requests/:id/accept | INTEGRATE | |
| POST expert/booking-requests/:id/decline | INTEGRATE | |
| GET expert/bookings?type=upcoming/past (page 1, limit 50) | INTEGRATE | |
| GET expert/bookings paging past page 1 | OPT-OUT | not needed yet: deferred later improvement (Phase 8 D-09 precedent, CONTEXT Deferred Ideas) |
| GET expert/bookings/:id/details | INTEGRATE | |
| GET expert/bookings/:id/summary (post-session summary) | OPT-OUT | not needed: the iOS expert Summary reads /details; post-session summaries are not in EXP-05..09 |
| PUT expert/bookings/:id/takeaways | OPT-OUT | explicitly out of scope: expert notes and takeaways are deferred (Phase 8 D-16 precedent) |
| PATCH expert/bookings/:id/status | OPT-OUT | not needed: status transitions are driven by the call flow and the backend, not by an expert screen in this phase |
| PATCH expert/bookings/:id/reschedule (start a reschedule) | OPT-OUT | explicitly out of scope: DEF-1 (starting a reschedule is deferred) |
| POST expert/bookings/:id/cancel | INTEGRATE | |
| POST expert/bookings/:id/accept and /decline (legacy booking-level routes) | OPT-OUT | not needed: duplicates of the booking-requests accept/decline routes that iOS uses |
| POST expert/bookings/:id/accept-reschedule | INTEGRATE | |
| POST expert/bookings/:id/decline-reschedule | INTEGRATE | |
| GET expert/earnings/weekly (current week) | INTEGRATE | |
| GET expert/earnings/weekly?week_start= (other weeks) | OPT-OUT | not needed: the Dashboard shows the current week only (D-10) |
| GET expert/earnings/transactions | OPT-OUT | explicitly out of scope: the Payouts screen is Phase 14 |
| GET expert/connect/status | INTEGRATE | |
| POST expert/connect/onboard | INTEGRATE | |
| POST expert/connect/refresh-link | INTEGRATE | |
| clickme://connect/return and /refresh deep links | OPT-OUT | explicitly out of scope: Phase 14 (CONTEXT Deferred Ideas); the Dashboard re-fetches Connect status on resume instead |
| GET expert/payouts/summary | INTEGRATE | |
| POST expert/payouts/withdraw | OPT-OUT | not needed: retired by the backend (410 ENDPOINT_RETIRED); payouts are admin transfers |
| Stripe Connect hosted onboarding (Account Link opened in a Custom Tab) | INTEGRATE | |
| Android Calendar insert via Intent.ACTION_INSERT | INTEGRATE | |
| Android Calendar Provider writes via ContentResolver | OPT-OUT | explicitly out of scope: EXP-07 and D-06 require the intent; no calendar permission is requested |

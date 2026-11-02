# API Coverage — ClickMe backend private booking notes (`GET` / `POST /client/bookings/:id/note`, `GET` / `POST /bookings/:id/note`), the note fields on the booking payloads, and the expert's view of a client (`GET /expert/clients/:id`)

> Full coverage by default. Opt-outs are explicit, reasoned decisions.
> Contract (first-party backend, read-only): `backend-reference/src/routes/note.js`, `src/controllers/note.controller.js`, `src/schemas/booking.schema.js` (`noteSchema`), `src/routes/expertClients.js`, `src/controllers/expertClients.controller.js`, `src/controllers/clientBooking.controller.js`, `src/controllers/expertBooking.controller.js`. Plans: 16.4-01 (wire), 16.4-03 (editor), 16.4-04 and 16.4-08 (Client Profile), 16.4-05 and 16.4-06 (client surfaces), 16.4-07 and 16.4-09 (expert surfaces), 16.4-10 (chat entry, gate).

| capability | decision | reason |
|---|---|---|
| client note read `GET /client/bookings/:id/note` | INTEGRATE | |
| client note write `POST /client/bookings/:id/note` (body exactly `{"note"}`, empty string clears) | INTEGRATE | |
| expert note read `GET /bookings/:id/note` | INTEGRATE | |
| expert note write `POST /bookings/:id/note` (same body) | INTEGRATE | |
| note response `booking_id`, `note` (null when empty) | INTEGRATE | |
| note errors: uniform 404 on GET (empty editor), 404 and 422 on POST (save dialog) | INTEGRATE | |
| `private_note_preview` on `GET /client/bookings` rows | INTEGRATE | |
| `private_note_preview` on `GET /expert/bookings` rows | INTEGRATE | |
| `private_note` on `GET /client/bookings/:id` | INTEGRATE | |
| `expert_private_note` on `GET /expert/bookings/:id/details` | INTEGRATE | |
| note surfaces on Upcoming Booking Details, Today's Sessions and the Dashboard | OPT-OUT | explicitly out of scope: CONTEXT phase boundary and D-07 (iOS has none) |
| client profile `GET /expert/clients/:id`: `id`, `personal_details.full_name`, `avatar_url`, `bio` | INTEGRATE | |
| client profile `location` (`city`, `state_province`, `country`, `timezone`), `languages`, `member_since` | INTEGRATE | |
| client profile `relationship` (`total_sessions`, `first_session_date`, `last_session_date`) | INTEGRATE | |
| client profile `personal_details.first_name`, `last_name` shown separately | OPT-OUT | not needed: decoded, but the screen shows `full_name` as iOS does |
| client profile errors: 403 `NO_RELATIONSHIP` and 404 `NOT_FOUND` as specific states, others with Retry | INTEGRATE | |
| history from `GET /expert/bookings?type=upcoming` and `type=past`, page 1, limit 50, net earnings from `earnings_breakdown` | INTEGRATE | |
| history pages beyond the first 50 of each bucket | OPT-OUT | explicitly out of scope: CONTEXT D-11 and Deferred Ideas |
| history row tap into the booking | OPT-OUT | explicitly out of scope: CONTEXT D-10 and Deferred Ideas |
| `no_show_party` per-row details call to show an amount on paid `missed` rows | OPT-OUT | not needed: RESEARCH Open Question 1 and A6; the list payload lacks the field and a per-row call is not worth it |

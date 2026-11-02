# API Coverage — ClickMe backend client payment history (`GET /client/transactions`), notification preferences (`GET` / `PATCH /notifications/preferences`) and feedback (`GET /feedback/types`, `POST /feedback/submit`)

> Full coverage by default. Opt-outs are explicit, reasoned decisions.
> Contract (first-party backend, read-only): `backend-reference/docs/ios/CLIENT-TRANSACTIONS.md`, `src/routes/clientTransactions.js`, `src/routes/notificationHistory.js`, `src/routes/feedback.js`, `src/controllers/notificationPrefs.controller.js`, `src/schemas/notificationPrefs.schema.js`, `src/controllers/feedback.controller.js`, `src/schemas/feedback.schema.js`. Plans: 16.5-01 (wire), 16.5-05 and 16.5-08 (Payment History), 16.5-06 and 16.5-09 (notification preferences), 16.5-07 and 16.5-10 (Feedback), 16.5-11 (routes).

| capability | decision | reason |
|---|---|---|
| ledger `GET /client/transactions` with `page` and `limit` | INTEGRATE | |
| ledger ids and dates: `booking_id`, `session_date`, `created_at` | INTEGRATE | |
| ledger money: `amount`, `currency`, `payment_status`, `refunded_amount` | INTEGRATE | |
| ledger `topic_title`, `expert_name` | INTEGRATE | |
| ledger `pagination` (`has_more`, `current_page`, `total_pages`) | INTEGRATE | |
| ledger `pagination.total_count` shown as a header count | OPT-OUT | not needed: decoded, but iOS never shows a total (RESEARCH Disagreements, "no header count on iOS") |
| ledger `expert_avatar_url` shown on a row | OPT-OUT | not needed: decoded, but the iOS row and UI-SPEC show no avatar |
| ledger `paid_at` shown on a row | OPT-OUT | not needed: decoded, but the iOS row and UI-SPEC date the row from the session or creation date |
| ledger `refunded_at` | OPT-OUT | always null by contract (CLIENT-TRANSACTIONS.md section 5); not declared on the DTO |
| ledger 500 past the end as end of list, on page 2 or later | INTEGRATE | |
| notification preferences read `GET /notifications/preferences` | INTEGRATE | |
| preferences write `PATCH /notifications/preferences`, five keys per row | INTEGRATE | |
| notification categories `booking`, `session`, `message`, `review` | INTEGRATE | |
| notification categories `payout`, `account` | OPT-OUT | explicitly out of scope: not shown and never sent, as on iOS (CONTEXT carried rules) |
| notification sub-category `booking/payment_actions` | OPT-OUT | explicitly out of scope: neither platform renders it; it stays on (RESEARCH disagreement 8) |
| separate toggles for `push_enabled`, `email_enabled`, `in_app_enabled` | OPT-OUT | explicitly out of scope: one master toggle per row sets all three, as on iOS (CONTEXT carried rules) |
| quiet hours | OPT-OUT | explicitly out of scope: the backend stores no such field and iOS never shows the card (D-01) |
| feedback types `GET /feedback/types`: `id`, `label` | INTEGRATE | |
| feedback type `description` shown in the picker | OPT-OUT | not needed: decoded, but UI-SPEC A9 shows the label only |
| feedback submit `POST /feedback/submit`: `type_id`, `details`, optional `email` | INTEGRATE | |
| feedback submit `metadata` | OPT-OUT | not needed: optional on the server and iOS always sends nil |
| feedback `submission_id`, `submitted_at` shown to the user | OPT-OUT | not needed: decoded, but the thanks dialog shows fixed copy (D-06) |
| feedback errors: 422 mapped to its field, 429 and others to the dialog | INTEGRATE | |

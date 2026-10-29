# API Coverage — ClickMe account, profile, help, payouts and tax endpoints, and Stripe Connect hosted onboarding (Phase 14)

> Full coverage by default. Opt-outs are explicit, reasoned decisions.
> Surface enumerated from `backend-reference/openapi/clickme-api.yaml` (`/user/password`, `/user/account`, `/user/account/delete-request`, `/help/*`, `/user/profile`, `/expert/profile`, `/expert/earnings/*`, `/expert/payouts/*`, `/expert/connect/*`, `/expert/tax-profile`), `backend-reference/src/app.js` (the `/connect/return` and `/connect/refresh` bridge), `docs/ios/STRIPE-INTEGRATION.md` sections 3 and 5, `docs/ios/EXPERT-EARNINGS-TRANSACTIONS.md`, and the iOS `ClickMeAPI+Account/+Support/+UserProfile/+Payouts/+TaxProfile` files (read 2026-10-29).

| capability | decision | reason |
|---|---|---|
| POST user/account/delete-request (password -> deletion token) | INTEGRATE | |
| DELETE user/account (token, reason, feedback, confirm flag) | INTEGRATE | |
| PATCH user/password | INTEGRATE | |
| GET user/profile (refresh after edits) | INTEGRATE | |
| PATCH user/profile basic, location, professional groups | INTEGRATE | |
| PATCH user/profile languages group (ids and custom) | INTEGRATE | |
| PATCH user/profile basic.email | OPT-OUT | not needed: email is read-only and the server answers 403 |
| PATCH user/profile location timezone_id and auto_detect | OPT-OUT | not needed: the timezone is edited on Availability (Phase 10), not in profile settings |
| POST user/profile/avatar | INTEGRATE | |
| DELETE user/profile/avatar | OPT-OUT | not needed: settings only replaces the photo and experts must keep one (UI-SPEC Screen 2) |
| GET expert/profile (settings load, meeting link) | INTEGRATE | |
| PATCH expert/profile professional_details.meeting_url | INTEGRATE | |
| PATCH expert/profile expertise tags (ExpertiseEditorSheet) | INTEGRATE | |
| PATCH expert/profile personal, location, languages, education, rate | OPT-OUT | not needed: shared fields are saved through PATCH user/profile (iOS parity) and education has no UI |
| GET help/faqs | INTEGRATE | |
| GET help/faqs search | OPT-OUT | not needed: iOS Help has no search and the FAQ is short |
| GET help/articles by id | OPT-OUT | not needed: the list returns snippets and there is no article screen (UI-SPEC Screen 5) |
| POST support tickets | OPT-OUT | explicitly out of scope: PROF-05 asks for a mailto support link |
| GET expert/payouts/summary (with payout_method) | INTEGRATE | |
| GET expert/connect/status | INTEGRATE | |
| POST expert/connect/onboard | INTEGRATE | |
| POST expert/connect/refresh-link | INTEGRATE | |
| Stripe hosted Account Link in a Custom Tab | INTEGRATE | |
| Backend /connect/return and /connect/refresh bridge to clickme:// | INTEGRATE | |
| GET expert/earnings/transactions (page, limit) | INTEGRATE | |
| GET expert/earnings/weekly | INTEGRATE | |
| GET expert/bookings upcoming (held-session count) | INTEGRATE | |
| POST expert/payouts/withdraw | OPT-OUT | explicitly out of scope: retired server-side (410 ENDPOINT_RETIRED); payouts are automatic |
| GET expert/tax-profile | INTEGRATE | |
| POST expert/tax-profile (TIN optional, tin_required answer) | INTEGRATE | |
| Stripe Express dashboard login link | OPT-OUT | not needed: no backend endpoint exists and iOS has no such entry |
| Pause or resume account | OPT-OUT | not needed yet: no backend endpoint; requested in 14-BACKEND-REQUEST.md and backlogged (D-06) |
| Saved experts, payment history, notification preferences, feedback | OPT-OUT | explicitly out of scope: hidden hub rows deferred by CONTEXT D-01 |

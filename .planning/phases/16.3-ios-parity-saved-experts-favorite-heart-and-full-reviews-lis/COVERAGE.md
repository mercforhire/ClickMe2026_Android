# API Coverage — ClickMe backend favorites (`GET /client/favorites`, `PUT` / `DELETE /client/favorites/:expertId`), `is_favorite` on `GET /experts/:id/details`, and the public reviews list `GET /experts/:id/reviews`

> Full coverage by default. Opt-outs are explicit, reasoned decisions.
> Contract (first-party backend, read-only): `backend-reference/src/routes/favorites.js`, `src/controllers/favorites.controller.js`, `src/schemas/favorites.schema.js`, `src/utils/pagination.js`, `src/controllers/publicProfile.controller.js` (`getExpertDetails`), `src/controllers/reviews.controller.js` (expert reviews list). No `docs/ios/` spec exists for favorites. Plans: 16.3-01 (favorites wire, details flag), 16.3-02 (reviews wire, keys, reviews ViewModel), 16.3-03 (heart), 16.3-04 and 16.3-06 (Saved Experts), 16.3-05 (Reviews screen), 16.3-07 (wiring).

| capability | decision | reason |
|---|---|---|
| list favorites `GET /client/favorites` (page 1, limit 100) | INTEGRATE | |
| list row `expert_id`, `full_name`, `headline`, `avatar_url`, `expertise_tags` | INTEGRATE | |
| list row extra RPC columns (avg_rating, total_reviews, favorited_at, ...) | OPT-OUT | not needed: the Saved Experts card (iOS `FavoriteExpertCard`) shows none of them; the profile seed stays Pending (RESEARCH A4); total_count, experience_years and is_favorite are ignored too |
| list `pagination` and pages beyond the first | OPT-OUT | explicitly out of scope: one request of up to 100 rows, no load-more and no total-count UI (CONTEXT D-12, RESEARCH A6) |
| add favorite `PUT /client/favorites/:expertId` (idempotent, empty body) | INTEGRATE | |
| remove favorite `DELETE /client/favorites/:expertId` (idempotent) | INTEGRATE | |
| toggle response `data.expert_id`, `data.is_favorite` (server reconcile) | INTEGRATE | |
| toggle errors 403 / 404 / 422 and network failures (rollback and snackbar) | INTEGRATE | |
| `is_favorite` on `GET /experts/:id/details` (heart start state) | INTEGRATE | |
| heart start state from a `GET /client/favorites?limit=100` scan (iOS behavior) | OPT-OUT | explicitly out of scope: CONTEXT D-01 uses the details flag instead (the iOS comment saying details lack it is stale) |
| favorite toggles on Explore and Search cards | OPT-OUT | explicitly out of scope: CONTEXT phase boundary (favorites only on Expert Profile and Saved Experts) |
| reviews list `GET /experts/:id/reviews` page 1, limit 20 | INTEGRATE | |
| reviews `summary.overall_rating`, `summary.total_reviews` | INTEGRATE | |
| review `reviewer.name`, `reviewer.avatar_url`, `rating`, `comment` | INTEGRATE | |
| review `session_context.topic`, `session_context.date_label` | INTEGRATE | |
| review `session_context.date` (raw timestamp shown to the user) | OPT-OUT | not needed: decoded, but the card shows the server's `date_label` as iOS does (CONTEXT D-16) |
| reviews `pagination` and pages beyond the first | OPT-OUT | explicitly out of scope: page 1 only, as iOS (CONTEXT D-16, RESEARCH A6) |

# API Coverage — ClickMe backend `GET /experts/search` (plus `GET /categories` via CategoryStore)

> Full coverage by default. Opt-outs are explicit, reasoned decisions.
> Contract: `backend-reference/src/schemas/discovery.schema.js` (`searchQuerySchema`, `.strict()`), `src/controllers/discovery.controller.js` (`searchExperts`), RPC columns in `supabase/migrations/20261016000100_discovery_hide_moderated_users.sql`. Plans: 16.1-01 (wire), 16.1-03 (ViewModel), 16.1-06 (navigation seed).

| capability | decision | reason |
|---|---|---|
| query param `q` (free-text search, empty string = default browse set) | INTEGRATE | |
| query param `category` with one category slug (chip) | INTEGRATE | |
| query param `category` with several values (comma-joined or repeated) | OPT-OUT | not needed: the chip row is single select, as on iOS (D-11) |
| query param `category` with an expertise-tag UUID | OPT-OUT | not needed: chips carry category slugs only and iOS never sends tag ids to search |
| query param `sort=relevance` (Most popular) | INTEGRATE | |
| query param `sort=rating` (Highest rated) | INTEGRATE | |
| query param `page` (sent as 1) | INTEGRATE | |
| query param `limit` (sent as 20) | INTEGRATE | |
| paging past page 1 (page > 1, load more, infinite scroll) | OPT-OUT | explicitly out of scope: D-04 loads the first page only, as iOS |
| response `pagination` block | OPT-OUT | explicitly out of scope: D-04; deliberately undeclared so a malformed block cannot fail a search |
| row `expert_id`, `full_name`, `headline`, `avatar_url` | INTEGRATE | |
| row `avg_rating`, `expertise_tags` | INTEGRATE | |
| row `total_reviews` | OPT-OUT | not needed: decoded but unused, the profile seed keeps reviewCount null (user decision 2026-10-30) |
| row `is_favorite` | OPT-OUT | not needed yet: favorites belong to Phase 16.3 |
| row `experience_years`, `relevance_score`, `matched_topic_ids`, `total_count` | OPT-OUT | not needed: iOS does not display them |
| query params `min_rate` / `max_rate` | OPT-OUT | not available: removed from the strict backend schema; iOS never sends them |
| `GET /categories` chip vocabulary through the existing CategoryStore cache | INTEGRATE | |

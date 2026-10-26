---
phase: 10
review: 10-REVIEW.md
recorded: 2026-10-26T01:12:35Z
---

# Phase 10: Code Review Disposition

Re-review of the gap-closure changes (10-17..10-19), scoped to files changed since 6eb390b.

| Finding | Severity | Disposition | Source |
|---------|----------|-------------|--------|
| WR-01 | warning | open | 10-REVIEW.md |
| WR-02 | warning | open | 10-REVIEW.md |
| WR-03 | warning | open | 10-REVIEW.md |
| IN-01 | info | open | 10-REVIEW.md |
| IN-02 | info | open | 10-REVIEW.md |
| IN-03 | info | open | 10-REVIEW.md |
| IN-04 | info | open | 10-REVIEW.md |
| IN-05 | info | open | 10-REVIEW.md |

## Prior review (6eb390b) — triage from the 10-20 gap pass

The IDs below refer to the first review (`git show 6eb390b:.planning/phases/10-expert-topics-availability/10-REVIEW.md`), not the table above. The re-review confirms CR-01, WR-02, WR-04 and WR-05 fixed; prior WR-01 is partially fixed (reverse order is the new WR-01).

| Finding | Severity | Disposition | Source |
|---------|----------|-------------|--------|
| CR-01 | critical | fixed | 10-REVIEW.md; fixed in 10-17 (seed state gates Save, edits blocked until the profile tags load) |
| WR-01 | warning | fixed | 10-REVIEW.md; fixed in 10-19 (edit and refresh ignored while saving) |
| WR-02 | warning | fixed | 10-REVIEW.md; fixed in 10-18 (stored duration kept, unchanged duration omitted from PATCH) |
| WR-03 | warning | deferred | 10-REVIEW.md; deferred by the user for the gap pass: latent, the server shape matches today; keep the first-launch expertHasTopics lookup lenient later (STATE todo) |
| WR-04 | warning | fixed | 10-REVIEW.md; fixed in 10-19 (silent PUT only when hydrate is lossless, D-15 body kept) |
| WR-05 | warning | fixed | 10-REVIEW.md; fixed in 10-18 (selected tags shown and removable when the tag load fails) |
| IN-01 | info | deferred | 10-REVIEW.md; deferred by the user for the gap pass: info-level cleanup (STATE todo) |
| IN-02 | info | deferred | 10-REVIEW.md; deferred by the user for the gap pass: info-level cleanup (STATE todo) |
| IN-03 | info | deferred | 10-REVIEW.md; deferred by the user for the gap pass: info-level cleanup (STATE todo) |
| IN-04 | info | deferred | 10-REVIEW.md; deferred by the user for the gap pass: info-level cleanup (STATE todo) |
| IN-05 | info | deferred | 10-REVIEW.md; deferred by the user for the gap pass: info-level cleanup (STATE todo) |
| IN-06 | info | deferred | 10-REVIEW.md; deferred by the user for the gap pass: info-level cleanup (STATE todo) |
| IN-07 | info | deferred | 10-REVIEW.md; deferred by the user for the gap pass: info-level cleanup (STATE todo) |

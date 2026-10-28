---
phase: 13
review: 13-REVIEW.md
titles: json
findings:
  - id: WR-01
    severity: warning
    disposition: open
    title: "Token registrar holds its mutex across unbounded FCM Task awaits, so logout can hang"
  - id: WR-02
    severity: warning
    disposition: open
    title: "Lock-screen privacy is applied to foreground pushes only; system-displayed pushes show full content"
  - id: WR-03
    severity: warning
    disposition: open
    title: "A release build without `google-services.json` succeeds silently and ships without push"
  - id: WR-04
    severity: warning
    disposition: open
    title: "Submitting a review can be cancelled by a push-driven stack change, and the app then loses the result"
  - id: IN-01
    severity: info
    disposition: open
    title: "Logout after a rejected or suspended session sends an unauthenticated token DELETE"
  - id: IN-02
    severity: info
    disposition: open
    title: "`ClickMeMessagingService.onNewToken` registers on a fire-and-forget app-scope coroutine"
  - id: IN-03
    severity: info
    disposition: open
    title: "A push-opened Booking Summary compares booking ids case-sensitively"
  - id: IN-04
    severity: info
    disposition: open
    title: "`_sessionStarts.value++` is not atomic"
  - id: IN-05
    severity: info
    disposition: open
    title: "Unused parameters and a silent no-op default on new UI entry points"
  - id: IN-06
    severity: info
    disposition: open
    title: "Booking Summary briefly shows \"Leave a review\" for an already-reviewed booking"
open: 10
total: 10
recorded: 2026-10-28T17:49:19.343Z
---

# Phase 13: Code Review Disposition

| Finding | Severity | Disposition | Source |
|---------|----------|-------------|--------|
| WR-01 | warning | open | - |
| WR-02 | warning | open | - |
| WR-03 | warning | open | - |
| WR-04 | warning | open | - |
| IN-01 | info | open | - |
| IN-02 | info | open | - |
| IN-03 | info | open | - |
| IN-04 | info | open | - |
| IN-05 | info | open | - |
| IN-06 | info | open | - |

Dispositions: `open` (recorded, not yet triaged), `fixed`, `skipped`, `deferred`.
Set `deferred` by hand and put the reason in the Source cell; both are preserved. A `|` in the reason is kept as prose and escaped on the next run.
Re-running the gate keeps every row it can. A row the current review no longer reports is kept and its Source cell flagged, so a finding does not leave this record silently. ONE exception: when a finding id is REUSED by a different finding, the earlier decision cannot keep a row — the id is taken — and it is dropped. A RECORDED decision (anything but `open`) is named on the console when that happens; a row still at `open` is replaced silently, because `open` records no decision to lose.

---
phase: 12-chat-report-block
review: 12-REVIEW.md
updated: 2026-10-27
counts: { critical: 1, warning: 6, info: 5 }
open: 8
fixed: 4
---

# Phase 12 — Code Review Disposition

Advisory review (standard depth, 87 source/resource files). No finding blocked execution.
Every row defaults to `open` until fixed (`/gsd-code-review 12 --fix`) or explicitly accepted.
Gap pass 12-16 and 12-17 (from 12-VERIFICATION.md) fixed CR-01, WR-01, WR-03 and IN-02.

| ID | Severity | Finding | Disposition | Note |
|----|----------|---------|-------------|------|
| CR-01 | critical | Expert Profile report/block snackbar cancels itself (`consumeSnackbar()` changes the `LaunchedEffect` key mid-`showSnackbar`) | fixed | 12-17 Task 1: snackbar launched on the composition scope; ExpertProfileScreenTest asserts both messages |
| WR-01 | warning | Unblock can leave the thread composer locked (empty thread or failed rescan) | fixed | 12-16 Task 1: the Unblocked notice applies onUnblockSucceeded and restarts an in-flight scan |
| WR-02 | warning | Report & Block actionable before block scan finishes; may record false "blocked by me" | open | Not in the 12-16/12-17 gap pass; carried in STATE |
| WR-03 | warning | Optimistic send tied to `viewModelScope`; Back cancels it and loses the message silently | fixed | 12-16 Task 3: POST in the application scope; failed or refused text returns to the thread's composer for the same user |
| WR-04 | warning | UI state data classes holding user text use default `toString` | open | Not in the 12-16/12-17 gap pass; carried in STATE |
| WR-05 | warning | Send path double-dialogs ACCOUNT_SUSPENDED; treats 408/429 as refused | open | Not in the 12-16/12-17 gap pass; carried in STATE |
| WR-06 | warning | `openChatThread` compares display seeds, recreating an already-open thread | open | Not in the 12-16/12-17 gap pass; carried in STATE |
| IN-01 | info | `onScreenShown` fires three times on first composition | open | Not in the 12-16/12-17 gap pass; carried in STATE |
| IN-02 | info | Message key flips `s:` → `c:` after reload | fixed | 12-16 Task 2 (raised to warning by 12-VERIFICATION): keys are c: for own messages only |
| IN-03 | info | Blank profile name yields "Report " copy | open | Not in the 12-16/12-17 gap pass; carried in STATE |
| IN-04 | info | Double bottom inset on Report & Block | open | Not in the 12-16/12-17 gap pass; carried in STATE |
| IN-05 | info | Smaller consistency items | open | Not in the 12-16/12-17 gap pass; carried in STATE |

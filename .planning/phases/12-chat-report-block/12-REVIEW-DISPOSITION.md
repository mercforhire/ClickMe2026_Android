---
phase: 12-chat-report-block
review: 12-REVIEW.md
updated: 2026-10-27
counts: { critical: 1, warning: 9, info: 8 }
open: 14
fixed: 4
re_review: 2026-10-27 (gap pass, 11 files, standard; earlier review in commit 7737143)
---

# Phase 12 — Code Review Disposition

Advisory review (standard depth, 87 source/resource files). No finding blocked execution.
Every row defaults to `open` until fixed (`/gsd-code-review 12 --fix`) or explicitly accepted.
Gap pass 12-16 and 12-17 (from 12-VERIFICATION.md) fixed CR-01, WR-01, WR-03 and IN-02.
The gap-pass re-review (current 12-REVIEW.md) confirmed those four fixed and raised the GR- rows below; the first-pass report is in git at 7737143.

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
| GR-WR-01 | warning | Send that fails after the thread closed is restored as plain text, dropping its client_msg_id; an ambiguous failure (IOException/5xx) re-sent by the user can duplicate | open | Gap-pass re-review |
| GR-WR-02 | warning | Thread reopened while a send is in flight is not told when it succeeds; message invisible until the next quiet reload | open | Gap-pass re-review |
| GR-WR-03 | warning | All post-close failures (incl. THREAD_BLOCKED, ACCOUNT_SUSPENDED) parked as unexplained composer text; a failure after logout re-populates the cleared store | open | Gap-pass re-review |
| GR-IN-01 | info | A Failed bubble already on screen when Back is pressed is still lost | open | Gap-pass re-review |
| GR-IN-02 | info | Joined kept texts cut silently by the composer cap; map unbounded | open | Gap-pass re-review |
| GR-IN-03 | info | Missing tests: logout mid-send, success while reopened VM loads, null owner id | open | Gap-pass re-review |

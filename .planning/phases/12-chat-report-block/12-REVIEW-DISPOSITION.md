---
phase: 12-chat-report-block
review: 12-REVIEW.md
updated: 2026-10-27
counts: { critical: 1, warning: 6, info: 5 }
open: 12
---

# Phase 12 — Code Review Disposition

Advisory review (standard depth, 87 source/resource files). No finding blocked execution.
Every row defaults to `open` until fixed (`/gsd-code-review 12 --fix`) or explicitly accepted.

| ID | Severity | Finding | Disposition | Note |
|----|----------|---------|-------------|------|
| CR-01 | critical | Expert Profile report/block snackbar cancels itself (`consumeSnackbar()` changes the `LaunchedEffect` key mid-`showSnackbar`) | open | Confirmed by orchestrator against `ExpertProfileScreen.kt:113-124` |
| WR-01 | warning | Unblock can leave the thread composer locked (empty thread or failed rescan) | open | |
| WR-02 | warning | Report & Block actionable before block scan finishes; may record false "blocked by me" | open | |
| WR-03 | warning | Optimistic send tied to `viewModelScope`; Back cancels it and loses the message silently | open | |
| WR-04 | warning | UI state data classes holding user text use default `toString` | open | |
| WR-05 | warning | Send path double-dialogs ACCOUNT_SUSPENDED; treats 408/429 as refused | open | |
| WR-06 | warning | `openChatThread` compares display seeds, recreating an already-open thread | open | |
| IN-01 | info | `onScreenShown` fires three times on first composition | open | |
| IN-02 | info | Message key flips `s:` → `c:` after reload | open | |
| IN-03 | info | Blank profile name yields "Report " copy | open | |
| IN-04 | info | Double bottom inset on Report & Block | open | |
| IN-05 | info | Smaller consistency items | open | |

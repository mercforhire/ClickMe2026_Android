---
phase: 17-play-store-release
plan: 03
subsystem: release-docs
tags: [account-deletion, privacy-policy, payments-policy, play-console, d-05, d-06, docs, tracer]
requires: []
provides:
  - "Publish-ready account-deletion page (D-06, REL-06)"
  - "Anchored privacy-policy edit list E1-E5"
  - "D-05 payments evidence and the 13-row 17-08 answer table"
affects: [17-08, 17-09]
tech-stack:
  added: []
  patterns: [XHTML-compatible static page validated with xmllint]
key-files:
  created:
    - .planning/phases/17-play-store-release/17-DELETE-ACCOUNT-PAGE.html
    - .planning/phases/17-play-store-release/17-PRIVACY-POLICY-EDITS.md
    - .planning/phases/17-play-store-release/17-PAYMENTS-DECISION.md
  modified: []
key-decisions:
  - "D-05 basis corrected: the exemption is Google FAQ 10281818 (1:1 online paid services), not the policy page and not Apple's person-to-person wording; conclusion (keep Stripe) unchanged"
  - "Deletion page and privacy edits state what the backend keeps (reviews, chat messages, reports, booking and payment records) exactly as purgeAndLockAccount does"
requirements-completed: [REL-06, REL-07]
status: complete
duration: about 20 min
completed: 2026-11-06
commits: 3
plan_head_before: 25942db1b3ae502aa5036bd56faeed737fa75bec
plan_head_after: 2b75fa5b7dd0124d4c753aee8707d01286c7a0cf
actuals:
  tokens: 11000
  tasks: 3
  commits: 3
---

# Phase 17 Plan 03: Deletion page, privacy edits and payments evidence Summary

Three docs ready for the user's publish step and the 17-08 decision checkpoint: a self-contained account-deletion page whose deleted and kept lists follow `purgeAndLockAccount` step by step, an anchored privacy-policy edit sheet (E1-E5) fetched against the live policy, and the dated D-05 payments evidence with a 13-row answer table.

## Tasks

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 (tracer) | Account-deletion page | af16e8f3 | 17-DELETE-ACCOUNT-PAGE.html |
| 2 | Privacy-policy edit sheet | 867a308d | 17-PRIVACY-POLICY-EDITS.md |
| 3 | Payments decision record and 17-08 table | 2b75fa5b | 17-PAYMENTS-DECISION.md |

Tracer gate: the Task 1 verify (xmllint plus grep gates) passed and was re-run before expansion; no checkpoint (automated-only verify, end-of-phase mode).

## Verification

- PAGE-OK: xmllint exits 0, `<script` count 0, `mailto:support@clickme.app` 2, `<h2>` 5, analytics or font host 0.
- EDITS-OK: 5 `### E1-E5` headings, 5 `Anchor:` lines, "Firebase Cloud Messaging (Google)", "delete-account.html", "Deleted user", "140" all present.
- DECISION-OK: FAQ id and quote present, both section headings present, 13 numbered rows, 13 `(pending 17-08)`, recording-API grep over `app/src/main` prints nothing.
- `git status --porcelain app/` prints nothing (no app code touched).

## Deviations from Plan

None to the plan's tasks. Findings worth the user's attention (not auto-fixed because they are outside this plan's scope):

1. **In-app delete copy over-promises** (REL-06 transparency). `delete_account_body` says "All your data, including bookings, profile information, and payment details, will be permanently removed." The backend keeps reviews, chat messages, reports and booking and payment records. The web page and privacy edit E4 state what is really kept. Google compares the in-app text with the page. Candidate for a closed-track fix build (D-04); no app code was changed here.
2. **Contact address mismatch.** The live privacy policy gives clickmeapp@uptrendinvestments.net as the contact (5 mentions); the page uses support@clickme.app as the plan requires. Folded into 17-08 row 9 (confirm support@clickme.app is a monitored mailbox).
3. **Code vs RESEARCH on the deletion cascade:** matches RESEARCH. Two precisions from the code: a client's booking requests still waiting for approval do not block deletion (they are cancelled and the card authorization voided), and the expert guard also covers pending-approval requests; the page says so. The "SESSION_IN_PROGRESS" guard also covers a confirmed session starting within 15 minutes; the page says so.
4. **Policy page vs FAQ:** the policy page 9858738 lists "peer-to-peer payments" among cases where Play Billing must not be used (a different meaning, payments between users). Recorded in 17-PAYMENTS-DECISION.md so it is not mistaken for the exemption.

## Flagged Assumptions Awaiting 17-08

- **A5:** Google accepts Stripe as a service provider (card data collected, not shared). Table row 2.
- **A8:** "My app doesn't provide any financial features". Table row 6.
- **A11:** no retention periods in code, so pages say "as long as financial and tax law requires". Table row 8.

## Known Stubs

None. The `(pending 17-08)` cells are the intended placeholder for the checkpoint's answers.

## Threat Flags

None. T-17-06 (forged request) mitigated on the page by requiring the account's own email and a confirmation reply before deletion; T-17-07 by deleted and kept lists written from the cascade; T-17-08 by the no-script, no-external-resource grep gates; T-17-09 by the dated FAQ copy and recorded grep.

## Self-Check: PASSED

Created files exist (3 of 3). Commits af16e8f3, 867a308d and 2b75fa5b are ancestors of HEAD. Every task's acceptance criteria were re-run and passed.

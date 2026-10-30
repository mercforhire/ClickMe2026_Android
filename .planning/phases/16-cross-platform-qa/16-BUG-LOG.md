# Phase 16 Bug Log

QA-01: one log for the user's own findings plus every failed runbook row (D-11). The user reports results in the conversation; Claude records and triages them here. A failed row in `16-MATRIX-RUNBOOK.md` gets a Result of `[!] fail (BUG-NN, vc N)` and one row below.

## Rules

- One row per bug. IDs are `BUG-01`, `BUG-02` and so on.
- No secrets, tokens, passwords, Stripe keys or card numbers in any cell. Ids and error codes only; cards by brand and last 4.
- Evidence is a path under `/Users/LeonChen/ClickMe-release` (outside the repo). Raw logcat and screenshots never enter the repo.
- Device and build cell: device, versionCode and installer package (for example SM-A075F, vc 2, com.android.vending).

## Severity rubric (D-07)

A bug blocks the release if it:

- loses money or charges the wrong amount,
- prevents a booked session or call from happening,
- loses user data,
- crashes the app,
- or breaks a store-policy requirement (for example the foreground-service or account-deletion rules).

Everything else is not blocking:

- `fix-if-cheap`: visual, accessibility polish and edge-case bugs; fixed when the change touches at most 2 files and needs no product decision.
- `post-release`: logged and left for after release.

Write the Severity cell as `blocking (clause)`, `fix-if-cheap` or `post-release`.

## Owner routing (D-12)

- **android:** goes into the next fix batch (one versionCode bump and one internal-track upload per batch, D-15).
- **ios** and **backend:** one line is added to the report-to-iOS list in the STATE Pending Todos section. The bug is fixed in the owning repo during Phase 16 only when it is blocking by D-07 AND it blocks the Android release. Backend and iOS edits are run by the orchestrator sequentially without worktree isolation, and the orchestrator commits. Write "blocks Android release: yes" or "blocks Android release: no" inside the Severity cell for ios and backend rows.

## Status lifecycle

`open` then `fixed (pending re-test)` then `verified` or `re-test failed`. Non-blocking rows may end as `post-release`. Rows owned by other repos and not fixed here end as `reported (ios)` or `reported (backend)`.

## Bugs

| ID | Source (runbook row or free-form) | Steps | Device and build | Owner (android/ios/backend) | Severity (D-07) | Status | Fix commit | Re-test result |
|---|---|---|---|---|---|---|---|---|

No bugs logged yet.

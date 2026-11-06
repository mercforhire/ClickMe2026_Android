---
phase: 17-play-store-release
plan: 04
subsystem: release
tags: [play-console, data-safety, foreground-service, content-rating, store-listing, app-access, docs]
requires:
  - phase: 13-push-notifications-internal-testing
    provides: 13-PLAY-CONSOLE-DRAFTS.md (store listing, data safety and FGS drafts)
provides:
  - 17-CONSOLE-PASTE.md, the single paste-ready source for all Play Console App content, listing and release fields
  - corrected 13-PLAY-CONSOLE-DRAFTS.md pointing to the new source
affects: [17-05, 17-08, 17-09]
tech-stack:
  added: []
  patterns: [paste-from-fenced-blocks, "(confirm at 17-08 #n)" assumption markers]
key-files:
  created:
    - .planning/phases/17-play-store-release/17-CONSOLE-PASTE.md
  modified:
    - .planning/phases/13-push-notifications-internal-testing/13-PLAY-CONSOLE-DRAFTS.md
key-decisions:
  - "Data safety splits financial info into User payment info, Purchase history and Other financial info; card data collected = Yes, shared = No"
  - "FGS use case is Background Audio Access under Microphone; calling-type use cases explicitly excluded"
  - "Content rating A7 has no 17-08 row, so it is flagged for checking in Console at questionnaire time"
requirements-completed: [REL-04, REL-06]
duration: 20min
completed: 2026-11-06
status: complete
estimate:
  tokens: 65000
  raw_tokens: 65000
  tasks: 3
  confidence: low
actuals:
  tokens: 5500
  tasks: 3
  commits: 3
plan_head_before: 25942db1b3ae502aa5036bd56faeed737fa75bec
plan_head_after: 7ca34258e3ec9639082dc93090215b93fb5cb294
---

# Phase 17 Plan 04: Console paste file Summary

**One reviewed paste file (`17-CONSOLE-PASTE.md`, 11 sections in Console order) covering Data safety v2 with the deletion URL, App access, Ads, Content rating, Target audience, Financial features, the Microphone / Background Audio Access FGS declaration, the store listing with exact graphic specs, and the release fields; Phase 13 drafts corrected so stale text cannot be pasted.**

## Tasks

| Task | Name | Commit |
|------|------|--------|
| 1 | Skeleton in Console order and complete Data safety section (tracer) | 99a49cc1 |
| 2 | App content declarations (privacy, App access, Ads, rating, audience, financial, FGS, other) | 9ef0be9b |
| 3 | Store listing, release fields, tester instructions, Phase 13 draft corrections | 7ca34258 |

## Measured facts

- Full description: 2120 characters (limit 4,000). Short description: 68 characters (limit 80).
- Release notes: 116 characters (limit 500).
- Task verify commands printed DATASAFETY-OK, APPCONTENT-OK and LISTING-OK. No ranking words in sections 10-11. No line pairs the word password with a value. No stub lines remain.
- No app code touched.

## Flagged assumptions awaiting 17-08

- **A5** Stripe accepted as a service provider, so card data is not shared (Data safety row User payment info: confirm at 17-08 #2). Alternative is Shared = Yes.
- **A7** IARC wording and category are read in Console. 17-08's answer sheet has no row for it, so the paste file tells the user to check at questionnaire time rather than citing a wrong row number.
- **A8** No financial features (section 7: confirm at 17-08 #6).
- **A12** No Console prompt for the NFC permission (confirm at 17-08 #3). Section 9 carries a fallback reviewer answer.
- Also marked: FCM message preview in the Messages row (confirm at 17-08 #4).

## Deviations from Plan

None in substance. Two small judgement calls:

- The A7 marker uses no `17-08 #n` reference because `17-PAYMENTS-DECISION.md` rows 1-13 (per plan 17-03) have no content-rating row; citing #6 would have been wrong.
- Section 2 refers to the reviewer secret as "credential" in prose, so no line pairs the word password with a value. The plan's Task 2 verification still passes.

## Notes for the user

- Graphics paths named in section 10 (`/Users/LeonChen/ClickMe-release/store-assets/...`) belong to plan 17-05; the files do not exist yet.
- `17-PAYMENTS-DECISION.md`, `17-PRIVACY-POLICY-EDITS.md` and `17-DELETE-ACCOUNT-PAGE.html` come from plan 17-03 (parallel) and are referenced by name only.
- The executor never opened the backend reviewer-account document (T-17-10). The reviewer credential is typed into Console by the account owner.

## Known Stubs

None.

## Threat Flags

None.

## Self-Check: PASSED

- 17-CONSOLE-PASTE.md present, 11 numbered sections, 0 stub lines.
- Commits 99a49cc1, 9ef0be9b and 7ca34258 are ancestors of HEAD.
- `git status` clean apart from this SUMMARY before its commit.

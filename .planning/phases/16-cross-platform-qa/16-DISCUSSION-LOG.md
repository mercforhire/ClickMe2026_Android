# Phase 16: Cross-Platform QA - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-10-29
**Phase:** 16-cross-platform-qa
**Areas discussed:** Money-path backend, Release-blocking bar, Testers & bug intake, Test-session logistics

---

## Money-path backend

| Option | Description | Selected |
|--------|-------------|----------|
| Local Mac sandbox, both apps | Reuse 07-LOCAL-STRIPE-TESTING; repoint the iOS Development build to the Mac | |
| Hosted test-mode staging | New Railway staging with sk_test and its own database | |
| Live mode, real small charges | Production with real cards on cheap topics, then refund | ✓ |

**User's choice:** Live mode, real small charges

| Option | Description | Selected |
|--------|-------------|----------|
| Sandbox Connect on a uat expert | Full onboarding on the sandbox | |
| Onboarding link only | Link opens and the return is handled | ✓ |
| You decide | | |

| Option | Description | Selected |
|--------|-------------|----------|
| Android-only on local sandbox | 3DS and declines one-sided on the sandbox | |
| Real 3DS card in live mode | A real bank card that forces a challenge | ✓ |
| Drop 3DS/declines from the bar | Amend the QA-02/03 wording | |

| Option | Description | Selected |
|--------|-------------|----------|
| Your own real expert account, min price | Live Connect already enabled, about $1 CAD topic | ✓ |
| appreview@clickme.test | Store-review account | |
| Set one up in Phase 16 | New live expert account | |

| Option | Description | Selected |
|--------|-------------|----------|
| Through the app's own cancel flows | In-app cancel refunds, dashboard for leftovers | ✓ |
| Dashboard refund for everything | | |
| Let small charges stand | | |

---

## Release-blocking bar

| Option | Description | Selected |
|--------|-------------|----------|
| Matrix + tier-1 rows | Tiers 2–4 if time allows | ✓ |
| Matrix only | | |
| Everything | | |

| Option | Description | Selected |
|--------|-------------|----------|
| Money, session, data or crash | Plus store-policy breaks | ✓ |
| Anything in a tier-1 flow | | |
| Case by case | | |

| Option | Description | Selected |
|--------|-------------|----------|
| Signed prodRelease from Play internal track | Closes 999.1 | ✓ |
| devDebug against production | | |

| Option | Description | Selected |
|--------|-------------|----------|
| Out, post-release | androidTest a11y automation | ✓ |
| In, as a parallel plan | | |

---

## Testers & bug intake

| Option | Description | Selected |
|--------|-------------|----------|
| A few people you know, mixed devices | | |
| Just you, gap accepted | | ✓ |
| Closed testing track | | |

| Option | Description | Selected |
|--------|-------------|----------|
| One markdown log in the phase dir | 16-BUG-LOG.md | ✓ |
| GitHub issues | | |
| Play Console feedback + log | | |

| Option | Description | Selected |
|--------|-------------|----------|
| Log it, fix only if blocking | Owner tag; fix in iOS/backend only if it blocks the release | ✓ |
| Fix wherever it is | | |
| Android only | | |

---

## Test-session logistics

| Option | Description | Selected |
|--------|-------------|----------|
| Samsung + your iPhone, two accounts | | ✓ |
| Samsung + iOS simulator | | |
| Recruit a second person for calls | | |

| Option | Description | Selected |
|--------|-------------|----------|
| Runbook + live log capture + fix loop | | ✓ |
| Runbook only | | |
| You decide | | |

| Option | Description | Selected |
|--------|-------------|----------|
| Yes, Phase 16 is the human gate | | ✓ |
| No, keep deferring | | |

| Option | Description | Selected |
|--------|-------------|----------|
| Batch fixes per session, one upload | | ✓ |
| adb install the signed build per fix | | |

---

## Claude's Discretion

- Runbook ordering, grouping and per-row evidence; versionCode mechanism; regression tests with fixes.

## Deferred Ideas

- androidTest accessibility automation; wider tester group; hosted test-mode staging backend; full Connect onboarding; tier 2–4 rows not run.

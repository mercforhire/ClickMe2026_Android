# Phase 17: Play Store Release - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-11-05
**Phase:** 17-play-store-release
**Areas discussed:** Closed-testing route, Policy answers, Release build & rollout, Store smoke & assets

---

## Closed-testing route

| Option | Description | Selected |
|--------|-------------|----------|
| Re-check, then run test | Re-check the Dashboard; if production access is still gated, run the 12×14 closed test (adds ≥2 weeks) | ✓ |
| Run closed test now | Treat it as required and start the closed test as soon as the build is ready | |
| Contact Play support first | Ask Google about an org exemption, with the closed test in parallel | |

| Option | Description | Selected |
|--------|-------------|----------|
| Friends/family/staff | People the user knows | |
| Paid tester service | A service that supplies 12+ testers who stay opted in for 14 days | ✓ |
| Mix of both | Own people plus a service | |

| Option | Description | Selected |
|--------|-------------|----------|
| Install and open | Stay opted in, light use, light feedback | ✓ |
| Guided tasks | Short task list, structured feedback | |

| Option | Description | Selected |
|--------|-------------|----------|
| Updates allowed | Push fixes during the 14 days; the clock keeps running | ✓ |
| You decide | Research confirms the rule | |

**Notes:** Research must confirm that updates do not reset the 14-day clock.

---

## Policy answers

| Option | Description | Selected |
|--------|-------------|----------|
| Research, then confirm | Research checks the policy; the user confirms before submission | ✓ |
| Treat as exempt | Assume the exemption applies | |
| Ship free-only first | Hide paid topics if Play Billing is needed | |

| Option | Description | Selected |
|--------|-------------|----------|
| Need a new page | Create delete-account.html; the plan writes the content, the user publishes it | ✓ |
| Already exists | Use an existing URL | |
| Use privacy policy | A deletion section in privacy.html | |

| Option | Description | Selected |
|--------|-------------|----------|
| Reuse appreview account | appreview@clickme.test (client and expert, re-creatable) | ✓ |
| Separate Play account | Seed playreview@ | |

| Option | Description | Selected |
|--------|-------------|----------|
| 18+ only | Adults only | ✓ |
| 16+ | Broader reach | |
| Match iOS rating | Same as the App Store | |

---

## Release build & rollout

| Option | Description | Selected |
|--------|-------------|----------|
| 1.0.0 | versionName 1.0.0, versionCode continues from 5 | ✓ |
| Match iOS version | Same versionName as iOS | |

| Option | Description | Selected |
|--------|-------------|----------|
| Fix POL-02 + BUG-22/34 | SavedStateHandle for 3 screens, plus call controls reachable | ✓ |
| Fix POL-02 only | Data-loss fix only | |
| Ship as signed off | No code changes | |

| Option | Description | Selected |
|--------|-------------|----------|
| Staged 20% → 100% | Watch vitals, then go to 100% | |
| 100% at once | Full release on approval | ✓ |
| Managed publishing | Publish manually after approval | |

| Option | Description | Selected |
|--------|-------------|----------|
| Canada + US | The main user base | ✓ |
| Match iOS availability | Same as iOS | |
| All countries | Worldwide | |

---

## Store smoke & assets

| Option | Description | Selected |
|--------|-------------|----------|
| Android = new client | Play install signs up as a client; the iOS expert accepts and joins | ✓ |
| Both directions | Also a new Android expert booked by an iOS client | |

| Option | Description | Selected |
|--------|-------------|----------|
| Free topic | No money moves | ✓ |
| Paid, then refund | Real-card paid session, refunded | |

| Option | Description | Selected |
|--------|-------------|----------|
| On closed track + after live | Full run on the closed-track install, short repeat once live | ✓ |
| After live only | Only from the public listing | |

| Option | Description | Selected |
|--------|-------------|----------|
| I make them; plan lists specs | The user makes all assets | |
| Plan generates what it can | Executor captures screenshots via adb and exports the 512 icon; the user records the FGS video and makes the feature graphic | ✓ |
| Reuse iOS assets | Adapt the App Store art | |

---

## Claude's Discretion

- How to sequence the phase around the 14-day closed test.
- Recommendations on the open data safety items (card data, NFC, push content, privacy policy naming providers). The user confirms them at the payments checkpoint.
- The wording of the release notes and the tester instructions.
- The forced-logout push-token DELETE fix, only if it is trivial.

## Deferred Ideas

- The todo "Sync Android bug fixes to iOS" was reviewed and not folded in (it is iOS work).
- The accepted release deviations (FND-13, PAY-05, PAYOUT-02, POL-04, REL-05) and the post-release Android bugs.

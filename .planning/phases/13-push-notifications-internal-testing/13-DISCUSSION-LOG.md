# Phase 13: Push, Notifications & Internal Testing - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md. This log preserves the alternatives considered.

**Date:** 2026-10-27
**Phase:** 13-push-notifications-internal-testing
**Areas discussed:** Permission & channels, Routing edge cases, List & Write Review, Internal test logistics

---

## Permission & channels

| Option | Description | Selected |
|--------|-------------|----------|
| After login/signup | iOS parity; does nothing once answered | ✓ |
| Contextual, with a rationale first | Explainer card before the first booking or chat, then the prompt | |
| First app launch | Ask before login | |

| Option | Description | Selected |
|--------|-------------|----------|
| Quiet + list hint | Never re-ask; token still registered; "Notifications are off — Turn on" row in the list | ✓ |
| Fully silent, like iOS | Nothing after a denial | |

| Option | Description | Selected |
|--------|-------------|----------|
| Per-category + backend tweak | Bookings, Sessions, Messages, Account & payouts, General; push.js sets android channel_id | ✓ |
| Per-category, Android-only | Background pushes all land on General | |
| Single channel | One channel, no backend change | |

**Notes:** The backend payload has no channel_id today, so background pushes land on the default channel.

---

## Routing edge cases

| Question | Options | Selected |
|----------|---------|----------|
| Push for the other mode | Switch mode then route / Stay in mode (iOS) / Ask with a dialog | Switch mode, then route |
| Client booking push target | By booking status / Always Upcoming (iOS) / Always Summary | By booking status |
| Foreground arrival | System heads-up / In-app snackbar | System heads-up |
| Chat push for the open thread | Suppress / Show anyway (iOS) | Suppress |
| booking/requests push | Request Decision if id, else hub / Always the requests list (iOS) | Request Decision if id, else hub |
| Tap while logged out | Hold and route after login / Drop it | Drop it |
| payout/* and account/* before Phase 14 | Nearest existing screen / Notifications list | Notifications list |

---

## List & Write Review

| Question | Options | Selected |
|----------|---------|----------|
| Review entry points | Summary + Past card / Summary only / Push opens Write Review directly | Summary + Past card |
| Presentation and post-submit | Pushed screen, pop + snackbar / ModalBottomSheet | Pushed screen, pop + snackbar |
| List scope | iOS parity / Parity + paging | iOS parity |

---

## Internal test logistics

| Question | Options | Selected |
|----------|---------|----------|
| Firebase setup | Same project as iOS / Separate dev project | Same Firebase project as iOS |
| Human-only steps | Checklist plan, non-blocking / Blocking gate at phase end | Checklist plan, non-blocking |
| Testers | Email list, user supplies / Just me for now | Just me for now |

**Notes:** "Just me for now" partly falls short of SC-5 ("a few real users"). Recorded as D-22, with a wider group to be added by Phase 16.

---

## Claude's Discretion

- Channel ids, names and importance; notification icon and accent color
- The visible-thread signal for chat suppression
- The device id source for token registration
- The pending-destination holder and how the shells consume it
- The Feedback block and Past card affordance details
- Placeholder google-services.json vs a guarded plugin for CI

## Deferred Ideas

- List paging and read state (needs a backend endpoint)
- Holding a push across login
- A wider tester group by Phase 16
- Phase 14 payout and security destinations
- Notification preferences UI

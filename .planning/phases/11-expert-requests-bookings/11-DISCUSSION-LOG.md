# Phase 11: Expert Requests & Bookings - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-10-25
**Phase:** 11-expert-requests-bookings
**Areas discussed:** Decline contract, Accept → Confirmed flow, Dashboard payout card, Bookings, Summary & cancel

Scouting findings shown to the user first:
- iOS decline codes don't match the backend enum, and iOS leaves out the required `reason_text`.
- iOS never shows `BookingConfirmedView`.
- iOS always promises a "full refund" on an expert cancel, but the backend applies the 24h rule.

---

## Decline contract

| Option | Description | Selected |
|--------|-------------|----------|
| Backend's 4 codes | CONFLICT / OUT_OF_SCOPE / UNAVAILABLE / OTHER; drop "Pricing mismatch" | ✓ |
| iOS's 5, mapped | Keep the 5 labels, map Pricing → OTHER | |

| Option | Description | Selected |
|--------|-------------|----------|
| Optional, fall back to label | Send the label as reason_text when empty | ✓ |
| Required field | Decline disabled until text is entered | |
| Required only for Other | | |

| Option | Description | Selected |
|--------|-------------|----------|
| Pop back to list | Snackbar, refreshed list | ✓ |
| Stay, footer shows "Declined" (iOS) | | |

## Accept → Confirmed flow

| Option | Description | Selected |
|--------|-------------|----------|
| Replace decision screen | Confirmed replaces Request Decision in the stack | ✓ |
| Push on top | | |

| Option | Description | Selected |
|--------|-------------|----------|
| Pop to Dashboard root | Refreshed dashboard | ✓ |
| Back to Incoming Requests | | |

| Option | Description | Selected |
|--------|-------------|----------|
| Client + topic | "ClickMe · {topic} with {client}", meeting type + message in description | ✓ |
| iOS parity | "ClickMe · {name}", meeting note | |

| Option | Description | Selected |
|--------|-------------|----------|
| Alert, then back to list (409 ALREADY_PROCESSED) | | ✓ |
| Alert, stay (iOS) | | |

| Option | Description | Selected |
|--------|-------------|----------|
| Cap 1000, counter near limit | | ✓ |
| Cap silently | | |

## Dashboard payout card

| Option | Description | Selected |
|--------|-------------|----------|
| Status + launch onboarding | Connect status; mint link → Custom Tab; Phase 14 adds deep links | ✓ |
| Status only, CTA disabled | | |
| Full iOS card now | Pulls in part of PAYOUT-01 | |

| Option | Description | Selected |
|--------|-------------|----------|
| Net this week + session count | | ✓ |
| Net only (iOS) | | |

| Option | Description | Selected |
|--------|-------------|----------|
| Only targets that exist | | |
| Full grid, placeholders | All 6; Profile Settings / Payouts → coming soon | ✓ |
| Drop the grid | | |

| Option | Description | Selected |
|--------|-------------|----------|
| Switch to Bookings tab (reschedule banner + View Calendar) | Plus a "Tap to review" badge on cards | ✓ |
| Reschedule → first item's Summary | | |

| Option | Description | Selected |
|--------|-------------|----------|
| Resume + realtime + pull | | ✓ |
| iOS parity: reappear only | | |

## Bookings, Summary & cancel

| Option | Description | Selected |
|--------|-------------|----------|
| Cards + Today + Summary (Join) | | ✓ |
| iOS parity (cards + Today) | | |

| Option | Description | Selected |
|--------|-------------|----------|
| iOS parity, limit 50 | Merge rejoinable completed into Upcoming | ✓ |
| Match Phase 8 client (20/tab) | | |

| Option | Description | Selected |
|--------|-------------|----------|
| Mirror backend, client-facing wording | 24h rule, hold release, free → none | ✓ |
| Generic copy | | |

| Option | Description | Selected |
|--------|-------------|----------|
| Reuse Phase 8 two-step screen | | ✓ |
| ModalBottomSheet (iOS-like) | | |

**Reschedule response plan:** the expert variant of RescheduleResponseCard, a read-only "Awaiting client's response" state, a 409 SLOT_UNAVAILABLE message plus refresh, and `attemptModifyBooking`. The user approved it with "Looks right, wrap up".

## Claude's Discretion

- Layout and copy beyond what's pinned, route keys and package names, whether the cancel screen is shared or a sibling, RestrictedAction names, the form of the "coming soon" placeholder, and the DTOs.

## Deferred Ideas

- Message buttons → Phase 12. Payouts screen, deep links and Profile Settings → Phase 14. Expert private note → deferred. Expert-initiated reschedule → DEF-1. Paging → later. Backend note on late expert-cancel refunds. Client profile view → DEF-3.

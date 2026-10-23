# Phase 8: Client Bookings & Cancellation - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-10-23
**Phase:** 08-client-bookings-cancellation
**Areas discussed:** Reschedule entry point, Cancel: comment & refund, My Bookings list behavior, Summary & stub actions

---

## Reschedule entry point

**Where to accept or decline**

| Option | Description | Selected |
|--------|-------------|----------|
| Card on Upcoming Details | Amber card with Accept/Decline at the top of Upcoming Details; Summary shows it too | ✓ |
| Route taps to Booking Summary | Send pending_reschedule taps to Summary (closer to iOS code) | |
| Both entry points | Shared component on both screens | |

**PAYMENT_METHOD_REQUIRED on accept**

| Option | Description | Selected |
|--------|-------------|----------|
| Mirror iOS | Retry with the default saved card; no card → Payment Methods → Accept again | ✓ |
| Explain and send to Payment Methods | Dialog plus a button, no automatic retry | |

**Client waiting on the expert**

| Option | Description | Selected |
|--------|-------------|----------|
| Read-only "Awaiting expert's response" | iOS parity | ✓ |
| Hide the card | | |

**Decline reason**

| Option | Description | Selected |
|--------|-------------|----------|
| Optional reason prompt | 500-character cap, saved as decline_reason_text | ✓ |
| Simple confirm only | | |

---

## Cancel: comment & refund

**The comment**

| Option | Description | Selected |
|--------|-------------|----------|
| Merge into reason | "<label>: <comment>", capped at 500 characters | ✓ |
| Drop the comment field | | |
| Collect but don't send (iOS parity) | | |

**Refund preview**

| Option | Description | Selected |
|--------|-------------|----------|
| Apply the 24h rule on the client | Full refund if >24h, none within 24h, hold released for held payments | ✓ |
| Generic policy text only | | |
| iOS parity (full price always) | | |

**After a successful cancel**

| Option | Description | Selected |
|--------|-------------|----------|
| Show server result | Confirmation with refund_amount, then Past tab refreshed | ✓ |
| iOS parity (silent return) | | |

**Layout**

| Option | Description | Selected |
|--------|-------------|----------|
| Full screen, two steps | Matches the iOS flow | ✓ |
| Bottom sheet + AlertDialog | | |

---

## My Bookings list behavior

**Pagination**

| Option | Description | Selected |
|--------|-------------|----------|
| Load more on scroll | | |
| Page 1 only (iOS parity) | 20 per tab, known limit | ✓ |

**Refresh triggers**

| Option | Description | Selected |
|--------|-------------|----------|
| All three | Pull, quiet reload on return, realtime booking.update | ✓ |
| Pull + on return only | | |

**Past empty state**

| Option | Description | Selected |
|--------|-------------|----------|
| Simple empty state | "No past bookings yet" | ✓ |
| Blank (iOS parity) | | |

**Upcoming card actions**

| Option | Description | Selected |
|--------|-------------|----------|
| Join placeholder only | Disabled Join; Reschedule, Message and Note hidden | ✓ |
| Hide all actions | | |
| Disabled with "coming soon" | | |

---

## Summary & stub actions

**Private Note**

| Option | Description | Selected |
|--------|-------------|----------|
| Defer it | Not in BOOK-05..09 | ✓ |
| Build it in Phase 8 | | |

**Booking Summary actions**

| Option | Description | Selected |
|--------|-------------|----------|
| Details + Book Again | Message hidden until Phase 12; review hidden until Phase 13 | ✓ |
| Details only | | |
| Include read-only Feedback | | |

**Detail screen fixes**

| Option | Description | Selected |
|--------|-------------|----------|
| Fix both | Real status pill and the Money formatter fee | ✓ |
| iOS parity | | |

**Payment pill**

| Option | Description | Selected |
|--------|-------------|----------|
| Show for paid bookings | Card hold placed / Paid / Refunded | ✓ |
| Hide it | | |

---

## Claude's Discretion

- Accept confirm-dialog copy (including payment-conversion wording).
- Error copy for the reschedule and cancel error codes.
- Visual layout and spacing beyond what is pinned.
- Route keys and navigation structure.
- Response-model extensions.

## Deferred Ideas

- Private note screen.
- Paging past page 1.
- Backend: save the cancel reason.
- Backend: a refund preview endpoint.
- Message Expert → Phase 12. Leave a Review and Feedback → Phase 13. Join → Phase 9. Push deep links → Phase 13.

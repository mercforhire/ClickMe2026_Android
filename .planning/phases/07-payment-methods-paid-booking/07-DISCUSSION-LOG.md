# Phase 7: Payment Methods & Paid Booking - Discussion Log

> **This log is an audit trail only.** Planning, research and execution agents should not use it as input. The decisions are in CONTEXT.md; this log keeps the alternatives that were considered.

**Date:** 2026-10-22
**Phase:** 07-payment-methods-paid-booking
**Areas discussed:** Stripe test key blocker, Payment Methods entry, Paid CTA & no-card gate, Errors, 3DS & retry

---

## Stripe test key blocker

| Option | Description | Selected |
|--------|-------------|----------|
| I'll get it from the dashboard | User gets the pk_test of the account behind the backend secret key | ✓ |
| Ask the backend owner | Wait on someone outside the project; plan meanwhile | |
| Not sure it's the key | Start Phase 7 with a diagnostic task | |

| Option | Description | Selected |
|--------|-------------|----------|
| Yes, test mode | Test cards work against the production backend | |
| It's live | Real charges; needs staging or careful testing | |
| Don't know | Confirming the mode becomes an early Phase 7 item | ✓ |

| Option | Description | Selected |
|--------|-------------|----------|
| Code first, device test later | Build and unit-test; device checks become open items | ✓ |
| Block until fixed | The first plan is a checkpoint until the key works | |

**User's choice:** The user gets the key from the dashboard. The Stripe mode is unknown and gets confirmed early. Code first, with device tests later.

---

## Payment Methods entry

| Option | Description | Selected |
|--------|-------------|----------|
| Profile placeholder + booking | A row on the client Profile placeholder tab, plus the booking gate | ✓ |
| Booking flow only | Reachable only from Make a Booking until Phase 14 | |

| Option | Description | Selected |
|--------|-------------|----------|
| Overflow menu (iOS parity) | A ⋮ DropdownMenu with Set as default and Remove | ✓ |
| Swipe to delete + menu | Swipe for Remove, a menu for the default | |

| Option | Description | Selected |
|--------|-------------|----------|
| Confirm dialog (iOS) | An AlertDialog, then DELETE | ✓ |
| Remove + Undo snackbar | Optimistic removal | |

| Option | Description | Selected |
|--------|-------------|----------|
| Whatever server returns | Reload and show the server's default | ✓ |
| Auto-promote next card | The app sets a new default | |

---

## Paid CTA & no-card gate

| Option | Description | Selected |
|--------|-------------|----------|
| iOS parity: always gate | Any paid topic with no card routes to Payment Methods | ✓ |
| Gate long-lead only | Short-lead adds a card inline in PaymentSheet | |

| Option | Description | Selected |
|--------|-------------|----------|
| Back to form, re-tap Book | Reload the cards and keep the selections | ✓ |
| Auto-continue booking | Start the paid flow on return | |

| Option | Description | Selected |
|--------|-------------|----------|
| Revert to first topic | iOS parity | ✓ |
| Keep first free | Phase 6 D-02 | |

| Option | Description | Selected |
|--------|-------------|----------|
| iOS parity | Topic price, plus the "How does payment work?" sheet | ✓ |
| Also show server total | Show the payment-intent summary | |

---

## Errors, 3DS & retry

| Option | Description | Selected |
|--------|-------------|----------|
| Same as free path | One handler for slot-taken and meeting-link | ✓ |
| iOS paid copy as-is | The iOS paid-path messages exactly | |

| Option | Description | Selected |
|--------|-------------|----------|
| Alert, keep form | An alert; keep the slot and the same attempt id | ✓ |
| Silent reset | No alert | |

| Option | Description | Selected |
|--------|-------------|----------|
| Network alert + pi ref | Network copy plus the payment reference; keep the attempt id | ✓ |
| Auto-retry once | Retry confirm silently once | |

| Option | Description | Selected |
|--------|-------------|----------|
| Yes, SavedStateHandle | Keep the attempt id across process death | |
| Memory only | iOS parity | ✓ |

---

## Claude's Discretion

- The Stripe Compose integration API, PaymentConfiguration init, PaymentSheet appearance
- How the ViewModel exposes the paid step and events
- Payment Methods visuals, brand icons and copy details
- Where the shared lifecycle-steps composable lives, and how the Payment Methods route is registered

## Deferred Ideas

- Auto-continuing after a card is added
- Gating long-lead bookings only
- Showing the payment-intent total
- Saving the attempt id across process death
- Promo codes

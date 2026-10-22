# Phase 6: Booking Flow & Free Bookings - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-10-22
**Phase:** 06-booking-flow-free-bookings
**Areas discussed:** Paid topics before Phase 7, Calendar & slot display, Errors & meeting type, Request Sent & navigation

---

## Paid topics before Phase 7

| Option | Description | Selected |
|--------|-------------|----------|
| Show all, disable CTA | Paid topics listed; Book Now disabled with "coming soon" note | ✓ |
| Hide paid topics | Only free topics in the picker | |
| Build paid UI shell now | Payment info link + Add Payment Method wiring now | |

| Option | Description | Selected |
|--------|-------------|----------|
| First topic (iOS parity) | topics.first | |
| First free topic | Prefer a bookable topic, fall back to first | ✓ |
| None | User must pick | |

## Calendar & slot display

| Question | Options | Selected |
|----------|---------|----------|
| Day markers | Mark days with open slots / Markers + jump to first open day / iOS parity | Mark days with open slots |
| Held slots | Dimmed, not tappable (iOS) / Hidden | Dimmed, not tappable |
| Timezone | Friendly + note / Raw IANA id (iOS) | Friendly + note |
| Past months | Block / Allow (iOS parity) | Block |

## Errors & meeting type

| Question | Options | Selected |
|----------|---------|----------|
| Skype/Zoom | Offer both + handle 422 with auto-switch / In-app Voice only / Offer both, alert only | Offer both + handle 422 |
| Slot taken | Alert + clear + reload slots / Alert + clear | Alert + clear + reload |
| Error style | AlertDialog "Couldn't book" / Snackbar | AlertDialog |

## Request Sent & navigation

| Question | Options | Selected |
|----------|---------|----------|
| Back stack | Replace the form / iOS parity (push on top) | Replace the form |
| My Bookings | Switch to Bookings tab / Hide until Phase 8 | Switch to Bookings tab |
| Data source | Local data, no fetch / Fetch GET /client/bookings/:id (iOS) | Fetch (iOS) |
| Animation | Simple version / Full iOS stagger + glow / None | Simple version |

## Claude's Discretion

- Slot row layout (horizontal chips vs FlowRow), skeletons, calendar dot style
- Copy for the paid note, the free headline, and the timezone note
- Topic picker sheet layout, the no-topics empty state
- Navigator replace-top and cross-tab API shape
- First day of the week

## Deferred Ideas

- Paid CTA, payment gate, payment info sheet, payment-lifecycle card → Phase 7
- Auto-jump to the first open day (not chosen)
- Full iOS animation with the glow pulse (dropped)

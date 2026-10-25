# Phase 10: Expert Topics & Availability - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-10-24
**Phase:** 10-expert-topics-availability
**Areas discussed:** Availability entry point, Topic Editor scope, Price field & currency, Timezone & unsaved edits

---

## Availability entry point

| Question | Options | Selected |
|---|---|---|
| Entry | Both (Topics TopAppBar + Profile row) / Topics tab only / Profile row only | Both |
| Save | TopAppBar Save / Bottom save button | TopAppBar Save |
| Call guard | On Save / On opening the screen | On Save |
| Picker | Port iOS 30-min lists / Material TimePicker | Port iOS 30-min lists |

## Topic Editor scope

| Question | Options | Selected |
|---|---|---|
| Inline expertise sheet | Port it / Defer to Phase 14 | Port it |
| Delete | ⋮ menu + confirm / iOS parity, no confirm | ⋮ menu + confirm |
| Editor presentation | Pushed full screen / Full-screen dialog | Pushed full screen |
| Empty Topics tab | Empty state + CTA / Mirror iOS section | Empty state + CTA |

## Price field & currency

| Question | Options | Selected |
|---|---|---|
| Price input | Decimals, currency-aware / Whole units only (iOS) | Decimals, currency-aware |
| Currency | Keep topic's, else default / Always /me default (iOS) / Currency picker | Keep topic's, else default |
| Floor fetch | Launch /me, refresh if missing / Refresh /me every open | Launch /me, refresh if missing |
| Free | Blank/0 = free (iOS) / Explicit Free switch | Blank/0 = free |

## Timezone & unsaved edits

| Question | Options | Selected |
|---|---|---|
| TZ list | Reuse signup Timezones.kt / Full IANA + search | Reuse Timezones.kt |
| TZ picker | Bottom sheet list + Auto-detect / Dropdown menu | Bottom sheet + Auto-detect |
| UTC fill | Silent PUT (iOS) / Fill only, user saves | Silent PUT |
| Unsaved edits | Discard prompt / iOS parity, drop silently | iOS parity, drop silently (user overrode recommendation) |

## Claude's Discretion

- Layout and copy beyond the pinned items, route key names, the look of the "Saved" confirmation, the time-picker implementation, DTOs and repository shape.
- Research: whether the backend accepts a midnight end time.

## Deferred Ideas

- Expanded or full IANA timezone list.
- Unsaved-changes discard prompt.
- Topic currency picker.

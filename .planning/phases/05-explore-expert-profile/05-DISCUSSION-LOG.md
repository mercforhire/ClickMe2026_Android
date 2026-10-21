# Phase 5: Explore & Expert Profile - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md. This log preserves the alternatives considered.

**Date:** 2026-10-21
**Phase:** 05-explore-expert-profile
**Areas discussed:** Unbuilt destinations, Categories & CategoryStore, Expert Profile fidelity, Price & currency display

---

## Unbuilt destinations

| Question | Options | Selected |
|----------|---------|----------|
| Today's Sessions in Phase 5 | Hide until Phase 8 (rec.) / Show cards, taps do nothing / Show cards, Join hidden | Show cards, taps do nothing |
| Join Call on Today's cards | Visible but always dimmed (rec.) / Not rendered | Visible but always dimmed |
| "See all" links | Hide (rec.) / Switch to Search tab | Hide |
| Book Session before Phase 6 | Push stub booking screen (rec.) / Disabled button | Push stub booking screen |
| Deferred profile controls | Hide all (rec.) / Message disabled, hide rest / All disabled | Hide all |

**Notes:** The user chose to show Today's Sessions over the recommended "hide", so the Explore home looks complete.

---

## Categories & CategoryStore

| Question | Options | Selected |
|----------|---------|----------|
| Where CategoryStore is used | Build All Categories sheet (rec.) / Store only, no UI / Chip strip reads the store | Build All Categories sheet |
| Chip filter | Port as on iOS (rec.) / Display-only | Port as on iOS |
| Clear on logout | Keep across logout (rec.) / Clear on logout | Keep across logout |
| Icons | Slug → Material Symbols (rec.) / Text-only | Slug → Material Symbols |
| Sheet selection | Multi-select as on iOS (rec.) / Single-select | Multi-select |

---

## Expert Profile fidelity (plus Explore states)

| Question | Options | Selected |
|----------|---------|----------|
| Empty sections | Hide (rec.) / Header + empty message / iOS bare headers | Hide |
| 404 on details | Full-screen unavailable (rec.) / Snackbar then pop | Full-screen unavailable |
| Hero | Static glow, no dot (rec.) / Pulsing, no dot / Full parity | Static glow, no dot |
| Loading | Preview then fill, parallel (rec.) / Full-screen spinner | Preview then fill, parallel |
| Explore errors | One full-screen ErrorState (rec.) / Per-section retry (iOS) | Per-section retry (iOS) |
| Zero experts | Inline empty message (rec.) / Blank (iOS) | Inline empty message |

**Notes:** The user chose iOS-parity per-section retry over the recommended single error state.

---

## Price & currency display

| Question | Options | Selected |
|----------|---------|----------|
| Formatter | Shared Money formatter (rec.) / Shared, backed by /meta/currencies | Shared Money formatter |
| Locale | Device locale (rec.) / Fixed en-US | Device locale |
| Decimals | Drop .00 on whole amounts (rec.) / Always full decimals | Always full decimals |
| Free topics | "Free" everywhere (rec.) / iOS parity | "Free" everywhere |

**Notes:** The user chose always full decimals ("$50.00") over the recommended trimmed format.

## Claude's Discretion

Skeletons and card dimensions, empty and unavailable copy, the icon choice for each slug, how the selection state is held, how the route key carries the preview data, and the package for the Today's Sessions DTO.

## Deferred Ideas

The rest of DEF-7, the pulsing hero glow, and the "See all" links (they return with DEF-2 and DEF-3).

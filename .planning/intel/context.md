# Context

No DOC-type documents were ingested. This file holds the PRD's narrative, non-requirement sections (rationale, risks, testing approach) with source attribution, so the roadmapper has them in one place. Topics: 4.

## Topic: Why 8 weeks, not 4
- source: /Users/LeonChen/Documents/ClickMe_Android/docs/PORT_ROADMAP.md, "Why 8 weeks, not 4"

> The first draft fit everything into 4 weeks. That only works if nothing slips:
> - Week 3 had expert core, the Agora call **and** realtime.
> - Week 4 had chat, push, payouts, settings and store compliance.
>
> Each of those is roughly a week of work on its own. This version gives each high-risk piece its own week (Make a Booking and Stripe, the call, push and chat). It also leaves time for setting up the Play Store account and getting through review (see below).

## Topic: Scope size and testing approach
- source: /Users/LeonChen/Documents/ClickMe_Android/docs/PORT_ROADMAP.md, "Scope at a glance"

> The iOS app is about 50k lines of screen code across roughly 70 screen folders, plus about 9k lines of networking, models and managers.

> **Testing tip:** the iOS app is finished. Use it as the "other side" for every flow; for example, book from Android and accept on iOS. You don't need to finish both modes on Android before you can test end to end.

## Topic: Biggest schedule risks
- source: /Users/LeonChen/Documents/ClickMe_Android/docs/PORT_ROADMAP.md, "Biggest schedule risks"

> 1. **Make a Booking and Stripe** (Week 3): timezone regrouping, three payment paths and 3DS. The Week 1 spike reduces this risk.
> 2. **Call service and overlay** (Week 4): background audio and keeping the call across navigation. The Week 1 spike reduces this risk.
> 3. **Push deep-link routing** into per-tab back stacks (Week 6).
> 4. **Play Console org verification** (D-U-N-S number plus Google's review): an outside wait you can't control. Start in Week 1.

## Topic: External references
- source: /Users/LeonChen/Documents/ClickMe_Android/CLAUDE.md and /Users/LeonChen/Documents/ClickMe_Android/docs/PORT_ROADMAP.md (cross_refs)
- `ios-reference/` → symlink to ../ClickMe2026 (resolves). Behavior source of truth.
- `backend-reference/` → symlink to "../ClickMe2026 Backend" (resolves). `backend-reference/docs/ios/` holds the authoritative contract specs (listed in decisions.md under DEC-backend-contract).
- These are external references, not ingest targets. They were not synthesized and are not part of the cross-ref cycle graph.

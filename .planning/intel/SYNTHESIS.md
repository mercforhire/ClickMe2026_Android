# Ingest Synthesis: ClickMe Android (MVP port from iOS)

Mode: new
Precedence: ADR > SPEC > PRD > DOC (per-doc overrides: CLAUDE.md = 0, PORT_ROADMAP.md = 1)
Synthesized: 2026-10-19

## Docs synthesized: 2
- ADR: 1. /Users/LeonChen/Documents/ClickMe_Android/CLAUDE.md (LOCKED, high confidence, manifest-typed)
- PRD: 1. /Users/LeonChen/Documents/ClickMe_Android/docs/PORT_ROADMAP.md (high confidence, manifest-typed)
- SPEC: 0
- DOC: 0
- UNKNOWN: 0
- Cross-ref cycles: none (all cross_refs are external: ios-reference/, backend-reference/)

## Decisions locked: 14 (all from CLAUDE.md)
DEC-ios-source-of-truth, DEC-ui-compose-m3, DEC-mvvm-compose-state, DEC-coroutines, DEC-hilt, DEC-retrofit-moshi, DEC-jwt-encrypted-sharedprefs, DEC-fcm, DEC-agora, DEC-stripe-paymentsheet, DEC-sdk-levels, DEC-platform-swaps, DEC-file-organization, DEC-backend-contract
Detail: .planning/intel/decisions.md

## Requirements: 37 MVP, 8 deferred (v2)
Grouped by the PRD's weeks. Each week's "End of Week N" checkpoint is recorded verbatim as its acceptance criteria. Weeks 7 and 8 have no checkpoint in the source.
- Week 1 (Oct 19–25) Foundation and SDK spikes: REQ-play-console-org-account, REQ-project-scaffolding, REQ-networking-core, REQ-session-user-manager, REQ-design-system, REQ-app-shell, REQ-sdk-spikes
- Week 2 (Oct 26–Nov 1) Auth, signup, realtime plumbing: REQ-login-flow, REQ-signup-flow, REQ-onboarding-mode-switch, REQ-realtime-service
- Week 3 (Nov 2–8) Client discovery and booking: REQ-explore, REQ-expert-profile, REQ-payment-methods, REQ-make-a-booking, REQ-request-sent
- Week 4 (Nov 9–15) Client bookings and the call: REQ-my-bookings, REQ-upcoming-booking-details, REQ-client-cancellation, REQ-client-booking-summary, REQ-call
- Week 5 (Nov 16–22) Expert core: REQ-topics-editor, REQ-availability, REQ-expert-dashboard, REQ-incoming-requests, REQ-expert-bookings
- Week 6 (Nov 23–29) Chat, push, notifications: REQ-chat, REQ-report-block-unblock, REQ-push-fcm, REQ-notifications-list, REQ-write-review, REQ-internal-testing
- Week 7 (Nov 30–Dec 6) Payouts, settings, polish: REQ-payouts, REQ-profile-settings, REQ-polish
- Week 8 (Dec 7–13) QA and release: REQ-qa-cross-platform, REQ-play-release
- Out of scope (v2): DEF-1 through DEF-8
- Also recorded: the MVP cut order (payouts → notifications list → write review) and the never-cut list (delete account, report/block, Stripe paths, call paths)
Detail: .planning/intel/requirements.md

## Constraints: 17
- port-exclusion: 5 (the "Do not port" list)
- ios-defect: 5 (iOS issues not to copy)
- release-process: 3 (Play org account lead time, store-required features, microphone foreground service)
- api-contract: 4 (refresh-token reuse, date/error formats, money units, production backend and push platform)
Detail: .planning/intel/constraints.md

## Context topics: 4
Why 8 weeks, scope size and testing approach, schedule risks, external references
Detail: .planning/intel/context.md

## Conflicts: 0 blockers, 2 competing-variants/warnings, 6 info
- WARNING (resolved): Reschedule scope. Responding is in the MVP; only initiating is deferred.
- WARNING (resolved): package name is now `com.uptrendinvestments.clickmeapp` (DEC-application-id).
Status: AWAITING USER. Both warnings need a decision before routing.
Detail: /Users/LeonChen/Documents/ClickMe_Android/.planning/INGEST-CONFLICTS.md

## Notes for gsd-roadmapper
- Derive phases from the PRD's 8 weeks. The Week 1 Play Console org-account work is a long-lead item and runs alongside everything else from day one.
- Every phase must respect the 14 LOCKED decisions. PRD-added libraries (OkHttp, Coil, Navigation Compose, DataStore, socket.io-client-java, SplashScreen, Custom Tabs) are allowed additions, not locked decisions.
- Behavior comes from `ios-reference/`, and contract details from `backend-reference/docs/ios/`. The ios-defect constraints list the documented exceptions.

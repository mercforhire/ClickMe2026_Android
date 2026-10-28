---
phase: "12"
slug: "chat-report-block"
baseline: 12-UI-SPEC.md
overall: 20
mode: code-only
screenshots: none (Android Compose; no dev server)
interaction_capture: false
created: "2026-10-27"
---

# Phase 12 — UI Review

**Overall: 20/24** · code-only audit by gsd-ui-auditor, findings spot-checked by the orchestrator.

## Audit limits

- Static code audit only; no screenshots. Device behavior is covered by 12-UAT.md (5/5 passed, including 200% font scale and TalkBack).
- The auditor read UI-SPEC lines 1–464 of 667 (the copywriting table and "Assumptions and Open Items" were not read) and did not diff string values against the contract. Copywriting and Experience Design scores are lower-confidence.

## Pillar Scores

| Pillar | Score | Key finding |
|--------|-------|-------------|
| 1 Copywriting | 3/4 | Strings externalised across the five contract files; one hard-coded literal (`" · "` separator). Values not diffed against the contract |
| 2 Visuals | 3/4 | All contract components and all Message entry points present; decorative `contentDescription = null` correct; Filled-vs-Outlined icon drift |
| 3 Color | 4/4 | No hex or `Color(0x…)` literals in chat/safety code; failed bubble uses `error` border at 50% alpha, never a filled error button |
| 4 Typography | 4/4 | No `fontSize`/`.sp` overrides; all text on M3 slots; Display unused |
| 5 Spacing | 3/4 | All dp values are tokens or contract-listed exceptions; small stroke constants duplicated per file |
| 6 Experience Design | 3/4 | Pull-to-refresh, `imePadding`, live regions/state descriptions/custom actions and the in-flight back guard (`ReportAndBlockScreen.kt:88`) present; some contract behaviors not exercised by UAT |

## Priority Fixes (all WARNING, no BLOCKER)

1. **Hard-coded separator** — `screens/chat/chatting/subviews/MessageBubble.kt:231` `Text(text = " · ")`. The spec shows `· Read`, but the literal sits in a composable and splits the status line for TalkBack. Move to a string resource, ideally one formatted status string with "Read" as an annotated span. *(verified)*
2. **Icon family drift** — the spec (UI-SPEC line 40) says Material Icons Extended, Outlined. 12 usages in `screens/chat`, `components/chat`, `components/safety` are `Icons.Filled.*`: `Block` ×3 (e.g. `BlockCard.kt:128`), `Delete` (`FailedMessageMenu.kt:39`), `PersonOff`, `Search`, `Close`, `Check`, `ArrowUpward`, `ArrowDownward`, plus `AutoMirrored.Filled.ArrowBack` and `AutoMirrored.Filled.Send`. Switch to `Icons.Outlined.*` / `Icons.AutoMirrored.Outlined.*`. *(verified)*
3. **Behaviors not covered by UAT** — `RequestActionFooter` stacking below 320dp or at 1.5× font; icon-to-text Message fallback on cards with no Join control; Report sheet buttons stacking with Submit first at 200%; the 9000-character counter and the 10,000 cap. Check on device or add Compose tests.

## Minor Recommendations

- Share the stroke constants (`SpinnerStroke`, `LoadEarlierStroke`, …) duplicated across BlockCard, ReportCard, ReportUserSheet, MessageButton.
- Spot-check the 60/30/10 accent split on the thread on device.
- Audit which M3 slot each heading uses against the spec's sizes.
- Confirm `call_control_chat_unavailable` was removed from the in-call strings.

## Files Audited

- `.planning/phases/12-chat-report-block/12-UI-SPEC.md` (lines 1–464)
- `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/**`, `components/chat/MessageButton.kt`, `components/safety/*`, the Message-entry screens (Expert Profile, Upcoming Booking, Booking Summary, Upcoming card, Request Decision, Request footer, Expert Booking Summary, Expert Upcoming card), `screens/call/calling/subviews/MeetingCallControls.kt`
- `app/src/main/res/values/strings_chat_*.xml`, `strings_report_user.xml` (entry counts only)

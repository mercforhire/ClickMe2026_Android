# Phase 12: Chat, Report & Block - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-10-27
**Phase:** 12-chat-report-block
**Areas discussed:** Send failure UX, Block from Profile, Chat entry & nav, Conversations list

---

## Send failure UX

| Question | Options | Selected |
|---|---|---|
| Failed send bubble | Failed + tap to retry / Remove + restore text / Auto-retry then fail | Failed + tap to retry |
| Offline send | Try now, fail fast / Queue and auto-send / Disable composer offline | Try now, fail fast |
| 403 THREAD_BLOCKED / RECIPIENT_UNAVAILABLE | Remove bubble + banner / Mark failed, no retry | Remove bubble + banner |
| Composer length | Match backend cap / No limit (iOS) | Match backend cap |

## Block from Profile

| Question | Options | Selected |
|---|---|---|
| Profile Block flow | Confirm dialog, then block / Port iOS exactly (combined sheet) | Confirm dialog, then block |
| In-chat Report & Block layout | iOS combined screen / Separate menu items | iOS combined screen |
| After successful block | Stay, show blocked state / Exit the thread (iOS-ish) | Exit the thread |
| Expert-side report/block outside chat | Chat only / Also on Request Decision | Chat only |
| Profile menu when already blocked | Always 'Block' / Look up and show Unblock | Always 'Block' |
| Message to a blocked user | Yes, opens blocked thread / Hide Message when blocked | Yes, opens blocked thread |
| After report | 'Thanks, we'll review this' / Offer block afterward | 'Thanks, we'll review this' |

## Chat entry & nav

| Question | Options | Selected |
|---|---|---|
| Where the thread opens | Switch to Chats tab, push / Push in current tab | Switch to Chats tab, push |
| Message buttons (multi) | Expert Profile / Client Upcoming Details + Summary / Request Decision + expert Summary / My Bookings cards | All four |
| initiateChat in flight | Button spinner / Open thread immediately | Button spinner |
| Expert booking cards too? | Yes / Client only | Yes, expert cards too |

## Conversations list

| Question | Options | Selected |
|---|---|---|
| Chats tab badge | No badge (iOS) / Dot badge | No badge |
| Loading | One page of 50 + realtime / Infinite paging | One page of 50 + realtime |
| Search bar | Keep / Drop | Keep |
| Blocked rows | Subtle 'Blocked' label / No difference (iOS) / Hide | No difference (iOS) |

## Claude's Discretion

- Visual design of bubbles, banners, system rows and the Report & Block screen; Message button placement on cards; list time label format; repository scoping and realtime fan-out.

## Deferred Ideas

- Chats tab unread badge; list paging past 50; persistent offline queue; "Blocked" row label; expert-side Client Profile report/block (DEF-3); chat image attachments.

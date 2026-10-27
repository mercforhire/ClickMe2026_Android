---
status: complete
phase: 12-chat-report-block
source: [12-VERIFICATION.md]
started: 2026-10-27T21:46:49Z
updated: 2026-10-27T22:45:50.193Z
---

## Current Test

[testing complete]

## Tests

### 1. SC-1 on the Samsung SM-A075F (devDebug, production backend), iOS app as the other side: Conversations in both modes; Message from Expert Profile, client Upcoming Details, client Booking Summary, a My Bookings card, expert Request Decision (pending and expired), expert Booking Summary, an expert Upcoming card and the in-call Chat control
expected: Every entry lands on the thread in the Chats tab and Back goes to the list; the in-call control keeps the call running in the pill
result: pass

### 2. SC-2 with the iOS client: live messages both ways, Android send appears at once and reconciles without a duplicate, typing both ways, Read on Android bubbles, Load earlier pulls 30 more, booking events inline and live after iOS accepts or declines, airplane-mode failed bubble retries without a duplicate, background then foreground catches up. Also: Send then Back at once, then reopen (WR-03 fix: the message must not vanish; a quick reopen while the POST is in flight is the open GR-WR-02 case)
expected: All behaviors hold across a real two-client socket
result: pass

### 3. SC-3 report from chat and from the profile (backend row stores the enum value), block from chat (returns to the list with "Blocked {name}") and from the profile (stays, "You blocked {name}"), the 10/hour report limit message
expected: Profile report and block confirmation snackbars are visible for about 4 s (CR-01 fix) and not shown twice
result: pass

### 4. SC-4 the blocker sees "You blocked this conversation" and Unblock in Report & Block, the blocked side sees "You can't reply to this conversation" and no Unblock, an iOS send into the blocked thread fails, RECIPIENT_UNAVAILABLE with a suspended account. Also block from the profile on a thread with no messages, open the thread, Report & Block, Unblock, return
expected: Matches CHAT-UNBLOCK section 5; after Unblock the composer is back even for the empty thread (WR-01 fix)
result: pass

### 5. Visual and system: 200% font scale on Conversations, a failed bubble, the blocked banner, Report & Block, the Report sheet and the Request Decision footer; TalkBack on bubble actions and the reason group; jump-to-latest; list move-to-top animation; composer IME behavior; signed prodRelease R8 smoke (needs the deferred Phase 2 keystore)
expected: Nothing clips; accessibility labels read correctly
result: pass

## Summary

total: 5
passed: 5
issues: 0
pending: 0
skipped: 0
blocked: 0

## Gaps

[none]

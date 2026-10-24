# Phase 9: Voice Call - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md. This log preserves the alternatives considered.

**Date:** 2026-10-23
**Phase:** 09-voice-call
**Areas discussed:** Join entry points & window, Skype/Zoom external path, Call surface, pill & notification, Guards & lifecycle edge cases

---

## Join entry points & window

| Option | Description | Selected |
|--------|-------------|----------|
| All three | UBD, My Bookings card, Explore Today's Sessions all call CallCenter.startCall | ✓ |
| UBD + Today's Sessions only | Match iOS exactly | |
| UBD only | Minimum for CALL-01 | |

| Option | Description | Selected |
|--------|-------------|----------|
| iOS client-side rule | isWithinJoinWindow: ±1h, no pending approval, completed rejoinable until endTime | ✓ |
| Server actions.canJoin | Tighter (~5 min) window | |
| You decide | | |

| Option | Description | Selected |
|--------|-------------|----------|
| Ticker re-evaluates | Join enables live while the screen is open | ✓ |
| On load/refresh only | | |

| Option | Description | Selected |
|--------|-------------|----------|
| Port iOS generic AlertDialog | Container tears down, root dialog shows the message | ✓ |
| Snackbar on the launching screen | | |

## Skype/Zoom external path

| Option | Description | Selected |
|--------|-------------|----------|
| Port the iOS surface | Same container/pill, Open/Copy, confirm-connection on Open, /end on Hang up | ✓ |
| Launch the link directly | No container, no /end | |

| Option | Description | Selected |
|--------|-------------|----------|
| App intent first, Custom Tab fallback | | ✓ |
| Always Custom Tab | | |

| Option | Description | Selected |
|--------|-------------|----------|
| No service for external | No mic, so no microphone foreground service | ✓ |
| Plain notification | | |

## Call surface, pill & notification

| Option | Description | Selected |
|--------|-------------|----------|
| Root composable overlay | CallOverlayHost in ClickMeApp above AppRoute | ✓ |
| Separate CallActivity | | |

| Option | Description | Selected |
|--------|-------------|----------|
| Back minimizes to the pill | Only Hang up ends the call | ✓ |
| Back asks to end the call | | |

| Option | Description | Selected |
|--------|-------------|----------|
| Tap to open + Hang up action | | ✓ |
| Tap to open only | | |
| Tap + Hang up + Mute | | |

| Option | Description | Selected |
|--------|-------------|----------|
| Hide Chat until Phase 12 | | |
| Show it disabled | Keeps the iOS control layout | ✓ |
| Wire it now | | |

## Guards & lifecycle edge cases

| Option | Description | Selected |
|--------|-------------|----------|
| API now + wire existing call sites | Mode switch, logout, add payment method, cancel/reschedule | ✓ |
| API now, mode switch only | | |

| Option | Description | Selected |
|--------|-------------|----------|
| Call keeps going after a task swipe | The foreground service outlives the task | ✓ |
| End the call on a task swipe | | |

| Option | Description | Selected |
|--------|-------------|----------|
| No auto-restore after process death | The user taps Join again | ✓ |
| Offer to rejoin | | |

| Option | Description | Selected |
|--------|-------------|----------|
| Early device check, non-blocking | | |
| Blocking device gate first | | |
| Verify only at phase end | One end-to-end UAT check | ✓ |

## Claude's Discretion

- Mic and notification permission UX, audio focus, routing and interruptions, a forced logout mid-call, the ticker cadence, pill and notification visuals, and how the service and CallCenter share state.

## Deferred Ideas

- Enabling the Chat button (Phase 12), expert Join entry points (Phase 11), guard call sites on later screens (Phases 10, 11, 14), and the Play foreground-service declaration (Phase 17).

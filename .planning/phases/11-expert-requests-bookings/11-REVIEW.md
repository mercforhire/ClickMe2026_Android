---
phase: 11-expert-requests-bookings
reviewed: 2026-10-26T00:00:00Z
depth: standard
files_reviewed: 4
files_reviewed_list:
  - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/AgoraEngineConfig.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/AgoraManager.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/managers/AgoraEngineConfigTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/managers/AgoraManagerCreateEngineTest.kt
findings:
  critical: 0
  warning: 2
  info: 3
  total: 5
status: issues_found
---

# Phase 11: Code Review Report (incremental: gap-closure plans 11-16 and 11-17)

**Reviewed:** 2026-10-26
**Depth:** standard
**Files Reviewed:** 4

## Summary

Incremental review of `git diff 6a79524..HEAD` for the Agora files, with the full files read for context and compared against iOS `ClickMe2026/Managers/AgoraManager.swift` (`initializeAgora`, `joinChannel`).

The core changes are correct:

- **Context-shadowing fix (G-11-1).** `buildRtcEngineConfig` takes `appContext` and sets fields without a receiver scope. `createEngine` reads `context.applicationContext` before the call. This removes the null `getContext()` shadowing.
- **Channel profile parity (G-11-5).** Engine config and join options both use `CHANNEL_PROFILE_COMMUNICATION`, matching iOS lines 99 and 145. The constant is 0 and the SDK default is 1, so the config test does catch a regression.
- **Redaction.** Log lines carry codes and elapsed milliseconds only. No token, channel name or uid is logged. `describeEngineFailure` omits the message and cause, and the tests use sentinel values to prove it.
- **Mutex and `isLeaving`.** The mutex discipline and `isLeaving` handling are unchanged and sound.
- **Event mapping.** `mapUserOffline`, `mapConnectionStateChanged` and the `RemoteJoined`/`RemoteOffline` events are unchanged by this diff and still correct.

No security issues or crashes were found. The gaps are about verification of the new cleanup path and robustness.

## Warnings

### WR-01: Engine cleanup on setup failure (the 11-17 headline fix) has no test

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/managers/AgoraManager.kt:164-181` (tests: `AgoraManagerCreateEngineTest.kt`)
**Issue:** The `created` tracking and `runCatching { RtcEngine.destroy() }` path runs only when `createRtcEngine` returned a non-null engine and a later `setAudioProfile`, `setAudioScenario` or speakerphone call throws. All five tests inject a factory that returns null or throws, so `created` is always null and the destroy branch is never executed. `RtcEngine.destroy()` is a static call hard-wired into `createEngine` and `destroy()`, so the test seam stops short of it. If someone removes the `created != null` guard, or the SDK reuses its singleton after a failed setup, no test fails. The prior review's WR-01 is therefore only fixed by inspection.
**Fix:** Add a `destroyRtcEngine: () -> Unit = { RtcEngine.destroy() }` constructor parameter, defaulting in the Hilt secondary constructor like `createRtcEngine`. Call it from both `createEngine` and `destroy()`. Then add a test with a factory returning a stub `RtcEngine` whose `setAudioProfile` throws. Assert that the destroy lambda ran exactly once, that `join` returned false, and that a second `join` calls the factory again. If `RtcEngine` is not mockable, a thin `RtcEngineHandle` interface works.

### WR-02: `destroy()` is not exception-safe, unlike the new setup-failure cleanup

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/managers/AgoraManager.kt:217-227`
**Issue:** `createEngine` wraps its cleanup in `runCatching` (line 180), but `destroy()` calls `RtcEngine.destroy()` bare. If it throws, the exception escapes `withLock`. `engine`, `isLeaving` and `leaveSignal` are never reset, so the manager holds a dead engine reference. The next `join` skips `createEngine` (`engine ?: createEngine()`) and calls `joinChannel` on a destroyed engine. The exception also propagates to `CallCenter`'s teardown path, which may skip its remaining cleanup. This predates the diff but sits next to the new defensive code and is inconsistent with it.
**Fix:**
```kotlin
if (engine != null) {
    try {
        RtcEngine.destroy()
    } finally {
        engine = null
        isLeaving = false
        leaveSignal = null
    }
}
```
Alternatively, use `runCatching` and log with `describeEngineFailure`.

## Info

### IN-01: Per-step SDK return codes in `createEngine` are ignored

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/managers/AgoraManager.kt:165-168`
**Issue:** `setAudioProfile`, `setAudioScenario`, `setDefaultAudioRoutetoSpeakerphone` and `setEnableSpeakerphone` return Int error codes. A non-zero return (for example, speaker routing rejected) is silently accepted and the engine is cached as healthy. `joinChannel` failure is now logged with its code, but these are not. For a device-debug effort (G-11-5) this is a small observability gap.
**Fix:** Log the code when non-zero, for example `rtc.setEnableSpeakerphone(true).let { if (it != 0) Log.w(TAG, "setEnableSpeakerphone code=$it") }`.

### IN-02: New remote-presence logs are unconditional INFO in release builds

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/managers/AgoraManager.kt:89, 97, 107, 115, 122`
**Issue:** The lines are redacted and contain no identifiers. They are still emitted in production, where they leak call-timing and peer-presence signals to anyone with logcat access. The comment says they exist for device diagnosis of G-11-5, which is a debug need.
**Fix:** Gate the `Log.i` calls on `BuildConfig.DEBUG`, or remove them once G-11-5 is closed.

### IN-03: Minor parity and coverage gaps against iOS and the tests

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/managers/AgoraManager.kt:165-168`
**Issue:**
- iOS `initializeAgora` also calls `disableVideo()` and `enableAudioVolumeIndication(200, 3, false)`. Android omits both. Video is off by default in the 4.x SDK, and volume indication is unused by the Android UI as far as this diff shows, so this is not a defect. Confirm that omission is intentional.
- No test covers the success path (`joinChannel` returning 0 or non-zero, the `joinChannel failed code=` warning, or engine reuse on the second `join`), because the stub factory never returns an engine.

**Fix:** Either note the intentional omissions in the class KDoc, or add them. Cover the success path once the `RtcEngine` seam from WR-01 exists.

---

_Reviewed: 2026-10-26_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_

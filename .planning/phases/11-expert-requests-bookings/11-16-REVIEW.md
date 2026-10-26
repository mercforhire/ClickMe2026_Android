---
phase: 11-expert-requests-bookings
reviewed: 2026-10-26T22:45:00Z
depth: standard
files_reviewed: 4
files_reviewed_list:
  - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/AgoraManager.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/AgoraEngineConfig.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/managers/AgoraEngineConfigTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/managers/AgoraManagerCreateEngineTest.kt
findings:
  critical: 0
  warning: 1
  info: 2
  total: 3
status: issues_found
---

# Phase 11 (plan 11-16): Code Review Report

**Reviewed:** 2026-10-26T22:45:00Z
**Depth:** standard
**Files Reviewed:** 4
**Status:** issues_found

## Summary

Gap-closure review of G-11-1 (diff `865e6cd..HEAD`). The fix is correct. `createEngine()` reads `context.applicationContext` outside any config scope. The config is built by `buildRtcEngineConfig(appContext, ...)` with plain assignments (no `apply`/`with`/`run`). A null `RtcEngine.create` result is guarded with its own log line. The failure logs carry only the class name and the top frame, never the message or cause (T-09-02).

I ran both new test classes (`:app:testDevDebugUnitTest`, 7 tests): all pass. The `AgoraManagerCreateEngineTest` regression would have failed on the pre-fix code. The old NPE was caught before the factory was reached, so `calls` would have been 0.

Shadowing sweep of `AgoraManager`: the only SDK-object scope left is `ChannelMediaOptions().apply { ... }` in `join` (lines 124-130). It reads no outer name today, so there is no live bug. The scope does carry a latent hazard, covered in IN-01. The anonymous `IRtcEngineEventHandler` has no non-`on*` members, so `isLeaving`, `leaveSignal`, `_events` and `TAG` cannot be shadowed there. I checked `RtcEngineConfig` and `ChannelMediaOptions` with `javap` against Agora SDK 4.6.4. `RtcEngineConfig` exposes a synthetic `context` property (`getContext()`) plus `appId`, `channelProfile`, `audioScenario`, `areaCode` and others. None of these names are used in an `apply` block now.

No critical issues.

## Warnings

### WR-01: A failure after `RtcEngine.create` succeeds leaks an undestroyed native engine

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/managers/AgoraManager.kt:143-161`
**Issue:** `rtc` is created at line 143, but `engine = rtc` is only assigned at line 152, after four SDK calls (`setAudioProfile`, `setAudioScenario`, `setDefaultAudioRoutetoSpeakerphone`, `setEnableSpeakerphone`). If any of them throws, the `catch` blocks log and return null. The native engine is already live, `engine` is still null, and `destroy()` is guarded by `if (engine != null)`. So `RtcEngine.destroy()` is never called for it. The next `join()` calls `RtcEngine.create` again while the first instance still exists. Agora's engine is effectively a process singleton, so the retry can return the same instance or fail, with the old event handler still attached. This is the same failure class the plan is trying to make diagnosable. The pre-fix code had the same shape, but the plan restructured this function, so it is worth closing here.
**Fix:**
```kotlin
private fun createEngine(): RtcEngine? {
    var created: RtcEngine? = null
    try {
        val appContext = context.applicationContext
        val config = buildRtcEngineConfig(appContext, BuildConfig.AGORA_APP_ID, handler)
        val rtc = createRtcEngine(config)
        if (rtc == null) {
            Log.w(TAG, "engine create returned null")
            return null
        }
        created = rtc
        rtc.setAudioProfile(Constants.AUDIO_PROFILE_SPEECH_STANDARD)
        // ...remaining setup...
        engine = rtc
        return rtc
    } catch (e: Exception) {
        Log.w(TAG, "engine create failed ${describeEngineFailure(e)}")
    } catch (e: LinkageError) {
        Log.w(TAG, "engine create failed ${describeEngineFailure(e)}")
    }
    if (created != null) runCatching { RtcEngine.destroy() }
    return null
}
```
`destroy()` runs under `mutex` on `io`, so calling `RtcEngine.destroy()` here is safe (join already holds the lock). Add a test where the factory returns a mock `RtcEngine` whose `setAudioProfile` throws, and assert the cleanup.

## Info

### IN-01: `ChannelMediaOptions().apply {}` in `join` sits next to a shadowing trap on `token`

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/managers/AgoraManager.kt:124-130`
**Issue:** `ChannelMediaOptions` has a public `token` field and `getToken()`. `join(token: String, ...)` has a parameter of the same name. This is the same mistake as G-11-1: inside the `apply`, a bare `token` resolves to the options' own (null) field, not the parameter. Today the block assigns only constants, so nothing is wrong. A future `token = token` would silently self-assign null. Per-channel tokens are a plausible future need (token renewal, rejoin). Nothing guards this, since the new tests stop at the factory boundary.
**Fix:** Do not use an SDK-object receiver scope where outer names are in play. Either write explicit `options.channelProfile = ...` assignments, as `buildRtcEngineConfig` does, or add a comment saying that `token` and `uid` must not be read inside this block. Optionally extract `buildChannelMediaOptions()` into `AgoraEngineConfig.kt` with a trivial JVM test.

### IN-02: `describeEngineFailure` can produce an uninformative or odd frame

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/managers/AgoraEngineConfig.kt:40-44`
**Issue:** Only `stackTrace.first()` is reported. For a Kotlin null-check failure the top frame is `kotlin.jvm.internal.Intrinsics.checkNotNull...`, and for JNI it is a native frame whose `lineNumber` is -2 (`Foo.bar:-2`). Neither identifies the caller, which is the diagnosability gap this plan set out to close. Reporting the first frame outside `kotlin.jvm.internal`/`java.` (or the first two frames) keeps the redaction guarantee, since frames contain no arguments.
**Fix:**
```kotlin
val frame = error.stackTrace.firstOrNull { !it.className.startsWith("kotlin.jvm.internal.") }
    ?: error.stackTrace.firstOrNull() ?: return name
val line = if (frame.lineNumber >= 0) ":${frame.lineNumber}" else ""
return "$name at ${frame.className}.${frame.methodName}$line"
```
Update the `describeFailureNamesClassAndTopFrameWithoutMessage` expectation only if the format changes.

---

_Reviewed: 2026-10-26T22:45:00Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_

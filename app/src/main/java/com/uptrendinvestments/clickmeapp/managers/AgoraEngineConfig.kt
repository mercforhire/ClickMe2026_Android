package com.uptrendinvestments.clickmeapp.managers

import android.content.Context
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngineConfig

/*
 * Agora SDK setup helpers that are free of the native runtime, so they are JVM-testable. Like AgoraEventMapping.kt,
 * nothing here loads the Agora native libraries.
 */

/**
 * Builds the [RtcEngineConfig] for `RtcEngine.create`.
 *
 * The context parameter is deliberately named `appContext`. [RtcEngineConfig] has a public getter `getContext()`
 * that Kotlin exposes as a `context` property inside any config receiver scope (`apply`, `with`, `run`), and that
 * getter returns the still-null `mContext` field. Reading an outer `context` inside such a scope silently picks
 * the config's own null getter and throws a NullPointerException, which made every in-app voice join fail (G-11-1).
 * Pass the application context in as a parameter and never read an outer `context` inside a config scope.
 */
internal fun buildRtcEngineConfig(
    appContext: Context,
    appId: String,
    handler: IRtcEngineEventHandler,
): RtcEngineConfig {
    val config = RtcEngineConfig()
    config.mContext = appContext
    config.mAppId = appId
    config.mEventHandler = handler
    return config
}

/**
 * Describes an engine-creation failure for logcat as the simple class name plus the top stack frame
 * (`NullPointerException at com.example.Foo.bar:42`), or the class name alone when there is no stack trace.
 *
 * Never includes the exception message or the cause: SDK and JNI messages can echo call arguments such as the
 * RTC token, channel name or uid (T-09-02).
 */
internal fun describeEngineFailure(error: Throwable): String {
    val name = error.javaClass.simpleName.ifBlank { error.javaClass.name }
    val frame = error.stackTrace.firstOrNull() ?: return name
    return "$name at ${frame.className}.${frame.methodName}:${frame.lineNumber}"
}

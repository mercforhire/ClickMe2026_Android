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

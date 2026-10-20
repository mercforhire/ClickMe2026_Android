package com.uptrendinvestments.clickmeapp.network

import okhttp3.Interceptor

/** Release stub: no HTTP logging ships in release builds. The debug source set provides the real logger. */
object DebugNetwork {
    fun interceptors(): List<Interceptor> = emptyList()
}

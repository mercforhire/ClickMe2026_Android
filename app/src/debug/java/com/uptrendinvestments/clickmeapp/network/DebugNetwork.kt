package com.uptrendinvestments.clickmeapp.network

import android.util.Log
import okhttp3.Interceptor
import okhttp3.logging.HttpLoggingInterceptor

/**
 * Debug-only HTTP logging. BASIC logs the request line, response code and size, never bodies (login bodies
 * hold the password; refresh responses hold both tokens). Authorization is redacted regardless.
 * The release source set has a stub with the same signature that returns nothing.
 */
object DebugNetwork {
    fun interceptors(): List<Interceptor> =
        listOf(
            HttpLoggingInterceptor { message -> Log.d("ClickMeHttp", message) }.apply {
                level = HttpLoggingInterceptor.Level.BASIC
                redactHeader("Authorization")
            },
        )
}

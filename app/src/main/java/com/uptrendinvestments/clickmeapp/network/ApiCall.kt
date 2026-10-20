package com.uptrendinvestments.clickmeapp.network

import kotlin.coroutines.cancellation.CancellationException

/**
 * Runs a network call and converts every failure into an [ApiException]. Every UserManager network call
 * goes through this so callers handle one exception type. Coroutine cancellation is rethrown untouched.
 */
suspend fun <T> apiCall(block: suspend () -> T): T =
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        throw ApiErrorParser.from(e)
    }

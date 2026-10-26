package com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Nav3 route to Request Decision (accept or decline one pending request).
 *
 * The request detail endpoint returns no status and no expiry (C3), so the tapped list row hands both over:
 * [isExpired] and [expiresAtMillis]. A key built without them (future push routing) is treated as pending, and the
 * server answers 409 ALREADY_PROCESSED if it is not (Open Question Resolution 3).
 *
 * [listEarningsMajor] and [listCurrency] are the list's major-unit earnings, correct to the cent, used for the
 * "Session price" fallback when the detail has no usable price (C4). The expiry is epoch millis because `Instant`
 * is not `@Serializable`.
 */
@Serializable
data class RequestDecisionKey(
    val requestId: String,
    val isExpired: Boolean = false,
    val expiresAtMillis: Long? = null,
    val listEarningsMajor: Double? = null,
    val listCurrency: String? = null,
) : NavKey

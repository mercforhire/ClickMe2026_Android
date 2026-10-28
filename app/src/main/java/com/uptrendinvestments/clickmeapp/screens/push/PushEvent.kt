package com.uptrendinvestments.clickmeapp.screens.push

import java.util.Locale

/**
 * What a push payload says, reduced to a routing category and the entity ids. Port of the iOS `PushEvent`.
 *
 * FCM data values are always strings (the backend `safeData` stringifies them). `MainActivity` is exported, so any app
 * can start it with arbitrary extras: every id is validated here as a canonical UUID and lowercased, and anything else
 * becomes null so it never reaches a route or a URL path (ASVS V5).
 *
 * It carries ids and categories only, never a title or a body.
 */
data class PushEvent(
    val category: String?,
    val subCategory: String?,
    val bookingId: String?,
    val threadId: String?,
    val reviewId: String?,
    val payoutId: String?,
    val eventType: String?,
) {
    companion object {
        private const val KEY_CATEGORY = "category"
        private const val KEY_SUB_CATEGORY = "sub_category"
        private const val KEY_EVENT_TYPE = "event_type"
        private const val KEY_REVIEW_ID = "review_id"
        private const val KEY_PAYOUT_ID = "payout_id"
        private val BOOKING_ID_KEYS = listOf("booking_id", "bookingId")
        private val THREAD_ID_KEYS = listOf("thread_id", "threadId", "chatThreadId", "chat_thread_id")

        /** Every key the parser reads. A map with none of these set is not a push payload. */
        val PUSH_KEYS: Set<String> =
            setOf(KEY_CATEGORY, KEY_SUB_CATEGORY, KEY_EVENT_TYPE, KEY_REVIEW_ID, KEY_PAYOUT_ID) +
                BOOKING_ID_KEYS +
                THREAD_ID_KEYS

        /** Uppercased `event_type` to category and sub_category, for payloads with no `category` (RESEARCH F-1). */
        private val EVENT_TYPE_FALLBACK: Map<String, Pair<String, String>> =
            mapOf(
                "NEW_MESSAGE" to ("message" to "new_message"),
                "NEW_BOOKING_REQUEST" to ("booking" to "requests"),
                "BOOKING_REQUEST" to ("booking" to "requests"),
                "REQUEST_CREATED" to ("booking" to "requests"),
                "BOOKING_CONFIRMED" to ("booking" to "confirmations"),
                "BOOKING_DECLINED" to ("booking" to "declines"),
                "BOOKING_CANCELLED" to ("booking" to "cancellations"),
                "BOOKING_RESCHEDULED" to ("booking" to "reschedules"),
                "BOOKING_EXPIRED" to ("booking" to "expirations"),
                "BOOKING_COMPLETED" to ("session" to "ended"),
                "REVIEW_SUBMITTED" to ("review" to "received"),
                "REFUND_ISSUED" to ("booking" to "refunds"),
                "PAYOUT_PAID" to ("payout" to "paid"),
                "PAYOUT_FAILED" to ("payout" to "failed"),
                "KYC_ACTION_REQUIRED" to ("payout" to "kyc_action"),
                "DISPUTE_CREATED" to ("payout" to "dispute"),
                "PASSWORD_CHANGED" to ("account" to "password_changed"),
                "DELETION_REQUESTED" to ("account" to "deletion_requested"),
            )

        private val UUID_REGEX =
            Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

        /**
         * Parses an FCM data map or launcher-intent extras. Returns null when the map carries none of [PUSH_KEYS], so
         * a plain launcher open is not routed.
         */
        fun from(data: Map<String, String?>): PushEvent? {
            if (PUSH_KEYS.none { !data[it].isNullOrBlank() }) return null
            val eventType = clean(data[KEY_EVENT_TYPE])
            val bookingId = firstId(data, BOOKING_ID_KEYS)
            var category = clean(data[KEY_CATEGORY])?.lowercase(Locale.ROOT)
            var subCategory = clean(data[KEY_SUB_CATEGORY])
            if (category == null) {
                val fallback = eventType?.let { EVENT_TYPE_FALLBACK[it.uppercase(Locale.ROOT)] }
                if (fallback != null) {
                    category = fallback.first
                    subCategory = fallback.second
                } else if (bookingId != null) {
                    // The cron reminder, no-show and nudge pushes carry only booking_id (RESEARCH F-1).
                    category = "booking"
                }
            }
            return PushEvent(
                category = category,
                subCategory = subCategory,
                bookingId = bookingId,
                threadId = firstId(data, THREAD_ID_KEYS),
                reviewId = canonicalUuid(data[KEY_REVIEW_ID]),
                payoutId = canonicalUuid(data[KEY_PAYOUT_ID]),
                eventType = eventType,
            )
        }

        /**
         * Parses a `GET /notifications` row: the push `data` map comes back as `meta_data` (RESEARCH F-8), and the row
         * columns [category] and [subCategory] override it when non-blank. Only string values of [metaData] are read.
         * The server never returns `message` rows, so the thread aliases exist for parity only.
         */
        fun fromRow(
            category: String?,
            subCategory: String?,
            metaData: Map<String, Any?>?,
        ): PushEvent? {
            val data = LinkedHashMap<String, String?>()
            metaData?.forEach { (key, value) -> if (value is String) data[key] = value }
            if (!category.isNullOrBlank()) data[KEY_CATEGORY] = category
            if (!subCategory.isNullOrBlank()) data[KEY_SUB_CATEGORY] = subCategory
            return from(data)
        }

        private fun clean(raw: String?): String? = raw?.trim()?.takeIf { it.isNotEmpty() }

        private fun firstId(
            data: Map<String, String?>,
            keys: List<String>,
        ): String? = keys.firstNotNullOfOrNull { canonicalUuid(data[it]) }

        private fun canonicalUuid(raw: String?): String? {
            val value = raw?.trim() ?: return null
            return if (UUID_REGEX.matches(value)) value.lowercase(Locale.ROOT) else null
        }
    }
}

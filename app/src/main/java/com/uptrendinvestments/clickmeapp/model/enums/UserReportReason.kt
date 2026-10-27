package com.uptrendinvestments.clickmeapp.model.enums

/**
 * The six backend report reasons (`REPORT_REASONS` in `moderation.schema.js`, USER-REPORTING section 1). [wire] is
 * the exact value sent in the request body. Display labels live with the UI (plan 12-07); the iOS
 * `defaultReasons` labels are not ported because the backend rejects them (carried decision, SAFE-01).
 */
enum class UserReportReason(
    val wire: String,
) {
    Harassment("harassment"),
    Spam("spam"),
    InappropriateContent("inappropriate_content"),
    ScamOrFraud("scam_or_fraud"),
    Impersonation("impersonation"),
    Other("other"),
    ;

    companion object {
        private val byWire: Map<String, UserReportReason> = entries.associateBy { it.wire }

        /** Returns the matching reason, or null for an unknown or missing wire value. */
        fun fromWire(raw: String?): UserReportReason? = raw?.let(byWire::get)
    }
}

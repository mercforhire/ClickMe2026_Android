package com.uptrendinvestments.clickmeapp.screens.clientmode.payments

import androidx.annotation.StringRes
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.responses.ClientTransactionItem
import com.uptrendinvestments.clickmeapp.util.Money
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val ROW_DATE_PATTERN = "MMM d, yyyy"

/**
 * The payment status of one `GET /client/transactions` row, with its label. This is this endpoint's own vocabulary:
 * do not share it with `/client/bookings/:id`, whose 4-value helper collapses a declined card to "none"
 * (`CLIENT-TRANSACTIONS.md` section 3). The raw value is matched exactly, so the booking spelling of cancelled is
 * not this table's "canceled" and maps to [Unknown].
 */
enum class ClientPaymentStatus(
    @StringRes val label: Int,
) {
    Paid(R.string.payment_history_status_paid),
    AuthorizedNotCharged(R.string.payment_history_status_authorized),
    Scheduled(R.string.payment_history_status_scheduled),
    Processing(R.string.payment_history_status_processing),
    PaymentNeeded(R.string.payment_history_status_payment_needed),
    Refunded(R.string.payment_history_status_refunded),
    PartiallyRefunded(R.string.payment_history_status_partially_refunded),
    PaymentFailed(R.string.payment_history_status_failed),
    Canceled(R.string.payment_history_status_canceled),
    AuthExpired(R.string.payment_history_status_auth_expired),
    Processed(R.string.payment_history_status_processed),
    NoPayment(R.string.payment_history_status_no_payment),
    Unknown(R.string.payouts_activity_none),
}

/** The pill tint family for a [ClientPaymentStatus]; the screen maps each to a theme color. */
enum class ClientPaymentPillTint { Primary, Tertiary, Error, Neutral }

/** Paid is the primary tint, in-flight and held states tertiary, failures error, everything else neutral. */
val ClientPaymentStatus.tint: ClientPaymentPillTint
    get() =
        when (this) {
            ClientPaymentStatus.Paid -> ClientPaymentPillTint.Primary
            ClientPaymentStatus.AuthorizedNotCharged,
            ClientPaymentStatus.Scheduled,
            ClientPaymentStatus.Processing,
            ClientPaymentStatus.PaymentNeeded,
            -> ClientPaymentPillTint.Tertiary
            ClientPaymentStatus.PaymentFailed,
            ClientPaymentStatus.Canceled,
            ClientPaymentStatus.AuthExpired,
            -> ClientPaymentPillTint.Error
            ClientPaymentStatus.Refunded,
            ClientPaymentStatus.PartiallyRefunded,
            ClientPaymentStatus.Processed,
            ClientPaymentStatus.NoPayment,
            ClientPaymentStatus.Unknown,
            -> ClientPaymentPillTint.Neutral
        }

/**
 * Maps the raw server [status] to a [ClientPaymentStatus]. `none` with an [amount] above 0 is a legacy row whose real
 * charge was never backfilled and reads Processed; otherwise No payment. Any other value, including the empty string,
 * is [ClientPaymentStatus.Unknown] and never throws.
 */
fun clientPaymentStatusOf(
    status: String,
    amount: Long?,
): ClientPaymentStatus =
    when (status) {
        "captured" -> ClientPaymentStatus.Paid
        "requires_capture" -> ClientPaymentStatus.AuthorizedNotCharged
        "scheduled" -> ClientPaymentStatus.Scheduled
        "processing" -> ClientPaymentStatus.Processing
        "requires_payment_method" -> ClientPaymentStatus.PaymentNeeded
        "refunded" -> ClientPaymentStatus.Refunded
        "partially_refunded" -> ClientPaymentStatus.PartiallyRefunded
        "failed" -> ClientPaymentStatus.PaymentFailed
        "canceled" -> ClientPaymentStatus.Canceled
        "auth_expired" -> ClientPaymentStatus.AuthExpired
        "none" -> if ((amount ?: 0L) > 0L) ClientPaymentStatus.Processed else ClientPaymentStatus.NoPayment
        else -> ClientPaymentStatus.Unknown
    }

/**
 * True when some money came back but not all of it: `0 < refundedAmount < amount` with a positive amount. A null or
 * zero amount cannot be partially refunded. The refund timestamp is never read (it is always null).
 */
fun isPartialRefund(item: ClientTransactionItem): Boolean {
    val amount = item.amount ?: return false
    return amount > 0L && item.refundedAmount > 0L && item.refundedAmount < amount
}

/**
 * The amount text in the currency's own decimals (JPY none, CAD two). A null [amount] has no text, so the row shows a
 * dash rather than a false zero; 0 formats as zero.
 */
fun clientAmountText(
    amount: Long?,
    currency: String,
    locale: Locale,
): String? = amount?.let { Money.format(it, currency, locale) }

/** The row's date: the session date, else when the ledger row was created, as "MMM d, yyyy" in [zone]. */
fun clientRowDate(
    item: ClientTransactionItem,
    zone: ZoneId,
    locale: Locale,
): String =
    DateTimeFormatter
        .ofPattern(ROW_DATE_PATTERN, locale)
        .withZone(zone)
        .format(item.sessionDate ?: item.createdAt)

/** A fully refunded row mutes its amount; a partial refund and every other status do not. */
fun amountIsMuted(status: String): Boolean = status == "refunded"

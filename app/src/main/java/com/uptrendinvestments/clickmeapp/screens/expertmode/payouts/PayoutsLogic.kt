package com.uptrendinvestments.clickmeapp.screens.expertmode.payouts

import com.uptrendinvestments.clickmeapp.model.responses.EarningsTransactionItem
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingItem
import com.uptrendinvestments.clickmeapp.model.responses.PayoutSummaryData
import com.uptrendinvestments.clickmeapp.screens.expertmode.dashboard.PayoutOnboarding
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * What the Payouts screen shows once a load has answered (port of the iOS `PayoutsView` branches). The state is derived
 * from the Connect onboarding result, never assumed: nothing here says payouts are ready while setup is incomplete.
 */
sealed interface PayoutsContent {
    /** Stripe Connect setup is incomplete. [useRefreshLink] is true when a connected account already exists. */
    data class SetupRequired(
        val useRefreshLink: Boolean,
    ) : PayoutsContent

    /** Onboarded, no earnings yet, and [heldCount] upcoming sessions whose payment is still held. */
    data class OnHold(
        val heldCount: Int,
    ) : PayoutsContent

    /** Onboarded, no earnings yet, nothing held. */
    data object ReadyNoEarnings : PayoutsContent

    /** Onboarded with a balance. [eligible] is true only when the available amount is above zero. */
    data class Loaded(
        val summary: PayoutSummaryData,
        val eligible: Boolean,
    ) : PayoutsContent
}

/**
 * Picks the content for a finished load, or null when neither the Connect status nor the summary answered (the caller
 * shows the Failed state). A negative available balance is a [PayoutsContent.Loaded] and never clamped.
 */
fun selectPayoutsContent(
    onboarding: PayoutOnboarding,
    summary: PayoutSummaryData?,
    heldCount: Int,
): PayoutsContent? =
    when (onboarding) {
        is PayoutOnboarding.Required -> PayoutsContent.SetupRequired(onboarding.useRefreshLink)
        PayoutOnboarding.Unknown -> null
        PayoutOnboarding.Onboarded ->
            if (summary == null || (summary.availableAmount == 0L && summary.pendingAmount == 0L)) {
                if (heldCount > 0) PayoutsContent.OnHold(heldCount) else PayoutsContent.ReadyNoEarnings
            } else {
                PayoutsContent.Loaded(summary, eligible = summary.availableAmount > 0)
            }
    }

/**
 * Upcoming booking statuses whose payment authorization is still held (iOS `fetchUpcomingHeldCount`, RESEARCH A8):
 * money does not move until the session completes.
 */
val HELD_BOOKING_STATUSES: Set<String> = setOf("pending_approval", "confirmed", "pending_reschedule", "in_progress")

/** The number of [items] whose status is in [HELD_BOOKING_STATUSES]. */
fun heldBookingCount(items: List<ExpertBookingItem>): Int = items.count { it.statusCode in HELD_BOOKING_STATUSES }

/** How the payout method row reads. */
sealed interface PayoutMethodLabel {
    data object BankTransfer : PayoutMethodLabel

    /** Any other server value, prettified. */
    data class Raw(
        val text: String,
    ) : PayoutMethodLabel

    data object None : PayoutMethodLabel
}

/** `bank_transfer` is "Bank Transfer"; another value has underscores turned into spaces and a capital first letter. */
fun payoutMethodLabel(raw: String?): PayoutMethodLabel {
    val value = raw?.trim().orEmpty()
    return when {
        value.isEmpty() -> PayoutMethodLabel.None
        value == "bank_transfer" -> PayoutMethodLabel.BankTransfer
        else -> PayoutMethodLabel.Raw(value.replace('_', ' ').replaceFirstChar { it.titlecase(Locale.ROOT) })
    }
}

private val NextPayoutFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy")

/** The server's `yyyy-MM-dd` next payout date as `MMMM d, yyyy`, or null when absent or malformed (manual schedule). */
fun nextPayoutDateText(
    date: String?,
    locale: Locale,
): String? =
    date?.let {
        try {
            LocalDate.parse(it).format(NextPayoutFormat.withLocale(locale))
        } catch (_: DateTimeParseException) {
            null
        }
    }

/** How many ledger rows the Payouts "Recent activity" preview shows (iOS `previewRows`). */
const val PREVIEW_ROW_COUNT = 5

/** The first [PREVIEW_ROW_COUNT] rows of the ledger, in server order (newest first). */
fun previewRows(rows: List<EarningsTransactionItem>): List<EarningsTransactionItem> = rows.take(PREVIEW_ROW_COUNT)

/** "See all activity" shows when the server has more rows or the first page held more than the preview shows. */
fun shouldShowSeeAll(
    rows: List<EarningsTransactionItem>,
    hasMore: Boolean,
): Boolean = hasMore || rows.size > PREVIEW_ROW_COUNT

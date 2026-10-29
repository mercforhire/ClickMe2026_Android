package com.uptrendinvestments.clickmeapp.screens.expertmode.payouts

import com.uptrendinvestments.clickmeapp.screens.expertmode.dashboard.PayoutOnboarding
import com.uptrendinvestments.clickmeapp.testing.earningsTransaction
import com.uptrendinvestments.clickmeapp.testing.expertBookingItem
import com.uptrendinvestments.clickmeapp.testing.payoutSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class PayoutsLogicTest {
    @Test
    fun stateTable() {
        assertEquals(
            PayoutsContent.SetupRequired(true),
            selectPayoutsContent(PayoutOnboarding.Required(useRefreshLink = true), null, 0),
        )
        assertEquals(
            PayoutsContent.SetupRequired(false),
            selectPayoutsContent(PayoutOnboarding.Required(useRefreshLink = false), payoutSummary(), 3),
        )
        assertEquals(PayoutsContent.OnHold(2), selectPayoutsContent(PayoutOnboarding.Onboarded, null, 2))
        assertEquals(
            PayoutsContent.ReadyNoEarnings,
            selectPayoutsContent(PayoutOnboarding.Onboarded, payoutSummary(available = 0L, pending = 0L), 0),
        )
        assertEquals(
            PayoutsContent.OnHold(1),
            selectPayoutsContent(PayoutOnboarding.Onboarded, payoutSummary(available = 0L, pending = 0L), 1),
        )
        val pendingOnly = payoutSummary(available = 0L, pending = 500L)
        assertEquals(
            PayoutsContent.Loaded(pendingOnly, eligible = false),
            selectPayoutsContent(PayoutOnboarding.Onboarded, pendingOnly, 5),
        )
        val negative = payoutSummary(available = -500L, pending = 0L)
        assertEquals(
            PayoutsContent.Loaded(negative, eligible = false),
            selectPayoutsContent(PayoutOnboarding.Onboarded, negative, 0),
        )
        val positive = payoutSummary(available = 100L)
        assertEquals(
            PayoutsContent.Loaded(positive, eligible = true),
            selectPayoutsContent(PayoutOnboarding.Onboarded, positive, 0),
        )
        assertNull(selectPayoutsContent(PayoutOnboarding.Unknown, null, 0))
    }

    @Test
    fun heldCountUsesFourStatuses() {
        val items =
            listOf("confirmed", "pending_approval", "pending_reschedule", "in_progress", "completed", "cancelled")
                .mapIndexed { index, status -> expertBookingItem(id = "b$index", statusCode = status) }
        assertEquals(4, heldBookingCount(items))
        assertEquals(0, heldBookingCount(emptyList()))
    }

    @Test
    fun labels() {
        assertEquals(PayoutMethodLabel.BankTransfer, payoutMethodLabel("bank_transfer"))
        assertEquals(PayoutMethodLabel.Raw("Instant card"), payoutMethodLabel("instant_card"))
        assertEquals(PayoutMethodLabel.None, payoutMethodLabel(null))
        assertEquals(PayoutMethodLabel.None, payoutMethodLabel("  "))
        assertEquals("November 7, 2026", nextPayoutDateText("2026-11-07", Locale.US))
        assertNull(nextPayoutDateText(null, Locale.US))
        assertNull(nextPayoutDateText("bad", Locale.US))
    }

    private fun rows(count: Int) = (1..count).map { earningsTransaction(id = "e$it") }

    @Test
    fun previewAndSeeAllBoundary() {
        assertEquals(5, PREVIEW_ROW_COUNT)
        assertTrue(previewRows(rows(0)).isEmpty())
        assertFalse(shouldShowSeeAll(rows(0), hasMore = false))

        assertEquals(5, previewRows(rows(5)).size)
        assertFalse(shouldShowSeeAll(rows(5), hasMore = false))

        assertEquals(rows(5), previewRows(rows(6)))
        assertTrue(shouldShowSeeAll(rows(6), hasMore = false))

        assertEquals(3, previewRows(rows(3)).size)
        assertTrue(shouldShowSeeAll(rows(3), hasMore = true))
    }
}

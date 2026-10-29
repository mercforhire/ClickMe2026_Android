package com.uptrendinvestments.clickmeapp.screens.expertmode.payouts

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.uptrendinvestments.clickmeapp.model.responses.EarningsTransactionItem
import com.uptrendinvestments.clickmeapp.testing.earningsTransaction
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.util.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class EarningsTransactionRowTest {
    @get:Rule
    val rule = createComposeRule()

    private val us = Locale.US

    private fun show(item: EarningsTransactionItem) {
        rule.setContent {
            ClickMeTheme {
                EarningsTransactionRow(item = item, zone = ZoneOffset.UTC, locale = us)
            }
        }
    }

    @Test
    fun statusLabels() {
        assertEquals(EarningsStatus.Pending, earningsStatusOf("pending"))
        assertEquals(EarningsStatus.Processing, earningsStatusOf("processing"))
        assertEquals(EarningsStatus.Settled, earningsStatusOf("settled_direct"))
        assertEquals(EarningsStatus.Processed, earningsStatusOf("paid"))
        assertEquals(EarningsStatus.Failed, earningsStatusOf("failed"))
        assertEquals(EarningsStatus.Unknown, earningsStatusOf("mystery"))
        assertEquals(EarningsStatus.Unknown, earningsStatusOf(""))
        assertEquals(EarningsStatus.Unknown, earningsStatusOf(null))
        assertNotEquals(EarningsStatus.Pending, earningsStatusOf("settled_direct"))
    }

    @Test
    fun statusIsTrimmedAndCaseInsensitive() {
        assertEquals(EarningsStatus.Settled, earningsStatusOf("  Settled_Direct "))
        assertEquals(EarningsStatus.Failed, earningsStatusOf("FAILED"))
    }

    @Test
    fun nullFieldsRenderDash() {
        val amount = Money.format(6375L, "CAD", us)
        show(earningsTransaction(topic = null, client = null))

        rule
            .onNodeWithTag(EARNINGS_ROW_TAG)
            .assertContentDescriptionEquals("—, —, Jul 30, 2026, Settled, $amount")
        rule.onNodeWithText("— · Jul 30, 2026").assertExists()
    }

    @Test
    fun blankTextCountsAsMissing() {
        val amount = Money.format(6375L, "CAD", us)
        show(earningsTransaction(topic = "  ", client = ""))

        rule
            .onNodeWithTag(EARNINGS_ROW_TAG)
            .assertContentDescriptionEquals("—, —, Jul 30, 2026, Settled, $amount")
    }

    @Test
    fun sessionDateFallsBackToCreatedAt() {
        val item = earningsTransaction(sessionDate = null, createdAt = Instant.parse("2026-07-29T10:00:00Z"))

        assertEquals("Jul 29, 2026", earningsRowDate(item, ZoneOffset.UTC, us))
        assertEquals("Jul 30, 2026", earningsRowDate(earningsTransaction(), ZoneOffset.UTC, us))
    }

    @Test
    fun dateUsesTheGivenZone() {
        val item = earningsTransaction(sessionDate = Instant.parse("2026-07-30T02:00:00Z"))

        assertEquals("Jul 30, 2026", earningsRowDate(item, ZoneOffset.UTC, us))
        assertEquals("Jul 29, 2026", earningsRowDate(item, ZoneOffset.ofHours(-5), us))
    }

    @Test
    fun pillTints() {
        assertEquals(EarningsPillTint.Tertiary, EarningsStatus.Pending.tint)
        assertEquals(EarningsPillTint.Primary, EarningsStatus.Settled.tint)
        assertEquals(EarningsPillTint.Primary, EarningsStatus.Processed.tint)
        assertEquals(EarningsPillTint.Error, EarningsStatus.Failed.tint)
        assertEquals(EarningsPillTint.Neutral, EarningsStatus.Processing.tint)
        assertEquals(EarningsPillTint.Neutral, EarningsStatus.Unknown.tint)
    }

    @Test
    fun rowSemantics() {
        val amount = Money.format(6375L, "CAD", us)
        show(earningsTransaction())

        val node = rule.onNodeWithTag(EARNINGS_ROW_TAG)
        node.assertContentDescriptionEquals("Career Advice, Liam Carter, Jul 30, 2026, Settled, $amount")
        val config = node.fetchSemanticsNode().config
        assertNull(config.getOrNull(SemanticsActions.OnClick))
        assertNull(config.getOrNull(SemanticsProperties.Role))
        rule.onNodeWithText("Settled").assertExists()
    }

    @Test
    fun negativeNetIsFormatted() {
        show(earningsTransaction(net = -500L, currency = "CAD"))

        rule.onNodeWithText(Money.format(-500L, "CAD", us)).assertExists()
    }
}

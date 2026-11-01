package com.uptrendinvestments.clickmeapp.screens.clientmode.reviews

import com.uptrendinvestments.clickmeapp.screens.clientmode.reviews.subviews.StarKind
import com.uptrendinvestments.clickmeapp.screens.clientmode.reviews.subviews.starKinds
import org.junit.Assert.assertEquals
import org.junit.Test

class ExpertReviewsStarRowTest {
    private val full = StarKind.Full
    private val half = StarKind.Half
    private val empty = StarKind.Empty

    @Test
    fun threeAndAHalfGivesThreeFullOneHalfOneEmpty() {
        assertEquals(listOf(full, full, full, half, empty), starKinds(3.5))
    }

    @Test
    fun zeroIsFiveEmpty() {
        assertEquals(List(5) { empty }, starKinds(0.0))
    }

    @Test
    fun fiveIsFiveFull() {
        assertEquals(List(5) { full }, starKinds(5.0))
    }

    @Test
    fun belowTheHalfThresholdStaysEmpty() {
        assertEquals(listOf(full, full, full, full, empty), starKinds(4.2))
    }

    @Test
    fun fourAndAHalfEndsWithHalf() {
        assertEquals(half, starKinds(4.5).last())
    }
}

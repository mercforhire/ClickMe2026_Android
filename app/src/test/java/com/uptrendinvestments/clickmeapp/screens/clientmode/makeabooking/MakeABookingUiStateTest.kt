package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking

import com.uptrendinvestments.clickmeapp.model.responses.TopicOfDiscussionDto
import com.uptrendinvestments.clickmeapp.model.responses.TopicPriceDto
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.util.PriceDisplay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class MakeABookingUiStateTest {
    private val now = Instant.parse("2026-10-15T12:00:00Z")
    private val today = LocalDate.of(2026, 10, 15)
    private val futureStart = Instant.parse("2026-10-15T15:00:00Z")
    private val pastStart = Instant.parse("2026-10-15T09:00:00Z")

    private val free = BookingTopicUi("free", "Intro", 30, null, PriceDisplay.Free, isFree = true)
    private val paid = BookingTopicUi("paid", "Deep dive", 60, null, PriceDisplay.Paid("$50.00"), isFree = false)

    private fun state(
        topics: TopicsState = TopicsState.Loaded(listOf(free, paid)),
        selectedTopicId: String? = null,
        availability: AvailabilityState =
            AvailabilityState.Loaded(
                mapOf(
                    today to
                        listOf(
                            BookingTimeSlot(pastStart, null, true),
                            BookingTimeSlot(futureStart, null, true),
                        ),
                ),
            ),
        selectedSlotStart: Instant? = null,
        isSubmitting: Boolean = false,
        displayedMonth: YearMonth = YearMonth.from(today),
    ) = MakeABookingUiState(
        expertId = "e1",
        expertName = "Expert",
        expertTitle = "Coach",
        expertImageUrl = null,
        zone = ZoneId.of("America/Toronto"),
        today = today,
        now = now,
        selectedDate = today,
        displayedMonth = displayedMonth,
        topics = topics,
        selectedTopicId = selectedTopicId,
        availability = availability,
        selectedSlotStart = selectedSlotStart,
        isSubmitting = isSubmitting,
    )

    @Test
    fun free_dto_without_duration_defaults_to_30() {
        val ui =
            TopicOfDiscussionDto(id = "t1", title = "Intro", price = TopicPriceDto(isFree = true))
                .toBookingTopicUi()
        assertEquals(30, ui.durationMins)
        assertTrue(ui.isFree)
        assertEquals(PriceDisplay.Free, ui.price)
    }

    @Test
    fun paid_dto_maps_to_paid_price() {
        val ui =
            TopicOfDiscussionDto(
                id = "t2",
                title = "Deep dive",
                durationMins = 45,
                description = "  ",
                price = TopicPriceDto(amount = 5000, currency = "USD", isFree = false),
            ).toBookingTopicUi()
        assertFalse(ui.isFree)
        assertEquals(45, ui.durationMins)
        assertNull(ui.description)
        assertTrue(ui.price is PriceDisplay.Paid)
    }

    @Test
    fun selected_topic_is_the_loaded_topic_with_matching_id() {
        assertEquals(free, state(selectedTopicId = "free").selectedTopic)
        assertNull(state(selectedTopicId = "missing").selectedTopic)
        assertNull(state(selectedTopicId = null).selectedTopic)
    }

    @Test
    fun selected_topic_is_null_while_loading_or_failed() {
        assertNull(state(topics = TopicsState.Loading, selectedTopicId = "free").selectedTopic)
        val failed = TopicsState.Failed(FieldError.Text("x"))
        assertNull(state(topics = failed, selectedTopicId = "free").selectedTopic)
    }

    @Test
    fun slots_for_selected_date_and_selected_slot() {
        val s = state(selectedSlotStart = futureStart)
        assertEquals(2, s.slotsForSelectedDate.size)
        assertEquals(futureStart, s.selectedSlot?.start)
        assertNull(state(selectedSlotStart = Instant.parse("2026-10-15T20:00:00Z")).selectedSlot)
        assertTrue(state(availability = AvailabilityState.Loading).slotsForSelectedDate.isEmpty())
    }

    @Test
    fun open_days_is_empty_unless_loaded() {
        assertTrue(state(availability = AvailabilityState.Loading).openDays.isEmpty())
        assertTrue(state(availability = AvailabilityState.Failed(FieldError.Text("x"))).openDays.isEmpty())
        assertEquals(setOf(today), state().openDays)
    }

    @Test
    fun can_go_to_previous_month_only_after_current_month() {
        assertFalse(state(displayedMonth = YearMonth.of(2026, 10)).canGoToPreviousMonth)
        assertTrue(state(displayedMonth = YearMonth.of(2026, 11)).canGoToPreviousMonth)
    }

    @Test
    fun disabled_reason_no_topic() {
        assertEquals(BookDisabledReason.NoTopic, state(selectedSlotStart = futureStart).bookDisabledReason)
    }

    @Test
    fun disabled_reason_paid_topic_wins_over_a_selected_slot() {
        val s = state(selectedTopicId = "paid", selectedSlotStart = futureStart)
        assertEquals(BookDisabledReason.PaidTopic, s.bookDisabledReason)
    }

    @Test
    fun disabled_reason_no_slot_for_free_topic_without_slot() {
        assertEquals(BookDisabledReason.NoSlot, state(selectedTopicId = "free").bookDisabledReason)
    }

    @Test
    fun disabled_reason_no_slot_when_selected_slot_is_in_the_past() {
        val s = state(selectedTopicId = "free", selectedSlotStart = pastStart)
        assertEquals(BookDisabledReason.NoSlot, s.bookDisabledReason)
    }

    @Test
    fun no_disabled_reason_with_free_topic_and_selectable_slot() {
        val s = state(selectedTopicId = "free", selectedSlotStart = futureStart)
        assertNull(s.bookDisabledReason)
    }

    @Test
    fun submitting_keeps_no_disabled_reason_so_book_now_keeps_its_filled_look() {
        // While submitting, Book Now stays enabled-looking and ignores taps itself (D-11); the reason stays null.
        val s = state(selectedTopicId = "free", selectedSlotStart = futureStart, isSubmitting = true)
        assertNull(s.bookDisabledReason)
    }
}

package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking

import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.model.responses.AvailabilityDayDto
import com.uptrendinvestments.clickmeapp.model.responses.AvailabilitySlotDto
import com.uptrendinvestments.clickmeapp.model.responses.ExpertAvailabilityData
import com.uptrendinvestments.clickmeapp.model.responses.PublicProfileDetailsData
import com.uptrendinvestments.clickmeapp.model.responses.TopicOfDiscussionDto
import com.uptrendinvestments.clickmeapp.model.responses.TopicPriceDto
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.ExpertProfileRepository
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MutableClock
import java.time.Clock
import java.time.Instant

/** The fixed "now" of the booking tests: 2026-10-22 12:00 in America/Toronto (EDT). */
const val NOW = "2026-10-22T16:00:00Z"

private const val SLOT_MINUTES = 30L

fun bookingKey(): BookingKey = BookingKey("e1", "Ada Lovelace", "Engineer", null)

fun topicDto(
    id: String,
    isFree: Boolean,
    durationMins: Int? = 30,
    amount: Long? = null,
    currency: String? = null,
): TopicOfDiscussionDto =
    TopicOfDiscussionDto(
        id = id,
        title = "Topic $id",
        durationMins = durationMins,
        price = TopicPriceDto(amount = amount, currency = currency, isFree = isFree),
    )

fun detailsWith(vararg topics: TopicOfDiscussionDto): PublicProfileDetailsData =
    PublicProfileDetailsData(topicsOfDiscussion = topics.toList())

fun slotDto(
    startUtc: String,
    available: Boolean? = true,
    held: Boolean? = null,
): AvailabilitySlotDto {
    val start = Instant.parse(startUtc)
    return AvailabilitySlotDto(
        startUtc = start,
        endUtc = start.plusSeconds(SLOT_MINUTES * 60),
        available = available,
        held = held,
    )
}

/** One day holding all [slots]; the day's date is irrelevant because slots are regrouped by start instant. */
fun availabilityOf(
    vararg slots: AvailabilitySlotDto,
    expertTimezone: String = "America/Vancouver",
): ExpertAvailabilityData =
    ExpertAvailabilityData(
        expertTimezone = expertTimezone,
        days = listOf(AvailabilityDayDto(date = "2026-10-22", slots = slots.toList())),
    )

fun newViewModel(
    api: FakeClickMeApi,
    savedState: SavedStateHandle = SavedStateHandle(),
    clock: Clock = MutableClock(Instant.parse(NOW)),
): MakeABookingViewModel =
    MakeABookingViewModel(bookingKey(), savedState, ExpertProfileRepository(api), BookingRepository(api), clock)

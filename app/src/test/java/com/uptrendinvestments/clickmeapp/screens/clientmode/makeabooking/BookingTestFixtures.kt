package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking

import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.managers.MonotonicClock
import com.uptrendinvestments.clickmeapp.model.responses.AvailabilityDayDto
import com.uptrendinvestments.clickmeapp.model.responses.AvailabilitySlotDto
import com.uptrendinvestments.clickmeapp.model.responses.ConfirmBookingData
import com.uptrendinvestments.clickmeapp.model.responses.ExpertAvailabilityData
import com.uptrendinvestments.clickmeapp.model.responses.PaymentIntentResponse
import com.uptrendinvestments.clickmeapp.model.responses.PaymentMethodItem
import com.uptrendinvestments.clickmeapp.model.responses.PaymentMethodSetupData
import com.uptrendinvestments.clickmeapp.model.responses.PaymentMethodsData
import com.uptrendinvestments.clickmeapp.model.responses.PublicProfileDetailsData
import com.uptrendinvestments.clickmeapp.model.responses.TopicOfDiscussionDto
import com.uptrendinvestments.clickmeapp.model.responses.TopicPriceDto
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.ExpertProfileRepository
import com.uptrendinvestments.clickmeapp.screens.settings.paymentmethods.PaymentMethodsRepository
import com.uptrendinvestments.clickmeapp.stripe.StripePaymentMethodReader
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.FakeCompletedBookingStore
import com.uptrendinvestments.clickmeapp.testing.FakeStripePaymentMethodReader
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

fun cardDto(
    id: String,
    isDefault: Boolean = false,
    brand: String = "visa",
    last4: String = "4242",
    expMonth: Int = 9,
    expYear: Int = 2028,
): PaymentMethodItem =
    PaymentMethodItem(
        id = id,
        brand = brand,
        last4 = last4,
        expMonth = expMonth,
        expYear = expYear,
        isDefault = isDefault,
    )

fun FakeClickMeApi.stubCards(vararg cards: PaymentMethodItem) {
    getPaymentMethodsHandler = { PaymentMethodsData(cards.toList()) }
}

fun newViewModel(
    api: FakeClickMeApi,
    savedState: SavedStateHandle = SavedStateHandle(),
    clock: Clock = MutableClock(Instant.parse(NOW)),
    completedBookings: CompletedBookingStore = FakeCompletedBookingStore(),
    reader: StripePaymentMethodReader = FakeStripePaymentMethodReader(),
    monotonicClock: MonotonicClock = MonotonicClock { 0L },
): MakeABookingViewModel =
    MakeABookingViewModel(
        bookingKey(),
        savedState,
        ExpertProfileRepository(api),
        BookingRepository(api),
        PaymentMethodsRepository(api),
        completedBookings,
        clock,
        reader,
        monotonicClock,
    )

/**
 * The paid booking context: topics [free t1 (60 min), paid t2 (60 min, 5000 CAD)], two slots (2026-10-22T20:00Z and
 * 2026-10-23T01:00Z), cards [pm_default (default), pm_other], and a successful payment-intent, setup-intent and
 * confirm.
 */
fun paidFixture(api: FakeClickMeApi) {
    api.expertDetailsHandler = {
        detailsWith(
            topicDto("t1", isFree = true, durationMins = 60),
            topicDto("t2", isFree = false, durationMins = 60, amount = 5000, currency = "CAD"),
        )
    }
    api.expertAvailabilityHandler = { _, _ ->
        availabilityOf(slotDto("2026-10-22T20:00:00Z"), slotDto("2026-10-23T01:00:00Z"))
    }
    api.stubCards(cardDto("pm_default", isDefault = true), cardDto("pm_other"))
    api.paymentIntentHandler = { PaymentIntentResponse("pi_1", "pi_1_secret_x", "cad") }
    api.setupIntentHandler = { PaymentMethodSetupData("seti_1_secret_x", "cus_1", "ek_1") }
    api.confirmBookingHandler = { ConfirmBookingData("b9") }
}

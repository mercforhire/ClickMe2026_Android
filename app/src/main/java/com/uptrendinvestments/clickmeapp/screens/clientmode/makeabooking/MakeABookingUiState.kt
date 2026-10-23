package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking

import androidx.annotation.StringRes
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.enums.MeetingType
import com.uptrendinvestments.clickmeapp.model.responses.TopicOfDiscussionDto
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.util.PriceDisplay
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** D-11: a topic without a `duration_mins` is booked as a 30 minute session. */
const val DEFAULT_DURATION_MINS = 30

/** A topic the client can pick on the booking screen. Mirrors iOS `BookingTopic`. */
data class BookingTopicUi(
    val id: String,
    val title: String,
    val durationMins: Int,
    val description: String?,
    val price: PriceDisplay,
    val isFree: Boolean,
)

fun TopicOfDiscussionDto.toBookingTopicUi(): BookingTopicUi =
    BookingTopicUi(
        id = id,
        title = title,
        durationMins = durationMins ?: DEFAULT_DURATION_MINS,
        description = description?.takeIf { it.isNotBlank() },
        price = PriceDisplay.of(price.isFree, price.amount, price.currency),
        isFree = price.isFree,
    )

sealed interface TopicsState {
    data object Loading : TopicsState

    data class Loaded(
        val topics: List<BookingTopicUi>,
    ) : TopicsState

    data class Failed(
        val message: FieldError,
    ) : TopicsState
}

sealed interface AvailabilityState {
    data object Loading : AvailabilityState

    data class Loaded(
        val slotsByDate: Map<LocalDate, List<BookingTimeSlot>>,
    ) : AvailabilityState

    data class Failed(
        val message: FieldError,
    ) : AvailabilityState
}

/** Why the booking CTA is disabled; each maps to a UI-SPEC CTA `stateDescription`. */
enum class BookDisabledReason { NoTopic, CheckingPaymentMethods, NoSlot }

/** What the enabled CTA does (D-08): book, or send a paid-topic client without a saved card to Payment Methods. */
enum class BookCtaAction { Book, AddPaymentMethod }

/**
 * Everything the Make a Booking screen renders, plus the rules derived from it.
 *
 * [now] is stamped by the ViewModel from the injected Clock on every load and user action, so past-slot
 * dimming (D-04) and the open-day dots (D-03) are deterministic in tests. [zone] is the viewer's zone that [today]
 * and the slot grouping were derived from; every formatter must use it (never `ZoneId.systemDefault()` directly), so
 * labels and grouping always agree after a zone change. [meetingType] defaults to In-app
 * Voice (D-08); both meeting types are always offered. The expert fields come from `BookingKey` and have no
 * loading state. [dialogTitle] titles the [dialogError] dialog: "Couldn't book" for a booking that failed, and a
 * neutral title for a notice where no booking was attempted (WR-02).
 */
data class MakeABookingUiState(
    val expertId: String,
    val expertName: String,
    val expertTitle: String,
    val expertImageUrl: String?,
    val zone: ZoneId,
    val today: LocalDate,
    val now: Instant,
    val selectedDate: LocalDate,
    val displayedMonth: YearMonth,
    val topics: TopicsState = TopicsState.Loading,
    val selectedTopicId: String? = null,
    val availability: AvailabilityState = AvailabilityState.Loading,
    val expertTimezone: String? = null,
    val selectedSlotStart: Instant? = null,
    val meetingType: MeetingType = MeetingType.InAppVoice,
    val notes: String = "",
    val isSubmitting: Boolean = false,
    val paymentMethods: PaymentMethodsAvailability = PaymentMethodsAvailability.Unknown,
    val dialogError: FieldError? = null,
    @StringRes val dialogTitle: Int = R.string.make_a_booking_error_title,
    val completedBookingId: String? = null,
) {
    /** The loaded topics, or empty while loading or failed. */
    val topicList: List<BookingTopicUi>
        get() = (topics as? TopicsState.Loaded)?.topics.orEmpty()

    val selectedTopic: BookingTopicUi?
        get() = selectedTopicId?.let { id -> topicList.firstOrNull { it.id == id } }

    val slotsForSelectedDate: List<BookingTimeSlot>
        get() = (availability as? AvailabilityState.Loaded)?.slotsByDate?.get(selectedDate).orEmpty()

    val selectedSlot: BookingTimeSlot?
        get() = selectedSlotStart?.let { start -> slotsForSelectedDate.firstOrNull { it.start == start } }

    val openDays: Set<LocalDate>
        get() = (availability as? AvailabilityState.Loaded)?.slotsByDate?.openDays(now).orEmpty()

    val canGoToPreviousMonth: Boolean
        get() = canGoToPreviousMonth(displayedMonth, today)

    /** False when [selectedDate] is outside the loaded months, so the slots card asks for a day instead (IN-08). */
    val isSelectedDateLoaded: Boolean
        get() = isInLoadedWindow(selectedDate, displayedMonth)

    /**
     * D-08: no topic first. For a paid topic, unknown saved cards disable the CTA, and no cards leave it enabled
     * without a slot (it routes to Payment Methods, iOS `canBook`). Otherwise a missing or stale slot (D-06).
     */
    val bookDisabledReason: BookDisabledReason?
        get() {
            val topic = selectedTopic ?: return BookDisabledReason.NoTopic
            if (!topic.isFree) {
                when (paymentMethods) {
                    PaymentMethodsAvailability.Unknown -> return BookDisabledReason.CheckingPaymentMethods
                    PaymentMethodsAvailability.None -> return null
                    PaymentMethodsAvailability.Available -> Unit
                }
            }
            val slot = selectedSlot
            return if (slot == null || !slot.isSelectable(now)) BookDisabledReason.NoSlot else null
        }

    /** iOS `requiresPaymentMethodSetup`: a paid topic with no saved card. */
    val bookCtaAction: BookCtaAction
        get() =
            if (selectedTopic?.isFree == false && paymentMethods == PaymentMethodsAvailability.None) {
                BookCtaAction.AddPaymentMethod
            } else {
                BookCtaAction.Book
            }
}

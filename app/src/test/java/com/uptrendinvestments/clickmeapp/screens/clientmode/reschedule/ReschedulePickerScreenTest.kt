package com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.uptrendinvestments.clickmeapp.components.RescheduleViewer
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.calendarDayTestTag
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.slotChipTestTag
import com.uptrendinvestments.clickmeapp.testing.assertActionablesNamed
import com.uptrendinvestments.clickmeapp.testing.assertTouchTargets
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h3000dp")
class ReschedulePickerScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val zone = ZoneId.of("America/Toronto")
    private val clientHint = "Let the expert know why you'd like to move this session."
    private val expertHint = "Let your client know why you'd like to move this session."

    private val submitted = mutableListOf<Unit>()
    private val selectedSlots = mutableListOf<Instant>()
    private val selectedDates = mutableListOf<LocalDate>()
    private var retryLoadCount = 0
    private var confirmCount = 0
    private val dismissed = mutableListOf<Unit>()
    private var addPaymentCount = 0
    private var backCount = 0

    private fun show(state: ReschedulePickerUiState) {
        rule.setContent {
            ClickMeTheme {
                ReschedulePickerScreen(
                    state = state,
                    onBack = { backCount++ },
                    onRetryLoad = { retryLoadCount++ },
                    onPreviousMonth = {},
                    onNextMonth = {},
                    onSelectDate = { selectedDates += it },
                    onSelectSlot = { selectedSlots += it },
                    onRetrySlots = {},
                    onMessageChange = {},
                    onSubmit = { submitted += Unit },
                    onConfirmDisclosure = { confirmCount++ },
                    onDismissDialog = { dismissed += Unit },
                    onAddPaymentMethod = { addPaymentCount++ },
                    timeFormat = java.text.SimpleDateFormat("h:mm a", java.util.Locale.US),
                )
            }
        }
    }

    private fun stateIs(expected: String) =
        SemanticsMatcher("stateDescription == $expected") {
            it.config.getOrNull(SemanticsProperties.StateDescription) == expected
        }

    private fun daysFromNow(days: Long): Instant = PickerNow.plus(Duration.ofDays(days))

    /** A state whose selected day holds the single chip at [start]. */
    private fun stateWithChip(
        start: Instant,
        booking: PickerBooking = pickerBooking(),
    ): ReschedulePickerUiState {
        val date = start.atZone(zone).toLocalDate()
        return pickerState(booking = booking, selectedDate = date, slots = slotsOn(date, zone, start))
    }

    @Test
    fun loadedClientShowsBlocks() {
        val booking = pickerBooking()
        show(pickerState(booking = booking))

        rule.onNodeWithText("Ada Lovelace").assertExists()
        rule.onNodeWithText("Engineer").assertExists()
        rule.onNodeWithText("Nov 4, 2026 · ", substring = true).assertExists()
        rule.onNodeWithTag(RESCHEDULE_DAY_TAG_PREFIX + "2026-11-04").assertExists()
        rule.onNodeWithTag(RESCHEDULE_BANNER_TAG).assertTextContains("Current: ", substring = true)
        rule.onNodeWithText("Available Time Slots").assertExists()
        rule.onNodeWithText("Message Text (optional)").assertExists()
        rule.onNodeWithText(clientHint).assertExists()
        rule.onNodeWithTag(RESCHEDULE_MESSAGE_TAG).assertExists()
        rule.onNodeWithTag(RESCHEDULE_SUBMIT_TAG).assertTextContains("Submit Reschedule Request")
    }

    @Test
    fun currentChipIsShownDisabled() {
        val booking = pickerBooking()
        show(pickerState(booking = booking))

        rule
            .onNodeWithTag(slotChipTestTag(booking.currentStart, RESCHEDULE_SLOT_TAG_PREFIX))
            .assertTextContains("Current", substring = true)
            .assertIsNotEnabled()
    }

    @Test
    fun paidBookingShowsNoteAndDimsBeyondLimit() {
        val far = daysFromNow(7)
        show(stateWithChip(far, pickerBooking(isPaid = true)))

        rule
            .onNodeWithTag(RESCHEDULE_PAID_NOTE_TAG)
            .assertTextContains("Paid sessions can be rescheduled up to 6 days ahead.")
        rule.onNodeWithTag(slotChipTestTag(far, RESCHEDULE_SLOT_TAG_PREFIX)).assertIsNotEnabled()
    }

    @Test
    fun freeBookingShowsNoNoteAndKeepsFarChipEnabled() {
        val far = daysFromNow(7)
        show(stateWithChip(far, pickerBooking(isPaid = false)))

        rule.onNodeWithTag(RESCHEDULE_PAID_NOTE_TAG).assertDoesNotExist()
        rule.onNodeWithTag(slotChipTestTag(far, RESCHEDULE_SLOT_TAG_PREFIX)).assertIsEnabled()
    }

    @Test
    fun authorizationExpiryNeverDimsChips() {
        val chip = daysFromNow(3)
        val booking = pickerBooking(isPaid = false, authorizationExpiresAt = daysFromNow(1))
        show(stateWithChip(chip, booking))

        rule.onNodeWithTag(slotChipTestTag(chip, RESCHEDULE_SLOT_TAG_PREFIX)).assertIsEnabled()
    }

    @Test
    fun submitDisabledWithoutSlot() {
        show(pickerState(selectedSlotStart = null))

        rule
            .onNodeWithTag(RESCHEDULE_SUBMIT_TAG)
            .assertIsNotEnabled()
            .assert(stateIs("Choose a new time first"))
    }

    @Test
    fun submitEnabledWithSlotCallsBack() {
        val chip = pickerBooking().currentStart.plus(Duration.ofHours(1))
        show(pickerState(selectedSlotStart = chip))

        rule.onNodeWithTag(RESCHEDULE_SUBMIT_TAG).assertIsEnabled().performClick()
        assertEquals(1, submitted.size)
    }

    @Test
    fun submitInFlight() {
        val chip = pickerBooking().currentStart.plus(Duration.ofHours(1))
        show(pickerState(selectedSlotStart = chip, isSubmitting = true))

        rule.onNodeWithTag(RESCHEDULE_SUBMIT_TAG).assert(stateIs("In progress")).performClick()
        rule.onNodeWithText("Submit Reschedule Request").assertDoesNotExist()
        assertEquals(0, submitted.size)
        rule.onNodeWithTag(RESCHEDULE_MESSAGE_TAG).assert(!hasSetTextAction())
    }

    @Test
    fun selectingASlotCallsBack() {
        val booking = pickerBooking()
        val later = booking.currentStart.plus(Duration.ofHours(1))
        show(pickerState(booking = booking))

        rule.onNodeWithTag(slotChipTestTag(later, RESCHEDULE_SLOT_TAG_PREFIX)).performClick()
        assertEquals(listOf(later), selectedSlots)
        rule.onNodeWithTag(calendarDayTestTag(LocalDate.of(2026, 11, 4), RESCHEDULE_DAY_TAG_PREFIX)).performClick()
        assertTrue(selectedDates.contains(LocalDate.of(2026, 11, 4)))
    }

    @Test
    fun expertViewerFallbacks() {
        val booking = pickerBooking(viewer = RescheduleViewer.Expert, counterpartName = null, subtitle = null)
        show(pickerState(booking = booking))

        rule.onNodeWithText("Client").assertExists()
        rule.onNodeWithText("Session").assertExists()
        rule.onNodeWithText(expertHint).assertExists()
    }

    @Test
    fun actionablesNamedAndTargets() {
        show(pickerState())

        rule.assertActionablesNamed()
        rule.assertTouchTargets()
    }
}

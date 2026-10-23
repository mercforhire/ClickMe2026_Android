package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking

import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.IOException

class PaidFlowErrorTest {
    private fun http(
        status: Int,
        code: ErrorCode?,
        message: String? = null,
        detail: Map<String, Any?>? = null,
    ) = ApiException.Http(
        status = status,
        code = code,
        rawCode = code?.wire,
        serverMessage = message,
        fieldErrors = emptyList(),
        detail = detail,
    )

    private fun res(id: Int) = FieldError.Res(id)

    private fun createIntent(error: ApiException) = error.toPaidFailure(PaidPhase.CreateIntent, null)

    private fun confirm(
        error: ApiException,
        pi: String? = "pi_1",
        afterUnknownOutcome: Boolean = false,
    ) = error.toPaidFailure(PaidPhase.Confirm, pi, afterUnknownOutcome)

    @Test
    fun booking_attempt_conflict_asks_for_a_new_attempt_in_both_phases() {
        val expected = PaidFailure.Show(res(R.string.make_a_booking_error_try_again), newAttemptId = true)
        assertEquals(expected, createIntent(http(409, ErrorCode.BookingAttemptConflict)))
        assertEquals(expected, confirm(http(409, ErrorCode.BookingAttemptConflict)))
    }

    @Test
    fun pi_codes_show_the_server_message_and_ask_for_a_new_attempt() {
        listOf(
            ErrorCode.IntentExpired to 410,
            ErrorCode.PiContextMismatch to 409,
            ErrorCode.PiAmountMismatch to 409,
        ).forEach { (code, status) ->
            assertEquals(
                PaidFailure.Show(FieldError.Text("Intent expired"), newAttemptId = true),
                confirm(http(status, code, "Intent expired")),
            )
        }
    }

    @Test
    fun slot_unavailable_clears_the_slot_reloads_and_asks_for_a_new_attempt() {
        assertEquals(
            PaidFailure.Show(
                res(R.string.make_a_booking_error_slot_taken),
                newAttemptId = true,
                clearSlotAndReload = true,
            ),
            createIntent(http(409, ErrorCode.SlotUnavailable)),
        )
    }

    @Test
    fun slot_unavailable_after_an_unknown_outcome_points_to_my_bookings() {
        assertEquals(
            PaidFailure.Show(
                res(R.string.make_a_booking_error_slot_taken_check_bookings),
                newAttemptId = true,
                clearSlotAndReload = true,
            ),
            confirm(http(409, ErrorCode.SlotUnavailable), afterUnknownOutcome = true),
        )
    }

    @Test
    fun meeting_link_unavailable_uses_the_free_path_copy_and_resets_the_meeting_type() {
        assertEquals(
            PaidFailure.Show(res(R.string.make_a_booking_error_no_link), resetMeetingType = true),
            confirm(http(422, ErrorCode.MeetingLinkUnavailable)),
        )
    }

    @Test
    fun mapped_codes_use_their_copy_and_carry_no_reference_even_in_confirm() {
        val table =
            listOf(
                ErrorCode.ExpertNotBookable to R.string.make_a_booking_error_not_bookable,
                ErrorCode.InvalidPaymentMethod to R.string.make_a_booking_error_invalid_payment_method,
                ErrorCode.DirectChargeDisabled to R.string.make_a_booking_error_payments_unavailable,
                ErrorCode.PaymentFailed to R.string.make_a_booking_error_card_declined,
                ErrorCode.DuplicateBooking to R.string.make_a_booking_error_duplicate,
                ErrorCode.AmountBelowMinimum to R.string.make_a_booking_error_amount_below_minimum,
                ErrorCode.InvalidPromo to R.string.make_a_booking_error_invalid_promo,
                ErrorCode.CannotBookSelf to R.string.make_a_booking_error_self,
            )
        table.forEach { (code, string) ->
            val expected = PaidFailure.Show(res(string))
            assertEquals(code.name, expected, createIntent(http(422, code)))
            assertEquals(code.name, expected, confirm(http(422, code)))
        }
    }

    @Test
    fun requires_action_with_a_connected_account_starts_the_challenge() {
        val detail = mapOf("client_secret" to "pi_1_secret_x", "stripe_account" to "acct_1")
        assertEquals(
            PaidFailure.StartChallenge("pi_1_secret_x", "acct_1"),
            confirm(http(402, ErrorCode.PaymentRequiresAction, detail = detail)),
        )
    }

    @Test
    fun requires_action_without_a_usable_account_is_a_hard_failure() {
        val hard = PaidFailure.Show(res(R.string.make_a_booking_error_sca_unavailable))
        listOf(
            mapOf("client_secret" to "pi_1_secret_x", "stripe_account" to ""),
            mapOf("client_secret" to "pi_1_secret_x", "stripe_account" to "  "),
            mapOf("client_secret" to "pi_1_secret_x", "stripe_account" to null),
            mapOf("client_secret" to "pi_1_secret_x"),
        ).forEach { detail ->
            assertEquals(hard, confirm(http(402, ErrorCode.PaymentRequiresAction, detail = detail)))
        }
    }

    @Test
    fun requires_action_without_a_client_secret_shows_the_server_message() {
        assertEquals(
            PaidFailure.Show(FieldError.Text("Needs auth")),
            confirm(http(402, ErrorCode.PaymentRequiresAction, "Needs auth")),
        )
        assertEquals(
            PaidFailure.Show(FieldError.Text("Needs auth")),
            confirm(http(402, ErrorCode.PaymentRequiresAction, "Needs auth", mapOf("stripe_account" to "acct_1"))),
        )
    }

    @Test
    fun account_suspended_is_silent() {
        assertEquals(PaidFailure.Silent, confirm(http(403, ErrorCode.AccountSuspended)))
    }

    @Test
    fun unmapped_code_in_confirm_with_a_payment_intent_carries_the_reference() {
        assertEquals(
            PaidFailure.Show(FieldError.Text("Boom"), paymentReference = "pi_1"),
            confirm(http(500, null, "Boom")),
        )
    }

    @Test
    fun unmapped_code_has_no_reference_in_create_intent_or_without_a_payment_intent() {
        assertEquals(PaidFailure.Show(FieldError.Text("Boom")), createIntent(http(500, null, "Boom")))
        assertEquals(PaidFailure.Show(FieldError.Text("Boom")), confirm(http(500, null, "Boom"), pi = null))
    }

    @Test
    fun unmapped_code_without_a_message_uses_the_unknown_copy() {
        assertEquals(PaidFailure.Show(res(R.string.login_error_unknown)), createIntent(http(500, null)))
    }

    @Test
    fun confirm_network_failure_carries_the_reference_and_flags_an_unknown_outcome() {
        assertEquals(
            PaidFailure.Show(res(R.string.login_error_network), paymentReference = "pi_1", outcomeUnknown = true),
            confirm(ApiException.Network(IOException())),
        )
    }

    @Test
    fun confirm_network_failure_without_a_payment_intent_is_still_an_unknown_outcome() {
        assertEquals(
            PaidFailure.Show(res(R.string.login_error_network), outcomeUnknown = true),
            confirm(ApiException.Network(IOException()), pi = null),
        )
    }

    @Test
    fun create_intent_network_failure_is_a_plain_network_message() {
        assertEquals(
            PaidFailure.Show(res(R.string.login_error_network)),
            createIntent(ApiException.Network(IOException())),
        )
    }

    @Test
    fun confirm_decoding_failure_carries_the_reference_and_flags_an_unknown_outcome() {
        assertEquals(
            PaidFailure.Show(res(R.string.login_error_unknown), paymentReference = "pi_1", outcomeUnknown = true),
            confirm(ApiException.Decoding(RuntimeException())),
        )
    }

    @Test
    fun create_intent_decoding_failure_is_a_plain_unknown_message() {
        assertEquals(
            PaidFailure.Show(res(R.string.login_error_unknown)),
            createIntent(ApiException.Decoding(RuntimeException())),
        )
    }

    @Test
    fun only_conflict_pi_and_slot_rows_ask_for_a_new_attempt_id() {
        val asking =
            setOf(
                ErrorCode.BookingAttemptConflict,
                ErrorCode.IntentExpired,
                ErrorCode.PiContextMismatch,
                ErrorCode.PiAmountMismatch,
                ErrorCode.SlotUnavailable,
            )
        ErrorCode.entries.forEach { code ->
            val failure = confirm(http(400, code, "msg", mapOf("client_secret" to "s", "stripe_account" to "a")))
            val asked = (failure as? PaidFailure.Show)?.newAttemptId ?: false
            assertEquals(code.name, code in asking, asked)
        }
    }

    @Test
    fun start_challenge_never_prints_its_secrets() {
        val text = PaidFailure.StartChallenge("pi_1_secret_x", "acct_1").toString()
        assertFalse(text.contains("pi_1_secret_x"))
        assertFalse(text.contains("acct_1"))
    }
}

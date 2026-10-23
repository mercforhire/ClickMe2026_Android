package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * iOS `PaymentMethodsAvailability`. [Unknown] until the first `/me/payment-methods` answer; a failed load is
 * [None] (iOS treats it as no cards, D-08).
 */
enum class PaymentMethodsAvailability { Unknown, None, Available }

/**
 * iOS `PaidStep`. [CreatingIntent] covers payment-intent and setup-intent; [CollectingPaymentMethod] is the
 * FlowController sheet; [Confirming] covers `/bookings/confirm`, the 3DS challenge and the confirm retry. The
 * screen shows the CTA spinner for every step except [Idle].
 */
enum class PaidStep { Idle, CreatingIntent, CollectingPaymentMethod, Confirming }

/**
 * A one-shot request for the Compose host. The host consumes it before launching so a recreation cannot launch
 * twice (RESEARCH Pattern 1). Never stored in `SavedStateHandle` (D-17). `toString()` prints the type name only so
 * the Stripe secrets cannot reach a log.
 */
sealed interface StripeRequest {
    /** Present the payment sheet to collect a card, backed by a SetupIntent. */
    data class PresentSheet(
        val setupIntentClientSecret: String,
        val customerId: String,
        val ephemeralKey: String,
    ) : StripeRequest {
        override fun toString(): String = "PresentSheet"
    }

    /**
     * The FlowController finished configuring: present its options from the controller of the current composition
     * (WR-05). The configure callback may outlive the composition that started it (a rotation), so it reports to the
     * ViewModel instead of presenting on the controller it captured. Carries no secret.
     */
    data object PresentOptions : StripeRequest

    /** Run the 3DS challenge on the expert's connected account. */
    data class RunChallenge(
        val clientSecret: String,
        val stripeAccount: String,
    ) : StripeRequest {
        override fun toString(): String = "RunChallenge"
    }
}

private val utcDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC)

private val utcTimeSlotFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT).withZone(ZoneOffset.UTC)

/**
 * `yyyy-MM-dd` of this instant in UTC with ASCII digits. iOS `utcDateString` (en_US_POSIX); the server builds slot
 * keys in UTC.
 */
fun Instant.toUtcDateString(): String = utcDateFormatter.format(this)

/**
 * `HH:mm` of this instant in UTC with ASCII digits. iOS `utcTimeSlotString` (en_US_POSIX); the server validates
 * `^\d{2}:\d{2}$`.
 */
fun Instant.toUtcTimeSlotString(): String = utcTimeSlotFormatter.format(this)

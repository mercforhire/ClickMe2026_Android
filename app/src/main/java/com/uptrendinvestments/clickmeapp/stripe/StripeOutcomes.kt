package com.uptrendinvestments.clickmeapp.stripe

/**
 * What the Compose host reports back after the Stripe payment sheet (FlowController) closes. The host maps the
 * Stripe SDK result to this type, so the ViewModels stay free of SDK imports (07-09).
 */
sealed interface SheetOutcome {
    /** The user picked or added a payment method and the sheet finished. */
    data object Completed : SheetOutcome

    /** The user dismissed the sheet. */
    data object Canceled : SheetOutcome

    /** The sheet could not be configured, presented or completed; [message] is Stripe's text when there is one. */
    data class Failed(
        val message: String?,
    ) : SheetOutcome
}

/**
 * What the Compose host reports back after the 3DS challenge (PaymentLauncher) closes. Mapped from the Stripe SDK
 * result by the host (07-13).
 */
sealed interface ChallengeOutcome {
    /** The card holder passed verification. */
    data object Completed : ChallengeOutcome

    /** The user closed the challenge without finishing (D-15). */
    data object Canceled : ChallengeOutcome

    /** Verification failed; [message] is Stripe's text when there is one (D-15). */
    data class Failed(
        val message: String?,
    ) : ChallengeOutcome
}

/**
 * A SetupIntent minted for an attempt that was cancelled before confirm may be reused for 10 minutes, because
 * `/me/payment-methods/setup-intent` is limited to 10 calls per 15 minutes per user (research correction 4). The
 * window stays well inside the ephemeral key's lifetime.
 */
const val SETUP_INTENT_REUSE_WINDOW_MS = 600_000L

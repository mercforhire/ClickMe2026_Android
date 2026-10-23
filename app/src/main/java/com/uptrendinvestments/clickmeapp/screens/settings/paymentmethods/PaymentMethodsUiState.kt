package com.uptrendinvestments.clickmeapp.screens.settings.paymentmethods

import androidx.annotation.StringRes
import com.uptrendinvestments.clickmeapp.model.responses.PaymentMethodItem
import com.uptrendinvestments.clickmeapp.screens.login.FieldError

/** Load state of the saved-card list. An empty list is [Loaded], not a failure (PAY-01). */
sealed interface PaymentMethodsListState {
    data object Loading : PaymentMethodsListState

    data class Failed(
        val message: FieldError,
    ) : PaymentMethodsListState

    data class Loaded(
        val cards: List<PaymentMethodItem>,
    ) : PaymentMethodsListState
}

/** The kind of in-flight per-card request. */
enum class CardOperationKind { SetDefault, Remove }

/** The single in-flight set-default or remove (D-06); identifies the row that shows the spinner. */
data class CardOperation(
    val cardId: String,
    val kind: CardOperationKind,
)

/**
 * What the Stripe host needs to present PaymentSheet in setup mode. The secrets live in memory only and are
 * redacted from [toString] (T-07-24).
 */
data class AddCardRequest(
    val clientSecret: String,
    val customerId: String,
    val ephemeralKey: String,
) {
    override fun toString(): String = "AddCardRequest"
}

/** An error alert: [title] is one of the three "Couldn't ..." titles, [message] the server or standard copy. */
data class PaymentMethodsDialog(
    @StringRes val title: Int,
    val message: FieldError,
)

/**
 * State of the Payment Methods screen.
 *
 * [busy] is the single in-flight set-default or remove (D-06). [isAddingCard] is true from the Add tap until the
 * sheet reports an outcome. [addCardRequest] is a one-shot request the screen consumes before presenting
 * PaymentSheet; never persisted.
 */
data class PaymentMethodsUiState(
    val list: PaymentMethodsListState = PaymentMethodsListState.Loading,
    val busy: CardOperation? = null,
    val isAddingCard: Boolean = false,
    val addCardRequest: AddCardRequest? = null,
    val dialog: PaymentMethodsDialog? = null,
)

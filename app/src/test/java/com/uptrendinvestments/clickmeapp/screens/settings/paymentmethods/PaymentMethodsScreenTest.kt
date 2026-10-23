package com.uptrendinvestments.clickmeapp.screens.settings.paymentmethods

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.managers.MonotonicClock
import com.uptrendinvestments.clickmeapp.model.responses.PaymentMethodItem
import com.uptrendinvestments.clickmeapp.model.responses.PaymentMethodSetupData
import com.uptrendinvestments.clickmeapp.model.responses.PaymentMethodsData
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.screens.settings.paymentmethods.subviews.paymentMethodMenuTag
import com.uptrendinvestments.clickmeapp.stripe.SheetOutcome
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PaymentMethodsScreenTest {
    @get:Rule(order = 0)
    val main = MainDispatcherRule(UnconfinedTestDispatcher())

    @get:Rule(order = 1)
    val rule = createComposeRule()

    private val visa = PaymentMethodItem("pm_1", "visa", "4242", 9, 2028, isDefault = true)
    private val master = PaymentMethodItem("pm_2", "mastercard", "4444", 12, 2031, isDefault = false)

    private val retries = mutableListOf<Unit>()
    private val adds = mutableListOf<Unit>()
    private val setDefaults = mutableListOf<String>()
    private val removals = mutableListOf<String>()
    private val dismissals = mutableListOf<Unit>()

    private fun setContent(state: PaymentMethodsUiState) {
        rule.setContent {
            ClickMeTheme {
                PaymentMethodsContent(
                    state = state,
                    onBack = {},
                    onRetry = { retries += Unit },
                    onAddCard = { adds += Unit },
                    onSetDefault = { setDefaults += it },
                    onRemove = { removals += it },
                    onDismissDialog = { dismissals += Unit },
                )
            }
        }
    }

    private fun loaded(
        vararg cards: PaymentMethodItem,
        busy: CardOperation? = null,
        isAddingCard: Boolean = false,
        dialog: PaymentMethodsDialog? = null,
    ) = PaymentMethodsUiState(
        list = PaymentMethodsListState.Loaded(cards.toList()),
        busy = busy,
        isAddingCard = isAddingCard,
        dialog = dialog,
    )

    @Test
    fun loadingShowsMessageUnderTheAppBar() {
        setContent(PaymentMethodsUiState())

        rule.onNodeWithText("Payment Methods").assertExists()
        rule.onNodeWithText("Loading payment methods…").assertExists()
    }

    @Test
    fun failedShowsHeadingMessageAndRetry() {
        setContent(PaymentMethodsUiState(list = PaymentMethodsListState.Failed(FieldError.Text("Server down"))))

        rule.onNodeWithText("Couldn't load payment methods").assertExists()
        rule.onNodeWithText("Server down").assertExists()
        rule.onNodeWithText("Retry").performClick()

        assertEquals(1, retries.size)
    }

    @Test
    fun emptyShowsInvitationAddButtonAndTrustCopy() {
        setContent(loaded())

        rule.onNodeWithText("No saved cards yet").assertExists()
        rule.onNodeWithText("Add your first card to book faster next time.").assertExists()
        rule.onNodeWithTag(PAYMENT_METHODS_ADD_TAG).assertExists()
        rule.onNodeWithText("Add Payment Method").assertExists()
        rule.onNodeWithText("Cards are securely stored by Stripe.", substring = true).assertExists()
    }

    @Test
    fun loadedListShowsRowsHeadingAndOneDefaultBadge() {
        setContent(loaded(visa, master))

        rule
            .onNodeWithText("Saved cards")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        rule.onNodeWithText("Visa •••• 4242", substring = true).assertExists()
        rule.onNodeWithText("Expires 09/28", substring = true).assertExists()
        rule.onNodeWithText("Mastercard •••• 4444", substring = true).assertExists()
        rule.onAllNodesWithText("Default", substring = true).assertCountEquals(1)
        rule
            .onNodeWithContentDescription("Visa ending 4242, expires 09/28, default card")
            .assertExists()
        rule.onNodeWithTag(PAYMENT_METHODS_ADD_TAG).performScrollTo().assertExists()
    }

    @Test
    fun defaultCardMenuHasOnlyRemoveAndOtherCardHasBoth() {
        setContent(loaded(visa, master))

        rule.onNodeWithTag(paymentMethodMenuTag("pm_1")).performClick()
        rule.onNodeWithText("Set as default").assertDoesNotExist()
        rule.onNodeWithText("Remove").assertExists()
    }

    @Test
    fun setAsDefaultCallsBackWithTheCardId() {
        setContent(loaded(visa, master))

        rule.onNodeWithTag(paymentMethodMenuTag("pm_2")).performClick()
        rule.onNodeWithText("Remove").assertExists()
        rule.onNodeWithText("Set as default").performClick()

        assertEquals(listOf("pm_2"), setDefaults)
    }

    @Test
    fun removeAsksFirstThenRemovesOnConfirm() {
        setContent(loaded(visa, master))

        rule.onNodeWithTag(paymentMethodMenuTag("pm_2")).performClick()
        rule.onNodeWithText("Remove").performClick()

        rule.onNodeWithText("Remove this card?").assertExists()
        rule.onNodeWithText("Mastercard •••• 4444 will no longer be available at checkout.").assertExists()
        assertEquals(emptyList<String>(), removals)

        rule.onNode(hasText("Remove") and hasAnyAncestor(isDialog())).performClick()

        assertEquals(listOf("pm_2"), removals)
        rule.onNodeWithText("Remove this card?").assertDoesNotExist()
    }

    @Test
    fun cancelingTheRemoveDialogCallsNothing() {
        setContent(loaded(visa, master))

        rule.onNodeWithTag(paymentMethodMenuTag("pm_2")).performClick()
        rule.onNodeWithText("Remove").performClick()
        rule.onNodeWithText("Cancel").performClick()

        assertEquals(emptyList<String>(), removals)
        rule.onNodeWithText("Remove this card?").assertDoesNotExist()
    }

    @Test
    fun busyRowShowsSpinnerAndOtherMenusAreDisabled() {
        setContent(loaded(visa, master, busy = CardOperation("pm_2", CardOperationKind.Remove)))

        rule.onNodeWithTag(paymentMethodMenuTag("pm_2")).assertDoesNotExist()
        rule
            .onNode(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Removing"))
            .assertExists()
        rule.onNodeWithTag(paymentMethodMenuTag("pm_1")).assertIsNotEnabled()
    }

    @Test
    fun addingStateAnnouncesAddingAndIgnoresTaps() {
        setContent(loaded(visa, isAddingCard = true))

        rule
            .onNode(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Adding"))
            .assertExists()
        rule.onNodeWithTag(PAYMENT_METHODS_ADD_TAG).performScrollTo().performClick()

        assertEquals(0, adds.size)
    }

    @Test
    fun addButtonCallsBack() {
        setContent(loaded(visa))

        rule.onNodeWithTag(PAYMENT_METHODS_ADD_TAG).performScrollTo().performClick()

        assertEquals(1, adds.size)
    }

    @Test
    fun errorDialogShowsTitleAndMessageAndOkDismisses() {
        setContent(
            loaded(
                visa,
                dialog = PaymentMethodsDialog(R.string.payment_methods_error_remove_title, FieldError.Text("Nope")),
            ),
        )

        rule.onNodeWithText("Couldn't remove").assertExists()
        rule.onNodeWithText("Nope").assertExists()
        rule.onNodeWithText("OK").performClick()

        assertEquals(1, dismissals.size)
    }

    @Test
    fun screenHandsTheAddCardRequestToTheHostOnceAndTheHostClearsIt() {
        val api = FakeClickMeApi()
        api.getPaymentMethodsHandler = { PaymentMethodsData(listOf(visa, master)) }
        api.setupIntentHandler = { PaymentMethodSetupData("seti_1_secret_x", "cus_1", "ek_1") }
        val viewModel = PaymentMethodsViewModel(PaymentMethodsRepository(api), MonotonicClock { 0L })
        val received = mutableListOf<AddCardRequest>()
        var report: (SheetOutcome) -> Unit = {}

        rule.setContent {
            ClickMeTheme {
                PaymentMethodsScreen(
                    viewModel = viewModel,
                    onBack = {},
                    addCardHost = { request, onLaunched, onResult ->
                        report = onResult
                        LaunchedEffect(request) {
                            if (request != null) {
                                received += request
                                onLaunched()
                            }
                        }
                    },
                )
            }
        }

        rule.onNodeWithTag(PAYMENT_METHODS_ADD_TAG).performScrollTo().performClick()
        rule.waitForIdle()

        assertEquals(1, received.size)
        assertEquals("seti_1_secret_x", received.single().clientSecret)
        assertNull(viewModel.uiState.value.addCardRequest)

        rule.runOnIdle { report(SheetOutcome.Canceled) }
        rule.waitForIdle()

        assertEquals(1, received.size)
        assertNotNull(viewModel.uiState.value.list as? PaymentMethodsListState.Loaded)
        assertEquals(1, api.getPaymentMethodsCalls)
        rule.onNodeWithText("Visa •••• 4242", substring = true).assertExists()
    }
}

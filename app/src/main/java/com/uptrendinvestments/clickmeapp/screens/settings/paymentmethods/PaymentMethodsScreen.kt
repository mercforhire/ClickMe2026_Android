package com.uptrendinvestments.clickmeapp.screens.settings.paymentmethods

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stripe.android.paymentsheet.PaymentSheet
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.EmptyState
import com.uptrendinvestments.clickmeapp.components.EmptyStateAction
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButton
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButtonDefaults
import com.uptrendinvestments.clickmeapp.model.responses.PaymentMethodItem
import com.uptrendinvestments.clickmeapp.screens.login.AuthErrorDialog
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.screens.login.asString
import com.uptrendinvestments.clickmeapp.screens.settings.paymentmethods.subviews.PaymentMethodRemoveDialog
import com.uptrendinvestments.clickmeapp.screens.settings.paymentmethods.subviews.PaymentMethodRow
import com.uptrendinvestments.clickmeapp.screens.settings.paymentmethods.subviews.brandDisplayNameRes
import com.uptrendinvestments.clickmeapp.stripe.SheetOutcome
import com.uptrendinvestments.clickmeapp.stripe.clickMeSheetConfiguration
import com.uptrendinvestments.clickmeapp.stripe.rememberClickMePaymentSheetAppearance
import com.uptrendinvestments.clickmeapp.stripe.toSheetOutcome
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras

/** Test tag on the "Add Payment Method" button. */
const val PAYMENT_METHODS_ADD_TAG = "payment_methods_add"

private val AddButtonMinHeight = 56.dp
private val AddIconSize = 24.dp
private val AddSpinnerStroke = 2.dp

/**
 * Port of iOS `PaymentMethodsView`. Back always pops: an in-flight set-default or remove finishes in the ViewModel
 * (UI-SPEC). The D-04 entry points that open this screen are wired in 07-12.
 *
 * [addCardHost] is the Stripe seam. The default presents PaymentSheet; tests pass a fake so Robolectric never
 * instantiates the SDK, and the real sheet is checked on a device (D-03).
 */
@Composable
fun PaymentMethodsScreen(
    viewModel: PaymentMethodsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    addCardHost: @Composable (AddCardRequest?, () -> Unit, (SheetOutcome) -> Unit) -> Unit =
        { request, onLaunched, onResult -> AddCardSheetHost(request, onLaunched, onResult) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PaymentMethodsContent(
        state = state,
        onBack = onBack,
        onRetry = viewModel::retry,
        onAddCard = viewModel::addCard,
        onSetDefault = viewModel::setDefault,
        onRemove = viewModel::remove,
        onDismissDialog = viewModel::dismissDialog,
        modifier = modifier,
    )
    addCardHost(state.addCardRequest, viewModel::onAddCardLaunched, viewModel::onAddCardResult)
}

/**
 * Presents Stripe's PaymentSheet in setup mode for [request]. PCI scope stays inside Stripe's UI: the app never sees
 * card data. The request is consumed through [onLaunched] before the sheet is presented, so a recreation cannot
 * present it twice (RESEARCH Pattern 1, Pitfall 8). Results reach the same ViewModel through `rememberUpdatedState`.
 */
@Composable
fun AddCardSheetHost(
    request: AddCardRequest?,
    onLaunched: () -> Unit,
    onResult: (SheetOutcome) -> Unit,
) {
    val latest by rememberUpdatedState(onResult)
    val sheet = remember { PaymentSheet.Builder { result -> latest(result.toSheetOutcome()) } }.build()
    val appearance = rememberClickMePaymentSheetAppearance()
    val saveLabel = stringResource(R.string.payment_methods_sheet_save)

    LaunchedEffect(request) {
        val r = request ?: return@LaunchedEffect
        onLaunched()
        sheet.presentWithSetupIntent(
            r.clientSecret,
            clickMeSheetConfiguration(r.customerId, r.ephemeralKey, appearance, saveLabel),
        )
    }
}

/** Stateless Payment Methods screen: loading, failed, empty and loaded states under a "Payment Methods" bar. */
@Composable
fun PaymentMethodsContent(
    state: PaymentMethodsUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onAddCard: () -> Unit,
    onSetDefault: (String) -> Unit,
    onRemove: (String) -> Unit,
    onDismissDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Only the id is saved; the card itself is looked up from the current list.
    var removeTargetId by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = { AuthTopAppBar(title = stringResource(R.string.payment_methods_title), onBack = onBack) },
    ) { innerPadding ->
        when (val list = state.list) {
            PaymentMethodsListState.Loading -> LoadingBody(Modifier.padding(innerPadding))
            is PaymentMethodsListState.Failed -> FailedBody(list.message, onRetry, Modifier.padding(innerPadding))
            is PaymentMethodsListState.Loaded ->
                if (list.cards.isEmpty()) {
                    EmptyBody(state.isAddingCard, onAddCard, Modifier.padding(innerPadding))
                } else {
                    CardsBody(
                        cards = list.cards,
                        state = state,
                        onAddCard = onAddCard,
                        onSetDefault = onSetDefault,
                        onRequestRemove = { removeTargetId = it },
                        modifier = Modifier.padding(innerPadding),
                    )
                }
        }
    }

    val loadedCards = (state.list as? PaymentMethodsListState.Loaded)?.cards.orEmpty()
    val target = loadedCards.firstOrNull { it.id == removeTargetId }
    if (target != null) {
        PaymentMethodRemoveDialog(
            cardName = stringResource(brandDisplayNameRes(target.brand)),
            last4 = target.last4.orEmpty(),
            onConfirm = {
                removeTargetId = null
                onRemove(target.id)
            },
            onDismiss = { removeTargetId = null },
        )
    }

    state.dialog?.let { dialog ->
        AuthErrorDialog(
            message = dialog.message.asString(),
            onDismiss = onDismissDialog,
            title = stringResource(dialog.title),
        )
    }
}

@Composable
private fun LoadingBody(modifier: Modifier = Modifier) {
    val loadingLabel = stringResource(R.string.payment_methods_loading_a11y)
    Column(
        modifier =
            modifier.fillMaxSize().semantics(mergeDescendants = true) {
                liveRegion = LiveRegionMode.Polite
                contentDescription = loadingLabel
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = stringResource(R.string.payment_methods_loading),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.extras.onSurfaceMuted,
        )
    }
}

@Composable
private fun FailedBody(
    message: FieldError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize().semantics { liveRegion = LiveRegionMode.Polite },
        contentAlignment = Alignment.Center,
    ) {
        EmptyState(
            icon = Icons.Outlined.ErrorOutline,
            heading = stringResource(R.string.payment_methods_error_load_heading),
            body = message.asString(),
            action = EmptyStateAction(stringResource(R.string.section_retry), onRetry),
        )
    }
}

@Composable
private fun EmptyBody(
    isAddingCard: Boolean,
    onAddCard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(Spacing.lg))
        EmptyState(
            icon = Icons.Outlined.CreditCard,
            heading = stringResource(R.string.payment_methods_empty_heading),
            body = stringResource(R.string.payment_methods_empty_body),
        )
        Spacer(Modifier.height(Spacing.lg))
        AddCardButton(isAddingCard, onAddCard)
        Spacer(Modifier.height(Spacing.lg))
        TrustCopy()
        Spacer(Modifier.height(Spacing.xl))
    }
}

@Composable
private fun CardsBody(
    cards: List<PaymentMethodItem>,
    state: PaymentMethodsUiState,
    onAddCard: () -> Unit,
    onSetDefault: (String) -> Unit,
    onRequestRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(start = Spacing.md, end = Spacing.md, top = Spacing.lg, bottom = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        item(key = "saved_cards_label") {
            Text(
                text = stringResource(R.string.payment_methods_saved_cards),
                modifier = Modifier.padding(horizontal = Spacing.xs).semantics { heading() },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.extras.onSurfaceMuted,
            )
        }
        items(cards, key = { it.id }) { card ->
            PaymentMethodRow(
                card = card,
                busy = state.busy?.takeIf { it.cardId == card.id }?.kind,
                actionsEnabled = state.busy == null,
                onSetDefault = { onSetDefault(card.id) },
                onRemove = { onRequestRemove(card.id) },
                modifier = Modifier.animateItem(),
            )
        }
        // The list gap is md; these items add the rest of the lg spacing from the UI-SPEC.
        item(key = "add_button") {
            Column {
                Spacer(Modifier.height(Spacing.sm))
                AddCardButton(state.isAddingCard, onAddCard)
            }
        }
        item(key = "trust_copy") {
            Column {
                Spacer(Modifier.height(Spacing.sm))
                TrustCopy()
            }
        }
    }
}

@Composable
private fun AddCardButton(
    isAddingCard: Boolean,
    onAddCard: () -> Unit,
) {
    val adding = stringResource(R.string.payment_methods_state_adding)
    PressScaleButton(
        onClick = { if (!isAddingCard) onAddCard() },
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = AddButtonMinHeight)
                .testTag(PAYMENT_METHODS_ADD_TAG)
                .semantics { if (isAddingCard) stateDescription = adding },
        colors = PressScaleButtonDefaults.filledColors(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isAddingCard) {
                CircularProgressIndicator(
                    modifier = Modifier.size(AddIconSize),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = AddSpinnerStroke,
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.AddCircle,
                    contentDescription = null,
                    modifier = Modifier.size(AddIconSize),
                )
            }
            Spacer(Modifier.width(Spacing.sm))
            Text(text = stringResource(R.string.payment_methods_add), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun TrustCopy() {
    Text(
        text = stringResource(R.string.payment_methods_trust),
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.extras.onSurfaceMuted,
        textAlign = TextAlign.Center,
    )
}

private val PreviewVisa = PaymentMethodItem("pm_1", "visa", "4242", 9, 2028, isDefault = true)
private val PreviewMaster = PaymentMethodItem("pm_2", "mastercard", "4444", 12, 2031, isDefault = false)

@Composable
private fun PreviewContent(state: PaymentMethodsUiState) {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            PaymentMethodsContent(
                state = state,
                onBack = {},
                onRetry = {},
                onAddCard = {},
                onSetDefault = {},
                onRemove = {},
                onDismissDialog = {},
            )
        }
    }
}

@Preview(name = "PaymentMethods loaded")
@Composable
private fun PaymentMethodsLoadedPreview() {
    PreviewContent(PaymentMethodsUiState(list = PaymentMethodsListState.Loaded(listOf(PreviewVisa, PreviewMaster))))
}

@Preview(name = "PaymentMethods empty")
@Composable
private fun PaymentMethodsEmptyPreview() {
    PreviewContent(PaymentMethodsUiState(list = PaymentMethodsListState.Loaded(emptyList())))
}

@Preview(name = "PaymentMethods busy")
@Composable
private fun PaymentMethodsBusyPreview() {
    PreviewContent(
        PaymentMethodsUiState(
            list = PaymentMethodsListState.Loaded(listOf(PreviewVisa, PreviewMaster)),
            busy = CardOperation("pm_2", CardOperationKind.Remove),
            isAddingCard = true,
        ),
    )
}

package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.stripe.android.payments.paymentlauncher.rememberPaymentLauncher
import com.stripe.android.paymentsheet.PaymentSheet
import com.uptrendinvestments.clickmeapp.config.AppConfig
import com.uptrendinvestments.clickmeapp.stripe.ChallengeOutcome
import com.uptrendinvestments.clickmeapp.stripe.SheetOutcome
import com.uptrendinvestments.clickmeapp.stripe.clickMeSheetConfiguration
import com.uptrendinvestments.clickmeapp.stripe.isPostCompletionReset
import com.uptrendinvestments.clickmeapp.stripe.rememberClickMePaymentSheetAppearance
import com.uptrendinvestments.clickmeapp.stripe.shouldConfirmSelection
import com.uptrendinvestments.clickmeapp.stripe.toChallengeOutcome
import com.uptrendinvestments.clickmeapp.stripe.toSheetOutcome
import com.uptrendinvestments.clickmeapp.stripe.userFacingStripeMessage

/** Holds the controller so the option callback, built before the controller exists, can confirm on it. */
private class FlowControllerHolder {
    var controller: PaymentSheet.FlowController? = null
}

/**
 * What one FlowController option callback means for the paid flow. The ViewModel's step stands in for a "sheet
 * presented" flag and survives rotation. A selection confirms; the SDK's reset after a completed setup is ignored
 * because [SheetOutcome.Completed] follows it through the result callback ([isPostCompletionReset]); anything else is
 * a dismissal, which never authorizes a card (D-18).
 */
internal fun routePaymentOptionResult(
    paidStep: PaidStep,
    didCancel: Boolean,
    hasPaymentOption: Boolean,
    confirm: () -> Unit,
    reportSheetOutcome: (SheetOutcome) -> Unit,
) {
    if (paidStep != PaidStep.CollectingPaymentMethod) return
    when {
        shouldConfirmSelection(didCancel, hasPaymentOption) -> confirm()
        isPostCompletionReset(didCancel, hasPaymentOption) -> Unit
        else -> reportSheetOutcome(SheetOutcome.Canceled)
    }
}

/**
 * The only place the paid booking flow touches the Stripe SDK. It turns the ViewModel's one-shot [StripeRequest]s
 * into Stripe UI and reports what happened as SDK-free outcomes.
 *
 * - [StripeRequest.PresentSheet]: `PaymentSheet.FlowController` in setup mode (RESEARCH Pattern 2). The controller
 *   is configured with the SetupIntent secret and the ClickMe configuration. The configure callback runs in Stripe's
 *   ViewModel scope and can outlive this composition (a rotation during the network round trip), so it never
 *   presents on the controller it captured: it reports [onSheetConfigured], the ViewModel answers with
 *   [StripeRequest.PresentOptions], and `presentPaymentOptions()` runs on the controller of the composition that is
 *   current then (WR-05). The configuration itself lives in Stripe's activity-scoped FlowController ViewModel, so
 *   the new controller sees it. When the sheet closes, [shouldConfirmSelection]
 *   decides whether `confirm()` runs: a dismissal reports [SheetOutcome.Canceled] even when a saved card is
 *   preselected (D-18, unlike iOS), so a dismissal can never authorize a card.
 * - [StripeRequest.RunChallenge]: `PaymentLauncher.handleNextActionForPaymentIntent` on the expert's connected
 *   account (RESEARCH Pattern 3). The launcher is keyed by [challengeAccount], which the ViewModel keeps set until
 *   the outcome arrives, so a rotation rebuilds the launcher with the same account. It is never the platform
 *   account (D-15).
 *
 * Each request is consumed through [onRequestLaunched] before it is launched, so a rotation or recomposition cannot
 * launch a second sheet or challenge (RESEARCH Pattern 1, correction 6). Results arrive through
 * `rememberUpdatedState`, so a callback built before a rotation still reaches the current ViewModel.
 *
 * Path A: the app never confirms the booking PaymentIntent. The only client-side PaymentIntent call is
 * `handleNextActionForPaymentIntent` after the server answered 402. Device-unproven (RESEARCH Pitfall 16); A3
 * (`PaymentResult.Completed` for a `requires_capture` PaymentIntent) and A4 (FlowController state across rotation)
 * are device checks. Secrets are never logged.
 */
@Composable
fun PaidFlowStripeHost(
    request: StripeRequest?,
    paidStep: PaidStep,
    challengeAccount: String?,
    onRequestLaunched: () -> Unit,
    onSheetConfigured: () -> Unit,
    onSheetOutcome: (SheetOutcome) -> Unit,
    onChallengeOutcome: (ChallengeOutcome) -> Unit,
) {
    val latestStep by rememberUpdatedState(paidStep)
    val latestConfigured by rememberUpdatedState(onSheetConfigured)
    val latestSheet by rememberUpdatedState(onSheetOutcome)
    val latestChallenge by rememberUpdatedState(onChallengeOutcome)
    val appearance = rememberClickMePaymentSheetAppearance()

    val holder = remember { FlowControllerHolder() }
    val flowController =
        remember {
            PaymentSheet.FlowController.Builder(
                resultCallback = { latestSheet(it.toSheetOutcome()) },
                paymentOptionResultCallback = { result ->
                    routePaymentOptionResult(
                        paidStep = latestStep,
                        didCancel = result.didCancel,
                        hasPaymentOption = result.paymentOption != null,
                        confirm = { holder.controller?.confirm() },
                        reportSheetOutcome = { latestSheet(it) },
                    )
                },
            )
        }.build()
    holder.controller = flowController

    LaunchedEffect(request) {
        when (val r = request) {
            is StripeRequest.PresentSheet -> {
                onRequestLaunched()
                flowController.configureWithSetupIntent(
                    setupIntentClientSecret = r.setupIntentClientSecret,
                    configuration = clickMeSheetConfiguration(r.customerId, r.ephemeralKey, appearance),
                ) { success, error ->
                    // Never touch the captured flowController here: it may belong to a disposed composition.
                    if (success) {
                        latestConfigured()
                    } else {
                        latestSheet(SheetOutcome.Failed(error.userFacingStripeMessage()))
                    }
                }
            }
            StripeRequest.PresentOptions -> {
                onRequestLaunched()
                flowController.presentPaymentOptions()
            }
            else -> Unit
        }
    }

    val launcher =
        rememberPaymentLauncher(
            publishableKey = AppConfig.stripePublishableKey,
            stripeAccountId = challengeAccount,
            callback = { latestChallenge(it.toChallengeOutcome()) },
        )

    LaunchedEffect(request, challengeAccount) {
        val r = request as? StripeRequest.RunChallenge ?: return@LaunchedEffect
        // The launcher in this composition is keyed to the 402's account; wait for it, never use another account.
        if (challengeAccount != r.stripeAccount) return@LaunchedEffect
        onRequestLaunched()
        launcher.handleNextActionForPaymentIntent(r.clientSecret)
    }
}

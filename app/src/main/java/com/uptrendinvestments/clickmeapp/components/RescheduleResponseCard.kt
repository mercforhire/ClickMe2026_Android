package com.uptrendinvestments.clickmeapp.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.buttonstyles.DestructiveOutlinedButton
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButton
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.capNotes
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.formatProposalSlot
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.rememberDeviceTimeFormat
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.DECLINE_REASON_MAX_CHARS
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleDialog
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleProposalUi
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleSubmitting
import com.uptrendinvestments.clickmeapp.screens.login.AuthErrorDialog
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.screens.login.asString
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

const val RESCHEDULE_CARD_TAG = "reschedule_card"
const val RESCHEDULE_ACCEPT_TAG = "reschedule_accept"
const val RESCHEDULE_DECLINE_TAG = "reschedule_decline"
const val RESCHEDULE_DECLINE_REASON_TAG = "reschedule_decline_reason"
const val RESCHEDULE_DECLINE_SHEET_TAG = "reschedule_decline_sheet"

/** Saves the decline reason as plain text; a restored value passes the same cap as typing (D-12, T-15-05). */
internal val DeclineReasonSaver: Saver<String, String> =
    Saver(save = { it }, restore = { capNotes(it, DECLINE_REASON_MAX_CHARS) })

/** Who is looking at the card: the client (Phase 8) or the expert (Phase 11, D-18). */
enum class RescheduleViewer { Client, Expert }

private val ActionHeight = 56.dp
private val NarrowWidth = 320.dp
private const val LARGE_FONT_SCALE = 1.5f

/**
 * The reschedule response card shared by Upcoming Booking Details and Booking Summary (D-01), and by the expert
 * Booking Summary (D-18). It is stateless apart from its confirm dialog and decline sheet: the owner passes the proposal, the
 * in-flight state and the callbacks.
 *
 * For the [RescheduleViewer.Client] viewer an expert-initiated proposal shows Decline and Accept, each behind a confirm
 * dialog, and a client-initiated proposal is read-only with "Awaiting expert's response" (D-03). For the
 * [RescheduleViewer.Expert] viewer it is the other way round, with expert copy and an accept dialog that never
 * mentions payment. Times use [zone] and [timeFormat] so the viewer's zone and 12/24 hour setting win. There is
 * deliberately no action to start a reschedule here (DEF-1).
 */
@Composable
fun RescheduleResponseCard(
    proposal: RescheduleProposalUi,
    submitting: RescheduleSubmitting,
    onAccept: () -> Unit,
    onDecline: (reason: String) -> Unit,
    modifier: Modifier = Modifier,
    viewer: RescheduleViewer = RescheduleViewer.Client,
    zone: ZoneId = ZoneId.systemDefault(),
    timeFormat: java.text.DateFormat = rememberDeviceTimeFormat(),
    locale: Locale = LocalConfiguration.current.locales[0],
) {
    var showAccept by rememberSaveable { mutableStateOf(false) }
    var showDecline by rememberSaveable { mutableStateOf(false) }
    var declineText by rememberSaveable(stateSaver = DeclineReasonSaver) { mutableStateOf("") }
    // The viewer responds to the other party's proposal and only watches their own.
    val actionable = proposal.isActionableFor(viewer)
    // A restored dialog or sheet must not outlive the proposal being actionable (it changed while the app was gone).
    LaunchedEffect(actionable) {
        if (!actionable) {
            showAccept = false
            showDecline = false
        }
    }
    val canRespond = actionable && submitting == RescheduleSubmitting.None

    Surface(
        modifier = modifier.fillMaxWidth().testTag(RESCHEDULE_CARD_TAG),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f)),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            ProposalHeader(viewer = viewer, byViewer = !actionable)
            ProposalTimes(proposal = proposal, zone = zone, timeFormat = timeFormat, locale = locale)
            proposal.note?.let { note -> ProposalNote(note = note, viewer = viewer, byViewer = !actionable) }
            if (actionable) {
                ProposalActions(
                    submitting = submitting,
                    onAcceptClick = { showAccept = true },
                    onDeclineClick = { showDecline = true },
                )
            } else {
                AwaitingPill(viewer = viewer)
            }
        }
    }

    if (showAccept && canRespond) {
        AcceptDialog(
            proposal = proposal,
            viewer = viewer,
            onDismiss = { showAccept = false },
            onConfirm = {
                showAccept = false
                onAccept()
            },
        )
    }
    if (showDecline && canRespond) {
        DeclineSheet(
            viewer = viewer,
            reason = declineText,
            onReasonChange = { declineText = capNotes(it, DECLINE_REASON_MAX_CHARS) },
            onDismiss = { showDecline = false },
            onConfirm = {
                val reason = declineText
                showDecline = false
                declineText = ""
                onDecline(reason)
            },
        )
    }
}

private fun RescheduleProposalUi.isActionableFor(viewer: RescheduleViewer): Boolean =
    when (viewer) {
        RescheduleViewer.Client -> initiatedByExpert
        RescheduleViewer.Expert -> !initiatedByExpert
    }

@Composable
private fun ProposalHeader(
    viewer: RescheduleViewer,
    byViewer: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Icon(
            imageVector = Icons.Outlined.Update,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.tertiary,
        )
        Text(
            text =
                stringResource(
                    when (viewer) {
                        RescheduleViewer.Client ->
                            if (byViewer) R.string.reschedule_title_client else R.string.reschedule_title_expert
                        RescheduleViewer.Expert ->
                            if (byViewer) {
                                R.string.reschedule_expert_title_by_you
                            } else {
                                R.string.reschedule_expert_title_by_client
                            }
                    },
                ),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
    }
}

@Composable
private fun ProposalTimes(
    proposal: RescheduleProposalUi,
    zone: ZoneId,
    timeFormat: java.text.DateFormat,
    locale: Locale,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        TimeBlock(
            label = stringResource(R.string.reschedule_current_time),
            value = formatProposalSlot(proposal.currentStart, zone, locale, timeFormat),
        )
        Icon(
            imageVector = Icons.Outlined.ArrowDownward,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.extras.onSurfaceMuted,
        )
        TimeBlock(
            label = stringResource(R.string.reschedule_proposed_time),
            value = formatProposalSlot(proposal.proposedStart, zone, locale, timeFormat),
        )
    }
}

@Composable
private fun TimeBlock(
    label: String,
    value: String,
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.extras.onSurfaceMuted,
        )
        Text(text = value, style = MaterialTheme.typography.titleMedium)
    }
}

/** The proposer's note is untrusted: plain selectable text only, no links (T-08-21). */
@Composable
private fun ProposalNote(
    note: String,
    viewer: RescheduleViewer,
    byViewer: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            text =
                stringResource(
                    when (viewer) {
                        RescheduleViewer.Client ->
                            if (byViewer) R.string.reschedule_note_client else R.string.reschedule_note_expert
                        RescheduleViewer.Expert ->
                            if (byViewer) {
                                R.string.reschedule_expert_note_you
                            } else {
                                R.string.reschedule_expert_note_client
                            }
                    },
                ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.extras.onSurfaceMuted,
        )
        SelectionContainer {
            Text(text = note, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun AwaitingPill(viewer: RescheduleViewer) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Icon(
                imageVector = Icons.Outlined.HourglassEmpty,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text =
                    stringResource(
                        when (viewer) {
                            RescheduleViewer.Client -> R.string.reschedule_awaiting_expert
                            RescheduleViewer.Expert -> R.string.reschedule_expert_awaiting_client
                        },
                    ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProposalActions(
    submitting: RescheduleSubmitting,
    onAcceptClick: () -> Unit,
    onDeclineClick: () -> Unit,
) {
    val idle = submitting == RescheduleSubmitting.None
    val accepting = submitting == RescheduleSubmitting.Accepting
    val declining = submitting == RescheduleSubmitting.Declining
    val acceptingText = stringResource(R.string.reschedule_accepting)
    val decliningText = stringResource(R.string.reschedule_declining)

    val decline: @Composable (Modifier) -> Unit = { buttonModifier ->
        DestructiveOutlinedButton(
            onClick = onDeclineClick,
            enabled = idle,
            modifier =
                buttonModifier
                    .heightIn(min = ActionHeight)
                    .testTag(RESCHEDULE_DECLINE_TAG)
                    .semantics { if (declining) stateDescription = decliningText },
        ) {
            ActionLabel(text = stringResource(R.string.reschedule_decline), busy = declining)
        }
    }
    val accept: @Composable (Modifier) -> Unit = { buttonModifier ->
        PressScaleButton(
            onClick = onAcceptClick,
            enabled = idle,
            modifier =
                buttonModifier
                    .heightIn(min = ActionHeight)
                    .testTag(RESCHEDULE_ACCEPT_TAG)
                    .semantics { if (accepting) stateDescription = acceptingText },
        ) {
            ActionLabel(text = stringResource(R.string.reschedule_accept), busy = accepting)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val stacked = LocalDensity.current.fontScale >= LARGE_FONT_SCALE || maxWidth < NarrowWidth
        if (stacked) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                accept(Modifier.fillMaxWidth())
                decline(Modifier.fillMaxWidth())
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                decline(Modifier.weight(1f))
                accept(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ActionLabel(
    text: String,
    busy: Boolean,
) {
    if (busy) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            strokeWidth = 2.dp,
            color = LocalContentColor.current,
        )
    } else {
        Text(text = text, textAlign = TextAlign.Center)
    }
}

@Composable
private fun AcceptDialog(
    proposal: RescheduleProposalUi,
    viewer: RescheduleViewer,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val expert = viewer == RescheduleViewer.Expert
    val message =
        stringResource(
            if (expert) R.string.reschedule_expert_accept_dialog_message else R.string.reschedule_accept_dialog_message,
        )
    val disclosure = stringResource(R.string.reschedule_accept_dialog_disclosure)
    val chargesSoon = stringResource(R.string.reschedule_accept_dialog_charges_soon)
    // The 48h caveat only makes sense next to the payment disclosure. An expert never sees payment text (D-18).
    val body =
        buildString {
            append(message)
            if (!expert && proposal.showsPaymentDisclosure) {
                append("\n\n").append(disclosure)
                if (proposal.chargesSoon) append("\n\n").append(chargesSoon)
            }
        }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.extraLarge,
        title = {
            Text(
                text = stringResource(R.string.reschedule_accept_dialog_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.verticalScroll(rememberScrollState()),
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.reschedule_dialog_cancel),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.reschedule_accept),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
    )
}

/**
 * The decline-reason sheet. A text field never sits in a dialog window, which does not follow the IME rules
 * (UI-SPEC R7), so this is a `ModalBottomSheet` whose scrollable body is lifted above the keyboard (R6). The reason and
 * the open state live in the caller's saveable state, so both survive recreation and process death (D-12).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeclineSheet(
    viewer: RescheduleViewer,
    reason: String,
    onReasonChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(start = Spacing.md, end = Spacing.md, bottom = Spacing.md)
                    .testTag(RESCHEDULE_DECLINE_SHEET_TAG),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                text = stringResource(R.string.reschedule_decline_dialog_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.reschedule_decline_dialog_message),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = reason,
                onValueChange = onReasonChange,
                label = { Text(stringResource(R.string.reschedule_decline_reason_label)) },
                maxLines = 4,
                modifier = Modifier.fillMaxWidth().testTag(RESCHEDULE_DECLINE_REASON_TAG),
            )
            Text(
                text = stringResource(R.string.reschedule_decline_reason_counter, reason.length),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.extras.onSurfaceMuted,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(
                        text =
                            stringResource(
                                when (viewer) {
                                    RescheduleViewer.Client -> R.string.reschedule_decline_keep
                                    RescheduleViewer.Expert -> R.string.reschedule_expert_decline_keep
                                },
                            ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                TextButton(onClick = onConfirm) {
                    Text(
                        text = stringResource(R.string.reschedule_decline),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

/**
 * The recovery and error dialogs shown after a reschedule response: add a payment method (D-02), the card can't be
 * used (D-02), or a "Couldn't update booking" error. [onAddPaymentMethod] navigates; the dialog then dismisses itself.
 */
@Composable
fun RescheduleOutcomeDialog(
    dialog: RescheduleDialog,
    onDismiss: () -> Unit,
    onAddPaymentMethod: () -> Unit,
) {
    when (dialog) {
        RescheduleDialog.PaymentMethodNeeded ->
            PaymentMethodDialog(
                message = stringResource(R.string.reschedule_payment_needed_message),
                onDismiss = onDismiss,
                onAddPaymentMethod = onAddPaymentMethod,
            )
        RescheduleDialog.CardUnusable ->
            PaymentMethodDialog(
                message = stringResource(R.string.reschedule_card_unusable_message),
                onDismiss = onDismiss,
                onAddPaymentMethod = onAddPaymentMethod,
            )
        is RescheduleDialog.Error ->
            AuthErrorDialog(
                message = dialog.message.asString(),
                onDismiss = onDismiss,
                title = stringResource(R.string.reschedule_error_title),
            )
    }
}

@Composable
private fun PaymentMethodDialog(
    message: String,
    onDismiss: () -> Unit,
    onAddPaymentMethod: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.extraLarge,
        title = {
            Text(
                text = stringResource(R.string.reschedule_payment_needed_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.reschedule_not_now),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onAddPaymentMethod()
                    onDismiss()
                },
            ) {
                Text(
                    text = stringResource(R.string.reschedule_add_payment_method),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
    )
}

private val PreviewProposal =
    RescheduleProposalUi(
        currentStart = Instant.parse("2026-10-24T18:00:00Z"),
        proposedStart = Instant.parse("2026-10-28T18:00:00Z"),
        proposedEnd = Instant.parse("2026-10-28T19:00:00Z"),
        note = "Can we move this to Wednesday?",
        initiatedByExpert = true,
        showsPaymentDisclosure = false,
        chargesSoon = false,
    )

@Preview(name = "RescheduleResponseCard expert", showBackground = false)
@Composable
private fun RescheduleResponseCardExpertPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            RescheduleResponseCard(
                proposal = PreviewProposal,
                submitting = RescheduleSubmitting.None,
                onAccept = {},
                onDecline = {},
                modifier = Modifier.padding(Spacing.md),
            )
        }
    }
}

@Preview(name = "RescheduleResponseCard client", showBackground = false)
@Composable
private fun RescheduleResponseCardClientPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            RescheduleResponseCard(
                proposal = PreviewProposal.copy(initiatedByExpert = false),
                submitting = RescheduleSubmitting.None,
                onAccept = {},
                onDecline = {},
                modifier = Modifier.padding(Spacing.md),
            )
        }
    }
}

@Preview(name = "RescheduleResponseCard submitting", showBackground = false)
@Composable
private fun RescheduleResponseCardSubmittingPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            RescheduleResponseCard(
                proposal = PreviewProposal,
                submitting = RescheduleSubmitting.Accepting,
                onAccept = {},
                onDecline = {},
                modifier = Modifier.padding(Spacing.md),
            )
        }
    }
}

@Preview(name = "RescheduleOutcomeDialog error", showBackground = false)
@Composable
private fun RescheduleOutcomeDialogPreview() {
    ClickMeTheme {
        RescheduleOutcomeDialog(
            dialog = RescheduleDialog.Error(FieldError.Res(R.string.reschedule_error_not_pending)),
            onDismiss = {},
            onAddPaymentMethod = {},
        )
    }
}

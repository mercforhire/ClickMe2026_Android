package com.uptrendinvestments.clickmeapp.screens.expertmode.cancellation.subviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.avatar.Avatar
import com.uptrendinvestments.clickmeapp.components.buttonstyles.DestructiveOutlinedButton
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButton
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.formatShortDate
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.rememberDeviceTimeFormat
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.formatSessionTimeRange
import com.uptrendinvestments.clickmeapp.screens.expertmode.cancellation.ExpertCancelSummary
import com.uptrendinvestments.clickmeapp.screens.expertmode.cancellation.ExpertRefundPreview
import com.uptrendinvestments.clickmeapp.screens.expertmode.clientDisplayName
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras
import com.uptrendinvestments.clickmeapp.util.Money
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/** Test tag on the "Confirm Cancellation" button. */
const val EXPERT_CANCEL_CONFIRM_TAG = "expert_cancel_confirm"

/** Test tag on the "Keep Booking" button. */
const val EXPERT_CANCEL_KEEP_TAG = "expert_cancel_keep"

private val WarningCircle = 64.dp
private val WarningIcon = 32.dp
private val SummaryAvatar = 48.dp
private val RowIcon = 20.dp
private val ButtonMinHeight = 56.dp
private val SpinnerSize = 24.dp
private const val WARNING_FILL_ALPHA = 0.12f
private const val WARNING_BORDER_ALPHA = 0.3f

/**
 * Step 2 of the expert cancel flow (D-17): the booking summary, what happens to the client's money ([refund]) and the
 * two buttons. Keep Booking is the filled (safe) action and Confirm Cancellation is error-outlined.
 *
 * While [isSubmitting] both buttons are disabled, the confirm button shows a spinner and announces "Cancelling".
 * Content is centred vertically when it fits and scrolls when it does not. Focus moves to the heading on entry so
 * TalkBack announces the step.
 */
@Composable
fun ExpertCancelConfirmStep(
    summary: ExpertCancelSummary,
    refund: ExpertRefundPreview,
    isSubmitting: Boolean,
    onKeepBooking: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault(),
    timeFormat: java.text.DateFormat = rememberDeviceTimeFormat(),
    locale: Locale = LocalConfiguration.current.locales[0],
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val minHeight = maxHeight
        Column(
            modifier =
                Modifier
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = minHeight)
                    .padding(horizontal = Spacing.md, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg, Alignment.CenterVertically),
        ) {
            ConfirmCard(summary = summary, refund = refund, zone = zone, timeFormat = timeFormat, locale = locale)
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                PressScaleButton(
                    onClick = onKeepBooking,
                    enabled = !isSubmitting,
                    modifier = Modifier.fillMaxWidth().heightIn(min = ButtonMinHeight).testTag(EXPERT_CANCEL_KEEP_TAG),
                ) {
                    Text(stringResource(R.string.cancel_booking_keep))
                }
                ConfirmButton(isSubmitting = isSubmitting, onConfirm = onConfirm)
            }
        }
    }
}

@Composable
private fun ConfirmButton(
    isSubmitting: Boolean,
    onConfirm: () -> Unit,
) {
    val cancelling = stringResource(R.string.cancel_booking_cancelling)
    DestructiveOutlinedButton(
        onClick = onConfirm,
        enabled = !isSubmitting,
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = ButtonMinHeight)
                .testTag(EXPERT_CANCEL_CONFIRM_TAG)
                .semantics { if (isSubmitting) stateDescription = cancelling },
    ) {
        if (isSubmitting) {
            CircularProgressIndicator(
                modifier = Modifier.size(SpinnerSize),
                color = MaterialTheme.colorScheme.error,
                strokeWidth = 2.dp,
            )
        } else {
            Icon(imageVector = Icons.Outlined.Cancel, contentDescription = null, modifier = Modifier.size(RowIcon))
            Spacer(Modifier.width(Spacing.sm))
            Text(stringResource(R.string.cancel_booking_confirm))
        }
    }
}

@Composable
private fun ConfirmCard(
    summary: ExpertCancelSummary,
    refund: ExpertRefundPreview,
    zone: ZoneId,
    timeFormat: java.text.DateFormat,
    locale: Locale,
) {
    val headingFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { headingFocus.requestFocus() }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            WarningBadge()
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Text(
                    text = stringResource(R.string.cancel_booking_confirm_title),
                    modifier =
                        Modifier
                            .focusRequester(headingFocus)
                            .focusable()
                            .semantics { heading() },
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.expert_cancel_confirm_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            SummaryCard(summary = summary, refund = refund, zone = zone, timeFormat = timeFormat, locale = locale)
            if (refund is ExpertRefundPreview.FullRefund) {
                Text(
                    text = stringResource(R.string.expert_cancel_refund_note),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.extras.onSurfaceMuted,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun WarningBadge() {
    val tertiary = MaterialTheme.colorScheme.tertiary
    Box(
        modifier =
            Modifier
                .size(WarningCircle)
                .background(tertiary.copy(alpha = WARNING_FILL_ALPHA), CircleShape)
                .border(BorderStroke(1.dp, tertiary.copy(alpha = WARNING_BORDER_ALPHA)), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.WarningAmber,
            contentDescription = null,
            modifier = Modifier.size(WarningIcon),
            tint = tertiary,
        )
    }
}

@Composable
private fun SummaryCard(
    summary: ExpertCancelSummary,
    refund: ExpertRefundPreview,
    zone: ZoneId,
    timeFormat: java.text.DateFormat,
    locale: Locale,
) {
    val whenText =
        stringResource(
            R.string.cancel_booking_date_time,
            formatShortDate(summary.start.atZone(zone).toLocalDate(), locale),
            formatSessionTimeRange(summary.start, summary.end, zone, timeFormat),
        )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Avatar(url = summary.clientAvatarUrl, contentDescription = null, size = SummaryAvatar)
                Column {
                    Text(
                        text = clientDisplayName(summary.clientName),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    summary.topic?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.extras.onSurfaceMuted,
                        )
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                InfoRow(icon = Icons.Outlined.CalendarMonth, text = whenText)
                RefundRow(refund = refund, locale = locale)
            }
        }
    }
}

@Composable
private fun RefundRow(
    refund: ExpertRefundPreview,
    locale: Locale,
) {
    when (refund) {
        is ExpertRefundPreview.HoldReleased -> {
            val amount = formatRefundAmount(refund.amountMajor, refund.currency, locale)
            if (amount == null) {
                InfoRow(Icons.Outlined.Lock, stringResource(R.string.expert_cancel_refund_hold))
            } else {
                val text = stringResource(R.string.expert_cancel_refund_hold_amount, amount)
                InfoRow(Icons.Outlined.Lock, amountEmphasised(text, amount, amountStyle()))
            }
        }
        is ExpertRefundPreview.FullRefund -> {
            val amount = formatRefundAmount(refund.amountMajor, refund.currency, locale)
            if (amount == null) {
                InfoRow(Icons.Outlined.CreditCard, stringResource(R.string.expert_cancel_refund_full_no_amount))
            } else {
                val text = stringResource(R.string.expert_cancel_refund_full, amount)
                InfoRow(Icons.Outlined.CreditCard, amountEmphasised(text, amount, amountStyle()))
            }
        }
        ExpertRefundPreview.None -> Unit
    }
}

@Composable
private fun amountStyle(): SpanStyle = MaterialTheme.typography.titleMedium.toSpanStyle()

/** Null when the currency is missing or blank or the amount is not finite, so the plain sentence is used. */
private fun formatRefundAmount(
    amount: Double,
    currency: String?,
    locale: Locale,
): String? =
    if (currency.isNullOrBlank() || !amount.isFinite()) {
        null
    } else {
        Money.formatMajor(amount, currency, locale)
    }

private fun amountEmphasised(
    text: String,
    amount: String,
    style: SpanStyle,
) = buildAnnotatedString {
    val index = text.indexOf(amount)
    if (index < 0) {
        append(text)
    } else {
        append(text.substring(0, index))
        withStyle(style) { append(amount) }
        append(text.substring(index + amount.length))
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    text: String,
    iconTint: Color = MaterialTheme.extras.onSurfaceMuted,
) = InfoRow(icon, AnnotatedString(text), iconTint)

@Composable
private fun InfoRow(
    icon: ImageVector,
    text: AnnotatedString,
    iconTint: Color = MaterialTheme.extras.onSurfaceMuted,
) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(RowIcon),
            tint = iconTint,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Preview(name = "ExpertCancelConfirmStep")
@Composable
private fun ExpertCancelConfirmStepPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ExpertCancelConfirmStep(
                summary =
                    ExpertCancelSummary(
                        clientName = "Ada Lovelace",
                        clientAvatarUrl = null,
                        topic = "Algorithms",
                        start = Instant.parse("2026-10-24T18:00:00Z"),
                        end = Instant.parse("2026-10-24T19:00:00Z"),
                    ),
                refund = ExpertRefundPreview.HoldReleased(75.0, "USD"),
                isSubmitting = false,
                onKeepBooking = {},
                onConfirm = {},
            )
        }
    }
}

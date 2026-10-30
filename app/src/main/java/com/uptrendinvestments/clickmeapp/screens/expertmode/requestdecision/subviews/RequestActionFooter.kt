package com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision.subviews

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.buttonstyles.DestructiveOutlinedButton
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButton
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButtonDefaults
import com.uptrendinvestments.clickmeapp.components.chat.MessageButton
import com.uptrendinvestments.clickmeapp.components.chat.MessageButtonVariant
import com.uptrendinvestments.clickmeapp.screens.expertmode.clientDisplayName
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.contentMaxWidth

/** Test tag on the footer's Accept button. */
const val REQUEST_ACCEPT_TAG = "request_accept"

/** Test tag on the footer's Decline button. */
const val REQUEST_DECLINE_TAG = "request_decline"

/** Test tag on the Message control, in the footer and in the expired state. */
const val REQUEST_MESSAGE_TAG = "request_message"

/** Test tag on the footer's full-width background surface. */
const val REQUEST_FOOTER_TAG = "request_footer"

private val FooterButtonMinHeight = 56.dp
private const val STACK_BELOW_WIDTH_DP = 320
private const val STACK_AT_FONT_SCALE = 1.5f

/**
 * Pinned Message / Decline / Accept bar of a pending request (surface 3 block 6, UI-SPEC surface 5). A 56 dp Message
 * icon button leads two equal buttons; below 320dp width or at font scale 1.5 and above they stack as Accept,
 * Decline, then the text-form Message for [clientName]. All are disabled while a decision is in flight ([enabled]
 * false). Decline and Accept only open their sheet; Message calls [onMessage] and shows a spinner while [messageBusy].
 */
@Composable
@Suppress("LongParameterList")
fun RequestActionFooter(
    enabled: Boolean,
    onDecline: () -> Unit,
    onAccept: () -> Unit,
    clientName: String?,
    messageBusy: Boolean,
    onMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val messageLabel = stringResource(R.string.chat_entry_message_name, clientDisplayName(clientName))
    val stacked =
        LocalConfiguration.current.screenWidthDp < STACK_BELOW_WIDTH_DP ||
            LocalDensity.current.fontScale >= STACK_AT_FONT_SCALE
    Surface(modifier = modifier.fillMaxWidth().testTag(REQUEST_FOOTER_TAG), color = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            if (stacked) {
                Column(
                    modifier = Modifier.contentMaxWidth().padding(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    AcceptButton(enabled, onAccept, Modifier.fillMaxWidth())
                    DeclineButton(enabled, onDecline, Modifier.fillMaxWidth())
                    MessageButton(
                        label = messageLabel,
                        onClick = onMessage,
                        modifier = Modifier.fillMaxWidth().testTag(REQUEST_MESSAGE_TAG),
                        busy = messageBusy,
                        enabled = enabled,
                    )
                }
            } else {
                Row(
                    modifier = Modifier.contentMaxWidth().padding(Spacing.md),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    MessageButton(
                        label = messageLabel,
                        onClick = onMessage,
                        modifier = Modifier.testTag(REQUEST_MESSAGE_TAG),
                        busy = messageBusy,
                        enabled = enabled,
                        variant = MessageButtonVariant.Icon,
                        contentDescription = messageLabel,
                        iconTarget = FooterButtonMinHeight,
                    )
                    DeclineButton(enabled, onDecline, Modifier.weight(1f))
                    AcceptButton(enabled, onAccept, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DeclineButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    DestructiveOutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = FooterButtonMinHeight).testTag(REQUEST_DECLINE_TAG),
        enabled = enabled,
    ) {
        Text(text = stringResource(R.string.request_decision_decline), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun AcceptButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    PressScaleButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = FooterButtonMinHeight).testTag(REQUEST_ACCEPT_TAG),
        enabled = enabled,
        colors = PressScaleButtonDefaults.filledColors(),
    ) {
        Text(text = stringResource(R.string.request_decision_accept), style = MaterialTheme.typography.labelLarge)
    }
}

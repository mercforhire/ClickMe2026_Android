package com.uptrendinvestments.clickmeapp.screens.chat.reportandblock.subviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.buttonstyles.DestructiveOutlinedButton
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButton
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButtonDefaults
import com.uptrendinvestments.clickmeapp.screens.chat.BlockState
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

/** Test tags of the Block card buttons. */
const val BLOCK_BUTTON_TAG = "block_button"
const val UNBLOCK_BUTTON_TAG = "unblock_button"

private val ButtonMinHeight = 56.dp
private val IconSize = 24.dp
private val SpinnerStroke = 2.dp
private val CardBorderWidth = 1.dp

/**
 * The Block card (UI-SPEC surface 3, CHAT-UNBLOCK section 5 matrix):
 * - not blocked: "Block user" with the destructive outlined button;
 * - blocked by me ([BlockState.showsUnblock]): "You blocked this conversation" with a tonal "Unblock user";
 * - blocked otherwise (the peer, a legacy block, or after a NOT_BLOCKER answer): "You can't reply to this
 *   conversation" and no button.
 *
 * [locked] ignores taps while any action runs. [showActions] is false on a self-thread, where no button is offered.
 */
@Composable
fun BlockCard(
    block: BlockState,
    peerName: String?,
    blockInFlight: Boolean,
    unblockInFlight: Boolean,
    showActions: Boolean,
    onBlock: () -> Unit,
    onUnblock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locked = blockInFlight || unblockInFlight
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(CardBorderWidth, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    text = stringResource(headlineRes(block)),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = bodyText(block, peerName),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (showActions) {
                when {
                    block.showsBlock -> BlockButton(locked = locked, onBlock = onBlock)
                    block.showsUnblock ->
                        UnblockButton(inFlight = unblockInFlight, locked = locked, onUnblock = onUnblock)
                }
            }
        }
    }
}

private fun headlineRes(block: BlockState): Int =
    when {
        block.showsBlock -> R.string.block_card_title
        block.showsUnblock -> R.string.blocked_by_me_title
        else -> R.string.blocked_by_peer_title
    }

@Composable
private fun bodyText(
    block: BlockState,
    peerName: String?,
): String =
    when {
        block.showsBlock -> stringResource(R.string.block_card_body)
        block.showsUnblock ->
            if (peerName.isNullOrBlank()) {
                stringResource(R.string.unblock_body_fallback)
            } else {
                stringResource(R.string.unblock_body, peerName)
            }
        else -> stringResource(R.string.blocked_by_peer_body)
    }

@Composable
private fun BlockButton(
    locked: Boolean,
    onBlock: () -> Unit,
) {
    DestructiveOutlinedButton(
        onClick = { if (!locked) onBlock() },
        modifier = Modifier.fillMaxWidth().heightIn(min = ButtonMinHeight).testTag(BLOCK_BUTTON_TAG),
        enabled = !locked,
    ) {
        Icon(imageVector = Icons.Outlined.Block, contentDescription = null, modifier = Modifier.size(IconSize))
        Spacer(Modifier.width(Spacing.sm))
        Text(text = stringResource(R.string.block_button), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun UnblockButton(
    inFlight: Boolean,
    locked: Boolean,
    onUnblock: () -> Unit,
) {
    val unblocking = stringResource(R.string.unblock_in_flight)
    PressScaleButton(
        onClick = { if (!locked) onUnblock() },
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = ButtonMinHeight)
                .testTag(UNBLOCK_BUTTON_TAG)
                .semantics { if (inFlight) stateDescription = unblocking },
        enabled = !locked,
        colors = PressScaleButtonDefaults.tonalColors(),
    ) {
        if (inFlight) {
            CircularProgressIndicator(
                modifier = Modifier.size(IconSize),
                color = MaterialTheme.colorScheme.onSurface,
                strokeWidth = SpinnerStroke,
            )
        } else {
            Text(text = stringResource(R.string.unblock_button), style = MaterialTheme.typography.labelLarge)
        }
    }
}

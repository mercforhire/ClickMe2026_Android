package com.uptrendinvestments.clickmeapp.screens.chat.chatting.subviews

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.screens.chat.capText
import com.uptrendinvestments.clickmeapp.screens.chat.chatting.CHAT_COMPOSER_MAX
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.contentMaxWidth
import com.uptrendinvestments.clickmeapp.ui.theme.extras

/** Test tag on the composer text field. */
const val CHAT_INPUT_TAG = "chat_input"

/** Test tag on the Send button. */
const val CHAT_SEND_TAG = "chat_send"

private val SendButtonSize = 48.dp
private val SendIconSize = 24.dp
private const val COMPOSER_MAX_LINES = 5
private const val COUNTER_FROM = 9_000
private const val DISABLED_ALPHA = 0.38f

/**
 * The pinned message composer (UI-SPEC surface 2): a field that grows to [COMPOSER_MAX_LINES] lines and a 48 dp Send
 * button that stays in layout. [text] is the ViewModel's draft (already capped); edits are reported through
 * [onTextChange]. The field is disabled while [enabled] is false (thread loading or failed to load) and Send while
 * [canSend] is false. The Scaffold content padding already carries the navigation-bar inset, so only the IME is
 * added here. The surface spans the window while its content is capped at 600 dp on a large window (D-02).
 */
@Composable
@Suppress("LongParameterList")
fun ChatComposer(
    text: String,
    enabled: Boolean,
    canSend: Boolean,
    atCap: Boolean,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    // A local copy keeps the cursor stable while the ViewModel state travels back through the flow.
    var value by remember { mutableStateOf(TextFieldValue(text)) }
    LaunchedEffect(text) {
        if (value.text != text) value = TextFieldValue(text, TextRange(text.length))
    }
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .imePadding()
                .background(MaterialTheme.colorScheme.surface),
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        if (text.length >= COUNTER_FROM) {
            CharacterCounter(length = text.length, atCap = atCap, modifier = Modifier.contentMaxWidth())
        }
        Row(
            modifier = Modifier.contentMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.Bottom,
        ) {
            ComposerField(
                value = value,
                enabled = enabled,
                onValueChange = { entered ->
                    val capped = capText(entered.text, CHAT_COMPOSER_MAX)
                    value =
                        if (capped == entered.text) entered else entered.copy(capped, TextRange(capped.length))
                    if (capped != text) onTextChange(capped)
                },
                focusRequester = focusRequester,
                modifier = Modifier.weight(1f),
            )
            SendButton(
                canSend = canSend,
                onSend = {
                    onSend()
                    // The field keeps focus and the keyboard stays open after a send (UI-SPEC Flow 4).
                    focusRequester.requestFocus()
                },
            )
        }
    }
}

/**
 * "{n}/10000" from [COUNTER_FROM] characters, `tertiary` at the cap. Not a live region below the cap, so typing never
 * speaks the count; at the cap it announces "Message length limit reached" once, politely (UI-SPEC Flow 4).
 */
@Composable
private fun CharacterCounter(
    length: Int,
    atCap: Boolean,
    modifier: Modifier = Modifier,
) {
    val limitReached = stringResource(R.string.chat_composer_limit_reached)
    Text(
        text = stringResource(R.string.chat_composer_counter, length),
        modifier =
            modifier
                .fillMaxWidth()
                .padding(start = Spacing.md, end = Spacing.md, top = Spacing.xs)
                .semantics {
                    if (atCap) {
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = limitReached
                    }
                },
        style = MaterialTheme.typography.bodySmall,
        color = if (atCap) MaterialTheme.colorScheme.tertiary else MaterialTheme.extras.onSurfaceMuted,
        textAlign = TextAlign.End,
    )
}

@Composable
private fun ComposerField(
    value: TextFieldValue,
    enabled: Boolean,
    onValueChange: (TextFieldValue) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = MaterialTheme.shapes.extraLarge
    val scheme = MaterialTheme.colorScheme
    val name = stringResource(R.string.chat_composer_placeholder)
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier =
            modifier
                .border(
                    width = if (focused) 2.dp else 1.dp,
                    color = if (focused) scheme.primary else scheme.outlineVariant,
                    shape = shape,
                ).semantics {
                    // The placeholder is the name only while the field is empty; keep it once text is typed.
                    if (value.text.isNotEmpty()) contentDescription = name
                }.focusRequester(focusRequester)
                .onFocusChanged { focused = it.isFocused }
                .testTag(CHAT_INPUT_TAG),
        enabled = enabled,
        textStyle = MaterialTheme.typography.bodyLarge,
        placeholder = {
            Text(
                text = stringResource(R.string.chat_composer_placeholder),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.extras.onSurfaceMuted,
            )
        },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        minLines = 1,
        maxLines = COMPOSER_MAX_LINES,
        shape = shape,
        colors =
            TextFieldDefaults.colors(
                focusedTextColor = scheme.onSurface,
                unfocusedTextColor = scheme.onSurface,
                focusedContainerColor = scheme.surfaceContainer,
                unfocusedContainerColor = scheme.surfaceContainer,
                disabledContainerColor = scheme.surfaceContainer,
                cursorColor = scheme.primary,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
    )
}

@Composable
private fun SendButton(
    canSend: Boolean,
    onSend: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val disabledReason = stringResource(R.string.chat_composer_send_disabled_a11y)
    IconButton(
        onClick = onSend,
        modifier =
            Modifier
                .size(SendButtonSize)
                .semantics {
                    // A disabled Send says why (UI-SPEC Flow 4).
                    if (!canSend) stateDescription = disabledReason
                }.testTag(CHAT_SEND_TAG),
        enabled = canSend,
        colors =
            IconButtonDefaults.iconButtonColors(
                containerColor = scheme.primary,
                contentColor = scheme.onPrimary,
                disabledContainerColor = scheme.surfaceContainerHigh,
                disabledContentColor = scheme.onSurfaceVariant.copy(alpha = DISABLED_ALPHA),
            ),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.Send,
            contentDescription = stringResource(R.string.chat_composer_send),
            modifier = Modifier.size(SendIconSize),
        )
    }
}

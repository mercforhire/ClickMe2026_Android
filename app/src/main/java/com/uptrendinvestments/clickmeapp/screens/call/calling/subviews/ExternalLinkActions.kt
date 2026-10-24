package com.uptrendinvestments.clickmeapp.screens.call.calling.subviews

import android.content.ClipData
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButton
import com.uptrendinvestments.clickmeapp.screens.call.calling.ExternalMeetingProvider
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.formatSlotTime
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.rememberDeviceTimeFormat
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.time.Instant
import java.time.ZoneId

const val EXTERNAL_OPEN_TAG = "call_external_open"
const val EXTERNAL_COPY_TAG = "call_external_copy"
const val EXTERNAL_COPIED_TAG = "call_external_copied"
const val EXTERNAL_STATUS_TAG = "call_external_status"

/** How long the "Link copied" chip stays after a copy (iOS: fade in, hold 1.5 s, fade out). */
private const val LINK_COPIED_HOLD_MS = 1_500L
private const val CHIP_FADE_MS = 200
private const val CHIP_BORDER_ALPHA = 0.6f
private val OpenMinHeight = 56.dp
private val CopyMinHeight = 48.dp
private val ActionIconSize = 24.dp

/** "Open in Zoom", or the generic "Open Meeting" when the provider cannot be told from the URL (T-09-31). */
@Composable
fun externalOpenLabel(provider: ExternalMeetingProvider): String {
    val providerName =
        when (provider) {
            ExternalMeetingProvider.Skype -> R.string.call_provider_skype
            ExternalMeetingProvider.Zoom -> R.string.call_provider_zoom
            ExternalMeetingProvider.GoogleMeet -> R.string.call_provider_google_meet
            ExternalMeetingProvider.MicrosoftTeams -> R.string.call_provider_teams
            ExternalMeetingProvider.Unknown -> null
        }
    return if (providerName == null) {
        stringResource(R.string.call_external_open_meeting)
    } else {
        stringResource(R.string.call_external_open_in, stringResource(providerName))
    }
}

/**
 * Open and Copy Link for the external (Skype, Zoom, Meet, Teams) variant. Both are disabled until [joinUrl] exists.
 * [onOpen] is the only path that counts as engaging with the meeting; Copy just puts the URL on the clipboard and shows
 * a "Link copied" chip, so copying alone never fires confirm-connection (D-05, T-09-32). The host launches the link.
 */
@Composable
fun ExternalLinkActions(
    provider: ExternalMeetingProvider,
    joinUrl: String?,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }
    val clipLabel = stringResource(R.string.call_external_copy_link)
    val hasLink = joinUrl != null
    val outline = MaterialTheme.colorScheme.outlineVariant
    val copyBorderColor = if (hasLink) outline else outline.copy(alpha = DISABLED_CONTENT_ALPHA)

    LaunchedEffect(copied) {
        if (copied) {
            delay(LINK_COPIED_HOLD_MS)
            copied = false
        }
    }

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        PressScaleButton(
            onClick = onOpen,
            enabled = hasLink,
            modifier = Modifier.fillMaxWidth().heightIn(min = OpenMinHeight).testTag(EXTERNAL_OPEN_TAG),
        ) {
            Icon(
                imageVector = if (provider.isKnown) Icons.Outlined.Videocam else Icons.AutoMirrored.Outlined.OpenInNew,
                contentDescription = null,
                modifier = Modifier.size(ActionIconSize),
            )
            Spacer(Modifier.width(Spacing.sm))
            Text(externalOpenLabel(provider))
        }
        PressScaleButton(
            onClick = {
                if (joinUrl != null) {
                    scope.launch {
                        clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(clipLabel, joinUrl)))
                        copied = true
                    }
                }
            },
            enabled = hasLink,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = CopyMinHeight)
                    .border(1.dp, copyBorderColor, CircleShape)
                    .testTag(EXTERNAL_COPY_TAG),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    disabledContainerColor = Color.Transparent,
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_CONTENT_ALPHA),
                ),
        ) {
            Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(ActionIconSize))
            Spacer(Modifier.width(Spacing.sm))
            Text(clipLabel)
        }
        AnimatedVisibility(
            visible = copied,
            enter = fadeIn(tween(CHIP_FADE_MS)),
            exit = fadeOut(tween(CHIP_FADE_MS)),
        ) {
            Surface(
                modifier =
                    Modifier
                        .testTag(EXTERNAL_COPIED_TAG)
                        .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = CHIP_BORDER_ALPHA)),
            ) {
                Text(
                    text = stringResource(R.string.call_external_link_copied),
                    modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

private const val DISABLED_CONTENT_ALPHA = 0.38f

/**
 * Status copy under the external hero. Before Open it asks the user to open the link; after Open it says the meeting is
 * running and when it is scheduled to end. Unlike iOS it does not promise an automatic end at that time: the call is
 * only cut off 3600 s later (CALL-07), so "scheduled to end" is the honest wording.
 */
@Composable
fun ExternalStatusText(
    hasOpened: Boolean,
    scheduledEnd: Instant?,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault(),
    timeFormat: DateFormat = rememberDeviceTimeFormat(),
) {
    val text =
        when {
            !hasOpened -> stringResource(R.string.call_external_status_not_opened)
            scheduledEnd == null -> stringResource(R.string.call_external_status_opened_no_end)
            else ->
                stringResource(
                    R.string.call_external_status_opened_end,
                    formatSlotTime(scheduledEnd, zone, timeFormat),
                )
        }
    Text(
        text = text,
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg)
                .testTag(EXTERNAL_STATUS_TAG)
                .semantics { liveRegion = LiveRegionMode.Polite },
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Preview(name = "ExternalLinkActions")
@Composable
private fun ExternalLinkActionsPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                ExternalLinkActions(ExternalMeetingProvider.Zoom, "https://zoom.us/j/1", onOpen = {})
                ExternalStatusText(hasOpened = false, scheduledEnd = null)
            }
        }
    }
}

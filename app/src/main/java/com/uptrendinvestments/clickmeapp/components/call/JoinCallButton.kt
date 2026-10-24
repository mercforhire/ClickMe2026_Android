package com.uptrendinvestments.clickmeapp.components.call

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButton
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

private val JoinButtonMinHeight = 56.dp
private val JoinIconSize = 20.dp

/**
 * Shared "Join Call" button for the three Join sites (D-01). Enabled by [isWithinJoinWindow] at the call
 * site; when disabled it exposes why to accessibility. Callers add their own test tag through [modifier].
 */
@Composable
fun JoinCallButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val joinState = stringResource(R.string.explore_join_call_state)
    PressScaleButton(
        onClick = onClick,
        enabled = enabled,
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = JoinButtonMinHeight)
                .then(if (!enabled) Modifier.semantics { stateDescription = joinState } else Modifier),
    ) {
        Icon(
            imageVector = Icons.Outlined.Videocam,
            contentDescription = null,
            modifier = Modifier.size(JoinIconSize),
        )
        Spacer(Modifier.width(Spacing.sm))
        Text(stringResource(R.string.explore_join_call))
    }
}

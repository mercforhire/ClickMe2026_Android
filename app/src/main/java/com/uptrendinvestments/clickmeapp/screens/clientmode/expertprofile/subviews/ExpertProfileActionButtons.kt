package com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.subviews

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.SkeletonBlock
import com.uptrendinvestments.clickmeapp.components.SkeletonGroup
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButton
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

/** Test tag on the enabled Book Session button. */
const val BOOK_SESSION_TAG = "expert_profile_book_session"

private val ButtonMinHeight = 56.dp
private val IconSize = 20.dp

/**
 * Book Session is the only action on the profile (D-05). An expert who is not bookable gets the same pill, disabled
 * and relabelled (D-04); the server still gates the booking itself.
 */
@Composable
fun ExpertProfileActionButtons(
    isBookable: Boolean,
    onBookSession: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val buttonModifier = modifier.fillMaxWidth().heightIn(min = ButtonMinHeight)
    if (isBookable) {
        PressScaleButton(
            onClick = onBookSession,
            modifier = buttonModifier.testTag(BOOK_SESSION_TAG),
        ) {
            Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(IconSize))
            Spacer(Modifier.width(Spacing.sm))
            Text(stringResource(R.string.expert_profile_book_session))
        }
    } else {
        PressScaleButton(
            onClick = {},
            modifier = buttonModifier,
            enabled = false,
        ) {
            Icon(Icons.Outlined.EventBusy, contentDescription = null, modifier = Modifier.size(IconSize))
            Spacer(Modifier.width(Spacing.sm))
            Text(stringResource(R.string.expert_profile_not_bookable))
        }
    }
}

/** A 56 dp pill shown while the details load, so the button never flashes enabled and then disabled (D-15). */
@Composable
fun ExpertProfileActionButtonsSkeleton(modifier: Modifier = Modifier) {
    SkeletonGroup(modifier = modifier.fillMaxWidth()) {
        SkeletonBlock(Modifier.fillMaxWidth().height(ButtonMinHeight), CircleShape)
    }
}

@Preview(name = "ExpertProfileActionButtons")
@Composable
private fun ExpertProfileActionButtonsPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ExpertProfileActionButtons(isBookable = true, onBookSession = {})
        }
    }
}

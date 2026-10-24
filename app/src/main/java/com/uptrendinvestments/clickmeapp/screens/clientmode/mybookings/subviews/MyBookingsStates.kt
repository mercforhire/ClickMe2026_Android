package com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.subviews

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.EmptyState
import com.uptrendinvestments.clickmeapp.components.EmptyStateAction
import com.uptrendinvestments.clickmeapp.components.SkeletonBlock
import com.uptrendinvestments.clickmeapp.components.SkeletonGroup
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingsTab
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.screens.login.asString
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

private const val SKELETON_CARD_COUNT = 3
private val SkeletonAvatar = 56.dp
private val SkeletonButtonHeight = 56.dp

/** Loading placeholder: three skeleton cards shaped like Upcoming cards, announced once as "Loading". */
@Composable
fun MyBookingsSkeleton(modifier: Modifier = Modifier) {
    SkeletonGroup(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(start = Spacing.md, end = Spacing.md, top = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            repeat(SKELETON_CARD_COUNT) { SkeletonCard() }
        }
    }
}

@Composable
private fun SkeletonCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SkeletonBlock(Modifier.size(SkeletonAvatar), shape = CircleShape)
                Spacer(Modifier.width(Spacing.md))
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    SkeletonBlock(Modifier.width(160.dp).height(20.dp))
                    SkeletonBlock(Modifier.width(120.dp).height(16.dp))
                }
            }
            SkeletonBlock(Modifier.width(200.dp).height(16.dp))
            SkeletonBlock(Modifier.fillMaxWidth().height(SkeletonButtonHeight))
        }
    }
}

/** Empty list copy per tab (D-11). Only the Upcoming tab offers an action. */
@Composable
fun MyBookingsEmptyState(
    tab: BookingsTab,
    onExploreExperts: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (tab) {
        BookingsTab.Upcoming ->
            EmptyState(
                icon = Icons.Outlined.CalendarMonth,
                heading = stringResource(R.string.my_bookings_empty_upcoming_title),
                body = stringResource(R.string.my_bookings_empty_upcoming_body),
                modifier = modifier,
                action =
                    EmptyStateAction(
                        label = stringResource(R.string.my_bookings_empty_upcoming_action),
                        onClick = onExploreExperts,
                    ),
            )
        BookingsTab.Past ->
            EmptyState(
                icon = Icons.Outlined.History,
                heading = stringResource(R.string.my_bookings_empty_past_title),
                body = stringResource(R.string.my_bookings_empty_past_body),
                modifier = modifier,
            )
    }
}

/** First-load failure with Retry (D-11). */
@Composable
fun MyBookingsError(
    message: FieldError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EmptyState(
        icon = Icons.Outlined.ErrorOutline,
        heading = stringResource(R.string.my_bookings_load_error_title),
        body = message.asString(),
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
        action = EmptyStateAction(stringResource(R.string.section_retry), onRetry),
    )
}

@Preview(name = "MyBookingsSkeleton")
@Composable
private fun MyBookingsSkeletonPreview() {
    ClickMeTheme { Surface(color = MaterialTheme.colorScheme.surface) { MyBookingsSkeleton() } }
}

@Preview(name = "MyBookingsEmptyState")
@Composable
private fun MyBookingsEmptyStatePreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            MyBookingsEmptyState(tab = BookingsTab.Upcoming, onExploreExperts = {})
        }
    }
}

@Preview(name = "MyBookingsError")
@Composable
private fun MyBookingsErrorPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            MyBookingsError(message = FieldError.Text("Check your connection."), onRetry = {})
        }
    }
}

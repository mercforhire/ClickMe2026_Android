package com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings.subviews

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.components.SkeletonBlock
import com.uptrendinvestments.clickmeapp.components.SkeletonGroup
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

private const val SKELETON_CARD_COUNT = 3
private val SkeletonAvatar = 56.dp
private val SkeletonButtonHeight = 56.dp

/**
 * Loading placeholder: three skeleton cards shaped like the real cards, announced once as "Loading" (UI-SPEC
 * surface 5). [past] renders the Past card shape (a name bar and a badge bar over two text bars) instead of the
 * Upcoming one (avatar, two bars, a date bar and the Join button bar).
 */
@Composable
fun ExpertBookingsSkeleton(
    modifier: Modifier = Modifier,
    past: Boolean = false,
) {
    SkeletonGroup(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(start = Spacing.md, end = Spacing.md, top = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            repeat(SKELETON_CARD_COUNT) { if (past) SkeletonPastCard() else SkeletonUpcomingCard() }
        }
    }
}

@Composable
private fun SkeletonUpcomingCard() {
    SkeletonCardSurface {
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

@Composable
private fun SkeletonPastCard() {
    SkeletonCardSurface {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SkeletonBlock(Modifier.width(160.dp).height(20.dp))
            SkeletonBlock(Modifier.width(80.dp).height(24.dp), shape = CircleShape)
        }
        SkeletonBlock(Modifier.width(200.dp).height(16.dp))
        SkeletonBlock(Modifier.width(140.dp).height(12.dp))
    }
}

@Composable
private fun SkeletonCardSurface(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            content()
        }
    }
}

@Preview(name = "ExpertBookingsSkeleton upcoming")
@Composable
private fun ExpertBookingsSkeletonPreview() {
    ClickMeTheme { Surface(color = MaterialTheme.colorScheme.surface) { ExpertBookingsSkeleton() } }
}

@Preview(name = "ExpertBookingsSkeleton past")
@Composable
private fun ExpertBookingsSkeletonPastPreview() {
    ClickMeTheme { Surface(color = MaterialTheme.colorScheme.surface) { ExpertBookingsSkeleton(past = true) } }
}

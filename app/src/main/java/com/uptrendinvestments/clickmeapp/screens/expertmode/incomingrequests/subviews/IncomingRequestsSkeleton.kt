package com.uptrendinvestments.clickmeapp.screens.expertmode.incomingrequests.subviews

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
import androidx.compose.material3.HorizontalDivider
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

/** Loading placeholder: three skeleton cards shaped like request cards, announced once as "Loading". */
@Composable
fun IncomingRequestsSkeleton(modifier: Modifier = Modifier) {
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
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    SkeletonBlock(Modifier.width(160.dp).height(20.dp))
                    SkeletonBlock(Modifier.width(120.dp).height(16.dp))
                }
                SkeletonBlock(Modifier.width(72.dp).height(20.dp))
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                SkeletonBlock(Modifier.width(200.dp).height(16.dp))
                SkeletonBlock(Modifier.width(120.dp).height(16.dp))
            }
        }
    }
}

@Preview(name = "IncomingRequestsSkeleton")
@Composable
private fun IncomingRequestsSkeletonPreview() {
    ClickMeTheme { Surface(color = MaterialTheme.colorScheme.surface) { IncomingRequestsSkeleton() } }
}

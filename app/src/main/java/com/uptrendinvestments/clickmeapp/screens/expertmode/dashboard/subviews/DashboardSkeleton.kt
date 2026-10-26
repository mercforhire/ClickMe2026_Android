package com.uptrendinvestments.clickmeapp.screens.expertmode.dashboard.subviews

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.components.SkeletonBlock
import com.uptrendinvestments.clickmeapp.components.SkeletonGroup
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

/** Test tag on the [DashboardSkeleton] container. */
const val DASHBOARD_SKELETON_TAG = "dashboard_skeleton"

private val HeroBarWidth = 160.dp
private val HeroBarHeight = 28.dp
private val BannerHeight = 72.dp
private val HeadingHeight = 24.dp
private val SessionHeight = 128.dp
private val ToolHeight = 112.dp
private const val SESSION_COUNT = 2
private const val TOOL_ROWS = 3
private const val TOOLS_PER_ROW = 2

/**
 * First-load placeholder for the Dashboard (UI-SPEC surface 1, Loading): a hero bar, the banner block, then three
 * sections each with a heading bar. One merged "Loading" description. The lower blocks (financial summary and tools)
 * are included now so plan 11-14's sections do not change the first-load layout.
 */
@Composable
fun DashboardSkeleton(modifier: Modifier = Modifier) {
    SkeletonGroup(modifier = modifier.testTag(DASHBOARD_SKELETON_TAG)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md).padding(top = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            SkeletonBlock(modifier = Modifier.width(HeroBarWidth).height(HeroBarHeight))
            SkeletonBlock(
                modifier = Modifier.fillMaxWidth().height(BannerHeight),
                shape = MaterialTheme.shapes.medium,
            )
            SkeletonSection(blockCount = SESSION_COUNT, blockHeight = SessionHeight)
            SkeletonSection(blockCount = 1, blockHeight = SessionHeight)
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                SkeletonBlock(modifier = Modifier.width(HeroBarWidth).height(HeadingHeight))
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    repeat(TOOL_ROWS) {
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                            repeat(TOOLS_PER_ROW) {
                                SkeletonBlock(
                                    modifier = Modifier.weight(1f).height(ToolHeight),
                                    shape = MaterialTheme.shapes.medium,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SkeletonSection(
    blockCount: Int,
    blockHeight: Dp,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        SkeletonBlock(modifier = Modifier.width(HeroBarWidth).height(HeadingHeight))
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            repeat(blockCount) {
                SkeletonBlock(
                    modifier = Modifier.fillMaxWidth().height(blockHeight),
                    shape = MaterialTheme.shapes.medium,
                )
            }
        }
    }
}

@Preview(name = "DashboardSkeleton")
@Composable
private fun DashboardSkeletonPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            DashboardSkeleton()
        }
    }
}

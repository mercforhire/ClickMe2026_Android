package com.uptrendinvestments.clickmeapp.screens.clientmode.explore.subviews

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.SectionError
import com.uptrendinvestments.clickmeapp.components.SectionHeader
import com.uptrendinvestments.clickmeapp.components.SkeletonBlock
import com.uptrendinvestments.clickmeapp.components.SkeletonGroup
import com.uptrendinvestments.clickmeapp.screens.clientmode.explore.CategoryChipUi
import com.uptrendinvestments.clickmeapp.screens.clientmode.explore.SectionState
import com.uptrendinvestments.clickmeapp.screens.login.asString
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

private const val SKELETON_CARD_COUNT = 5

/**
 * Trending Categories (D-09, D-10, D-16). The heading and "View all" action stay in every state; the row shows
 * server chips, five skeleton cards while loading, or a per-section error with Retry.
 */
@Composable
fun ExploreCategoriesSection(
    state: SectionState<List<CategoryChipUi>>,
    selection: Set<String>,
    onChipTap: (String) -> Unit,
    onViewAll: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(
            title = stringResource(R.string.explore_section_trending_categories),
            actionLabel =
                if (selection.isEmpty()) {
                    stringResource(R.string.explore_view_all)
                } else {
                    stringResource(R.string.explore_view_all_count, selection.size)
                },
            onAction = onViewAll,
        )
        Spacer(Modifier.height(Spacing.md))
        when (state) {
            SectionState.Loading ->
                SkeletonGroup {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        repeat(SKELETON_CARD_COUNT) {
                            SkeletonBlock(Modifier.size(width = 112.dp, height = 104.dp), MaterialTheme.shapes.medium)
                        }
                    }
                }
            is SectionState.Error -> SectionError(message = state.message.asString(), onRetry = onRetry)
            is SectionState.Loaded ->
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    items(state.data, key = { it.slug }) { category ->
                        ExploreCategoryCard(
                            category = category,
                            selected = category.slug in selection,
                            onClick = { onChipTap(category.slug) },
                        )
                    }
                }
        }
    }
}

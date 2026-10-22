package com.uptrendinvestments.clickmeapp.screens.clientmode.explore.subviews

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.SectionError
import com.uptrendinvestments.clickmeapp.components.SectionHeader
import com.uptrendinvestments.clickmeapp.components.SkeletonBlock
import com.uptrendinvestments.clickmeapp.components.SkeletonGroup
import com.uptrendinvestments.clickmeapp.screens.clientmode.explore.ExpertCardUi
import com.uptrendinvestments.clickmeapp.screens.clientmode.explore.SectionState
import com.uptrendinvestments.clickmeapp.screens.login.asString
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras

private const val SKELETON_CARD_COUNT = 3
private val SkeletonCardHeight = 128.dp

/**
 * Recommended Experts (D-03, D-16, D-17). The heading is always shown and has no action. Zero experts shows an
 * explicit empty state, with a Clear filter button when a category filter is active.
 */
@Composable
fun ExploreExpertsSection(
    state: SectionState<List<ExpertCardUi>>,
    hasFilter: Boolean,
    onExpertClick: (ExpertCardUi) -> Unit,
    onClearFilter: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = stringResource(R.string.explore_section_recommended_experts))
        Spacer(Modifier.height(Spacing.md))
        when (state) {
            SectionState.Loading ->
                SkeletonGroup {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        repeat(SKELETON_CARD_COUNT) {
                            SkeletonBlock(
                                modifier = Modifier.fillMaxWidth().height(SkeletonCardHeight),
                                shape = MaterialTheme.shapes.medium,
                            )
                        }
                    }
                }
            is SectionState.Error -> SectionError(message = state.message.asString(), onRetry = onRetry)
            is SectionState.Loaded ->
                if (state.data.isEmpty()) {
                    ExpertsEmpty(hasFilter = hasFilter, onClearFilter = onClearFilter)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        state.data.forEach { expert ->
                            ExpertCard(expert = expert, onClick = { onExpertClick(expert) })
                        }
                    }
                }
        }
    }
}

@Composable
private fun ExpertsEmpty(
    hasFilter: Boolean,
    onClearFilter: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text =
                stringResource(
                    if (hasFilter) R.string.explore_experts_empty_filtered else R.string.explore_experts_empty,
                ),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.extras.onSurfaceMuted,
            textAlign = TextAlign.Center,
        )
        if (hasFilter) {
            Spacer(Modifier.height(Spacing.sm))
            TextButton(onClick = onClearFilter, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(
                    text = stringResource(R.string.explore_clear_filter),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textDecoration = TextDecoration.Underline,
                )
            }
        }
    }
}

@Preview(name = "ExploreExpertsSection empty")
@Composable
private fun ExploreExpertsSectionEmptyPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ExploreExpertsSection(
                state = SectionState.Loaded(emptyList()),
                hasFilter = true,
                onExpertClick = {},
                onClearFilter = {},
                onRetry = {},
                modifier = Modifier.padding(Spacing.md),
            )
        }
    }
}

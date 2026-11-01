package com.uptrendinvestments.clickmeapp.screens.clientmode.favorites.subviews

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.backgrounds.GreenGlowBlobLayer
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButton
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButtonDefaults
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.contentMaxWidth
import com.uptrendinvestments.clickmeapp.ui.theme.extras

private val ButtonMinHeight = 56.dp

/**
 * Port of iOS `FavoritesEmptyState`: the glowing star, "No favorites yet", the body, two buttons and the Pro Tip card.
 * Both buttons are wired (D-05 Explore Experts, D-06 View My History; iOS does nothing for either). The column scrolls
 * so pull-to-refresh works from here, and it is a polite live region so TalkBack announces it once when it appears.
 */
@Composable
fun FavoritesEmptyState(
    onExplore: () -> Unit,
    onViewHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        GreenGlowBlobLayer()
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .semantics { liveRegion = LiveRegionMode.Polite },
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.contentMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                FavoritesGlowingStar()
                Spacer(Modifier.height(Spacing.xl))
                Text(
                    text = stringResource(R.string.favorites_empty_title),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(Spacing.md))
                Text(
                    text = stringResource(R.string.favorites_empty_body),
                    modifier = Modifier.padding(horizontal = Spacing.xl),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(Spacing.xl))
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    PressScaleButton(
                        onClick = onExplore,
                        modifier = Modifier.fillMaxWidth().heightIn(min = ButtonMinHeight),
                        colors = PressScaleButtonDefaults.filledColors(),
                    ) {
                        Text(stringResource(R.string.favorites_explore_experts))
                    }
                    PressScaleButton(
                        onClick = onViewHistory,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = ButtonMinHeight)
                                .border(1.dp, MaterialTheme.extras.overlayWhite10, CircleShape),
                        colors = PressScaleButtonDefaults.tonalColors(),
                    ) {
                        Text(stringResource(R.string.favorites_view_history))
                    }
                }
                Spacer(Modifier.height(Spacing.lg))
                // D-08: the Pro Tip lives in the empty state only.
                FavoritesProTipCard(modifier = Modifier.padding(horizontal = Spacing.lg))
                Spacer(Modifier.height(Spacing.lg))
            }
        }
    }
}

@Preview(name = "FavoritesEmptyState")
@Composable
private fun FavoritesEmptyStatePreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            FavoritesEmptyState(onExplore = {}, onViewHistory = {})
        }
    }
}

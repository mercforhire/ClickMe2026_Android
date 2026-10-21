package com.uptrendinvestments.clickmeapp.screens.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.backgrounds.GreenGlowBlobLayer
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButton
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras
import kotlinx.coroutines.launch

/** Test tag on the carousel pager. */
const val ONBOARDING_PAGER_TAG = "onboardingPager"

private const val DOT_ANIM_MS = 200
private const val SCRIM_ALPHA = 0.35f
private const val GRADIENT_START = 0.5f
private val IllustrationHeight = 260.dp
private val DotSize = 8.dp
private val ActiveDotWidth = 24.dp
private val PrimaryButtonMinHeight = 56.dp
private val SkipMinHeight = 48.dp

/**
 * Four-page onboarding carousel (iOS OnboardingView, D-21). Skip (every page) and Get Started (last
 * page) both call [onFinish]. System Back steps to the previous page from pages 2 to 4; on page 1 it is
 * not handled, so the activity exits and the carousel is not marked seen. Deliberately hosted inline
 * rather than in a separate window (RESEARCH Pitfall 11b), so the back dispatcher reaches [BackHandler].
 */
@Composable
fun OnboardingContent(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    pages: List<OnboardingPage> = OnboardingPages.all,
) {
    val pagerState = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.lastIndex

    BackHandler(enabled = pagerState.currentPage > 0) {
        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
    }

    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        GreenGlowBlobLayer(Modifier.matchParentSize())
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).testTag(ONBOARDING_PAGER_TAG),
            ) { index ->
                OnboardingPageContent(pages[index])
            }
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PageDots(pagerState = pagerState, count = pages.size)
                Spacer(Modifier.height(Spacing.md))
                PressScaleButton(
                    onClick = {
                        if (isLastPage) {
                            onFinish()
                        } else {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = PrimaryButtonMinHeight),
                ) {
                    Text(
                        stringResource(if (isLastPage) R.string.onboarding_get_started else R.string.onboarding_next),
                    )
                }
                Spacer(Modifier.height(Spacing.sm))
                TextButton(onClick = onFinish, modifier = Modifier.heightIn(min = SkipMinHeight)) {
                    Text(
                        text = stringResource(R.string.onboarding_skip),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(Spacing.lg))
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = Spacing.xxl)
                .padding(horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Illustration(page.imageRes)
        Spacer(Modifier.height(Spacing.xl))
        Text(
            text = stringResource(page.titleRes),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = stringResource(page.subtitleRes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.extras.onSurfaceMuted,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Illustration(imageRes: Int) {
    val shape = MaterialTheme.shapes.large
    val scrim = MaterialTheme.colorScheme.scrim.copy(alpha = SCRIM_ALPHA)
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(IllustrationHeight)
                .clip(shape)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
    ) {
        Image(
            painter = painterResource(imageRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(GRADIENT_START to Color.Transparent, 1f to scrim),
                    ),
                ),
        )
    }
}

@Composable
private fun PageDots(
    pagerState: PagerState,
    count: Int,
) {
    val current = pagerState.currentPage
    val label = stringResource(R.string.onboarding_page_a11y, current + 1, count)
    Row(
        modifier = Modifier.clearAndSetSemantics { contentDescription = label },
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index -> Dot(active = index == current) }
    }
}

@Composable
private fun Dot(active: Boolean) {
    val width: Dp by animateDpAsState(if (active) ActiveDotWidth else DotSize, tween(DOT_ANIM_MS), label = "dotWidth")
    val color by animateColorAsState(
        targetValue = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.extras.overlayWhite20,
        animationSpec = tween(DOT_ANIM_MS),
        label = "dotColor",
    )
    Box(Modifier.size(width = width, height = DotSize).clip(CircleShape).background(color))
}

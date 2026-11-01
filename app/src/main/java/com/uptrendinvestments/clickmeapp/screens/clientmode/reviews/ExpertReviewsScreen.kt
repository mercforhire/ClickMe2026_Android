package com.uptrendinvestments.clickmeapp.screens.clientmode.reviews

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.EmptyState
import com.uptrendinvestments.clickmeapp.components.ErrorState
import com.uptrendinvestments.clickmeapp.components.RefreshFailedSnackbarEffect
import com.uptrendinvestments.clickmeapp.components.SkeletonBlock
import com.uptrendinvestments.clickmeapp.components.SkeletonGroup
import com.uptrendinvestments.clickmeapp.screens.clientmode.reviews.subviews.ExpertReviewsCard
import com.uptrendinvestments.clickmeapp.screens.clientmode.reviews.subviews.ExpertReviewsOverallCard
import com.uptrendinvestments.clickmeapp.screens.login.asString
import com.uptrendinvestments.clickmeapp.ui.theme.Motion
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.contentMaxWidth

/** Test tag on the first-load skeleton group. */
const val EXPERT_REVIEWS_SKELETON_TAG = "expert_reviews_skeleton"

private val ListBottomPadding = 48.dp
private val OverallSkeletonHeight = 112.dp
private val CardSkeletonHeight = 120.dp
private const val CARD_SKELETON_COUNT = 3
private const val OVERALL_ITEM_KEY = "overall"
private const val EMPTY_ITEM_KEY = "empty"

/**
 * Full Reviews screen over [viewModel] (iOS `ExpertReviewsView`, Phase 16.3 D-16). The route is registered by the
 * navigation shells; [onBack] pops it.
 */
@Composable
fun ExpertReviewsScreen(
    viewModel: ExpertReviewsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ExpertReviewsContent(
        state = state,
        onBack = onBack,
        onRetry = viewModel::retry,
        onRefresh = viewModel::refresh,
        onRefreshFailureConsumed = viewModel::consumeRefreshFailure,
        modifier = modifier,
    )
}

/**
 * Stateless Reviews screen: app bar titled "{FirstName}'s Reviews" (or "Reviews"), then one card per review of page 1
 * (no paging, D-16 scope).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpertReviewsContent(
    state: ExpertReviewsUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onRefreshFailureConsumed: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    RefreshFailedSnackbarEffect(
        refreshFailed = state.refreshFailed,
        snackbarHostState = snackbarHostState,
        onRetry = onRefresh,
        onShown = onRefreshFailureConsumed,
    )
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = { ReviewsTopBar(state.titleFirstName, onBack, scrollBehavior) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            Crossfade(
                targetState = state.load,
                animationSpec = tween(Motion.CONTROL_STATE_DURATION_MS),
                label = "expertReviewsLoad",
            ) { load ->
                when (load) {
                    ExpertReviewsLoad.Loading -> ReviewsSkeleton()
                    is ExpertReviewsLoad.Failed ->
                        ScrollableCentered {
                            ErrorState(
                                onRetry = onRetry,
                                heading = stringResource(R.string.expert_reviews_load_error_title),
                                body = load.message.asString(),
                                retryLabel = stringResource(R.string.section_retry),
                            )
                        }
                    is ExpertReviewsLoad.Loaded -> ReviewsList(load)
                }
            }
        }
    }
}

@Composable
private fun ReviewsList(load: ExpertReviewsLoad.Loaded) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(start = Spacing.md, end = Spacing.md, top = Spacing.sm, bottom = ListBottomPadding),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        item(key = OVERALL_ITEM_KEY) {
            ExpertReviewsOverallCard(load.overallRating, load.totalReviews, Modifier.contentMaxWidth())
        }
        if (load.reviews.isEmpty()) {
            item(key = EMPTY_ITEM_KEY) {
                EmptyState(
                    icon = Icons.Outlined.ChatBubbleOutline,
                    heading = stringResource(R.string.rating_no_reviews),
                    body = stringResource(R.string.expert_reviews_empty_body),
                    modifier = Modifier.padding(top = Spacing.xl).contentMaxWidth(),
                )
            }
        } else {
            items(items = load.reviews, key = { it.id }) { review ->
                ExpertReviewsCard(review, Modifier.contentMaxWidth())
            }
        }
    }
}

/** One 112 dp block for the overall card and three 120 dp blocks for review cards (UI-SPEC E7 loading). */
@Composable
private fun ReviewsSkeleton() {
    SkeletonGroup(Modifier.fillMaxSize().testTag(EXPERT_REVIEWS_SKELETON_TAG)) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = Spacing.md, end = Spacing.md, top = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            SkeletonBlock(Modifier.fillMaxWidth().height(OverallSkeletonHeight), MaterialTheme.shapes.medium)
            repeat(CARD_SKELETON_COUNT) {
                SkeletonBlock(Modifier.fillMaxWidth().height(CardSkeletonHeight), MaterialTheme.shapes.medium)
            }
        }
    }
}

/** Vertically scrollable, centered column so pull-to-refresh works from the error state. */
@Composable
private fun ScrollableCentered(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReviewsTopBar(
    firstName: String?,
    onBack: () -> Unit,
    scrollBehavior: androidx.compose.material3.TopAppBarScrollBehavior,
) {
    TopAppBar(
        title = {
            Text(
                text =
                    if (firstName != null) {
                        stringResource(R.string.expert_reviews_title, firstName)
                    } else {
                        stringResource(R.string.expert_reviews_title_fallback)
                    },
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.a11y_back),
                )
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            ),
        scrollBehavior = scrollBehavior,
    )
}

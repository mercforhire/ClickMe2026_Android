package com.uptrendinvestments.clickmeapp.screens.clientmode.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.SectionAppear
import com.uptrendinvestments.clickmeapp.components.avatar.TopBarAvatarButton
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.ExpertProfileKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.explore.categoriesmodal.AllCategoriesSheet
import com.uptrendinvestments.clickmeapp.screens.clientmode.explore.categoriesmodal.CategoriesSheetViewModel
import com.uptrendinvestments.clickmeapp.screens.clientmode.explore.subviews.ExploreCategoriesSection
import com.uptrendinvestments.clickmeapp.screens.clientmode.explore.subviews.ExploreExpertsSection
import com.uptrendinvestments.clickmeapp.screens.clientmode.explore.subviews.ExploreFeaturedTopicsSection
import com.uptrendinvestments.clickmeapp.screens.clientmode.explore.subviews.ExploreHeroHeader
import com.uptrendinvestments.clickmeapp.screens.clientmode.explore.subviews.ExploreTodaysSessionsSection
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import kotlinx.coroutines.launch

/** Test tag on the Explore home list. */
const val EXPLORE_LIST_TAG = "explore_list"

/**
 * Hilt-aware Explore tab root. Both ViewModels live as long as the Explore Nav3 entry, so the selection, the sheet
 * draft and the scroll position survive tab switches and profile push/pop (D-10).
 */
@Composable
fun ExploreScreen(
    avatarUrl: String?,
    onAvatarClick: () -> Unit,
    onOpenExpert: (ExpertProfileKey) -> Unit,
    onOpenBooking: (bookingId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExploreViewModel = hiltViewModel(),
    sheetViewModel: CategoriesSheetViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showSheet by rememberSaveable { mutableStateOf(false) }
    val nameFallback = stringResource(R.string.explore_expert_name_fallback)

    ExploreContent(
        state = state,
        avatarUrl = avatarUrl,
        onAvatarClick = onAvatarClick,
        onRefresh = viewModel::onRefresh,
        onRetry = viewModel::onRetry,
        onChipTap = viewModel::onChipTap,
        onViewAll = {
            sheetViewModel.open(state.selection)
            showSheet = true
        },
        onClearFilter = viewModel::clearFilter,
        onExpertClick = { onOpenExpert(it.toProfileKey(nameFallback)) },
        onFeaturedTopicClick = { onOpenExpert(it.toProfileKey(nameFallback)) },
        onSessionClick = onOpenBooking,
        onRefreshFailureConsumed = viewModel::onRefreshFailureConsumed,
        modifier = modifier,
    )

    if (showSheet) {
        AllCategoriesSheet(
            viewModel = sheetViewModel,
            onApply = {
                viewModel.applySelection(it)
                showSheet = false
            },
            onDismiss = { showSheet = false },
        )
    }
}

/** Stateless Explore home: app bar, pull-to-refresh and the five sections in iOS order (DISC-01). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreContent(
    state: ExploreUiState,
    avatarUrl: String?,
    onAvatarClick: () -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onChipTap: (String) -> Unit,
    onViewAll: () -> Unit,
    onClearFilter: () -> Unit,
    onExpertClick: (ExpertCardUi) -> Unit,
    onFeaturedTopicClick: (FeaturedTopicUi) -> Unit,
    onSessionClick: (bookingId: String) -> Unit,
    onRefreshFailureConsumed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val refreshFailedMessage = stringResource(R.string.explore_refresh_failed)
    val showToday = state.todaysSessions.isNotEmpty()
    val showFeatured = state.featuredTopics.isVisible()
    val expertsIndex = 2 + (if (showToday) 1 else 0) + (if (showFeatured) 1 else 0)

    LaunchedEffect(state.refreshFailed) {
        if (state.refreshFailed) {
            // Launched on the composition scope so consuming the flag does not cancel the Snackbar.
            scope.launch { snackbarHostState.showSnackbar(refreshFailedMessage) }
            onRefreshFailureConsumed()
        }
    }

    // Applying a filter scrolls to Recommended Experts, but only when its header is off screen.
    var lastSelection by remember { mutableStateOf(state.selection) }
    LaunchedEffect(state.selection) {
        if (state.selection != lastSelection) {
            lastSelection = state.selection
            val headerVisible = listState.layoutInfo.visibleItemsInfo.any { it.key == EXPERTS_KEY }
            if (!headerVisible) listState.animateScrollToItem(expertsIndex)
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tab_explore), style = MaterialTheme.typography.titleLarge) },
                actions = { TopBarAvatarButton(avatarUrl, onClick = onAvatarClick) },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().testTag(EXPLORE_LIST_TAG),
                contentPadding =
                    PaddingValues(start = Spacing.md, end = Spacing.md, top = Spacing.lg, bottom = Spacing.xl),
                verticalArrangement = Arrangement.spacedBy(Spacing.xl),
            ) {
                item(key = "hero") { ExploreHeroHeader() }
                if (showToday) {
                    item(key = "today") {
                        SectionAppear { ExploreTodaysSessionsSection(state.todaysSessions, onSessionClick) }
                    }
                }
                item(key = "categories") {
                    ExploreCategoriesSection(
                        state = state.categories,
                        selection = state.selection,
                        onChipTap = onChipTap,
                        onViewAll = onViewAll,
                        onRetry = onRetry,
                    )
                }
                if (showFeatured) {
                    item(key = "featured") {
                        SectionAppear {
                            ExploreFeaturedTopicsSection(
                                state = state.featuredTopics,
                                onTopicClick = onFeaturedTopicClick,
                                onRetry = onRetry,
                            )
                        }
                    }
                }
                item(key = EXPERTS_KEY) {
                    ExploreExpertsSection(
                        state = state.experts,
                        hasFilter = state.hasFilter,
                        onExpertClick = onExpertClick,
                        onClearFilter = onClearFilter,
                        onRetry = onRetry,
                    )
                }
            }
        }
    }
}

private const val EXPERTS_KEY = "experts"

/** Featured Topics is absent while loading or empty; an error keeps the heading with Retry (D-16). */
private fun SectionState<List<FeaturedTopicUi>>.isVisible(): Boolean =
    when (this) {
        SectionState.Loading -> false
        is SectionState.Error -> true
        is SectionState.Loaded -> data.isNotEmpty()
    }

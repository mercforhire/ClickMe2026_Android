package com.uptrendinvestments.clickmeapp.screens.clientmode.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.ErrorState
import com.uptrendinvestments.clickmeapp.components.RefreshFailedSnackbarEffect
import com.uptrendinvestments.clickmeapp.components.SkeletonBlock
import com.uptrendinvestments.clickmeapp.components.SkeletonGroup
import com.uptrendinvestments.clickmeapp.components.backgrounds.GreenGlowBlobLayer
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.ExpertProfileKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.search.subviews.SearchExpertCard
import com.uptrendinvestments.clickmeapp.screens.clientmode.search.subviews.SearchExpertCategoryChips
import com.uptrendinvestments.clickmeapp.screens.clientmode.search.subviews.SearchExpertGlowingIllustration
import com.uptrendinvestments.clickmeapp.screens.clientmode.search.subviews.SearchExpertSearchBar
import com.uptrendinvestments.clickmeapp.screens.clientmode.search.subviews.SearchExpertSortRow
import com.uptrendinvestments.clickmeapp.screens.login.asString
import com.uptrendinvestments.clickmeapp.ui.theme.contentMaxWidth
import com.uptrendinvestments.clickmeapp.ui.theme.extras

/** Test tag on the Search results list. */
const val SEARCH_RESULTS_LIST_TAG = "search_results_list"

/** Test tag on each of the three skeleton cards shown while a search loads. */
const val SEARCH_SKELETON_CARD_TAG = "search_skeleton_card"

/** Test tag on the inline no-results block. */
const val SEARCH_EMPTY_STATE_TAG = "search_empty_state"

/** Test tag on the inline failed-search block. */
const val SEARCH_ERROR_STATE_TAG = "search_error_state"

private const val SKELETON_CARD_COUNT = 3
private val SkeletonCardHeight = 112.dp
private val CardCorner = 16.dp
private val ListHorizontalPadding = 20.dp
private val ListBottomPadding = 40.dp
private val CardSpacing = 12.dp
private val TopSpacing = 20.dp
private val HeaderSpacing = 14.dp
private val HeaderBottomPadding = 8.dp
private val EmptyVerticalPadding = 40.dp
private val EmptyBodyHorizontalPadding = 32.dp
private val EmptyTextSpacing = 8.dp
private val EmptyColumnSpacing = 20.dp

/**
 * Hilt entry of the Search tab root, a port of iOS `SearchExpertView`. The ViewModel is scoped to the SearchRoot Nav3
 * entry and loads once, so there is deliberately no effect that reloads on appearance: state survives tab switches and
 * re-taps (D-02, D-03).
 */
@Composable
fun SearchExpertScreen(
    onOpenExpert: (ExpertProfileKey) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchExpertViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val nameFallback = stringResource(R.string.explore_expert_name_fallback)
    SearchExpertContent(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onCategorySelected = viewModel::onCategorySelected,
        onSortSelected = viewModel::onSortSelected,
        onExpertClick = { onOpenExpert(it.toProfileKey(nameFallback)) },
        onRefresh = viewModel::onRefresh,
        onRetry = viewModel::onRetry,
        onRefreshFailureConsumed = viewModel::onRefreshFailureConsumed,
        modifier = modifier,
    )
}

/**
 * Port of iOS `SearchExpertView.content`: the green glow layer, a 20 dp top spacer under the status-bar inset (no app
 * bar, D-06) and the results area with pull-to-refresh. Stateless and Hilt-free so it renders under Robolectric and
 * in the shell tests. The search field, category chips and sort row sit right after the spacer, in the iOS order.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchExpertContent(
    state: SearchExpertUiState,
    onQueryChange: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onSortSelected: (SearchSort) -> Unit,
    onExpertClick: (SearchExpertUi) -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onRefreshFailureConsumed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    RefreshFailedSnackbarEffect(
        refreshFailed = state.refreshFailed,
        snackbarHostState = snackbarHostState,
        onRetry = onRefresh,
        onShown = onRefreshFailureConsumed,
    )
    // Hoisted outside the `when`, so the scroll position survives tab switches and loading cycles (D-03).
    val listState = rememberLazyListState()
    // A new result set starts at the top. The last revision seen is saveable so that coming back to the tab (a new
    // composition with the same revision) restores the scroll position instead of resetting it.
    var seenRevision by rememberSaveable { mutableIntStateOf(state.searchRevision) }
    LaunchedEffect(state.searchRevision) {
        if (state.searchRevision != seenRevision) {
            seenRevision = state.searchRevision
            listState.scrollToItem(0)
        }
    }

    Box(modifier.fillMaxSize()) {
        GreenGlowBlobLayer()
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                ),
        ) {
            Spacer(Modifier.height(TopSpacing))
            // Outside the PullToRefreshBox so the controls stay visible and usable in every results state (D-07).
            Column(
                modifier = Modifier.padding(bottom = HeaderBottomPadding),
                verticalArrangement = Arrangement.spacedBy(HeaderSpacing),
            ) {
                SearchExpertSearchBar(
                    query = state.query,
                    onQueryChange = onQueryChange,
                    modifier = Modifier.padding(horizontal = ListHorizontalPadding),
                )
                SearchExpertCategoryChips(
                    categories = state.categories,
                    selectedSlug = state.selectedCategorySlug,
                    onSelect = onCategorySelected,
                )
                SearchExpertSortRow(
                    sort = state.sort,
                    onSortSelected = onSortSelected,
                    modifier = Modifier.padding(horizontal = ListHorizontalPadding),
                )
            }
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                when (val results = state.results) {
                    SearchResults.Loading -> SearchSkeletonList()
                    is SearchResults.Failed ->
                        ScrollableCentered {
                            ErrorState(
                                onRetry = onRetry,
                                modifier = Modifier.testTag(SEARCH_ERROR_STATE_TAG),
                                heading = stringResource(R.string.search_error_heading),
                                body = results.message.asString(),
                                retryLabel = stringResource(R.string.section_retry),
                            )
                        }
                    is SearchResults.Loaded ->
                        if (results.experts.isEmpty()) {
                            ScrollableCentered { SearchEmptyResults() }
                        } else {
                            SearchResultsList(results.experts, listState, onExpertClick)
                        }
                }
            }
        }
        // In the content, not a shell slot: the slot is offset by the system bars only, so while the search field has
        // focus the refresh-failed Retry would sit under the keyboard.
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter).imePadding())
    }
}

/**
 * Result cards. Uses default index keys: the tag-union search RPC does not guarantee unique expert ids, and a
 * duplicate explicit key would crash the list.
 */
@Composable
private fun SearchResultsList(
    experts: List<SearchExpertUi>,
    listState: LazyListState,
    onExpertClick: (SearchExpertUi) -> Unit,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().testTag(SEARCH_RESULTS_LIST_TAG),
        contentPadding =
            PaddingValues(
                start = ListHorizontalPadding,
                end = ListHorizontalPadding,
                bottom = ListBottomPadding,
            ),
        verticalArrangement = Arrangement.spacedBy(CardSpacing),
    ) {
        items(experts) { expert ->
            SearchExpertCard(
                expert = expert,
                onClick = { onExpertClick(expert) },
                modifier = Modifier.contentMaxWidth(),
            )
        }
    }
}

/** Three skeleton cards (D-07), vertically scrollable so the pull gesture works while loading. */
@Composable
private fun SearchSkeletonList() {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = ListHorizontalPadding, end = ListHorizontalPadding, bottom = ListBottomPadding),
    ) {
        SkeletonGroup {
            Column(verticalArrangement = Arrangement.spacedBy(CardSpacing)) {
                repeat(SKELETON_CARD_COUNT) {
                    Box(Modifier.testTag(SEARCH_SKELETON_CARD_TAG)) {
                        SkeletonBlock(
                            modifier = Modifier.fillMaxWidth().height(SkeletonCardHeight),
                            shape = RoundedCornerShape(CardCorner),
                        )
                    }
                }
            }
        }
    }
}

/** Inline no-results status (D-06): a status message with no action buttons, the search controls stay usable. */
@Composable
private fun SearchEmptyResults() {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(SEARCH_EMPTY_STATE_TAG)
                .padding(vertical = EmptyVerticalPadding)
                .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(EmptyColumnSpacing),
    ) {
        SearchExpertGlowingIllustration()
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(EmptyTextSpacing),
        ) {
            Text(
                text = stringResource(R.string.search_empty_title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.search_empty_body),
                modifier = Modifier.padding(horizontal = EmptyBodyHorizontalPadding),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.extras.onSurfaceMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Vertically scrollable, centered column so pull-to-refresh works from the empty and error states. */
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

package com.uptrendinvestments.clickmeapp.screens.clientmode.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.ExpertProfileKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.favorites.subviews.FavoriteExpertCard
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.contentMaxWidth

/** Test tag on the populated Saved Experts list. */
const val FAVORITES_LIST_TAG = "favorites_list"

private val ListBottomPadding = 48.dp

/**
 * Hilt-aware Saved Experts screen (iOS `FavoritesView`, PARITY-AUDIT #2).
 *
 * D-09: the screen reports it was shown on resume and on every (re)entry to composition, and hidden on pause and
 * disposal, so the ViewModel reloads when the screen comes back into view. [onExplore] and [onViewHistory] are the
 * shell's tab switches (D-05, D-06); "View My History" first posts the Past launch, then switches.
 */
@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel,
    onBack: () -> Unit,
    onOpenExpert: (ExpertProfileKey) -> Unit,
    onExplore: () -> Unit,
    onViewHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onScreenShown() }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { viewModel.onScreenHidden() }
    DisposableEffect(viewModel) { onDispose { viewModel.onScreenHidden() } }
    // The Nav3 entry may not receive ON_RESUME when it is re-shown; it leaves and re-enters composition instead.
    LaunchedEffect(Unit) { viewModel.onScreenShown() }

    val nameFallback = stringResource(R.string.explore_expert_name_fallback)
    FavoritesContent(
        state = state,
        onBack = onBack,
        onOpenExpert = { onOpenExpert(it.toProfileKey(nameFallback)) },
        onRemoveConfirmed = viewModel::remove,
        onExplore = onExplore,
        onViewHistory = {
            // D-06: post the launch first, then switch tabs, so My Bookings finds it when it appears.
            viewModel.viewHistory()
            onViewHistory()
        },
        onRetry = viewModel::retry,
        onRefresh = viewModel::refresh,
        onRefreshFailureConsumed = viewModel::consumeRefreshFailure,
        modifier = modifier,
    )
}

/**
 * Stateless Saved Experts: the "Favorite Experts" app bar and the list of cards (iOS `FavoritesView`). The other
 * states are added alongside the list.
 */
@Suppress("UNUSED_PARAMETER", "LongParameterList")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesContent(
    state: FavoritesUiState,
    onBack: () -> Unit,
    onOpenExpert: (FavoriteExpertUi) -> Unit,
    onRemoveConfirmed: (expertId: String) -> Unit,
    onExplore: () -> Unit,
    onViewHistory: () -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onRefreshFailureConsumed: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.favorites_title),
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (val load = state.load) {
                is FavoritesLoad.Loaded ->
                    if (load.experts.isNotEmpty()) {
                        FavoritesList(experts = load.experts, onOpenExpert = onOpenExpert, onRemove = {})
                    }
                else -> Unit
            }
        }
    }
}

@Composable
private fun FavoritesList(
    experts: List<FavoriteExpertUi>,
    onOpenExpert: (FavoriteExpertUi) -> Unit,
    onRemove: (FavoriteExpertUi) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag(FAVORITES_LIST_TAG),
        contentPadding = PaddingValues(start = Spacing.md, end = Spacing.md, bottom = ListBottomPadding),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        item(key = "tagline") {
            Text(
                text = stringResource(R.string.favorites_tagline),
                // Bottom lg in the spec: 8 dp here plus the 16 dp list gap.
                modifier = Modifier.contentMaxWidth().padding(top = Spacing.md, bottom = Spacing.sm),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(items = experts, key = { it.expertId }) { expert ->
            FavoriteExpertCard(
                expert = expert,
                onOpen = { onOpenExpert(expert) },
                onRemove = { onRemove(expert) },
                modifier = Modifier.animateItem().contentMaxWidth(),
            )
        }
    }
}

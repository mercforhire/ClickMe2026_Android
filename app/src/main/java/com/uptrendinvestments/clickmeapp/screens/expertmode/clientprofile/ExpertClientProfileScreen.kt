package com.uptrendinvestments.clickmeapp.screens.expertmode.clientprofile

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.EmptyState
import com.uptrendinvestments.clickmeapp.components.EmptyStateAction
import com.uptrendinvestments.clickmeapp.components.ErrorState
import com.uptrendinvestments.clickmeapp.components.RefreshFailedSnackbarEffect
import com.uptrendinvestments.clickmeapp.components.SkeletonBlock
import com.uptrendinvestments.clickmeapp.components.SkeletonGroup
import com.uptrendinvestments.clickmeapp.screens.expertmode.clientDisplayName
import com.uptrendinvestments.clickmeapp.screens.expertmode.clientprofile.subviews.ClientProfileAvatar
import com.uptrendinvestments.clickmeapp.screens.expertmode.clientprofile.subviews.ClientProfileBookingHistoryHeader
import com.uptrendinvestments.clickmeapp.screens.expertmode.clientprofile.subviews.ClientProfileBookingHistoryRow
import com.uptrendinvestments.clickmeapp.screens.expertmode.clientprofile.subviews.ClientProfileNameBadge
import com.uptrendinvestments.clickmeapp.screens.expertmode.clientprofile.subviews.ClientProfilePersonalInfo
import com.uptrendinvestments.clickmeapp.screens.expertmode.clientprofile.subviews.ClientProfileRelationshipCard
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.screens.login.asString
import com.uptrendinvestments.clickmeapp.ui.theme.Motion
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.contentMaxWidth
import java.time.ZoneId
import java.util.Locale

/** Test tag on the loaded list. */
const val CLIENT_PROFILE_LIST_TAG = "client_profile_list"

/** Test tag on the first-load skeleton group. */
const val CLIENT_PROFILE_SKELETON_TAG = "client_profile_skeleton"

private val ListBottomPadding = Spacing.xxl
private val SkeletonAvatarSize = 142.dp
private val SkeletonNameWidth = 180.dp
private val SkeletonNameHeight = 28.dp
private val SkeletonCardHeight = 72.dp
private const val SKELETON_CARD_COUNT = 3

/**
 * The expert's read-only Client Profile over [viewModel] (port of iOS `ClientProfileView`). The route is registered by
 * the expert shell; [onBack] pops it.
 */
@Composable
fun ExpertClientProfileScreen(
    viewModel: ExpertClientProfileViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ExpertClientProfileContent(
        state = state,
        onBack = onBack,
        onRetry = viewModel::retry,
        onRefresh = viewModel::refresh,
        onRefreshFailureConsumed = viewModel::consumeRefreshFailure,
        modifier = modifier,
    )
}

/**
 * Stateless Client Profile: a "Client Profile" bar with a back arrow over a pull-to-refresh body, cross-faded between
 * the load states. [zone] and [locale] format the dates; they default to the device's.
 *
 * States (Phase 16.4, Phase 15 visuals in place of the iOS centered spinner):
 * - Loading is a skeleton, not a spinner.
 * - Failed (D-12) is the generic [ErrorState] with Retry; the ViewModel never offers a partial history.
 * - NoRelationship (D-14) is reachable because `GET /chats` is not filtered by mode, so an expert-mode chat list can
 *   hold someone who never booked this expert. Unavailable (D-15) is a 404. A retry cannot change either answer, so
 *   both offer "Go back" instead of Retry; iOS shows a generic error with Retry for both.
 * - Every state sits in a scrollable column, so pull-to-refresh works from all of them, as on iOS.
 * - A failed pull over loaded content keeps it and shows the refresh-failed snackbar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpertClientProfileContent(
    state: ExpertClientProfileUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onRefreshFailureConsumed: () -> Unit,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault(),
    locale: Locale = LocalConfiguration.current.locales[0],
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    RefreshFailedSnackbarEffect(
        refreshFailed = state.refreshFailed,
        snackbarHostState = snackbarHostState,
        onRetry = onRefresh,
        onShown = onRefreshFailureConsumed,
    )
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = { AuthTopAppBar(title = stringResource(R.string.client_profile_title), onBack = onBack) },
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
                label = "clientProfileLoad",
            ) { load ->
                when (load) {
                    ClientProfileLoad.Loading -> ProfileSkeleton()
                    is ClientProfileLoad.Failed ->
                        ScrollableColumn(Arrangement.Center) {
                            ErrorState(
                                onRetry = onRetry,
                                heading = stringResource(R.string.client_profile_load_error),
                                body = load.message.asString(),
                                retryLabel = stringResource(R.string.section_retry),
                            )
                        }
                    ClientProfileLoad.NoRelationship ->
                        FriendlyState(
                            heading = stringResource(R.string.client_profile_no_relationship_title),
                            body = stringResource(R.string.client_profile_no_relationship_body),
                            onBack = onBack,
                        )
                    ClientProfileLoad.Unavailable ->
                        FriendlyState(
                            heading = stringResource(R.string.client_profile_unavailable_title),
                            body = stringResource(R.string.client_profile_unavailable_body),
                            onBack = onBack,
                        )
                    is ClientProfileLoad.Loaded -> ProfileList(load, zone, locale)
                }
            }
        }
    }
}

/** The 403 and 404 states: an icon, the copy and a "Go back" action, no Retry. */
@Composable
private fun FriendlyState(
    heading: String,
    body: String,
    onBack: () -> Unit,
) {
    ScrollableColumn(Arrangement.Top) {
        EmptyState(
            icon = Icons.Outlined.PersonOff,
            heading = heading,
            body = body,
            modifier = Modifier.padding(top = Spacing.xxxl),
            action = EmptyStateAction(stringResource(R.string.expert_profile_go_back), onBack),
        )
    }
}

/** A 142 dp circle, a name line and three card blocks (UI-SPEC loading), announced once as "Loading". */
@Composable
private fun ProfileSkeleton() {
    SkeletonGroup(Modifier.fillMaxSize().testTag(CLIENT_PROFILE_SKELETON_TAG)) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = Spacing.md, end = Spacing.md, top = Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            SkeletonBlock(Modifier.size(SkeletonAvatarSize), CircleShape)
            SkeletonBlock(Modifier.width(SkeletonNameWidth).height(SkeletonNameHeight))
            repeat(SKELETON_CARD_COUNT) {
                SkeletonBlock(
                    Modifier.contentMaxWidth().height(SkeletonCardHeight),
                    MaterialTheme.shapes.medium,
                )
            }
        }
    }
}

/** Vertically scrollable column so pull-to-refresh works from the error and friendly states. */
@Composable
private fun ScrollableColumn(
    arrangement: Arrangement.Vertical,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = arrangement,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        content()
    }
}

/**
 * The loaded profile in the iOS order: avatar and name block, About, Your Sessions Together, then the history when it
 * has rows. Items sit [Spacing.md] apart and each section after the first adds another [Spacing.md] on top, so
 * sections are [Spacing.xl] apart while history rows stay [Spacing.md] apart.
 */
@Composable
private fun ProfileList(
    load: ClientProfileLoad.Loaded,
    zone: ZoneId,
    locale: Locale,
) {
    val profile = load.profile
    val name = clientDisplayName(profile.personalDetails.fullName)
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag(CLIENT_PROFILE_LIST_TAG),
        contentPadding =
            PaddingValues(start = Spacing.md, end = Spacing.md, top = Spacing.md, bottom = ListBottomPadding),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        item(key = "header") {
            Column(
                modifier = Modifier.contentMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                ClientProfileAvatar(profile.personalDetails.avatarUrl)
                ClientProfileNameBadge(name)
            }
        }
        item(key = "about") {
            ClientProfilePersonalInfo(profile, zone, locale, Modifier.contentMaxWidth().padding(top = Spacing.md))
        }
        item(key = "relationship") {
            ClientProfileRelationshipCard(
                profile.relationship,
                zone,
                locale,
                Modifier.contentMaxWidth().padding(top = Spacing.md),
            )
        }
        if (load.history.isNotEmpty()) {
            item(key = "history-header") {
                ClientProfileBookingHistoryHeader(Modifier.contentMaxWidth().padding(top = Spacing.md))
            }
            items(items = load.history, key = { it.item.bookingId }) { row ->
                ClientProfileBookingHistoryRow(row, zone, locale, Modifier.contentMaxWidth())
            }
        }
    }
}

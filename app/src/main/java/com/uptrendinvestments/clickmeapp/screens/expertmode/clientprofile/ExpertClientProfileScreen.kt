package com.uptrendinvestments.clickmeapp.screens.expertmode.clientprofile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.responses.ExpertClientProfileData
import com.uptrendinvestments.clickmeapp.screens.expertmode.clientDisplayName
import com.uptrendinvestments.clickmeapp.screens.expertmode.clientprofile.subviews.ClientProfileAvatar
import com.uptrendinvestments.clickmeapp.screens.expertmode.clientprofile.subviews.ClientProfileNameBadge
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.contentMaxWidth
import java.time.ZoneId
import java.util.Locale

/** Test tag on the loaded list. */
const val CLIENT_PROFILE_LIST_TAG = "client_profile_list"

private val ListBottomPadding = Spacing.xxl

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
 * Stateless Client Profile: a "Client Profile" bar with a back arrow over a pull-to-refresh body. [zone] and [locale]
 * format the dates; they default to the device's.
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
            val load = state.load
            if (load is ClientProfileLoad.Loaded) {
                ProfileList(load.profile, zone, locale)
            }
        }
    }
}

@Composable
private fun ProfileList(
    profile: ExpertClientProfileData,
    zone: ZoneId,
    locale: Locale,
) {
    val name = clientDisplayName(profile.personalDetails.fullName)
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag(CLIENT_PROFILE_LIST_TAG),
        contentPadding =
            PaddingValues(start = Spacing.md, end = Spacing.md, top = Spacing.md, bottom = ListBottomPadding),
        verticalArrangement = Arrangement.spacedBy(Spacing.xl),
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
    }
}

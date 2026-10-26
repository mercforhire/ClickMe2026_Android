package com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.EmptyState
import com.uptrendinvestments.clickmeapp.components.EmptyStateAction
import com.uptrendinvestments.clickmeapp.components.SkeletonBlock
import com.uptrendinvestments.clickmeapp.components.SkeletonGroup
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.rememberDeviceTimeFormat
import com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision.subviews.RequestClientHeader
import com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision.subviews.RequestDetailsCard
import com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision.subviews.RequestExpiredBanner
import com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision.subviews.RequestMessageCard
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.screens.login.asString
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import java.time.ZoneId
import java.util.Locale

private val HeaderSkeletonAvatar = 72.dp
private val NameSkeletonWidth = 160.dp
private val NameSkeletonHeight = 24.dp
private val SubSkeletonWidth = 96.dp
private val SubSkeletonHeight = 16.dp
private val DetailsSkeletonHeight = 160.dp
private val MessageSkeletonHeight = 96.dp
private val EarningsSkeletonHeight = 128.dp

/**
 * Request Decision (EXP-06): the client, schedule, message and earnings of one pending request. Plan 11-12 adds the
 * accept and decline footer and sheets on top of this screen.
 */
@Composable
fun RequestDecisionScreen(
    viewModel: RequestDecisionViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RequestDecisionContent(state = state, onBack = onBack, onRetry = viewModel::retry, modifier = modifier)
}

/** Stateless Request Decision screen. [zone] and [timeFormat] are parameters so tests are deterministic. */
@Composable
fun RequestDecisionContent(
    state: RequestDecisionUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault(),
    timeFormat: java.text.DateFormat = rememberDeviceTimeFormat(),
    locale: Locale = LocalConfiguration.current.locales[0],
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = { AuthTopAppBar(title = stringResource(R.string.request_decision_title), onBack = onBack) },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (val load = state.load) {
                RequestDecisionLoad.Loading -> LoadingBody()
                is RequestDecisionLoad.Failed -> FailedBody(message = load.message.asString(), onRetry = onRetry)
                is RequestDecisionLoad.Loaded ->
                    LoadedBody(
                        details = load.details,
                        isExpired = state.isExpired,
                        zone = zone,
                        timeFormat = timeFormat,
                        locale = locale,
                    )
            }
        }
    }
}

@Composable
private fun LoadedBody(
    details: RequestDecisionDetails,
    isExpired: Boolean,
    zone: ZoneId,
    timeFormat: java.text.DateFormat,
    locale: Locale,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = Spacing.md, end = Spacing.md, top = Spacing.md, bottom = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        if (isExpired) RequestExpiredBanner()
        RequestClientHeader(name = details.clientName, avatarUrl = details.clientAvatarUrl)
        RequestDetailsCard(
            start = details.start,
            end = details.end,
            topic = details.topic,
            meetingType = details.meetingType,
            zone = zone,
            timeFormat = timeFormat,
            locale = locale,
        )
        details.clientMessage?.let { RequestMessageCard(message = it) }
    }
}

@Composable
private fun LoadingBody() {
    SkeletonGroup(modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }) {
        Column(
            modifier = Modifier.padding(start = Spacing.md, end = Spacing.md, top = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                SkeletonBlock(Modifier.size(HeaderSkeletonAvatar), shape = CircleShape)
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    SkeletonBlock(Modifier.width(NameSkeletonWidth).height(NameSkeletonHeight))
                    SkeletonBlock(Modifier.width(SubSkeletonWidth).height(SubSkeletonHeight))
                }
            }
            SkeletonBlock(Modifier.fillMaxWidth().height(DetailsSkeletonHeight))
            SkeletonBlock(Modifier.fillMaxWidth().height(MessageSkeletonHeight))
            SkeletonBlock(Modifier.fillMaxWidth().height(EarningsSkeletonHeight))
        }
    }
}

@Composable
private fun FailedBody(
    message: String,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize().semantics { liveRegion = LiveRegionMode.Polite },
        contentAlignment = Alignment.Center,
    ) {
        EmptyState(
            icon = Icons.Outlined.ErrorOutline,
            heading = stringResource(R.string.request_decision_load_error),
            body = message,
            action = EmptyStateAction(stringResource(R.string.expert_retry), onRetry),
        )
    }
}

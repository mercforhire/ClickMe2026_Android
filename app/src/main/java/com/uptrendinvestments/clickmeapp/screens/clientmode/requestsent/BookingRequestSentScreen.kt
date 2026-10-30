package com.uptrendinvestments.clickmeapp.screens.clientmode.requestsent

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.EmptyState
import com.uptrendinvestments.clickmeapp.components.EmptyStateAction
import com.uptrendinvestments.clickmeapp.components.PaymentLifecycleCard
import com.uptrendinvestments.clickmeapp.components.PaymentLifecycleStage
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.formatSessionWhen
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.rememberDeviceTimeFormat
import com.uptrendinvestments.clickmeapp.screens.clientmode.requestsent.subviews.BookingRequestSentActions
import com.uptrendinvestments.clickmeapp.screens.clientmode.requestsent.subviews.BookingRequestSentCheckIcon
import com.uptrendinvestments.clickmeapp.screens.clientmode.requestsent.subviews.BookingRequestSentDetailsCard
import com.uptrendinvestments.clickmeapp.screens.clientmode.requestsent.subviews.BookingRequestSentHeadline
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.contentMaxWidth
import com.uptrendinvestments.clickmeapp.ui.theme.extras
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.time.ZoneId

private const val ANIM_DURATION_MS = 300
private const val ICON_DELAY_MS = 0
private const val HEADLINE_DELAY_MS = 150
private const val CARD_DELAY_MS = 250
private const val ACTIONS_DELAY_MS = 400
private const val ICON_START_SCALE = 0.6f
private val SlideDistance = 16.dp

/** Test tag on the paid-booking [PaymentLifecycleCard] shown below the details card. */
internal const val REQUEST_SENT_PAYMENT_CARD_TAG = "request_sent_payment_card"

/** Test tag on the capped content column of the loaded Request Sent screen. */
internal const val REQUEST_SENT_CONTENT_TAG = "request_sent_content"

/**
 * Port of iOS BookingRequestSentView. Unlike iOS it replaces the booking screen in the back stack (D-12), so Back
 * from here reaches Expert Profile. The ViewModel fetches the booking by id; the 700 ms entry animation (D-15)
 * replaces the iOS staggered animation with glow pulse and plays once per successful load.
 */
@Composable
fun BookingRequestSentScreen(
    viewModel: BookingRequestSentViewModel,
    onBack: () -> Unit,
    onViewBookings: () -> Unit,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BookingRequestSentContent(
        state = state,
        onBack = onBack,
        onRetry = viewModel::retry,
        onViewBookings = onViewBookings,
        onBackToHome = onBackToHome,
        modifier = modifier,
    )
}

/** Stateless Request Sent screen: loading, failed (with Retry) and loaded states under a "Booking Request" bar. */
@Composable
fun BookingRequestSentContent(
    state: BookingRequestSentUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onViewBookings: () -> Unit,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault(),
    timeFormat: java.text.DateFormat = rememberDeviceTimeFormat(),
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = { AuthTopAppBar(title = stringResource(R.string.request_sent_title), onBack = onBack) },
    ) { innerPadding ->
        when (state) {
            BookingRequestSentUiState.Loading -> LoadingBody(Modifier.padding(innerPadding))
            BookingRequestSentUiState.Failed -> FailedBody(onRetry, Modifier.padding(innerPadding))
            is BookingRequestSentUiState.Loaded ->
                LoadedBody(
                    details = state.details,
                    onViewBookings = onViewBookings,
                    onBackToHome = onBackToHome,
                    zone = zone,
                    timeFormat = timeFormat,
                    contentPadding = innerPadding,
                )
        }
    }
}

@Composable
private fun LoadingBody(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = stringResource(R.string.request_sent_loading),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.extras.onSurfaceMuted,
        )
    }
}

@Composable
private fun FailedBody(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize().semantics { liveRegion = LiveRegionMode.Polite },
        contentAlignment = Alignment.Center,
    ) {
        // Fixed copy rather than the server message: the booking exists (UI-SPEC deviation).
        EmptyState(
            icon = Icons.Outlined.CloudOff,
            heading = stringResource(R.string.request_sent_error_heading),
            body = stringResource(R.string.request_sent_error_body),
            action = EmptyStateAction(stringResource(R.string.section_retry), onRetry),
        )
    }
}

@Composable
private fun LoadedBody(
    details: BookingRequestSentDetails,
    onViewBookings: () -> Unit,
    onBackToHome: () -> Unit,
    zone: ZoneId,
    timeFormat: java.text.DateFormat,
    contentPadding: PaddingValues,
) {
    // Saved so rotation or returning to the screen does not replay the entry (D-15). Set when the animation ends.
    var played by rememberSaveable { mutableStateOf(false) }
    val initial = if (played) 1f else 0f
    val progress = remember { List(BLOCK_COUNT) { Animatable(initial) } }

    LaunchedEffect(Unit) {
        if (played) {
            progress.forEach { it.snapTo(1f) }
        } else {
            coroutineScope {
                BLOCK_DELAYS_MS.forEachIndexed { index, delay ->
                    launch {
                        progress[index].animateTo(
                            targetValue = 1f,
                            animationSpec = tween(ANIM_DURATION_MS, delayMillis = delay, easing = FastOutSlowInEasing),
                        )
                    }
                }
            }
            played = true
        }
    }

    val actionsVisible by remember { derivedStateOf { progress[ACTIONS_INDEX].value >= 1f } }
    val expertName = details.expertName ?: stringResource(R.string.request_sent_label_expert)
    val locale = LocalConfiguration.current.locales[0]

    // The scroll area stays full width; only the content column is capped on a large window (D-02).
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState()),
    ) {
        Column(
            modifier =
                Modifier
                    .contentMaxWidth()
                    .testTag(REQUEST_SENT_CONTENT_TAG)
                    .padding(start = Spacing.md, end = Spacing.md, top = Spacing.lg, bottom = Spacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            BookingRequestSentCheckIcon(
                modifier =
                    Modifier.graphicsLayer {
                        val p = progress[ICON_INDEX].value
                        scaleX = ICON_START_SCALE + (1f - ICON_START_SCALE) * p
                        scaleY = scaleX
                        alpha = p
                    },
            )
            BookingRequestSentHeadline(
                expertName = expertName,
                modifier = Modifier.fadeSlideUp { progress[HEADLINE_INDEX].value },
            )
            BookingRequestSentDetailsCard(
                details = details,
                modifier = Modifier.fadeSlideUp { progress[CARD_INDEX].value },
                zone = zone,
                timeFormat = timeFormat,
            )
            // Paid bookings only (iOS `!viewModel.isFree`). Enters with the details card, not as a fifth stage.
            if (!details.isFree) {
                PaymentLifecycleCard(
                    stage = PaymentLifecycleStage.HoldPlaced,
                    sessionWhen = formatSessionWhen(details.start, zone, locale, timeFormat),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .testTag(REQUEST_SENT_PAYMENT_CARD_TAG)
                            .fadeSlideUp { progress[CARD_INDEX].value },
                )
            }
            BookingRequestSentActions(
                onViewBookings = onViewBookings,
                onBackToHome = onBackToHome,
                enabled = actionsVisible,
                modifier = Modifier.fadeSlideUp { progress[ACTIONS_INDEX].value },
            )
        }
    }
}

private const val ICON_INDEX = 0
private const val HEADLINE_INDEX = 1
private const val CARD_INDEX = 2
private const val ACTIONS_INDEX = 3
private const val BLOCK_COUNT = 4
private val BLOCK_DELAYS_MS = listOf(ICON_DELAY_MS, HEADLINE_DELAY_MS, CARD_DELAY_MS, ACTIONS_DELAY_MS)

/** Alpha 0 to 1 and translate up from [SlideDistance]; the block keeps its layout space (no shift). */
private fun Modifier.fadeSlideUp(progress: () -> Float): Modifier =
    graphicsLayer {
        val p = progress()
        alpha = p
        translationY = (1f - p) * SlideDistance.toPx()
    }

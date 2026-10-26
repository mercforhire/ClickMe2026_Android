package com.uptrendinvestments.clickmeapp.screens.expertmode.confirmed

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.rememberDeviceTimeFormat
import com.uptrendinvestments.clickmeapp.screens.clientmode.requestsent.subviews.BookingRequestSentCheckIcon
import com.uptrendinvestments.clickmeapp.screens.expertmode.clientDisplayName
import com.uptrendinvestments.clickmeapp.screens.expertmode.confirmed.subviews.ConfirmedActions
import com.uptrendinvestments.clickmeapp.screens.expertmode.confirmed.subviews.ConfirmedDetailsCard
import com.uptrendinvestments.clickmeapp.screens.expertmode.confirmed.subviews.ConfirmedHeadline
import com.uptrendinvestments.clickmeapp.screens.expertmode.meetingTypeLabel
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

private const val ANIM_DURATION_MS = 300
private const val ICON_DELAY_MS = 0
private const val HEADLINE_DELAY_MS = 150
private const val CARD_DELAY_MS = 250
private const val ACTIONS_DELAY_MS = 400
private const val ICON_START_SCALE = 0.6f
private val SlideDistance = 16.dp

private const val ICON_INDEX = 0
private const val HEADLINE_INDEX = 1
private const val CARD_INDEX = 2
private const val ACTIONS_INDEX = 3
private const val BLOCK_COUNT = 4
private val BLOCK_DELAYS_MS = listOf(ICON_DELAY_MS, HEADLINE_DELAY_MS, CARD_DELAY_MS, ACTIONS_DELAY_MS)

/**
 * Booking Confirmed (EXP-06, EXP-07, D-04). iOS never shows its `BookingConfirmedView`; Android shows it after an
 * accept, replacing Request Decision, so system Back pops to Incoming Requests. It shows only what [key] carries and
 * makes no network request (Pitfall 8).
 *
 * "Add to Calendar" hands a calendar app an `ACTION_INSERT` intent (no permission, D-06) and shows a "No calendar app
 * found" snackbar when nothing can handle it.
 */
@Composable
fun BookingConfirmedScreen(
    key: BookingConfirmedKey,
    onBack: () -> Unit,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val topicFallback = stringResource(R.string.booking_confirmed_topic_fallback)
    val topic = key.topic?.trim()?.takeIf { it.isNotEmpty() } ?: topicFallback
    val title = stringResource(R.string.booking_confirmed_calendar_title, topic, clientDisplayName(key.clientName))
    val label = meetingTypeLabel(key.meetingType)
    val noCalendar = stringResource(R.string.booking_confirmed_no_calendar)

    // Taps are ignored while the calendar app is in front; the flag clears when this screen resumes.
    var launching by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        launching = false
        onPauseOrDispose {}
    }

    BookingConfirmedContent(
        key = key,
        onBack = onBack,
        onAddToCalendar = {
            if (!launching) {
                launching = true
                val intent = buildCalendarInsertIntent(calendarEventSpec(key, title, label))
                context.launchCalendarInsert(intent) {
                    launching = false
                    scope.launch { snackbarHostState.showSnackbar(noCalendar) }
                }
            }
        },
        onBackToHome = onBackToHome,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}

/**
 * Stateless Confirmed layout: check badge, headline, details card and the two actions, with the Phase 6 entry
 * animation (D-04): the blocks fade up in turn, once (a saved flag stops a rotation replaying it), and the buttons
 * enable when the last block lands. With animations off the final state shows at once.
 */
@Composable
fun BookingConfirmedContent(
    key: BookingConfirmedKey,
    onBack: () -> Unit,
    onAddToCalendar: () -> Unit,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault(),
    timeFormat: java.text.DateFormat = rememberDeviceTimeFormat(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    // Saved so rotation or returning to the screen does not replay the entry. Set when the animation ends.
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

    val actionsEnabled by remember { derivedStateOf { progress[ACTIONS_INDEX].value >= 1f } }
    val locale = LocalConfiguration.current.locales[0]

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = { AuthTopAppBar(title = stringResource(R.string.booking_confirmed_title), onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(start = Spacing.md, end = Spacing.md, top = Spacing.lg, bottom = Spacing.xl)
                    .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            // Reused as is from Phase 6 (C13); decorative, the headline announces the result.
            BookingRequestSentCheckIcon(
                modifier =
                    Modifier.graphicsLayer {
                        val p = progress[ICON_INDEX].value
                        scaleX = ICON_START_SCALE + (1f - ICON_START_SCALE) * p
                        scaleY = scaleX
                        alpha = p
                    },
            )
            ConfirmedHeadline(
                clientName = key.clientName,
                start = Instant.ofEpochMilli(key.startMillis),
                modifier = Modifier.fadeSlideUp { progress[HEADLINE_INDEX].value },
                zone = zone,
                timeFormat = timeFormat,
                locale = locale,
            )
            ConfirmedDetailsCard(
                key = key,
                modifier = Modifier.fadeSlideUp { progress[CARD_INDEX].value },
                zone = zone,
                timeFormat = timeFormat,
                locale = locale,
            )
            ConfirmedActions(
                enabled = actionsEnabled,
                onAddToCalendar = onAddToCalendar,
                onBackToHome = onBackToHome,
                modifier = Modifier.fadeSlideUp { progress[ACTIONS_INDEX].value },
            )
        }
    }
}

/** Alpha 0 to 1 and translate up from [SlideDistance]; the block keeps its layout space (no shift). */
private fun Modifier.fadeSlideUp(progress: () -> Float): Modifier =
    graphicsLayer {
        val p = progress()
        alpha = p
        translationY = (1f - p) * SlideDistance.toPx()
    }

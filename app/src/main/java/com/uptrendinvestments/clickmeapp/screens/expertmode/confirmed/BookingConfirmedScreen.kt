package com.uptrendinvestments.clickmeapp.screens.expertmode.confirmed

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButton
import com.uptrendinvestments.clickmeapp.screens.expertmode.clientDisplayName
import com.uptrendinvestments.clickmeapp.screens.expertmode.meetingTypeLabel
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import kotlinx.coroutines.launch

/** Test tag on the Add to Calendar button. */
internal const val CONFIRMED_ADD_CALENDAR_TAG = "confirmed_add_calendar"

/**
 * Booking Confirmed (D-04): iOS never shows its `BookingConfirmedView`, Android shows it after an accept. First cut:
 * Add to Calendar and the snackbar; the full layout follows in the next task.
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

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = { AuthTopAppBar(title = stringResource(R.string.booking_confirmed_title), onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(Spacing.md)) {
            PressScaleButton(
                onClick = {
                    val intent = buildCalendarInsertIntent(calendarEventSpec(key, title, label))
                    context.launchCalendarInsert(intent) {
                        scope.launch { snackbarHostState.showSnackbar(noCalendar) }
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag(CONFIRMED_ADD_CALENDAR_TAG),
            ) { Text(stringResource(R.string.booking_confirmed_add_to_calendar)) }
            PressScaleButton(onClick = onBackToHome, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.booking_confirmed_back_home))
            }
        }
    }
}

package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.EmptyState
import com.uptrendinvestments.clickmeapp.components.avatar.Avatar
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras

/**
 * Stub body under [BookingKey] (D-04): shows the expert and a "coming soon" message so Book Session has a real
 * destination. Phase 6 ports iOS `Make a Booking/MakeABookingView.swift` here, replacing this body and keeping
 * [BookingKey]. The bottom bar is hidden by the route metadata, not by this screen.
 */
@Composable
fun MakeABookingStubScreen(
    key: BookingKey,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            AuthTopAppBar(
                title = stringResource(R.string.make_a_booking_title),
                onBack = onBack,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(top = Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Avatar(url = key.expertImageUrl, contentDescription = null, size = 96.dp)
            Spacer(Modifier.height(Spacing.md))
            Text(
                text = key.expertName,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            if (key.expertTitle.isNotBlank()) {
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = key.expertTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.extras.onSurfaceMuted,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(Spacing.xl))
            EmptyState(
                icon = Icons.Outlined.CalendarMonth,
                heading = stringResource(R.string.make_a_booking_coming_soon_heading),
                body = stringResource(R.string.make_a_booking_coming_soon_body, key.expertName),
            )
        }
    }
}

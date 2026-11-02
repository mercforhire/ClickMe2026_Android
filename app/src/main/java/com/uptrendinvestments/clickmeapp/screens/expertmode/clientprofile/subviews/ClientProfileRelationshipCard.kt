package com.uptrendinvestments.clickmeapp.screens.expertmode.clientprofile.subviews

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.responses.ClientRelationshipDto
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * "Your Sessions Together" (iOS `ClientProfileRelationshipCard`): the session count with Session or Sessions, Client
 * since and Last session as three equal-height cells. Values and labels wrap instead of scaling down (the iOS
 * 0.7 scale-down is dropped), and each cell is one merged TalkBack node reading value then label.
 *
 * The counts come from the server's aggregate, so they can exceed the history rows below (D-11 accepted). A null date
 * shows a dash.
 */
@Composable
fun ClientProfileRelationshipCard(
    relationship: ClientRelationshipDto,
    zone: ZoneId,
    locale: Locale,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        ClientProfileSectionHeading(stringResource(R.string.client_profile_sessions_title))
        Spacer(Modifier.height(Spacing.md))
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            StatCell(
                value = relationship.totalSessions.toString(),
                label =
                    pluralStringResource(R.plurals.client_profile_sessions, relationship.totalSessions),
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            StatCell(
                value = monthText(relationship.firstSessionDate, zone, locale),
                label = stringResource(R.string.client_profile_client_since),
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            StatCell(
                value = monthText(relationship.lastSessionDate, zone, locale),
                label = stringResource(R.string.client_profile_last_session),
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
    }
}

/** "Feb 2026" for [date] in [zone], following [locale], or the dash when [date] is null. */
@Composable
private fun monthText(
    date: Instant?,
    zone: ZoneId,
    locale: Locale,
): String =
    if (date == null) {
        stringResource(R.string.client_profile_no_date)
    } else {
        DateTimeFormatter
            .ofPattern(DateFormat.getBestDateTimePattern(locale, "MMMyyyy"), locale)
            .format(date.atZone(zone))
    }

@Composable
private fun StatCell(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    ClientProfileCard(modifier.semantics(mergeDescendants = true) { }) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.md, horizontal = Spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(name = "ClientProfileRelationshipCard")
@Composable
private fun ClientProfileRelationshipCardPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ClientProfileRelationshipCard(
                relationship = ClientRelationshipDto(totalSessions = 3),
                zone = ZoneId.systemDefault(),
                locale = Locale.getDefault(),
                modifier = Modifier.padding(Spacing.md),
            )
        }
    }
}

package com.uptrendinvestments.clickmeapp.screens.expertmode.clientprofile.subviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.responses.ExpertClientProfileData
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.formatMonthYear
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale

private val RowIconSize = 24.dp
private val BioIconSize = 16.dp

/**
 * The About section (iOS `ClientProfilePersonalInfo`): a bio card, then Location, Timezone, Languages and Member
 * since, each hidden when empty. The heading stays even when every row is empty (UI-SPEC U3, as on iOS). Each row is
 * one merged TalkBack node reading label then value (UI-SPEC C7).
 *
 * Read-only. Every value is server text from the client, rendered as plain [Text] with no autolink or markdown
 * (T-16.4-23). The country is the raw code the server sends, "CAN" rather than "Canada" (flagged assumption A8).
 */
@Composable
fun ClientProfilePersonalInfo(
    profile: ExpertClientProfileData,
    zone: ZoneId,
    locale: Locale,
    modifier: Modifier = Modifier,
) {
    val bio = profile.personalDetails.bio?.takeIf { it.isNotBlank() }
    val location =
        with(profile.location) { listOfNotNull(city, stateProvince, country).filter { it.isNotBlank() } }
            .joinToString(", ")
            .takeIf { it.isNotEmpty() }
    val timezone = profile.location.timezone?.takeIf { it.isNotBlank() }
    val languages =
        profile.languages
            .filter { it.isNotBlank() }
            .joinToString(", ")
            .takeIf { it.isNotEmpty() }
    val memberSince =
        profile.memberSince?.let { formatMonthYear(YearMonth.from(it.atZone(zone)), locale) }
    Column(modifier.fillMaxWidth()) {
        ClientProfileSectionHeading(stringResource(R.string.client_profile_about))
        Spacer(Modifier.height(Spacing.md))
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            if (bio != null) BioCard(bio)
            if (location != null) {
                InfoRow(Icons.Outlined.LocationOn, stringResource(R.string.client_profile_location), location)
            }
            if (timezone != null) {
                InfoRow(Icons.Outlined.Schedule, stringResource(R.string.client_profile_timezone), timezone)
            }
            if (languages != null) {
                InfoRow(Icons.Outlined.Translate, stringResource(R.string.client_profile_languages), languages)
            }
            if (memberSince != null) {
                InfoRow(
                    Icons.Outlined.EventAvailable,
                    stringResource(R.string.client_profile_member_since),
                    memberSince,
                )
            }
        }
    }
}

/** A `titleLarge` section heading that TalkBack announces as a heading. */
@Composable
internal fun ClientProfileSectionHeading(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier.semantics { heading() },
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

/** The shared Client Profile card: `surfaceContainerLow`, a 1 dp border and the medium shape. */
@Composable
internal fun ClientProfileCard(
    modifier: Modifier = Modifier,
    border: BorderStroke = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
        border = border,
        content = content,
    )
}

@Composable
private fun BioCard(bio: String) {
    ClientProfileCard(Modifier.fillMaxWidth().semantics(mergeDescendants = true) { }) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Icon(
                    imageVector = Icons.Outlined.FormatQuote,
                    contentDescription = null,
                    modifier = Modifier.size(BioIconSize),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.client_profile_bio),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = bio,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
) {
    ClientProfileCard(Modifier.fillMaxWidth().semantics(mergeDescendants = true) { }) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(RowIconSize),
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Preview(name = "ClientProfilePersonalInfo")
@Composable
private fun ClientProfilePersonalInfoPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ClientProfilePersonalInfo(
                profile =
                    ExpertClientProfileData(
                        id = "c1",
                        languages = listOf("English", "Spanish"),
                    ),
                zone = ZoneId.systemDefault(),
                locale = Locale.US,
                modifier = Modifier.padding(Spacing.md),
            )
        }
    }
}

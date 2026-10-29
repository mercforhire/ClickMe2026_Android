package com.uptrendinvestments.clickmeapp.screens.clientmode.profile.subviews

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.BuildConfig
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.avatar.Avatar
import com.uptrendinvestments.clickmeapp.components.buttonstyles.DestructiveOutlinedButton
import com.uptrendinvestments.clickmeapp.components.web.rememberCustomTabLauncher
import com.uptrendinvestments.clickmeapp.config.AppLinks
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras
import kotlinx.coroutines.launch

const val PROFILE_HUB_USER_CARD_TAG = "profile_hub_user_card"
const val PROFILE_HUB_MODE_SWITCH_TAG = "profile_hub_mode_switch"
const val PROFILE_HUB_LOGOUT_TAG = "profile_hub_logout"

private val AvatarSize = 56.dp
private val TargetSize = 48.dp
private val IconSize = 24.dp
private val ModeCardMinHeight = 72.dp
private val LogOutMinHeight = 56.dp

/** "Version 0.1.0 (1)" from the build; the shells pass it to the hub content so tests can pass a fixed label. */
@Composable
fun profileHubVersionLabel(): String =
    stringResource(R.string.profile_hub_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)

/**
 * Shared hub frame: a top-level `TopAppBar` titled [title] (no back, no actions), a snackbar host, and one scrolling
 * column (`md` sides, `lg` top, `lg` between blocks, `xl` at the bottom).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileHubScaffold(
    title: String,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.semantics { heading() },
                    )
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(start = Spacing.md, top = Spacing.lg, end = Spacing.md, bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            content = content,
        )
    }
}

/** Opens the privacy policy in a Custom Tab; shows the link-failed snackbar when nothing can open it (D-02). */
@Composable
fun rememberPrivacyLinkOpener(snackbarHostState: SnackbarHostState): () -> Unit {
    val scope = rememberCoroutineScope()
    val failed = stringResource(R.string.profile_hub_link_failed)
    val launch =
        rememberCustomTabLauncher {
            scope.launch { snackbarHostState.showSnackbar(failed) }
        }
    return { launch(Uri.parse(AppLinks.PRIVACY_POLICY)) }
}

/**
 * The signed-in user's card: avatar, name and email, and a decorative edit glyph. The whole card is one button that
 * opens settings; its merged spoken label is "{name}, {email}, Edit profile". Name and email wrap with no line cap.
 */
@Composable
fun ProfileHubUserCard(
    name: String?,
    email: String?,
    avatarUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shownName = name?.takeIf { it.isNotBlank() } ?: stringResource(R.string.profile_hub_your_profile)
    val shownEmail = email?.takeIf { it.isNotBlank() }
    val editLabel = stringResource(R.string.profile_hub_edit_profile)
    val spoken =
        if (shownEmail != null) {
            stringResource(R.string.profile_hub_user_card_a11y, shownName, shownEmail, editLabel)
        } else {
            stringResource(R.string.profile_hub_user_card_a11y_no_email, shownName, editLabel)
        }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag(PROFILE_HUB_USER_CARD_TAG)
                    .clickable(role = Role.Button, onClick = onClick)
                    .semantics(mergeDescendants = true) { contentDescription = spoken }
                    .padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(url = avatarUrl, contentDescription = null, size = AvatarSize)
            Column(Modifier.weight(1f)) {
                Text(
                    text = shownName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (shownEmail != null) {
                    Text(
                        text = shownEmail,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Box(Modifier.size(TargetSize), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(IconSize),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** The "Switch to Expert / Client Mode" card: the one accent mark (12% tile) above the neutral rows. */
@Composable
fun ProfileHubModeSwitchCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = ModeCardMinHeight)
                    .testTag(PROFILE_HUB_MODE_SWITCH_TAG)
                    .clickable(role = Role.Button, onClick = onClick)
                    .semantics(mergeDescendants = true) { }
                    .padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(TargetSize)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.SwapHoriz,
                    contentDescription = null,
                    modifier = Modifier.size(IconSize),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(IconSize),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** A section caption ("Account", "Support", "Professional"). */
@Composable
fun ProfileHubSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth().semantics { heading() },
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Centered version and copyright lines under the Log out button. */
@Composable
fun ProfileHubVersionFooter(
    versionLabel: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = versionLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.extras.onSurfaceMuted,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.profile_hub_copyright),
            modifier = Modifier.padding(top = Spacing.xs),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.extras.onSurfaceMuted,
            textAlign = TextAlign.Center,
        )
    }
}

/** Full-width error-outlined "Log out" pill; the caller opens [LogOutConfirmDialog] from [onClick]. */
@Composable
fun ProfileHubLogOutButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DestructiveOutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = LogOutMinHeight).testTag(PROFILE_HUB_LOGOUT_TAG),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.Logout,
            contentDescription = null,
            modifier = Modifier.size(IconSize),
        )
        Spacer(Modifier.size(Spacing.sm))
        Text(stringResource(R.string.profile_hub_log_out))
    }
}

/** "Log out of ClickMe?" confirmation (the iOS `.alert`). Only [onConfirm] ends the session. */
@Composable
fun LogOutConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.extraLarge,
        title = {
            Text(
                text = stringResource(R.string.profile_hub_log_out_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.profile_hub_log_out_dismiss),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.profile_hub_log_out_confirm),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
    )
}

@Preview(name = "ProfileHubUserCard")
@Composable
private fun ProfileHubUserCardPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                ProfileHubUserCard("Ana Lee", "ana@example.com", null, onClick = {})
                ProfileHubModeSwitchCard("Switch to Expert Mode", "Start offering consultations", onClick = {})
            }
        }
    }
}

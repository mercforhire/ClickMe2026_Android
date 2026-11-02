package com.uptrendinvestments.clickmeapp.screens.expertmode.profile

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.ProfileMenuRow
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.ExpertProfileKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.profile.ProfileHubCard
import com.uptrendinvestments.clickmeapp.screens.clientmode.profile.ProfileHubSection
import com.uptrendinvestments.clickmeapp.screens.clientmode.profile.subviews.LogOutConfirmDialog
import com.uptrendinvestments.clickmeapp.screens.clientmode.profile.subviews.ProfileHubLogOutButton
import com.uptrendinvestments.clickmeapp.screens.clientmode.profile.subviews.ProfileHubModeSwitchCard
import com.uptrendinvestments.clickmeapp.screens.clientmode.profile.subviews.ProfileHubScaffold
import com.uptrendinvestments.clickmeapp.screens.clientmode.profile.subviews.ProfileHubUserCard
import com.uptrendinvestments.clickmeapp.screens.clientmode.profile.subviews.ProfileHubVersionFooter
import com.uptrendinvestments.clickmeapp.screens.clientmode.profile.subviews.rememberPrivacyLinkOpener
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

/** Test tag of the Availability row, kept from the throwaway Profile footer so existing shell tests still find it. */
const val PROFILE_AVAILABILITY_ROW_TAG = "profile_availability_row"

/**
 * What the expert hub can do (PROF-01, D-02, D-08): the Professional rows, Security, Notifications (Preferences),
 * FAQ & Help and Send Feedback. There is no Payment History row; that ledger is client-only. The shell maps each
 * callback to a destination; "Expertise & Topics" selects the Topics tab. [onLogOut] runs only after the user
 * confirms the dialog.
 */
data class ExpertProfileHubActions(
    val onEditProfile: () -> Unit,
    val onSwitchToClient: () -> Unit,
    val onOpenExpertise: () -> Unit,
    val onOpenAvailability: () -> Unit,
    val onOpenPayouts: () -> Unit,
    val onOpenTaxProfile: () -> Unit,
    val onOpenSecurity: () -> Unit,
    val onOpenNotifications: () -> Unit,
    val onOpenHelp: () -> Unit,
    val onOpenFeedback: () -> Unit,
    val onLogOut: () -> Unit,
)

/**
 * Expert Profile tab root. "View My Public Profile" hands the self-view key to [onOpenPublicProfile]; nothing opens
 * when no user id is known yet. [debugContent] renders below the version footer.
 */
@Composable
fun ExpertProfileHomeScreen(
    actions: ExpertProfileHubActions,
    onOpenPublicProfile: (ExpertProfileKey) -> Unit,
    versionLabel: String,
    modifier: Modifier = Modifier,
    debugContent: @Composable () -> Unit = {},
    viewModel: ExpertProfileHomeViewModel = hiltViewModel(),
) {
    val card by viewModel.card.collectAsStateWithLifecycle()
    ExpertProfileHomeContent(
        card = card,
        versionLabel = versionLabel,
        actions = actions,
        onViewPublicProfile = { viewModel.publicProfileKey()?.let(onOpenPublicProfile) },
        modifier = modifier,
        debugContent = debugContent,
    )
}

/** Stateless expert hub body. */
@Composable
fun ExpertProfileHomeContent(
    card: ProfileHubCard,
    versionLabel: String,
    actions: ExpertProfileHubActions,
    onViewPublicProfile: () -> Unit,
    modifier: Modifier = Modifier,
    debugContent: @Composable () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val openPrivacy = rememberPrivacyLinkOpener(snackbarHostState)
    var confirmingLogOut by rememberSaveable { mutableStateOf(false) }

    ProfileHubScaffold(
        title = stringResource(R.string.profile_hub_title_expert),
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    ) {
        ProfileHubUserCard(
            name = card.name,
            email = card.email,
            avatarUrl = card.avatarUrl,
            onClick = actions.onEditProfile,
        )
        ProfileHubModeSwitchCard(
            title = stringResource(R.string.profile_hub_switch_to_client),
            subtitle = stringResource(R.string.profile_hub_switch_to_client_subtitle),
            onClick = actions.onSwitchToClient,
        )
        ProfileHubSection(stringResource(R.string.profile_hub_section_professional)) {
            ProfileMenuRow(
                title = stringResource(R.string.profile_hub_row_public_profile),
                icon = Icons.Outlined.Badge,
                onClick = onViewPublicProfile,
            )
            ProfileMenuRow(
                title = stringResource(R.string.profile_hub_row_expertise),
                icon = Icons.Outlined.Psychology,
                onClick = actions.onOpenExpertise,
            )
            ProfileMenuRow(
                title = stringResource(R.string.profile_hub_row_availability),
                icon = Icons.Outlined.CalendarMonth,
                onClick = actions.onOpenAvailability,
                modifier = Modifier.testTag(PROFILE_AVAILABILITY_ROW_TAG),
            )
            ProfileMenuRow(
                title = stringResource(R.string.profile_hub_row_payouts),
                icon = Icons.Outlined.Payments,
                onClick = actions.onOpenPayouts,
            )
            ProfileMenuRow(
                title = stringResource(R.string.profile_hub_row_tax),
                icon = Icons.Outlined.Description,
                onClick = actions.onOpenTaxProfile,
            )
        }
        ProfileHubSection(stringResource(R.string.profile_hub_section_account)) {
            ProfileMenuRow(
                title = stringResource(R.string.profile_hub_row_security),
                icon = Icons.Outlined.Lock,
                onClick = actions.onOpenSecurity,
            )
        }
        ProfileHubSection(stringResource(R.string.profile_hub_section_preferences)) {
            ProfileMenuRow(
                title = stringResource(R.string.profile_hub_row_notifications),
                icon = Icons.Outlined.Notifications,
                onClick = actions.onOpenNotifications,
            )
        }
        ProfileHubSection(stringResource(R.string.profile_hub_section_support)) {
            ProfileMenuRow(
                title = stringResource(R.string.profile_hub_row_help),
                icon = Icons.AutoMirrored.Outlined.HelpOutline,
                onClick = actions.onOpenHelp,
            )
            ProfileMenuRow(
                title = stringResource(R.string.profile_hub_row_feedback),
                icon = Icons.Outlined.Feedback,
                onClick = actions.onOpenFeedback,
            )
            ProfileMenuRow(
                title = stringResource(R.string.profile_hub_row_privacy),
                icon = Icons.Outlined.PrivacyTip,
                onClick = openPrivacy,
            )
        }
        ProfileHubLogOutButton(onClick = { confirmingLogOut = true })
        ProfileHubVersionFooter(versionLabel = versionLabel, modifier = Modifier.padding(top = Spacing.sm))
        debugContent()
    }

    if (confirmingLogOut) {
        LogOutConfirmDialog(
            onConfirm = {
                confirmingLogOut = false
                actions.onLogOut()
            },
            onDismiss = { confirmingLogOut = false },
        )
    }
}

@Preview(name = "ExpertProfileHomeContent")
@Composable
private fun ExpertProfileHomeContentPreview() {
    ClickMeTheme {
        ExpertProfileHomeContent(
            card = ProfileHubCard("Ana Lee", "ana@example.com", null),
            versionLabel = "Version 0.1.0 (1)",
            actions =
                ExpertProfileHubActions(
                    onEditProfile = {},
                    onSwitchToClient = {},
                    onOpenExpertise = {},
                    onOpenAvailability = {},
                    onOpenPayouts = {},
                    onOpenTaxProfile = {},
                    onOpenSecurity = {},
                    onOpenNotifications = {},
                    onOpenHelp = {},
                    onOpenFeedback = {},
                    onLogOut = {},
                ),
            onViewPublicProfile = {},
        )
    }
}

package com.uptrendinvestments.clickmeapp.screens.clientmode.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.ProfileMenuRow
import com.uptrendinvestments.clickmeapp.screens.clientmode.profile.subviews.LogOutConfirmDialog
import com.uptrendinvestments.clickmeapp.screens.clientmode.profile.subviews.ProfileHubLogOutButton
import com.uptrendinvestments.clickmeapp.screens.clientmode.profile.subviews.ProfileHubModeSwitchCard
import com.uptrendinvestments.clickmeapp.screens.clientmode.profile.subviews.ProfileHubScaffold
import com.uptrendinvestments.clickmeapp.screens.clientmode.profile.subviews.ProfileHubSectionHeader
import com.uptrendinvestments.clickmeapp.screens.clientmode.profile.subviews.ProfileHubUserCard
import com.uptrendinvestments.clickmeapp.screens.clientmode.profile.subviews.ProfileHubVersionFooter
import com.uptrendinvestments.clickmeapp.screens.clientmode.profile.subviews.rememberPrivacyLinkOpener
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

/**
 * What the client hub can do. The hub holds no navigation keys: the shell maps each callback to a destination
 * (Phase 14 wiring), and [onLogOut] runs only after the user confirms the dialog.
 */
data class ClientProfileHubActions(
    val onEditProfile: () -> Unit,
    val onSwitchToExpert: () -> Unit,
    val onOpenSavedExperts: () -> Unit,
    val onOpenSecurity: () -> Unit,
    val onOpenPayment: () -> Unit,
    val onOpenPaymentHistory: () -> Unit,
    val onOpenHelp: () -> Unit,
    val onLogOut: () -> Unit,
)

/**
 * Client Profile tab root (PROF-01, D-01..D-04): the user card, the mode-switch card, the Discovery row (Saved
 * Experts), Account and Support rows, Log out and the version footer. Only rows that work are shown. [debugContent] renders below the footer for the debug
 * affordances the shell still owns.
 */
@Composable
fun ClientProfileHomeScreen(
    actions: ClientProfileHubActions,
    versionLabel: String,
    modifier: Modifier = Modifier,
    debugContent: @Composable () -> Unit = {},
    viewModel: ClientProfileHomeViewModel = hiltViewModel(),
) {
    val card by viewModel.card.collectAsStateWithLifecycle()
    ClientProfileHomeContent(
        card = card,
        versionLabel = versionLabel,
        actions = actions,
        modifier = modifier,
        debugContent = debugContent,
    )
}

/** Stateless client hub body. */
@Composable
fun ClientProfileHomeContent(
    card: ProfileHubCard,
    versionLabel: String,
    actions: ClientProfileHubActions,
    modifier: Modifier = Modifier,
    debugContent: @Composable () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val openPrivacy = rememberPrivacyLinkOpener(snackbarHostState)
    var confirmingLogOut by rememberSaveable { mutableStateOf(false) }

    ProfileHubScaffold(
        title = stringResource(R.string.profile_hub_title_client),
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
            title = stringResource(R.string.profile_hub_switch_to_expert),
            subtitle = stringResource(R.string.profile_hub_switch_to_expert_subtitle),
            onClick = actions.onSwitchToExpert,
        )
        ProfileHubSection(stringResource(R.string.profile_hub_section_discovery)) {
            ProfileMenuRow(
                title = stringResource(R.string.profile_hub_row_saved_experts),
                icon = Icons.Outlined.FavoriteBorder,
                onClick = actions.onOpenSavedExperts,
            )
        }
        ProfileHubSection(stringResource(R.string.profile_hub_section_account)) {
            ProfileMenuRow(
                title = stringResource(R.string.profile_hub_row_security),
                icon = Icons.Outlined.Lock,
                onClick = actions.onOpenSecurity,
            )
            ProfileMenuRow(
                title = stringResource(R.string.profile_hub_row_payment),
                icon = Icons.Outlined.CreditCard,
                onClick = actions.onOpenPayment,
            )
            ProfileMenuRow(
                title = stringResource(R.string.profile_hub_row_payment_history),
                icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                onClick = actions.onOpenPaymentHistory,
            )
        }
        ProfileHubSection(stringResource(R.string.profile_hub_section_support)) {
            ProfileMenuRow(
                title = stringResource(R.string.profile_hub_row_help),
                icon = Icons.AutoMirrored.Outlined.HelpOutline,
                onClick = actions.onOpenHelp,
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

/** A captioned group of rows: `md` under the caption, `sm` between rows. Shared by both hubs. */
@Composable
internal fun ProfileHubSection(
    title: String,
    rows: @Composable ColumnScope.() -> Unit,
) {
    Column {
        ProfileHubSectionHeader(text = title)
        Spacer(Modifier.height(Spacing.md))
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm), content = rows)
    }
}

@Preview(name = "ClientProfileHomeContent")
@Composable
private fun ClientProfileHomeContentPreview() {
    ClickMeTheme {
        ClientProfileHomeContent(
            card = ProfileHubCard("Ana Lee", "ana@example.com", null),
            versionLabel = "Version 0.1.0 (1)",
            actions =
                ClientProfileHubActions(
                    onEditProfile = {},
                    onSwitchToExpert = {},
                    onOpenSavedExperts = {},
                    onOpenSecurity = {},
                    onOpenPayment = {},
                    onOpenPaymentHistory = {},
                    onOpenHelp = {},
                    onLogOut = {},
                ),
        )
    }
}

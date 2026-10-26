package com.uptrendinvestments.clickmeapp.screens.expertmode.dashboard.subviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.screens.expertmode.dashboard.PayoutOnboarding
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

/** Test tag on the payout onboarding card. */
const val DASHBOARD_PAYOUT_ONBOARDING_TAG = "dashboard_payout_onboarding"

private val TileSize = 48.dp
private val GlyphSize = 24.dp
private const val TILE_ALPHA = 0.12f
private const val CARD_FILL_ALPHA = 0.10f
private const val CARD_BORDER_ALPHA = 0.45f

/**
 * The Financial Summary block (UI-SPEC surface 1, block 5): the "Financial Summary" heading, then the payout
 * onboarding card while Stripe Connect setup is [PayoutOnboarding.Required]. With no card to show the block is just the
 * heading, so the Dashboard layout stays stable.
 */
@Composable
fun FinancialSummarySection(
    onboarding: PayoutOnboarding,
    isMinting: Boolean,
    onStartPayoutSetup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.dashboard_financial_summary),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        if (onboarding is PayoutOnboarding.Required) {
            Spacer(Modifier.height(Spacing.md))
            PayoutOnboardingCard(isBusy = isMinting, onClick = onStartPayoutSetup)
        }
    }
}

/**
 * "Complete payout setup" card (D-09): one button that mints a Stripe link and opens it in a Custom Tab. While the
 * link is minting it reads "Opening secure setup…" with a spinner, and repeat taps are ignored by the ViewModel.
 */
@Composable
fun PayoutOnboardingCard(
    isBusy: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tertiary = MaterialTheme.colorScheme.tertiary
    val busyDescription = stringResource(R.string.dashboard_payout_setup_busy_a11y)
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .testTag(DASHBOARD_PAYOUT_ONBOARDING_TAG)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { if (isBusy) stateDescription = busyDescription },
        shape = MaterialTheme.shapes.medium,
        color = tertiary.copy(alpha = CARD_FILL_ALPHA),
        border = BorderStroke(1.dp, tertiary.copy(alpha = CARD_BORDER_ALPHA)),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(TileSize)
                        .background(tertiary.copy(alpha = TILE_ALPHA), MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.CreditCard,
                    contentDescription = null,
                    modifier = Modifier.size(GlyphSize),
                    tint = tertiary,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text =
                        stringResource(
                            if (isBusy) R.string.dashboard_payout_setup_busy else R.string.dashboard_payout_setup_title,
                        ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.dashboard_payout_setup_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(GlyphSize),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(GlyphSize),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

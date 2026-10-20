package com.uptrendinvestments.clickmeapp.screens.signup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AssignmentInd
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.EmptyState
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

/**
 * Route for an expert whose profile setup is unfinished (D-05). Phase 4 builds the real signup flow;
 * until then this offers Log out so the user can switch accounts.
 */
@Composable
fun SignupSetupPlaceholder(
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(vertical = Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xl, Alignment.CenterVertically),
    ) {
        EmptyState(
            icon = Icons.Outlined.AssignmentInd,
            heading = stringResource(R.string.signup_setup_placeholder_title),
            body = stringResource(R.string.signup_setup_placeholder_body),
        )
        OutlinedButton(onClick = onLogout) {
            Text(stringResource(R.string.profile_log_out))
        }
    }
}

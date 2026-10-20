package com.uptrendinvestments.clickmeapp.screens.signup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.EmptyState
import com.uptrendinvestments.clickmeapp.components.EmptyStateAction
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme

/**
 * "Coming soon" destination of the Login "Sign Up" footer (D-05). Phase 4 (SIGN-01) replaces it with
 * `SignupInitialScreen`, so it stays small and uses only shared components and no ViewModel.
 * Both the top bar arrow and the "Back to Login" action call [onBack].
 */
@Composable
fun SignUpPlaceholderScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = { AuthTopAppBar(title = stringResource(R.string.signup_placeholder_title), onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            EmptyState(
                icon = Icons.Outlined.PersonAdd,
                heading = stringResource(R.string.signup_placeholder_heading),
                body = stringResource(R.string.signup_placeholder_body),
                action =
                    EmptyStateAction(
                        label = stringResource(R.string.signup_placeholder_back_to_login),
                        onClick = onBack,
                    ),
            )
        }
    }
}

@Preview(name = "SignUpPlaceholder")
@Composable
private fun SignUpPlaceholderScreenPreview() {
    ClickMeTheme { SignUpPlaceholderScreen(onBack = {}) }
}

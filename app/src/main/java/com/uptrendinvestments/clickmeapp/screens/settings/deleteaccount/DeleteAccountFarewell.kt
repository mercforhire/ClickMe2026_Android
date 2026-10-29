package com.uptrendinvestments.clickmeapp.screens.settings.deleteaccount

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButton
import com.uptrendinvestments.clickmeapp.managers.UserManager
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Test tag on the Farewell root. */
const val DELETE_FAREWELL_TAG = "delete_farewell"

/** Test tag on the Farewell's "Back to Login". */
const val DELETE_BACK_TO_LOGIN_TAG = "delete_back_to_login"

private val IconTileSize = 96.dp
private val IconSize = 48.dp
private val ButtonMinHeight = 56.dp
private const val FAREWELL_FADE_MS = 300

/**
 * The full-screen "We're sorry to see you go!" shown once the account is deleted. No app bar and no way out except
 * [onBackToLogin]. Makes no request of its own: every authenticated call after the DELETE would be rejected.
 */
@Composable
fun DeleteAccountFarewell(
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val headlineFocus = remember { FocusRequester() }
    // TalkBack starts on the headline (UI-SPEC Accessibility Contract).
    LaunchedEffect(Unit) { runCatching { headlineFocus.requestFocus() } }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .testTag(DELETE_FAREWELL_TAG),
    ) {
        Column(
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .statusBarsPadding()
                    .padding(horizontal = Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(IconTileSize)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(IconSize),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(Spacing.lg))
            Text(
                text = stringResource(R.string.delete_account_farewell_headline),
                modifier =
                    Modifier
                        .focusRequester(headlineFocus)
                        .focusable()
                        .semantics { heading() },
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = stringResource(R.string.delete_account_farewell_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        PressScaleButton(
            onClick = onBackToLogin,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(Spacing.md)
                    .fillMaxWidth()
                    .heightIn(min = ButtonMinHeight)
                    .testTag(DELETE_BACK_TO_LOGIN_TAG),
        ) {
            Text(stringResource(R.string.delete_account_back_to_login))
        }
    }
}

/**
 * Drives the Farewell. [visible] is the root flag, independent of the login state (RESEARCH Pitfall 3).
 * [backToLogin] logs out first when the session is still alive, so the logged-in shell never flashes, and only then
 * hides the Farewell; Login is what remains. A forced logout may already have happened, in which case it is skipped.
 */
@HiltViewModel
class AccountDeletionFarewellViewModel
    @Inject
    constructor(
        private val state: AccountDeletionState,
        private val userManager: UserManager,
    ) : ViewModel() {
        val visible: StateFlow<Boolean> = state.farewellVisible

        private var busy = false

        fun backToLogin() {
            if (busy) return
            busy = true
            viewModelScope.launch {
                try {
                    if (userManager.isLoggedIn.value) userManager.logout()
                    state.clear()
                } finally {
                    busy = false
                }
            }
        }
    }

/**
 * Renders the Farewell above everything else while [AccountDeletionFarewellViewModel.visible] is true. Placed last in
 * `ClickMeApp`'s root `Box`, so a route change to Login underneath it (a forced logout after the deletion) does not
 * remove it. System Back is swallowed; the only way out is "Back to Login".
 */
@Composable
fun AccountDeletionFarewellHost(
    modifier: Modifier = Modifier,
    viewModel: AccountDeletionFarewellViewModel = hiltViewModel(),
) {
    val visible by viewModel.visible.collectAsStateWithLifecycle()
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(FAREWELL_FADE_MS)),
        exit = ExitTransition.None,
    ) {
        BackHandler(enabled = true) {}
        DeleteAccountFarewell(onBackToLogin = viewModel::backToLogin)
    }
}

package com.uptrendinvestments.clickmeapp.screens.settings.modeswitching

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.enums.AppMode
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.screens.settings.modeswitching.subviews.ModeSwitchCard
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.contentMaxWidth
import kotlinx.serialization.Serializable

/** Nav key for the Mode Switcher page. */
@Serializable
data object ModeSwitchKey : NavKey

/**
 * Mode Switcher (iOS ModeSwitchView): two radio-group cards, Client and Expert, the [currentMode] one
 * marked Active. Tapping a card reports the choice and closes the page; tapping the active card only
 * closes it (D-18). The D-17 setup gate is applied by AppViewModel.switchMode, not here.
 *
 * Pushed from the top-bar avatar in plan 04-18 (D-20), which also hides the bottom bar for this page.
 */
@Composable
fun ModeSwitchScreen(
    currentMode: AppMode,
    onSwitchMode: (AppMode) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = { AuthTopAppBar(title = stringResource(R.string.mode_switch_title), onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .contentMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.lg)
                    .selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            AppMode.entries.forEach { mode ->
                ModeSwitchCard(
                    mode = mode,
                    isActive = mode == currentMode,
                    onClick = {
                        if (mode != currentMode) onSwitchMode(mode)
                        onBack()
                    },
                )
            }
        }
    }
}

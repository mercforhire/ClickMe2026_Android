package com.uptrendinvestments.clickmeapp.screens.login

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.uptrendinvestments.clickmeapp.R

/**
 * Top app bar with a back arrow for the pushed auth screens (CLAUDE.md swap: iOS back chevron).
 * [backEnabled] = false disables the arrow, e.g. while a submit that must not be abandoned is running.
 * A null [onBack] hides the navigation icon; [containerColor] may be transparent so a background glow
 * shows through; [actions] fills the trailing slot (e.g. a MoreVert overflow button).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthTopAppBar(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    backEnabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        modifier = modifier,
        title = { Text(text = title, style = MaterialTheme.typography.titleLarge) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack, enabled = backEnabled) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.a11y_back),
                    )
                }
            }
        },
        actions = actions,
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = containerColor,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                actionIconContentColor = MaterialTheme.colorScheme.onSurface,
            ),
    )
}

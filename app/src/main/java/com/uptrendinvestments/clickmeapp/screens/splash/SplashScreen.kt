package com.uptrendinvestments.clickmeapp.screens.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.contentMaxWidth
import com.uptrendinvestments.clickmeapp.ui.theme.extras

/**
 * The Compose splash stage shown after the OS splash (D-07), mirroring iOS `SplashScreenView`: a full-bleed radial
 * gradient and a centered mark, wordmark and tagline.
 *
 * This is the resting composition; plan 16.5-04 adds the entrance, glow and merged semantics to it. There is no
 * `Scaffold` and no `BackHandler`: system back is not intercepted during the splash.
 *
 * [onComposed] is reported once, after the first composition, and starts the minimum display time in
 * `AppViewModel.onSplashComposed`.
 */
@Composable
fun SplashScreen(
    onComposed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val latestOnComposed by rememberUpdatedState(onComposed)
    LaunchedEffect(Unit) { latestOnComposed() }

    val radiusPx = with(LocalDensity.current) { 520.dp.toPx() }
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors =
                            listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surfaceContainerLowest,
                            ),
                        radius = radiusPx,
                    ),
                ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.contentMaxWidth().offset(y = (-20).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.clickme_mark),
                contentDescription = null,
                modifier = Modifier.size(148.dp),
            )
            Spacer(Modifier.height(Spacing.xl))
            Text(
                text = stringResource(R.string.splash_wordmark),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = stringResource(R.string.splash_tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.extras.onSurfaceMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

package com.uptrendinvestments.clickmeapp.screens.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.avatar.Avatar
import com.uptrendinvestments.clickmeapp.components.avatar.TopBarAvatarButton
import com.uptrendinvestments.clickmeapp.components.backgrounds.GreenGlowBlobLayer
import com.uptrendinvestments.clickmeapp.components.web.rememberCustomTabLauncher
import com.uptrendinvestments.clickmeapp.config.AppLinks
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras

private val AvatarSizes = listOf(32.dp, 40.dp, 56.dp, 96.dp)

/** Debug gallery section for the media components. No network images, so tests stay hermetic. */
@Composable
fun GalleryMediaSection(modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        AvatarBlock()
        GlowBlock()
        CustomTabBlock()
    }
}

@Composable
private fun AvatarBlock() {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(stringResource(R.string.gallery_heading_avatar), style = MaterialTheme.typography.titleLarge)
        Caption(stringResource(R.string.gallery_caption_sizes))
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AvatarSizes.forEach { Avatar(url = null, contentDescription = null, size = it) }
            Avatar(url = "   ", contentDescription = null)
        }
        Caption(stringResource(R.string.gallery_caption_fallback))
        TopBarAvatarButton(avatarUrl = null, onClick = {})
        Caption(stringResource(R.string.gallery_caption_topbar))
    }
}

@Composable
private fun GlowBlock() {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(stringResource(R.string.gallery_heading_glow), style = MaterialTheme.typography.titleLarge)
        Box(
            Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(MaterialTheme.colorScheme.surface),
        ) {
            GreenGlowBlobLayer()
            Text(
                text = stringResource(R.string.gallery_caption_glow),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.extras.onSurfaceMuted,
                modifier = Modifier.padding(Spacing.md),
            )
        }
    }
}

@Composable
private fun CustomTabBlock() {
    var failed by remember { mutableStateOf(false) }
    val launcher = rememberCustomTabLauncher(onFailure = { failed = true })
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(stringResource(R.string.gallery_heading_custom_tab), style = MaterialTheme.typography.titleLarge)
        OutlinedButton(onClick = {
            failed = false
            launcher(AppLinks.PRIVACY_POLICY.toUri())
        }) {
            Text(stringResource(R.string.gallery_sample_open_privacy))
        }
        OutlinedButton(onClick = {
            failed = false
            launcher("javascript:alert(1)".toUri())
        }) {
            Text(stringResource(R.string.gallery_sample_open_blocked))
        }
        if (failed) {
            Text(
                text = stringResource(R.string.custom_tab_open_failed),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.extras.destructive,
            )
        }
    }
}

@Composable
private fun Caption(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.extras.onSurfaceMuted,
    )
}

@Preview
@Composable
private fun GalleryMediaSectionPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            GalleryMediaSection(Modifier.padding(Spacing.md))
        }
    }
}

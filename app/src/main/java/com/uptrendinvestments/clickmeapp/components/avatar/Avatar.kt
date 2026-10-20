package com.uptrendinvestments.clickmeapp.components.avatar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme

const val AVATAR_FALLBACK_TAG = "avatar_fallback"
const val AVATAR_IMAGE_TAG = "avatar_image"

/**
 * Circular user avatar. Shows a Person silhouette on a surfaceContainerLow disc when [url] is null
 * or blank, and while the remote image loads or if it fails (iOS parity).
 *
 * Allowed sizes per UI-SPEC: 32, 40, 56 and 96 dp. Pass a null [contentDescription] for decorative
 * avatars.
 */
@Composable
fun Avatar(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
) {
    val semanticsModifier =
        if (contentDescription != null) {
            Modifier.semantics { this.contentDescription = contentDescription }
        } else {
            Modifier
        }
    Box(modifier.size(size).clip(CircleShape).then(semanticsModifier)) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .testTag(AVATAR_FALLBACK_TAG),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                modifier = Modifier.size(size * 0.625f),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model =
                    ImageRequest
                        .Builder(LocalContext.current)
                        .data(url)
                        .crossfade(200)
                        .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().testTag(AVATAR_IMAGE_TAG),
            )
        }
    }
}

@Preview
@Composable
private fun AvatarPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Avatar(url = null, contentDescription = null, size = 56.dp)
        }
    }
}

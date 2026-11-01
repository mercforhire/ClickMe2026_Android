package com.uptrendinvestments.clickmeapp.screens.clientmode.favorites.subviews

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMePalette
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme

/** Test tag on the person icon shown while the photo loads, on failure and when there is no URL. */
const val FAVORITE_PHOTO_FALLBACK_TAG = "favorite_photo_fallback"

private val PhotoWidth = 72.dp
private val PhotoHeight = 80.dp
private val FallbackIconSize = 32.dp

/**
 * The 72 x 80 rounded-rectangle expert photo of a Saved Experts card (iOS `FavoriteExpertCard` image). `Avatar` is
 * circle-only, so this draws its own box: the person icon sits underneath and the remote image, when there is a URL,
 * covers it, so the fallback shows while loading, on failure and without a URL. Decorative.
 */
@Composable
fun FavoriteExpertPhoto(
    url: String?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(width = PhotoWidth, height = PhotoHeight)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(ClickMePalette.avatarFallback),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Person,
            contentDescription = null,
            modifier = Modifier.size(FallbackIconSize).testTag(FAVORITE_PHOTO_FALLBACK_TAG),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Preview(name = "FavoriteExpertPhoto")
@Composable
private fun FavoriteExpertPhotoPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            FavoriteExpertPhoto(url = null)
        }
    }
}

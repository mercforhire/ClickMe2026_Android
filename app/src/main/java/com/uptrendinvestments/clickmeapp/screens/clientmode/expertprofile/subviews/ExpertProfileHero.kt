package com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.subviews

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.SkeletonBlock
import com.uptrendinvestments.clickmeapp.components.SkeletonGroup
import com.uptrendinvestments.clickmeapp.components.avatar.Avatar
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.HeroRating
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras

private val HeroAvatarSize = 160.dp
private val RingWidth = 2.dp
private val RingGap = 4.dp
private val StarSize = 20.dp

/**
 * Profile hero (D-14, D-15): a static glow behind a 160 dp avatar in a primary ring, then the name, the title and the
 * rating line. It renders from the route key alone, so the title and rating fall back to skeletons while unknown.
 */
@Composable
fun ExpertProfileHero(
    name: String,
    title: String,
    isTitlePending: Boolean,
    imageUrl: String?,
    rating: HeroRating,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            com.uptrendinvestments.clickmeapp.components.backgrounds
                .GreenGlowBlobLayer(Modifier.matchParentSize())
            Box(
                modifier =
                    Modifier
                        .padding(vertical = Spacing.lg)
                        .border(RingWidth, MaterialTheme.colorScheme.primary, CircleShape)
                        .padding(RingGap),
            ) {
                Avatar(url = imageUrl, contentDescription = null, size = HeroAvatarSize)
            }
        }
        Spacer(Modifier.height(Spacing.md))
        Text(
            text = name,
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.sm))
        when {
            isTitlePending ->
                SkeletonGroup {
                    SkeletonBlock(Modifier.size(width = 160.dp, height = 20.dp))
                }
            title.isNotBlank() ->
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
        }
        Spacer(Modifier.height(Spacing.sm))
        HeroRatingLine(rating)
    }
}

@Composable
private fun HeroRatingLine(rating: HeroRating) {
    when (rating) {
        is HeroRating.Shown -> ShownRatingLine(rating)
        HeroRating.NoReviews ->
            Text(
                text = stringResource(R.string.rating_no_reviews),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.extras.onSurfaceMuted,
            )
        HeroRating.Pending ->
            SkeletonGroup {
                SkeletonBlock(Modifier.size(width = 160.dp, height = 24.dp))
            }
        HeroRating.Hidden -> Unit
    }
}

@Composable
private fun ShownRatingLine(rating: HeroRating.Shown) {
    val ratingText = stringResource(R.string.rating_value, rating.rating)
    val description = pluralStringResource(R.plurals.rating_semantics, rating.count, ratingText, rating.count)
    Row(
        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            modifier = Modifier.size(StarSize),
            tint = MaterialTheme.colorScheme.tertiary,
        )
        Text(
            text = ratingText,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = pluralStringResource(R.plurals.rating_hero_review_count, rating.count, rating.count),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.extras.onSurfaceMuted,
        )
    }
}

@Preview(name = "ExpertProfileHero")
@Composable
private fun ExpertProfileHeroPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ExpertProfileHero(
                name = "Ada Lovelace",
                title = "Mathematician",
                isTitlePending = false,
                imageUrl = null,
                rating = HeroRating.Shown(4.8, 7),
            )
        }
    }
}

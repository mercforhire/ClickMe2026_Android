package com.uptrendinvestments.clickmeapp.screens.clientmode.search.subviews

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.screens.clientmode.search.ALL_CATEGORIES_SLUG
import com.uptrendinvestments.clickmeapp.screens.clientmode.search.SearchCategoryChipUi
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme

/** Test tag on the category chip row. */
const val SEARCH_CHIPS_ROW_TAG = "search_category_chips"

/**
 * Test tag of the chip for [slug]. The All chip (the [ALL_CATEGORIES_SLUG] sentinel) is `search_chip_all`; a category
 * chip is `search_chip_cat_<slug>`, so a category whose slug happens to be "all" can never share the All chip's tag.
 */
fun searchChipTag(slug: String): String =
    if (slug == ALL_CATEGORIES_SLUG) "search_chip_all" else "search_chip_cat_$slug"

private val RowHorizontalPadding = 20.dp
private val ChipSpacing = 10.dp

/**
 * Port of iOS `SearchExpertCategoryChips`: "All" first, then one chip per category in CategoryStore order, single
 * select. Selecting a chip reports its slug and All reports [ALL_CATEGORIES_SLUG] (D-11). Re-tapping the selected chip
 * reports its slug again and keeps it selected, as on iOS.
 *
 * Uses the Material 3 [FilterChip], which exposes the selected state to TalkBack and keeps the 48 dp touch target
 * (RESEARCH Pitfall 3). The project's removable multi-select chip is deliberately not used: it draws a close icon when
 * selected, which a single-select filter must not.
 */
@Composable
fun SearchExpertCategoryChips(
    categories: List<SearchCategoryChipUi>,
    selectedSlug: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth().testTag(SEARCH_CHIPS_ROW_TAG),
        contentPadding = PaddingValues(horizontal = RowHorizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(ChipSpacing),
    ) {
        item(key = ALL_CHIP_KEY) {
            SearchCategoryChip(
                name = stringResource(R.string.search_chip_all),
                slug = ALL_CATEGORIES_SLUG,
                selected = selectedSlug == ALL_CATEGORIES_SLUG,
                onSelect = onSelect,
            )
        }
        items(categories, key = { categoryChipKey(it.slug) }) { category ->
            SearchCategoryChip(
                name = category.name,
                slug = category.slug,
                selected = selectedSlug == category.slug,
                onSelect = onSelect,
            )
        }
    }
}

/**
 * `LazyRow` key of the All item. Category keys are built by [categoryChipKey] with a `cat_` prefix, and this key has a
 * different prefix, so no category slug (including "all" or "") can produce the same key and crash the row.
 */
private const val ALL_CHIP_KEY = "__all__"

/** `LazyRow` key of the chip for the category [slug]; the `cat_` prefix keeps it disjoint from [ALL_CHIP_KEY]. */
private fun categoryChipKey(slug: String): String = "cat_$slug"

@Composable
private fun SearchCategoryChip(
    name: String,
    slug: String,
    selected: Boolean,
    onSelect: (String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    FilterChip(
        selected = selected,
        onClick = { onSelect(slug) },
        label = {
            Text(
                text = name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        },
        modifier = Modifier.testTag(searchChipTag(slug)),
        shape = CircleShape,
        colors =
            FilterChipDefaults.filterChipColors(
                containerColor = colors.surfaceContainerLow,
                labelColor = colors.onSurface,
                selectedContainerColor = colors.primary,
                selectedLabelColor = colors.onPrimary,
            ),
        border =
            FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = selected,
                borderColor = colors.outlineVariant,
                selectedBorderColor = colors.primary,
            ),
    )
}

@Preview(name = "SearchExpertCategoryChips")
@Composable
private fun SearchExpertCategoryChipsPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            SearchExpertCategoryChips(
                categories =
                    listOf(
                        SearchCategoryChipUi(slug = "business", name = "Business"),
                        SearchCategoryChipUi(slug = "design", name = "Design"),
                    ),
                selectedSlug = "design",
                onSelect = {},
            )
        }
    }
}

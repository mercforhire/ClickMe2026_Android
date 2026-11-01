package com.uptrendinvestments.clickmeapp.screens.clientmode.reviews

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.navigation.Navigator

/**
 * Registers [ExpertReviewsKey] for a shell. Hilt-free: the Hilt-backed screen arrives through [reviews], so
 * HomeShellsTest drives the real metadata and back callback with stand-in content.
 *
 * Both shells register it, because the expert's own public profile keeps "See all" (D-15). [metadata] decides the
 * bottom bar per shell (RESEARCH A1): the client shell keeps the default (bar visible, like its Expert Profile entry)
 * and the expert shell passes `BottomBar.hidden()` (like `expertProfileEntry`). The key's origin copies the profile's,
 * so the same expert's reviews on two stacks never share a key (RESEARCH Pitfall 1).
 */
fun EntryProviderScope<NavKey>.expertReviewsEntry(
    navigator: Navigator,
    metadata: Map<String, Any> = emptyMap(),
    reviews: @Composable (key: ExpertReviewsKey, onBack: () -> Unit) -> Unit,
) {
    entry<ExpertReviewsKey>(metadata = metadata) { key -> reviews(key, navigator::goBack) }
}

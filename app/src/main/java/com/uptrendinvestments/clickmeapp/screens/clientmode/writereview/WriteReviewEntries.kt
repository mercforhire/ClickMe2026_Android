package com.uptrendinvestments.clickmeapp.screens.clientmode.writereview

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.navigation.BottomBar
import com.uptrendinvestments.clickmeapp.navigation.Navigator

/** Registers the Write Review route with the bottom bar hidden. Client shell only, since it is client-only (D-19). */
fun EntryProviderScope<NavKey>.writeReviewEntries(navigator: Navigator) {
    entry<WriteReviewKey>(metadata = BottomBar.hidden()) { WriteReviewRoute(it, navigator) }
}

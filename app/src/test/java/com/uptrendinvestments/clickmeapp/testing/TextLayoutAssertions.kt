package com.uptrendinvestments.clickmeapp.testing

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.text.TextLayoutResult

/**
 * The laid-out line count of every [text] label inside the node tagged [inTag], in tree order. Reads the unmerged tree
 * so the Text node itself is found even when its button merges descendants. Only meaningful under Robolectric native
 * graphics (`@GraphicsMode(NATIVE)`), where text is measured with the real font.
 */
fun SemanticsNodeInteractionsProvider.lineCountsOf(
    text: String,
    inTag: String,
): List<Int> {
    val nodes =
        onAllNodes(hasText(text) and hasAnyAncestor(hasTestTag(inTag)), useUnmergedTree = true)
            .fetchSemanticsNodes()
    check(nodes.isNotEmpty()) { "No \"$text\" label found inside a node tagged \"$inTag\"" }
    return nodes.map { node ->
        val layouts = mutableListOf<TextLayoutResult>()
        val action =
            checkNotNull(node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action) {
                "\"$text\" inside \"$inTag\" exposes no text layout action"
            }
        action.invoke(layouts)
        check(layouts.isNotEmpty()) { "\"$text\" inside \"$inTag\" returned no text layout" }
        layouts.first().lineCount
    }
}

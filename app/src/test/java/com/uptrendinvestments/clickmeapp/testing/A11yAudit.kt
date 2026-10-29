package com.uptrendinvestments.clickmeapp.testing

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * JVM substitute for Compose ATF checks (Phase 15 D-14 decision); proves exposure, not TalkBack speech
 * (RESEARCH Pitfall 9). `enableAccessibilityChecks()` is a no-op under Robolectric, so these four checks run
 * inside the existing Robolectric compose tests instead: actionables are named, click targets are at least 48 dp,
 * labels appear in the expected traversal order, and a text is a heading.
 */

private const val TOUCH_EPSILON_PX = 0.5f

/** Accessible name of a merged node: its content descriptions and texts (an editable value is not a name). */
private fun SemanticsNode.accessibleName(): String {
    val descriptions = config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()
    val texts = config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text }
    return (descriptions + texts).filter { it.isNotBlank() }.joinToString(" ")
}

private fun SemanticsNode.describe(): String = "Node#$id bounds=$boundsInRoot config=$config"

/** Fails when any clickable or editable node has no [accessibleName]. */
fun SemanticsNodeInteractionsProvider.assertActionablesNamed() {
    val unnamed =
        onAllNodes(hasClickAction() or hasSetTextAction())
            .fetchSemanticsNodes()
            .filter { it.accessibleName().isEmpty() }
    if (unnamed.isNotEmpty()) {
        throw AssertionError(
            "Actionable nodes without an accessible name:\n" + unnamed.joinToString("\n") { it.describe() },
        )
    }
}

/**
 * Fails when any clickable node is laid out smaller than [min] in either dimension. Uses layout size (including
 * minimumInteractiveComponentSize padding), not `touchBoundsInRoot`: Compose pads touch bounds to 48 dp for every
 * pointer-input node, which would hide small targets.
 */
fun SemanticsNodeInteractionsProvider.assertTouchTargets(min: Dp = 48.dp) {
    val small =
        onAllNodes(hasClickAction())
            .fetchSemanticsNodes()
            .filter { node ->
                val minPx = with(node.layoutInfo.density) { min.toPx() }
                val width = maxOf(node.boundsInRoot.width, node.layoutInfo.width.toFloat())
                val height = maxOf(node.boundsInRoot.height, node.layoutInfo.height.toFloat())
                width < minPx - TOUCH_EPSILON_PX || height < minPx - TOUCH_EPSILON_PX
            }
    if (small.isNotEmpty()) {
        throw AssertionError(
            "Click targets smaller than $min:\n" +
                small.joinToString("\n") { it.describe() },
        )
    }
}

/**
 * Named nodes of the merged tree in reading order. Inside a traversal group children sort by TraversalIndex
 * (default 0f), then top, then left; elsewhere by top, then left.
 */
fun SemanticsNodeInteractionsProvider.traversalLabels(): List<String> {
    val labels = mutableListOf<String>()

    fun visit(node: SemanticsNode) {
        val name = node.accessibleName()
        if (name.isNotEmpty()) labels += name
        val inGroup = node.config.getOrNull(SemanticsProperties.IsTraversalGroup) == true
        val children =
            node.children.sortedWith(
                compareBy<SemanticsNode>(
                    { if (inGroup) it.config.getOrNull(SemanticsProperties.TraversalIndex) ?: 0f else 0f },
                    { it.boundsInRoot.top },
                    { it.boundsInRoot.left },
                ),
            )
        children.forEach(::visit)
    }
    visit(onRoot().fetchSemanticsNode())
    return labels
}

/** Fails unless the [expected] labels appear in this order (substring match per label) in [traversalLabels]. */
fun SemanticsNodeInteractionsProvider.assertTraversalOrder(vararg expected: String) {
    val actual = traversalLabels()
    var from = 0
    for (label in expected) {
        val index = (from until actual.size).firstOrNull { actual[it].contains(label) }
        if (index == null) {
            throw AssertionError(
                "Expected '$label' after position $from in order ${expected.toList()}; actual traversal: $actual",
            )
        }
        from = index + 1
    }
}

/** Fails unless the node with exactly this [text] is marked as a heading. */
fun SemanticsNodeInteractionsProvider.assertIsHeading(text: String) {
    onNode(hasText(text)).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
}

package com.uptrendinvestments.clickmeapp.testing

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Proves [A11yAudit] fails on the defects it exists to catch (Phase 15 D-14 substitute). */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class A11yAuditTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun unnamedButtonFails() {
        rule.setContent { Box(Modifier.size(50.dp).clickable {}) }
        val error = assertThrows(AssertionError::class.java) { rule.assertActionablesNamed() }
        assertTrue(error.message.orEmpty().contains("Node#"))
    }

    @Test
    fun namedButtonPasses() {
        rule.setContent {
            IconButton(onClick = {}) { Icon(Icons.Filled.Close, contentDescription = "Close") }
        }
        rule.assertActionablesNamed()
        rule.assertTouchTargets()
    }

    @Test
    fun smallTargetFails() {
        rule.setContent {
            Box(
                Modifier
                    .size(40.dp)
                    .clickable(onClickLabel = "x") {}
                    .semantics { contentDescription = "Tiny" },
            )
        }
        rule.assertActionablesNamed()
        assertThrows(AssertionError::class.java) { rule.assertTouchTargets() }
    }

    @Test
    fun traversalOrderFollowsLayoutAndIndex() {
        rule.setContent {
            Column(Modifier.semantics { isTraversalGroup = true }) {
                Text("A")
                Text("B")
                Text("C", Modifier.semantics { traversalIndex = -1f })
            }
        }
        assertEquals(listOf("C", "A", "B"), rule.traversalLabels())
        rule.assertTraversalOrder("C", "A", "B")
        assertThrows(AssertionError::class.java) { rule.assertTraversalOrder("A", "C") }
    }

    @Test
    fun headingCheck() {
        rule.setContent {
            Column {
                Text("Title", Modifier.semantics { heading() })
                Text("Plain")
            }
        }
        rule.assertIsHeading("Title")
        assertThrows(AssertionError::class.java) { rule.assertIsHeading("Plain") }
    }
}

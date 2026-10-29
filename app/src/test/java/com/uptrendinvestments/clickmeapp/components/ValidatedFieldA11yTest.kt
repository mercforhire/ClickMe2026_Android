package com.uptrendinvestments.clickmeapp.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Semantics contract of [ValidatedField] (Phase 15 POL-04). Proves the merged tree exposes the name, not how
 * TalkBack speaks it (device-checklist row, RESEARCH Pitfall 9).
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class ValidatedFieldA11yTest {
    @get:Rule
    val rule = createComposeRule()

    private val isField = SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText)

    private fun fieldNode(): SemanticsNode = rule.onNode(isField).fetchSemanticsNode()

    private fun names(node: SemanticsNode): List<String> {
        val descriptions = node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()
        val texts = node.config.getOrNull(SemanticsProperties.Text).orEmpty()
        return descriptions + texts.map { it.text }
    }

    private fun setField(
        value: String,
        isSecure: Boolean = false,
        error: String? = null,
    ) {
        rule.setContent {
            Field(value, isSecure, error)
        }
    }

    @Composable
    private fun Field(
        value: String,
        isSecure: Boolean,
        error: String?,
    ) {
        ClickMeTheme {
            ValidatedField(
                value = value,
                onValueChange = {},
                placeholder = if (isSecure) "Password" else "Email",
                leadingIcon = if (isSecure) Icons.Outlined.Lock else Icons.Outlined.Email,
                error = error,
                isSecure = isSecure,
            )
        }
    }

    @Test
    fun nameWhenEmpty() {
        setField("")
        assertTrue(names(fieldNode()).any { it.contains("Email") })
    }

    @Test
    fun nameKeptWhenFilled() {
        setField("abc@x.com")
        val node = fieldNode()
        assertTrue(names(node).any { it.contains("Email") })
        assertEquals("abc@x.com", node.config[SemanticsProperties.EditableText].text)
    }

    @Test
    fun passwordValueNeverNamed() {
        setField("hunter2", isSecure = true)
        val node = fieldNode()
        assertTrue(names(node).any { it.contains("Password") })
        assertFalse(names(node).any { it.contains("hunter2") })
    }

    @Test
    fun errorIsErrorSemantics() {
        setField("bad", error = "Bad email")
        assertEquals("Bad email", fieldNode().config[SemanticsProperties.Error])
    }
}

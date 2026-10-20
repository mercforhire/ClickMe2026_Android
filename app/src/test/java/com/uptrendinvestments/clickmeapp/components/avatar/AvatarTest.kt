package com.uptrendinvestments.clickmeapp.components.avatar

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.components.backgrounds.GreenGlowBlobLayer
import com.uptrendinvestments.clickmeapp.screens.debug.GalleryMediaSection
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AvatarTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun nullUrl_showsFallbackOnly() {
        rule.setContent { ClickMeTheme { Avatar(url = null, contentDescription = null) } }

        rule.onNodeWithTag(AVATAR_FALLBACK_TAG).assertExists()
        rule.onAllNodesWithTag(AVATAR_IMAGE_TAG).assertCountEquals(0)
    }

    @Test
    fun blankUrl_showsFallbackOnly() {
        rule.setContent { ClickMeTheme { Avatar(url = "   ", contentDescription = null) } }

        rule.onNodeWithTag(AVATAR_FALLBACK_TAG).assertExists()
        rule.onAllNodesWithTag(AVATAR_IMAGE_TAG).assertCountEquals(0)
    }

    @Test
    fun unresolvableUrl_keepsFallbackUnderImage() {
        rule.setContent {
            ClickMeTheme { Avatar(url = "file:///nonexistent/avatar.png", contentDescription = null) }
        }

        rule.onNodeWithTag(AVATAR_IMAGE_TAG).assertExists()
        rule.onNodeWithTag(AVATAR_FALLBACK_TAG).assertExists()
    }

    @Test
    fun contentDescription_isExposedWhenProvided() {
        rule.setContent { ClickMeTheme { Avatar(url = null, contentDescription = "Alex") } }

        rule.onNodeWithContentDescription("Alex").assertExists()
    }

    @Test
    fun contentDescription_isAbsentWhenNull() {
        rule.setContent { ClickMeTheme { Avatar(url = null, contentDescription = null) } }

        rule
            .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription))
            .assertCountEquals(0)
    }

    @Test
    fun topBarAvatarButton_isLabelledClickableAndAtLeast48dp() {
        var clicks = 0
        rule.setContent { ClickMeTheme { TopBarAvatarButton(avatarUrl = null, onClick = { clicks++ }) } }

        val button = rule.onNodeWithContentDescription("Your profile")
        button.assertHasClickAction()
        button.assertWidthIsAtLeast(48.dp)
        button.assertHeightIsAtLeast(48.dp)
        button.performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun greenGlowBlobLayer_isDecorative() {
        rule.setContent {
            ClickMeTheme {
                Box {
                    GreenGlowBlobLayer()
                    Text("content")
                }
            }
        }

        rule.onNodeWithText("content").assertIsDisplayed()
        rule
            .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription))
            .assertCountEquals(0)
        rule
            .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text))
            .assertCountEquals(1)
    }

    @Test
    fun galleryMediaSection_rendersHeadings() {
        rule.setContent { ClickMeTheme { GalleryMediaSection() } }

        rule.onNodeWithText("Avatar").assertExists()
        rule.onNodeWithText("GreenGlowBlobLayer").assertExists()
        rule.onNodeWithText("Custom Tab launcher").assertExists()
    }
}

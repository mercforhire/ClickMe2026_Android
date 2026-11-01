package com.uptrendinvestments.clickmeapp.components.avatar

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.components.backgrounds.GreenGlowBlobLayer
import com.uptrendinvestments.clickmeapp.screens.debug.GalleryMediaSection
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMePalette
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

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
    fun size160_rendersA160dpNode() {
        rule.setContent { ClickMeTheme { Avatar(url = null, contentDescription = "p", size = 160.dp) } }

        rule.onNodeWithContentDescription("p").assertWidthIsEqualTo(160.dp).assertHeightIsEqualTo(160.dp)
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun localImage_isShownInsteadOfRemoteImage() {
        rule.setContent {
            ClickMeTheme { Avatar(url = "https://x/y.png", contentDescription = null, image = ImageBitmap(4, 4)) }
        }

        rule.onNodeWithTag(AVATAR_LOCAL_IMAGE_TAG).assertExists()
        rule.onAllNodesWithTag(AVATAR_IMAGE_TAG).assertCountEquals(0)
    }

    @Test
    fun nullImageAndNullUrl_showsFallbackWithoutLocalImage() {
        rule.setContent { ClickMeTheme { Avatar(url = null, contentDescription = null, image = null) } }

        rule.onNodeWithTag(AVATAR_FALLBACK_TAG).assertExists()
        rule.onAllNodesWithTag(AVATAR_LOCAL_IMAGE_TAG).assertCountEquals(0)
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
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun fallbackColor_paintsTheFallbackDisc() {
        val green = ClickMePalette.avatarFallback
        rule.setContent {
            ClickMeTheme { Avatar(url = null, contentDescription = null, size = 80.dp, fallbackColor = green) }
        }

        assertDiscPixelMatches(green)
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun noFallbackColor_keepsSurfaceContainerLow() {
        var expected = Color.Unspecified
        rule.setContent {
            ClickMeTheme {
                expected = MaterialTheme.colorScheme.surfaceContainerLow
                Avatar(url = null, contentDescription = null, size = 80.dp)
            }
        }

        assertDiscPixelMatches(expected)
    }

    private fun assertDiscPixelMatches(expected: Color) {
        val pixels = rule.onNodeWithTag(AVATAR_FALLBACK_TAG).captureToImage().toPixelMap()
        // Inside the disc but above the person icon.
        val actual = pixels[pixels.width / 2, pixels.height / 10]
        listOf(
            Triple("red", actual.red, expected.red),
            Triple("green", actual.green, expected.green),
            Triple("blue", actual.blue, expected.blue),
        ).forEach { (channel, got, want) ->
            assertEquals("$channel channel", want * 255f, got * 255f, 2f)
        }
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

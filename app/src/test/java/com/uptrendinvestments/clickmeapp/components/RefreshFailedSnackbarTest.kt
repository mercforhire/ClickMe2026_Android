package com.uptrendinvestments.clickmeapp.components

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RefreshFailedSnackbarTest {
    @get:Rule
    val rule = createComposeRule()

    private val message = "Couldn't refresh. Check your connection and try again."

    private val host = SnackbarHostState()
    private var failed by mutableStateOf(false)
    private var shown = 0
    private var retries = 0

    private fun show() {
        rule.setContent {
            ClickMeTheme {
                RefreshFailedSnackbarEffect(
                    refreshFailed = failed,
                    snackbarHostState = host,
                    onRetry = { retries++ },
                    onShown = {
                        shown++
                        failed = false
                    },
                    message = message,
                    retryLabel = "Retry",
                )
                SnackbarHost(host)
            }
        }
    }

    private fun count(text: String) = rule.onAllNodesWithText(text).fetchSemanticsNodes().size

    @Test
    fun showsMessageAndRetry() {
        show()
        rule.runOnIdle { failed = true }
        rule.waitForIdle()
        rule.onNodeWithText(message).assertExists()
        rule.onNodeWithText("Retry").assertExists()
        assertEquals(1, shown)
    }

    @Test
    fun retryRunsOnRetry() {
        show()
        rule.runOnIdle { failed = true }
        rule.waitForIdle()
        rule.onNodeWithText("Retry").performClick()
        rule.waitForIdle()
        assertEquals(1, retries)
        assertEquals(0, count(message))
    }

    @Test
    fun secondFailureWhileVisibleIsSilent() {
        show()
        rule.runOnIdle { failed = true }
        rule.waitForIdle()
        rule.runOnIdle { failed = true }
        rule.waitForIdle()
        assertEquals(2, shown)
        assertEquals(1, count(message))
        assertEquals(0, retries)
    }

    @Test
    fun otherSnackbarIsDismissedFirst() {
        lateinit var launchOther: () -> Unit
        rule.setContent {
            ClickMeTheme {
                val scope = rememberCoroutineScope()
                launchOther = { scope.launch { host.showSnackbar("Saved") } }
                RefreshFailedSnackbarEffect(
                    refreshFailed = failed,
                    snackbarHostState = host,
                    onRetry = { retries++ },
                    onShown = { failed = false },
                    message = message,
                    retryLabel = "Retry",
                )
                SnackbarHost(host)
            }
        }
        rule.runOnIdle { launchOther() }
        rule.waitForIdle()
        rule.onNodeWithText("Saved").assertExists()
        rule.runOnIdle { failed = true }
        rule.waitForIdle()
        assertEquals(0, count("Saved"))
        rule.onNodeWithText(message).assertExists()
    }

    @Test
    fun dismissDoesNothing() {
        show()
        rule.runOnIdle { failed = true }
        rule.waitForIdle()
        rule.runOnIdle { host.currentSnackbarData?.dismiss() }
        rule.waitForIdle()
        assertEquals(0, retries)
        assertEquals(0, count(message))
    }
}

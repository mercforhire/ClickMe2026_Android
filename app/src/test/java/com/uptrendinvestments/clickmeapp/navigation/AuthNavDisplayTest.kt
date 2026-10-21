package com.uptrendinvestments.clickmeapp.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private class CounterViewModel : ViewModel() {
    val id: Int = System.identityHashCode(this)
}

private class ClearTrackingViewModel(
    private val onCleared: () -> Unit,
) : ViewModel() {
    override fun onCleared() = onCleared.invoke()
}

private class SavedHandleViewModel(
    val handle: SavedStateHandle,
) : ViewModel()

@RunWith(RobolectricTestRunner::class)
class AuthNavDisplayTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var authNavigator: AuthNavigator? = null
    private var prefill: String? = null
    private val forgotViewModelIds = mutableListOf<Int>()
    private var savedHandleSeen = false
    private var setNewViewModelCleared = false

    private fun nav(): AuthNavigator = checkNotNull(authNavigator)

    private fun entry(key: NavKey): NavEntry<NavKey> =
        NavEntry(key) {
            when (key) {
                is LoginKey -> Text("stub:login")
                is ForgotPasswordKey -> {
                    val counter = viewModel { CounterViewModel() }
                    val saved = viewModel { SavedHandleViewModel(createSavedStateHandle()) }
                    savedHandleSeen = saved.handle.keys().isEmpty()
                    forgotViewModelIds += counter.id
                    Text("stub:forgot:${key.email}")
                }
                is SetNewPasswordKey -> {
                    viewModel { ClearTrackingViewModel { setNewViewModelCleared = true } }
                    Text("stub:setnew:${key.email}")
                }
                is PasswordChangedKey -> Text("stub:changed:${key.email}")
                else -> Text("stub:other")
            }
        }

    private fun setContent() {
        composeRule.setContent {
            ClickMeTheme {
                val state = rememberNavigationState(LoginKey, setOf(LoginKey))
                val auth = remember(state) { AuthNavigator(Navigator(state)) }
                authNavigator = auth
                AuthNavDisplay(
                    navigationState = state,
                    authNavigator = auth,
                    onReturnToLogin = { prefill = it },
                    entryProvider = ::entry,
                )
            }
        }
    }

    private fun backLikeNavDisplay() {
        composeRule.runOnIdle { nav().handleBack { prefill = it } }
        composeRule.waitForIdle()
    }

    @Test
    fun showsLoginThenForgotWithEmail() {
        setContent()
        composeRule.onNode(hasText("stub:login")).assertExists()
        composeRule.runOnIdle { nav().openForgotPassword("a@b.co") }
        composeRule.waitForIdle()
        composeRule.onNode(hasText("stub:forgot:a@b.co")).assertExists()
    }

    @Test
    fun entryScopedViewModelIsFreshPerPushAndSavedStateIsProvided() {
        setContent()
        composeRule.runOnIdle { nav().openForgotPassword("a@b.co") }
        composeRule.waitForIdle()
        backLikeNavDisplay()
        composeRule.onNode(hasText("stub:login")).assertExists()
        composeRule.runOnIdle { nav().openForgotPassword("a@b.co") }
        composeRule.waitForIdle()
        composeRule.onNode(hasText("stub:forgot:a@b.co")).assertExists()
        val distinct = forgotViewModelIds.distinct()
        assertEquals(2, distinct.size)
        assertNotEquals(distinct[0], distinct[1])
        assertEquals(true, savedHandleSeen)
    }

    @Test
    fun backOnPasswordChangedShowsLoginWithPrefill() {
        setContent()
        composeRule.runOnIdle {
            nav().openForgotPassword("a@b.co")
            nav().openSetNewPassword("a@b.co")
            nav().openPasswordChanged("a@b.co")
        }
        composeRule.waitForIdle()
        composeRule.onNode(hasText("stub:changed:a@b.co")).assertExists()
        backLikeNavDisplay()
        composeRule.onNode(hasText("stub:login")).assertExists()
        assertEquals("a@b.co", prefill)
    }

    @Test
    fun passwordChangedReleasesRecoveryScreensAndTheirViewModels() {
        setContent()
        composeRule.runOnIdle {
            nav().openForgotPassword("a@b.co")
            nav().openSetNewPassword("a@b.co")
        }
        composeRule.waitForIdle()
        composeRule.onNode(hasText("stub:setnew:a@b.co")).assertExists()
        composeRule.runOnIdle { nav().openPasswordChanged("a@b.co") }
        composeRule.waitForIdle()
        composeRule.onNode(hasText("stub:changed:a@b.co")).assertExists()
        composeRule.onNode(hasText("stub:setnew:a@b.co")).assertDoesNotExist()
        composeRule.onNode(hasText("stub:forgot:a@b.co")).assertDoesNotExist()
        assertTrue("Set New Password ViewModel was not cleared", setNewViewModelCleared)
    }

    @Test
    fun backFromSetNewPasswordShowsForgotWithEmailIntact() {
        setContent()
        composeRule.runOnIdle {
            nav().openForgotPassword("a@b.co")
            nav().openSetNewPassword("a@b.co")
        }
        composeRule.waitForIdle()
        composeRule.onNode(hasText("stub:setnew:a@b.co")).assertExists()
        backLikeNavDisplay()
        composeRule.onNode(hasText("stub:forgot:a@b.co")).assertExists()
    }
}

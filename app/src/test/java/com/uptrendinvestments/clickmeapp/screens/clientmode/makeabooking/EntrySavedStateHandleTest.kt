package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.HomeClientShell
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@Serializable
internal data object ProbeKey : NavKey

private class ProbeViewModel(
    val handle: SavedStateHandle,
) : ViewModel()

/**
 * Research Open Question 3: a Nav3 entry's ViewModel can be created with a SavedStateHandle, and what it holds
 * survives recreation, which is what `MakeABookingViewModel` needs for process-death restore. Uses the real
 * client shell and its entry decorators.
 */
@RunWith(RobolectricTestRunner::class)
class EntrySavedStateHandleTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun navEntryCanCreateASavedStateHandle() {
        composeRule.setContent {
            ClickMeTheme {
                HomeClientShell(
                    avatarUrl = null,
                    showDebugAffordances = false,
                    onSwitchMode = {},
                    onLogout = {},
                    onDebugForceRefresh = {},
                    debugRefreshStatus = null,
                    exploreRoot = { navigator, _ ->
                        Button(onClick = { navigator.navigate(ProbeKey) }) { Text("open probe") }
                    },
                    detailEntries = {
                        entry<ProbeKey> {
                            val vm = viewModel { ProbeViewModel(createSavedStateHandle()) }
                            vm.handle["probe"] = "x"
                            Text("probe ready ${vm.handle.get<String>("probe")}")
                        }
                    },
                )
            }
        }
        composeRule.onNodeWithText("open probe").performClick()
        composeRule.onNodeWithText("probe ready x").assertExists()
    }

    /**
     * Emulates process death for an entry: the saveable state is saved and restored, and the ViewModels are gone
     * (a fresh [ViewModelStoreOwner] above the shell). A value written to the entry ViewModel's SavedStateHandle
     * must come back in the recreated ViewModel, through the shell's real Nav3 entry decorators. This is the path
     * `MakeABookingViewModel` relies on to restore the form and a completed booking id.
     */
    @Test
    fun entrySavedStateHandleSurvivesRecreationWithFreshViewModels() {
        val tester = StateRestorationTester(composeRule)
        // Read during composition only, so swapping it does not recompose before the save.
        var owner: ViewModelStoreOwner = FreshViewModelStoreOwner()
        var created = 0
        tester.setContent {
            CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
                ClickMeTheme {
                    HomeClientShell(
                        avatarUrl = null,
                        showDebugAffordances = false,
                        onSwitchMode = {},
                        onLogout = {},
                        onDebugForceRefresh = {},
                        debugRefreshStatus = null,
                        exploreRoot = { navigator, _ ->
                            Button(onClick = { navigator.navigate(ProbeKey) }) { Text("open probe") }
                        },
                        detailEntries = {
                            entry<ProbeKey> {
                                val vm =
                                    viewModel {
                                        created++
                                        ProbeViewModel(createSavedStateHandle())
                                    }
                                val value by vm.handle.getStateFlow<String?>("probe", null).collectAsState()
                                Button(onClick = { vm.handle["probe"] = "kept" }) { Text("write probe") }
                                Text("probe value $value")
                            }
                        },
                    )
                }
            }
        }
        composeRule.onNodeWithText("open probe").performClick()
        composeRule.onNodeWithText("write probe").performClick()
        composeRule.onNodeWithText("probe value kept").assertExists()

        owner = FreshViewModelStoreOwner()
        tester.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithText("probe value kept").assertExists()
        assertEquals(2, created)
    }
}

private class FreshViewModelStoreOwner : ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()
}

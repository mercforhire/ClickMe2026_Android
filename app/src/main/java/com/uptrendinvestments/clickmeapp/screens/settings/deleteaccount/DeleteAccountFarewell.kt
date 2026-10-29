package com.uptrendinvestments.clickmeapp.screens.settings.deleteaccount

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import com.uptrendinvestments.clickmeapp.managers.UserManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@Composable
fun DeleteAccountFarewell(
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier,
) = Unit

@HiltViewModel
class AccountDeletionFarewellViewModel
    @Inject
    constructor(
        private val state: AccountDeletionState,
        private val userManager: UserManager,
    ) : ViewModel() {
        val visible: StateFlow<Boolean> = state.farewellVisible

        fun backToLogin() = Unit
    }

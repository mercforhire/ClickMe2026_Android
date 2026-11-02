package com.uptrendinvestments.clickmeapp.screens.clientmode.payments

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Nav3 route to Payment History, pushed from the client hub on the Profile stack only. */
@Serializable
data object ClientTransactionsKey : NavKey

package com.uptrendinvestments.clickmeapp.screens.clientmode.favorites

import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.login.FieldError

/**
 * The one failure message for a favorites change: the profile heart rollback (D-03) and the Saved Experts remove
 * rollback (D-10 discretion: a snackbar instead of the iOS alert, RESEARCH Open Question 2). The server message is
 * shown when there is one, else "Couldn't update favorites" (the iOS alert title). The caller shows it as a snackbar
 * and never logs it.
 */
fun ApiException.toFavoriteFailure(): FieldError =
    userMessage?.let { FieldError.Text(it) } ?: FieldError.Res(R.string.favorites_update_failed)

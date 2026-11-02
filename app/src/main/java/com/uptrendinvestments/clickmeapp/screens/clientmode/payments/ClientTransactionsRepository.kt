package com.uptrendinvestments.clickmeapp.screens.clientmode.payments

import com.uptrendinvestments.clickmeapp.model.responses.ClientTransactionsData
import com.uptrendinvestments.clickmeapp.network.ClickMeApi
import com.uptrendinvestments.clickmeapp.network.apiCall
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Payment-history source for the client (iOS `ClickMeAPI+ClientTransactions`). Stateless; throws
 * [com.uptrendinvestments.clickmeapp.network.ApiException] only. The server scopes the ledger by the JWT, so no user
 * id is sent. Rows hold expert names and are never logged.
 */
@Singleton
class ClientTransactionsRepository
    @Inject
    constructor(
        private val api: ClickMeApi,
    ) {
        /** `GET /client/transactions`: one page of the caller's payments, newest first. */
        suspend fun page(
            page: Int,
            limit: Int,
        ): ClientTransactionsData = apiCall { api.getClientTransactions(page, limit) }.data
    }

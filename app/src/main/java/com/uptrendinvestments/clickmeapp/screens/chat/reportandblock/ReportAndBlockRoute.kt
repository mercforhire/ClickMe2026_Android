package com.uptrendinvestments.clickmeapp.screens.chat.reportandblock

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.enums.AppMode
import com.uptrendinvestments.clickmeapp.navigation.Navigator
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar

/**
 * The Report & Block route. The body is a placeholder until the Report & Block screen replaces it; the signature
 * stays.
 */
@Composable
fun ReportAndBlockRoute(
    key: ChatReportBlockKey,
    navigator: Navigator,
    mode: AppMode,
) {
    Scaffold(
        topBar = {
            AuthTopAppBar(
                title = stringResource(R.string.report_block_title),
                onBack = navigator::goBack,
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding))
    }
}

package com.localfix.app.ui

import androidx.compose.runtime.Composable
import com.localfix.app.ui.home.ResidentHomeScreen
import com.localfix.app.ui.home.ResidentHomeUiState
import com.localfix.app.ui.theme.LocalFixTheme

@Composable
fun LocalFixApp() {
    LocalFixTheme {
        ResidentHomeScreen(
            uiState = ResidentHomeUiState.sample,
            onReportIssue = {},
            onRequestClick = {},
            onCategoryClick = {},
        )
    }
}

package com.localfix.app.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.localfix.app.ui.theme.LocalFixTheme
import org.junit.Rule
import org.junit.Test

class ResidentHomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun residentHomeShowsPrimaryApartmentInformation() {
        composeRule.setContent {
            LocalFixTheme {
                ResidentHomeScreen(
                    uiState = ResidentHomeUiState.sample,
                    onReportIssue = {},
                    onRequestClick = {},
                    onCategoryClick = {},
                )
            }
        }

        composeRule.onNodeWithText("Lakeview Residency").assertIsDisplayed()
        composeRule.onNodeWithText("Report an issue").assertIsDisplayed()
        composeRule.onNodeWithText("Leaking kitchen tap").assertIsDisplayed()
    }
}

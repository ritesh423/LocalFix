package com.localfix.app.ui.requests

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.localfix.app.ui.components.RequestLoadUiState
import com.localfix.app.ui.theme.LocalFixTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ResidentRequestsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyResponseIsExplainedInsteadOfLookingLikeLoading() {
        composeRule.setContent {
            LocalFixTheme {
                ResidentRequestsScreen(
                    uiState = ResidentRequestsUiState.sample.copy(
                        requests = emptyList(),
                        requestLoadState = RequestLoadUiState.Empty,
                    ),
                    onFilterSelected = {},
                    onReportIssue = {},
                    onRequestClick = {},
                    onRetryRequests = {},
                )
            }
        }

        composeRule.onNodeWithText("No requests yet").assertIsDisplayed()
        composeRule.onAllNodesWithText("Loading your requests…").assertCountEquals(0)
    }

    @Test
    fun initialFailureShowsRetryAction() {
        var retried = false
        composeRule.setContent {
            LocalFixTheme {
                ResidentRequestsScreen(
                    uiState = ResidentRequestsUiState.sample.copy(
                        requests = emptyList(),
                        requestLoadState = RequestLoadUiState.Failed("Server unavailable"),
                    ),
                    onFilterSelected = {},
                    onReportIssue = {},
                    onRequestClick = {},
                    onRetryRequests = { retried = true },
                )
            }
        }

        composeRule.onNodeWithText("Requests couldn't be loaded").assertIsDisplayed()
        composeRule.onNodeWithText("Try again").performClick()
        assertTrue(retried)
    }

    @Test
    fun refreshFailureKeepsShowingPreviouslyLoadedRequests() {
        composeRule.setContent {
            LocalFixTheme {
                ResidentRequestsScreen(
                    uiState = ResidentRequestsUiState.sample.copy(
                        requestLoadState = RequestLoadUiState.Stale("Server unavailable."),
                    ),
                    onFilterSelected = {},
                    onReportIssue = {},
                    onRequestClick = {},
                    onRetryRequests = {},
                )
            }
        }

        composeRule.onNodeWithText("Server unavailable. Showing requests loaded earlier.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Leaking kitchen tap").assertIsDisplayed()
    }
}

package com.localfix.app.ui.manager

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.localfix.app.ui.components.RequestLoadUiState
import com.localfix.app.ui.theme.LocalFixTheme
import org.junit.Rule
import org.junit.Test

class ManagerQueueScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun managerQueueShowsTheBackendOperationsSummary() {
        composeRule.setContent {
            LocalFixTheme {
                ManagerQueueScreen(
                    uiState = ManagerQueueUiState(
                        propertyName = "Lakeview Residency",
                        summary = ManagerSummaryUiState(
                            activeRequests = 4,
                            needsAssignment = 1,
                            assigned = 1,
                            inProgress = 1,
                            blocked = 0,
                            awaitingConfirmation = 1,
                            completed = 3,
                        ),
                        tickets = emptyList(),
                        loadState = RequestLoadUiState.Empty,
                    ),
                    onTicketClick = {},
                    onRetry = {},
                    onSwitchRole = {},
                )
            }
        }

        composeRule.onNodeWithText("4 active requests").assertIsDisplayed()
        composeRule.onNodeWithText("Need assignment").assertIsDisplayed()
        composeRule.onNodeWithText("Blocked").assertIsDisplayed()
        composeRule.onNodeWithText("Resident review").assertIsDisplayed()
        composeRule.onNodeWithText("Completed").assertIsDisplayed()
    }
}

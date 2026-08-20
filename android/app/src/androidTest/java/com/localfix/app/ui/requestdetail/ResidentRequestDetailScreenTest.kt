package com.localfix.app.ui.requestdetail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.localfix.app.ui.requests.RequestStatusTone
import com.localfix.app.ui.theme.LocalFixTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ResidentRequestDetailScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun failedDeliveryCanBeRetriedOrDiscardedWithConfirmation() {
        var retried = false
        var discarded = false
        composeRule.setContent {
            LocalFixTheme {
                ResidentRequestDetailScreen(
                    uiState = failedRequestState(),
                    onBack = {},
                    onReviewDecisionSelected = {},
                    onRatingSelected = {},
                    onFeedbackChanged = {},
                    onSubmitReview = {},
                    onRetryDelivery = { retried = true },
                    onDiscardDelivery = { discarded = true },
                )
            }
        }

        composeRule.onNodeWithTag("retry-failed-request")
            .performScrollTo()
            .performClick()
        assertTrue(retried)

        composeRule.onNodeWithTag("discard-failed-request").performClick()
        composeRule.onNodeWithText("Discard this request?").assertIsDisplayed()
        composeRule.onNodeWithTag("confirm-discard-failed-request").performClick()
        assertTrue(discarded)
    }

    @Test
    fun queuedReviewShowsItsSavedStateWithoutAnotherSubmitButton() {
        composeRule.setContent {
            LocalFixTheme {
                ResidentRequestDetailScreen(
                    uiState = failedRequestState().copy(
                        id = "LF-90000000",
                        statusLabel = "Confirm repair",
                        residentRating = 5,
                        residentFeedback = "The repair works properly now.",
                        delivery = null,
                        reviewDelivery = ReviewDeliveryUiState(
                            title = "Review waiting to send",
                            message = "Your review is saved on this device.",
                            isFailure = false,
                        ),
                    ),
                    onBack = {},
                    onReviewDecisionSelected = {},
                    onRatingSelected = {},
                    onFeedbackChanged = {},
                    onSubmitReview = {},
                    onRetryDelivery = {},
                    onDiscardDelivery = {},
                )
            }
        }

        composeRule.onNodeWithText("Review waiting to send")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onAllNodesWithTag("resident-submit-review").assertCountEquals(0)
    }

    private fun failedRequestState() = ResidentRequestDetailUiState(
        requestId = "50000000-0000-0000-0000-000000000004",
        id = "LOCAL-50000000",
        title = "Kitchen tap is leaking",
        description = "Water continues dripping after the tap is fully closed.",
        categoryLabel = "Plumbing",
        statusLabel = "Send failed",
        statusTone = RequestStatusTone.ATTENTION,
        urgencyLabel = "Soon",
        accessWindowLabel = "Morning · 8 AM–12 PM",
        assignedWorker = "This request wasn't sent.",
        updatedLabel = "Not sent",
        photoUri = null,
        completionNote = null,
        partsUsed = emptyList(),
        completionPhotoUrl = null,
        residentRating = null,
        residentFeedback = null,
        canReview = false,
        delivery = RequestDeliveryUiState(
            title = "Request wasn't sent",
            message = "This request wasn't sent. Its details are still saved.",
            canRetry = true,
            canDiscard = true,
        ),
        review = ResidentReviewUiState(),
    )
}

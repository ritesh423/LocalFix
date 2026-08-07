package com.localfix.app.ui.create

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.localfix.app.ui.theme.LocalFixTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CreateRequestScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun selectedPhotoCanBePreviewedAndRemoved() {
        var removeCalled = false
        composeRule.setContent {
            LocalFixTheme {
                CreateRequestScreen(
                    uiState = CreateRequestUiState(
                        draft = RequestDraft(
                            photoUri = "content://localfix/photo/test",
                        ),
                    ),
                    onBack = {},
                    onDiscard = {},
                    onCategorySelected = {},
                    onTitleChanged = {},
                    onDescriptionChanged = {},
                    onUrgencySelected = {},
                    onAccessWindowSelected = {},
                    onPhotoSelected = {},
                    onPhotoRemoved = { removeCalled = true },
                    onPhotoSelectionFailed = {},
                    onSubmit = {},
                )
            }
        }

        composeRule.onNodeWithText("Photo attached").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("request-photo-preview").assertIsDisplayed()
        composeRule.onNodeWithTag("remove-request-photo").performClick()

        assertTrue(removeCalled)
    }
}

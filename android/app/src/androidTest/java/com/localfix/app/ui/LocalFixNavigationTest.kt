package com.localfix.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.localfix.app.data.DefaultAppContainer
import org.junit.Rule
import org.junit.Test

class LocalFixNavigationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun residentCanMoveBetweenMainDestinationsAndSwitchWorkspace() {
        composeRule.setContent {
            LocalFixApp(DefaultAppContainer())
        }

        composeRule.onNodeWithText("Choose your workspace").assertIsDisplayed()
        composeRule.onNodeWithText("Resident").performClick()
        composeRule.onNodeWithText("Your home, taken care of.").assertIsDisplayed()

        composeRule.onNodeWithTag("resident-nav-resident/requests").performClick()
        composeRule.onNodeWithText("My requests").assertIsDisplayed()
        composeRule.onNodeWithTag("request-filter-completed").performClick()
        composeRule.onNodeWithText("Washing machine vibration").assertIsDisplayed()

        composeRule.onNodeWithTag("resident-nav-resident/profile").performClick()
        composeRule.onNodeWithText("Your apartment and contact details.").assertIsDisplayed()
        composeRule.onNodeWithText("Switch workspace").performClick()

        composeRule.onNodeWithText("Choose your workspace").assertIsDisplayed()
    }
}

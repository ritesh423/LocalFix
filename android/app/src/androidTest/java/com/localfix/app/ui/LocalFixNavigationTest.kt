package com.localfix.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import com.localfix.app.data.AppContainer
import com.localfix.app.data.draft.InMemoryRequestDraftRepository
import com.localfix.app.data.draft.RequestDraftRepository
import com.localfix.app.data.manager.ManagerRepository
import com.localfix.app.data.manager.SampleManagerRepository
import com.localfix.app.data.resident.ResidentRepository
import com.localfix.app.data.resident.SampleResidentRepository
import com.localfix.app.data.worker.SampleWorkerRepository
import com.localfix.app.data.worker.WorkerRepository
import org.junit.Rule
import org.junit.Test

class LocalFixNavigationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun residentCanMoveBetweenMainDestinationsAndSwitchWorkspace() {
        composeRule.setContent {
            LocalFixApp(testAppContainer())
        }

        composeRule.onNodeWithText("Choose your workspace").assertIsDisplayed()
        composeRule.onNodeWithText("Resident").performClick()
        composeRule.onNodeWithText("Your home, taken care of.").assertIsDisplayed()
        composeRule.onNodeWithText("Leaking kitchen tap").performClick()
        composeRule.onNodeWithText("The tap keeps dripping even when fully closed.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Morning · 8 AM–12 PM").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.onNodeWithText("Your home, taken care of.").assertIsDisplayed()
        composeRule.onNodeWithText("Report an issue").performClick()
        composeRule.onNodeWithText("Give the maintenance team enough detail to act quickly.")
            .assertIsDisplayed()
        composeRule.onNodeWithTag("request-category-plumbing").performClick()
        composeRule.onNodeWithTag("request-title").performTextInput("Water dripping below sink")
        composeRule.onNodeWithTag("request-description").performTextInput(
            "Water collects below the kitchen sink after using the tap.",
        )
        composeRule.onNodeWithTag("create-request-list").performScrollToIndex(6)
        composeRule.onNodeWithTag("submit-request").performClick()
        composeRule.onNodeWithText("My requests").assertIsDisplayed()
        composeRule.onNodeWithText("Water dripping below sink").assertIsDisplayed()
        composeRule.onNodeWithText("Water dripping below sink").performClick()
        composeRule.onNodeWithText(
            "Water collects below the kitchen sink after using the tap.",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Awaiting assignment").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Back").performClick()

        composeRule.onNodeWithTag("resident-nav-resident/requests").performClick()
        composeRule.onNodeWithText("My requests").assertIsDisplayed()
        composeRule.onNodeWithTag("request-filter-completed").performClick()
        composeRule.onNodeWithText("Washing machine vibration").assertIsDisplayed()

        composeRule.onNodeWithTag("resident-nav-resident/profile").performClick()
        composeRule.onNodeWithText("Your apartment and contact details.").assertIsDisplayed()
        composeRule.onNodeWithText("Switch workspace").performClick()

        composeRule.onNodeWithText("Choose your workspace").assertIsDisplayed()
    }

    @Test
    fun managerCanAssignAnOpenRequestAndReturnToQueue() {
        composeRule.setContent {
            LocalFixApp(testAppContainer())
        }

        composeRule.onNodeWithText("Property manager").performClick()
        composeRule.onNodeWithText("Property queue").assertIsDisplayed()
        composeRule.onNodeWithText("Bathroom pipe is leaking").performClick()
        composeRule.onNodeWithText("Set final priority").assertIsDisplayed()
        composeRule.onNodeWithTag("manager-priority-soon").performClick()
        composeRule.onNodeWithText("Arun Kumar").performClick()
        composeRule.onNodeWithTag("manager-assign-ticket").performClick()

        composeRule.onNodeWithText("Property queue").assertIsDisplayed()
        composeRule.onNodeWithText("Assigned to Arun Kumar").assertIsDisplayed()
    }

    @Test
    fun workerCanOpenAnAssignedJobAndStartWork() {
        composeRule.setContent {
            LocalFixApp(testAppContainer())
        }

        composeRule.onNodeWithText("Maintenance worker").performClick()
        composeRule.onNodeWithText("My jobs").assertIsDisplayed()
        composeRule.onNodeWithText("Bathroom pipe is leaking").performClick()
        composeRule.onNodeWithText("Morning · 8 AM–12 PM").assertIsDisplayed()
        composeRule.onNodeWithTag("worker-start-job").performClick()

        composeRule.onNodeWithText("Work started").assertIsDisplayed()
        composeRule.onNodeWithText("In progress").assertIsDisplayed()
    }

    private fun testAppContainer(): AppContainer = object : AppContainer {
        override val residentRepository: ResidentRepository = SampleResidentRepository()
        override val managerRepository: ManagerRepository = SampleManagerRepository()
        override val workerRepository: WorkerRepository = SampleWorkerRepository()
        override val requestDraftRepository: RequestDraftRepository =
            InMemoryRequestDraftRepository()
    }
}

package com.localfix.app.ui.manager

import com.localfix.app.data.manager.ManagerPriority
import com.localfix.app.data.manager.SampleManagerRepository
import com.localfix.app.data.model.ServiceCategory
import com.localfix.app.testing.MainDispatcherRule
import com.localfix.app.ui.components.RequestLoadUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ManagerViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun repositoryDataBecomesQueueCountsAndReadableCards() = runTest {
        val viewModel = ManagerViewModel(SampleManagerRepository())
        advanceUntilIdle()

        val queue = viewModel.uiState.value.queue
        assertEquals(1, queue.needsAssignmentCount)
        assertEquals(1, queue.assignedCount)
        assertEquals(RequestLoadUiState.Content, queue.loadState)
        assertEquals("Apartment A-204", queue.tickets.first().unitLabel)
    }

    @Test
    fun openingAPlumbingTicketMarksThePlumberAsTheBestMatch() = runTest {
        val viewModel = ManagerViewModel(SampleManagerRepository())
        advanceUntilIdle()

        viewModel.openTicket(OPEN_TICKET_ID)
        advanceUntilIdle()

        val assignment = viewModel.uiState.value.assignment
        val recommended = assignment.workers.single { it.isRecommended }
        assertEquals(ServiceCategory.PLUMBING, recommended.specialty)
        assertEquals("Arun Kumar", recommended.name)
    }

    @Test
    fun assignButtonNeedsBothAWorkerAndFinalPriority() = runTest {
        val viewModel = ManagerViewModel(SampleManagerRepository())
        advanceUntilIdle()
        viewModel.openTicket(OPEN_TICKET_ID)
        advanceUntilIdle()

        viewModel.selectPriority(ManagerPriority.SOON)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.assignment.canAssign)

        viewModel.selectWorker(PLUMBER_ID)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.assignment.canAssign)
    }

    @Test
    fun successfulAssignmentMovesTheRequestOutOfTheOpenCount() = runTest {
        val viewModel = ManagerViewModel(SampleManagerRepository())
        advanceUntilIdle()
        viewModel.openTicket(OPEN_TICKET_ID)
        advanceUntilIdle()
        viewModel.selectPriority(ManagerPriority.SOON)
        viewModel.selectWorker(PLUMBER_ID)
        advanceUntilIdle()

        viewModel.assignTicket()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.assignment.assignmentCompleted)
        assertEquals(0, viewModel.uiState.value.queue.needsAssignmentCount)
        assertEquals(2, viewModel.uiState.value.queue.assignedCount)
        assertEquals("Arun Kumar", viewModel.uiState.value.queue.tickets.first().assignedWorker)
    }

    private companion object {
        const val OPEN_TICKET_ID = "90000000-0000-0000-0000-000000000001"
        const val PLUMBER_ID = "40000000-0000-0000-0000-000000000001"
    }
}

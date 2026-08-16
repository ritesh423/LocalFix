package com.localfix.app.ui.worker

import com.localfix.app.data.worker.SampleWorkerRepository
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
class WorkerViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun repositoryJobsBecomeWorkerQueueCountsAndCards() = runTest {
        val viewModel = WorkerViewModel(SampleWorkerRepository())
        advanceUntilIdle()

        val queue = viewModel.uiState.value.queue
        assertEquals("Arun Kumar", queue.workerName)
        assertEquals(1, queue.readyCount)
        assertEquals(1, queue.inProgressCount)
        assertEquals(RequestLoadUiState.Content, queue.loadState)
        assertEquals("Apartment A-204", queue.jobs.first().unitLabel)
    }

    @Test
    fun onlyAnAssignedJobCanShowTheStartAction() = runTest {
        val viewModel = WorkerViewModel(SampleWorkerRepository())
        advanceUntilIdle()

        viewModel.openJob(ASSIGNED_JOB_ID)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.detail.job?.canStart == true)

        viewModel.openJob(IN_PROGRESS_JOB_ID)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.detail.job?.canStart == true)
    }

    @Test
    fun startingAJobUpdatesBothDetailAndQueueState() = runTest {
        val viewModel = WorkerViewModel(SampleWorkerRepository())
        advanceUntilIdle()
        viewModel.openJob(ASSIGNED_JOB_ID)
        advanceUntilIdle()

        viewModel.startJob()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.detail.hasJustStarted)
        assertEquals("In progress", viewModel.uiState.value.detail.job?.statusLabel)
        assertFalse(viewModel.uiState.value.detail.job?.canStart == true)
        assertEquals(0, viewModel.uiState.value.queue.readyCount)
        assertEquals(2, viewModel.uiState.value.queue.inProgressCount)
    }

    private companion object {
        const val ASSIGNED_JOB_ID = "90000000-0000-0000-0000-000000000001"
        const val IN_PROGRESS_JOB_ID = "90000000-0000-0000-0000-000000000002"
    }
}

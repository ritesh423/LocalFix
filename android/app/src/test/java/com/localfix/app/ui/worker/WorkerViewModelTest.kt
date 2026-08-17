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

    @Test
    fun emptyCompletionFormShowsRequiredEvidenceErrors() = runTest {
        val viewModel = WorkerViewModel(SampleWorkerRepository())
        advanceUntilIdle()
        viewModel.openJob(IN_PROGRESS_JOB_ID)
        advanceUntilIdle()

        viewModel.submitCompletion()
        advanceUntilIdle()

        val errors = viewModel.uiState.value.detail.completionErrors
        assertEquals(
            "Describe the completed repair in at least 10 characters",
            errors.completionNote,
        )
        assertEquals("Add an after-repair photo", errors.photo)
        assertEquals("In progress", viewModel.uiState.value.detail.job?.statusLabel)
    }

    @Test
    fun validCompletionMovesJobToResidentConfirmation() = runTest {
        val viewModel = WorkerViewModel(SampleWorkerRepository())
        advanceUntilIdle()
        viewModel.openJob(IN_PROGRESS_JOB_ID)
        advanceUntilIdle()
        viewModel.updateCompletionNote("Cleared the drain and tested the water flow.")
        viewModel.updatePartsUsed("Drain seal, cleaning solution")
        viewModel.updateCompletionPhoto("content://localfix/completion/kitchen-sink")
        advanceUntilIdle()

        viewModel.submitCompletion()
        advanceUntilIdle()

        val detail = viewModel.uiState.value.detail
        assertTrue(detail.hasJustSubmittedCompletion)
        assertEquals("Awaiting confirmation", detail.job?.statusLabel)
        assertFalse(detail.job?.canSubmitCompletion == true)
        assertEquals(
            listOf("Drain seal", "cleaning solution"),
            detail.job?.partsUsed,
        )
    }

    @Test
    fun returnedJobShowsTheResidentsReasonAndImmutableActivity() = runTest {
        val viewModel = WorkerViewModel(SampleWorkerRepository())
        advanceUntilIdle()

        viewModel.openJob(ASSIGNED_JOB_ID)
        advanceUntilIdle()

        val detail = viewModel.uiState.value.detail
        assertEquals(
            "The lower pipe joint is still dripping after using the sink.",
            detail.job?.reworkReason,
        )
        assertEquals("Resident requested more work", detail.history.last().title)
        assertEquals(5, detail.history.last().ticketVersion)

        viewModel.startJob()
        advanceUntilIdle()

        assertEquals("Work started", viewModel.uiState.value.detail.history.last().title)
        assertEquals(6, viewModel.uiState.value.detail.history.last().ticketVersion)
    }

    private companion object {
        const val ASSIGNED_JOB_ID = "90000000-0000-0000-0000-000000000001"
        const val IN_PROGRESS_JOB_ID = "90000000-0000-0000-0000-000000000002"
    }
}

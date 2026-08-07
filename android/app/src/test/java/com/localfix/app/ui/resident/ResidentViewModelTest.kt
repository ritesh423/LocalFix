package com.localfix.app.ui.resident

import com.localfix.app.data.draft.InMemoryRequestDraftRepository
import com.localfix.app.data.model.AccessWindow
import com.localfix.app.data.model.SavedRequestDraft
import com.localfix.app.data.model.UrgencySuggestion
import com.localfix.app.data.resident.SampleResidentRepository
import com.localfix.app.data.model.ServiceCategory
import com.localfix.app.data.model.TicketStatus
import com.localfix.app.testing.MainDispatcherRule
import com.localfix.app.ui.requests.RequestFilter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ResidentViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun repositoryDataIsMappedForEveryResidentDestination() = runTest {
        val viewModel = createViewModel()

        advanceUntilIdle()

        assertEquals("Lakeview Residency", viewModel.uiState.value.home.propertyName)
        assertEquals(3, viewModel.uiState.value.requests.requests.size)
        assertEquals("Ritesh", viewModel.uiState.value.profile.name)
    }

    @Test
    fun completedFilterKeepsOnlyCompletedRequests() = runTest {
        val viewModel = createViewModel()

        viewModel.selectRequestFilter(RequestFilter.COMPLETED)
        advanceUntilIdle()

        assertEquals(RequestFilter.COMPLETED, viewModel.uiState.value.requests.selectedFilter)
        assertEquals(
            listOf("LF-0994"),
            viewModel.uiState.value.requests.requests.map { request -> request.id },
        )
    }

    @Test
    fun activeFilterExcludesClosedRequests() = runTest {
        val viewModel = createViewModel()

        viewModel.selectRequestFilter(RequestFilter.ACTIVE)
        advanceUntilIdle()

        assertEquals(
            listOf("LF-1042", "LF-1018"),
            viewModel.uiState.value.requests.requests.map { request -> request.id },
        )
    }

    @Test
    fun emptyDraftShowsRequiredFieldErrors() = runTest {
        val viewModel = createViewModel()

        viewModel.submitRequestDraft()

        assertEquals("Choose a service category", viewModel.createRequestState.value.errors.category)
        assertEquals("Enter at least 5 characters", viewModel.createRequestState.value.errors.title)
        assertEquals(false, viewModel.createRequestState.value.isSubmitting)
    }

    @Test
    fun validDraftCreatesAnOpenRequest() = runTest {
        val repository = SampleResidentRepository()
        val draftRepository = InMemoryRequestDraftRepository()
        val viewModel = ResidentViewModel(repository, draftRepository)

        viewModel.updateDraftCategory(ServiceCategory.PLUMBING)
        viewModel.updateDraftTitle("Water dripping below sink")
        viewModel.updateDraftDescription("Water collects below the kitchen sink after using the tap.")
        viewModel.updateDraftPhoto("content://localfix/photo/kitchen-sink")
        viewModel.submitRequestDraft()
        advanceUntilIdle()

        assertEquals(4, repository.residentData.value.requests.size)
        assertEquals(TicketStatus.OPEN, repository.residentData.value.requests.first().status)
        assertEquals("Water dripping below sink", repository.residentData.value.requests.first().title)
        assertEquals(
            "content://localfix/photo/kitchen-sink",
            repository.residentData.value.requests.first().photoUri,
        )
        assertEquals("LF-1043", viewModel.createRequestState.value.submittedRequestId)
        assertEquals(null, draftRepository.observeDraft().first())
    }

    @Test
    fun savedDraftIsRestoredWhenViewModelStarts() = runTest {
        val savedDraft = SavedRequestDraft(
            category = ServiceCategory.ELECTRICAL,
            title = "Bedroom switch sparks",
            description = "A small spark appears whenever the bedroom switch is used.",
            urgencySuggestion = UrgencySuggestion.SOON,
            accessWindow = AccessWindow.EVENING,
            photoUri = "content://localfix/photo/bedroom-switch",
        )
        val viewModel = ResidentViewModel(
            SampleResidentRepository(),
            InMemoryRequestDraftRepository(savedDraft),
        )

        advanceUntilIdle()

        assertEquals(savedDraft.title, viewModel.createRequestState.value.draft.title)
        assertEquals(savedDraft.category, viewModel.createRequestState.value.draft.category)
        assertEquals(savedDraft.accessWindow, viewModel.createRequestState.value.draft.accessWindow)
        assertEquals(savedDraft.photoUri, viewModel.createRequestState.value.draft.photoUri)
    }

    @Test
    fun editingDraftAutosavesLatestValues() = runTest {
        val draftRepository = InMemoryRequestDraftRepository()
        val viewModel = ResidentViewModel(SampleResidentRepository(), draftRepository)

        viewModel.updateDraftCategory(ServiceCategory.APPLIANCE)
        viewModel.updateDraftTitle("Fridge is making noise")
        advanceUntilIdle()

        val savedDraft = draftRepository.observeDraft().first()
        assertEquals(ServiceCategory.APPLIANCE, savedDraft?.category)
        assertEquals("Fridge is making noise", savedDraft?.title)
    }

    @Test
    fun discardingDraftClearsSavedCopy() = runTest {
        val draftRepository = InMemoryRequestDraftRepository(
            SavedRequestDraft(
                category = ServiceCategory.PLUMBING,
                title = "Leaking tap",
                description = "The bathroom tap continues dripping overnight.",
                urgencySuggestion = UrgencySuggestion.ROUTINE,
                accessWindow = AccessWindow.ANYTIME,
            ),
        )
        val viewModel = ResidentViewModel(SampleResidentRepository(), draftRepository)
        advanceUntilIdle()

        viewModel.discardRequestDraft()
        advanceUntilIdle()

        assertEquals(null, draftRepository.observeDraft().first())
        assertEquals("", viewModel.createRequestState.value.draft.title)
    }

    @Test
    fun removingPhotoUpdatesScreenStateAndSavedDraft() = runTest {
        val draftRepository = InMemoryRequestDraftRepository()
        val viewModel = ResidentViewModel(SampleResidentRepository(), draftRepository)
        viewModel.updateDraftPhoto("content://localfix/photo/leaking-tap")
        advanceUntilIdle()

        viewModel.removeDraftPhoto()
        advanceUntilIdle()

        assertEquals(null, viewModel.createRequestState.value.draft.photoUri)
        assertEquals(null, draftRepository.observeDraft().first()?.photoUri)
    }

    private fun createViewModel(): ResidentViewModel = ResidentViewModel(
        SampleResidentRepository(),
        InMemoryRequestDraftRepository(),
    )
}

package com.localfix.app.ui.resident

import com.localfix.app.data.draft.InMemoryRequestDraftRepository
import com.localfix.app.data.model.AccessWindow
import com.localfix.app.data.model.NewMaintenanceRequest
import com.localfix.app.data.model.MaintenanceRequest
import com.localfix.app.data.model.ResidentData
import com.localfix.app.data.model.RequestDeliveryState
import com.localfix.app.data.model.SavedRequestDraft
import com.localfix.app.data.model.UrgencySuggestion
import com.localfix.app.data.resident.ResidentRepository
import com.localfix.app.data.model.ResidentReviewDecision
import com.localfix.app.data.resident.RequestSyncState
import com.localfix.app.data.resident.SampleResidentRepository
import com.localfix.app.data.model.ServiceCategory
import com.localfix.app.data.model.TicketStatus
import com.localfix.app.testing.MainDispatcherRule
import com.localfix.app.ui.components.RequestLoadUiState
import com.localfix.app.ui.requests.RequestFilter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
        assertEquals(
            "Morning · 8 AM–12 PM",
            viewModel.uiState.value.requestDetails["LF-1042"]?.accessWindowLabel,
        )
        assertEquals(
            "The tap keeps dripping even when fully closed.",
            viewModel.uiState.value.requestDetails["LF-1042"]?.description,
        )
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
        assertEquals(
            "content://localfix/photo/kitchen-sink",
            viewModel.uiState.value.requestDetails["LF-1043"]?.photoUri,
        )
        assertEquals(null, draftRepository.observeDraft().first())
    }

    @Test
    fun savedDraftIsRestoredWhenViewModelStarts() = runTest {
        val savedDraft = SavedRequestDraft(
            clientRequestId = "50000000-0000-0000-0000-000000000002",
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
                clientRequestId = "50000000-0000-0000-0000-000000000003",
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

    @Test
    fun initialRequestLoadHasADedicatedLoadingState() = runTest {
        val repository = FixedStateResidentRepository(
            data = SampleResidentRepository().residentData.value.copy(requests = emptyList()),
            syncState = RequestSyncState.InitialLoading,
        )
        val viewModel = ResidentViewModel(repository, InMemoryRequestDraftRepository())
        advanceUntilIdle()

        assertEquals(RequestLoadUiState.Loading, viewModel.uiState.value.home.requestLoadState)
        assertEquals(RequestLoadUiState.Loading, viewModel.uiState.value.requests.requestLoadState)
    }

    @Test
    fun successfulEmptyResponseIsDifferentFromLoading() = runTest {
        val repository = FixedStateResidentRepository(
            data = SampleResidentRepository().residentData.value.copy(requests = emptyList()),
            syncState = RequestSyncState.Ready,
        )
        val viewModel = ResidentViewModel(repository, InMemoryRequestDraftRepository())
        advanceUntilIdle()

        assertEquals(RequestLoadUiState.Empty, viewModel.uiState.value.home.requestLoadState)
        assertEquals(RequestLoadUiState.Empty, viewModel.uiState.value.requests.requestLoadState)
    }

    @Test
    fun firstLoadFailureIsDifferentFromStaleData() = runTest {
        val accountData = SampleResidentRepository().residentData.value
        val failedRepository = FixedStateResidentRepository(
            data = accountData.copy(requests = emptyList()),
            syncState = RequestSyncState.Error("Server unavailable", hasPreviousResult = false),
        )
        val staleRepository = FixedStateResidentRepository(
            data = accountData,
            syncState = RequestSyncState.Error("Server unavailable", hasPreviousResult = true),
        )

        val failedViewModel = ResidentViewModel(
            failedRepository,
            InMemoryRequestDraftRepository(),
        )
        val staleViewModel = ResidentViewModel(
            staleRepository,
            InMemoryRequestDraftRepository(),
        )
        advanceUntilIdle()

        assertEquals(
            RequestLoadUiState.Failed("Server unavailable"),
            failedViewModel.uiState.value.requests.requestLoadState,
        )
        assertEquals(
            RequestLoadUiState.Stale("Server unavailable"),
            staleViewModel.uiState.value.requests.requestLoadState,
        )
        assertEquals(3, staleViewModel.uiState.value.requests.requests.size)
    }

    @Test
    fun offlineQueuedRequestRemainsVisibleWithAnHonestStatus() = runTest {
        val accountData = SampleResidentRepository().residentData.value
        val localRequest = MaintenanceRequest(
            id = "50000000-0000-0000-0000-000000000004",
            title = "Kitchen tap is leaking",
            description = "Water continues dripping after the tap is fully closed.",
            category = ServiceCategory.PLUMBING,
            status = TicketStatus.OPEN,
            urgencySuggestion = UrgencySuggestion.SOON,
            accessWindow = AccessWindow.MORNING,
            assignedWorker = "Will be available after the request is sent",
            updatedLabel = "Saved on this device",
            deliveryState = RequestDeliveryState.PENDING,
        )
        val repository = FixedStateResidentRepository(
            data = accountData.copy(requests = listOf(localRequest)),
            syncState = RequestSyncState.Error("Server unavailable", hasPreviousResult = false),
        )

        val viewModel = ResidentViewModel(repository, InMemoryRequestDraftRepository())
        advanceUntilIdle()

        val request = viewModel.uiState.value.requests.requests.single()
        assertEquals("Waiting to send", request.statusLabel)
        assertEquals("LOCAL-50000000", request.reference)
        assertEquals(
            RequestLoadUiState.Stale("Server unavailable"),
            viewModel.uiState.value.requests.requestLoadState,
        )
        assertEquals(
            "Waiting to send",
            viewModel.uiState.value.requestDetails[localRequest.id]?.delivery?.title,
        )
        assertEquals(
            false,
            viewModel.uiState.value.requestDetails[localRequest.id]?.delivery?.canRetry,
        )
    }

    @Test
    fun failedLocalRequestCanBeRetriedFromItsDetailPage() = runTest {
        val failedRequest = failedLocalRequest()
        val repository = FixedStateResidentRepository(
            data = SampleResidentRepository().residentData.value.copy(
                requests = listOf(failedRequest),
            ),
            syncState = RequestSyncState.Ready,
        )
        val viewModel = ResidentViewModel(repository, InMemoryRequestDraftRepository())
        viewModel.openRequest(failedRequest.id)
        advanceUntilIdle()

        assertEquals(
            true,
            viewModel.uiState.value.requestDetails[failedRequest.id]?.delivery?.canRetry,
        )
        viewModel.retryFailedRequest(failedRequest.id)
        advanceUntilIdle()

        assertEquals(failedRequest.id, repository.lastRetriedRequestId)
        assertEquals(
            "Waiting to send",
            viewModel.uiState.value.requestDetails[failedRequest.id]?.delivery?.title,
        )
    }

    @Test
    fun discardingFailedLocalRequestEmitsNavigationEvent() = runTest {
        val failedRequest = failedLocalRequest()
        val repository = FixedStateResidentRepository(
            data = SampleResidentRepository().residentData.value.copy(
                requests = listOf(failedRequest),
            ),
            syncState = RequestSyncState.Ready,
        )
        val viewModel = ResidentViewModel(repository, InMemoryRequestDraftRepository())
        viewModel.openRequest(failedRequest.id)
        advanceUntilIdle()

        viewModel.discardFailedRequest(failedRequest.id)
        advanceUntilIdle()

        assertEquals(failedRequest.id, repository.lastDiscardedRequestId)
        assertEquals(failedRequest.id, viewModel.discardedRequestId.value)
        assertEquals(null, viewModel.uiState.value.requestDetails[failedRequest.id])
    }

    @Test
    fun confirmingARepairRequiresARatingAndCompletesTheRequest() = runTest {
        val repository = SampleResidentRepository()
        val viewModel = ResidentViewModel(repository, InMemoryRequestDraftRepository())
        viewModel.openRequest("LF-1018")
        viewModel.selectReviewDecision(ResidentReviewDecision.CONFIRM)

        viewModel.submitReview()
        advanceUntilIdle()

        assertEquals(
            "Choose a rating from 1 to 5",
            viewModel.uiState.value.requestDetails["LF-1018"]?.review?.ratingError,
        )

        viewModel.selectReviewRating(5)
        viewModel.updateReviewFeedback("The switch works safely now.")
        viewModel.submitReview()
        advanceUntilIdle()

        val request = repository.residentData.value.requests.single { it.id == "LF-1018" }
        assertEquals(TicketStatus.COMPLETED, request.status)
        assertEquals(5, request.residentRating)
        assertEquals(false, viewModel.uiState.value.requestDetails["LF-1018"]?.canReview)
    }

    @Test
    fun requestingMoreWorkRequiresAUsefulReasonAndReturnsTicketToWorker() = runTest {
        val repository = SampleResidentRepository()
        val viewModel = ResidentViewModel(repository, InMemoryRequestDraftRepository())
        viewModel.openRequest("LF-1018")
        viewModel.selectReviewDecision(ResidentReviewDecision.REQUEST_REWORK)
        viewModel.updateReviewFeedback("Still bad")

        viewModel.submitReview()
        advanceUntilIdle()

        assertEquals(
            "Describe what still needs attention in at least 10 characters",
            viewModel.uiState.value.requestDetails["LF-1018"]?.review?.feedbackError,
        )

        viewModel.updateReviewFeedback("The switch still sparks when pressed.")
        viewModel.submitReview()
        advanceUntilIdle()

        val request = repository.residentData.value.requests.single { it.id == "LF-1018" }
        assertEquals(TicketStatus.ASSIGNED, request.status)
        assertEquals("The switch still sparks when pressed.", request.residentFeedback)
    }

    @Test
    fun queuedReviewIsShownAsSavedAndCannotBeSubmittedTwice() = runTest {
        val queuedReview = SampleResidentRepository().residentData.value.requests
            .single { it.id == "LF-1018" }
            .copy(
                residentRating = 5,
                residentFeedback = "The switch works safely now.",
                reviewDeliveryState = RequestDeliveryState.PENDING,
                pendingReviewDecision = ResidentReviewDecision.CONFIRM,
            )
        val repository = FixedStateResidentRepository(
            data = SampleResidentRepository().residentData.value.copy(
                requests = listOf(queuedReview),
            ),
            syncState = RequestSyncState.Ready,
        )
        val viewModel = ResidentViewModel(repository, InMemoryRequestDraftRepository())

        viewModel.openRequest(queuedReview.id)
        advanceUntilIdle()

        val detail = viewModel.uiState.value.requestDetails.getValue(queuedReview.id)
        assertEquals(false, detail.canReview)
        assertEquals("Review waiting to send", detail.reviewDelivery?.title)
        assertEquals(5, detail.residentRating)
        assertEquals(0, viewModel.uiState.value.home.awaitingConfirmationCount)
    }

    @Test
    fun failedReviewRestoresTheResidentsChoicesForCorrection() = runTest {
        val failedReview = SampleResidentRepository().residentData.value.requests
            .single { it.id == "LF-1018" }
            .copy(
                residentFeedback = "The switch still sparks when pressed.",
                reviewDeliveryState = RequestDeliveryState.FAILED,
                pendingReviewDecision = ResidentReviewDecision.REQUEST_REWORK,
                reviewFailureMessage = "This review wasn't sent.",
            )
        val repository = FixedStateResidentRepository(
            data = SampleResidentRepository().residentData.value.copy(
                requests = listOf(failedReview),
            ),
            syncState = RequestSyncState.Ready,
        )
        val viewModel = ResidentViewModel(repository, InMemoryRequestDraftRepository())

        viewModel.openRequest(failedReview.id)
        advanceUntilIdle()

        val detail = viewModel.uiState.value.requestDetails.getValue(failedReview.id)
        assertEquals(true, detail.canReview)
        assertEquals("Review wasn't sent", detail.reviewDelivery?.title)
        assertEquals(ResidentReviewDecision.REQUEST_REWORK, detail.review.selectedDecision)
        assertEquals("The switch still sparks when pressed.", detail.review.feedback)
        assertEquals(1, viewModel.uiState.value.home.awaitingConfirmationCount)
    }

    private fun createViewModel(): ResidentViewModel = ResidentViewModel(
        SampleResidentRepository(),
        InMemoryRequestDraftRepository(),
    )

    private fun failedLocalRequest() = MaintenanceRequest(
        id = "50000000-0000-0000-0000-000000000004",
        title = "Kitchen tap is leaking",
        description = "Water continues dripping after the tap is fully closed.",
        category = ServiceCategory.PLUMBING,
        status = TicketStatus.OPEN,
        urgencySuggestion = UrgencySuggestion.SOON,
        accessWindow = AccessWindow.MORNING,
        assignedWorker = "This request wasn't sent. Its details are still saved.",
        updatedLabel = "Not sent",
        deliveryState = RequestDeliveryState.FAILED,
    )

    private class FixedStateResidentRepository(
        data: ResidentData,
        syncState: RequestSyncState,
    ) : ResidentRepository {
        private val mutableResidentData = MutableStateFlow(data)
        override val residentData: StateFlow<ResidentData> = mutableResidentData
        override val requestSyncState: StateFlow<RequestSyncState> = MutableStateFlow(syncState)
        var lastRetriedRequestId: String? = null
        var lastDiscardedRequestId: String? = null

        override suspend fun createRequest(request: NewMaintenanceRequest): String =
            error("Not needed in this test")

        override suspend fun reviewRequest(
            ticketId: String,
            expectedVersion: Int,
            decision: ResidentReviewDecision,
            rating: Int?,
            feedback: String?,
        ) = error("Not needed in this test")

        override suspend fun refreshRequests() = Unit

        override suspend fun retryFailedRequest(clientRequestId: String) {
            lastRetriedRequestId = clientRequestId
            mutableResidentData.value = mutableResidentData.value.copy(
                requests = mutableResidentData.value.requests.map { request ->
                    if (request.id == clientRequestId) {
                        request.copy(deliveryState = RequestDeliveryState.PENDING)
                    } else {
                        request
                    }
                },
            )
        }

        override suspend fun discardFailedRequest(clientRequestId: String) {
            lastDiscardedRequestId = clientRequestId
            mutableResidentData.value = mutableResidentData.value.copy(
                requests = mutableResidentData.value.requests.filterNot {
                    it.id == clientRequestId
                },
            )
        }
    }
}

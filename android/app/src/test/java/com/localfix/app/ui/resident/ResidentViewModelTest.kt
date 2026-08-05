package com.localfix.app.ui.resident

import com.localfix.app.data.resident.SampleResidentRepository
import com.localfix.app.testing.MainDispatcherRule
import com.localfix.app.ui.requests.RequestFilter
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
        val viewModel = ResidentViewModel(SampleResidentRepository())

        advanceUntilIdle()

        assertEquals("Lakeview Residency", viewModel.uiState.value.home.propertyName)
        assertEquals(3, viewModel.uiState.value.requests.requests.size)
        assertEquals("Ritesh", viewModel.uiState.value.profile.name)
    }

    @Test
    fun completedFilterKeepsOnlyCompletedRequests() = runTest {
        val viewModel = ResidentViewModel(SampleResidentRepository())

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
        val viewModel = ResidentViewModel(SampleResidentRepository())

        viewModel.selectRequestFilter(RequestFilter.ACTIVE)
        advanceUntilIdle()

        assertEquals(
            listOf("LF-1042", "LF-1018"),
            viewModel.uiState.value.requests.requests.map { request -> request.id },
        )
    }
}

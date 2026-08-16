package com.localfix.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.localfix.app.data.worker.WorkerRepository
import com.localfix.app.ui.worker.WorkerJobDetailScreen
import com.localfix.app.ui.worker.WorkerQueueScreen
import com.localfix.app.ui.worker.WorkerViewModel

@Composable
fun WorkerNavigation(
    repository: WorkerRepository,
    onSwitchRole: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val workerViewModel: WorkerViewModel = viewModel(
        factory = WorkerViewModel.factory(repository),
    )
    val uiState by workerViewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = WORKER_QUEUE_ROUTE,
        modifier = modifier,
    ) {
        composable(WORKER_QUEUE_ROUTE) {
            WorkerQueueScreen(
                uiState = uiState.queue,
                onJobClick = { ticketId ->
                    workerViewModel.openJob(ticketId)
                    navController.navigate(workerJobRoute(ticketId))
                },
                onRetry = workerViewModel::refresh,
                onSwitchRole = onSwitchRole,
            )
        }
        composable(
            route = WORKER_JOB_ROUTE,
            arguments = listOf(
                navArgument(WORKER_TICKET_ID_ARGUMENT) { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val ticketId = backStackEntry.arguments?.getString(WORKER_TICKET_ID_ARGUMENT)
            LaunchedEffect(ticketId) {
                if (ticketId != null && uiState.detail.job?.id != ticketId) {
                    workerViewModel.openJob(ticketId)
                }
            }
            WorkerJobDetailScreen(
                uiState = uiState.detail,
                onBack = { navController.popBackStack() },
                onStartJob = workerViewModel::startJob,
                onCompletionNoteChanged = workerViewModel::updateCompletionNote,
                onPartsUsedChanged = workerViewModel::updatePartsUsed,
                onPhotoSelected = workerViewModel::updateCompletionPhoto,
                onPhotoRemoved = workerViewModel::removeCompletionPhoto,
                onPhotoSelectionFailed = workerViewModel::reportCompletionPhotoFailure,
                onSubmitCompletion = workerViewModel::submitCompletion,
            )
        }
    }
}

private const val WORKER_QUEUE_ROUTE = "worker/queue"
private const val WORKER_TICKET_ID_ARGUMENT = "ticketId"
private const val WORKER_JOB_ROUTE = "worker/tickets/{$WORKER_TICKET_ID_ARGUMENT}"

private fun workerJobRoute(ticketId: String): String = "worker/tickets/$ticketId"

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
import com.localfix.app.data.manager.ManagerRepository
import com.localfix.app.ui.manager.ManagerAssignmentScreen
import com.localfix.app.ui.manager.ManagerQueueScreen
import com.localfix.app.ui.manager.ManagerResidentInviteScreen
import com.localfix.app.ui.manager.ManagerViewModel

@Composable
fun ManagerNavigation(
    repository: ManagerRepository,
    propertyName: String? = null,
    onSwitchRole: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val managerViewModel: ManagerViewModel = viewModel(
        factory = ManagerViewModel.factory(repository),
    )
    val uiState by managerViewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    LaunchedEffect(uiState.assignment.assignmentCompleted) {
        if (uiState.assignment.assignmentCompleted) {
            managerViewModel.consumeAssignmentCompleted()
            navController.popBackStack()
        }
    }

    NavHost(
        navController = navController,
        startDestination = MANAGER_QUEUE_ROUTE,
        modifier = modifier,
    ) {
        composable(MANAGER_QUEUE_ROUTE) {
            ManagerQueueScreen(
                uiState = propertyName?.let { name ->
                    uiState.queue.copy(propertyName = name)
                } ?: uiState.queue,
                onTicketClick = { ticketId ->
                    managerViewModel.openTicket(ticketId)
                    navController.navigate(managerTicketRoute(ticketId))
                },
                onRetry = managerViewModel::refresh,
                onInviteResident = {
                    managerViewModel.openResidentInvite()
                    navController.navigate(MANAGER_RESIDENT_INVITE_ROUTE)
                },
                onSwitchRole = onSwitchRole,
            )
        }
        composable(MANAGER_RESIDENT_INVITE_ROUTE) {
            ManagerResidentInviteScreen(
                uiState = uiState.residentInvite,
                onUnitSelected = managerViewModel::selectInviteUnit,
                onCreateInvite = managerViewModel::createResidentInvite,
                onBack = {
                    managerViewModel.closeResidentInvite()
                    navController.popBackStack()
                },
            )
        }
        composable(
            route = MANAGER_TICKET_ROUTE,
            arguments = listOf(
                navArgument(MANAGER_TICKET_ID_ARGUMENT) { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val ticketId = backStackEntry.arguments?.getString(MANAGER_TICKET_ID_ARGUMENT)
            LaunchedEffect(ticketId) {
                if (ticketId != null && uiState.assignment.ticket?.id != ticketId) {
                    managerViewModel.openTicket(ticketId)
                }
            }
            ManagerAssignmentScreen(
                uiState = uiState.assignment,
                onBack = { navController.popBackStack() },
                onPrioritySelected = managerViewModel::selectPriority,
                onWorkerSelected = managerViewModel::selectWorker,
                onAssign = managerViewModel::assignTicket,
            )
        }
    }
}

private const val MANAGER_QUEUE_ROUTE = "manager/queue"
private const val MANAGER_RESIDENT_INVITE_ROUTE = "manager/resident-invite"
private const val MANAGER_TICKET_ID_ARGUMENT = "ticketId"
private const val MANAGER_TICKET_ROUTE = "manager/tickets/{$MANAGER_TICKET_ID_ARGUMENT}"

private fun managerTicketRoute(ticketId: String): String = "manager/tickets/$ticketId"

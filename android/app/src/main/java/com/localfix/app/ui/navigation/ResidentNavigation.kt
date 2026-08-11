package com.localfix.app.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.localfix.app.data.draft.RequestDraftRepository
import com.localfix.app.data.resident.ResidentRepository
import com.localfix.app.data.model.ServiceCategory
import com.localfix.app.ui.create.CreateRequestScreen
import com.localfix.app.ui.home.ResidentHomeScreen
import com.localfix.app.ui.home.ServiceCategoryType
import com.localfix.app.ui.profile.ResidentProfileScreen
import com.localfix.app.ui.requestdetail.ResidentRequestDetailScreen
import com.localfix.app.ui.requests.ResidentRequestsScreen
import com.localfix.app.ui.resident.ResidentViewModel

@Composable
fun ResidentNavigation(
    repository: ResidentRepository,
    requestDraftRepository: RequestDraftRepository,
    onSwitchRole: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val residentViewModel: ResidentViewModel = viewModel(
        factory = ResidentViewModel.factory(repository, requestDraftRepository),
    )
    val uiState by residentViewModel.uiState.collectAsStateWithLifecycle()
    val createRequestState by residentViewModel.createRequestState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination

    LaunchedEffect(createRequestState.submittedRequestId) {
        if (createRequestState.submittedRequestId != null) {
            residentViewModel.consumeRequestSubmission()
            navController.navigate(ResidentDestination.REQUESTS.route) {
                popUpTo(CREATE_REQUEST_ROUTE) {
                    inclusive = true
                }
                launchSingleTop = true
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (currentDestination?.route in MAIN_RESIDENT_ROUTES) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    ResidentDestination.entries.forEach { destination ->
                        NavigationBarItem(
                            modifier = Modifier.testTag("resident-nav-${destination.route}"),
                            selected = currentDestination?.hierarchy?.any {
                                it.route == destination.route
                            } == true,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = null,
                                )
                            },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = ResidentDestination.HOME.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(ResidentDestination.HOME.route) {
                ResidentHomeScreen(
                    uiState = uiState.home,
                    onReportIssue = {
                        navController.navigate(CREATE_REQUEST_ROUTE)
                    },
                    onRequestClick = { requestId ->
                        navController.navigate(requestDetailRoute(requestId))
                    },
                    onCategoryClick = { category ->
                        residentViewModel.startRequestDraft(category.toDataCategory())
                        navController.navigate(CREATE_REQUEST_ROUTE)
                    },
                )
            }
            composable(ResidentDestination.REQUESTS.route) {
                ResidentRequestsScreen(
                    uiState = uiState.requests,
                    onFilterSelected = residentViewModel::selectRequestFilter,
                    onReportIssue = {
                        navController.navigate(CREATE_REQUEST_ROUTE)
                    },
                    onRequestClick = { requestId ->
                        navController.navigate(requestDetailRoute(requestId))
                    },
                )
            }
            composable(ResidentDestination.PROFILE.route) {
                ResidentProfileScreen(
                    uiState = uiState.profile,
                    onSwitchRole = onSwitchRole,
                )
            }
            composable(CREATE_REQUEST_ROUTE) {
                CreateRequestScreen(
                    uiState = createRequestState,
                    onBack = { navController.popBackStack() },
                    onDiscard = {
                        residentViewModel.discardRequestDraft()
                        navController.popBackStack()
                    },
                    onCategorySelected = residentViewModel::updateDraftCategory,
                    onTitleChanged = residentViewModel::updateDraftTitle,
                    onDescriptionChanged = residentViewModel::updateDraftDescription,
                    onUrgencySelected = residentViewModel::updateDraftUrgency,
                    onAccessWindowSelected = residentViewModel::updateDraftAccessWindow,
                    onPhotoSelected = residentViewModel::updateDraftPhoto,
                    onPhotoRemoved = residentViewModel::removeDraftPhoto,
                    onPhotoSelectionFailed = residentViewModel::reportPhotoSelectionFailure,
                    onSubmit = residentViewModel::submitRequestDraft,
                )
            }
            composable(
                route = REQUEST_DETAIL_ROUTE,
                arguments = listOf(
                    navArgument(REQUEST_ID_ARGUMENT) { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val requestId = backStackEntry.arguments?.getString(REQUEST_ID_ARGUMENT)
                ResidentRequestDetailScreen(
                    uiState = requestId?.let(uiState.requestDetails::get),
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

private const val CREATE_REQUEST_ROUTE = "resident/requests/new"
private const val REQUEST_ID_ARGUMENT = "requestId"
private const val REQUEST_DETAIL_ROUTE = "resident/requests/{$REQUEST_ID_ARGUMENT}"

private fun requestDetailRoute(requestId: String): String = "resident/requests/$requestId"

private enum class ResidentDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    HOME("resident/home", "Home", Icons.Outlined.Home),
    REQUESTS("resident/requests", "Requests", Icons.AutoMirrored.Outlined.ReceiptLong),
    PROFILE("resident/profile", "Profile", Icons.Outlined.PersonOutline),
}

private val MAIN_RESIDENT_ROUTES = ResidentDestination.entries.map { destination ->
    destination.route
}.toSet()

private fun ServiceCategoryType.toDataCategory(): ServiceCategory = when (this) {
    ServiceCategoryType.PLUMBING -> ServiceCategory.PLUMBING
    ServiceCategoryType.ELECTRICAL -> ServiceCategory.ELECTRICAL
    ServiceCategoryType.APPLIANCE -> ServiceCategory.APPLIANCE
    ServiceCategoryType.OTHER -> ServiceCategory.OTHER
}

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.localfix.app.data.resident.ResidentRepository
import com.localfix.app.ui.home.ResidentHomeScreen
import com.localfix.app.ui.profile.ResidentProfileScreen
import com.localfix.app.ui.requests.ResidentRequestsScreen
import com.localfix.app.ui.resident.ResidentViewModel

@Composable
fun ResidentNavigation(
    repository: ResidentRepository,
    onSwitchRole: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val residentViewModel: ResidentViewModel = viewModel(
        factory = ResidentViewModel.factory(repository),
    )
    val uiState by residentViewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
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
                        navController.navigate(ResidentDestination.REQUESTS.route)
                    },
                    onRequestClick = {
                        navController.navigate(ResidentDestination.REQUESTS.route)
                    },
                    onCategoryClick = {
                        navController.navigate(ResidentDestination.REQUESTS.route)
                    },
                )
            }
            composable(ResidentDestination.REQUESTS.route) {
                ResidentRequestsScreen(
                    uiState = uiState.requests,
                    onFilterSelected = residentViewModel::selectRequestFilter,
                    onReportIssue = {},
                    onRequestClick = {},
                )
            }
            composable(ResidentDestination.PROFILE.route) {
                ResidentProfileScreen(
                    uiState = uiState.profile,
                    onSwitchRole = onSwitchRole,
                )
            }
        }
    }
}

private enum class ResidentDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    HOME("resident/home", "Home", Icons.Outlined.Home),
    REQUESTS("resident/requests", "Requests", Icons.AutoMirrored.Outlined.ReceiptLong),
    PROFILE("resident/profile", "Profile", Icons.Outlined.PersonOutline),
}

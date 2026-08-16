package com.localfix.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.localfix.app.data.AppContainer
import com.localfix.app.ui.navigation.ManagerNavigation
import com.localfix.app.ui.navigation.ResidentNavigation
import com.localfix.app.ui.navigation.WorkerNavigation
import com.localfix.app.ui.role.RoleSelectionScreen
import com.localfix.app.ui.session.AppRole
import com.localfix.app.ui.theme.LocalFixTheme

@Composable
fun LocalFixApp(appContainer: AppContainer) {
    var activeRoleName by rememberSaveable { mutableStateOf<String?>(null) }
    val activeRole = activeRoleName?.let(AppRole::valueOf)

    LocalFixTheme {
        when (activeRole) {
            null -> RoleSelectionScreen(
                onRoleSelected = { role -> activeRoleName = role.name },
            )
            AppRole.RESIDENT -> ResidentNavigation(
                repository = appContainer.residentRepository,
                requestDraftRepository = appContainer.requestDraftRepository,
                onSwitchRole = { activeRoleName = null },
            )
            AppRole.MANAGER -> ManagerNavigation(
                repository = appContainer.managerRepository,
                onSwitchRole = { activeRoleName = null },
            )
            AppRole.WORKER -> WorkerNavigation(
                repository = appContainer.workerRepository,
                onSwitchRole = { activeRoleName = null },
            )
        }
    }
}

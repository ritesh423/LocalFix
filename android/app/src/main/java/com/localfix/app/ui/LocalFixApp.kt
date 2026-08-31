package com.localfix.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.localfix.app.data.AppContainer
import com.localfix.app.data.auth.AuthSession
import com.localfix.app.data.notifications.PushRole
import com.localfix.app.ui.auth.AuthStatus
import com.localfix.app.ui.auth.AuthViewModel
import com.localfix.app.ui.auth.CheckingSessionScreen
import com.localfix.app.ui.auth.JoinWorkspaceScreen
import com.localfix.app.ui.auth.SignInScreen
import com.localfix.app.ui.auth.VerifyEmailScreen
import com.localfix.app.ui.auth.WorkspaceAccessScreen
import com.localfix.app.ui.navigation.ManagerNavigation
import com.localfix.app.ui.navigation.ResidentNavigation
import com.localfix.app.ui.navigation.WorkerNavigation
import com.localfix.app.ui.role.RoleSelectionScreen
import com.localfix.app.ui.session.AppRole
import com.localfix.app.ui.theme.LocalFixTheme

@Composable
fun LocalFixApp(
    appContainer: AppContainer,
    onRequestNotificationPermission: () -> Unit = {},
) {
    LocalFixTheme {
        val authRepository = appContainer.authRepository
        val authSessionApi = appContainer.authSessionApi
        if (authRepository == null || authSessionApi == null) {
            DemoWorkspaceContent(
                appContainer = appContainer,
                onRequestNotificationPermission = onRequestNotificationPermission,
            )
        } else {
            val authViewModel: AuthViewModel = viewModel(
                factory = AuthViewModel.factory(authRepository, authSessionApi),
            )
            val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
            when (authUiState.status) {
                AuthStatus.CHECKING_SESSION -> CheckingSessionScreen()
                AuthStatus.SIGNED_OUT,
                AuthStatus.SIGNING_IN,
                AuthStatus.SIGNING_UP,
                -> SignInScreen(
                    uiState = authUiState,
                    onEmailChange = authViewModel::updateEmail,
                    onPasswordChange = authViewModel::updatePassword,
                    onConfirmPasswordChange = authViewModel::updateConfirmPassword,
                    onInviteCodeChange = authViewModel::updateInviteCode,
                    onSignIn = authViewModel::signIn,
                    onCreateAccount = authViewModel::createAccount,
                    onShowSignIn = authViewModel::showSignIn,
                    onShowCreateAccount = authViewModel::showCreateAccount,
                )
                AuthStatus.VERIFY_EMAIL -> VerifyEmailScreen(
                    email = authUiState.email,
                    message = authUiState.message,
                    onCheckVerification = authViewModel::confirmEmailVerified,
                    onSignOut = authViewModel::signOut,
                )
                AuthStatus.NO_WORKSPACE,
                AuthStatus.JOINING_WORKSPACE,
                -> JoinWorkspaceScreen(
                    inviteCode = authUiState.inviteCode,
                    inviteCodeError = authUiState.inviteCodeError,
                    message = authUiState.message,
                    isJoining = authUiState.status == AuthStatus.JOINING_WORKSPACE,
                    onInviteCodeChange = authViewModel::updateInviteCode,
                    onJoin = authViewModel::joinWorkspace,
                    onSignOut = authViewModel::signOut,
                )
                AuthStatus.SESSION_ERROR -> WorkspaceAccessScreen(
                    message = authUiState.message
                        ?: "LocalFix couldn't load your workspace.",
                    onRetry = authViewModel::refreshSession,
                    onSignOut = authViewModel::signOut,
                )
                AuthStatus.AUTHENTICATED -> AuthenticatedWorkspaceContent(
                    session = requireNotNull(authUiState.session),
                    appContainer = appContainer,
                    onRequestNotificationPermission = onRequestNotificationPermission,
                    onSignOut = authViewModel::signOut,
                )
            }
        }
    }
}

@Composable
private fun DemoWorkspaceContent(
    appContainer: AppContainer,
    onRequestNotificationPermission: () -> Unit,
) {
    var activeRoleName by rememberSaveable { mutableStateOf<String?>(null) }
    WorkspaceContent(
        activeRole = activeRoleName?.let(AppRole::valueOf),
        availableRoles = AppRole.entries,
        appContainer = appContainer,
        onRoleSelected = { role ->
            activeRoleName = role.name
            appContainer.pushRegistrationManager?.activate(PushRole.valueOf(role.name))
            onRequestNotificationPermission()
        },
        onSwitchRole = { activeRoleName = null },
    )
}

@Composable
private fun AuthenticatedWorkspaceContent(
    session: AuthSession,
    appContainer: AppContainer,
    onRequestNotificationPermission: () -> Unit,
    onSignOut: () -> Unit,
) {
    val availableRoles = session.memberships.mapNotNull { membership ->
        runCatching { AppRole.valueOf(membership.role.uppercase()) }.getOrNull()
    }.distinct()
    if (availableRoles.isEmpty()) {
        WorkspaceAccessScreen(
            message = "This account does not have a supported LocalFix role.",
            onRetry = {},
            onSignOut = onSignOut,
        )
        return
    }

    var selectedRoleName by rememberSaveable(session.user.firebaseUid) {
        mutableStateOf<String?>(null)
    }
    val activeRole = if (availableRoles.size == 1) {
        availableRoles.first()
    } else {
        selectedRoleName?.let { name ->
            availableRoles.find { role -> role.name == name }
        }
    }

    LaunchedEffect(activeRole) {
        activeRole?.let { role ->
            appContainer.pushRegistrationManager?.activate(PushRole.valueOf(role.name))
            onRequestNotificationPermission()
        }
    }

    WorkspaceContent(
        activeRole = activeRole,
        availableRoles = availableRoles,
        appContainer = appContainer,
        onRoleSelected = { selectedRoleName = it.name },
        onSwitchRole = {
            if (availableRoles.size == 1) onSignOut() else selectedRoleName = null
        },
    )
}

@Composable
private fun WorkspaceContent(
    activeRole: AppRole?,
    availableRoles: List<AppRole>,
    appContainer: AppContainer,
    onRoleSelected: (AppRole) -> Unit,
    onSwitchRole: () -> Unit,
) {
    when (activeRole) {
        null -> RoleSelectionScreen(
            roles = availableRoles,
            onRoleSelected = onRoleSelected,
        )
        AppRole.RESIDENT -> ResidentNavigation(
            repository = appContainer.residentRepository,
            requestDraftRepository = appContainer.requestDraftRepository,
            onSwitchRole = onSwitchRole,
        )
        AppRole.MANAGER -> ManagerNavigation(
            repository = appContainer.managerRepository,
            onSwitchRole = onSwitchRole,
        )
        AppRole.WORKER -> WorkerNavigation(
            repository = appContainer.workerRepository,
            onSwitchRole = onSwitchRole,
        )
    }
}

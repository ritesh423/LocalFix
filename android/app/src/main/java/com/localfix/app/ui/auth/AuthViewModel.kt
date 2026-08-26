package com.localfix.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.localfix.app.data.auth.AuthRepository
import com.localfix.app.data.remote.AuthSessionApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val authSessionApi: AuthSessionApi,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(AuthUiState())
    val uiState = mutableUiState.asStateFlow()

    init {
        if (authRepository.currentUser == null) {
            mutableUiState.update { it.copy(status = AuthStatus.SIGNED_OUT) }
        } else {
            refreshSession()
        }
    }

    fun updateEmail(email: String) {
        mutableUiState.update {
            it.copy(email = email.trimStart(), emailError = null, message = null)
        }
    }

    fun updatePassword(password: String) {
        mutableUiState.update {
            it.copy(password = password, passwordError = null, message = null)
        }
    }

    fun signIn() {
        val current = mutableUiState.value
        val email = current.email.trim()
        val emailError = if (!email.contains('@') || email.endsWith('@')) {
            "Enter a valid email address"
        } else {
            null
        }
        val passwordError = if (current.password.isBlank()) {
            "Enter your password"
        } else {
            null
        }
        if (emailError != null || passwordError != null) {
            mutableUiState.update {
                it.copy(emailError = emailError, passwordError = passwordError)
            }
            return
        }

        mutableUiState.update {
            it.copy(
                status = AuthStatus.SIGNING_IN,
                email = email,
                emailError = null,
                passwordError = null,
                message = null,
            )
        }
        viewModelScope.launch {
            runCatching { authRepository.signIn(email, current.password) }
                .onSuccess { loadWorkspaceSession() }
                .onFailure {
                    mutableUiState.update {
                        it.copy(
                            status = AuthStatus.SIGNED_OUT,
                            password = "",
                            message = "That email or password didn't work. Try again.",
                        )
                    }
                }
        }
    }

    fun refreshSession() {
        mutableUiState.update {
            it.copy(status = AuthStatus.CHECKING_SESSION, message = null)
        }
        viewModelScope.launch { loadWorkspaceSession() }
    }

    fun signOut() {
        authRepository.signOut()
        mutableUiState.value = AuthUiState(
            status = AuthStatus.SIGNED_OUT,
            email = mutableUiState.value.email,
        )
    }

    private suspend fun loadWorkspaceSession() {
        runCatching { authSessionApi.getAuthSession() }
            .onSuccess { session ->
                mutableUiState.update {
                    it.copy(
                        status = if (session.memberships.isEmpty()) {
                            AuthStatus.NO_WORKSPACE
                        } else {
                            AuthStatus.AUTHENTICATED
                        },
                        password = "",
                        session = session,
                        message = null,
                    )
                }
            }
            .onFailure {
                mutableUiState.update {
                    it.copy(
                        status = AuthStatus.SESSION_ERROR,
                        password = "",
                        message = "You're signed in, but LocalFix couldn't load your workspace.",
                    )
                }
            }
    }

    companion object {
        fun factory(
            authRepository: AuthRepository,
            authSessionApi: AuthSessionApi,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { AuthViewModel(authRepository, authSessionApi) }
        }
    }
}

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

    fun updateConfirmPassword(password: String) {
        mutableUiState.update {
            it.copy(
                confirmPassword = password,
                confirmPasswordError = null,
                message = null,
            )
        }
    }

    fun updateInviteCode(inviteCode: String) {
        mutableUiState.update {
            it.copy(
                inviteCode = inviteCode.uppercase(),
                inviteCodeError = null,
                message = null,
            )
        }
    }

    fun showSignIn() {
        changeMode(AuthMode.SIGN_IN)
    }

    fun showCreateAccount() {
        changeMode(AuthMode.CREATE_ACCOUNT)
    }

    fun showPasswordReset() {
        mutableUiState.update {
            it.copy(
                status = AuthStatus.RESET_PASSWORD,
                mode = AuthMode.SIGN_IN,
                password = "",
                emailError = null,
                passwordError = null,
                message = null,
            )
        }
    }

    fun returnToSignIn() {
        mutableUiState.update {
            it.copy(
                status = AuthStatus.SIGNED_OUT,
                mode = AuthMode.SIGN_IN,
                password = "",
                emailError = null,
                passwordError = null,
                message = null,
            )
        }
    }

    fun sendPasswordReset() {
        val email = mutableUiState.value.email.trim()
        val emailError = validateEmail(email)
        if (emailError != null) {
            mutableUiState.update { it.copy(emailError = emailError) }
            return
        }
        mutableUiState.update {
            it.copy(
                status = AuthStatus.SENDING_PASSWORD_RESET,
                email = email,
                emailError = null,
                message = null,
            )
        }
        viewModelScope.launch {
            runCatching { authRepository.sendPasswordReset(email) }
                .onSuccess {
                    mutableUiState.update {
                        it.copy(
                            status = AuthStatus.RESET_PASSWORD,
                            message = "If an account exists for $email, Firebase has sent a password reset link.",
                        )
                    }
                }
                .onFailure {
                    mutableUiState.update {
                        it.copy(
                            status = AuthStatus.RESET_PASSWORD,
                            message = "We couldn't send the reset link. Check your connection and try again.",
                        )
                    }
                }
        }
    }

    fun signIn() {
        val current = mutableUiState.value
        val email = current.email.trim()
        val emailError = validateEmail(email)
        val passwordError = validateSignInPassword(current.password)
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

    fun createAccount() {
        val current = mutableUiState.value
        val email = current.email.trim()
        val emailError = validateEmail(email)
        val passwordError = if (current.password.length < 6) {
            "Use at least 6 characters"
        } else {
            null
        }
        val confirmPasswordError = if (current.confirmPassword != current.password) {
            "Passwords do not match"
        } else {
            null
        }
        val inviteCodeError = validateInviteCode(current.inviteCode)
        if (
            emailError != null ||
            passwordError != null ||
            confirmPasswordError != null ||
            inviteCodeError != null
        ) {
            mutableUiState.update {
                it.copy(
                    emailError = emailError,
                    passwordError = passwordError,
                    confirmPasswordError = confirmPasswordError,
                    inviteCodeError = inviteCodeError,
                )
            }
            return
        }

        mutableUiState.update {
            it.copy(
                status = AuthStatus.SIGNING_UP,
                email = email,
                emailError = null,
                passwordError = null,
                confirmPasswordError = null,
                inviteCodeError = null,
                message = null,
            )
        }
        viewModelScope.launch {
            runCatching { authRepository.createAccount(email, current.password) }
                .onSuccess {
                    mutableUiState.update {
                        it.copy(
                            status = AuthStatus.VERIFY_EMAIL,
                            password = "",
                            confirmPassword = "",
                            message = "We sent a verification link to $email.",
                        )
                    }
                }
                .onFailure {
                    mutableUiState.update {
                        it.copy(
                            status = AuthStatus.SIGNED_OUT,
                            password = "",
                            confirmPassword = "",
                            message = "We couldn't create that account. The email may already be in use.",
                        )
                    }
                }
        }
    }

    fun confirmEmailVerified() {
        mutableUiState.update {
            it.copy(status = AuthStatus.CHECKING_SESSION, message = null)
        }
        viewModelScope.launch {
            runCatching { authRepository.refreshCurrentUser() }
                .onSuccess { user ->
                    if (user?.emailVerified == true) {
                        if (mutableUiState.value.inviteCode.isBlank()) {
                            mutableUiState.update {
                                it.copy(status = AuthStatus.NO_WORKSPACE)
                            }
                        } else {
                            redeemInvite()
                        }
                    } else {
                        mutableUiState.update {
                            it.copy(
                                status = AuthStatus.VERIFY_EMAIL,
                                message = "That email is not verified yet. Open the link, then try again.",
                            )
                        }
                    }
                }
                .onFailure {
                    mutableUiState.update {
                        it.copy(
                            status = AuthStatus.VERIFY_EMAIL,
                            message = "We couldn't check your email verification. Try again.",
                        )
                    }
                }
        }
    }

    fun resendEmailVerification() {
        if (mutableUiState.value.isResendingVerification) return
        mutableUiState.update {
            it.copy(isResendingVerification = true, message = null)
        }
        viewModelScope.launch {
            runCatching { authRepository.resendEmailVerification() }
                .onSuccess {
                    mutableUiState.update {
                        it.copy(
                            isResendingVerification = false,
                            message = "A new verification email was sent to ${it.email}.",
                        )
                    }
                }
                .onFailure {
                    mutableUiState.update {
                        it.copy(
                            isResendingVerification = false,
                            message = "We couldn't resend the email. Wait a moment and try again.",
                        )
                    }
                }
        }
    }

    fun joinWorkspace() {
        val inviteCodeError = validateInviteCode(mutableUiState.value.inviteCode)
        if (inviteCodeError != null) {
            mutableUiState.update { it.copy(inviteCodeError = inviteCodeError) }
            return
        }
        mutableUiState.update {
            it.copy(
                status = AuthStatus.JOINING_WORKSPACE,
                inviteCodeError = null,
                message = null,
            )
        }
        viewModelScope.launch { redeemInvite() }
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
                        status = when {
                            session.memberships.isNotEmpty() -> AuthStatus.AUTHENTICATED
                            authRepository.currentUser?.emailVerified == false -> {
                                AuthStatus.VERIFY_EMAIL
                            }
                            else -> AuthStatus.NO_WORKSPACE
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

    private suspend fun redeemInvite() {
        val inviteCode = mutableUiState.value.inviteCode.trim()
        runCatching { authSessionApi.redeemResidentInvite(inviteCode) }
            .onSuccess { loadWorkspaceSession() }
            .onFailure {
                mutableUiState.update {
                    it.copy(
                        status = AuthStatus.NO_WORKSPACE,
                        message = "That invite could not be used. Check the code or ask your manager for a new one.",
                    )
                }
            }
    }

    private fun changeMode(mode: AuthMode) {
        mutableUiState.update {
            it.copy(
                mode = mode,
                password = "",
                confirmPassword = "",
                emailError = null,
                passwordError = null,
                confirmPasswordError = null,
                inviteCodeError = null,
                message = null,
            )
        }
    }

    private fun validateEmail(email: String): String? =
        if (!email.contains('@') || email.endsWith('@')) {
            "Enter a valid email address"
        } else {
            null
        }

    private fun validateSignInPassword(password: String): String? =
        if (password.isBlank()) "Enter your password" else null

    private fun validateInviteCode(inviteCode: String): String? =
        if (inviteCode.count(Char::isLetterOrDigit) < 8) {
            "Enter the invite code from your apartment manager"
        } else {
            null
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

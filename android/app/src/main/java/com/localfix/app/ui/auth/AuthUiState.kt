package com.localfix.app.ui.auth

import com.localfix.app.data.auth.AuthSession

enum class AuthStatus {
    CHECKING_SESSION,
    SIGNED_OUT,
    SIGNING_IN,
    AUTHENTICATED,
    NO_WORKSPACE,
    SESSION_ERROR,
}

data class AuthUiState(
    val status: AuthStatus = AuthStatus.CHECKING_SESSION,
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val message: String? = null,
    val session: AuthSession? = null,
)

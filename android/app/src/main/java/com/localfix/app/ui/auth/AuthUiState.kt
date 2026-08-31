package com.localfix.app.ui.auth

import com.localfix.app.data.auth.AuthSession

enum class AuthStatus {
    CHECKING_SESSION,
    SIGNED_OUT,
    SIGNING_IN,
    SIGNING_UP,
    VERIFY_EMAIL,
    JOINING_WORKSPACE,
    AUTHENTICATED,
    NO_WORKSPACE,
    SESSION_ERROR,
}


enum class AuthMode {
    SIGN_IN,
    CREATE_ACCOUNT,
}

data class AuthUiState(
    val status: AuthStatus = AuthStatus.CHECKING_SESSION,
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val inviteCode: String = "",
    val mode: AuthMode = AuthMode.SIGN_IN,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val inviteCodeError: String? = null,
    val message: String? = null,
    val session: AuthSession? = null,
)

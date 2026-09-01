package com.localfix.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.localfix.app.ui.theme.LocalFixRadius
import com.localfix.app.ui.theme.LocalFixSpacing

@Composable
fun SignInScreen(
    uiState: AuthUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onInviteCodeChange: (String) -> Unit,
    onSignIn: () -> Unit,
    onCreateAccount: () -> Unit,
    onShowSignIn: () -> Unit,
    onShowCreateAccount: () -> Unit,
    onForgotPassword: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val isCreateAccount = uiState.mode == AuthMode.CREATE_ACCOUNT
    val isBusy = uiState.status in setOf(
        AuthStatus.SIGNING_IN,
        AuthStatus.SIGNING_UP,
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding(),
    ) {
        SignInHeader(isCreateAccount = isCreateAccount)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LocalFixSpacing.large),
        ) {
            Text(
                text = if (isCreateAccount) "Create your account" else "Welcome back",
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.height(LocalFixSpacing.small))
            Text(
                text = if (isCreateAccount) {
                    "Use the invite given to you by your apartment manager."
                } else {
                    "Use the account linked to your apartment workspace."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.height(LocalFixSpacing.extraLarge))
            OutlinedTextField(
                value = uiState.email,
                onValueChange = onEmailChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sign-in-email"),
                enabled = !isBusy,
                label = { Text("Email") },
                singleLine = true,
                isError = uiState.emailError != null,
                supportingText = uiState.emailError?.let { message ->
                    { Text(message) }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
            )
            Spacer(modifier = Modifier.height(LocalFixSpacing.medium))
            OutlinedTextField(
                value = uiState.password,
                onValueChange = onPasswordChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sign-in-password"),
                enabled = !isBusy,
                label = { Text("Password") },
                singleLine = true,
                isError = uiState.passwordError != null,
                supportingText = uiState.passwordError?.let { message ->
                    { Text(message) }
                },
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) {
                                Icons.Outlined.VisibilityOff
                            } else {
                                Icons.Outlined.Visibility
                            },
                            contentDescription = if (passwordVisible) {
                                "Hide password"
                            } else {
                                "Show password"
                            },
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = if (isCreateAccount) ImeAction.Next else ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { if (!isCreateAccount) onSignIn() },
                ),
            )
            if (isCreateAccount) {
                Spacer(modifier = Modifier.height(LocalFixSpacing.medium))
                OutlinedTextField(
                    value = uiState.confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sign-up-confirm-password"),
                    enabled = !isBusy,
                    label = { Text("Confirm password") },
                    singleLine = true,
                    isError = uiState.confirmPasswordError != null,
                    supportingText = uiState.confirmPasswordError?.let { message ->
                        { Text(message) }
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next,
                    ),
                )
                Spacer(modifier = Modifier.height(LocalFixSpacing.medium))
                OutlinedTextField(
                    value = uiState.inviteCode,
                    onValueChange = onInviteCodeChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sign-up-invite-code"),
                    enabled = !isBusy,
                    label = { Text("Apartment invite code") },
                    placeholder = { Text("LF-XXXX-XXXX-XXXX") },
                    singleLine = true,
                    isError = uiState.inviteCodeError != null,
                    supportingText = uiState.inviteCodeError?.let { message ->
                        { Text(message) }
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { onCreateAccount() }),
                )
            }
            uiState.message?.let { message ->
                Spacer(modifier = Modifier.height(LocalFixSpacing.medium))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(LocalFixRadius.medium),
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(LocalFixSpacing.medium),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Spacer(modifier = Modifier.height(LocalFixSpacing.large))
            Button(
                onClick = if (isCreateAccount) onCreateAccount else onSignIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("sign-in-submit"),
                enabled = !isBusy,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
                shape = RoundedCornerShape(LocalFixRadius.medium),
            ) {
                if (isBusy) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        if (isCreateAccount) "Create account" else "Sign in",
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(modifier = Modifier.height(LocalFixSpacing.medium))
            if (!isCreateAccount) {
                TextButton(
                    onClick = onForgotPassword,
                    modifier = Modifier.align(Alignment.End),
                    enabled = !isBusy,
                ) {
                    Text("Forgot password?")
                }
            }
            TextButton(
                onClick = if (isCreateAccount) onShowSignIn else onShowCreateAccount,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                enabled = !isBusy,
            ) {
                Text(
                    if (isCreateAccount) {
                        "Already have an account? Sign in"
                    } else {
                        "New resident? Create an account"
                    }
                )
            }
        }
    }
}

@Composable
private fun SignInHeader(isCreateAccount: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(
                    bottomStart = LocalFixRadius.extraLarge,
                    bottomEnd = LocalFixRadius.extraLarge,
                ),
            )
            .statusBarsPadding()
            .padding(LocalFixSpacing.large),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(LocalFixSpacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondary,
                shape = RoundedCornerShape(LocalFixRadius.medium),
            ) {
                Text(
                    text = "LF",
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                    color = MaterialTheme.colorScheme.onSecondary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Text(
                text = "LocalFix",
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.headlineSmall,
            )
        }
        Spacer(modifier = Modifier.height(LocalFixSpacing.extraLarge))
        Text(
            text = if (isCreateAccount) {
                "Join the right apartment."
            } else {
                "Your building, one sign-in."
            },
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(LocalFixSpacing.small))
        Text(
            text = if (isCreateAccount) {
                "Your invite securely connects you to your home."
            } else {
                "Open the right workspace without choosing a role you don't have."
            },
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.74f),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
fun VerifyEmailScreen(
    email: String,
    message: String?,
    onCheckVerification: () -> Unit,
    onResendVerification: () -> Unit,
    isResending: Boolean,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OnboardingActionScreen(
        title = "Verify your email",
        description = "We sent a verification link to $email. Open it, then return here.",
        message = message,
        primaryLabel = "I've verified my email",
        onPrimary = onCheckVerification,
        secondaryLabel = if (isResending) {
            "Sending another email…"
        } else {
            "Send another verification email"
        },
        onSecondary = onResendVerification,
        secondaryEnabled = !isResending,
        onSignOut = onSignOut,
        modifier = modifier,
    )
}

@Composable
fun PasswordResetScreen(
    email: String,
    emailError: String?,
    message: String?,
    isSending: Boolean,
    onEmailChange: (String) -> Unit,
    onSendReset: () -> Unit,
    onBackToSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .padding(LocalFixSpacing.large),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Reset your password", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(LocalFixSpacing.small))
        Text(
            "Firebase will email you a secure link for choosing a new password.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(LocalFixSpacing.large))
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            modifier = Modifier.fillMaxWidth().testTag("password-reset-email"),
            enabled = !isSending,
            label = { Text("Email") },
            singleLine = true,
            isError = emailError != null,
            supportingText = emailError?.let { error -> { Text(error) } },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onSendReset() }),
        )
        message?.let {
            Spacer(modifier = Modifier.height(LocalFixSpacing.medium))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(modifier = Modifier.height(LocalFixSpacing.large))
        Button(
            onClick = onSendReset,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = !isSending,
        ) {
            if (isSending) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
            } else {
                Text("Send reset link")
            }
        }
        TextButton(
            onClick = onBackToSignIn,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            enabled = !isSending,
        ) {
            Text("Back to sign in")
        }
    }
}

@Composable
fun JoinWorkspaceScreen(
    inviteCode: String,
    inviteCodeError: String?,
    message: String?,
    isJoining: Boolean,
    onInviteCodeChange: (String) -> Unit,
    onJoin: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .padding(LocalFixSpacing.large),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Join your apartment", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(LocalFixSpacing.small))
        Text(
            "Enter the invite code provided by your apartment manager.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(LocalFixSpacing.large))
        OutlinedTextField(
            value = inviteCode,
            onValueChange = onInviteCodeChange,
            modifier = Modifier.fillMaxWidth().testTag("join-invite-code"),
            enabled = !isJoining,
            label = { Text("Apartment invite code") },
            placeholder = { Text("LF-XXXX-XXXX-XXXX") },
            isError = inviteCodeError != null,
            supportingText = inviteCodeError?.let { error -> { Text(error) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onJoin() }),
        )
        message?.let {
            Spacer(modifier = Modifier.height(LocalFixSpacing.medium))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(modifier = Modifier.height(LocalFixSpacing.large))
        Button(
            onClick = onJoin,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = !isJoining,
        ) {
            if (isJoining) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
            } else {
                Text("Join apartment")
            }
        }
        TextButton(
            onClick = onSignOut,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            enabled = !isJoining,
        ) {
            Text("Use another account")
        }
    }
}

@Composable
private fun OnboardingActionScreen(
    title: String,
    description: String,
    message: String?,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
    secondaryEnabled: Boolean = true,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(LocalFixSpacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(LocalFixSpacing.small))
        Text(
            description,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )
        message?.let {
            Spacer(modifier = Modifier.height(LocalFixSpacing.medium))
            Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(LocalFixSpacing.large))
        Button(onClick = onPrimary, modifier = Modifier.fillMaxWidth()) {
            Text(primaryLabel)
        }
        if (secondaryLabel != null && onSecondary != null) {
            Spacer(modifier = Modifier.height(LocalFixSpacing.extraSmall))
            TextButton(
                onClick = onSecondary,
                enabled = secondaryEnabled,
            ) {
                Text(secondaryLabel)
            }
        }
        Spacer(modifier = Modifier.height(LocalFixSpacing.small))
        TextButton(onClick = onSignOut) {
            Text("Use another account")
        }
    }
}

@Composable
fun WorkspaceAccessScreen(
    message: String,
    onRetry: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(LocalFixSpacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Workspace unavailable",
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(modifier = Modifier.height(LocalFixSpacing.small))
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(LocalFixSpacing.large))
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
            Text("Try again")
        }
        Spacer(modifier = Modifier.height(LocalFixSpacing.small))
        Button(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
            Text("Sign out")
        }
    }
}

@Composable
fun CheckingSessionScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
        Spacer(modifier = Modifier.height(LocalFixSpacing.medium))
        Text("Opening your LocalFix workspace")
    }
}

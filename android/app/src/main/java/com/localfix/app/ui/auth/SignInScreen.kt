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
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val isSigningIn = uiState.status == AuthStatus.SIGNING_IN

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding(),
    ) {
        SignInHeader()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LocalFixSpacing.large),
        ) {
            Text(
                text = "Welcome back",
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.height(LocalFixSpacing.small))
            Text(
                text = "Use the account linked to your apartment workspace.",
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
                enabled = !isSigningIn,
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
                enabled = !isSigningIn,
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
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onSignIn() }),
            )
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
                onClick = onSignIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("sign-in-submit"),
                enabled = !isSigningIn,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
                shape = RoundedCornerShape(LocalFixRadius.medium),
            ) {
                if (isSigningIn) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Sign in", fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(modifier = Modifier.height(LocalFixSpacing.medium))
            Text(
                text = "Accounts are provided by your apartment manager.",
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun SignInHeader() {
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
            text = "Your building, one sign-in.",
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(LocalFixSpacing.small))
        Text(
            text = "Open the right workspace without choosing a role you don't have.",
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.74f),
            style = MaterialTheme.typography.bodyLarge,
        )
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

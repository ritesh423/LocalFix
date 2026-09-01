package com.localfix.app.ui.auth

import com.localfix.app.data.auth.AuthRepository
import com.localfix.app.data.auth.AuthenticatedUser
import com.localfix.app.data.auth.AuthSession
import com.localfix.app.data.auth.WorkspaceMembership
import com.localfix.app.data.remote.AuthSessionApi
import com.localfix.app.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun validCredentialsLoadTheServerControlledWorkspace() = runTest {
        val authRepository = FakeAuthRepository()
        val viewModel = AuthViewModel(
            authRepository = authRepository,
            authSessionApi = FakeAuthSessionApi(residentSession()),
        )
        viewModel.updateEmail("resident@example.com")
        viewModel.updatePassword("safe-password")

        viewModel.signIn()
        advanceUntilIdle()

        assertEquals(AuthStatus.AUTHENTICATED, viewModel.uiState.value.status)
        assertEquals("resident", viewModel.uiState.value.session?.memberships?.single()?.role)
        assertEquals("resident@example.com", authRepository.lastEmail)
        assertEquals("", viewModel.uiState.value.password)
    }

    @Test
    fun invalidFormNeverSendsCredentialsToFirebase() = runTest {
        val authRepository = FakeAuthRepository()
        val viewModel = AuthViewModel(
            authRepository = authRepository,
            authSessionApi = FakeAuthSessionApi(residentSession()),
        )
        viewModel.updateEmail("not-an-email")

        viewModel.signIn()
        advanceUntilIdle()

        assertEquals("Enter a valid email address", viewModel.uiState.value.emailError)
        assertEquals("Enter your password", viewModel.uiState.value.passwordError)
        assertEquals(0, authRepository.signInCount)
    }

    @Test
    fun firebaseUserWithoutMembershipGetsNoWorkspaceState() = runTest {
        val authRepository = FakeAuthRepository()
        val viewModel = AuthViewModel(
            authRepository = authRepository,
            authSessionApi = FakeAuthSessionApi(
                residentSession().copy(memberships = emptyList()),
            ),
        )
        viewModel.updateEmail("resident@example.com")
        viewModel.updatePassword("safe-password")

        viewModel.signIn()
        advanceUntilIdle()

        assertEquals(AuthStatus.NO_WORKSPACE, viewModel.uiState.value.status)
    }

    @Test
    fun newResidentAccountWaitsForEmailVerification() = runTest {
        val authRepository = FakeAuthRepository()
        val viewModel = AuthViewModel(
            authRepository = authRepository,
            authSessionApi = FakeAuthSessionApi(residentSession()),
        )
        viewModel.showCreateAccount()
        viewModel.updateEmail("new-resident@example.com")
        viewModel.updatePassword("safe-password")
        viewModel.updateConfirmPassword("safe-password")
        viewModel.updateInviteCode("LF-2345-6789-ABCD")

        viewModel.createAccount()
        advanceUntilIdle()

        assertEquals(AuthStatus.VERIFY_EMAIL, viewModel.uiState.value.status)
        assertEquals(1, authRepository.createAccountCount)
        assertEquals("", viewModel.uiState.value.password)
    }

    @Test
    fun passwordResetUsesFirebaseWithoutRevealingWhetherAccountExists() = runTest {
        val authRepository = FakeAuthRepository()
        val viewModel = AuthViewModel(
            authRepository = authRepository,
            authSessionApi = FakeAuthSessionApi(residentSession()),
        )
        viewModel.showPasswordReset()
        viewModel.updateEmail("resident@example.com")

        viewModel.sendPasswordReset()
        advanceUntilIdle()

        assertEquals(AuthStatus.RESET_PASSWORD, viewModel.uiState.value.status)
        assertEquals(
            listOf("resident@example.com"),
            authRepository.passwordResetEmails,
        )
        assertEquals(
            true,
            viewModel.uiState.value.message?.startsWith("If an account exists"),
        )
    }

    @Test
    fun unverifiedResidentCanRequestAnotherVerificationEmail() = runTest {
        val authRepository = FakeAuthRepository()
        val viewModel = AuthViewModel(
            authRepository = authRepository,
            authSessionApi = FakeAuthSessionApi(residentSession()),
        )
        viewModel.showCreateAccount()
        viewModel.updateEmail("new-resident@example.com")
        viewModel.updatePassword("safe-password")
        viewModel.updateConfirmPassword("safe-password")
        viewModel.updateInviteCode("LF-2345-6789-ABCD")
        viewModel.createAccount()
        advanceUntilIdle()

        viewModel.resendEmailVerification()
        advanceUntilIdle()

        assertEquals(1, authRepository.verificationResendCount)
        assertEquals(false, viewModel.uiState.value.isResendingVerification)
        assertEquals(
            "A new verification email was sent to new-resident@example.com.",
            viewModel.uiState.value.message,
        )
    }

    private class FakeAuthRepository : AuthRepository {
        override var currentUser: AuthenticatedUser? = null
        var lastEmail: String? = null
        var signInCount = 0
        var createAccountCount = 0
        val passwordResetEmails = mutableListOf<String>()
        var verificationResendCount = 0

        override suspend fun signIn(email: String, password: String) {
            signInCount += 1
            lastEmail = email
            currentUser = AuthenticatedUser(
                firebaseUid = "firebase-resident-123",
                email = email,
                displayName = "Ritesh",
                emailVerified = true,
            )
        }

        override suspend fun createAccount(email: String, password: String) {
            createAccountCount += 1
            currentUser = AuthenticatedUser(
                firebaseUid = "new-firebase-resident",
                email = email,
                displayName = null,
                emailVerified = false,
            )
        }

        override suspend fun refreshCurrentUser(): AuthenticatedUser? = currentUser

        override suspend fun sendPasswordReset(email: String) {
            passwordResetEmails += email
        }

        override suspend fun resendEmailVerification() {
            check(currentUser != null)
            verificationResendCount += 1
        }

        override fun signOut() {
            currentUser = null
        }

        override suspend fun getIdToken(): String? = "signed-firebase-token"
    }

    private class FakeAuthSessionApi(
        private val session: AuthSession,
    ) : AuthSessionApi {
        override suspend fun getAuthSession(): AuthSession = session

        override suspend fun redeemResidentInvite(
            inviteCode: String,
        ): WorkspaceMembership = session.memberships.first()
    }

    private fun residentSession() = AuthSession(
        user = AuthenticatedUser(
            firebaseUid = "firebase-resident-123",
            email = "resident@example.com",
            displayName = "Ritesh",
            emailVerified = true,
        ),
        memberships = listOf(
            WorkspaceMembership(
                propertyId = "20000000-0000-0000-0000-000000000001",
                propertyName = "Lakeview Residency",
                userId = "10000000-0000-0000-0000-000000000101",
                role = "resident",
                unitId = "30000000-0000-0000-0000-000000000204",
                unitLabel = "Apartment A-204",
            ),
        ),
    )
}

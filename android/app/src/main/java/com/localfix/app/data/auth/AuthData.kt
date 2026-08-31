package com.localfix.app.data.auth

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

data class AuthenticatedUser(
    val firebaseUid: String,
    val email: String?,
    val displayName: String?,
    val emailVerified: Boolean,
)

data class WorkspaceMembership(
    val propertyId: String,
    val propertyName: String?,
    val userId: String,
    val role: String,
    val unitId: String?,
    val unitLabel: String?,
)

data class AuthSession(
    val user: AuthenticatedUser,
    val memberships: List<WorkspaceMembership>,
)

fun interface AuthTokenProvider {
    suspend fun getIdToken(): String?
}

interface AuthRepository : AuthTokenProvider {
    val currentUser: AuthenticatedUser?

    suspend fun signIn(email: String, password: String)

    suspend fun createAccount(email: String, password: String)

    suspend fun refreshCurrentUser(): AuthenticatedUser?

    fun signOut()
}

class FirebaseAuthRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
) : AuthRepository {
    override val currentUser: AuthenticatedUser?
        get() = firebaseAuth.currentUser?.let { user ->
            AuthenticatedUser(
                firebaseUid = user.uid,
                email = user.email,
                displayName = user.displayName,
                emailVerified = user.isEmailVerified,
            )
        }

    override suspend fun signIn(email: String, password: String) {
        firebaseAuth.signInWithEmailAndPassword(email, password).await()
    }

    override suspend fun createAccount(email: String, password: String) {
        val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        result.user?.sendEmailVerification()?.await()
    }

    override suspend fun refreshCurrentUser(): AuthenticatedUser? {
        firebaseAuth.currentUser?.reload()?.await()
        firebaseAuth.currentUser?.getIdToken(true)?.await()
        return currentUser
    }

    override fun signOut() {
        firebaseAuth.signOut()
    }

    override suspend fun getIdToken(): String? =
        firebaseAuth.currentUser?.getIdToken(false)?.await()?.token
}

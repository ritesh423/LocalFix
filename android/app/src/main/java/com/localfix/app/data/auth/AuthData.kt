package com.localfix.app.data.auth

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

data class AuthenticatedUser(
    val firebaseUid: String,
    val email: String?,
    val displayName: String?,
)

data class WorkspaceMembership(
    val propertyId: String,
    val userId: String,
    val role: String,
    val unitId: String?,
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
            )
        }

    override suspend fun signIn(email: String, password: String) {
        firebaseAuth.signInWithEmailAndPassword(email, password).await()
    }

    override fun signOut() {
        firebaseAuth.signOut()
    }

    override suspend fun getIdToken(): String? =
        firebaseAuth.currentUser?.getIdToken(false)?.await()?.token
}

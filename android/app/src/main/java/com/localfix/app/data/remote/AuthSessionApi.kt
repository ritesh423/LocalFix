package com.localfix.app.data.remote

import com.localfix.app.data.auth.AuthenticatedUser
import com.localfix.app.data.auth.AuthSession
import com.localfix.app.data.auth.WorkspaceMembership
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface AuthSessionApi {
    suspend fun getAuthSession(): AuthSession
}

@Serializable
data class AuthSessionResponse(
    val user: AuthenticatedUserResponse,
    val memberships: List<WorkspaceMembershipResponse>,
) {
    fun toDomain(): AuthSession = AuthSession(
        user = user.toDomain(),
        memberships = memberships.map(WorkspaceMembershipResponse::toDomain),
    )
}

@Serializable
data class AuthenticatedUserResponse(
    @SerialName("firebase_uid") val firebaseUid: String,
    val email: String? = null,
    @SerialName("display_name") val displayName: String? = null,
) {
    fun toDomain(): AuthenticatedUser = AuthenticatedUser(
        firebaseUid = firebaseUid,
        email = email,
        displayName = displayName,
    )
}

@Serializable
data class WorkspaceMembershipResponse(
    @SerialName("property_id") val propertyId: String,
    @SerialName("user_id") val userId: String,
    val role: String,
    @SerialName("unit_id") val unitId: String? = null,
) {
    fun toDomain(): WorkspaceMembership = WorkspaceMembership(
        propertyId = propertyId,
        userId = userId,
        role = role,
        unitId = unitId,
    )
}

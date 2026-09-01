package com.localfix.app.data.remote

import com.localfix.app.data.auth.AuthenticatedUser
import com.localfix.app.data.auth.AuthSession
import com.localfix.app.data.auth.WorkspaceMembership
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface AuthSessionApi {
    suspend fun getAuthSession(): AuthSession

    suspend fun redeemInvite(inviteCode: String): WorkspaceMembership
}

@Serializable
data class InviteRedemptionPayload(
    @SerialName("invite_code") val inviteCode: String,
)

@Serializable
data class InviteRedemptionResponse(
    val membership: WorkspaceMembershipResponse,
)

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
    @SerialName("email_verified") val emailVerified: Boolean = false,
) {
    fun toDomain(): AuthenticatedUser = AuthenticatedUser(
        firebaseUid = firebaseUid,
        email = email,
        displayName = displayName,
        emailVerified = emailVerified,
    )
}

@Serializable
data class WorkspaceMembershipResponse(
    @SerialName("property_id") val propertyId: String,
    @SerialName("property_name") val propertyName: String? = null,
    @SerialName("user_id") val userId: String,
    val role: String,
    @SerialName("unit_id") val unitId: String? = null,
    @SerialName("unit_label") val unitLabel: String? = null,
) {
    fun toDomain(): WorkspaceMembership = WorkspaceMembership(
        propertyId = propertyId,
        propertyName = propertyName,
        userId = userId,
        role = role,
        unitId = unitId,
        unitLabel = unitLabel,
    )
}

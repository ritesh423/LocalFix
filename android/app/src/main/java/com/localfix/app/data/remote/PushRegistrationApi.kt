package com.localfix.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface PushRegistrationApi {
    suspend fun registerPushDevice(
        role: String,
        request: PushRegistrationPayload,
    ): PushRegistrationResponse
}

@Serializable
data class PushRegistrationPayload(
    @SerialName("installation_id") val installationId: String,
    @SerialName("firebase_installation_id")
    val firebaseInstallationId: String,
    val platform: String = "android",
)

@Serializable
data class PushRegistrationResponse(
    @SerialName("installation_id") val installationId: String,
    val platform: String,
    val role: String,
)

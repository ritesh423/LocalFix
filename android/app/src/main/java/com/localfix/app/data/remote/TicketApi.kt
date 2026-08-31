package com.localfix.app.data.remote

import android.content.ContentResolver
import android.net.Uri
import com.localfix.app.data.auth.AuthTokenProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

interface TicketApi {
    suspend fun createTicket(request: TicketCreatePayload): TicketResponse

    suspend fun listTickets(): List<TicketResponse>

    suspend fun reviewTicket(
        ticketId: String,
        request: TicketReviewPayload,
    ): TicketResponse

    fun completionPhotoUrl(ticketId: String): String? = null
}

interface ManagerTicketApi {
    suspend fun listManagerTickets(): List<TicketResponse>

    suspend fun listManagerWorkers(): List<WorkerResponse>

    suspend fun getManagerSummary(): ManagerSummaryResponse

    suspend fun listManagerUnits(): List<ManagerPropertyUnitResponse> = emptyList()

    suspend fun createManagerResidentInvite(
        request: ManagerResidentInviteCreatePayload,
    ): ManagerResidentInviteResponse = error("Resident invites are not supported.")

    suspend fun assignTicket(
        ticketId: String,
        request: TicketAssignmentPayload,
    ): TicketResponse
}

interface WorkerTicketApi {
    suspend fun listWorkerTickets(): List<TicketResponse>

    suspend fun listWorkerTicketEvents(ticketId: String): List<TicketEventResponse>

    suspend fun startTicket(
        ticketId: String,
        request: TicketStartPayload,
    ): TicketResponse

    suspend fun submitCompletion(
        ticketId: String,
        request: TicketCompletionPayload,
    ): TicketResponse
}

@Serializable
data class TicketCreatePayload(
    @SerialName("client_request_id") val clientRequestId: String,
    val title: String,
    val description: String,
    val category: String,
    @SerialName("urgency_suggestion") val urgencySuggestion: String,
    @SerialName("access_window") val accessWindow: String,
)

@Serializable
data class TicketReviewPayload(
    @SerialName("expected_version") val expectedVersion: Int,
    val decision: String,
    val rating: Int? = null,
    val feedback: String? = null,
)

@Serializable
data class TicketAssignmentPayload(
    @SerialName("expected_version") val expectedVersion: Int,
    val priority: String,
    @SerialName("worker_id") val workerId: String,
)

@Serializable
data class TicketStartPayload(
    @SerialName("expected_version") val expectedVersion: Int,
)

data class TicketCompletionPayload(
    val expectedVersion: Int,
    val completionNote: String,
    val partsUsed: List<String>,
    val photoUri: String,
)

@Serializable
data class WorkerResponse(
    val id: String,
    val name: String,
    val specialty: String,
)

@Serializable
data class ManagerSummaryResponse(
    @SerialName("total_requests") val totalRequests: Int,
    @SerialName("active_requests") val activeRequests: Int,
    @SerialName("needs_assignment") val needsAssignment: Int,
    val assigned: Int,
    @SerialName("in_progress") val inProgress: Int,
    val blocked: Int,
    @SerialName("awaiting_confirmation") val awaitingConfirmation: Int,
    val completed: Int,
)

@Serializable
data class ManagerPropertyUnitResponse(
    val id: String,
    val label: String,
)

@Serializable
data class ManagerResidentInviteCreatePayload(
    @SerialName("unit_id") val unitId: String,
    @SerialName("valid_days") val validDays: Int = 7,
)

@Serializable
data class ManagerResidentInviteResponse(
    @SerialName("invite_code") val inviteCode: String,
    @SerialName("unit_id") val unitId: String,
    @SerialName("unit_label") val unitLabel: String,
    @SerialName("expires_at") val expiresAt: String,
)

@Serializable
data class TicketResponse(
    val id: String,
    @SerialName("client_request_id") val clientRequestId: String,
    @SerialName("property_id") val propertyId: String? = null,
    @SerialName("unit_id") val unitId: String,
    @SerialName("resident_id") val residentId: String? = null,
    val title: String,
    val description: String,
    val category: String,
    @SerialName("urgency_suggestion") val urgencySuggestion: String,
    val priority: String? = null,
    @SerialName("access_window") val accessWindow: String,
    val status: String,
    val version: Int,
    @SerialName("assigned_worker_id") val assignedWorkerId: String? = null,
    @SerialName("assigned_worker") val assignedWorker: String?,
    @SerialName("completion_note") val completionNote: String? = null,
    @SerialName("parts_used") val partsUsed: List<String> = emptyList(),
    @SerialName("has_completion_photo") val hasCompletionPhoto: Boolean = false,
    @SerialName("completion_submitted_at") val completionSubmittedAt: String? = null,
    @SerialName("resident_rating") val residentRating: Int? = null,
    @SerialName("resident_feedback") val residentFeedback: String? = null,
    @SerialName("resident_reviewed_at") val residentReviewedAt: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class TicketEventResponse(
    val id: String,
    @SerialName("ticket_id") val ticketId: String,
    @SerialName("actor_role") val actorRole: String,
    val action: String,
    @SerialName("from_status") val fromStatus: String? = null,
    @SerialName("to_status") val toStatus: String,
    @SerialName("ticket_version") val ticketVersion: Int,
    val detail: String? = null,
    @SerialName("created_at") val createdAt: String,
)

class HttpTicketApi(
    baseUrl: String,
    private val contentResolver: ContentResolver,
    private val authTokenProvider: AuthTokenProvider = AuthTokenProvider { null },
    private val json: Json = Json { ignoreUnknownKeys = true },
) : TicketApi, ManagerTicketApi, WorkerTicketApi, PushRegistrationApi, AuthSessionApi {
    private val baseUrl = baseUrl.trimEnd('/')

    override suspend fun createTicket(request: TicketCreatePayload): TicketResponse {
        val responseBody = execute(
            method = "POST",
            path = "/tickets",
            requestBody = json.encodeToString(request),
        )
        return json.decodeFromString(responseBody)
    }

    override suspend fun getAuthSession(): com.localfix.app.data.auth.AuthSession {
        val responseBody = execute(method = "GET", path = "/auth/session")
        return json.decodeFromString<AuthSessionResponse>(responseBody).toDomain()
    }

    override suspend fun redeemResidentInvite(
        inviteCode: String,
    ): com.localfix.app.data.auth.WorkspaceMembership {
        val responseBody = execute(
            method = "POST",
            path = "/auth/resident-invites/redeem",
            requestBody = json.encodeToString(
                ResidentInviteRedemptionPayload(inviteCode),
            ),
        )
        return json.decodeFromString<ResidentInviteRedemptionResponse>(responseBody)
            .membership
            .toDomain()
    }

    override suspend fun registerPushDevice(
        role: String,
        request: PushRegistrationPayload,
    ): PushRegistrationResponse {
        val responseBody = execute(
            method = "POST",
            path = "/devices/${role.lowercase()}",
            requestBody = json.encodeToString(request),
        )
        return json.decodeFromString(responseBody)
    }

    override suspend fun listTickets(): List<TicketResponse> {
        val responseBody = execute(method = "GET", path = "/tickets")
        return json.decodeFromString(responseBody)
    }

    override suspend fun reviewTicket(
        ticketId: String,
        request: TicketReviewPayload,
    ): TicketResponse {
        val responseBody = execute(
            method = "POST",
            path = "/tickets/$ticketId/review",
            requestBody = json.encodeToString(request),
        )
        return json.decodeFromString(responseBody)
    }

    override fun completionPhotoUrl(ticketId: String): String =
        "$baseUrl/tickets/$ticketId/completion-photo"

    override suspend fun listManagerTickets(): List<TicketResponse> {
        val responseBody = execute(method = "GET", path = "/manager/tickets")
        return json.decodeFromString(responseBody)
    }

    override suspend fun listManagerWorkers(): List<WorkerResponse> {
        val responseBody = execute(method = "GET", path = "/manager/workers")
        return json.decodeFromString(responseBody)
    }

    override suspend fun getManagerSummary(): ManagerSummaryResponse {
        val responseBody = execute(method = "GET", path = "/manager/summary")
        return json.decodeFromString(responseBody)
    }

    override suspend fun listManagerUnits(): List<ManagerPropertyUnitResponse> {
        val responseBody = execute(method = "GET", path = "/manager/units")
        return json.decodeFromString(responseBody)
    }

    override suspend fun createManagerResidentInvite(
        request: ManagerResidentInviteCreatePayload,
    ): ManagerResidentInviteResponse {
        val responseBody = execute(
            method = "POST",
            path = "/manager/resident-invites",
            requestBody = json.encodeToString(request),
        )
        return json.decodeFromString(responseBody)
    }

    override suspend fun assignTicket(
        ticketId: String,
        request: TicketAssignmentPayload,
    ): TicketResponse {
        val responseBody = execute(
            method = "POST",
            path = "/manager/tickets/$ticketId/assignment",
            requestBody = json.encodeToString(request),
        )
        return json.decodeFromString(responseBody)
    }

    override suspend fun listWorkerTickets(): List<TicketResponse> {
        val responseBody = execute(method = "GET", path = "/worker/tickets")
        return json.decodeFromString(responseBody)
    }

    override suspend fun listWorkerTicketEvents(
        ticketId: String,
    ): List<TicketEventResponse> {
        val responseBody = execute(
            method = "GET",
            path = "/worker/tickets/$ticketId/events",
        )
        return json.decodeFromString(responseBody)
    }

    override suspend fun startTicket(
        ticketId: String,
        request: TicketStartPayload,
    ): TicketResponse {
        val responseBody = execute(
            method = "POST",
            path = "/worker/tickets/$ticketId/start",
            requestBody = json.encodeToString(request),
        )
        return json.decodeFromString(responseBody)
    }

    override suspend fun submitCompletion(
        ticketId: String,
        request: TicketCompletionPayload,
    ): TicketResponse {
        val responseBody = executeCompletionUpload(ticketId, request)
        return json.decodeFromString(responseBody)
    }

    private suspend fun executeCompletionUpload(
        ticketId: String,
        request: TicketCompletionPayload,
    ): String = withContext(Dispatchers.IO) {
        val photoUri = Uri.parse(request.photoUri)
        val contentType = contentResolver.getType(photoUri) ?: "image/jpeg"
        val boundary = "LocalFix-${UUID.randomUUID()}"
        val connection = URL("$baseUrl/worker/tickets/$ticketId/completion")
            .openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
            connection.readTimeout = READ_TIMEOUT_MILLIS
            connection.doOutput = true
            connection.setChunkedStreamingMode(64 * 1024)
            connection.setRequestProperty("Accept", "application/json")
            addAuthorizationHeader(connection)
            connection.setRequestProperty(
                "Content-Type",
                "multipart/form-data; boundary=$boundary",
            )
            connection.outputStream.buffered().use { output ->
                fun writeText(value: String) {
                    output.write(value.toByteArray(Charsets.UTF_8))
                }

                fun writeField(name: String, value: String) {
                    writeText("--$boundary\r\n")
                    writeText("Content-Disposition: form-data; name=\"$name\"\r\n\r\n")
                    writeText(value)
                    writeText("\r\n")
                }

                writeField("expected_version", request.expectedVersion.toString())
                writeField("completion_note", request.completionNote)
                request.partsUsed.forEach { part -> writeField("parts_used", part) }
                writeText("--$boundary\r\n")
                writeText(
                    "Content-Disposition: form-data; name=\"photo\"; " +
                        "filename=\"completion-photo.${contentType.toExtension()}\"\r\n",
                )
                writeText("Content-Type: $contentType\r\n\r\n")
                val input = runCatching { contentResolver.openInputStream(photoUri) }
                    .getOrElse { error ->
                        throw CompletionPhotoUnavailableException(error)
                    }
                    ?: throw CompletionPhotoUnavailableException()
                input.use { it.copyTo(output) }
                writeText("\r\n--$boundary--\r\n")
            }

            val statusCode = connection.responseCode
            val responseText = (if (statusCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            })?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (statusCode !in 200..299) {
                throw TicketApiException(statusCode, responseText)
            }
            responseText
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun execute(
        method: String,
        path: String,
        requestBody: String? = null,
    ): String = withContext(Dispatchers.IO) {
        val connection = URL("$baseUrl$path").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
            connection.readTimeout = READ_TIMEOUT_MILLIS
            connection.setRequestProperty("Accept", "application/json")
            addAuthorizationHeader(connection)
            if (requestBody != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.bufferedWriter().use { writer ->
                    writer.write(requestBody)
                }
            }

            val statusCode = connection.responseCode
            val responseText = (if (statusCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            })?.bufferedReader()?.use { reader -> reader.readText() }.orEmpty()

            if (statusCode !in 200..299) {
                throw TicketApiException(statusCode, responseText)
            }
            responseText
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun addAuthorizationHeader(connection: HttpURLConnection) {
        authTokenProvider.getIdToken()?.takeIf(String::isNotBlank)?.let { token ->
            connection.setRequestProperty("Authorization", "Bearer $token")
        }
    }

    private companion object {
        const val CONNECT_TIMEOUT_MILLIS = 5_000
        const val READ_TIMEOUT_MILLIS = 10_000
    }
}

private fun String.toExtension(): String = when (this) {
    "image/png" -> "png"
    "image/webp" -> "webp"
    "image/heic" -> "heic"
    "image/heif" -> "heif"
    else -> "jpg"
}

class TicketApiException(
    val statusCode: Int,
    val responseBody: String,
) : Exception("Ticket API request failed with HTTP $statusCode")

class CompletionPhotoUnavailableException(cause: Throwable? = null) :
    IllegalStateException("The selected completion photo is no longer available.", cause)

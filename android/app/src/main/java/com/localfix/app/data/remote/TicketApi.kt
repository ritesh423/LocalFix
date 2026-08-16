package com.localfix.app.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

interface TicketApi {
    suspend fun createTicket(request: TicketCreatePayload): TicketResponse

    suspend fun listTickets(): List<TicketResponse>
}

interface ManagerTicketApi {
    suspend fun listManagerTickets(): List<TicketResponse>

    suspend fun listManagerWorkers(): List<WorkerResponse>

    suspend fun assignTicket(
        ticketId: String,
        request: TicketAssignmentPayload,
    ): TicketResponse
}

interface WorkerTicketApi {
    suspend fun listWorkerTickets(): List<TicketResponse>

    suspend fun startTicket(
        ticketId: String,
        request: TicketStartPayload,
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
data class TicketAssignmentPayload(
    @SerialName("expected_version") val expectedVersion: Int,
    val priority: String,
    @SerialName("worker_id") val workerId: String,
)

@Serializable
data class TicketStartPayload(
    @SerialName("expected_version") val expectedVersion: Int,
)

@Serializable
data class WorkerResponse(
    val id: String,
    val name: String,
    val specialty: String,
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
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)

class HttpTicketApi(
    baseUrl: String,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : TicketApi, ManagerTicketApi, WorkerTicketApi {
    private val baseUrl = baseUrl.trimEnd('/')

    override suspend fun createTicket(request: TicketCreatePayload): TicketResponse {
        val responseBody = execute(
            method = "POST",
            path = "/tickets",
            requestBody = json.encodeToString(request),
        )
        return json.decodeFromString(responseBody)
    }

    override suspend fun listTickets(): List<TicketResponse> {
        val responseBody = execute(method = "GET", path = "/tickets")
        return json.decodeFromString(responseBody)
    }

    override suspend fun listManagerTickets(): List<TicketResponse> {
        val responseBody = execute(method = "GET", path = "/manager/tickets")
        return json.decodeFromString(responseBody)
    }

    override suspend fun listManagerWorkers(): List<WorkerResponse> {
        val responseBody = execute(method = "GET", path = "/manager/workers")
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

    private companion object {
        const val CONNECT_TIMEOUT_MILLIS = 5_000
        const val READ_TIMEOUT_MILLIS = 10_000
    }
}

class TicketApiException(
    val statusCode: Int,
    val responseBody: String,
) : Exception("Ticket API request failed with HTTP $statusCode")

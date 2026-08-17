package com.localfix.app.data.resident

import com.localfix.app.data.local.PendingResidentRequestEntity
import com.localfix.app.data.local.ResidentTicketEntity
import com.localfix.app.data.model.NewMaintenanceRequest
import com.localfix.app.data.model.RequestDeliveryState
import com.localfix.app.data.model.ServiceCategory
import com.localfix.app.data.model.TicketStatus
import com.localfix.app.data.model.UrgencySuggestion
import com.localfix.app.data.model.AccessWindow
import com.localfix.app.data.remote.TicketCreatePayload
import com.localfix.app.data.remote.TicketResponse
import java.time.Clock

internal fun NewMaintenanceRequest.toPendingEntity(
    clock: Clock,
): PendingResidentRequestEntity = PendingResidentRequestEntity(
    clientRequestId = clientRequestId,
    title = title,
    description = description,
    category = category,
    urgencySuggestion = urgencySuggestion,
    accessWindow = accessWindow,
    photoUri = photoUri,
    deliveryState = RequestDeliveryState.PENDING,
    failureMessage = null,
    queuedAt = clock.instant().toString(),
)

internal fun PendingResidentRequestEntity.toPayload(): TicketCreatePayload = TicketCreatePayload(
    clientRequestId = clientRequestId,
    title = title,
    description = description,
    category = category.name.lowercase(),
    urgencySuggestion = urgencySuggestion.name.lowercase(),
    accessWindow = accessWindow.name.lowercase(),
)

internal fun TicketResponse.toResidentTicketEntity(): ResidentTicketEntity = ResidentTicketEntity(
    id = id,
    propertyId = propertyId.orEmpty(),
    unitId = unitId,
    residentId = residentId.orEmpty(),
    title = title,
    description = description,
    category = ServiceCategory.valueOf(category.uppercase()),
    status = TicketStatus.valueOf(status.uppercase()),
    urgencySuggestion = UrgencySuggestion.valueOf(urgencySuggestion.uppercase()),
    accessWindow = AccessWindow.valueOf(accessWindow.uppercase()),
    assignedWorker = assignedWorker,
    version = version,
    completionNote = completionNote,
    partsUsed = partsUsed,
    hasCompletionPhoto = hasCompletionPhoto,
    residentRating = residentRating,
    residentFeedback = residentFeedback,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

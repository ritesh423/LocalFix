package com.localfix.app.data.local

import androidx.room.TypeConverter
import com.localfix.app.data.model.AccessWindow
import com.localfix.app.data.model.ServiceCategory
import com.localfix.app.data.model.RequestDeliveryState
import com.localfix.app.data.model.TicketStatus
import com.localfix.app.data.model.TicketCommandType
import com.localfix.app.data.model.UrgencySuggestion
import com.localfix.app.data.model.ResidentReviewDecision
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RoomConverters {
    @TypeConverter
    fun serviceCategoryToString(value: ServiceCategory?): String? = value?.name

    @TypeConverter
    fun stringToServiceCategory(value: String?): ServiceCategory? =
        value?.let(ServiceCategory::valueOf)

    @TypeConverter
    fun urgencyToString(value: UrgencySuggestion): String = value.name

    @TypeConverter
    fun stringToUrgency(value: String): UrgencySuggestion = UrgencySuggestion.valueOf(value)

    @TypeConverter
    fun accessWindowToString(value: AccessWindow): String = value.name

    @TypeConverter
    fun stringToAccessWindow(value: String): AccessWindow = AccessWindow.valueOf(value)

    @TypeConverter
    fun ticketStatusToString(value: TicketStatus): String = value.name

    @TypeConverter
    fun stringToTicketStatus(value: String): TicketStatus = TicketStatus.valueOf(value)

    @TypeConverter
    fun stringListToJson(value: List<String>): String = Json.encodeToString(value)

    @TypeConverter
    fun jsonToStringList(value: String): List<String> = Json.decodeFromString(value)

    @TypeConverter
    fun deliveryStateToString(value: RequestDeliveryState): String = value.name

    @TypeConverter
    fun stringToDeliveryState(value: String): RequestDeliveryState =
        RequestDeliveryState.valueOf(value)

    @TypeConverter
    fun reviewDecisionToString(value: ResidentReviewDecision): String = value.name

    @TypeConverter
    fun stringToReviewDecision(value: String): ResidentReviewDecision =
        ResidentReviewDecision.valueOf(value)

    @TypeConverter
    fun ticketCommandTypeToString(value: TicketCommandType): String = value.name

    @TypeConverter
    fun stringToTicketCommandType(value: String): TicketCommandType =
        TicketCommandType.valueOf(value)
}

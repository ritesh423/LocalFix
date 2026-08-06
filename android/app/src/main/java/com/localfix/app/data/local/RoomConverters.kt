package com.localfix.app.data.local

import androidx.room.TypeConverter
import com.localfix.app.data.model.AccessWindow
import com.localfix.app.data.model.ServiceCategory
import com.localfix.app.data.model.UrgencySuggestion

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
}

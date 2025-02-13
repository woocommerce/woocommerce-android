package org.wordpress.android.fluxc.persistence.converters

import androidx.room.TypeConverter
import org.wordpress.android.util.DateTimeUtils
import java.util.Date

/**
 * A Room type converter for ISO 8601-formatted dates
 */
class ISO8601DateConverter {
    @TypeConverter
    fun stringToDate(value: String?) = value?.let { DateTimeUtils.dateUTCFromIso8601(it) }

    @TypeConverter
    fun dateToString(date: Date?) = date?.let { DateTimeUtils.iso8601FromDate(it) }
}

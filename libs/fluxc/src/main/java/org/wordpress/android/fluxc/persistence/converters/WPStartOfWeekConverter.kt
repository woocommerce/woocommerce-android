package org.wordpress.android.fluxc.persistence.converters

import androidx.room.TypeConverter
import java.time.DayOfWeek

class WPStartOfWeekConverter {
    @TypeConverter
    fun fromWPStartOfWeek(value: Int?): DayOfWeek? {
        return when (value) {
            WP_SUNDAY -> DayOfWeek.SUNDAY
            WP_MONDAY -> DayOfWeek.MONDAY
            WP_TUESDAY -> DayOfWeek.TUESDAY
            WP_WEDNESDAY -> DayOfWeek.WEDNESDAY
            WP_THURSDAY -> DayOfWeek.THURSDAY
            WP_FRIDAY -> DayOfWeek.FRIDAY
            WP_SATURDAY -> DayOfWeek.SATURDAY
            else -> null
        }
    }

    @TypeConverter
    fun toWPStartOfWeek(dayOfWeek: DayOfWeek?): Int? {
        return when (dayOfWeek) {
            DayOfWeek.SUNDAY -> WP_SUNDAY
            DayOfWeek.MONDAY -> WP_MONDAY
            DayOfWeek.TUESDAY -> WP_TUESDAY
            DayOfWeek.WEDNESDAY -> WP_WEDNESDAY
            DayOfWeek.THURSDAY -> WP_THURSDAY
            DayOfWeek.FRIDAY -> WP_FRIDAY
            DayOfWeek.SATURDAY -> WP_SATURDAY
            null -> null
        }
    }

    private companion object {
        const val WP_SUNDAY = 0
        const val WP_MONDAY = 1
        const val WP_TUESDAY = 2
        const val WP_WEDNESDAY = 3
        const val WP_THURSDAY = 4
        const val WP_FRIDAY = 5
        const val WP_SATURDAY = 6
    }
}

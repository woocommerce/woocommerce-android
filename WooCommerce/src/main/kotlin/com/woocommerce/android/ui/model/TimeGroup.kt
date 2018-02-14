package com.woocommerce.android.ui.model

import org.apache.commons.lang3.time.DateUtils
import java.util.Date

enum class TimeGroup {
    GROUP_TODAY,
    GROUP_YESTERDAY,
    GROUP_OLDER_TWO_DAYS,
    GROUP_OLDER_WEEK,
    GROUP_OLDER_MONTH;

    companion object {
        fun getTimeGroupForDate(date: Date): TimeGroup {
            val dateToday = Date()
            return when {
                date < DateUtils.addMonths(dateToday, -1) -> GROUP_OLDER_MONTH
                date < DateUtils.addWeeks(dateToday, -1) -> GROUP_OLDER_WEEK
                date < DateUtils.addDays(dateToday, -2) -> GROUP_OLDER_TWO_DAYS
                DateUtils.isSameDay(DateUtils.addDays(dateToday, -2), date) -> GROUP_OLDER_TWO_DAYS
                DateUtils.isSameDay(DateUtils.addDays(dateToday, -1), date) -> GROUP_YESTERDAY
                else -> GROUP_TODAY
            }
        }
    }
}

package com.woocommerce.android.model

import androidx.annotation.StringRes
import com.woocommerce.android.R
import org.apache.commons.lang3.time.DateUtils
import org.wordpress.android.util.DateTimeUtils
import java.util.Calendar
import java.util.Date

enum class TimeGroup(@StringRes val labelRes: Int) {
    GROUP_FUTURE(R.string.date_timeframe_future),
    GROUP_TODAY(R.string.date_timeframe_today),
    GROUP_YESTERDAY(R.string.date_timeframe_yesterday),
    GROUP_OLDER_TWO_DAYS(R.string.date_timeframe_older_two_days),
    GROUP_OLDER_WEEK(R.string.date_timeframe_older_week),
    GROUP_OLDER_MONTH(R.string.date_timeframe_older_month);

    companion object {
        fun getTimeGroupForDate(date: Date): TimeGroup {
            // Normalize the dates to drop time information
            val dateToday = DateUtils.round(DateTimeUtils.nowUTC(), Calendar.DATE)
            val dateToCheck = DateUtils.round(date, Calendar.DATE)

            return when {
                dateToCheck.after(dateToday) -> GROUP_FUTURE
                dateToCheck < DateUtils.addMonths(dateToday, -1) -> GROUP_OLDER_MONTH
                dateToCheck < DateUtils.addWeeks(dateToday, -1) -> GROUP_OLDER_WEEK
                dateToCheck < DateUtils.addDays(dateToday, -2) -> GROUP_OLDER_TWO_DAYS
                DateUtils.isSameDay(DateUtils.addDays(dateToday, -2), dateToCheck) -> GROUP_OLDER_TWO_DAYS
                DateUtils.isSameDay(DateUtils.addDays(dateToday, -1), dateToCheck) -> GROUP_YESTERDAY
                else -> GROUP_TODAY
            }
        }
    }
}

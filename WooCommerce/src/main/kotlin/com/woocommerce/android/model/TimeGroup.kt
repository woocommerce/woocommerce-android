package com.woocommerce.android.model

import androidx.annotation.StringRes
import com.woocommerce.android.R
import org.apache.commons.lang3.time.DateUtils
import org.wordpress.android.util.DateTimeUtils
import java.util.Date

enum class TimeGroup(@StringRes val labelRes: Int) {
    GROUP_FUTURE(R.string.date_timeframe_future),
    GROUP_TODAY(R.string.date_timeframe_today),
    GROUP_YESTERDAY(R.string.date_timeframe_yesterday),
    GROUP_OLDER_TWO_DAYS(R.string.date_timeframe_older_two_days),
    GROUP_OLDER_WEEK(R.string.date_timeframe_older_week),
    GROUP_OLDER_MONTH(R.string.date_timeframe_older_month);

    companion object {
        fun getTimeGroupForDate(localDate: Date): TimeGroup {
            val utcDate = DateTimeUtils.localDateToUTC(localDate)
            val dateToday = DateTimeUtils.nowUTC()

            return when {
                utcDate < DateUtils.addMonths(dateToday, -1) -> GROUP_OLDER_MONTH
                utcDate < DateUtils.addWeeks(dateToday, -1) -> GROUP_OLDER_WEEK
                utcDate < DateUtils.addDays(dateToday, -2) -> GROUP_OLDER_TWO_DAYS
                DateUtils.isSameDay(DateUtils.addDays(dateToday, -2), utcDate) -> GROUP_OLDER_TWO_DAYS
                DateUtils.isSameDay(DateUtils.addDays(dateToday, -1), utcDate) -> GROUP_YESTERDAY
                DateUtils.isSameDay(dateToday, utcDate) -> GROUP_TODAY
                else -> GROUP_FUTURE
            }
        }
    }
}

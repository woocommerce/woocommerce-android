package com.woocommerce.android.model

import androidx.annotation.StringRes
import com.woocommerce.android.R
import org.apache.commons.lang3.time.DateUtils
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
            val dateToday = Date()

            return when {
                localDate < DateUtils.addMonths(dateToday, -1) -> GROUP_OLDER_MONTH
                localDate < DateUtils.addWeeks(dateToday, -1) -> GROUP_OLDER_WEEK
                localDate < DateUtils.addDays(dateToday, -2) -> GROUP_OLDER_TWO_DAYS
                DateUtils.isSameDay(DateUtils.addDays(dateToday, -2), localDate) -> GROUP_OLDER_TWO_DAYS
                DateUtils.isSameDay(DateUtils.addDays(dateToday, -1), localDate) -> GROUP_YESTERDAY
                DateUtils.isSameDay(dateToday, localDate) -> GROUP_TODAY
                else -> GROUP_FUTURE
            }
        }
    }
}

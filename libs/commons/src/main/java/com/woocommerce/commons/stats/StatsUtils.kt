package com.woocommerce.commons.stats

import java.util.Date

object StatsUtils {
    fun String.asRevenueRangeId(
        startDate: Date,
        endDate: Date
    ): String {
        return StatsTimeRange(
            startDate,
            endDate
        ).toRevenueRangeId(this)
    }

    fun StatsTimeRange.toRevenueRangeId(medium: String): String {
        return medium +
            DateUtils.getYearMonthDayStringFromDate(start) +
            DateUtils.getYearMonthDayStringFromDate(end)
    }
}

package com.woocommerce.commons.stats

object StatsUtils {
    fun StatsTimeRange.toRevenueRangeId(medium: String): String {
        return medium +
            DateUtils.getYearMonthDayStringFromDate(start) +
            DateUtils.getYearMonthDayStringFromDate(end)
    }
}

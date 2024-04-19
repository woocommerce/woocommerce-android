package com.woocommerce.android.ui.dashboard.stats

import android.icu.text.SimpleDateFormat
import com.woocommerce.android.extensions.formatToMMMMyyyy
import com.woocommerce.android.extensions.formatToYYYY
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType

class DashboardStatsRangeFormatter {
    fun formatRangeDate(rangeSelection: StatsTimeRangeSelection): String {
        val startDate = rangeSelection.currentRange.start
        val endDate = rangeSelection.currentRange.end
        val dateFormatter = SimpleDateFormat.getDateInstance()

        return when(rangeSelection.selectionType) {
            SelectionType.TODAY -> dateFormatter.format(startDate)
            SelectionType.MONTH_TO_DATE -> startDate.formatToMMMMyyyy()
            SelectionType.YEAR_TO_DATE -> startDate.formatToYYYY()
            SelectionType.WEEK_TO_DATE, SelectionType.CUSTOM ->
                "${dateFormatter.format(startDate)} – ${dateFormatter.format(endDate)}"
            else -> error("Unsupported range value used in Dashboard performance card: ${rangeSelection.selectionType}")
        }
    }
}

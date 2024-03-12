package com.woocommerce.android.ui.analytics.ranges

import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import kotlin.time.Duration.Companion.milliseconds

@Suppress("MagicNumber")
val StatsTimeRangeSelection.revenueStatsGranularity: StatsGranularity
    get() {
        fun calculateCustomRangeGranularity(): StatsGranularity {
            val daysDifference = (currentRange.end.time - currentRange.start.time).milliseconds.inWholeDays

            return when {
                daysDifference < 2 -> StatsGranularity.HOURS
                daysDifference <= 28 -> StatsGranularity.DAYS
                daysDifference <= 90 -> StatsGranularity.WEEKS
                daysDifference <= 3 * 365 -> StatsGranularity.MONTHS
                else -> StatsGranularity.YEARS
            }
        }

        return when (selectionType) {
            SelectionType.TODAY, SelectionType.YESTERDAY -> StatsGranularity.HOURS
            SelectionType.LAST_WEEK, SelectionType.WEEK_TO_DATE -> StatsGranularity.DAYS
            SelectionType.LAST_MONTH, SelectionType.MONTH_TO_DATE -> StatsGranularity.DAYS
            SelectionType.LAST_QUARTER, SelectionType.QUARTER_TO_DATE,
            SelectionType.LAST_YEAR, SelectionType.YEAR_TO_DATE -> StatsGranularity.MONTHS

            SelectionType.CUSTOM -> calculateCustomRangeGranularity()
        }
    }

val StatsTimeRangeSelection.visitorStatsGranularity: StatsGranularity
    get() = revenueStatsGranularity.let {
        // Visitor stats do not support hours granularity
        if (it == StatsGranularity.HOURS) StatsGranularity.DAYS else it
    }

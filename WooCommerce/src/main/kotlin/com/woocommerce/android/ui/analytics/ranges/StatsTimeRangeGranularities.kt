package com.woocommerce.android.ui.analytics.ranges

import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType
import com.woocommerce.commons.stats.StatsTimeRange
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import java.lang.Exception
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit

@Suppress("MagicNumber")
val StatsTimeRangeSelection.revenueStatsGranularity: StatsGranularity
    get() {
        fun calculateCustomRangeGranularity(): StatsGranularity {
            val dayCount = currentRange.dayCount

            return when {
                dayCount <= 1 -> StatsGranularity.HOURS
                dayCount <= 28 -> StatsGranularity.DAYS
                dayCount <= 90 -> StatsGranularity.WEEKS
                dayCount <= 3 * 365 -> StatsGranularity.MONTHS
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

val StatsTimeRangeSelection.visitorSummaryStatsGranularity: StatsGranularity
    get() = when (selectionType) {
        SelectionType.TODAY, SelectionType.YESTERDAY -> StatsGranularity.DAYS
        SelectionType.LAST_WEEK, SelectionType.WEEK_TO_DATE -> StatsGranularity.WEEKS
        SelectionType.LAST_MONTH, SelectionType.MONTH_TO_DATE -> StatsGranularity.MONTHS
        SelectionType.LAST_YEAR, SelectionType.YEAR_TO_DATE -> StatsGranularity.YEARS
        SelectionType.LAST_QUARTER, SelectionType.QUARTER_TO_DATE ->
            throw NotSupportedGranularity("Summary visitor stats unsupported quarter ranges")

        SelectionType.CUSTOM -> {
            when {
                currentRange.dayCount <= 1 -> StatsGranularity.DAYS
                else -> throw NotSupportedGranularity(
                    "Summary visitor stats unsupported for custom ranges with more than 1 day"
                )
            }
        }
    }

// Rounds the range length to whole days, so the hour gained or lost on a DST change doesn't change the count
val StatsTimeRange.dayCount: Int
    get() = (end.time - start.time).milliseconds.toDouble(DurationUnit.DAYS).roundToInt()

class NotSupportedGranularity(message: String) : Exception(message)

val StatsTimeRangeSelection.myStoreTrackingGranularityString: String
    get() = selectionType.toDashBoardTrackingGranularityString()

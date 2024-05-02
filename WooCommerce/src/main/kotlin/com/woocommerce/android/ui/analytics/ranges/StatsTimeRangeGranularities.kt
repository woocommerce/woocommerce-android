package com.woocommerce.android.ui.analytics.ranges

import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import java.lang.Exception
import kotlin.time.Duration.Companion.days

@Suppress("MagicNumber")
val StatsTimeRangeSelection.revenueStatsGranularity: StatsGranularity
    get() {
        fun calculateCustomRangeGranularity(): StatsGranularity {
            val daysDifference = currentRange.end.time - currentRange.start.time

            return when {
                daysDifference <= 1.days.inWholeMilliseconds -> StatsGranularity.HOURS
                daysDifference <= 28.days.inWholeMilliseconds -> StatsGranularity.DAYS
                daysDifference <= 90.days.inWholeMilliseconds -> StatsGranularity.WEEKS
                daysDifference <= (3 * 365).days.inWholeMilliseconds -> StatsGranularity.MONTHS
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
            val difference = currentRange.end.time - currentRange.start.time

            when {
                difference <= 1.days.inWholeMilliseconds -> StatsGranularity.DAYS
                else -> throw NotSupportedGranularity(
                    "Summary visitor stats unsupported for custom ranges with more than 1 day"
                )
            }
        }
    }

class NotSupportedGranularity(message: String) : Exception(message)

val StatsTimeRangeSelection.myStoreTrackingGranularityString: String
    get() = selectionType.toDashBoardTrackingGranularityString()

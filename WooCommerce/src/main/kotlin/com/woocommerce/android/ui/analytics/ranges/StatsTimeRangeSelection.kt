package com.woocommerce.android.ui.analytics.ranges

import android.os.Parcelable
import com.woocommerce.android.R
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType.CUSTOM
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType.LAST_MONTH
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType.LAST_QUARTER
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType.LAST_WEEK
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType.LAST_YEAR
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType.MONTH_TO_DATE
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType.QUARTER_TO_DATE
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType.TODAY
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType.WEEK_TO_DATE
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType.YEAR_TO_DATE
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType.YESTERDAY
import com.woocommerce.android.ui.analytics.ranges.data.CustomRangeData
import com.woocommerce.android.ui.analytics.ranges.data.LastMonthRangeData
import com.woocommerce.android.ui.analytics.ranges.data.LastQuarterRangeData
import com.woocommerce.android.ui.analytics.ranges.data.LastWeekRangeData
import com.woocommerce.android.ui.analytics.ranges.data.LastYearRangeData
import com.woocommerce.android.ui.analytics.ranges.data.MonthToDateRangeData
import com.woocommerce.android.ui.analytics.ranges.data.QuarterToDateRangeData
import com.woocommerce.android.ui.analytics.ranges.data.TodayRangeData
import com.woocommerce.android.ui.analytics.ranges.data.WeekToDateRangeData
import com.woocommerce.android.ui.analytics.ranges.data.YearToDateRangeData
import com.woocommerce.android.ui.analytics.ranges.data.YesterdayRangeData
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Parcelize
data class StatsTimeRange(
    val start: Date,
    val end: Date
) : Parcelable

/**
 * This class represents the date range selection for the Analytics Hub and the Stats screen
 *
 * You can create it through the [StatsTimeRangeSelection.SelectionType.generateSelectionData]
 * function since it will return the correct data for the given selection type
 *
 * When creating the object through the available constructor, the Selection will be set as [CUSTOM]
 */
@Parcelize
class StatsTimeRangeSelection private constructor(
    val selectionType: SelectionType,
    val currentRange: StatsTimeRange,
    val previousRange: StatsTimeRange,
    val currentRangeDescription: String,
    val previousRangeDescription: String,
) : Parcelable {

    companion object Factory {
        fun build(
            rangeStart: Date,
            rangeEnd: Date,
            calendar: Calendar,
            locale: Locale,
        ): StatsTimeRangeSelection {
            val rangeData = CustomRangeData(rangeStart, rangeEnd, locale, calendar)
            return StatsTimeRangeSelection(
                selectionType = CUSTOM,
                currentRange = rangeData.currentRange,
                previousRange = rangeData.previousRange,
                currentRangeDescription = rangeData.formattedCurrentRange,
                previousRangeDescription = rangeData.formattedPreviousRange,
            )
        }

        fun build(
            selectionType: SelectionType,
            referenceDate: Date,
            calendar: Calendar,
            locale: Locale
        ): StatsTimeRangeSelection {
            val rangeData = generateTimeRangeData(selectionType, referenceDate, locale, calendar)
            return StatsTimeRangeSelection(
                selectionType = selectionType,
                currentRange = rangeData.currentRange,
                previousRange = rangeData.previousRange,
                currentRangeDescription = rangeData.formattedCurrentRange,
                previousRangeDescription = rangeData.formattedPreviousRange,
            )
        }

        private fun generateTimeRangeData(
            selectionType: SelectionType,
            referenceDate: Date,
            locale: Locale,
            calendar: Calendar
        ): StatsTimeRangeData {
            return when (selectionType) {
                TODAY -> TodayRangeData(referenceDate, locale, calendar)
                YESTERDAY -> YesterdayRangeData(referenceDate, locale, calendar)
                WEEK_TO_DATE -> WeekToDateRangeData(referenceDate, locale, calendar)
                LAST_WEEK -> LastWeekRangeData(referenceDate, locale, calendar)
                MONTH_TO_DATE -> MonthToDateRangeData(referenceDate, locale, calendar)
                LAST_MONTH -> LastMonthRangeData(referenceDate, locale, calendar)
                QUARTER_TO_DATE -> QuarterToDateRangeData(referenceDate, locale, calendar)
                LAST_QUARTER -> LastQuarterRangeData(referenceDate, locale, calendar)
                YEAR_TO_DATE -> YearToDateRangeData(referenceDate, locale, calendar)
                LAST_YEAR -> LastYearRangeData(referenceDate, locale, calendar)
                else -> throw IllegalStateException("Custom selection type should use the correct constructor")
            }
        }
    }

    enum class SelectionType(val localizedResourceId: Int) {
        TODAY(R.string.date_timeframe_today),
        YESTERDAY(R.string.date_timeframe_yesterday),
        LAST_WEEK(R.string.date_timeframe_last_week),
        LAST_MONTH(R.string.date_timeframe_last_month),
        LAST_QUARTER(R.string.date_timeframe_last_quarter),
        LAST_YEAR(R.string.date_timeframe_last_year),
        WEEK_TO_DATE(R.string.date_timeframe_week_to_date),
        MONTH_TO_DATE(R.string.date_timeframe_month_to_date),
        QUARTER_TO_DATE(R.string.date_timeframe_quarter_to_date),
        YEAR_TO_DATE(R.string.date_timeframe_year_to_date),
        CUSTOM(R.string.date_timeframe_custom);

        fun generateSelectionData(
            referenceStartDate: Date,
            referenceEndDate: Date,
            calendar: Calendar,
            locale: Locale
        ): StatsTimeRangeSelection {
            return if (this == CUSTOM) {
                build(
                    rangeStart = referenceStartDate,
                    rangeEnd = referenceEndDate,
                    calendar = calendar,
                    locale = locale
                )
            } else {
                build(
                    selectionType = this,
                    referenceDate = referenceStartDate,
                    calendar = calendar,
                    locale = locale
                )
            }
        }

        val identifier: String
            get() = when (this) {
                TODAY -> "Today"
                YESTERDAY -> "Yesterday"
                LAST_WEEK -> "Last Week"
                LAST_MONTH -> "Last Month"
                LAST_QUARTER -> "Last Quarter"
                LAST_YEAR -> "Last Year"
                WEEK_TO_DATE -> "Week to Date"
                MONTH_TO_DATE -> "Month to Date"
                QUARTER_TO_DATE -> "Quarter to Date"
                YEAR_TO_DATE -> "Year to Date"
                CUSTOM -> "Custom"
            }

        companion object {
            fun from(granularity: StatsGranularity) =
                when (granularity) {
                    StatsGranularity.DAYS -> TODAY
                    StatsGranularity.WEEKS -> WEEK_TO_DATE
                    StatsGranularity.MONTHS -> MONTH_TO_DATE
                    StatsGranularity.YEARS -> YEAR_TO_DATE
                    StatsGranularity.HOURS -> error("Hours shouldn't be used now")
                }

            fun from(description: String): SelectionType {
                return values().firstOrNull { it.toString() == description } ?: CUSTOM
            }

            val names: Array<String>
                get() = values().map { it.name }.toTypedArray()
        }
    }
}

fun SelectionType.toDashBoardTrackingGranularityString(): String {
    return when (this) {
        TODAY -> StatsGranularity.DAYS.name
        WEEK_TO_DATE -> StatsGranularity.WEEKS.name
        MONTH_TO_DATE -> StatsGranularity.MONTHS.name
        YEAR_TO_DATE -> StatsGranularity.YEARS.name
        CUSTOM -> this.identifier
        else -> error("My Store tracking granularity unsupported range")
    }.lowercase()
}

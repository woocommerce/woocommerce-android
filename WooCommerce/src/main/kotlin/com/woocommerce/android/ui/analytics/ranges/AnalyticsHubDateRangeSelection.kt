package com.woocommerce.android.ui.analytics.ranges

import com.woocommerce.android.R
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection.SelectionType.CUSTOM
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection.SelectionType.LAST_MONTH
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection.SelectionType.LAST_QUARTER
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection.SelectionType.LAST_WEEK
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection.SelectionType.LAST_YEAR
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection.SelectionType.MONTH_TO_DATE
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection.SelectionType.QUARTER_TO_DATE
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection.SelectionType.TODAY
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection.SelectionType.WEEK_TO_DATE
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection.SelectionType.YEAR_TO_DATE
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection.SelectionType.YESTERDAY
import com.woocommerce.android.ui.analytics.ranges.data.AnalyticsHubCustomRangeData
import com.woocommerce.android.ui.analytics.ranges.data.AnalyticsHubLastMonthRangeData
import com.woocommerce.android.ui.analytics.ranges.data.AnalyticsHubLastQuarterRangeData
import com.woocommerce.android.ui.analytics.ranges.data.AnalyticsHubLastWeekRangeData
import com.woocommerce.android.ui.analytics.ranges.data.AnalyticsHubLastYearRangeData
import com.woocommerce.android.ui.analytics.ranges.data.AnalyticsHubMonthToDateRangeData
import com.woocommerce.android.ui.analytics.ranges.data.AnalyticsHubQuarterToDateRangeData
import com.woocommerce.android.ui.analytics.ranges.data.AnalyticsHubTodayRangeData
import com.woocommerce.android.ui.analytics.ranges.data.AnalyticsHubWeekToDateRangeData
import com.woocommerce.android.ui.analytics.ranges.data.AnalyticsHubYearToDateRangeData
import com.woocommerce.android.ui.analytics.ranges.data.AnalyticsHubYesterdayRangeData
import java.io.Serializable
import java.lang.IllegalStateException
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * This class represents the date range selection for the Analytics Hub
 *
 * You should create it through the [AnalyticsHubDateRangeSelection.SelectionType.generateSelectionData]
 * function since it will return the correct data for the given selection type
 */
class AnalyticsHubDateRangeSelection: Serializable {
    val selectionType: SelectionType
    var currentRange: AnalyticsHubTimeRange
        private set
    var previousRange: AnalyticsHubTimeRange
        private set
    val currentRangeDescription: String
    val previousRangeDescription: String

    constructor(
        rangeStart: Date,
        rangeEnd: Date,
        calendar: Calendar,
        locale: Locale,
    ) {
        this.selectionType = CUSTOM
        val rangeData = AnalyticsHubCustomRangeData(rangeStart, rangeEnd)
        currentRange = rangeData.currentRange
        previousRange = rangeData.previousRange
        currentRangeDescription = currentRange.generateDescription(false, locale, calendar)
        previousRangeDescription = previousRange.generateDescription(false, locale, calendar)
    }

    private constructor(
        selectionType: SelectionType,
        referenceDate: Date,
        calendar: Calendar,
        locale: Locale
    ) {
        this.selectionType = selectionType
        val rangeData = generateTimeRangeData(selectionType, referenceDate, calendar)
        currentRange = rangeData.currentRange
        previousRange = rangeData.previousRange

        val simplifiedDescription = selectionType == TODAY || selectionType == YESTERDAY
        currentRangeDescription = currentRange.generateDescription(simplifiedDescription, locale, calendar)
        previousRangeDescription = previousRange.generateDescription(simplifiedDescription, locale, calendar)
    }

    private fun generateTimeRangeData(
        selectionType: SelectionType,
        referenceDate: Date,
        calendar: Calendar
    ): AnalyticsHubTimeRangeData {
        return when (selectionType) {
            TODAY -> AnalyticsHubTodayRangeData(referenceDate, calendar)
            YESTERDAY -> AnalyticsHubYesterdayRangeData(referenceDate, calendar)
            WEEK_TO_DATE -> AnalyticsHubWeekToDateRangeData(referenceDate, calendar)
            LAST_WEEK -> AnalyticsHubLastWeekRangeData(referenceDate, calendar)
            MONTH_TO_DATE -> AnalyticsHubMonthToDateRangeData(referenceDate, calendar)
            LAST_MONTH -> AnalyticsHubLastMonthRangeData(referenceDate, calendar)
            QUARTER_TO_DATE -> AnalyticsHubQuarterToDateRangeData(referenceDate, calendar)
            LAST_QUARTER -> AnalyticsHubLastQuarterRangeData(referenceDate, calendar)
            YEAR_TO_DATE -> AnalyticsHubYearToDateRangeData(referenceDate, calendar)
            LAST_YEAR -> AnalyticsHubLastYearRangeData(referenceDate, calendar)
            else -> throw IllegalStateException("Custom selection type should use the correct constructor")
        }
    }

    enum class SelectionType(val description: String, val localizedResourceId: Int) {
        TODAY("Today", R.string.date_timeframe_today),
        YESTERDAY("Yesterday", R.string.date_timeframe_yesterday),
        LAST_WEEK("Last Week", R.string.date_timeframe_last_week),
        LAST_MONTH("Last Month", R.string.date_timeframe_last_month),
        LAST_QUARTER("Last Quarter", R.string.date_timeframe_last_quarter),
        LAST_YEAR("Last Year", R.string.date_timeframe_last_year),
        WEEK_TO_DATE("Week to Date", R.string.date_timeframe_week_to_date),
        MONTH_TO_DATE("Month to Date", R.string.date_timeframe_month_to_date),
        QUARTER_TO_DATE("Quarter to Date", R.string.date_timeframe_quarter_to_date),
        YEAR_TO_DATE("Year to Date", R.string.date_timeframe_year_to_date),
        CUSTOM("Custom", R.string.date_timeframe_custom);

        fun generateSelectionData(
            referenceStartDate: Date = Date(),
            referenceEndDate: Date = Date(),
            calendar: Calendar = Calendar.getInstance(),
            locale: Locale = Locale.getDefault()
        ): AnalyticsHubDateRangeSelection {
            return if (this == CUSTOM) {
                AnalyticsHubDateRangeSelection(
                    rangeStart = referenceStartDate,
                    rangeEnd = referenceEndDate,
                    calendar = calendar,
                    locale = locale
                )
            } else {
                AnalyticsHubDateRangeSelection(
                    selectionType = this,
                    referenceDate = referenceStartDate,
                    calendar = calendar,
                    locale = locale
                )
            }
        }

        companion object {
            fun from(datePeriod: String): SelectionType = values()
                .find { it.description == datePeriod } ?: TODAY
        }
    }
}

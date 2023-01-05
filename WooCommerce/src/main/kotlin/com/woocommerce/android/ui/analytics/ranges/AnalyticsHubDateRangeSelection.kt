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
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * This class represents the date range selection for the Analytics Hub
 *
 * You can create it through the [AnalyticsHubDateRangeSelection.SelectionType.generateSelectionData]
 * function since it will return the correct data for the given selection type
 *
 * When creating the object through the available constructor, the Selection will be set as [CUSTOM]
 */
class AnalyticsHubDateRangeSelection {
    val selectionType: SelectionType
    val currentRange: AnalyticsHubTimeRange
    val previousRange: AnalyticsHubTimeRange
    val currentRangeDescription: String
    val previousRangeDescription: String

    constructor(
        rangeStart: Date,
        rangeEnd: Date,
        calendar: Calendar,
        locale: Locale,
    ) {
        this.selectionType = CUSTOM
        val rangeData = CustomRangeData(rangeStart, rangeEnd, calendar)
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
            TODAY -> TodayRangeData(referenceDate, calendar)
            YESTERDAY -> YesterdayRangeData(referenceDate, calendar)
            WEEK_TO_DATE -> WeekToDateRangeData(referenceDate, calendar)
            LAST_WEEK -> LastWeekRangeData(referenceDate, calendar)
            MONTH_TO_DATE -> MonthToDateRangeData(referenceDate, calendar)
            LAST_MONTH -> LastMonthRangeData(referenceDate, calendar)
            QUARTER_TO_DATE -> QuarterToDateRangeData(referenceDate, calendar)
            LAST_QUARTER -> LastQuarterRangeData(referenceDate, calendar)
            YEAR_TO_DATE -> YearToDateRangeData(referenceDate, calendar)
            LAST_YEAR -> LastYearRangeData(referenceDate, calendar)
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

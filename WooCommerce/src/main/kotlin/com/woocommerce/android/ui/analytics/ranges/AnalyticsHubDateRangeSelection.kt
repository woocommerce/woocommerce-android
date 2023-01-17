package com.woocommerce.android.ui.analytics.ranges

import android.os.Parcelable
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
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import java.io.Serializable
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Parcelize
data class AnalyticsHubTimeRange(
    val start: Date,
    val end: Date
) : Parcelable

/**
 * This class represents the date range selection for the Analytics Hub
 *
 * You can create it through the [AnalyticsHubDateRangeSelection.SelectionType.generateSelectionData]
 * function since it will return the correct data for the given selection type
 *
 * When creating the object through the available constructor, the Selection will be set as [CUSTOM]
 */
class AnalyticsHubDateRangeSelection : Serializable {
    val selectionType: SelectionType
    val currentRange: AnalyticsHubTimeRange
    val previousRange: AnalyticsHubTimeRange
    val currentRangeDescription: String
    val previousRangeDescription: String

    private constructor(
        rangeStart: Date,
        rangeEnd: Date,
        calendar: Calendar,
        locale: Locale,
    ) {
        val rangeData = CustomRangeData(rangeStart, rangeEnd, locale, calendar)
        selectionType = CUSTOM
        currentRange = rangeData.currentRange
        previousRange = rangeData.previousRange
        currentRangeDescription = rangeData.formattedCurrentRange
        previousRangeDescription = rangeData.formattedPreviousRange
    }

    private constructor(
        selectionType: SelectionType,
        referenceDate: Date,
        calendar: Calendar,
        locale: Locale
    ) {
        val rangeData = generateTimeRangeData(selectionType, referenceDate, locale, calendar)
        this.selectionType = selectionType
        currentRange = rangeData.currentRange
        previousRange = rangeData.previousRange
        currentRangeDescription = rangeData.formattedCurrentRange
        previousRangeDescription = rangeData.formattedPreviousRange
    }

    private fun generateTimeRangeData(
        selectionType: SelectionType,
        referenceDate: Date,
        locale: Locale,
        calendar: Calendar
    ): AnalyticsHubTimeRangeData {
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

    companion object {
        // Needed to avoid the [SerialVersionUIDInSerializableClass] warning from detekt
        const val serialVersionUID = 1L
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
            referenceStartDate: Date = Date(),
            referenceEndDate: Date = Date(),
            calendar: Calendar,
            locale: Locale
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

        val tracksIdentifier: String
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
                }

            fun from(description: String): SelectionType {
                return values().firstOrNull { it.toString() == description } ?: CUSTOM
            }

            val names: Array<String>
                get() = values().map { it.name }.toTypedArray()
        }
    }
}

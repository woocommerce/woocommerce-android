package com.woocommerce.android.ui.analytics.daterangeselector

import com.woocommerce.android.R
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.viewmodel.ResourceProvider
import javax.inject.Inject

class AnalyticsDateRangeFormatter @Inject constructor(
    private val dateUtils: DateUtils,
    private val resourceProvider: ResourceProvider
) {
    fun fromDescription(range: AnalyticsDateRange): String {
        return when (range) {
            is SimpleDateRange -> simpleRangeFromDescription(range)
            is MultipleDateRange -> multipleRangeFromDescription(range)
            else -> defaultRangeFromDescription(range)
        }
    }

    fun toDescription(range: AnalyticsDateRange, timePeriodDescription: String): String {
        return when (range) {
            is SimpleDateRange -> simpleRangeToDescription(range, timePeriodDescription)
            is MultipleDateRange -> multipleRangeToDescription(range, timePeriodDescription)
            else -> defaultRangeToDescription(range, timePeriodDescription)
        }
    }

    private fun defaultRangeFromDescription(range: AnalyticsDateRange): String {
        val comparisonPeriod = range.getComparisonPeriod()
        val formattedDate = if (com.zendesk.util.DateUtils.isSameDay(comparisonPeriod.from, comparisonPeriod.to)) {
            val yyyyMMddDate = dateUtils.getYearMonthDayStringFromDate(comparisonPeriod.from)
            dateUtils.getShortMonthDayAndYearString(yyyyMMddDate).orEmpty()
        } else {
            getFormattedCustomRange(comparisonPeriod)
        }
        return resourceProvider.getString(
            R.string.analytics_date_range_from_date,
            formattedDate
        )
    }

    private fun defaultRangeToDescription(range: AnalyticsDateRange, timePeriodDescription: String): String {
        val selectedPeriod = range.getSelectedPeriod()
        val formattedDate = if (com.zendesk.util.DateUtils.isSameDay(selectedPeriod.from, selectedPeriod.to)) {
            val yyyyMMddDate = dateUtils.getYearMonthDayStringFromDate(selectedPeriod.from)
            dateUtils.getShortMonthDayAndYearString(yyyyMMddDate).orEmpty()
        } else {
            getFormattedCustomRange(selectedPeriod)
        }
        return resourceProvider.getString(
            R.string.analytics_date_range_to_date,
            timePeriodDescription,
            formattedDate
        )
    }

    private fun simpleRangeFromDescription(range: SimpleDateRange): String {
        val yyyyMMddDate = dateUtils.getYearMonthDayStringFromDate(range.from)
        val formattedDate = dateUtils.getShortMonthDayAndYearString(yyyyMMddDate).orEmpty()

        return resourceProvider.getString(
            R.string.analytics_date_range_from_date,
            formattedDate
        )
    }

    private fun simpleRangeToDescription(range: SimpleDateRange, timePeriodDescription: String): String {
        val yyyyMMddDate = dateUtils.getYearMonthDayStringFromDate(range.to)
        val formattedDate = dateUtils.getShortMonthDayAndYearString(yyyyMMddDate).orEmpty()

        return resourceProvider.getString(
            R.string.analytics_date_range_to_date,
            timePeriodDescription,
            formattedDate
        )
    }

    private fun multipleRangeFromDescription(range: MultipleDateRange): String {
        val formattedDate = if (com.zendesk.util.DateUtils.isSameDay(range.from.from, range.from.to)) {
            val yyyyMMddDate = dateUtils.getYearMonthDayStringFromDate(range.from.from)
            dateUtils.getShortMonthDayAndYearString(yyyyMMddDate).orEmpty()
        } else {
            range.from.formatDatesToFriendlyPeriod()
        }
        return resourceProvider.getString(
            R.string.analytics_date_range_from_date,
            formattedDate
        )
    }

    private fun multipleRangeToDescription(range: MultipleDateRange, timePeriodDescription: String): String {
        val formattedDate = if (com.zendesk.util.DateUtils.isSameDay(range.to.from, range.to.to)) {
            val yyyyMMddDate = dateUtils.getYearMonthDayStringFromDate(range.to.from)
            dateUtils.getShortMonthDayAndYearString(yyyyMMddDate).orEmpty()
        } else {
            range.to.formatDatesToFriendlyPeriod()
        }
        return resourceProvider.getString(
            R.string.analytics_date_range_to_date,
            timePeriodDescription,
            formattedDate
        )
    }

    private fun getFormattedCustomRange(range: DateRange): String {
        val yyyyMMddFromDate = dateUtils.getYearMonthDayStringFromDate(range.from)
        val yyyyMMddToDate = dateUtils.getYearMonthDayStringFromDate(range.to)

        val shortFromDate = dateUtils.getShortMonthDayAndYearString(yyyyMMddFromDate).orEmpty()
        val shortToDate = dateUtils.getShortMonthDayAndYearString(yyyyMMddToDate).orEmpty()

        return resourceProvider.getString(R.string.analytics_date_range_custom, shortFromDate, shortToDate)
    }
}

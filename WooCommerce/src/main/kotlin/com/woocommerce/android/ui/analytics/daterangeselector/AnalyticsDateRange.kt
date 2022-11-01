package com.woocommerce.android.ui.analytics.daterangeselector

import android.os.Parcelable
import com.woocommerce.android.R
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.viewmodel.ResourceProvider
import com.zendesk.util.DateUtils.isSameDay
import kotlinx.parcelize.Parcelize
import java.util.Date

data class DateRange(val from: Date, val to: Date)

interface AnalyticsDateRange {
    fun getCurrentPeriod(): DateRange
    fun getPreviousPeriod(): DateRange
    fun getTotalPeriod(): DateRange

    fun fromDescription(dateUtils: DateUtils, resourceProvider: ResourceProvider): String
    fun toDescription(dateUtils: DateUtils, resourceProvider: ResourceProvider, timePeriodDescription: String): String
}

@Parcelize
class SimpleDateRange(
    val from: Date,
    val to: Date
) : AnalyticsDateRange, Parcelable {
    override fun getCurrentPeriod(): DateRange = DateRange(from = from, to = to)
    override fun getPreviousPeriod(): DateRange = DateRange(from = from, to = from)
    override fun getTotalPeriod(): DateRange = DateRange(from = from, to = to)

    override fun fromDescription(dateUtils: DateUtils, resourceProvider: ResourceProvider): String {
        return resourceProvider.getString(
            R.string.analytics_date_range_from_date,
            dateUtils.getShortMonthDayAndYearString(dateUtils.getYearMonthDayStringFromDate(from)).orEmpty()
        )
    }

    override fun toDescription(
        dateUtils: DateUtils,
        resourceProvider: ResourceProvider,
        timePeriodDescription: String
    ): String {
        return resourceProvider.getString(
            R.string.analytics_date_range_to_date,
            timePeriodDescription,
            dateUtils.getShortMonthDayAndYearString(
                dateUtils.getYearMonthDayStringFromDate(to)
            ).orEmpty()
        )
    }
}

@Parcelize
class MultipleDateRange(
    val from: SimpleDateRange,
    val to: SimpleDateRange
) : AnalyticsDateRange, Parcelable {
    override fun getCurrentPeriod(): DateRange = DateRange(from = to.from, to = to.to)
    override fun getPreviousPeriod(): DateRange = DateRange(from = from.from, to = from.to)
    override fun getTotalPeriod(): DateRange = DateRange(from = from.from, to = to.to)

    override fun fromDescription(dateUtils: DateUtils, resourceProvider: ResourceProvider): String {
        return if (isSameDay(from.from, from.to)) {
            resourceProvider.getString(
                R.string.analytics_date_range_from_date,
                dateUtils.getShortMonthDayAndYearString(
                    dateUtils.getYearMonthDayStringFromDate(from.from)
                ).orEmpty()
            )
        } else {
            resourceProvider.getString(
                R.string.analytics_date_range_from_date,
                from.formatDatesToFriendlyPeriod()
            )
        }
    }

    override fun toDescription(
        dateUtils: DateUtils,
        resourceProvider: ResourceProvider,
        timePeriodDescription: String
    ): String {
        return if (isSameDay(to.from, to.to)) {
            resourceProvider.getString(
                R.string.analytics_date_range_to_date,
                timePeriodDescription,
                dateUtils.getShortMonthDayAndYearString(
                    dateUtils.getYearMonthDayStringFromDate(to.from)
                ).orEmpty()
            )
        } else {
            resourceProvider.getString(
                R.string.analytics_date_range_to_date,
                timePeriodDescription,
                to.formatDatesToFriendlyPeriod()
            )
        }
    }
}

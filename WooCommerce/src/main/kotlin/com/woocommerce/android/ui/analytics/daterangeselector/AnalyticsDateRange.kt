package com.woocommerce.android.ui.analytics.daterangeselector

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

data class DateRange(val startDate: Date, val endDate: Date)

sealed interface AnalyticsDateRange {
    fun getCurrentPeriod(): DateRange
    fun getPreviousPeriod(): DateRange
    fun getTotalPeriod(): DateRange
}

@Parcelize
class SimpleDateRange(
    val from: Date,
    val to: Date
) : AnalyticsDateRange, Parcelable {
    override fun getCurrentPeriod(): DateRange = DateRange(startDate = from, endDate = to)
    override fun getPreviousPeriod(): DateRange = DateRange(startDate = from, endDate = from)
    override fun getTotalPeriod(): DateRange = DateRange(startDate = from, endDate = to)
}

@Parcelize
class MultipleDateRange(
    val from: SimpleDateRange,
    val to: SimpleDateRange
) : AnalyticsDateRange, Parcelable {
    override fun getCurrentPeriod(): DateRange = DateRange(startDate = to.from, endDate = to.to)
    override fun getPreviousPeriod(): DateRange = DateRange(startDate = from.from, endDate = from.to)
    override fun getTotalPeriod(): DateRange = DateRange(startDate = from.from, endDate = to.to)
}

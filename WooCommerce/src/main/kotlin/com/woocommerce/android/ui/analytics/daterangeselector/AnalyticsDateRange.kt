package com.woocommerce.android.ui.analytics.daterangeselector

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

data class DateRange(val from: Date, val to: Date)

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
    override fun getCurrentPeriod(): DateRange = DateRange(from = from, to = to)
    override fun getPreviousPeriod(): DateRange = DateRange(from = from, to = from)
    override fun getTotalPeriod(): DateRange = DateRange(from = from, to = to)
}

@Parcelize
class MultipleDateRange(
    val from: SimpleDateRange,
    val to: SimpleDateRange
) : AnalyticsDateRange, Parcelable {
    override fun getCurrentPeriod(): DateRange = DateRange(from = to.from, to = to.to)
    override fun getPreviousPeriod(): DateRange = DateRange(from = from.from, to = from.to)
    override fun getTotalPeriod(): DateRange = DateRange(from = from.from, to = to.to)
}

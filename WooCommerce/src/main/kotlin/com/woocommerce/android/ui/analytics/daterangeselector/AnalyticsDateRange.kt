package com.woocommerce.android.ui.analytics.daterangeselector

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

data class DateRange(val from: Date, val to: Date)

interface AnalyticsDateRange {
    fun getSelectedPeriod(): DateRange
    fun getComparisonPeriod(): DateRange
    fun getAnalyzedPeriod(): DateRange
}

@Parcelize
class SimpleDateRange(
    val from: Date,
    val to: Date
) : AnalyticsDateRange, Parcelable {
    override fun getSelectedPeriod(): DateRange = DateRange(from = from, to = to)
    override fun getComparisonPeriod(): DateRange = DateRange(from = from, to = from)
    override fun getAnalyzedPeriod(): DateRange = DateRange(from = from, to = to)
}

@Parcelize
class MultipleDateRange(
    val from: SimpleDateRange,
    val to: SimpleDateRange
) : AnalyticsDateRange, Parcelable {
    override fun getSelectedPeriod(): DateRange = DateRange(from = to.from, to = to.to)
    override fun getComparisonPeriod(): DateRange = DateRange(from = from.from, to = from.to)
    override fun getAnalyzedPeriod(): DateRange = DateRange(from = from.from, to = to.to)
}

package com.woocommerce.android.wear.ui.stats.range

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Calendar
import java.util.Date

@Parcelize
data class StatsTimeRange(
    val start: Date,
    val end: Date
) : Parcelable

abstract class StatsTimeRangeData(
    referenceCalendar: Calendar
) {
    abstract val currentRange: StatsTimeRange
    abstract val previousRange: StatsTimeRange
    abstract val formattedCurrentRange: String
    abstract val formattedPreviousRange: String

    protected val calendar = referenceCalendar.clone() as Calendar
}

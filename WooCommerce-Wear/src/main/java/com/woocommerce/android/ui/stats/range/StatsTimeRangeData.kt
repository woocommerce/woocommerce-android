package com.woocommerce.android.ui.stats.range

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.utils.SiteUtils
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale

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

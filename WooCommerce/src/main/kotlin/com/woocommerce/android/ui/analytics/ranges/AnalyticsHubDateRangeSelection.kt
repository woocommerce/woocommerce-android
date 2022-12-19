package com.woocommerce.android.ui.analytics.ranges

import java.util.Calendar
import java.util.Date

class AnalyticsHubDateRangeSelection(
    selectionType: AnalyticsHubRangeSelectionType,
    currentDate: Date = Date(),
    calendar: Calendar = Calendar.getInstance()
) {
    val currentRange: AnalyticsHubTimeRange?
    val previousRange: AnalyticsHubTimeRange?

    init {
        val rangeData = selectionType.generateTimeRangeData(currentDate, calendar)
        this.currentRange = rangeData.currentRange
        this.previousRange = rangeData.previousRange
    }
}

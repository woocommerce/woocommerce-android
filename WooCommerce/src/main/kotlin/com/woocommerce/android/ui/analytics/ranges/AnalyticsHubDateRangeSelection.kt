package com.woocommerce.android.ui.analytics.ranges

import java.util.*

class AnalyticsHubDateRangeSelection(
    private val selectionType: AnalyticsHubRangeSelectionType,
    private val currentDate: Date = Date()
) {
    val currentRange: AnalyticsHubTimeRange?
    val previousRange: AnalyticsHubTimeRange?

    init {
        val rangeData = selectionType.generateTimeRangeData(currentDate)
        this.currentRange = rangeData.currentRange
        this.previousRange = rangeData.previousRange
    }
}

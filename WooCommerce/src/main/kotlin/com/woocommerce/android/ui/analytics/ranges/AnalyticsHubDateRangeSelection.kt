package com.woocommerce.android.ui.analytics.ranges

import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection.AnalyticsHubRangeSelectionType.TODAY
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection.AnalyticsHubRangeSelectionType.YESTERDAY
import com.woocommerce.android.ui.analytics.ranges.data.AnalyticsHubTodayRangeData
import com.woocommerce.android.ui.analytics.ranges.data.AnalyticsHubYesterdayRangeData
import java.util.*

class AnalyticsHubDateRangeSelection(
    selectionType: AnalyticsHubRangeSelectionType,
    currentDate: Date = Date(),
    calendar: Calendar = Calendar.getInstance()
) {
    val currentRange: AnalyticsHubTimeRange?
    val previousRange: AnalyticsHubTimeRange?

    init {
        val rangeData = generateTimeRangeData(selectionType, currentDate, calendar)
        this.currentRange = rangeData.currentRange
        this.previousRange = rangeData.previousRange
    }

    private fun generateTimeRangeData(
        selectedType: AnalyticsHubRangeSelectionType,
        referenceDate: Date,
        calendar: Calendar
    ): AnalyticsHubTimeRangeData {
        return when (selectedType) {
            TODAY -> AnalyticsHubTodayRangeData(referenceDate, calendar)
            YESTERDAY -> AnalyticsHubYesterdayRangeData(referenceDate, calendar)
        }
    }

    enum class AnalyticsHubRangeSelectionType {
        TODAY,
        YESTERDAY;
    }
}
